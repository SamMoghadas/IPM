package com.car.rental.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ZKTeco TCP client. Enroll uses realtime events; verification uses
 * disconnect + poll attendance logs because many TFT firmwares do not
 * push ATTLOG while a PC session is held open.
 */
public class ZkFingerprintService implements FingerprintService {

    private static final Logger logger = Logger.getLogger(ZkFingerprintService.class.getName());

    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_ENABLEDEVICE = 1002;
    private static final int CMD_DISABLEDEVICE = 1003;
    private static final int CMD_ACK_OK = 2000;
    private static final int CMD_ACK_ERROR = 2001;
    private static final int CMD_PREPARE_DATA = 1500;
    private static final int CMD_DATA = 1501;
    private static final int CMD_REG_EVENT = 500;
    private static final int CMD_ATTLOG_RRQ = 13;
    private static final int CMD_USER_WRQ = 8;
    private static final int CMD_USERTEMP_RRQ = 9;
    private static final int CMD_DELETE_USER = 18;
    private static final int CMD_STARTENROLL = 61;

    private static final int EF_FINGER = 2;
    private static final int EF_ENROLLUSER = 4;
    private static final int EF_ENROLLFINGER = 8;
    private static final int EF_FPFTR = 256;

    private static final int ENROLL_TIMEOUT_SECONDS = 60;
    private static final int VERIFY_POLL_MS = 1500;

    private static final byte[] PACKET_START = new byte[]{0x50, 0x50, (byte) 0x82, 0x7D};

    private final String host;
    private final int port;
    private final int connectTimeoutMs;

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private int sessionId;
    private int replyNumber;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean listening = new AtomicBoolean(false);

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "zk-fingerprint-worker");
        t.setDaemon(true);
        return t;
    });
    private Future<?> listenTask;

    public ZkFingerprintService(String host, int port) {
        this(host, port, 8000);
    }

    public ZkFingerprintService(String host, int port, int connectTimeoutMs) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public ZkFingerprintService() {
        this("192.168.1.200", 4370, 8000);
    }

    @Override
    public synchronized void connect() throws FingerprintException {
        if (connected.get()) {
            try {
                enableDevice();
            } catch (Exception e) {
                logger.log(Level.WARNING, "ENABLEDEVICE on existing connection failed", e);
            }
            return;
        }
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(8000);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            sessionId = 0;
            replyNumber = 0;

            byte[] reply = sendCommand(CMD_CONNECT, new byte[0]);
            if (reply == null) {
                closeQuietly();
                throw new FingerprintException("No reply from device on CONNECT");
            }
            if (getCommand(reply) != CMD_ACK_OK) {
                closeQuietly();
                throw new FingerprintException("Device rejected CONNECT");
            }
            sessionId = getSessionId(reply);
            connected.set(true);

            try {
                enableDevice();
            } catch (Exception e) {
                logger.log(Level.WARNING, "ENABLEDEVICE after connect failed", e);
            }

            logger.info("Connected to ZK " + host + ":" + port + " session=" + sessionId);
        } catch (IOException e) {
            closeQuietly();
            throw new FingerprintException("Cannot connect to device " + host + ":" + port, e);
        }
    }

    @Override
    public synchronized void disconnect() {
        cancelListen();
        disconnectQuietly();
    }

    /** Disconnect without cancelling an in-progress listen/poll task. */
    private synchronized void disconnectQuietly() {
        if (!connected.get()) {
            return;
        }
        try {
            try {
                enableDevice();
            } catch (Exception ignored) {
            }
            sendCommand(CMD_EXIT, new byte[0]);
        } catch (Exception ignored) {
        }
        closeQuietly();
        connected.set(false);
        logger.info("Disconnected from ZK device");
    }

    @Override
    public boolean isConnected() {
        return connected.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Wait for a fingerprint punch.
     * Detects new punches by growth of the raw attendance buffer (not by
     * unreliable parsed timestamps).
     */
    @Override
    public void listenForVerification(int timeoutSeconds,
                                      Consumer<VerificationResult> onVerified,
                                      Runnable onTimeout,
                                      Consumer<FingerprintException> onError) {
        cancelListen();
        listening.set(true);

        listenTask = executor.submit(() -> {
            try {
                logger.info("Verification poll started (timeout=" + timeoutSeconds + "s)");

                if (!isConnected()) {
                    connect();
                }

                AttLogSnapshot baseline = readAttendanceSnapshot();
                logger.info("Attendance baseline rawBytes=" + baseline.rawSize
                        + " validRecords=" + baseline.records.size());

                disconnectQuietly();
                logger.info("Device disconnected for local punch — place finger on terminal");

                long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;

                while (listening.get() && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(VERIFY_POLL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (!listening.get()) {
                        break;
                    }

                    try {
                        connect();
                        AttLogSnapshot snap = readAttendanceSnapshot();
                        disconnectQuietly();

                        if (snap.rawSize > baseline.rawSize) {
                            VerificationResult newer = pickNewestValid(snap.records);
                            if (newer == null) {
                                // Raw grew but parse failed — try parse only the tail bytes
                                newer = parseTailRecord(snap.rawBuffer, baseline.rawSize);
                            }
                            if (newer != null) {
                                logger.info("New punch detected userId=" + newer.getDeviceUserId()
                                        + " time=" + newer.getDeviceTime()
                                        + " raw " + baseline.rawSize + "->" + snap.rawSize);
                                listening.set(false);
                                if (onVerified != null) {
                                    onVerified.accept(newer);
                                }
                                return;
                            }
                            logger.warning("Attendance buffer grew but no valid userId parsed. "
                                    + "raw " + baseline.rawSize + "->" + snap.rawSize
                                    + " — check record format");
                            // Still advance baseline so we don't spin on same growth
                            baseline = snap;
                        } else {
                            logger.fine("Poll: no new attendance (raw=" + snap.rawSize + ")");
                        }
                    } catch (Exception pollEx) {
                        logger.log(Level.WARNING, "Attendance poll failed, will retry", pollEx);
                        try {
                            disconnectQuietly();
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (listening.get()) {
                    listening.set(false);
                    logger.info("Verification poll timed out");
                    if (onTimeout != null) {
                        onTimeout.run();
                    }
                }
            } catch (Exception e) {
                listening.set(false);
                logger.log(Level.SEVERE, "Verification poll error", e);
                try {
                    disconnectQuietly();
                } catch (Exception ignored) {
                }
                if (onError != null) {
                    onError.accept(new FingerprintException("Error while verifying: " + e.getMessage(), e));
                }
            }
        });
    }

    private static VerificationResult pickNewestValid(List<VerificationResult> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }
        return records.get(records.size() - 1);
    }

    private VerificationResult parseTailRecord(byte[] buffer, int fromOffset) {
        if (buffer == null || fromOffset < 0 || fromOffset >= buffer.length) {
            return null;
        }
        int tailLen = buffer.length - fromOffset;
        int[] sizes = {40, 36, 32, 16};
        for (int rs : sizes) {
            if (tailLen >= rs) {
                // parse the last full record in the buffer
                int start = buffer.length - rs;
                VerificationResult r = parseAttendanceRecord(buffer, start, rs);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    private static final class AttLogSnapshot {
        final int rawSize;
        final byte[] rawBuffer;
        final List<VerificationResult> records;

        AttLogSnapshot(byte[] rawBuffer, List<VerificationResult> records) {
            this.rawBuffer = rawBuffer == null ? new byte[0] : rawBuffer;
            this.rawSize = this.rawBuffer.length;
            this.records = records == null ? new ArrayList<>() : records;
        }
    }

    private synchronized AttLogSnapshot readAttendanceSnapshot()
            throws IOException, FingerprintException {
        ensureConnected();
        disableDevice();
        try {
            byte[] reply = sendCommand(CMD_ATTLOG_RRQ, new byte[0]);
            if (reply == null) {
                return new AttLogSnapshot(new byte[0], new ArrayList<>());
            }
            int cmd = getCommand(reply);
            if (cmd == CMD_ACK_OK || cmd == CMD_ACK_ERROR) {
                return new AttLogSnapshot(new byte[0], new ArrayList<>());
            }
            if (cmd != CMD_PREPARE_DATA) {
                logger.warning("ATTLOG_RRQ unexpected cmd=" + cmd);
                return new AttLogSnapshot(new byte[0], new ArrayList<>());
            }

            int size = 0;
            if (reply.length >= 20) {
                size = (reply[16] & 0xFF)
                        | ((reply[17] & 0xFF) << 8)
                        | ((reply[18] & 0xFF) << 16)
                        | ((reply[19] & 0xFF) << 24);
            }
            if (size <= 0) {
                return new AttLogSnapshot(new byte[0], new ArrayList<>());
            }

            byte[] buffer = readDataBuffer(size);
            List<VerificationResult> records = parseAttendanceBuffer(buffer);
            return new AttLogSnapshot(buffer, records);
        } finally {
            try {
                enableDevice();
            } catch (Exception e) {
                logger.log(Level.WARNING, "ENABLEDEVICE after ATTLOG read failed", e);
            }
        }
    }

    private byte[] readDataBuffer(int totalSize) throws IOException {
        byte[] buffer = new byte[totalSize];
        int offset = 0;
        int previousTimeout = socket.getSoTimeout();
        socket.setSoTimeout(5000);
        try {
            while (offset < totalSize) {
                byte[] packet = readPacket();
                if (packet == null) {
                    break;
                }
                int cmd = getCommand(packet);
                if (cmd == CMD_DATA) {
                    int payloadLen = packet.length - 16;
                    if (payloadLen > 0) {
                        int copy = Math.min(payloadLen, totalSize - offset);
                        System.arraycopy(packet, 16, buffer, offset, copy);
                        offset += copy;
                    }
                    sendAck();
                } else if (cmd == CMD_ACK_OK) {
                    break;
                } else {
                    logger.fine("DATA read skip cmd=" + cmd);
                    break;
                }
            }
        } finally {
            try {
                socket.setSoTimeout(previousTimeout);
            } catch (Exception ignored) {
            }
        }
        if (offset < totalSize) {
            byte[] trimmed = new byte[offset];
            System.arraycopy(buffer, 0, trimmed, 0, offset);
            return trimmed;
        }
        return buffer;
    }

    private List<VerificationResult> parseAttendanceBuffer(byte[] buffer) {
        List<VerificationResult> list = new ArrayList<>();
        if (buffer == null || buffer.length == 0) {
            return list;
        }

        // Prefer sizes that both divide the buffer and yield valid user ids
        int[] candidates = {40, 36, 32, 16};
        int bestSize = 0;
        int bestValid = -1;
        for (int c : candidates) {
            if (buffer.length % c != 0) {
                continue;
            }
            int valid = 0;
            for (int i = 0; i + c <= buffer.length; i += c) {
                if (parseAttendanceRecord(buffer, i, c) != null) {
                    valid++;
                }
            }
            if (valid > bestValid) {
                bestValid = valid;
                bestSize = c;
            }
        }

        if (bestSize == 0) {
            // Fallback: try 40 even if remainder
            bestSize = 40;
        }

        for (int i = 0; i + bestSize <= buffer.length; i += bestSize) {
            VerificationResult r = parseAttendanceRecord(buffer, i, bestSize);
            if (r != null) {
                list.add(r);
            }
        }

        // Debug: first record hex (helps tune format on this firmware)
        if (buffer.length >= 16) {
            int dumpLen = Math.min(bestSize > 0 ? bestSize : 40, Math.min(40, buffer.length));
            logger.info("AttLog first-record hex (" + dumpLen + " bytes): " + toHex(buffer, 0, dumpLen)
                    + " chosenSize=" + bestSize + " valid=" + list.size());
        }
        return list;
    }

    private VerificationResult parseAttendanceRecord(byte[] buf, int offset, int recordSize) {
        try {
            if (offset + recordSize > buf.length) {
                return null;
            }

            String userId = extractUserId(buf, offset, recordSize);
            if (userId == null) {
                return null;
            }

            LocalDateTime time = extractTime(buf, offset, recordSize);
            if (time == null) {
                // Do NOT use now() — that caused false "new punch" detection
                time = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
            }

            int verifyType = 1;
            if (recordSize >= 25) {
                verifyType = buf[offset + 24] & 0xFF;
            }

            return new VerificationResult(userId, time, verifyType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Accept only printable device user ids (digits / letters / _ -).
     * Rejects binary garbage that previously became "��2".
     */
    private String extractUserId(byte[] buf, int offset, int recordSize) {
        // Layout A: null-terminated ASCII pin at start (up to 24 chars) — common TFT
        int max = Math.min(24, recordSize);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            int b = buf[offset + i] & 0xFF;
            if (b == 0) {
                break;
            }
            if (isUserIdChar(b)) {
                sb.append((char) b);
            } else if (sb.length() == 0) {
                // leading non-ascii → not string layout
                break;
            } else {
                break;
            }
        }
        if (sb.length() > 0) {
            return sb.toString();
        }

        // Layout B: uint16 internal uid at offset 0 (old devices) — expose as decimal string
        if (recordSize >= 2) {
            int uid = (buf[offset] & 0xFF) | ((buf[offset + 1] & 0xFF) << 8);
            if (uid >= 1 && uid <= 65535) {
                // Only accept if rest of "string area" is mostly zeros (heuristic)
                int nonZero = 0;
                for (int i = 2; i < Math.min(8, recordSize); i++) {
                    if (buf[offset + i] != 0) nonZero++;
                }
                if (nonZero <= 2) {
                    return String.valueOf(uid);
                }
            }
        }
        return null;
    }

    private static boolean isUserIdChar(int b) {
        return (b >= '0' && b <= '9')
                || (b >= 'A' && b <= 'Z')
                || (b >= 'a' && b <= 'z')
                || b == '_' || b == '-';
    }

    private LocalDateTime extractTime(byte[] buf, int offset, int recordSize) {
        // Layout A: 6-byte wall time (year-2000, mon, day, hour, min, sec) at +26 (40-byte style)
        if (recordSize >= 32) {
            LocalDateTime t = tryWallTime(buf, offset + 26);
            if (t != null) return t;
            t = tryWallTime(buf, offset + 27);
            if (t != null) return t;
            t = tryWallTime(buf, offset + 24);
            if (t != null) return t;
        }

        // Layout B: classic 4-byte ZK packed time near end of 16-byte records
        if (recordSize >= 8) {
            for (int off : new int[]{offset + 4, offset + 6, offset + recordSize - 4}) {
                if (off >= offset && off + 4 <= offset + recordSize) {
                    LocalDateTime t = decodeZkTime(buf, off);
                    if (t != null) return t;
                }
            }
        }
        return null;
    }

    private LocalDateTime tryWallTime(byte[] buf, int off) {
        if (off + 6 > buf.length) return null;
        int y = (buf[off] & 0xFF) + 2000;
        int mo = buf[off + 1] & 0xFF;
        int d = buf[off + 2] & 0xFF;
        int h = buf[off + 3] & 0xFF;
        int mi = buf[off + 4] & 0xFF;
        int s = buf[off + 5] & 0xFF;
        if (mo < 1 || mo > 12 || d < 1 || d > 31 || h > 23 || mi > 59 || s > 59) {
            return null;
        }
        if (y < 2000 || y > 2090) {
            return null;
        }
        try {
            return LocalDateTime.of(y, mo, d, h, mi, s);
        } catch (Exception e) {
            return null;
        }
    }

    /** ZK classic packed time (little-endian uint32). */
    private LocalDateTime decodeZkTime(byte[] buf, int off) {
        long t = (buf[off] & 0xFFL)
                | ((buf[off + 1] & 0xFFL) << 8)
                | ((buf[off + 2] & 0xFFL) << 16)
                | ((buf[off + 3] & 0xFFL) << 24);
        if (t == 0 || t > 0x7FFFFFFFL) {
            return null;
        }
        int second = (int) (t % 60);
        t /= 60;
        int minute = (int) (t % 60);
        t /= 60;
        int hour = (int) (t % 24);
        t /= 24;
        int day = (int) (t % 31) + 1;
        t /= 31;
        int month = (int) (t % 12) + 1;
        t /= 12;
        int year = (int) t + 2000;
        if (month < 1 || month > 12 || day < 1 || day > 31 || year < 2000 || year > 2090) {
            return null;
        }
        try {
            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (Exception e) {
            return null;
        }
    }

    private static String toHex(byte[] buf, int off, int len) {
        StringBuilder sb = new StringBuilder(len * 3);
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", buf[off + i] & 0xFF));
        }
        return sb.toString();
    }

    @Override
    public void cancelListen() {
        listening.set(false);
        if (listenTask != null) {
            listenTask.cancel(true);
            listenTask = null;
        }
    }

    @Override
    public List<DeviceUser> getUsers() throws FingerprintException {
        ensureConnected();
        return new ArrayList<>();
    }

    @Override
    public synchronized void createUser(String deviceUserId, String name) throws FingerprintException {
        ensureConnected();
        int uid = toInternalUid(deviceUserId);
        try {
            disableDevice();
            try {
                try {
                    deleteUserByUid(uid);
                } catch (FingerprintException ignored) {
                }
                writeUser(uid, name == null ? "" : name, deviceUserId);
            } finally {
                enableDevice();
            }
        } catch (IOException e) {
            throw new FingerprintException("createUser failed: " + e.getMessage(), e);
        }
    }

    public synchronized void updateUserName(String deviceUserId, String name) throws FingerprintException {
        ensureConnected();
        int uid = toInternalUid(deviceUserId);
        try {
            disableDevice();
            try {
                writeUser(uid, name == null ? "" : name, deviceUserId);
            } finally {
                enableDevice();
            }
        } catch (IOException e) {
            throw new FingerprintException("updateUserName failed: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void deleteUser(String deviceUserId) throws FingerprintException {
        ensureConnected();
        int uid = toInternalUid(deviceUserId);
        try {
            deleteUserByUid(uid);
        } catch (IOException e) {
            throw new FingerprintException("deleteUser failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void startEnroll(String deviceUserId, int fingerIndex,
                            Consumer<EnrollResult> onFinished,
                            Consumer<FingerprintException> onError) {
        executor.submit(() -> {
            try {
                ensureConnected();
                if (fingerIndex < 0 || fingerIndex > 9) {
                    throw new FingerprintException("finger index must be 0..9");
                }
                sendStartEnroll(deviceUserId, fingerIndex);
                waitForEnrollDeviceEvent(ENROLL_TIMEOUT_SECONDS);
                if (onFinished != null) {
                    onFinished.accept(new EnrollResult(true,
                            "Enroll finished for user " + deviceUserId));
                }
            } catch (FingerprintException e) {
                if (onError != null) {
                    onError.accept(e);
                }
            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(new FingerprintException("Enroll failed: " + e.getMessage(), e));
                }
            }
        });
    }

    public synchronized void registerUserWithFingerprint(String deviceUserId, String name, int fingerIndex)
            throws FingerprintException {
        ensureConnected();
        if (fingerIndex < 0 || fingerIndex > 9) {
            throw new FingerprintException("finger index must be 0..9");
        }
        boolean userCreated = false;
        try {
            createUser(deviceUserId, name);
            userCreated = true;
            try {
                sendStartEnroll(deviceUserId, fingerIndex);
            } catch (IOException e) {
                throw new FingerprintException("STARTENROLL failed: " + e.getMessage(), e);
            }
            waitForEnrollDeviceEvent(ENROLL_TIMEOUT_SECONDS);
            try {
                enableDevice();
            } catch (Exception e) {
                logger.log(Level.WARNING, "ENABLEDEVICE after enroll failed", e);
            }
        } catch (FingerprintException e) {
            if (userCreated) {
                try {
                    deleteUser(deviceUserId);
                } catch (FingerprintException delEx) {
                    logger.log(Level.WARNING, "Rollback deleteUser failed", delEx);
                }
            }
            throw e;
        }
    }

    public synchronized void enrollFingerOnly(String deviceUserId, int fingerIndex)
            throws FingerprintException {
        ensureConnected();
        if (fingerIndex < 0 || fingerIndex > 9) {
            throw new FingerprintException("finger index must be 0..9");
        }
        try {
            sendStartEnroll(deviceUserId, fingerIndex);
        } catch (IOException e) {
            throw new FingerprintException("STARTENROLL failed: " + e.getMessage(), e);
        }
        waitForEnrollDeviceEvent(ENROLL_TIMEOUT_SECONDS);
        try {
            enableDevice();
        } catch (Exception e) {
            logger.log(Level.WARNING, "ENABLEDEVICE after enrollFingerOnly failed", e);
        }
    }

    private void waitForEnrollDeviceEvent(int timeoutSeconds) throws FingerprintException {
        try {
            byte[] regData = new byte[]{(byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
            try {
                byte[] regReply = sendCommand(CMD_REG_EVENT, regData);
                if (regReply != null && getCommand(regReply) != CMD_ACK_OK) {
                    logger.warning("REG_EVENT register not ACK during enroll; still listening");
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not register events before enroll wait", e);
            }

            long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
            int previousTimeout = socket.getSoTimeout();
            socket.setSoTimeout(1000);
            try {
                while (System.currentTimeMillis() < deadline) {
                    try {
                        byte[] packet = readPacket();
                        if (packet == null) {
                            continue;
                        }
                        int cmd = getCommand(packet);
                        if (cmd != CMD_REG_EVENT) {
                            continue;
                        }

                        int eventCode = getSessionId(packet);
                        sendAck();
                        logger.info("Enroll realtime event code=" + eventCode);

                        if (eventCode == EF_FINGER || eventCode == EF_FPFTR
                                || (eventCode & EF_FPFTR) != 0) {
                            continue;
                        }

                        if (eventCode == EF_ENROLLFINGER || (eventCode & EF_ENROLLFINGER) != 0) {
                            int result = parseEnrollFingerResult(packet);
                            if (result == 0) {
                                logger.info("EF_ENROLLFINGER success (result=0)");
                                return;
                            }
                            throw new FingerprintException(
                                    "ثبت اثر انگشت ناموفق بود (کد " + result
                                            + "). احتمالاً اثر تکراری است؛ انگشت دیگری انتخاب کنید.");
                        }

                        if (eventCode == EF_ENROLLUSER || (eventCode & EF_ENROLLUSER) != 0) {
                            logger.info("EF_ENROLLUSER received — treating as enroll complete");
                            return;
                        }
                    } catch (java.net.SocketTimeoutException ste) {
                        // keep waiting
                    }
                }
            } finally {
                try {
                    socket.setSoTimeout(previousTimeout);
                } catch (Exception ignored) {
                }
            }

            throw new FingerprintException(
                    "زمان ثبت اثر انگشت تمام شد. هر ۳ بار اسکن را کامل کنید یا انگشت دیگری امتحان کنید.");
        } catch (FingerprintException e) {
            throw e;
        } catch (Exception e) {
            throw new FingerprintException("Error while waiting for enroll: " + e.getMessage(), e);
        }
    }

    private int parseEnrollFingerResult(byte[] fullPacket) {
        if (fullPacket.length < 18) {
            return 0;
        }
        return (fullPacket[16] & 0xFF) | ((fullPacket[17] & 0xFF) << 8);
    }

    @Override
    public DeviceInfo getDeviceInfo() throws FingerprintException {
        ensureConnected();
        return new DeviceInfo("unknown", "unknown", "ZMM100_TFT", host + ":" + port);
    }

    public static int toInternalUid(String deviceUserId) {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("deviceUserId is empty");
        }
        String trimmed = deviceUserId.trim();
        try {
            long n = Long.parseLong(trimmed);
            if (n >= 1 && n <= 65535) {
                return (int) n;
            }
        } catch (NumberFormatException ignored) {
        }
        int h = Math.abs(trimmed.hashCode() % 65535);
        return h == 0 ? 1 : h;
    }

    private void disableDevice() throws IOException, FingerprintException {
        requireAck(sendCommand(CMD_DISABLEDEVICE, new byte[0]), "DISABLEDEVICE");
    }

    private void enableDevice() throws IOException, FingerprintException {
        requireAck(sendCommand(CMD_ENABLEDEVICE, new byte[0]), "ENABLEDEVICE");
    }

    private void deleteUserByUid(int uid) throws IOException, FingerprintException {
        byte[] data = new byte[2];
        data[0] = (byte) (uid & 0xFF);
        data[1] = (byte) ((uid >> 8) & 0xFF);
        requireAck(sendCommand(CMD_DELETE_USER, data), "DELETE_USER");
    }

    private void writeUser(int uid, String name, String userId) throws IOException, FingerprintException {
        byte[] password = padFixed("", 8);
        byte[] nameBytes = padFixed(name, 24);
        byte[] card = new byte[4];
        byte[] groupId = padFixed("", 7);
        byte[] userIdBytes = padFixed(userId, 24);

        byte[] data = new byte[72];
        int o = 0;
        data[o++] = (byte) (uid & 0xFF);
        data[o++] = (byte) ((uid >> 8) & 0xFF);
        data[o++] = 0;
        System.arraycopy(password, 0, data, o, 8); o += 8;
        System.arraycopy(nameBytes, 0, data, o, 24); o += 24;
        System.arraycopy(card, 0, data, o, 4); o += 4;
        data[o++] = 0;
        System.arraycopy(groupId, 0, data, o, 7); o += 7;
        data[o++] = 0;
        System.arraycopy(userIdBytes, 0, data, o, 24);

        requireAck(sendCommand(CMD_USER_WRQ, data), "USER_WRQ");
    }

    private void sendStartEnroll(String deviceUserId, int fingerIndex)
            throws IOException, FingerprintException {
        byte[] data = new byte[26];
        System.arraycopy(padFixed(deviceUserId, 24), 0, data, 0, 24);
        data[24] = (byte) (fingerIndex & 0xFF);
        data[25] = 1;
        requireAck(sendCommand(CMD_STARTENROLL, data), "STARTENROLL");
    }

    private void ensureConnected() throws FingerprintException {
        if (!isConnected()) {
            throw new FingerprintException("Not connected to device");
        }
    }

    private void requireAck(byte[] reply, String op) throws FingerprintException {
        if (reply == null || getCommand(reply) != CMD_ACK_OK) {
            throw new FingerprintException(op + " not acknowledged by device");
        }
    }

    private synchronized byte[] sendCommand(int command, byte[] data) throws IOException {
        int replyId = replyNumber & 0xFFFF;
        replyNumber = (replyNumber + 1) & 0xFFFF;
        byte[] packet = buildPacket(command, sessionId, replyId, data);
        out.write(packet);
        out.flush();
        return readPacket();
    }

    private synchronized void sendAck() {
        try {
            out.write(buildPacket(CMD_ACK_OK, sessionId, 0, new byte[0]));
            out.flush();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to send ACK", e);
        }
    }

    private byte[] buildPacket(int command, int session, int replyId, byte[] data) {
        int dataLen = data == null ? 0 : data.length;
        int payloadSize = 8 + dataLen;
        byte[] payload = new byte[payloadSize];
        payload[0] = (byte) (command & 0xFF);
        payload[1] = (byte) ((command >> 8) & 0xFF);
        payload[2] = 0;
        payload[3] = 0;
        payload[4] = (byte) (session & 0xFF);
        payload[5] = (byte) ((session >> 8) & 0xFF);
        payload[6] = (byte) (replyId & 0xFF);
        payload[7] = (byte) ((replyId >> 8) & 0xFF);
        if (dataLen > 0) {
            System.arraycopy(data, 0, payload, 8, dataLen);
        }
        int checksum = createChecksum(payload);
        payload[2] = (byte) (checksum & 0xFF);
        payload[3] = (byte) ((checksum >> 8) & 0xFF);

        byte[] packet = new byte[8 + payloadSize];
        System.arraycopy(PACKET_START, 0, packet, 0, 4);
        packet[4] = (byte) (payloadSize & 0xFF);
        packet[5] = (byte) ((payloadSize >> 8) & 0xFF);
        packet[6] = (byte) ((payloadSize >> 16) & 0xFF);
        packet[7] = (byte) ((payloadSize >> 24) & 0xFF);
        System.arraycopy(payload, 0, packet, 8, payloadSize);
        return packet;
    }

    private static int createChecksum(byte[] payload) {
        int chksum = 0;
        for (int i = 0; i < payload.length; i += 2) {
            if (i == payload.length - 1) {
                chksum += payload[i] & 0xFF;
            } else {
                chksum += (payload[i] & 0xFF) + ((payload[i + 1] & 0xFF) << 8);
            }
            if (chksum > 65535) {
                chksum -= 65535;
            }
        }
        chksum = ~chksum;
        while (chksum < 0) {
            chksum += 65536;
        }
        return chksum & 0xFFFF;
    }

    private byte[] readPacket() throws IOException {
        byte[] header = readFully(8);
        if (header == null) return null;
        if ((header[0] & 0xFF) != 0x50 || (header[1] & 0xFF) != 0x50) {
            return null;
        }
        int payloadSize = (header[4] & 0xFF)
                | ((header[5] & 0xFF) << 8)
                | ((header[6] & 0xFF) << 16)
                | ((header[7] & 0xFF) << 24);
        if (payloadSize < 8 || payloadSize > 1024 * 1024) {
            return null;
        }
        byte[] payload = readFully(payloadSize);
        if (payload == null) return null;
        byte[] full = new byte[8 + payloadSize];
        System.arraycopy(header, 0, full, 0, 8);
        System.arraycopy(payload, 0, full, 8, payloadSize);
        return full;
    }

    private byte[] readFully(int len) throws IOException {
        byte[] buf = new byte[len];
        int off = 0;
        while (off < len) {
            int n = in.read(buf, off, len - off);
            if (n < 0) return null;
            off += n;
        }
        return buf;
    }

    private int getCommand(byte[] fullPacket) {
        return (fullPacket[8] & 0xFF) | ((fullPacket[9] & 0xFF) << 8);
    }

    private int getSessionId(byte[] fullPacket) {
        return (fullPacket[12] & 0xFF) | ((fullPacket[13] & 0xFF) << 8);
    }

    private static byte[] padFixed(String s, int len) {
        byte[] src = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[len];
        System.arraycopy(src, 0, out, 0, Math.min(src.length, len));
        return out;
    }

    private void closeQuietly() {
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        in = null;
        out = null;
        socket = null;
    }
}

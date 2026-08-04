package com.car.rental.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
 * Real implementation of FingerprintService for ZKTeco devices (TCP port 4370).
 *
 * Currently supports:
 *  - connect / disconnect
 *  - listenForVerification (realtime attendance events)
 *  - basic device session
 *
 * User management (getUsers / createUser / deleteUser / startEnroll) is prepared
 * but not fully implemented yet – will be completed in the next step.
 *
 * Protocol reference: https://github.com/adrobinoga/zk-protocol
 */
public class ZkFingerprintService implements FingerprintService {

    private static final Logger logger = Logger.getLogger(ZkFingerprintService.class.getName());

    // ZK protocol constants
    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_ENABLEDEVICE = 1002;
    private static final int CMD_DISABLEDEVICE = 1003;
    private static final int CMD_ACK_OK = 2000;
    private static final int CMD_ACK_ERROR = 2001;
    private static final int CMD_REG_EVENT = 500;

    private static final int EF_ATTLOG = 1;

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

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "zk-fingerprint-listener");
        t.setDaemon(true);
        return t;
    });
    private Future<?> listenTask;

    public ZkFingerprintService(String host, int port) {
        this(host, port, 5000);
    }

    public ZkFingerprintService(String host, int port, int connectTimeoutMs) {
        this.host = host;
        this.port = port;
        this.connectTimeoutMs = connectTimeoutMs;
    }

    /** Default: 192.168.1.200:4370 */
    public ZkFingerprintService() {
        this("192.168.1.200", 4370, 5000);
    }

    // ==================== Connection ====================

    @Override
    public synchronized void connect() throws FingerprintException {
        if (connected.get()) {
            return;
        }
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(3000);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            sessionId = 0;
            replyNumber = 0;

            // CMD_CONNECT
            byte[] reply = sendCommand(CMD_CONNECT, new byte[0]);
            if (reply == null || getCommand(reply) != CMD_ACK_OK) {
                closeQuietly();
                throw new FingerprintException("Device did not acknowledge CONNECT");
            }
            // session id comes from the reply header
            sessionId = getSessionId(reply);
            connected.set(true);
            logger.info("Connected to ZK device " + host + ":" + port + " session=" + sessionId);
        } catch (IOException e) {
            closeQuietly();
            throw new FingerprintException("Cannot connect to device " + host + ":" + port, e);
        }
    }

    @Override
    public synchronized void disconnect() {
        cancelListen();
        if (!connected.get()) {
            return;
        }
        try {
            sendCommand(CMD_EXIT, new byte[0]);
        } catch (Exception ignored) {
            // ignore on shutdown
        }
        closeQuietly();
        connected.set(false);
        logger.info("Disconnected from ZK device");
    }

    @Override
    public boolean isConnected() {
        return connected.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    // ==================== Real-time verification ====================

    @Override
    public void listenForVerification(int timeoutSeconds,
                                      Consumer<VerificationResult> onVerified,
                                      Runnable onTimeout,
                                      Consumer<FingerprintException> onError) {
        if (!isConnected()) {
            if (onError != null) {
                onError.accept(new FingerprintException("Not connected to device"));
            }
            return;
        }

        cancelListen();
        listening.set(true);

        listenTask = executor.submit(() -> {
            try {
                // Register for realtime events (all events: 0xFFFF)
                byte[] regData = new byte[]{(byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
                byte[] regReply = sendCommand(CMD_REG_EVENT, regData);
                if (regReply == null || getCommand(regReply) != CMD_ACK_OK) {
                    throw new FingerprintException("Failed to register realtime events");
                }

                long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
                socket.setSoTimeout(1000); // poll every 1s to check timeout / cancel

                while (listening.get() && System.currentTimeMillis() < deadline) {
                    try {
                        byte[] packet = readPacket();
                        if (packet == null) continue;

                        int cmd = getCommand(packet);
                        if (cmd == CMD_REG_EVENT) {
                            int eventCode = getSessionId(packet); // in realtime packets, session field = event code
                            if (eventCode == EF_ATTLOG) {
                                VerificationResult result = parseAttLog(packet);
                                if (result != null) {
                                    // ACK the event
                                    sendAck();
                                    listening.set(false);
                                    if (onVerified != null) {
                                        onVerified.accept(result);
                                    }
                                    return;
                                }
                            } else {
                                // ACK other events so device keeps sending
                                sendAck();
                            }
                        }
                    } catch (java.net.SocketTimeoutException ste) {
                        // expected while polling
                    }
                }

                if (listening.get()) {
                    listening.set(false);
                    if (onTimeout != null) {
                        onTimeout.run();
                    }
                }
            } catch (Exception e) {
                listening.set(false);
                if (onError != null) {
                    onError.accept(new FingerprintException("Error while listening: " + e.getMessage(), e));
                }
            }
        });
    }

    @Override
    public void cancelListen() {
        listening.set(false);
        if (listenTask != null) {
            listenTask.cancel(true);
            listenTask = null;
        }
    }

    // ==================== User management (stubs for next step) ====================

    @Override
    public List<DeviceUser> getUsers() throws FingerprintException {
        ensureConnected();
        // TODO: implement CMD_USER_WRQ / data read in next step
        logger.warning("getUsers() not fully implemented yet");
        return new ArrayList<>();
    }

    @Override
    public void createUser(String deviceUserId, String name) throws FingerprintException {
        ensureConnected();
        // TODO: implement user upload in next step
        throw new FingerprintException("createUser not implemented yet – will be added in next step");
    }

    @Override
    public void deleteUser(String deviceUserId) throws FingerprintException {
        ensureConnected();
        // TODO: implement CMD_DELETE_USER in next step
        throw new FingerprintException("deleteUser not implemented yet – will be added in next step");
    }

    @Override
    public void startEnroll(String deviceUserId, int fingerIndex,
                            Consumer<EnrollResult> onFinished,
                            Consumer<FingerprintException> onError) throws FingerprintException {
        ensureConnected();
        // TODO: implement enroll flow in next step
        if (onError != null) {
            onError.accept(new FingerprintException("startEnroll not implemented yet – will be added in next step"));
        }
    }

    @Override
    public DeviceInfo getDeviceInfo() throws FingerprintException {
        ensureConnected();
        // Minimal info; full options read can be added later
        return new DeviceInfo("unknown", "unknown", "ZMM100_TFT", host + ":" + port);
    }

    // ==================== Protocol helpers ====================

    private void ensureConnected() throws FingerprintException {
        if (!isConnected()) {
            throw new FingerprintException("Not connected to device");
        }
    }

    private synchronized byte[] sendCommand(int command, byte[] data) throws IOException {
        replyNumber = (replyNumber + 1) & 0xFFFF;
        byte[] packet = buildPacket(command, sessionId, replyNumber, data);
        out.write(packet);
        out.flush();
        return readPacket();
    }

    private synchronized void sendAck() {
        try {
            byte[] packet = buildPacket(CMD_ACK_OK, sessionId, 0, new byte[0]);
            out.write(packet);
            out.flush();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to send ACK", e);
        }
    }

    private byte[] buildPacket(int command, int session, int replyId, byte[] data) {
        int payloadSize = 8 + (data != null ? data.length : 0);
        ByteBuffer buf = ByteBuffer.allocate(8 + payloadSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(PACKET_START);
        buf.putInt(payloadSize);

        // payload header without checksum first
        ByteBuffer payload = ByteBuffer.allocate(payloadSize);
        payload.order(ByteOrder.LITTLE_ENDIAN);
        payload.putShort((short) command);
        payload.putShort((short) 0); // checksum placeholder
        payload.putShort((short) session);
        payload.putShort((short) replyId);
        if (data != null && data.length > 0) {
            payload.put(data);
        }

        byte[] payloadBytes = payload.array();
        int checksum = calculateChecksum(payloadBytes);
        payloadBytes[2] = (byte) (checksum & 0xFF);
        payloadBytes[3] = (byte) ((checksum >> 8) & 0xFF);

        buf.put(payloadBytes);
        return buf.array();
    }

    /**
     * ZK checksum: sum of payload as little-endian shorts, then bitwise adjustments.
     * Simplified version compatible with most devices.
     */
    private int calculateChecksum(byte[] payload) {
        // Zero checksum field before calculating
        int sum = 0;
        for (int i = 0; i < payload.length; i += 2) {
            int low = payload[i] & 0xFF;
            int high = (i + 1 < payload.length) ? (payload[i + 1] & 0xFF) : 0;
            // skip the checksum bytes themselves (index 2,3)
            if (i == 2) continue;
            sum += (high << 8) | low;
            sum &= 0xFFFF;
        }
        sum = ~sum & 0xFFFF;
        // Some firmwares use a slightly different algorithm; if CONNECT fails,
        // we may need to adjust. For many ZMM100 devices this works.
        return (sum + 1) & 0xFFFF;
    }

    private byte[] readPacket() throws IOException {
        // Read start + size (8 bytes)
        byte[] header = readFully(8);
        if (header == null) return null;

        if (header[0] != 0x50 || header[1] != 0x50) {
            logger.warning("Invalid packet start");
            return null;
        }

        int payloadSize = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (payloadSize < 8 || payloadSize > 65535) {
            logger.warning("Invalid payload size: " + payloadSize);
            return null;
        }

        byte[] payload = readFully(payloadSize);
        if (payload == null) return null;

        // Return full packet (header + payload) for helpers that expect it
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
        // payload starts at offset 8
        return ByteBuffer.wrap(fullPacket, 8, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
    }

    private int getSessionId(byte[] fullPacket) {
        return ByteBuffer.wrap(fullPacket, 12, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
    }

    /**
     * Parse EF_ATTLOG realtime payload.
     * Layout (after 8-byte payload header):
     *   user id string 9 bytes
     *   zeros 15 bytes
     *   verify type 2 bytes LE
     *   time 6 bytes: Y-2000, M, D, H, M, S
     */
    private VerificationResult parseAttLog(byte[] fullPacket) {
        try {
            // data starts at offset 16 (8 packet header + 8 payload header)
            int dataOffset = 16;
            if (fullPacket.length < dataOffset + 32) {
                return null;
            }

            // user id: 9 bytes null-terminated string
            int end = dataOffset;
            while (end < dataOffset + 9 && fullPacket[end] != 0) end++;
            String userId = new String(fullPacket, dataOffset, end - dataOffset, StandardCharsets.US_ASCII).trim();
            if (userId.isEmpty()) return null;

            int verifyType = ByteBuffer.wrap(fullPacket, dataOffset + 24, 2)
                    .order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;

            int y = (fullPacket[dataOffset + 26] & 0xFF) + 2000;
            int mo = fullPacket[dataOffset + 27] & 0xFF;
            int d = fullPacket[dataOffset + 28] & 0xFF;
            int h = fullPacket[dataOffset + 29] & 0xFF;
            int mi = fullPacket[dataOffset + 30] & 0xFF;
            int s = fullPacket[dataOffset + 31] & 0xFF;

            LocalDateTime time;
            try {
                time = LocalDateTime.of(y, mo, d, h, mi, s);
            } catch (Exception e) {
                time = LocalDateTime.now();
            }

            return new VerificationResult(userId, time, verifyType);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse ATTLOG", e);
            return null;
        }
    }

    private void closeQuietly() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        in = null;
        out = null;
        socket = null;
    }
}

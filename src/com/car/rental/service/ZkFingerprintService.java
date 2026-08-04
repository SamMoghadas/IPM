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
 * ZKTeco TCP client (port 4370).
 * CONNECT packet verified against real pyzk capture:
 * 50 50 82 7D 08 00 00 00 E8 03 17 FC 00 00 00 00
 */
public class ZkFingerprintService implements FingerprintService {

    private static final Logger logger = Logger.getLogger(ZkFingerprintService.class.getName());

    private static final int CMD_CONNECT = 1000;
    private static final int CMD_EXIT = 1001;
    private static final int CMD_ACK_OK = 2000;
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
    /** Next reply_id to send. First CONNECT must use 0 (matches pyzk). */
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
            replyNumber = 0; // pyzk first CONNECT uses reply_id = 0

            byte[] reply = sendCommand(CMD_CONNECT, new byte[0]);
            if (reply == null) {
                closeQuietly();
                throw new FingerprintException("No reply from device on CONNECT");
            }
            int cmd = getCommand(reply);
            if (cmd != CMD_ACK_OK) {
                closeQuietly();
                throw new FingerprintException("Device rejected CONNECT, cmd=" + cmd);
            }
            sessionId = getSessionId(reply);
            connected.set(true);
            logger.info("Connected to ZK device " + host + ":" + port + " session=" + sessionId);
            System.out.println("Connected! session=" + sessionId);
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
        }
        closeQuietly();
        connected.set(false);
        logger.info("Disconnected from ZK device");
    }

    @Override
    public boolean isConnected() {
        return connected.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

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
                byte[] regData = new byte[]{(byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
                byte[] regReply = sendCommand(CMD_REG_EVENT, regData);
                if (regReply == null || getCommand(regReply) != CMD_ACK_OK) {
                    throw new FingerprintException("Failed to register realtime events");
                }

                long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
                socket.setSoTimeout(1000);

                while (listening.get() && System.currentTimeMillis() < deadline) {
                    try {
                        byte[] packet = readPacket();
                        if (packet == null) continue;

                        int cmd = getCommand(packet);
                        if (cmd == CMD_REG_EVENT) {
                            int eventCode = getSessionId(packet);
                            if (eventCode == EF_ATTLOG) {
                                VerificationResult result = parseAttLog(packet);
                                if (result != null) {
                                    sendAck();
                                    listening.set(false);
                                    if (onVerified != null) {
                                        onVerified.accept(result);
                                    }
                                    return;
                                }
                            } else {
                                sendAck();
                            }
                        }
                    } catch (java.net.SocketTimeoutException ste) {
                        // poll
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

    @Override
    public List<DeviceUser> getUsers() throws FingerprintException {
        ensureConnected();
        return new ArrayList<>();
    }

    @Override
    public void createUser(String deviceUserId, String name) throws FingerprintException {
        ensureConnected();
        throw new FingerprintException("createUser not implemented yet");
    }

    @Override
    public void deleteUser(String deviceUserId) throws FingerprintException {
        ensureConnected();
        throw new FingerprintException("deleteUser not implemented yet");
    }

    @Override
    public void startEnroll(String deviceUserId, int fingerIndex,
                            Consumer<EnrollResult> onFinished,
                            Consumer<FingerprintException> onError) throws FingerprintException {
        ensureConnected();
        if (onError != null) {
            onError.accept(new FingerprintException("startEnroll not implemented yet"));
        }
    }

    @Override
    public DeviceInfo getDeviceInfo() throws FingerprintException {
        ensureConnected();
        return new DeviceInfo("unknown", "unknown", "ZMM100_TFT", host + ":" + port);
    }

    private void ensureConnected() throws FingerprintException {
        if (!isConnected()) {
            throw new FingerprintException("Not connected to device");
        }
    }

    private synchronized byte[] sendCommand(int command, byte[] data) throws IOException {
        int replyId = replyNumber & 0xFFFF;
        replyNumber = (replyNumber + 1) & 0xFFFF;

        byte[] packet = buildPacket(command, sessionId, replyId, data);
        System.out.println("JAVA SEND: " + toHex(packet));
        out.write(packet);
        out.flush();

        byte[] reply = readPacket();
        if (reply != null) {
            System.out.println("JAVA RECV: " + toHex(reply));
        } else {
            System.out.println("JAVA RECV: null");
        }
        return reply;
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
        int dataLen = (data != null) ? data.length : 0;
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

    /**
     * Checksum matching pyzk (USHRT_MAX = 65535 style folding).
     */
    private static int createChecksum(byte[] payload) {
        int chksum = 0;
        int size = payload.length;
        for (int i = 0; i < size; i += 2) {
            if (i == size - 1) {
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
            logger.warning("Invalid packet start: " + toHex(header));
            return null;
        }

        int payloadSize = (header[4] & 0xFF)
                | ((header[5] & 0xFF) << 8)
                | ((header[6] & 0xFF) << 16)
                | ((header[7] & 0xFF) << 24);

        if (payloadSize < 8 || payloadSize > 1024 * 1024) {
            logger.warning("Invalid payload size: " + payloadSize);
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

    private VerificationResult parseAttLog(byte[] fullPacket) {
        try {
            int dataOffset = 16;
            if (fullPacket.length < dataOffset + 32) {
                return null;
            }

            int end = dataOffset;
            while (end < dataOffset + 9 && fullPacket[end] != 0) end++;
            String userId = new String(fullPacket, dataOffset, end - dataOffset, StandardCharsets.US_ASCII).trim();
            if (userId.isEmpty()) return null;

            int verifyType = (fullPacket[dataOffset + 24] & 0xFF)
                    | ((fullPacket[dataOffset + 25] & 0xFF) << 8);

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

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(String.format("%02X", data[i]));
        }
        return sb.toString();
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

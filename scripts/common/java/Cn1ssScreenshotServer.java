import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/// Host-side WebSocket server that replaces the base64-over-stdout chunk
/// pipeline. Listens on `--port <n>` (0 = ephemeral), prints the bound port
/// on the first stdout line so the runner script can capture it, then
/// accepts WebSocket connections from the device under test. For each
/// screenshot the device sends a META text frame followed by a binary
/// frame containing the PNG bytes; the server writes the PNG to
/// `<out>/<safeName>.png` and echoes back an `ACK <safeName>` text frame so
/// the device knows it can move on to the next test.
///
/// No base64. No chunking. No fixed inter-test delays. The device unblocks
/// as soon as the bytes hit the host disk.
public class Cn1ssScreenshotServer {

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int OP_CONTINUATION = 0x0;
    private static final int OP_TEXT = 0x1;
    private static final int OP_BINARY = 0x2;
    private static final int OP_CLOSE = 0x8;
    private static final int OP_PING = 0x9;
    private static final int OP_PONG = 0xA;

    /// Map of png_fnv1a64 → first test name that produced it. Cross-test
    /// duplicates surface as `CN1SS:WARN:duplicate_image_with=...` lines
    /// (same diagnostic the device used to emit, now centralized).
    private static final ConcurrentHashMap<String, String> hashRegistry =
            new ConcurrentHashMap<String, String>();
    private static final AtomicInteger receivedCount = new AtomicInteger();
    private static Path outDir;

    public static void main(String[] args) throws Exception {
        int port = 0;
        outDir = Paths.get("cn1ss-out");
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i])) {
                port = Integer.parseInt(args[++i]);
            } else if ("--out".equals(args[i])) {
                outDir = Paths.get(args[++i]);
            }
        }
        Files.createDirectories(outDir);
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", port));
        int boundPort = serverSocket.getLocalPort();
        System.out.println("CN1SS_SERVER_PORT=" + boundPort);
        System.out.flush();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }));

        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                Thread t = new Thread(() -> handle(client), "Cn1ss-conn");
                t.setDaemon(true);
                t.start();
            } catch (IOException ex) {
                if (!serverSocket.isClosed()) {
                    System.err.println("[Cn1ssScreenshotServer] accept failed: " + ex);
                }
            }
        }
    }

    private static void handle(Socket s) {
        try {
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            if (!performHandshake(in, out)) {
                s.close();
                return;
            }
            serve(in, out);
        } catch (Exception ex) {
            System.err.println("[Cn1ssScreenshotServer] connection error: " + ex);
        } finally {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static boolean performHandshake(InputStream in, OutputStream out) throws Exception {
        String statusLine = readHeaderLine(in);
        if (statusLine == null) {
            return false;
        }
        Map<String, String> headers = new HashMap<String, String>();
        while (true) {
            String line = readHeaderLine(in);
            if (line == null || line.isEmpty()) {
                break;
            }
            int colon = line.indexOf(':');
            if (colon > 0) {
                headers.put(line.substring(0, colon).trim().toLowerCase(Locale.ROOT),
                        line.substring(colon + 1).trim());
            }
        }
        String key = headers.get("sec-websocket-key");
        if (key == null) {
            return false;
        }
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        String accept = Base64.getEncoder().encodeToString(
                sha1.digest((key + GUID).getBytes(StandardCharsets.ISO_8859_1)));
        String resp = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
        out.write(resp.getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
        return true;
    }

    private static String readHeaderLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(64);
        int prev = -1;
        while (true) {
            int b = in.read();
            if (b < 0) {
                if (buf.size() == 0) {
                    return null;
                }
                throw new EOFException("EOF in header");
            }
            if (prev == '\r' && b == '\n') {
                byte[] bytes = buf.toByteArray();
                return new String(bytes, 0, bytes.length - 1, StandardCharsets.ISO_8859_1);
            }
            buf.write(b);
            prev = b;
        }
    }

    private static void serve(InputStream in, OutputStream out) throws IOException {
        DataInputStream dis = new DataInputStream(in);
        String pendingMetaTest = null;
        long pendingMetaBytes = -1;
        String pendingMetaHash = null;
        ByteArrayOutputStream fragmentBuffer = null;
        int fragmentOpcode = -1;

        while (true) {
            int b1 = dis.read();
            if (b1 < 0) {
                return;
            }
            int b2 = dis.read();
            if (b2 < 0) {
                return;
            }
            boolean fin = (b1 & 0x80) != 0;
            int opcode = b1 & 0x0F;
            boolean masked = (b2 & 0x80) != 0;
            long len = b2 & 0x7F;
            if (len == 126) {
                len = dis.readUnsignedShort();
            } else if (len == 127) {
                len = dis.readLong();
            }
            byte[] mask = null;
            if (masked) {
                mask = new byte[4];
                dis.readFully(mask);
            }
            if (len > Integer.MAX_VALUE) {
                throw new IOException("payload too large");
            }
            byte[] payload = new byte[(int) len];
            dis.readFully(payload);
            if (masked) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] = (byte) (payload[i] ^ mask[i & 3]);
                }
            }

            int effectiveOp = opcode;
            byte[] effectivePayload = payload;
            if (opcode == OP_CONTINUATION) {
                if (fragmentBuffer == null) {
                    return;
                }
                fragmentBuffer.write(payload);
                if (!fin) {
                    continue;
                }
                effectiveOp = fragmentOpcode;
                effectivePayload = fragmentBuffer.toByteArray();
                fragmentBuffer = null;
                fragmentOpcode = -1;
            } else if (opcode == OP_TEXT || opcode == OP_BINARY) {
                if (!fin) {
                    fragmentBuffer = new ByteArrayOutputStream();
                    fragmentBuffer.write(payload);
                    fragmentOpcode = opcode;
                    continue;
                }
            }

            switch (effectiveOp) {
                case OP_TEXT: {
                    String text = new String(effectivePayload, StandardCharsets.UTF_8);
                    if (text.startsWith("META ")) {
                        Map<String, String> meta = parseMeta(text.substring(5));
                        pendingMetaTest = meta.get("test");
                        String bytesStr = meta.get("png_bytes");
                        pendingMetaBytes = bytesStr == null ? -1 : Long.parseLong(bytesStr);
                        pendingMetaHash = meta.get("png_fnv1a64");
                    }
                    break;
                }
                case OP_BINARY: {
                    if (pendingMetaTest == null) {
                        System.err.println("[Cn1ssScreenshotServer] binary frame without META; dropping " + effectivePayload.length + " bytes");
                        break;
                    }
                    writeAndAck(pendingMetaTest, pendingMetaBytes, pendingMetaHash, effectivePayload, out);
                    pendingMetaTest = null;
                    pendingMetaBytes = -1;
                    pendingMetaHash = null;
                    break;
                }
                case OP_PING: {
                    writeServerFrame(out, OP_PONG, effectivePayload);
                    break;
                }
                case OP_PONG: {
                    break;
                }
                case OP_CLOSE: {
                    writeServerFrame(out, OP_CLOSE, effectivePayload);
                    return;
                }
                default: {
                    return;
                }
            }
        }
    }

    private static void writeAndAck(String safeName, long expectedBytes, String expectedHash,
                                    byte[] pngBytes, OutputStream out) throws IOException {
        String status = "ok";
        StringBuilder warn = new StringBuilder();
        if (expectedBytes >= 0 && expectedBytes != pngBytes.length) {
            status = "length_mismatch";
            warn.append("expected=").append(expectedBytes).append(",got=").append(pngBytes.length);
        }
        String actualHash = fnv1a64Hex(pngBytes);
        if (expectedHash != null && !expectedHash.equals(actualHash)) {
            if (status.equals("ok")) {
                status = "hash_mismatch";
            }
            if (warn.length() > 0) {
                warn.append(';');
            }
            warn.append("expected_hash=").append(expectedHash).append(",actual_hash=").append(actualHash);
        }
        String prev = hashRegistry.putIfAbsent(actualHash, safeName);
        if (prev != null && !prev.equals(safeName)) {
            System.out.println("CN1SS:WARN:test=" + safeName
                    + " duplicate_image_with=" + prev + " png_fnv1a64=" + actualHash);
        }

        Path target = outDir.resolve(safeName + ".png");
        Files.write(target, pngBytes);
        int count = receivedCount.incrementAndGet();
        System.out.println("CN1SS:INFO:test=" + safeName + " png_bytes=" + pngBytes.length
                + " png_fnv1a64=" + actualHash + " status=" + status
                + (warn.length() == 0 ? "" : " warn=" + warn));
        String ack = "ACK " + safeName + " status=" + status;
        writeServerFrame(out, OP_TEXT, ack.getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, String> parseMeta(String json) {
        Map<String, String> result = new HashMap<String, String>();
        String s = json.trim();
        if (s.startsWith("{")) {
            s = s.substring(1);
        }
        if (s.endsWith("}")) {
            s = s.substring(0, s.length() - 1);
        }
        // Tiny JSON parser sufficient for the META frame shape produced by
        // the device helper (string keys, string or integer values, no
        // nested objects). Avoids pulling in a JSON library for this
        // single-line use.
        int i = 0;
        int n = s.length();
        while (i < n) {
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= n) break;
            if (s.charAt(i) != '"') break;
            int keyStart = ++i;
            while (i < n && s.charAt(i) != '"') i++;
            String key = s.substring(keyStart, i);
            i++;
            while (i < n && (s.charAt(i) == ':' || Character.isWhitespace(s.charAt(i)))) i++;
            String value;
            if (i < n && s.charAt(i) == '"') {
                int valStart = ++i;
                while (i < n && s.charAt(i) != '"') i++;
                value = s.substring(valStart, i);
                i++;
            } else {
                int valStart = i;
                while (i < n && s.charAt(i) != ',' && !Character.isWhitespace(s.charAt(i))) i++;
                value = s.substring(valStart, i);
            }
            result.put(key, value);
            while (i < n && (s.charAt(i) == ',' || Character.isWhitespace(s.charAt(i)))) i++;
        }
        return result;
    }

    private static void writeServerFrame(OutputStream out, int opcode, byte[] payload) throws IOException {
        synchronized (out) {
            out.write(0x80 | (opcode & 0x0F));
            int len = payload.length;
            if (len <= 125) {
                out.write(len);
            } else if (len <= 0xFFFF) {
                out.write(126);
                out.write((len >>> 8) & 0xFF);
                out.write(len & 0xFF);
            } else {
                out.write(127);
                for (int i = 7; i >= 0; i--) {
                    out.write((int) (((long) len >>> (i * 8)) & 0xFF));
                }
            }
            out.write(payload);
            out.flush();
        }
    }

    static String fnv1a64Hex(byte[] bytes) {
        long h = 0xcbf29ce484222325L;
        long prime = 0x100000001b3L;
        for (int i = 0; i < bytes.length; i++) {
            h ^= bytes[i] & 0xff;
            h *= prime;
        }
        StringBuilder sb = new StringBuilder(16);
        for (int i = 60; i >= 0; i -= 4) {
            int nib = (int) ((h >>> i) & 0xf);
            sb.append((char) (nib < 10 ? '0' + nib : 'a' + (nib - 10)));
        }
        return sb.toString();
    }
}

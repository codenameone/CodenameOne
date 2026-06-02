package com.codename1.impl.javase;

import com.codename1.impl.WebSocketEventSink;
import com.codename1.io.WebSocketState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/// Round-trip test for the hand-rolled JavaSE WebSocket client against a
/// minimal in-process RFC 6455 echo server. Validates the handshake, text +
/// binary frames, fragmentation handling, and the close round-trip.
///
/// Tests the impl directly via its `WebSocketImpl` interface rather than
/// going through the public `com.codename1.io.WebSocket` facade -- the
/// facade requires a full `CodenameOneImplementation` instance and we
/// don't want to spin up `JavaSEPort` (with its AWT / Swing wiring) for a
/// loopback network test.
///
/// Avoids lambdas because the JavaSE port module is at -source 1.7.
class JavaSEWebSocketImplTest {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Charset ASCII = Charset.forName("ISO-8859-1");

    private EchoServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new EchoServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.stop();
    }

    @Test
    void roundTripsTextAndBinaryFrames() throws Exception {
        final CountDownLatch connected = new CountDownLatch(1);
        final List<String> texts = new CopyOnWriteArrayList<String>();
        final List<byte[]> binaries = new CopyOnWriteArrayList<byte[]>();
        final AtomicReference<Exception> errored = new AtomicReference<Exception>();
        final CountDownLatch closed = new CountDownLatch(1);
        final AtomicInteger closeCode = new AtomicInteger();

        JavaSEWebSocketImpl impl = new JavaSEWebSocketImpl(
                "ws://127.0.0.1:" + server.port() + "/");
        impl.setEventSink(new WebSocketEventSink() {
            @Override
            public void onConnect() {
                connected.countDown();
            }

            @Override
            public void onTextMessage(String message) {
                texts.add(message);
            }

            @Override
            public void onBinaryMessage(byte[] message) {
                binaries.add(message);
            }

            @Override
            public void onClose(int code, String reason) {
                closeCode.set(code);
                closed.countDown();
            }

            @Override
            public void onError(Exception ex) {
                errored.set(ex);
            }
        });
        impl.connect(0);

        org.junit.jupiter.api.Assertions.assertTrue(
                connected.await(3, TimeUnit.SECONDS), "handshake completes");
        org.junit.jupiter.api.Assertions.assertEquals(WebSocketState.OPEN, impl.getReadyState());

        impl.sendText("hello");
        byte[] bin = new byte[] { 1, 2, 3, 4, 5 };
        impl.sendBinary(bin);

        long deadline = System.currentTimeMillis() + 3000;
        while ((texts.size() == 0 || binaries.size() == 0)
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        org.junit.jupiter.api.Assertions.assertEquals("hello", texts.get(0));
        org.junit.jupiter.api.Assertions.assertArrayEquals(bin, binaries.get(0));

        impl.close();
        org.junit.jupiter.api.Assertions.assertTrue(
                closed.await(3, TimeUnit.SECONDS), "close round-trip completes");
        org.junit.jupiter.api.Assertions.assertEquals(1000, closeCode.get());
        org.junit.jupiter.api.Assertions.assertEquals(WebSocketState.CLOSED, impl.getReadyState());
        org.junit.jupiter.api.Assertions.assertNull(errored.get(),
                "no errors during normal session");
    }

    @Test
    void negotiatesSubprotocol() throws Exception {
        final CountDownLatch connected = new CountDownLatch(1);
        final AtomicReference<Exception> errored = new AtomicReference<Exception>();

        JavaSEWebSocketImpl impl = new JavaSEWebSocketImpl(
                "ws://127.0.0.1:" + server.port() + "/");
        impl.setRequestedSubprotocols(new String[] { "graphql-transport-ws", "chat" });
        impl.setEventSink(new WebSocketEventSink() {
            @Override
            public void onConnect() {
                connected.countDown();
            }

            @Override
            public void onTextMessage(String message) {
            }

            @Override
            public void onBinaryMessage(byte[] message) {
            }

            @Override
            public void onClose(int code, String reason) {
            }

            @Override
            public void onError(Exception ex) {
                errored.set(ex);
            }
        });
        impl.connect(0);

        org.junit.jupiter.api.Assertions.assertTrue(
                connected.await(3, TimeUnit.SECONDS), "handshake completes");
        // The client offered both protocols in the Sec-WebSocket-Protocol header...
        org.junit.jupiter.api.Assertions.assertEquals("graphql-transport-ws, chat",
                server.lastRequestedProtocols);
        // ...and surfaces the server's selection.
        org.junit.jupiter.api.Assertions.assertEquals("graphql-transport-ws",
                impl.getSelectedSubprotocol());
        org.junit.jupiter.api.Assertions.assertNull(errored.get());
        impl.close();
    }

    @Test
    void largePayloadSurvivesFragmentation() throws Exception {
        final CountDownLatch connected = new CountDownLatch(1);
        final List<byte[]> received = new CopyOnWriteArrayList<byte[]>();

        JavaSEWebSocketImpl impl = new JavaSEWebSocketImpl(
                "ws://127.0.0.1:" + server.port() + "/");
        impl.setEventSink(new WebSocketEventSink() {
            @Override
            public void onConnect() {
                connected.countDown();
            }

            @Override
            public void onTextMessage(String message) {
            }

            @Override
            public void onBinaryMessage(byte[] message) {
                received.add(message);
            }

            @Override
            public void onClose(int code, String reason) {
            }

            @Override
            public void onError(Exception ex) {
            }
        });
        impl.connect(0);
        org.junit.jupiter.api.Assertions.assertTrue(connected.await(3, TimeUnit.SECONDS));

        // 200 KB triggers the MAX_FRAME_PAYLOAD fragmenter (65 KB).
        byte[] big = new byte[200_000];
        for (int i = 0; i < big.length; i++) {
            big[i] = (byte) (i & 0xFF);
        }
        impl.sendBinary(big);

        long deadline = System.currentTimeMillis() + 5000;
        while (received.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        org.junit.jupiter.api.Assertions.assertArrayEquals(big, received.get(0));
        impl.close();
    }

    /// Tiny RFC 6455 echo server. Single-connection, no TLS, frames are
    /// echoed back as-is (with the mask stripped). Used only by this test.
    private static final class EchoServer {
        private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        private ServerSocket serverSocket;
        private Thread thread;
        private volatile String lastRequestedProtocols;

        void start() throws IOException {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    acceptLoop();
                }
            }, "EchoServer");
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        void stop() throws IOException {
            serverSocket.close();
        }

        private void acceptLoop() {
            try {
                while (!serverSocket.isClosed()) {
                    final Socket client = serverSocket.accept();
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            handle(client);
                        }
                    }, "EchoConn");
                    t.setDaemon(true);
                    t.start();
                }
            } catch (IOException ignored) {
            }
        }

        private void handle(Socket s) {
            try {
                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();
                String statusLine = readHeaderLine(in);
                if (statusLine == null) {
                    return;
                }
                Map<String, String> headers = new HashMap<String, String>();
                while (true) {
                    String line = readHeaderLine(in);
                    if (line == null || line.length() == 0) {
                        break;
                    }
                    int colon = line.indexOf(':');
                    if (colon > 0) {
                        headers.put(line.substring(0, colon).trim().toLowerCase(Locale.ROOT),
                                line.substring(colon + 1).trim());
                    }
                }
                String key = headers.get("sec-websocket-key");
                String requestedProtocols = headers.get("sec-websocket-protocol");
                lastRequestedProtocols = requestedProtocols;
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                String accept = Base64.getEncoder().encodeToString(
                        sha1.digest((key + GUID).getBytes(ASCII)));
                String protocolHeader = "";
                if (requestedProtocols != null && requestedProtocols.length() > 0) {
                    // Negotiate by selecting the first offered subprotocol.
                    String selected = requestedProtocols.split(",")[0].trim();
                    protocolHeader = "Sec-WebSocket-Protocol: " + selected + "\r\n";
                }
                String resp = "HTTP/1.1 101 Switching Protocols\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Sec-WebSocket-Accept: " + accept + "\r\n"
                        + protocolHeader + "\r\n";
                out.write(resp.getBytes(ASCII));
                out.flush();

                echoLoop(in, out);
            } catch (Exception ignored) {
            } finally {
                try {
                    s.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void echoLoop(InputStream in, OutputStream out) throws IOException {
            DataInputStream dis = new DataInputStream(in);
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
                byte[] payload = new byte[(int) len];
                dis.readFully(payload);
                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte) (payload[i] ^ mask[i & 3]);
                    }
                }
                if (opcode == 0x8) {
                    writeFrame(out, 0x8, payload, true);
                    return;
                }
                if (opcode == 0x9) {
                    writeFrame(out, 0xA, payload, true);
                    continue;
                }
                if (opcode == 0xA) {
                    continue;
                }
                if (opcode == 0x0) {
                    if (fragmentBuffer == null) {
                        return;
                    }
                    fragmentBuffer.write(payload);
                    if (fin) {
                        writeFrame(out, fragmentOpcode, fragmentBuffer.toByteArray(), true);
                        fragmentBuffer = null;
                        fragmentOpcode = -1;
                    }
                    continue;
                }
                if (fin) {
                    writeFrame(out, opcode, payload, true);
                } else {
                    fragmentBuffer = new ByteArrayOutputStream();
                    fragmentBuffer.write(payload);
                    fragmentOpcode = opcode;
                }
            }
        }

        private static void writeFrame(OutputStream out, int opcode, byte[] payload,
                                       boolean fin) throws IOException {
            out.write((fin ? 0x80 : 0) | (opcode & 0x0F));
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

        private static String readHeaderLine(InputStream in) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(64);
            int prev = -1;
            while (true) {
                int b = in.read();
                if (b < 0) {
                    return null;
                }
                if (prev == '\r' && b == '\n') {
                    byte[] bytes = buf.toByteArray();
                    return new String(bytes, 0, bytes.length - 1, ASCII);
                }
                buf.write(b);
                prev = b;
            }
        }
    }
}

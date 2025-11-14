package com.codename1.io;

import com.codename1.junit.EdtTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation.TestSocket;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SocketTest extends UITestBase {

    @Override
    protected void setUpDisplay() throws Exception {
        super.setUpDisplay();
        implementation.clearSockets();
        implementation.setSocketAvailable(true);
        implementation.setServerSocketAvailable(true);
        implementation.setHostOrIP("device.local");
    }

    @EdtTest
    void supportedFlagsDelegateToImplementation() {
        implementation.setSocketAvailable(true);
        assertTrue(Socket.isSupported());
        implementation.setSocketAvailable(false);
        assertFalse(Socket.isSupported());

        implementation.setServerSocketAvailable(true);
        assertTrue(Socket.isServerSocketSupported());
    }

    @EdtTest
    void connectRejectsHostsContainingPort() {
        SocketConnection connection = new SocketConnection() {
            public void connectionError(int errorCode, String message) {
            }

            public void connectionEstablished(InputStream is, OutputStream os) {
            }
        };
        assertThrows(IllegalArgumentException.class, () -> Socket.connect("example.com:8080", 80, connection));
    }

    @EdtTest
    void connectFailureInvokesErrorCallback() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger errorCode = new AtomicInteger();
        AtomicReference<String> message = new AtomicReference<String>();
        SocketConnection connection = new SocketConnection() {
            public void connectionError(int code, String msg) {
                errorCode.set(code);
                message.set(msg);
                latch.countDown();
            }

            public void connectionEstablished(InputStream is, OutputStream os) {
                fail("Should not connect");
            }
        };

        Socket.connect("unreachable", 8080, connection);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertFalse(connection.isConnected());
        assertEquals(-1, errorCode.get());
        assertEquals("Failed to connect", message.get());
    }

    @EdtTest
    void connectEstablishesStreamsAndAllowsReadWrite() throws Exception {
        TestSocket socket = implementation.registerSocket("example.com", 1234);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<InputStream> inputRef = new AtomicReference<InputStream>();
        AtomicReference<OutputStream> outputRef = new AtomicReference<OutputStream>();

        SocketConnection connection = new SocketConnection() {
            public void connectionError(int errorCode, String message) {
                fail("Unexpected error: " + message);
            }

            public void connectionEstablished(InputStream is, OutputStream os) {
                inputRef.set(is);
                outputRef.set(os);
                latch.countDown();
            }
        };

        Socket.connect("example.com", 1234, connection);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(connection.isConnected());

        socket.enqueue("hi".getBytes(StandardCharsets.UTF_8));
        byte[] buffer = new byte[4];
        int read = inputRef.get().read(buffer);
        assertEquals(2, read);
        assertArrayEquals(new byte[]{'h', 'i'}, Arrays.copyOf(buffer, read));

        outputRef.get().write(new byte[]{1, 2, 3});
        outputRef.get().flush();
        outputRef.get().write(new byte[]{9, 8, 7}, 1, 2);
        outputRef.get().write(255);
        outputRef.get().flush();

        List<byte[]> outbound = socket.getOutboundMessages();
        assertEquals(3, outbound.size());
        assertArrayEquals(new byte[]{1, 2, 3}, outbound.get(0));
        assertArrayEquals(new byte[]{8, 7}, outbound.get(1));
        assertArrayEquals(new byte[]{(byte) 255}, outbound.get(2));

        outputRef.get().close();
        assertFalse(connection.isConnected());
        assertFalse(socket.isConnected());

        assertEquals(-1, inputRef.get().read(new byte[4]));
    }

    @EdtTest
    void connectWithCloseDisconnectsSocket() throws Exception {
        TestSocket socket = implementation.registerSocket("close.me", 9000);
        CountDownLatch latch = new CountDownLatch(1);
        SocketConnection connection = new SocketConnection() {
            public void connectionError(int errorCode, String message) {
                fail();
            }

            public void connectionEstablished(InputStream is, OutputStream os) {
                latch.countDown();
            }
        };

        Socket.Close closeHandle = Socket.connectWithClose("close.me", 9000, connection);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        closeHandle.close();
        assertFalse(socket.isConnected());
    }

    @EdtTest
    void inputStreamCloseDisconnectsSocket() throws Exception {
        TestSocket socket = implementation.registerSocket("input", 1100);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<InputStream> inputRef = new AtomicReference<InputStream>();

        SocketConnection connection = new SocketConnection() {
            public void connectionError(int errorCode, String message) {
                fail();
            }

            public void connectionEstablished(InputStream is, OutputStream os) {
                inputRef.set(is);
                latch.countDown();
            }
        };

        Socket.connect("input", 1100, connection);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        inputRef.get().close();
        assertFalse(socket.isConnected());
    }

    @EdtTest
    void getHostOrIpDelegatesToImplementation() {
        implementation.setHostOrIP("device.local");
        assertEquals("device.local", Socket.getHostOrIP());
    }
}

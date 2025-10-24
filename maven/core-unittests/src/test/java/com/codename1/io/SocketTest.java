package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class SocketTest {
    private CodenameOneImplementation implementation;

    @BeforeEach
    void setUp() {
        implementation = TestImplementationProvider.installImplementation(true);
        Display.getInstance();
    }

    @AfterEach
    void tearDown() {
        TestImplementationProvider.resetImplementation();
    }

    @Test
    void supportedFlagsDelegateToImplementation() {
        when(implementation.isSocketAvailable()).thenReturn(true);
        assertTrue(Socket.isSupported());
        when(implementation.isSocketAvailable()).thenReturn(false);
        assertFalse(Socket.isSupported());

        when(implementation.isServerSocketAvailable()).thenReturn(true);
        assertTrue(Socket.isServerSocketSupported());
    }

    @Test
    void connectRejectsHostsContainingPort() {
        SocketConnection connection = new SocketConnection() {
            public void connectionError(int errorCode, String message) {
            }

            public void connectionEstablished(InputStream is, OutputStream os) {
            }
        };
        assertThrows(IllegalArgumentException.class, () -> Socket.connect("example.com:8080", 80, connection));
    }

    @Test
    void connectFailureInvokesErrorCallback() throws InterruptedException {
        when(implementation.connectSocket(anyString(), anyInt(), anyInt())).thenReturn(null);
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

    @Test
    void connectEstablishesStreamsAndAllowsReadWrite() throws Exception {
        FakeSocketState state = prepareSocketState("example.com", 1234);
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

        state.enqueue("hi".getBytes(StandardCharsets.UTF_8));
        byte[] buffer = new byte[4];
        int read = inputRef.get().read(buffer);
        assertEquals(2, read);
        assertArrayEquals(new byte[]{'h', 'i'}, Arrays.copyOf(buffer, read));

        outputRef.get().write(new byte[]{1, 2, 3});
        outputRef.get().flush();
        assertEquals(1, state.outbound.size());
        assertArrayEquals(new byte[]{1, 2, 3}, state.outbound.get(0));

        outputRef.get().write(new byte[]{9, 8, 7}, 1, 2);
        outputRef.get().write(255);
        outputRef.get().flush();
        assertEquals(3, state.outbound.size());
        assertArrayEquals(new byte[]{8, 7}, state.outbound.get(1));
        assertArrayEquals(new byte[]{(byte) 255}, state.outbound.get(2));

        outputRef.get().close();
        assertFalse(connection.isConnected());
        assertFalse(state.connected);

        assertEquals(-1, inputRef.get().read(new byte[4]));
    }

    @Test
    void connectWithCloseDisconnectsSocket() throws Exception {
        FakeSocketState state = prepareSocketState("close.me", 9000);
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
        assertFalse(state.connected);
    }

    @Test
    void inputStreamCloseDisconnectsSocket() throws Exception {
        FakeSocketState state = prepareSocketState("input", 1100);
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
        assertFalse(state.connected);
    }

    @Test
    void getHostOrIpDelegatesToImplementation() {
        when(implementation.getHostOrIP()).thenReturn("device.local");
        assertEquals("device.local", Socket.getHostOrIP());
    }

    private FakeSocketState prepareSocketState(String host, int port) {
        FakeSocketState state = new FakeSocketState();
        when(implementation.isSocketAvailable()).thenReturn(true);
        when(implementation.connectSocket(eq(host), eq(port), anyInt())).thenReturn(state);
        when(implementation.isSocketConnected(state)).thenAnswer(invocation -> state.connected);
        when(implementation.getSocketAvailableInput(state)).thenAnswer(invocation -> state.available());
        when(implementation.readFromSocketStream(state)).thenAnswer(invocation -> state.read());
        when(implementation.getSocketErrorCode(state)).thenReturn(0);
        when(implementation.getSocketErrorMessage(state)).thenReturn(null);

        doAnswer(invocation -> {
            state.write(invocation.getArgument(1));
            return null;
        }).when(implementation).writeToSocketStream(eq(state), any(byte[].class));

        doAnswer(invocation -> {
            state.connected = false;
            return null;
        }).when(implementation).disconnectSocket(eq(state));

        return state;
    }

    private static class FakeSocketState {
        private final ConcurrentLinkedQueue<byte[]> inbound = new ConcurrentLinkedQueue<byte[]>();
        private final List<byte[]> outbound = new ArrayList<byte[]>();
        private volatile boolean connected = true;

        void enqueue(byte[] data) {
            inbound.add(data);
        }

        int available() {
            int total = 0;
            for (byte[] bytes : inbound) {
                total += bytes.length;
            }
            return total;
        }

        byte[] read() {
            byte[] data = inbound.poll();
            if (data == null) {
                return new byte[0];
            }
            return data;
        }

        void write(byte[] data) {
            outbound.add(Arrays.copyOf(data, data.length));
        }
    }
}

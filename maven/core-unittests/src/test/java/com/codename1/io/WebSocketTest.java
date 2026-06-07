package com.codename1.io;

import com.codename1.impl.WebSocketEventSink;
import com.codename1.impl.WebSocketImpl;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Wires a mock `WebSocketImpl` into the public facade and verifies that
/// fluent handler setters route to the right user-registered handler with
/// the `WebSocket` instance threaded through as the first argument.
class WebSocketTest {

    private MockImpl mockImpl;

    @BeforeEach
    void setUp() {
        WebSocketingImpl impl = new WebSocketingImpl();
        Util.setImplementation(impl);
        this.mockImpl = null; // populated lazily inside createWebSocketImpl
    }

    @Test
    void connectAndTextMessageRouteToHandlers() {
        List<String> messages = new ArrayList<>();
        AtomicReference<WebSocket> seenInOpen = new AtomicReference<>();
        AtomicInteger connectCount = new AtomicInteger();

        WebSocket ws = WebSocket.build("ws://test")
                .onConnect(w -> {
                    connectCount.incrementAndGet();
                    seenInOpen.set(w);
                })
                .onTextMessage((w, m) -> messages.add(m))
                .connect();

        // Drive the mock impl through the lifecycle the real platform would.
        mockImpl.testSink().onConnect();
        mockImpl.testSink().onTextMessage("hello");
        mockImpl.testSink().onTextMessage("world");

        assertEquals(1, connectCount.get());
        assertSame(ws, seenInOpen.get(), "handler receives the WebSocket instance");
        assertEquals(Arrays.asList("hello", "world"), messages);
    }

    @Test
    void subprotocolsAreOfferedAndSelectionSurfaced() {
        WebSocket ws = WebSocket.build("ws://test")
                .subprotocols("graphql-transport-ws", "chat")
                .connect();

        // The facade hands the offered list to the impl before connect.
        assertArrayEquals(new String[] { "graphql-transport-ws", "chat" },
                mockImpl.testRequestedSubprotocols());

        // The platform records the server's pick before firing onConnect;
        // the facade surfaces it.
        mockImpl.testSelect("graphql-transport-ws");
        mockImpl.testSink().onConnect();
        assertEquals("graphql-transport-ws", ws.getSelectedSubprotocol());
    }

    @Test
    void binaryHandlerReceivesBytes() {
        List<byte[]> received = new ArrayList<>();
        WebSocket ws = WebSocket.build("ws://test")
                .onBinaryMessage((w, b) -> received.add(b))
                .connect();

        byte[] payload = new byte[] { 1, 2, 3, 4 };
        mockImpl.testSink().onBinaryMessage(payload);

        assertEquals(1, received.size());
        assertSame(payload, received.get(0));
    }

    @Test
    void closeHandlerReceivesCodeAndReason() {
        AtomicInteger code = new AtomicInteger();
        AtomicReference<String> reason = new AtomicReference<>();
        WebSocket.build("ws://test")
                .onClose((w, c, r) -> {
                    code.set(c);
                    reason.set(r);
                })
                .connect();

        mockImpl.testSink().onClose(1000, "normal");

        assertEquals(1000, code.get());
        assertEquals("normal", reason.get());
    }

    @Test
    void errorHandlerReceivesException() {
        AtomicReference<Exception> caught = new AtomicReference<>();
        WebSocket.build("ws://test")
                .onError((w, e) -> caught.set(e))
                .connect();

        RuntimeException boom = new RuntimeException("boom");
        mockImpl.testSink().onError(boom);

        assertSame(boom, caught.get());
    }

    @Test
    void unregisteredHandlersAreSilentlyIgnored() {
        WebSocket.build("ws://test").connect();

        // No NPEs even though no handler was registered.
        mockImpl.testSink().onConnect();
        mockImpl.testSink().onTextMessage("ignored");
        mockImpl.testSink().onBinaryMessage(new byte[0]);
        mockImpl.testSink().onClose(1000, "");
        mockImpl.testSink().onError(new RuntimeException("ignored"));
    }

    @Test
    void handlerExceptionsRouteToErrorHandler() {
        AtomicReference<Exception> caught = new AtomicReference<>();
        WebSocket.build("ws://test")
                .onTextMessage((w, m) -> {
                    throw new IllegalArgumentException("bad message");
                })
                .onError((w, e) -> caught.set(e))
                .connect();

        mockImpl.testSink().onTextMessage("x");

        assertTrue(caught.get() instanceof IllegalArgumentException,
                "user-handler throwable surfaces via onError");
        assertEquals("bad message", caught.get().getMessage());
    }

    @Test
    void sendAndCloseDelegateToImpl() {
        WebSocket ws = WebSocket.build("ws://test").connect();

        ws.send("hi");
        ws.send(new byte[] { 9 });
        ws.close();

        assertEquals(Arrays.asList("hi"), mockImpl.textsSent);
        assertEquals(1, mockImpl.binariesSent.size());
        assertEquals((byte) 9, mockImpl.binariesSent.get(0)[0]);
        assertTrue(mockImpl.closed);
    }

    @Test
    void connectForwardsTimeoutToImpl() {
        WebSocket.build("ws://test").connect(7500);

        assertEquals(7500, mockImpl.connectTimeoutMs);
    }

    @Test
    void getReadyStateAndUrlPassThrough() {
        WebSocket ws = WebSocket.build("ws://test/path").connect();

        assertEquals("ws://test/path", ws.getUrl());
        // Default mock state is CONNECTING until we change it.
        assertEquals(WebSocketState.CONNECTING, ws.getReadyState());
        mockImpl.state = WebSocketState.OPEN;
        assertEquals(WebSocketState.OPEN, ws.getReadyState());
    }

    @Test
    void isSupportedDelegatesToImplementation() {
        // The test impl in setUp returns true; flip it for this test.
        WebSocketingImpl impl = new WebSocketingImpl();
        impl.supported = false;
        Util.setImplementation(impl);
        assertEquals(false, WebSocket.isSupported());

        impl.supported = true;
        assertEquals(true, WebSocket.isSupported());
    }

    private final class WebSocketingImpl extends TestCodenameOneImplementation {
        boolean supported = true;

        WebSocketingImpl() {
            super(true);
        }

        @Override
        public boolean isWebSocketSupported() {
            return supported;
        }

        @Override
        public WebSocketImpl createWebSocketImpl(String url) {
            MockImpl m = new MockImpl(url);
            WebSocketTest.this.mockImpl = m;
            return m;
        }
    }

    private static final class MockImpl extends WebSocketImpl {
        final List<String> textsSent = new ArrayList<>();
        final List<byte[]> binariesSent = new ArrayList<>();
        int connectTimeoutMs = -1;
        boolean closed;
        WebSocketState state = WebSocketState.CONNECTING;

        MockImpl(String url) {
            super(url);
        }

        @Override
        public void connect(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void sendText(String message) {
            textsSent.add(message);
        }

        @Override
        public void sendBinary(byte[] message) {
            binariesSent.add(message);
        }

        @Override
        public WebSocketState getReadyState() {
            return state;
        }

        /// Test-only accessor exposing the parent's protected final
        /// {@code sink()} so the test can fire events from the platform
        /// side without going through a real network layer. Named
        /// distinctly from the parent so we're not trying to override
        /// the final method.
        WebSocketEventSink testSink() {
            return super.sink();
        }

        /// Test-only accessors for the subprotocol plumbing on the base.
        String[] testRequestedSubprotocols() {
            return super.requestedSubprotocols();
        }

        void testSelect(String protocol) {
            super.setSelectedSubprotocol(protocol);
        }
    }
}

package com.codename1.io;

import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

class SocketConnectionTest {
    private TestCodenameOneImplementation implementation;

    @BeforeEach
    void setUp() {
        implementation = new TestCodenameOneImplementation(true);
        Util.setImplementation(implementation);
    }

    @Test
    void defaultConstructor() {
        TestSocketConnection socket = new TestSocketConnection();

        assertNotNull(socket);
        assertEquals(0, socket.getConnectTimeout());
        assertFalse(socket.isConnected());
    }

    @Test
    void constructWithTimeout() {
        TestSocketConnection socket = new TestSocketConnection(5000);

        assertEquals(5000, socket.getConnectTimeout());
    }

    @Test
    void setAndGetConnectTimeout() {
        TestSocketConnection socket = new TestSocketConnection();

        socket.setConnectTimeout(3000);

        assertEquals(3000, socket.getConnectTimeout());
    }

    @Test
    void setConnectedUpdatesState() {
        TestSocketConnection socket = new TestSocketConnection();

        assertFalse(socket.isConnected());

        socket.setConnected(true);
        assertTrue(socket.isConnected());

        socket.setConnected(false);
        assertFalse(socket.isConnected());
    }

    @Test
    void connectionEstablished() {
        TestSocketConnection socket = new TestSocketConnection();
        InputStream is = new ByteArrayInputStream(new byte[10]);
        OutputStream os = new ByteArrayOutputStream();

        socket.connectionEstablished(is, os);

        assertTrue(socket.wasConnectionEstablishedCalled());
        assertSame(is, socket.getLastInputStream());
        assertSame(os, socket.getLastOutputStream());
    }

    @Test
    void connectionError() {
        TestSocketConnection socket = new TestSocketConnection();

        socket.connectionError(404, "Not Found");

        assertTrue(socket.wasConnectionErrorCalled());
        assertEquals(404, socket.getLastErrorCode());
        assertEquals("Not Found", socket.getLastErrorMessage());
    }

    @Test
    void inputStreamPreservedByMemberField() {
        TestSocketConnection socket = new TestSocketConnection();
        InputStream is = new ByteArrayInputStream(new byte[10]);
        OutputStream os = new ByteArrayOutputStream();

        socket.connectionEstablished(is, os);

        assertSame(is, socket.input);
        assertSame(os, socket.output);
    }

    @Test
    void timeoutOfZeroMeansInfinite() {
        TestSocketConnection socket = new TestSocketConnection(0);

        assertEquals(0, socket.getConnectTimeout());
    }

    @Test
    void multipleConnectionEstablishedCalls() {
        TestSocketConnection socket = new TestSocketConnection();
        InputStream is1 = new ByteArrayInputStream(new byte[5]);
        OutputStream os1 = new ByteArrayOutputStream();
        InputStream is2 = new ByteArrayInputStream(new byte[10]);
        OutputStream os2 = new ByteArrayOutputStream();

        socket.connectionEstablished(is1, os1);
        socket.connectionEstablished(is2, os2);

        assertSame(is2, socket.getLastInputStream());
        assertSame(os2, socket.getLastOutputStream());
    }

    @Test
    void multipleConnectionErrorCalls() {
        TestSocketConnection socket = new TestSocketConnection();

        socket.connectionError(500, "Error1");
        socket.connectionError(404, "Error2");

        assertEquals(404, socket.getLastErrorCode());
        assertEquals("Error2", socket.getLastErrorMessage());
    }

    private static class TestSocketConnection extends SocketConnection {
        private boolean connectionEstablishedCalled;
        private boolean connectionErrorCalled;
        private InputStream lastInputStream;
        private OutputStream lastOutputStream;
        private int lastErrorCode;
        private String lastErrorMessage;

        public TestSocketConnection() {
            super();
        }

        public TestSocketConnection(int timeout) {
            super(timeout);
        }

        @Override
        public void connectionError(int errorCode, String message) {
            connectionErrorCalled = true;
            lastErrorCode = errorCode;
            lastErrorMessage = message;
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
            connectionEstablishedCalled = true;
            lastInputStream = is;
            lastOutputStream = os;
            this.input = is;
            this.output = os;
        }

        public boolean wasConnectionEstablishedCalled() {
            return connectionEstablishedCalled;
        }

        public boolean wasConnectionErrorCalled() {
            return connectionErrorCalled;
        }

        public InputStream getLastInputStream() {
            return lastInputStream;
        }

        public OutputStream getLastOutputStream() {
            return lastOutputStream;
        }

        public int getLastErrorCode() {
            return lastErrorCode;
        }

        public String getLastErrorMessage() {
            return lastErrorMessage;
        }
    }
}

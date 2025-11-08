package com.codename1.io;

import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.events.ActionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkEventTest {
    private TestCodenameOneImplementation implementation;
    private ConnectionRequest request;

    @BeforeEach
    void setUp() {
        implementation = new TestCodenameOneImplementation(true);
        Util.setImplementation(implementation);
        request = new ConnectionRequest();
    }

    @Test
    void constructExceptionEvent() {
        Exception exception = new IllegalStateException("test error");
        NetworkEvent event = new NetworkEvent(request, exception);

        assertSame(exception, event.getError());
        assertSame(request, event.getConnectionRequest());
        assertEquals(ActionEvent.Type.Exception, event.getEventType());
    }

    @Test
    void constructResponseCodeEvent() {
        NetworkEvent event = new NetworkEvent(request, 404, "Not Found");

        assertEquals(404, event.getResponseCode());
        assertEquals("Not Found", event.getMessage());
        assertEquals(ActionEvent.Type.Response, event.getEventType());
        assertSame(request, event.getConnectionRequest());
    }

    @Test
    void constructProgressEvent() {
        NetworkEvent event = new NetworkEvent(request, NetworkEvent.PROGRESS_TYPE_OUTPUT);

        assertEquals(NetworkEvent.PROGRESS_TYPE_OUTPUT, event.getProgressType());
        assertEquals(ActionEvent.Type.Progress, event.getEventType());
        assertSame(request, event.getConnectionRequest());
    }

    @Test
    void constructDataEvent() {
        Object metaData = new Object();
        NetworkEvent event = new NetworkEvent(request, metaData);

        assertSame(metaData, event.getMetaData());
        assertEquals(ActionEvent.Type.Data, event.getEventType());
        assertSame(request, event.getConnectionRequest());
    }

    @Test
    void dataEventWithNullRequest() {
        NetworkEvent event = new NetworkEvent(null, new Object());

        assertNull(event.getConnectionRequest());
    }

    @Test
    void setAndGetError() {
        NetworkEvent event = new NetworkEvent(request, NetworkEvent.PROGRESS_TYPE_INPUT);
        Exception error = new RuntimeException("test");

        event.setError(error);
        assertSame(error, event.getError());
    }

    @Test
    void setLengthAndGetLength() {
        NetworkEvent event = new NetworkEvent(request, NetworkEvent.PROGRESS_TYPE_OUTPUT);

        assertEquals(-1, event.getLength());

        event.setLength(1024);
        assertEquals(1024, event.getLength());
    }

    @Test
    void setSentReceivedAndGetSentReceived() {
        NetworkEvent event = new NetworkEvent(request, NetworkEvent.PROGRESS_TYPE_INPUT);

        assertEquals(0, event.getSentReceived());

        event.setSentReceived(512);
        assertEquals(512, event.getSentReceived());
    }

    @Test
    void getProgressPercentageWhenLengthUnknown() {
        NetworkEvent event = new NetworkEvent(request, NetworkEvent.PROGRESS_TYPE_INPUT);

        assertEquals(-1, event.getProgressPercentage());
    }

    @Test
    void getProgressPercentageWithKnownLength() {
        NetworkEvent event = new NetworkEvent(request, NetworkEvent.PROGRESS_TYPE_OUTPUT);
        event.setLength(1000);
        event.setSentReceived(250);

        assertEquals(25, event.getProgressPercentage());
    }

    @Test
    void getProgressPercentageRoundsDown() {
        NetworkEvent event = new NetworkEvent(request, NetworkEvent.PROGRESS_TYPE_INPUT);
        event.setLength(1000);
        event.setSentReceived(333);

        assertEquals(33, event.getProgressPercentage());
    }

    @Test
    void getProgressPercentageAtCompletion() {
        NetworkEvent event = new NetworkEvent(request, NetworkEvent.PROGRESS_TYPE_COMPLETED);
        event.setLength(500);
        event.setSentReceived(500);

        assertEquals(100, event.getProgressPercentage());
    }

    @Test
    void verifyProgressTypeConstants() {
        assertEquals(1, NetworkEvent.PROGRESS_TYPE_INITIALIZING);
        assertEquals(2, NetworkEvent.PROGRESS_TYPE_OUTPUT);
        assertEquals(3, NetworkEvent.PROGRESS_TYPE_INPUT);
        assertEquals(4, NetworkEvent.PROGRESS_TYPE_COMPLETED);
    }
}

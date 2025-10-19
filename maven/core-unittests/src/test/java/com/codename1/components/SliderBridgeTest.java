package com.codename1.components;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.test.UITestBase;
import com.codename1.ui.Slider;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SliderBridgeTest extends UITestBase {
    private Field progressListenersField;
    private Method setLengthMethod;
    private Method setSentReceivedMethod;

    @BeforeEach
    void setupReflection() throws Exception {
        progressListenersField = NetworkManager.class.getDeclaredField("progressListeners");
        progressListenersField.setAccessible(true);
        setLengthMethod = NetworkEvent.class.getDeclaredMethod("setLength", int.class);
        setLengthMethod.setAccessible(true);
        setSentReceivedMethod = NetworkEvent.class.getDeclaredMethod("setSentReceived", int.class);
        setSentReceivedMethod.setAccessible(true);
        clearProgressListeners();
    }

    @AfterEach
    void clearListenersAfter() throws IllegalAccessException {
        clearProgressListeners();
    }

    @Test
    void testBindProgressForAllSourcesUpdatesSlider() throws Exception {
        Slider slider = new Slider();
        SliderBridge.bindProgress((ConnectionRequest[]) null, slider);

        ActionListener<NetworkEvent> listener = captureProgressListener();
        ConnectionRequest request = mock(ConnectionRequest.class);
        fireProgressEvent(listener, request, NetworkEvent.PROGRESS_TYPE_INITIALIZING, -1, 0);
        assertTrue(slider.isInfinite(), "Slider should enter infinite mode when initializing");

        fireProgressEvent(listener, request, NetworkEvent.PROGRESS_TYPE_INPUT, 100, 25);
        assertFalse(slider.isInfinite(), "Slider should leave infinite mode when progress is known");
        assertEquals(25, slider.getProgress());

        fireProgressEvent(listener, request, NetworkEvent.PROGRESS_TYPE_COMPLETED, 0, 0);
        assertEquals(100, slider.getProgress(), "Completion should move slider to 100");
    }

    @Test
    void testBindProgressWithSpecificSourcesIgnoresOthers() throws Exception {
        Slider slider = new Slider();
        ConnectionRequest tracked = mock(ConnectionRequest.class);
        ConnectionRequest ignored = mock(ConnectionRequest.class);
        SliderBridge.bindProgress(new ConnectionRequest[]{tracked}, slider);

        ActionListener<NetworkEvent> listener = captureProgressListener();
        fireProgressEvent(listener, ignored, NetworkEvent.PROGRESS_TYPE_INPUT, 100, 50);
        assertEquals(0, slider.getProgress(), "Events from untracked requests should be ignored");

        fireProgressEvent(listener, tracked, NetworkEvent.PROGRESS_TYPE_INPUT, 200, 100);
        assertEquals(50, slider.getProgress(), "Tracked events should update slider progress");

        fireProgressEvent(listener, tracked, NetworkEvent.PROGRESS_TYPE_COMPLETED, 0, 0);
        assertEquals(100, slider.getProgress(), "Completion should set slider to full progress");
    }

    private void clearProgressListeners() throws IllegalAccessException {
        progressListenersField.set(NetworkManager.getInstance(), null);
    }

    @SuppressWarnings("unchecked")
    private ActionListener<NetworkEvent> captureProgressListener() throws IllegalAccessException {
        EventDispatcher dispatcher = (EventDispatcher) progressListenersField.get(NetworkManager.getInstance());
        assertNotNull(dispatcher, "Binding progress should register a dispatcher");
        Collection listeners = dispatcher.getListenerCollection();
        assertNotNull(listeners, "Dispatcher should expose listeners after binding");
        assertEquals(1, listeners.size(), "Expected a single progress listener");
        return (ActionListener<NetworkEvent>) listeners.iterator().next();
    }

    private void fireProgressEvent(ActionListener<NetworkEvent> listener, ConnectionRequest source, int type, int length, int sentReceived) throws Exception {
        NetworkEvent event = new NetworkEvent(source, type);
        setLengthMethod.invoke(event, length);
        setSentReceivedMethod.invoke(event, sentReceived);
        listener.actionPerformed(event);
    }
}

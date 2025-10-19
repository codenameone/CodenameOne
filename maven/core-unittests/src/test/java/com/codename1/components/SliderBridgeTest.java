package com.codename1.components;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.test.UITestBase;
import com.codename1.ui.Slider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class SliderBridgeTest extends UITestBase {
    private Method fireProgressEvent;
    private Field progressListenersField;

    @BeforeEach
    void setupReflection() throws Exception {
        fireProgressEvent = NetworkManager.class.getDeclaredMethod("fireProgressEvent", ConnectionRequest.class, int.class, int.class, int.class);
        fireProgressEvent.setAccessible(true);
        progressListenersField = NetworkManager.class.getDeclaredField("progressListeners");
        progressListenersField.setAccessible(true);
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

        ConnectionRequest request = new ConnectionRequest();
        fireProgressEvent.invoke(NetworkManager.getInstance(), request, NetworkEvent.PROGRESS_TYPE_INITIALIZING, 0, 0);
        assertTrue(slider.isInfinite(), "Slider should enter infinite mode when initializing");

        fireProgressEvent.invoke(NetworkManager.getInstance(), request, NetworkEvent.PROGRESS_TYPE_INPUT, 100, 25);
        assertFalse(slider.isInfinite(), "Slider should leave infinite mode when progress is known");
        assertEquals(25, slider.getProgress());

        fireProgressEvent.invoke(NetworkManager.getInstance(), request, NetworkEvent.PROGRESS_TYPE_COMPLETED, 0, 0);
        assertEquals(100, slider.getProgress(), "Completion should move slider to 100");
    }

    @Test
    void testBindProgressWithSpecificSourcesIgnoresOthers() throws Exception {
        Slider slider = new Slider();
        ConnectionRequest tracked = new ConnectionRequest();
        ConnectionRequest ignored = new ConnectionRequest();
        SliderBridge.bindProgress(new ConnectionRequest[]{tracked}, slider);

        fireProgressEvent.invoke(NetworkManager.getInstance(), ignored, NetworkEvent.PROGRESS_TYPE_INPUT, 100, 50);
        assertEquals(0, slider.getProgress(), "Events from untracked requests should be ignored");

        fireProgressEvent.invoke(NetworkManager.getInstance(), tracked, NetworkEvent.PROGRESS_TYPE_INPUT, 200, 100);
        assertEquals(50, slider.getProgress(), "Tracked events should update slider progress");

        fireProgressEvent.invoke(NetworkManager.getInstance(), tracked, NetworkEvent.PROGRESS_TYPE_COMPLETED, 0, 0);
        assertEquals(100, slider.getProgress(), "Completion should set slider to full progress");
    }

    private void clearProgressListeners() throws IllegalAccessException {
        progressListenersField.set(NetworkManager.getInstance(), null);
    }
}

package com.codename1.plugin;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.plugin.event.IsGalleryTypeSupportedEvent;
import com.codename1.plugin.event.OpenGalleryEvent;
import com.codename1.plugin.event.PluginEvent;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenGalleryTest extends UITestBase {
    private Display display;
    private PluginSupport pluginSupport;
    private final List<Plugin> registeredPlugins = new ArrayList<Plugin>();

    @BeforeEach
    void setUpDisplayContext() {
        display = Display.getInstance();
        pluginSupport = display.getPluginSupport();
        implementation.resetGalleryTracking();
    }

    @AfterEach
    void tearDownPlugins() {
        for (int i = registeredPlugins.size() - 1; i >= 0; i--) {
            pluginSupport.deregisterPlugin(registeredPlugins.get(i));
        }
        registeredPlugins.clear();
        implementation.resetGalleryTracking();
    }

    @FormTest
    void openGalleryDispatchesToImplementationWhenNotConsumed() {
        RecordingPlugin plugin = new RecordingPlugin();
        registerPlugin(plugin);

        ActionListener response = new EmptyActionListener();
        int type = Display.GALLERY_IMAGE;
        display.openGallery(response, type);

        assertEquals(1, implementation.getOpenGalleryCallCount());
        assertSame(response, implementation.getLastOpenGalleryResponse());
        assertEquals(type, implementation.getLastOpenGalleryType());
        assertEquals(1, plugin.events.size());
        PluginEvent event = plugin.events.get(0);
        assertTrue(event instanceof OpenGalleryEvent);
        OpenGalleryEvent openEvent = (OpenGalleryEvent) event;
        assertEquals(type, openEvent.getType());
        assertSame(response, openEvent.getResponse());
        assertFalse(event.isConsumed());
    }

    @FormTest
    void openGalleryDoesNotCallImplementationWhenPluginConsumesEvent() {
        ConsumingPlugin plugin = new ConsumingPlugin();
        registerPlugin(plugin);

        ActionListener response = new EmptyActionListener();
        int type = Display.GALLERY_IMAGE;
        display.openGallery(response, type);

        assertEquals(0, implementation.getOpenGalleryCallCount());
        assertEquals(1, plugin.events.size());
        assertTrue(plugin.events.get(0).isConsumed());
        OpenGalleryEvent openEvent = (OpenGalleryEvent) plugin.events.get(0);
        assertEquals(type, openEvent.getType());
        assertSame(response, openEvent.getResponse());
    }

    @FormTest
    void openImageGalleryDispatchesToImplementationWhenNotConsumed() {
        RecordingPlugin plugin = new RecordingPlugin();
        registerPlugin(plugin);

        ActionListener response = new EmptyActionListener();
        display.openImageGallery(response);

        assertEquals(1, implementation.getOpenImageGalleryCallCount());
        assertSame(response, implementation.getLastOpenImageGalleryResponse());
        assertEquals(1, plugin.events.size());
        PluginEvent event = plugin.events.get(0);
        assertTrue(event instanceof OpenGalleryEvent);
        OpenGalleryEvent openEvent = (OpenGalleryEvent) event;
        assertEquals(Display.GALLERY_IMAGE, openEvent.getType());
        assertSame(response, openEvent.getResponse());
        assertFalse(event.isConsumed());
    }

    @FormTest
    void openImageGalleryDoesNotCallImplementationWhenPluginConsumesEvent() {
        ConsumingPlugin plugin = new ConsumingPlugin();
        registerPlugin(plugin);

        ActionListener response = new EmptyActionListener();
        display.openImageGallery(response);

        assertEquals(0, implementation.getOpenImageGalleryCallCount());
        assertEquals(1, plugin.events.size());
        assertTrue(plugin.events.get(0).isConsumed());
        OpenGalleryEvent openEvent = (OpenGalleryEvent) plugin.events.get(0);
        assertEquals(Display.GALLERY_IMAGE, openEvent.getType());
        assertSame(response, openEvent.getResponse());
    }

    @FormTest
    void isGalleryTypeSupportedHandledByPlugin() {
        for (int i = 0; i < 2; i++) {
            boolean supported = (i == 0);
            SupportingPlugin plugin = new SupportingPlugin(supported);
            registerPlugin(plugin);

            int type = Display.GALLERY_IMAGE;
            boolean actual = display.isGalleryTypeSupported(type);

            assertEquals(supported, actual);
            assertEquals(0, implementation.getGalleryTypeSupportedCallCount());
            assertEquals(1, plugin.events.size());
            PluginEvent event = plugin.events.get(0);
            assertTrue(event instanceof IsGalleryTypeSupportedEvent);
            assertTrue(event.isConsumed());
            assertEquals(type, ((IsGalleryTypeSupportedEvent) event).getType());

            pluginSupport.deregisterPlugin(plugin);
            registeredPlugins.remove(plugin);
        }
    }

    @FormTest
    void isGalleryTypeSupportedFallsBackToImplementation() {
        for (int i = 0; i < 2; i++) {
            boolean supported = (i == 0);
            RecordingPlugin plugin = new RecordingPlugin();
            registerPlugin(plugin);

            int type = Display.GALLERY_IMAGE;
            implementation.resetGalleryTracking();
            implementation.setGalleryTypeSupported(type, supported);

            boolean actual = display.isGalleryTypeSupported(type);

            assertEquals(supported, actual);
            assertEquals(1, implementation.getGalleryTypeSupportedCallCount());
            assertEquals(type, implementation.getLastGalleryTypeQuery());
            assertEquals(1, plugin.events.size());
            PluginEvent event = plugin.events.get(0);
            assertTrue(event instanceof IsGalleryTypeSupportedEvent);
            assertFalse(event.isConsumed());
            assertEquals(type, ((IsGalleryTypeSupportedEvent) event).getType());

            pluginSupport.deregisterPlugin(plugin);
            registeredPlugins.remove(plugin);
        }
    }

    private void registerPlugin(Plugin plugin) {
        pluginSupport.registerPlugin(plugin);
        registeredPlugins.add(plugin);
    }

    private static class EmptyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
        }
    }

    private static class RecordingPlugin implements Plugin {
        private final List<PluginEvent> events = new ArrayList<PluginEvent>();

        public void actionPerformed(PluginEvent evt) {
            events.add(evt);
        }
    }

    private static class ConsumingPlugin extends RecordingPlugin {
        @Override
        public void actionPerformed(PluginEvent evt) {
            super.actionPerformed(evt);
            evt.consume();
        }
    }

    private static class SupportingPlugin extends RecordingPlugin {
        private final boolean supported;

        SupportingPlugin(boolean supported) {
            this.supported = supported;
        }

        @Override
        public void actionPerformed(PluginEvent evt) {
            super.actionPerformed(evt);
            if (evt instanceof IsGalleryTypeSupportedEvent) {
                IsGalleryTypeSupportedEvent supportedEvent = (IsGalleryTypeSupportedEvent) evt;
                supportedEvent.setPluginEventResponse(supported);
                evt.consume();
            }
        }
    }
}

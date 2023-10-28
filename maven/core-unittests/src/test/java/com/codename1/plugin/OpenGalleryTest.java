package com.codenameone.plugin;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.plugin.Plugin;
import com.codename1.plugin.PluginSupport;
import com.codename1.plugin.event.OpenGalleryEvent;
import com.codename1.plugin.event.PluginEvent;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class OpenGalleryTest {

    private Display display;

    private PluginSupport pluginSupport;

    private CodenameOneImplementation impl;

    @BeforeEach
    public void beforeEach() throws Exception {
        Constructor<Display> constructor = Display.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        display = constructor.newInstance();

        CodenameOneImplementation impl = mock(CodenameOneImplementation.class);
        this.impl = impl;
        doAnswer((i) -> {
            ActionListener response = i.getArgument(0);
            int type = i.getArgument(1);
            return null;
        }).when(impl).openGallery(any(ActionListener.class), anyInt());

        PluginSupport pluginSupport = new PluginSupport();
        PluginSupport pluginSupportSpy = spy(pluginSupport);
        this.pluginSupport = pluginSupportSpy;
        Field implField = Display.class.getDeclaredField("impl");
        implField.setAccessible(true);
        implField.set(display, impl);

        Field pluginSupportField = Display.class.getDeclaredField("pluginSupport");
        pluginSupportField.setAccessible(true);
        pluginSupportField.set(display, pluginSupportSpy);

    }

    @Test
    public void testOpenGalleryWithNoPlugins() throws Exception {
        ArgumentCaptor<OpenGalleryEvent> captor = ArgumentCaptor.forClass(OpenGalleryEvent.class);
        ActionListener response = mock(ActionListener.class);
        int type = Display.GALLERY_IMAGE;
        display.openGallery(response, type);
        verify(impl, times(1)).openGallery(response, type);
        verify(pluginSupport, times(1)).firePluginEvent(captor.capture());
        OpenGalleryEvent capturedEvent = captor.getValue();
        assertEquals(type, capturedEvent.getType());
        assertEquals(response, capturedEvent.getResponse());
    }

    @Test
    public void testOpenGalleryDoesNotCallImplementationWhenPluginConsumesEvent() throws Exception {
        ArgumentCaptor<OpenGalleryEvent> captor = ArgumentCaptor.forClass(OpenGalleryEvent.class);
        Plugin openGalleryPlugin = new Plugin() {
            @Override
            public void actionPerformed(PluginEvent evt) {
                evt.consume();
            }
        };
        Plugin openGalleryPluginSpy = spy(openGalleryPlugin);
        pluginSupport.registerPlugin(openGalleryPluginSpy);

        ActionListener response = mock(ActionListener.class);
        int type = Display.GALLERY_IMAGE;
        display.openGallery(response, type);
        verify(impl, times(0)).openGallery(response, type);
        verify(pluginSupport, times(1)).firePluginEvent(captor.capture());
        verify(openGalleryPluginSpy, times(1)).actionPerformed(any(PluginEvent.class));

        OpenGalleryEvent capturedEvent = captor.getValue();
        assertEquals(type, capturedEvent.getType());
        assertEquals(response, capturedEvent.getResponse());
    }

    @Test
    public void testOpenImageGalleryWithNoPlugins() throws Exception {
        ArgumentCaptor<OpenGalleryEvent> captor = ArgumentCaptor.forClass(OpenGalleryEvent.class);
        ActionListener response = mock(ActionListener.class);
        int type = Display.GALLERY_IMAGE;
        display.openImageGallery(response);
        verify(impl, times(1)).openImageGallery(response);
        verify(pluginSupport, times(1)).firePluginEvent(captor.capture());
        OpenGalleryEvent capturedEvent = captor.getValue();
        assertEquals(type, capturedEvent.getType());
        assertEquals(response, capturedEvent.getResponse());
    }

    @Test
    public void testOpenImageGalleryDoesNotCallImplementationWhenPluginConsumesEvent() throws Exception {
        ArgumentCaptor<OpenGalleryEvent> captor = ArgumentCaptor.forClass(OpenGalleryEvent.class);
        Plugin openGalleryPlugin = new Plugin() {
            @Override
            public void actionPerformed(PluginEvent evt) {
                evt.consume();
            }
        };
        Plugin openGalleryPluginSpy = spy(openGalleryPlugin);
        pluginSupport.registerPlugin(openGalleryPluginSpy);

        ActionListener response = mock(ActionListener.class);
        int type = Display.GALLERY_IMAGE;
        display.openImageGallery(response);
        verify(impl, times(0)).openImageGallery(response);
        verify(pluginSupport, times(1)).firePluginEvent(captor.capture());
        verify(openGalleryPluginSpy, times(1)).actionPerformed(any(PluginEvent.class));

        OpenGalleryEvent capturedEvent = captor.getValue();
        assertEquals(type, capturedEvent.getType());
        assertEquals(response, capturedEvent.getResponse());
    }
}

package com.codename1.plugin;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.plugin.event.IsGalleryTypeSupportedEvent;
import com.codename1.plugin.event.OpenGalleryEvent;
import com.codename1.plugin.event.PluginEvent;
import com.codename1.test.helpers.DisplayContext;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class OpenGalleryTest {

    private Display display;

    private PluginSupport pluginSupport;

    private CodenameOneImplementation impl;

    @BeforeEach
    public void beforeEach() throws Exception {
        DisplayContext context = new DisplayContext();
        display = context.makeDisplay();
        pluginSupport = context.getPluginSupport();
        impl = context.getImpl();
        doAnswer((i) -> {
            ActionListener response = i.getArgument(0);
            int type = i.getArgument(1);
            return null;
        }).when(impl).openGallery(any(ActionListener.class), anyInt());
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
        verify(openGalleryPluginSpy, times(1)).actionPerformed(any(OpenGalleryEvent.class));

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
        verify(openGalleryPluginSpy, times(1)).actionPerformed(any(OpenGalleryEvent.class));

        OpenGalleryEvent capturedEvent = captor.getValue();
        assertEquals(type, capturedEvent.getType());
        assertEquals(response, capturedEvent.getResponse());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testIsGalleryTypeSupportedSupportedByPlugin(final boolean supported) throws Exception {
        ArgumentCaptor<IsGalleryTypeSupportedEvent> captor = ArgumentCaptor.forClass(IsGalleryTypeSupportedEvent.class);
        Plugin openGalleryPlugin = new Plugin() {
            @Override
            public void actionPerformed(PluginEvent evt) {
                if (evt.getEventType() == ActionEvent.Type.IsGalleryTypeSupported) {
                    evt.setPluginEventResponse(supported);
                }
            }
        };
        Plugin openGalleryPluginSpy = spy(openGalleryPlugin);
        pluginSupport.registerPlugin(openGalleryPluginSpy);

        int type = Display.GALLERY_IMAGE;
        boolean actual = display.isGalleryTypeSupported(type);
        verify(impl, times(0)).isGalleryTypeSupported(type);
        verify(pluginSupport, times(1)).firePluginEvent(captor.capture());
        verify(openGalleryPluginSpy, times(1)).actionPerformed(any(PluginEvent.class));

        IsGalleryTypeSupportedEvent capturedEvent = captor.getValue();
        assertEquals(type, capturedEvent.getType());
        assertEquals(supported, actual);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testIsGalleryTypeSupportedSupportedByImplementation(final boolean supported) throws Exception {
        ArgumentCaptor<IsGalleryTypeSupportedEvent> captor = ArgumentCaptor.forClass(IsGalleryTypeSupportedEvent.class);
        Plugin openGalleryPlugin = new Plugin() {
            @Override
            public void actionPerformed(PluginEvent evt) {

            }
        };
        Plugin openGalleryPluginSpy = spy(openGalleryPlugin);
        pluginSupport.registerPlugin(openGalleryPluginSpy);

        doReturn( supported).when(impl).isGalleryTypeSupported(anyInt());

        int type = Display.GALLERY_IMAGE;
        boolean actual = display.isGalleryTypeSupported(type);
        verify(impl, times(1)).isGalleryTypeSupported(type);
        verify(pluginSupport, times(1)).firePluginEvent(captor.capture());
        verify(openGalleryPluginSpy, times(1)).actionPerformed(any(PluginEvent.class));

        IsGalleryTypeSupportedEvent capturedEvent = captor.getValue();
        assertEquals(type, capturedEvent.getType());
        assertEquals(supported, actual);
    }
}

package com.codenameone.plugin;

import com.codename1.plugin.Plugin;
import com.codename1.plugin.PluginSupport;
import com.codename1.plugin.event.PluginEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


public class PluginSupportTest {
    @Test
    public void testPluginSupport() {
        PluginSupport pluginSupport = new PluginSupport();
        Plugin mockPlugin = Mockito.mock(MockPlugin.class);
        PluginEvent mockEvent = Mockito.mock(PluginEvent.class);
        pluginSupport.registerPlugin(mockPlugin);
        PluginEvent returnedEvent = pluginSupport.firePluginEvent(mockEvent);
        Mockito.verify(mockPlugin, Mockito.times(1)).actionPerformed(mockEvent);
        assertEquals(mockEvent, returnedEvent);
    }

    @Test
    public void testConsumeShortCircuitsPluginEvents() {
        PluginSupport pluginSupport = new PluginSupport();
        Plugin mockPlugin = Mockito.mock(Plugin.class);
        PluginEvent pluginEvent = new PluginEvent(this, PluginEvent.Type.OpenGallery) {
        };
        PluginEvent eventSpy = Mockito.spy(pluginEvent);
        doAnswer((i) -> {
            PluginEvent evt = i.getArgument(0);
            evt.consume();
            return null;
        }).when(mockPlugin).actionPerformed(eventSpy);

        Plugin mockPlugin2 = Mockito.mock(Plugin.class);
        doAnswer((i) -> {
            PluginEvent evt = i.getArgument(0);
            evt.consume();
            return null;
        }).when(mockPlugin2).actionPerformed(eventSpy);


        pluginSupport.registerPlugin(mockPlugin);
        pluginSupport.registerPlugin(mockPlugin2);
        PluginEvent returnedEvent = pluginSupport.firePluginEvent(eventSpy);
        Mockito.verify(mockPlugin, Mockito.times(1)).actionPerformed(eventSpy);
        Mockito.verify(mockPlugin2, Mockito.times(0)).actionPerformed(eventSpy);
        assertEquals(eventSpy, returnedEvent);
    }

    @Test
    public void testAllPluginsReceiveEventWhenNotConsumed() {
        PluginSupport pluginSupport = new PluginSupport();
        Plugin mockPlugin = Mockito.mock(Plugin.class);
        PluginEvent pluginEvent = new PluginEvent(this, PluginEvent.Type.OpenGallery) {
        };
        PluginEvent eventSpy = Mockito.spy(pluginEvent);
        doAnswer((i) -> {

            return null;
        }).when(mockPlugin).actionPerformed(eventSpy);

        Plugin mockPlugin2 = Mockito.mock(Plugin.class);
        doAnswer((i) -> {
            return null;
        }).when(mockPlugin2).actionPerformed(eventSpy);

        pluginSupport.registerPlugin(mockPlugin);
        pluginSupport.registerPlugin(mockPlugin2);
        PluginEvent returnedEvent = pluginSupport.firePluginEvent(eventSpy);
        Mockito.verify(mockPlugin, Mockito.times(1)).actionPerformed(eventSpy);
        Mockito.verify(mockPlugin2, Mockito.times(1)).actionPerformed(eventSpy);
        assertEquals(eventSpy, returnedEvent);
    }

    @Test
    public void testPluginNoLongerReceivesEventsAfterDeregister() {
        PluginSupport pluginSupport = new PluginSupport();
        Plugin mockPlugin = Mockito.mock(MockPlugin.class);
        PluginEvent mockEvent = Mockito.mock(PluginEvent.class);
        pluginSupport.registerPlugin(mockPlugin);
        pluginSupport.deregisterPlugin(mockPlugin);
        PluginEvent returnedEvent = pluginSupport.firePluginEvent(mockEvent);
        Mockito.verify(mockPlugin, Mockito.times(0)).actionPerformed(mockEvent);
        assertEquals(mockEvent, returnedEvent);
    }
}

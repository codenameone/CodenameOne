package com.codename1.test.helpers;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.plugin.PluginSupport;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class DisplayContext {
    private PluginSupport pluginSupport;

    private CodenameOneImplementation impl;

    public Display makeDisplay() throws Exception {
        Display display;
        Constructor<Display> constructor = Display.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        display = constructor.newInstance();
        if (this.impl == null) {
            CodenameOneImplementation impl = mock(CodenameOneImplementation.class);
            this.impl = impl;
        }
        if (this.pluginSupport == null) {
            PluginSupport pluginSupport = new PluginSupport();
            PluginSupport pluginSupportSpy = spy(pluginSupport);
            this.pluginSupport = pluginSupportSpy;
        }

        Field implField = Display.class.getDeclaredField("impl");
        implField.setAccessible(true);
        implField.set(display, impl);

        Field pluginSupportField = Display.class.getDeclaredField("pluginSupport");
        pluginSupportField.setAccessible(true);
        pluginSupportField.set(display, this.pluginSupport);

        return display;
    }

    public PluginSupport getPluginSupport() {
        return pluginSupport;
    }

    public void setPluginSupport(PluginSupport pluginSupport) {
        this.pluginSupport = pluginSupport;
    }

    public CodenameOneImplementation getImpl() {
        return impl;
    }

    public void setImpl(CodenameOneImplementation impl) {
        this.impl = impl;
    }
}

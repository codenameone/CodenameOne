package com.codename1.test;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.plugin.PluginSupport;
import com.codename1.ui.Display;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provides a minimal initialized {@link Display} environment for unit tests that instantiate UI components.
 */
public abstract class UITestBase {
    protected Display display;
    protected CodenameOneImplementation implementation;
    protected PluginSupport pluginSupport;

    @BeforeEach
    protected void setUpDisplay() throws Exception {
        display = Display.getInstance();
        resetUIManager();

        implementation = mock(CodenameOneImplementation.class);
        when(implementation.getDisplayWidth()).thenReturn(1080);
        when(implementation.getDisplayHeight()).thenReturn(1920);
        when(implementation.getActualDisplayHeight()).thenReturn(1920);
        when(implementation.getDeviceDensity()).thenReturn(Display.DENSITY_MEDIUM);
        when(implementation.convertToPixels(anyInt(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
        when(implementation.createFont(anyInt(), anyInt(), anyInt())).thenAnswer(invocation -> new Object());
        when(implementation.getDefaultFont()).thenReturn(new Object());
        when(implementation.isTrueTypeSupported()).thenReturn(true);
        when(implementation.isLookupFontSupported()).thenReturn(true);
        when(implementation.isInitialized()).thenReturn(true);
        when(implementation.getCommandBehavior()).thenReturn(Display.COMMAND_BEHAVIOR_DEFAULT);
        when(implementation.isNativeFontSchemeSupported()).thenReturn(true);
        when(implementation.loadTrueTypeFont(anyString(), anyString())).thenAnswer(invocation -> new Object());
        when(implementation.deriveTrueTypeFont(any(), anyFloat(), anyInt())).thenAnswer(invocation -> new Object());
        when(implementation.loadNativeFont(anyString())).thenAnswer(invocation -> new Object());

        pluginSupport = new PluginSupport();

        setDisplayField("impl", implementation);
        setDisplayField("pluginSupport", pluginSupport);
        setDisplayField("codenameOneRunning", true);
        setDisplayField("edt", Thread.currentThread());
    }

    @AfterEach
    protected void tearDownDisplay() throws Exception {
        resetUIManager();
        setDisplayField("impl", null);
        setDisplayField("pluginSupport", null);
        setDisplayField("codenameOneRunning", false);
        setDisplayField("edt", null);
    }

    private void setDisplayField(String fieldName, Object value) throws Exception {
        Field field = Display.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        if ((field.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) {
            field.set(null, value);
        } else {
            field.set(display, value);
        }
    }

    private void resetUIManager() throws Exception {
        Field instanceField = UIManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}

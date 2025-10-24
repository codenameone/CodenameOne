package com.codename1.test;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.Util;
import com.codename1.plugin.PluginSupport;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyChar;
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
    private Graphics codenameOneGraphics;

    @BeforeEach
    protected void setUpDisplay() throws Exception {
        display = Display.getInstance();
        resetUIManager();

        implementation = mock(CodenameOneImplementation.class);
        final Object defaultFont = new Object();
        when(implementation.getDisplayWidth()).thenReturn(1080);
        when(implementation.getDisplayHeight()).thenReturn(1920);
        when(implementation.getActualDisplayHeight()).thenReturn(1920);
        when(implementation.getDeviceDensity()).thenReturn(Display.DENSITY_MEDIUM);
        when(implementation.convertToPixels(anyInt(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
        when(implementation.createFont(anyInt(), anyInt(), anyInt())).thenReturn(defaultFont);
        when(implementation.getDefaultFont()).thenReturn(defaultFont);
        when(implementation.isTrueTypeSupported()).thenReturn(true);
        when(implementation.isLookupFontSupported()).thenReturn(true);
        when(implementation.isInitialized()).thenReturn(true);
        when(implementation.getCommandBehavior()).thenReturn(Display.COMMAND_BEHAVIOR_DEFAULT);
        when(implementation.isNativeFontSchemeSupported()).thenReturn(true);
        when(implementation.loadTrueTypeFont(anyString(), anyString())).thenReturn(defaultFont);
        when(implementation.deriveTrueTypeFont(any(), anyFloat(), anyInt())).thenReturn(defaultFont);
        when(implementation.loadNativeFont(anyString())).thenReturn(defaultFont);
        when(implementation.getNativeGraphics()).thenReturn(new Object());
        when(implementation.paintNativePeersBehind()).thenReturn(false);
        when(implementation.handleEDTException(any(Throwable.class))).thenReturn(false);
        when(implementation.charWidth(any(), anyChar())).thenReturn(8);
        when(implementation.stringWidth(any(), anyString())).thenAnswer(invocation -> {
            String text = (String) invocation.getArgument(1);
            return text == null ? 0 : text.length() * 8;
        });
        when(implementation.charsWidth(any(), any(char[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            Integer length = (Integer) invocation.getArgument(3);
            return length == null ? 0 : Math.max(0, length) * 8;
        });
        when(implementation.getHeight(any())).thenReturn(16);
        when(implementation.getPlatformName()).thenReturn("and");
        when(implementation.getProperty(anyString(), anyString())).thenAnswer(invocation -> (String) invocation.getArgument(1));
        when(implementation.loadTrueTypeFont(anyString(), anyString())).thenAnswer(invocation -> new Object());
        when(implementation.deriveTrueTypeFont(any(), anyFloat(), anyInt())).thenAnswer(invocation -> new Object());
        when(implementation.loadNativeFont(anyString())).thenAnswer(invocation -> new Object());

        pluginSupport = new PluginSupport();

        setDisplayField("impl", implementation);
        setDisplayField("pluginSupport", pluginSupport);
        setDisplayField("codenameOneRunning", true);
        setDisplayField("edt", Thread.currentThread());
        codenameOneGraphics = createGraphics();
        setDisplayField("codenameOneGraphics", codenameOneGraphics);
        Util.setImplementation(implementation);
    }

    @AfterEach
    protected void tearDownDisplay() throws Exception {
        flushSerialCalls();
        resetUIManager();
        setDisplayField("codenameOneGraphics", null);
        setDisplayField("impl", null);
        setDisplayField("pluginSupport", null);
        setDisplayField("codenameOneRunning", false);
        setDisplayField("edt", null);
        Util.setImplementation(null);
        if (implementation != null) {
            Mockito.reset(implementation);
        }
        implementation = null;
        pluginSupport = null;
        codenameOneGraphics = null;
        display = null;
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
        UIManager instance = (UIManager) instanceField.get(null);
        if (instance != null) {
            clearMap(instance, "styles");
            clearMap(instance, "selectedStyles");
            clearMap(instance, "themeConstants");
            clearMap(instance, "imageCache");
            clearMap(instance, "parseCache");

            Field themePropsField = UIManager.class.getDeclaredField("themeProps");
            themePropsField.setAccessible(true);
            Object themeProps = themePropsField.get(instance);
            if (themeProps instanceof Map) {
                ((Map) themeProps).clear();
            }
            themePropsField.set(instance, null);

            Field resourceBundleField = UIManager.class.getDeclaredField("resourceBundle");
            resourceBundleField.setAccessible(true);
            Object resourceBundle = resourceBundleField.get(instance);
            if (resourceBundle instanceof Hashtable) {
                ((Hashtable) resourceBundle).clear();
            }
            resourceBundleField.set(instance, null);

            Field bundleField = UIManager.class.getDeclaredField("bundle");
            bundleField.setAccessible(true);
            Object bundle = bundleField.get(instance);
            if (bundle instanceof Map) {
                ((Map) bundle).clear();
            }
            bundleField.set(instance, null);
        }
        instanceField.set(null, null);

        Field accessibleField = UIManager.class.getDeclaredField("accessible");
        accessibleField.setAccessible(true);
        accessibleField.setBoolean(null, true);

        Field localeAccessibleField = UIManager.class.getDeclaredField("localeAccessible");
        localeAccessibleField.setAccessible(true);
        localeAccessibleField.setBoolean(null, true);
    }

    private void clearMap(UIManager manager, String fieldName) throws Exception {
        Field field = UIManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(manager);
        if (value instanceof Map) {
            ((Map) value).clear();
        } else if (value instanceof Hashtable) {
            ((Hashtable) value).clear();
        }
    }

    private Graphics createGraphics() throws Exception {
        java.lang.reflect.Constructor<Graphics> constructor = Graphics.class.getDeclaredConstructor(Object.class);
        constructor.setAccessible(true);
        Graphics graphics = constructor.newInstance(new Object());
        Field paintPeersField = Graphics.class.getDeclaredField("paintPeersBehind");
        paintPeersField.setAccessible(true);
        paintPeersField.setBoolean(graphics, false);
        return graphics;
    }

    /**
     * Processes any pending serial calls that were queued via {@link Display#callSerially(Runnable)}.
     */
    protected void flushSerialCalls() {
        try {
            Display display = Display.getInstance();

            Field pendingField = Display.class.getDeclaredField("pendingSerialCalls");
            pendingField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Runnable> pending = (List<Runnable>) pendingField.get(display);

            Field runningField = Display.class.getDeclaredField("runningSerialCallsQueue");
            runningField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Deque<Runnable> running = (Deque<Runnable>) runningField.get(display);

            if ((pending == null || pending.isEmpty()) && (running == null || running.isEmpty())) {
                return;
            }

            Deque<Runnable> workQueue = new ArrayDeque<Runnable>();
            if (running != null && !running.isEmpty()) {
                workQueue.addAll(running);
                running.clear();
            }
            if (pending != null && !pending.isEmpty()) {
                workQueue.addAll(new ArrayList<Runnable>(pending));
                pending.clear();
            }

            while (!workQueue.isEmpty()) {
                Runnable job = workQueue.removeFirst();
                job.run();

                if (running != null && !running.isEmpty()) {
                    workQueue.addAll(running);
                    running.clear();
                }
                if (pending != null && !pending.isEmpty()) {
                    workQueue.addAll(new ArrayList<Runnable>(pending));
                    pending.clear();
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to drain Display serial calls", e);
        }
    }
}

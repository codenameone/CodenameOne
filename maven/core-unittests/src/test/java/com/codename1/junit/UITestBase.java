package com.codename1.junit;

import com.codename1.impl.ImplementationFactory;
import com.codename1.io.Util;
import com.codename1.testing.SafeL10NManager;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

/**
 * Provides a minimal initialized {@link Display} environment for unit tests that instantiate UI components.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class UITestBase {
    protected Display display;
    protected TestCodenameOneImplementation implementation;

    protected void waitFor(CountDownLatch latch, int timeout) {
        waitFor(latch, 0, timeout);
    }

    protected void waitFor(CountDownLatch latch, int count, int timeout) {
        while(latch.getCount() > count) {
            assertTrue(timeout > 0);
            TestUtils.waitFor(5);
            timeout -= 5;
        }
    }

    @BeforeAll
    protected void setUpDisplay() throws Exception {
        DisplayTest.initInvokeAndBlockThreads();
        if (!Display.isInitialized()) {
            implementation = TestCodenameOneImplementation.getInstance();
            if (implementation == null) {
                implementation = new TestCodenameOneImplementation();
            }
            final TestCodenameOneImplementation implRef = implementation;
            ImplementationFactory.setInstance(new ImplementationFactory() {
                @Override
                public Object createImplementation() {
                    return implRef;
                }
            });
            // Setup SafeL10NManager before init if possible, or immediately after
            // But L10NManager is fetched from implementation.
            implementation.setLocalizationManager(new SafeL10NManager("en", "US"));

            Display.init(null);
        } else {
            implementation = TestCodenameOneImplementation.getInstance();
            implementation.setLocalizationManager(new SafeL10NManager("en", "US"));
        }
        Util.setImplementation(implementation);
        display = Display.getInstance();
    }

    @BeforeEach
    protected void setUpImplementation() {
        implementation = TestCodenameOneImplementation.getInstance();
        implementation.setLocalizationManager(new SafeL10NManager("en", "US"));
    }

    @AfterEach
    protected void tearDownDisplay() throws Exception {
        DisplayTest.flushEdt();
        resetUIManager();
        com.codename1.ui.Toolbar.setGlobalToolbar(false);
        if (implementation != null) {
            implementation.reset();
        }

        // Clear pending serial calls on the Display to avoid pollution
        try {
            Field pendingField = Display.class.getDeclaredField("pendingSerialCalls");
            pendingField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Runnable> pending = (List<Runnable>) pendingField.get(display);
            if (pending != null) {
                pending.clear();
            }

            Field runningField = Display.class.getDeclaredField("runningSerialCallsQueue");
            runningField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Deque<Runnable> running = (Deque<Runnable>) runningField.get(display);
            if (running != null) {
                running.clear();
            }
        } catch (Exception ignored) {
        }
    }

    @AfterAll
    protected void tearDownClass() {
        Display.deinitialize();
    }


    private void resetUIManager() throws Exception {
        UIManager.getInstance().setThemeProps(new Hashtable());
        UIManager.getInstance().getLookAndFeel().setRTL(false);
    }


    /**
     * Processes any pending serial calls that were queued via {@link Display#callSerially(Runnable)}.
     */
    protected void flushSerialCalls() {
        DisplayTest.flushEdt();
    }

    protected void tapComponent(Component c) {
        implementation.tapComponent(c);
        flushSerialCalls();
    }
}

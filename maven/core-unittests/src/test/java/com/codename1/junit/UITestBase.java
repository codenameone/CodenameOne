package com.codename1.junit;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.ImplementationFactory;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Hashtable;
import java.util.List;

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
    protected TestCodenameOneImplementation implementation;

    @BeforeEach
    protected void setUpDisplay() throws Exception {
        if(!Display.isInitialized() && TestCodenameOneImplementation.getInstance() == null) {
            implementation = new TestCodenameOneImplementation();
            ImplementationFactory.setInstance(new ImplementationFactory() {
                @Override
                public Object createImplementation() {
                    return implementation;
                }
            });
            Display.init(null);
        } else {
            implementation = TestCodenameOneImplementation.getInstance();
        }
    }

    @AfterEach
    protected void tearDownDisplay() throws Exception {
        resetUIManager();
        Display.deinitialize();
    }


    private void resetUIManager() throws Exception {
        UIManager.getInstance().setThemeProps(new Hashtable());
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

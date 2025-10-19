package com.codename1.components;

import com.codename1.test.UITestBase;
import com.codename1.ui.Display;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Base class for component tests that provides utilities for working with the mocked display.
 */
abstract class ComponentTestBase extends UITestBase {
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

            // Mirror Display.processSerialCalls() behaviour enough for tests by draining both
            // queues and executing each Runnable synchronously on the calling thread. We copy the
            // pending queue first to avoid ConcurrentModificationExceptions when runnables schedule
            // new serial tasks while executing.
            Deque<Runnable> workQueue = new ArrayDeque<>();
            if (running != null && !running.isEmpty()) {
                workQueue.addAll(running);
                running.clear();
            }
            if (pending != null && !pending.isEmpty()) {
                workQueue.addAll(new ArrayList<>(pending));
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
                    workQueue.addAll(new ArrayList<>(pending));
                    pending.clear();
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to drain Display serial calls", e);
        }
    }
}

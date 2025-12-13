package com.codename1.junit;

import com.codename1.ui.CN;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

public class EDTTestInterceptor  implements InvocationInterceptor {

    private static final long DEFAULT_TIMEOUT_MILLIS = 5000;

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> ctx,
                                    ExtensionContext ext) throws Throwable {
        CN.callSeriallyAndWait(() -> pretest(ctx.getExecutable().getName()));
        runOnMyThread(invocation);
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> ctx,
                                          ExtensionContext ext) throws Throwable {
        beforePretest();
        runOnMyThread(invocation);
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> ctx,
                                         ExtensionContext ext) throws Throwable {
        runOnMyThread(invocation);
    }

    private void runOnMyThread(Invocation<Void> invocation) throws Throwable {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        final Object lock = new Object();
        final boolean[] completed = new boolean[1];

        CN.callSerially(() -> {
            try {
                invocation.proceed();
            } catch (Throwable t) {
                thrown.set(t);
            }
            synchronized (lock) {
                completed[0] = true;
                lock.notifyAll();
            }
        });

        try {
            synchronized (lock) {
                if (!completed[0]) {
                    lock.wait(DEFAULT_TIMEOUT_MILLIS);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw ie;
        }

        if (!completed[0]) {
            throw new AssertionError("FormTest timed out after " + DEFAULT_TIMEOUT_MILLIS + "ms");
        }

        Throwable t = thrown.get();
        if (t != null) throw t; // preserves the original stack trace
    }

    protected void beforePretest() {}

    protected void pretest(String testName) {
    }
}
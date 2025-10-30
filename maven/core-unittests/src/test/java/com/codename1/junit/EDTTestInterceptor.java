package com.codename1.junit;

import com.codename1.ui.CN;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

public class EDTTestInterceptor  implements InvocationInterceptor {

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
        CN.callSeriallyAndWait(() -> {
            try {
                invocation.proceed();
            } catch (Throwable t) {
                thrown.set(t);
            }
        });
        Throwable t = thrown.get();
        if (t != null) throw t; // preserves the original stack trace
    }

    protected void beforePretest() {}

    protected void pretest(String testName) {
    }
}
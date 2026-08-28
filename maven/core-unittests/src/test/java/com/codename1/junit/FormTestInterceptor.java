package com.codename1.junit;

import com.codename1.impl.ImplementationFactory;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

public class FormTestInterceptor extends EDTTestInterceptor {
    @Override
    protected void beforePretest() {
        ImplementationFactory.setInstance(new ImplementationFactory() {
            @Override
            public Object createImplementation() {
                return new TestCodenameOneImplementation();
            }
        });
        if (!Display.isInitialized()) {
            // The two halves of isInitialized() come apart, and only one of them
            // is recoverable by the init() below. It answers
            // codenameOneRunning && impl.isInitialized(); the previous class's
            // dispatch thread clears the flag on the IMPLEMENTATION on its way
            // out, whenever it is next scheduled, and leaves codenameOneRunning
            // set. init() guards on codenameOneRunning, so it returns having
            // done nothing, and every test in the class then dispatches onto a
            // thread that is gone and reports "FormTest timed out after 5000ms;
            // edt=display-not-initialized". That is how ValidatorTest failed
            // twelve times on a loaded CI runner while the same 5528 tests
            // passed locally.
            //
            // Clearing codenameOneRunning first is what makes the init real. It
            // costs nothing in the ordinary case, where isInitialized() is
            // already true and this branch is not taken at all.
            //
            // Here rather than in a @BeforeEach, which is where it was tried
            // first and cannot work: EDTTestInterceptor dispatches @BeforeEach
            // onto the very dispatch thread that is gone, so the recovery would
            // be queued behind the thing it exists to repair. beforePretest()
            // runs on the test thread, before any dispatch.
            Display.deinitialize();
        }
        Display.init(null);
    }

    @Override
    protected void pretest(String testName) {
        Form form = new Form(testName);
        form.show();
    }
}
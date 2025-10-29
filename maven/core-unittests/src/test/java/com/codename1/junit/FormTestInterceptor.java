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
        Display.init(null);
    }

    @Override
    protected void pretest() throws Throwable {
        Form form = new Form("Test Form");
        form.show();
    }
}
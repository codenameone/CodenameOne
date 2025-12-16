package com.codename1.javascript;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class JSExceptionTest extends UITestBase {

    @FormTest
    public void testConstructor() {
        JSException ex = new JSException("Test message");
        Assertions.assertEquals("Test message", ex.getMessage());
        Assertions.assertTrue(ex instanceof RuntimeException);
    }
}

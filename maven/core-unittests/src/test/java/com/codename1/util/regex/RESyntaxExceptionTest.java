package com.codename1.util.regex;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class RESyntaxExceptionTest extends UITestBase {

    @FormTest
    public void testConstructor() {
        RESyntaxException ex = new RESyntaxException("Invalid regex");
        Assertions.assertEquals("Syntax error: Invalid regex", ex.getMessage());
        Assertions.assertTrue(ex instanceof RuntimeException);
    }
}

package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class BlockingDisallowedExceptionTest extends UITestBase {

    @FormTest
    public void testConstructor() {
        BlockingDisallowedException ex = new BlockingDisallowedException();
        Assertions.assertEquals("Attempt to run invokeAndBlock while blocking is disabled.", ex.getMessage());
        Assertions.assertTrue(ex instanceof IllegalStateException);
    }
}

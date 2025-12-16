package com.codename1.io;

import com.codename1.ui.Display;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.Assertions;

public class MalformedURLExceptionTest extends UITestBase {

    @FormTest
    public void testMalformedURLException() {
        MalformedURLException ex = new MalformedURLException();
        Assertions.assertNull(ex.getMessage());

        ex = new MalformedURLException("Test Message");
        Assertions.assertEquals("Test Message", ex.getMessage());
    }

}

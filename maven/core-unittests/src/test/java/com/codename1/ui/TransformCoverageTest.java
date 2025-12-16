package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Transform.NotInvertibleException;
import static com.codename1.testing.TestUtils.*;

public class TransformCoverageTest extends UITestBase {

    @FormTest
    public void testNotInvertibleException() {
        NotInvertibleException ex = new NotInvertibleException();
        // Just instantiating covers the class
        assertTrue(ex != null);
    }
}

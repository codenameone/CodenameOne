package com.codename1.io.gzip;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;
import com.codename1.junit.FormTest;

public class JZlibTest extends UITestBase {

    @FormTest
    public void testJZlibConstants() {
        // JZlib seems to be a class with constants and version info
        Assertions.assertNotNull(JZlib.version());
        // Version changed to 1.1.0
        Assertions.assertEquals("1.1.0", JZlib.version());

        // Check constants
        Assertions.assertEquals(0, JZlib.Z_OK);
        Assertions.assertEquals(1, JZlib.Z_STREAM_END);
        Assertions.assertEquals(2, JZlib.Z_NEED_DICT);
        Assertions.assertEquals(-1, JZlib.Z_ERRNO);
        Assertions.assertEquals(-2, JZlib.Z_STREAM_ERROR);
        Assertions.assertEquals(-3, JZlib.Z_DATA_ERROR);
        Assertions.assertEquals(-4, JZlib.Z_MEM_ERROR);
        Assertions.assertEquals(-5, JZlib.Z_BUF_ERROR);
        Assertions.assertEquals(-6, JZlib.Z_VERSION_ERROR);
    }
}

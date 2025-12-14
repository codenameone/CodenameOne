package com.codename1.io;

import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.Assertions;

public class UtilUUIDTest extends UITestBase {

    @FormTest
    public void testUtilUUID() {
        // Test getUUID()
        String uuid1 = Util.getUUID();
        Assertions.assertNotNull(uuid1);
        Assertions.assertTrue(uuid1.length() > 0);
        // Canonical form is 8-4-4-4-12 chars (36 chars total)
        Assertions.assertEquals(36, uuid1.length());

        // Test getUUID(long, long)
        long time = 123456789L;
        long clock = 987654321L;
        String uuid2 = Util.getUUID(time, clock);
        Assertions.assertNotNull(uuid2);
        Assertions.assertEquals(36, uuid2.length());

        // Verify consistency (same inputs should produce same output for getUUID(long, long))
        String uuid3 = Util.getUUID(time, clock);
        Assertions.assertEquals(uuid2, uuid3);

        // Verify randomness of getUUID()
        String uuid4 = Util.getUUID();
        Assertions.assertNotEquals(uuid1, uuid4);
    }
}

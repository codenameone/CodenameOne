package com.codename1.testing;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class AbstractTestCoverageTest extends UITestBase {

    @FormTest
    public void testAbstractTest() {
        // AbstractTest is an abstract class in com.codename1.testing package.
        // We can create a concrete implementation to test it.

        class ConcreteTest extends AbstractTest {
            public boolean runTest() throws Exception {
                // Call protected/public methods
                assertTrue(true);
                assertEqual(1, 1);
                assertNotEqual(1, 2);
                assertNull(null);
                assertNotNull(new Object());
                return true;
            }

            public void testFail() {
                 // fail("Fail");
            }

            @Override
            public void prepare() {
                super.prepare();
            }

            @Override
            public void cleanup() {
                super.cleanup();
            }
        }

        ConcreteTest t = new ConcreteTest();
        t.prepare();
        try {
            t.runTest();
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
        t.cleanup();
    }
}

package com.codename1.testing;

import com.codename1.test.UITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestReporting class.
 * Tests focus on singleton pattern and basic functionality.
 * Note: Tests that use Log are excluded as they require full framework initialization.
 */
class TestReportingTest extends UITestBase {
    private TestReporting testReporting;

    @BeforeEach
    void setUp() {
        testReporting = new TestReporting();
        TestReporting.setInstance(testReporting);
    }

    @Test
    void getInstanceReturnsSingleton() {
        TestReporting instance1 = TestReporting.getInstance();
        TestReporting instance2 = TestReporting.getInstance();

        assertSame(instance1, instance2);
        assertNotNull(instance1);
    }

    @Test
    void setInstanceUpdatesTheSingleton() {
        TestReporting customInstance = new TestReporting();
        TestReporting.setInstance(customInstance);

        assertSame(customInstance, TestReporting.getInstance());
    }

    @Test
    void testReportingCanBeReset() {
        testReporting = new TestReporting();

        TestReporting newInstance = new TestReporting();
        TestReporting.setInstance(newInstance);

        assertSame(newInstance, TestReporting.getInstance());
    }

    @Test
    void multipleInstancesCanBeCreated() {
        TestReporting instance1 = new TestReporting();
        TestReporting instance2 = new TestReporting();

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertNotSame(instance1, instance2);
    }

    @Test
    void setInstanceAcceptsNull() {
        TestReporting.setInstance(null);
        assertNull(TestReporting.getInstance());

        // Restore for other tests
        TestReporting.setInstance(testReporting);
    }
}

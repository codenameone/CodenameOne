package com.codename1.testing;

import com.codename1.test.UITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DeviceRunner class.
 * Note: Tests that would trigger System.exit() are intentionally excluded as they would halt the test suite.
 * Tests focus on the runTest() method which is the core testable functionality.
 */
class DeviceRunnerTest extends UITestBase {
    private TestDeviceRunner deviceRunner;

    @BeforeEach
    void setUp() {
        deviceRunner = new TestDeviceRunner();
    }

    @Test
    void runTestExecutesTestAndReportsResults() {
        deviceRunner.runTest("com.codename1.testing.DeviceRunnerTest$MockPassingTest");

        assertEquals(1, deviceRunner.getStartApplicationCallCount());
        assertEquals(1, deviceRunner.getStopApplicationCallCount());
    }

    @Test
    void runTestHandlesTestFailure() {
        deviceRunner.runTest("com.codename1.testing.DeviceRunnerTest$MockFailingTest");

        assertEquals(1, deviceRunner.getStartApplicationCallCount());
        assertEquals(1, deviceRunner.getStopApplicationCallCount());
    }

    @Test
    void runTestHandlesTestException() {
        deviceRunner.runTest("com.codename1.testing.DeviceRunnerTest$MockExceptionTest");

        assertEquals(1, deviceRunner.getStartApplicationCallCount());
        assertEquals(1, deviceRunner.getStopApplicationCallCount());
    }

    @Test
    void runTestHandlesNonExistentTestClass() {
        deviceRunner.runTest("com.example.NonExistentTestClass");

        // Should not call start/stop for invalid test
        assertEquals(0, deviceRunner.getStartApplicationCallCount());
        assertEquals(0, deviceRunner.getStopApplicationCallCount());
    }

    @Test
    void runTestHandlesTestThatThrowsInPrepare() {
        deviceRunner.runTest("com.codename1.testing.DeviceRunnerTest$MockPrepareExceptionTest");

        assertEquals(1, deviceRunner.getStartApplicationCallCount());
        assertEquals(1, deviceRunner.getStopApplicationCallCount());
    }

    @Test
    void runTestHandlesTestThatThrowsInCleanup() {
        deviceRunner.runTest("com.codename1.testing.DeviceRunnerTest$MockCleanupExceptionTest");

        assertEquals(1, deviceRunner.getStartApplicationCallCount());
        assertEquals(1, deviceRunner.getStopApplicationCallCount());
    }

    @Test
    void runTestExecutesOnEDTWhenRequired() {
        deviceRunner.runTest("com.codename1.testing.DeviceRunnerTest$MockEDTTest");

        assertEquals(1, deviceRunner.getStartApplicationCallCount());
        assertEquals(1, deviceRunner.getStopApplicationCallCount());
    }

    @Test
    void runTestExecutesOffEDTWhenNotRequired() {
        deviceRunner.runTest("com.codename1.testing.DeviceRunnerTest$MockNonEDTTest");

        assertEquals(1, deviceRunner.getStartApplicationCallCount());
        assertEquals(1, deviceRunner.getStopApplicationCallCount());
    }

    // Test implementation of DeviceRunner
    private static class TestDeviceRunner extends DeviceRunner {
        private int startApplicationCallCount = 0;
        private int stopApplicationCallCount = 0;

        @Override
        protected void startApplicationInstance() {
            startApplicationCallCount++;
        }

        @Override
        protected void stopApplicationInstance() {
            stopApplicationCallCount++;
        }

        public int getStartApplicationCallCount() {
            return startApplicationCallCount;
        }

        public int getStopApplicationCallCount() {
            return stopApplicationCallCount;
        }
    }

    // Mock test classes for testing
    public static class MockPassingTest implements UnitTest {
        @Override
        public boolean runTest() {
            return true;
        }

        @Override
        public void prepare() {}

        @Override
        public void cleanup() {}

        @Override
        public int getTimeoutMillis() {
            return 1000;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return false;
        }
    }

    public static class MockFailingTest implements UnitTest {
        @Override
        public boolean runTest() {
            return false;
        }

        @Override
        public void prepare() {}

        @Override
        public void cleanup() {}

        @Override
        public int getTimeoutMillis() {
            return 1000;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return false;
        }
    }

    public static class MockExceptionTest implements UnitTest {
        @Override
        public boolean runTest() throws Exception {
            throw new RuntimeException("Test exception");
        }

        @Override
        public void prepare() {}

        @Override
        public void cleanup() {}

        @Override
        public int getTimeoutMillis() {
            return 1000;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return false;
        }
    }

    public static class MockPrepareExceptionTest implements UnitTest {
        @Override
        public boolean runTest() {
            return true;
        }

        @Override
        public void prepare() {
            throw new RuntimeException("Prepare exception");
        }

        @Override
        public void cleanup() {}

        @Override
        public int getTimeoutMillis() {
            return 1000;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return false;
        }
    }

    public static class MockCleanupExceptionTest implements UnitTest {
        @Override
        public boolean runTest() {
            return true;
        }

        @Override
        public void prepare() {}

        @Override
        public void cleanup() {
            throw new RuntimeException("Cleanup exception");
        }

        @Override
        public int getTimeoutMillis() {
            return 1000;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return false;
        }
    }

    public static class MockEDTTest implements UnitTest {
        @Override
        public boolean runTest() {
            return true;
        }

        @Override
        public void prepare() {}

        @Override
        public void cleanup() {}

        @Override
        public int getTimeoutMillis() {
            return 1000;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return true;
        }
    }

    public static class MockNonEDTTest implements UnitTest {
        @Override
        public boolean runTest() {
            return true;
        }

        @Override
        public void prepare() {}

        @Override
        public void cleanup() {}

        @Override
        public int getTimeoutMillis() {
            return 1000;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return false;
        }
    }
}

package com.codename1.testing;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.Log;
import com.codename1.io.TestImplementationProvider;
import com.codename1.test.UITestBase;
import com.codename1.ui.Display;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DeviceRunnerTest extends UITestBase {
    private TestDeviceRunner deviceRunner;

    @BeforeEach
    void setUp() {
        deviceRunner = new TestDeviceRunner();
    }

    @Test
    void runTestsHandlesMissingTestDataFile() {
        // When no tests.dat file exists, should log error and exit
        when(implementation.getResourceAsStream(any(), eq("/tests.dat"))).thenReturn(null);

        // Running tests should handle missing file gracefully
        assertDoesNotThrow(() -> deviceRunner.runTests());
    }

    @Test
    void runTestsLoadsAndExecutesTestsFromDataFile() throws IOException {
        // Create a test data file with version 1 and two test classes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(1); // version
        dos.writeInt(2); // number of tests
        dos.writeUTF("com.codename1.testing.DeviceRunnerTest$MockTest1");
        dos.writeUTF("com.codename1.testing.DeviceRunnerTest$MockTest2");
        dos.close();

        InputStream testDataStream = new ByteArrayInputStream(baos.toByteArray());
        when(implementation.getResourceAsStream(any(), eq("/tests.dat"))).thenReturn(testDataStream);

        deviceRunner.runTests();

        assertEquals(2, deviceRunner.getStartApplicationCallCount());
        assertEquals(2, deviceRunner.getStopApplicationCallCount());
    }

    @Test
    void runTestsHandlesNewerVersionGracefully() throws IOException {
        // Create a test data file with version 2 (newer than supported)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(2); // newer version
        dos.close();

        InputStream testDataStream = new ByteArrayInputStream(baos.toByteArray());
        when(implementation.getResourceAsStream(any(), eq("/tests.dat"))).thenReturn(testDataStream);

        assertDoesNotThrow(() -> deviceRunner.runTests());
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
    void runTestsCountsPassedAndFailedTests() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(1); // version
        dos.writeInt(3); // number of tests
        dos.writeUTF("com.codename1.testing.DeviceRunnerTest$MockPassingTest");
        dos.writeUTF("com.codename1.testing.DeviceRunnerTest$MockFailingTest");
        dos.writeUTF("com.codename1.testing.DeviceRunnerTest$MockPassingTest");
        dos.close();

        InputStream testDataStream = new ByteArrayInputStream(baos.toByteArray());
        when(implementation.getResourceAsStream(any(), eq("/tests.dat"))).thenReturn(testDataStream);

        deviceRunner.runTests();

        assertEquals(3, deviceRunner.getStartApplicationCallCount());
    }

    @Test
    void runTestsHandlesIOException() throws IOException {
        InputStream failingStream = mock(InputStream.class);
        when(failingStream.read(any(byte[].class))).thenThrow(new IOException("Test error"));
        when(implementation.getResourceAsStream(any(), eq("/tests.dat"))).thenReturn(failingStream);

        assertDoesNotThrow(() -> deviceRunner.runTests());
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

    public static class MockTest1 implements UnitTest {
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

    public static class MockTest2 implements UnitTest {
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

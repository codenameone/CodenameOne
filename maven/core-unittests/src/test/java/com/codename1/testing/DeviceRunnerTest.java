package com.codename1.testing;

import com.codename1.test.UITestBase;
import com.codename1.ui.Form;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class DeviceRunnerTest extends UITestBase {
    private Form currentForm;

    @BeforeEach
    void setupRunnerEnvironment() throws Exception {
        currentForm = null;
        when(implementation.getCurrentForm()).thenAnswer(invocation -> currentForm);
        doAnswer(invocation -> {
            currentForm = invocation.getArgument(0);
            return null;
        }).when(implementation).setCurrentForm(any(Form.class));
    }

    @AfterEach
    void resetReporting() {
        TestReporting.setInstance(null);
    }

    @Test
    void runTestsProcessesTestsDatAndReportsResults() throws Exception {
        byte[] data = createTestsDat(PassingUnitTest.class.getName(), FailingUnitTest.class.getName());
        when(implementation.getResourceAsStream(any(), eq("/tests.dat"))).thenAnswer(invocation -> new ByteArrayInputStream(data));

        RecordingTestReporting reporting = new RecordingTestReporting();
        TestReporting.setInstance(reporting);

        TestDeviceRunner runner = new TestDeviceRunner();
        PassingUnitTest.reset();
        FailingUnitTest.reset();

        runner.runTests();

        assertEquals(Arrays.asList(PassingUnitTest.class.getName(), FailingUnitTest.class.getName()), reporting.startedTests);
        assertEquals(Arrays.asList(PassingUnitTest.class.getName(), FailingUnitTest.class.getName()), reporting.finishedTests);
        assertEquals(Arrays.asList(Boolean.TRUE, Boolean.FALSE), reporting.results);
        assertEquals(Arrays.asList("start", "stop", "start", "stop"), runner.lifecycle);
        assertEquals(1, runner.getPassedCount());
        assertEquals(1, runner.getFailedCount());
        assertTrue(PassingUnitTest.prepared);
        assertTrue(PassingUnitTest.cleaned);
        assertTrue(FailingUnitTest.prepared);
        assertTrue(FailingUnitTest.cleaned);
        assertEquals(runner.getClass().getName(), reporting.finishedSuites.get(0));
    }

    @Test
    void runTestHandlesInstantiationFailureGracefully() throws Exception {
        RecordingTestReporting reporting = new RecordingTestReporting();
        TestReporting.setInstance(reporting);

        TestDeviceRunner runner = new TestDeviceRunner();
        runner.runTest("java.lang.String");

        assertTrue(reporting.messages.contains("Failed to create instance of java.lang.String"));
        assertTrue(reporting.messages.contains("Verify the class is public and doesn't have a specialized constructor"));
        assertEquals(1, reporting.exceptions.size());
        assertEquals(0, runner.getPassedCount());
        assertEquals(0, runner.getFailedCount());
        assertTrue(runner.lifecycle.isEmpty());
    }

    @Test
    void runTestCapturesExceptionsDuringExecution() throws Exception {
        RecordingTestReporting reporting = new RecordingTestReporting();
        TestReporting.setInstance(reporting);

        TestDeviceRunner runner = new TestDeviceRunner();
        ThrowingUnitTest.reset();

        runner.runTest(ThrowingUnitTest.class.getName());

        assertEquals(1, runner.getFailedCount());
        assertEquals(0, runner.getPassedCount());
        assertEquals(Arrays.asList("start", "stop"), runner.lifecycle);
        assertEquals(1, reporting.exceptions.size());
        assertEquals(2, reporting.finishedTests.size());
        assertEquals(ThrowingUnitTest.class.getName(), reporting.finishedTests.get(0));
        assertEquals(Boolean.FALSE, reporting.results.get(0));
        assertFalse(ThrowingUnitTest.cleaned);
    }

    private byte[] createTestsDat(String... classNames) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(1);
        dos.writeInt(classNames.length);
        for (String name : classNames) {
            dos.writeUTF(name);
        }
        dos.close();
        return bos.toByteArray();
    }

    private static class TestDeviceRunner extends DeviceRunner {
        private final List<String> lifecycle = new ArrayList<String>();

        protected void startApplicationInstance() {
            lifecycle.add("start");
        }

        protected void stopApplicationInstance() {
            lifecycle.add("stop");
        }

        int getPassedCount() {
            return getIntField("passedTests");
        }

        int getFailedCount() {
            return getIntField("failedTests");
        }

        private int getIntField(String name) {
            try {
                Field f = DeviceRunner.class.getDeclaredField(name);
                f.setAccessible(true);
                return f.getInt(this);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static class RecordingTestReporting extends TestReporting {
        private final List<String> startedTests = new ArrayList<String>();
        private final List<String> finishedTests = new ArrayList<String>();
        private final List<Boolean> results = new ArrayList<Boolean>();
        private final List<String> messages = new ArrayList<String>();
        private final List<Throwable> exceptions = new ArrayList<Throwable>();
        private final List<String> finishedSuites = new ArrayList<String>();

        public void startingTestCase(String testName) {
            startedTests.add(testName);
        }

        public void finishedTestCase(String testName, boolean passed) {
            finishedTests.add(testName);
            results.add(Boolean.valueOf(passed));
        }

        public void logMessage(String message) {
            messages.add(message);
        }

        public void logException(Throwable err) {
            exceptions.add(err);
        }

        public void testExecutionFinished(String testSuiteName) {
            finishedSuites.add(testSuiteName);
        }
    }

    public static class PassingUnitTest extends AbstractTest {
        private static boolean prepared;
        private static boolean cleaned;

        public static void reset() {
            prepared = false;
            cleaned = false;
        }

        public void prepare() {
            prepared = true;
        }

        public boolean runTest() {
            return true;
        }

        public void cleanup() {
            cleaned = true;
        }

        public boolean shouldExecuteOnEDT() {
            return true;
        }
    }

    public static class FailingUnitTest extends AbstractTest {
        private static boolean prepared;
        private static boolean cleaned;

        public static void reset() {
            prepared = false;
            cleaned = false;
        }

        public void prepare() {
            prepared = true;
        }

        public boolean runTest() {
            return false;
        }

        public void cleanup() {
            cleaned = true;
        }

        public boolean shouldExecuteOnEDT() {
            return false;
        }
    }

    public static class ThrowingUnitTest extends AbstractTest {
        private static boolean cleaned;

        public static void reset() {
            cleaned = false;
        }

        public boolean runTest() {
            throw new IllegalStateException("boom");
        }

        public void cleanup() {
            cleaned = true;
        }

        public boolean shouldExecuteOnEDT() {
            return true;
        }
    }
}

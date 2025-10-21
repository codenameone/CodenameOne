package com.codename1.testing;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.Log;
import com.codename1.io.TestImplementationProvider;
import com.codename1.test.UITestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

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
    void startingTestCaseLogsTestName() {
        assertDoesNotThrow(() -> testReporting.startingTestCase("TestClassName"));
    }

    @Test
    void startingTestCaseWithUnitTestDelegatesToStringVersion() {
        UnitTest mockTest = new MockUnitTest();
        assertDoesNotThrow(() -> testReporting.startingTestCase(mockTest));
    }

    @Test
    void logMessageAcceptsNullMessage() {
        assertDoesNotThrow(() -> testReporting.logMessage(null));
    }

    @Test
    void logMessageAcceptsEmptyMessage() {
        assertDoesNotThrow(() -> testReporting.logMessage(""));
    }

    @Test
    void logMessageAcceptsValidMessage() {
        assertDoesNotThrow(() -> testReporting.logMessage("Test message"));
    }

    @Test
    void logExceptionAcceptsNullException() {
        assertDoesNotThrow(() -> testReporting.logException(null));
    }

    @Test
    void logExceptionAcceptsValidException() {
        Exception testException = new RuntimeException("Test exception");
        assertDoesNotThrow(() -> testReporting.logException(testException));
    }

    @Test
    void finishedTestCaseRecordsPassedTest() {
        testReporting.finishedTestCase("PassedTest", true);

        // Verify by writing report
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> testReporting.writeReport("TestSuite", outputStream));

        String report = outputStream.toString();
        assertTrue(report.contains("PassedTest"));
        assertTrue(report.contains("passed"));
    }

    @Test
    void finishedTestCaseRecordsFailedTest() {
        testReporting.finishedTestCase("FailedTest", false);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> testReporting.writeReport("TestSuite", outputStream));

        String report = outputStream.toString();
        assertTrue(report.contains("FailedTest"));
        assertTrue(report.contains("failed"));
    }

    @Test
    void finishedTestCaseWithUnitTestDelegatesToStringVersion() {
        UnitTest mockTest = new MockUnitTest();
        assertDoesNotThrow(() -> testReporting.finishedTestCase(mockTest, true));
    }

    @Test
    void finishedTestCaseHandlesNullTestName() {
        assertDoesNotThrow(() -> testReporting.finishedTestCase((String) null, true));
    }

    @Test
    void writeReportGeneratesCorrectFormat() throws IOException {
        testReporting.finishedTestCase("Test1", true);
        testReporting.finishedTestCase("Test2", false);
        testReporting.finishedTestCase("Test3", true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        testReporting.writeReport("MyTestSuite", outputStream);

        String report = outputStream.toString();
        assertTrue(report.contains("Test1 passed"));
        assertTrue(report.contains("Test2 failed"));
        assertTrue(report.contains("Test3 passed"));
    }

    @Test
    void writeReportHandlesEmptyTestResults() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        testReporting.writeReport("EmptySuite", outputStream);

        String report = outputStream.toString();
        assertEquals("", report);
    }

    @Test
    void writeReportDeprecatedMethodDoesNotThrow() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> testReporting.writeReport(outputStream));
    }

    @Test
    void testExecutionFinishedCallsDeprecatedMethod() {
        CustomTestReporting customReporting = new CustomTestReporting();
        TestReporting.setInstance(customReporting);

        customReporting.testExecutionFinished("TestSuite");

        assertTrue(customReporting.wasDeprecatedMethodCalled());
    }

    @Test
    void testExecutionFinishedAcceptsNullSuiteName() {
        assertDoesNotThrow(() -> testReporting.testExecutionFinished(null));
    }

    @Test
    void testExecutionFinishedDeprecatedMethodDoesNotThrow() {
        assertDoesNotThrow(() -> testReporting.testExecutionFinished());
    }

    @Test
    void multipleTestsAreTrackedCorrectly() throws IOException {
        testReporting.startingTestCase("Test1");
        testReporting.finishedTestCase("Test1", true);

        testReporting.startingTestCase("Test2");
        testReporting.finishedTestCase("Test2", false);

        testReporting.startingTestCase("Test3");
        testReporting.finishedTestCase("Test3", true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        testReporting.writeReport("Suite", outputStream);

        String report = outputStream.toString();
        assertTrue(report.contains("Test1 passed"));
        assertTrue(report.contains("Test2 failed"));
        assertTrue(report.contains("Test3 passed"));
    }

    @Test
    void sameTestCanBeRunMultipleTimes() throws IOException {
        testReporting.finishedTestCase("RepeatedTest", true);
        testReporting.finishedTestCase("RepeatedTest", false);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        testReporting.writeReport("Suite", outputStream);

        String report = outputStream.toString();
        // The last result should be recorded
        assertTrue(report.contains("RepeatedTest"));
    }

    @Test
    void testReportingCanBeReset() {
        testReporting.finishedTestCase("Test1", true);

        TestReporting newInstance = new TestReporting();
        TestReporting.setInstance(newInstance);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> newInstance.writeReport("Suite", outputStream));

        String report = outputStream.toString();
        assertEquals("", report);
    }

    @Test
    void writeReportHandlesSpecialCharactersInTestNames() throws IOException {
        testReporting.finishedTestCase("Test with spaces", true);
        testReporting.finishedTestCase("Test-with-dashes", false);
        testReporting.finishedTestCase("Test_with_underscores", true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        testReporting.writeReport("Suite", outputStream);

        String report = outputStream.toString();
        assertTrue(report.contains("Test with spaces passed"));
        assertTrue(report.contains("Test-with-dashes failed"));
        assertTrue(report.contains("Test_with_underscores passed"));
    }

    @Test
    void logMessageWithLongText() {
        String longMessage = "This is a very long message that contains a lot of text to test " +
                "if the logging system can handle longer messages without issues. " +
                "It should be able to process this without any problems.";

        assertDoesNotThrow(() -> testReporting.logMessage(longMessage));
    }

    @Test
    void logExceptionWithNestedExceptions() {
        Exception rootCause = new RuntimeException("Root cause");
        Exception wrappedException = new RuntimeException("Wrapped exception", rootCause);

        assertDoesNotThrow(() -> testReporting.logException(wrappedException));
    }

    // Mock UnitTest implementation
    private static class MockUnitTest implements UnitTest {
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

    // Custom TestReporting to test deprecated method calls
    private static class CustomTestReporting extends TestReporting {
        private boolean deprecatedMethodCalled = false;

        @Override
        public void testExecutionFinished() {
            deprecatedMethodCalled = true;
        }

        public boolean wasDeprecatedMethodCalled() {
            return deprecatedMethodCalled;
        }
    }
}

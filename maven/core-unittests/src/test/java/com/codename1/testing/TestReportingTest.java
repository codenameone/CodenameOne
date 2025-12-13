package com.codename1.testing;

import com.codename1.junit.FormTest;
import com.codename1.junit.TestLogger;
import com.codename1.junit.UITestBase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestReportingTest extends UITestBase {
    @BeforeEach
    void installLogger() {
        TestLogger.install();
        TestLogger.getPrinted().clear();
        TestLogger.getThrowables().clear();
    }

    @AfterEach
    void resetSingleton() {
        TestReporting.setInstance(null);
        TestLogger.remove();
    }

    @FormTest
    void finishedTestCaseStoresResultsAndWritesReport() throws IOException {
        TestReporting reporting = new TestReporting();
        reporting.finishedTestCase("alpha", true);
        reporting.finishedTestCase("beta", false);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        reporting.writeReport("suite", out);
        String report = new String(out.toByteArray(), "UTF-8");
        assertTrue(report.contains("alpha passed"));
        assertTrue(report.contains("beta failed"));
        assertFalse(TestLogger.getPrinted().isEmpty());
    }

    @FormTest
    void singletonAccessorsReturnConfiguredInstance() {
        TestReporting custom = new TestReporting();
        TestReporting.setInstance(custom);
        assertSame(custom, TestReporting.getInstance());

        TestReporting.setInstance(null);
        assertNotNull(TestReporting.getInstance());
    }

    @FormTest
    void deprecatedEntryPointsDelegateToNewMethods() throws IOException {
        DelegatingReporting reporting = new DelegatingReporting();
        DummyUnitTest dummy = new DummyUnitTest();

        reporting.startingTestCase(dummy);
        reporting.finishedTestCase(dummy, true);
        reporting.testExecutionFinished("suite");
        reporting.writeReport("suite", new ByteArrayOutputStream());

        assertEquals(1, reporting.started.size());
        assertEquals(DummyUnitTest.class.getName(), reporting.started.get(0));
        assertEquals(1, reporting.finished.size());
        assertEquals(Boolean.TRUE, reporting.finished.get(0));
        assertTrue(reporting.legacyFinishedInvoked);
        assertFalse(reporting.reportInvoked);
    }

    private static class DelegatingReporting extends TestReporting {
        private final List<String> started = new java.util.ArrayList<String>();
        private final List<Boolean> finished = new java.util.ArrayList<Boolean>();
        private boolean legacyFinishedInvoked;
        private boolean reportInvoked;

        @Override
        public void startingTestCase(String testName) {
            super.startingTestCase(testName);
            started.add(testName);
        }

        @Override
        public void finishedTestCase(String testName, boolean passed) {
            super.finishedTestCase(testName, passed);
            finished.add(Boolean.valueOf(passed));
        }

        @Override
        public void testExecutionFinished() {
            legacyFinishedInvoked = true;
        }

        @Override
        public void writeReport(OutputStream os) {
            reportInvoked = true;
        }
    }

    private static class DummyUnitTest extends AbstractTest {
        @Override
        public boolean runTest() {
            return true;
        }
    }
}

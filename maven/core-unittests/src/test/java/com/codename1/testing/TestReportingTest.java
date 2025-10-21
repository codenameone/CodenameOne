package com.codename1.testing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TestReportingTest {
    @AfterEach
    void resetSingleton() {
        TestReporting.setInstance(null);
    }

    @Test
    void finishedTestCaseStoresResultsAndWritesReport() throws Exception {
        TestReporting reporting = new TestReporting();
        reporting.finishedTestCase("alpha", true);
        reporting.finishedTestCase("beta", false);

        Hashtable<?, ?> table = getResults(reporting);
        assertEquals(Boolean.TRUE, table.get("alpha"));
        assertEquals(Boolean.FALSE, table.get("beta"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        reporting.writeReport("suite", out);
        String report = new String(out.toByteArray(), "UTF-8");
        assertTrue(report.contains("alpha passed"));
        assertTrue(report.contains("beta failed"));
    }

    @Test
    void singletonAccessorsReturnConfiguredInstance() {
        TestReporting custom = new TestReporting();
        TestReporting.setInstance(custom);
        assertSame(custom, TestReporting.getInstance());

        TestReporting.setInstance(null);
        assertNotNull(TestReporting.getInstance());
    }

    @Test
    void deprecatedEntryPointsDelegateToNewMethods() throws Exception {
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
        assertTrue(reporting.reportInvoked);
    }

    @SuppressWarnings("unchecked")
    private Hashtable<String, Boolean> getResults(TestReporting reporting) throws Exception {
        Field field = TestReporting.class.getDeclaredField("testsExecuted");
        field.setAccessible(true);
        return (Hashtable<String, Boolean>) field.get(reporting);
    }

    private static class DelegatingReporting extends TestReporting {
        private final List<String> started = new ArrayList<String>();
        private final List<Boolean> finished = new ArrayList<Boolean>();
        private boolean legacyFinishedInvoked;
        private boolean reportInvoked;

        public void startingTestCase(String testName) {
            super.startingTestCase(testName);
            started.add(testName);
        }

        public void finishedTestCase(String testName, boolean passed) {
            super.finishedTestCase(testName, passed);
            finished.add(Boolean.valueOf(passed));
        }

        public void testExecutionFinished() {
            legacyFinishedInvoked = true;
        }

        public void writeReport(OutputStream os) throws IOException {
            reportInvoked = true;
        }
    }

    private static class DummyUnitTest extends AbstractTest {
        public boolean runTest() {
            return true;
        }
    }
}

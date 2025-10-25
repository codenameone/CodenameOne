package com.codename1.testing;

import com.codename1.io.Log;
import com.codename1.test.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class TestRunnerComponentTest extends UITestBase {
    private Form currentForm;

    @BeforeEach
    void prepareDisplay() throws Exception {
        currentForm = null;
        when(implementation.getCurrentForm()).thenAnswer(invocation -> currentForm);
        doAnswer(invocation -> {
            currentForm = invocation.getArgument(0);
            return null;
        }).when(implementation).setCurrentForm(any(Form.class));
    }

    @AfterEach
    void cleanupForms() {
        currentForm = null;
    }

    @Test
    void runTestsUpdatesStatusForSuccessAndFailure() throws Exception {
        TestRunnerComponent component = new TestRunnerComponent();
        component.add(new SimpleTest("PassingTest", true, true, null), new SimpleTest("FailingTest", false, false, null));
        Form form = component.showForm();
        assertNotNull(form);
        assertSame(form, currentForm);

        component.runTests();
        flushSerialCalls();

        Container resultsPane = getResultsPane(component);
        assertEquals(3, resultsPane.getComponentCount());
        Button first = (Button) resultsPane.getComponentAt(1);
        Button second = (Button) resultsPane.getComponentAt(2);

        assertEquals("PassingTest: Passed", first.getText());
        assertEquals(0x00ff00, first.getUnselectedStyle().getBgColor());
        assertEquals("FailingTest: Failed", second.getText());
        assertEquals(0xff0000, second.getUnselectedStyle().getBgColor());
    }

    @Test
    void showFormCreatesAndReusesHostForm() {
        TestRunnerComponent component = new TestRunnerComponent();
        Form first = component.showForm();
        Form second = component.showForm();

        assertSame(first, second);
        assertTrue(first.contains(component));
    }

    @Test
    void runTestsAddsFailureActionListenerOnException() throws Exception {
        RuntimeException failure = new RuntimeException("explode");
        TestRunnerComponent component = new TestRunnerComponent();
        component.add(new SimpleTest("Explosive", true, true, failure));

        RecordingLog recordingLog = new RecordingLog();
        Log originalLog = replaceLog(recordingLog);

        Form form = component.showForm();
        assertNotNull(form);

        try {
            assertDoesNotThrow(component::runTests);
            flushSerialCalls();

            Container resultsPane = getResultsPane(component);
            assertEquals(2, resultsPane.getComponentCount());
            Button status = (Button) resultsPane.getComponentAt(1);
            assertEquals("Explosive: Failed", status.getText());

            ActionListener failureListener = null;
            for (Object listener : status.getListeners()) {
                if (listener instanceof ActionListener) {
                    ActionListener candidate = (ActionListener) listener;
                    candidate.actionPerformed(new ActionEvent(status));
                    if (recordingLog.loggedThrowable != null) {
                        failureListener = candidate;
                        break;
                    }
                }
            }
            assertNotNull(failureListener, "failure action listener should be installed");
            assertSame(failure, recordingLog.loggedThrowable, "failure should be forwarded to Log.e");
        } finally {
            restoreLog(originalLog);
        }
    }

    private Container getResultsPane(TestRunnerComponent component) throws Exception {
        Field field = TestRunnerComponent.class.getDeclaredField("resultsPane");
        field.setAccessible(true);
        return (Container) field.get(component);
    }

    private Log replaceLog(Log replacement) throws Exception {
        Field field = Log.class.getDeclaredField("instance");
        field.setAccessible(true);
        Log original = (Log) field.get(null);
        field.set(null, replacement);
        return original;
    }

    private void restoreLog(Log original) throws Exception {
        Field field = Log.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, original);
    }

    private static class SimpleTest extends AbstractTest {
        private final String name;
        private final boolean shouldExecuteOnEDT;
        private final boolean result;
        private final RuntimeException toThrow;
        SimpleTest(String name, boolean shouldExecuteOnEDT, boolean result, RuntimeException toThrow) {
            this.name = name;
            this.shouldExecuteOnEDT = shouldExecuteOnEDT;
            this.result = result;
            this.toThrow = toThrow;
        }

        @Override
        public boolean runTest() {
            if (toThrow != null) {
                throw toThrow;
            }
            return result;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return shouldExecuteOnEDT;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class RecordingLog extends Log {
        private Throwable loggedThrowable;

        @Override
        protected void logThrowable(Throwable t) {
            loggedThrowable = t;
        }
    }
}

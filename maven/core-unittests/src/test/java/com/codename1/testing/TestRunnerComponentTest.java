package com.codename1.testing;

import com.codename1.junit.FormTest;
import com.codename1.junit.TestLogger;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionListener;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class TestRunnerComponentTest extends UITestBase {
    @FormTest
    void runTestsUpdatesStatusForSuccessAndFailure() throws Exception {
        TestRunnerComponent component = new TestRunnerComponent();
        component.add(new SimpleTest("PassingTest", true, true, null), new SimpleTest("FailingTest", false, false, null));
        Form form = component.showForm();
        assertNotNull(form);
        assertSame(form, implementation.getCurrentForm());

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

    @FormTest
    void showFormCreatesAndReusesHostForm() {
        TestRunnerComponent component = new TestRunnerComponent();
        Form first = component.showForm();
        Form second = component.showForm();

        assertSame(first, second);
        assertTrue(first.contains(component));
    }

    @FormTest
    void runTestsAddsFailureActionListenerOnException() throws Exception {
        TestLogger.install();
        RuntimeException failure = new RuntimeException("explode");
        TestRunnerComponent component = new TestRunnerComponent();
        component.add(new SimpleTest("Explosive", true, true, failure));

        Form form = component.showForm();
        assertNotNull(form);

        assertDoesNotThrow(component::runTests);
        flushSerialCalls();

        Container resultsPane = getResultsPane(component);
        assertEquals(2, resultsPane.getComponentCount());
        Button status = (Button) resultsPane.getComponentAt(1);
        assertEquals("Explosive: Failed", status.getText());

        boolean found = false;
        for (Object listener : status.getListeners()) {
            if (listener instanceof ActionListener) {
                found = true;
                break;
            }
        }
        assertTrue(found);
        assertEquals(1, TestLogger.getThrowables().size());
        TestLogger.remove();
    }

    private Container getResultsPane(TestRunnerComponent component) throws Exception {
        Field field = TestRunnerComponent.class.getDeclaredField("resultsPane");
        field.setAccessible(true);
        return (Container) field.get(component);
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
}

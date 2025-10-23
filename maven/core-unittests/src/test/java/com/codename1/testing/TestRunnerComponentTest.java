package com.codename1.testing;

import com.codename1.test.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
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

        Form form = component.showForm();
        assertNotNull(form);

        RuntimeException thrown = null;
        try {
            component.runTests();
            fail("Expected component.runTests() to propagate the runtime failure");
        } catch (RuntimeException e) {
            thrown = e;
        }
        assertNotNull(thrown);
        assertEquals(failure.getMessage(), thrown.getMessage());
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

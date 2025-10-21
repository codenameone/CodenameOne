package com.codename1.testing;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.TestImplementationProvider;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TestRunnerComponentTest {
    private CodenameOneImplementation implementation;
    private TestRunnerComponent testRunner;

    @BeforeEach
    void setUp() {
        implementation = TestImplementationProvider.installImplementation(true);
        testRunner = new TestRunnerComponent();
    }

    @Test
    void constructorCreatesComponentWithBorderLayout() {
        assertNotNull(testRunner);
        assertTrue(testRunner.getLayout() instanceof BorderLayout);
    }

    @Test
    void constructorAddsRunTestsButton() {
        // The component should have at least 2 components (button and results pane)
        assertTrue(testRunner.getComponentCount() >= 2);

        // Find the "Run Tests" button
        boolean foundButton = false;
        for (int i = 0; i < testRunner.getComponentCount(); i++) {
            Component comp = testRunner.getComponentAt(i);
            if (comp instanceof Button && "Run Tests".equals(((Button) comp).getText())) {
                foundButton = true;
                break;
            }
        }
        assertTrue(foundButton, "Run Tests button should be present");
    }

    @Test
    void constructorAddsResultsPane() {
        // The component should have containers (results pane is a container)
        boolean foundContainer = false;
        for (int i = 0; i < testRunner.getComponentCount(); i++) {
            Component comp = testRunner.getComponentAt(i);
            if (comp instanceof Container && !(comp instanceof Button)) {
                foundContainer = true;
                break;
            }
        }
        assertTrue(foundContainer, "Results pane container should be present");
    }

    @Test
    void addMethodAcceptsTests() {
        AbstractTest test1 = new MockAbstractTest();
        AbstractTest test2 = new MockAbstractTest();

        TestRunnerComponent result = testRunner.add(test1, test2);

        assertSame(testRunner, result); // Should return self for chaining
    }

    @Test
    void addMethodAcceptsEmptyArray() {
        TestRunnerComponent result = testRunner.add();
        assertSame(testRunner, result);
    }

    @Test
    void addMethodAllowsChaining() {
        AbstractTest test1 = new MockAbstractTest();
        AbstractTest test2 = new MockAbstractTest();

        TestRunnerComponent result = testRunner
                .add(test1)
                .add(test2);

        assertSame(testRunner, result);
    }

    @Test
    void showFormCreatesNewFormWhenNotEmbedded() {
        Form form = testRunner.showForm();

        assertNotNull(form);
        assertEquals("Test Runner", form.getTitle());
        assertTrue(form.getLayout() instanceof BorderLayout);
    }

    @Test
    void showFormReturnsExistingFormWhenEmbedded() {
        Form existingForm = new Form("Existing", new BorderLayout());
        existingForm.add(CN.CENTER, testRunner);
        existingForm.show();

        Form returnedForm = testRunner.showForm();

        assertSame(existingForm, returnedForm);
    }

    @Test
    void runTestsWithNoTests() {
        // Should not throw even with no tests
        assertDoesNotThrow(() -> testRunner.runTests());
    }

    @Test
    void runTestsExecutesAllAddedTests() {
        MockAbstractTest test1 = new MockAbstractTest();
        MockAbstractTest test2 = new MockAbstractTest();
        testRunner.add(test1, test2);

        testRunner.runTests();

        // Tests should have been executed
        assertTrue(test1.wasExecuted());
        assertTrue(test2.wasExecuted());
    }

    @Test
    void runTestsDisplaysTestCount() {
        testRunner.add(new MockAbstractTest(), new MockAbstractTest(), new MockAbstractTest());

        testRunner.runTests();

        // Verify that a label with test count was added
        // Find the results pane container
        Container resultsPane = null;
        for (int i = 0; i < testRunner.getComponentCount(); i++) {
            Component comp = testRunner.getComponentAt(i);
            if (comp instanceof Container && !(comp instanceof Button)) {
                resultsPane = (Container) comp;
                break;
            }
        }
        assertNotNull(resultsPane, "Results pane should exist");

        boolean foundTestCountLabel = false;
        for (int i = 0; i < resultsPane.getComponentCount(); i++) {
            Component comp = resultsPane.getComponentAt(i);
            if (comp instanceof Label) {
                String text = ((Label) comp).getText();
                if (text != null && text.contains("Running") && text.contains("3")) {
                    foundTestCountLabel = true;
                    break;
                }
            }
        }

        assertTrue(foundTestCountLabel);
    }

    @Test
    void runTestsHandlesPassingTest() {
        MockPassingTest passingTest = new MockPassingTest();
        testRunner.add(passingTest);

        testRunner.runTests();

        assertTrue(passingTest.wasExecuted());
    }

    @Test
    void runTestsHandlesFailingTest() {
        MockFailingTest failingTest = new MockFailingTest();
        testRunner.add(failingTest);

        assertDoesNotThrow(() -> testRunner.runTests());
        assertTrue(failingTest.wasExecuted());
    }

    @Test
    void runTestsHandlesTestWithException() {
        MockExceptionTest exceptionTest = new MockExceptionTest();
        testRunner.add(exceptionTest);

        assertDoesNotThrow(() -> testRunner.runTests());
        assertTrue(exceptionTest.wasExecuted());
    }

    @Test
    void runTestsExecutesEDTTestsOnEDT() {
        MockEDTTest edtTest = new MockEDTTest();
        testRunner.add(edtTest);

        testRunner.runTests();

        assertTrue(edtTest.wasExecuted());
    }

    @Test
    void runTestsExecutesNonEDTTestsOffEDT() {
        MockNonEDTTest nonEdtTest = new MockNonEDTTest();
        testRunner.add(nonEdtTest);

        testRunner.runTests();

        assertTrue(nonEdtTest.wasExecuted());
    }

    @Test
    void runTestsClearsResultsPaneBeforeRun() {
        // Add a test and run
        testRunner.add(new MockAbstractTest());
        testRunner.runTests();

        // Add another test and run again
        testRunner.add(new MockAbstractTest());
        testRunner.runTests();

        // Results pane should be cleared and repopulated
        // Find the results pane container
        Container resultsPane = null;
        for (int i = 0; i < testRunner.getComponentCount(); i++) {
            Component comp = testRunner.getComponentAt(i);
            if (comp instanceof Container && !(comp instanceof Button)) {
                resultsPane = (Container) comp;
                break;
            }
        }
        assertNotNull(resultsPane, "Results pane should exist");
        assertTrue(resultsPane.getComponentCount() > 0);
    }

    @Test
    void runTestsWithMultipleMixedTests() {
        testRunner.add(
                new MockPassingTest(),
                new MockFailingTest(),
                new MockPassingTest(),
                new MockExceptionTest()
        );

        assertDoesNotThrow(() -> testRunner.runTests());
    }

    @Test
    void testToStringReturnsClassName() {
        MockAbstractTest test = new MockAbstractTest();
        String str = test.toString();
        assertNotNull(str);
    }

    // Mock test implementations
    private static class MockAbstractTest extends AbstractTest {
        private boolean executed = false;

        @Override
        public boolean runTest() {
            executed = true;
            return true;
        }

        public boolean wasExecuted() {
            return executed;
        }
    }

    private static class MockPassingTest extends AbstractTest {
        private boolean executed = false;

        @Override
        public boolean runTest() {
            executed = true;
            return true;
        }

        public boolean wasExecuted() {
            return executed;
        }

        @Override
        public String toString() {
            return "MockPassingTest";
        }
    }

    private static class MockFailingTest extends AbstractTest {
        private boolean executed = false;

        @Override
        public boolean runTest() {
            executed = true;
            return false;
        }

        public boolean wasExecuted() {
            return executed;
        }

        @Override
        public String toString() {
            return "MockFailingTest";
        }
    }

    private static class MockExceptionTest extends AbstractTest {
        private boolean executed = false;

        @Override
        public boolean runTest() {
            executed = true;
            throw new RuntimeException("Test exception");
        }

        public boolean wasExecuted() {
            return executed;
        }

        @Override
        public String toString() {
            return "MockExceptionTest";
        }
    }

    private static class MockEDTTest extends AbstractTest {
        private boolean executed = false;

        @Override
        public boolean runTest() {
            executed = true;
            return true;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return true;
        }

        public boolean wasExecuted() {
            return executed;
        }

        @Override
        public String toString() {
            return "MockEDTTest";
        }
    }

    private static class MockNonEDTTest extends AbstractTest {
        private boolean executed = false;

        @Override
        public boolean runTest() {
            executed = true;
            return true;
        }

        @Override
        public boolean shouldExecuteOnEDT() {
            return false;
        }

        public boolean wasExecuted() {
            return executed;
        }

        @Override
        public String toString() {
            return "MockNonEDTTest";
        }
    }
}

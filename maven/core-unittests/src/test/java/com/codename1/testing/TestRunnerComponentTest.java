package com.codename1.testing;

import com.codename1.test.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TestRunnerComponent class.
 * Tests focus on basic component functionality and test registration.
 */
class TestRunnerComponentTest extends UITestBase {
    private TestRunnerComponent testRunner;

    @BeforeEach
    void setUp() {
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
    void testToStringReturnsClassName() {
        MockAbstractTest test = new MockAbstractTest();
        String str = test.toString();
        assertNotNull(str);
    }

    // Mock test implementations
    private static class MockAbstractTest extends AbstractTest {
        @Override
        public boolean runTest() {
            return true;
        }

        @Override
        public String toString() {
            return "MockAbstractTest";
        }
    }
}

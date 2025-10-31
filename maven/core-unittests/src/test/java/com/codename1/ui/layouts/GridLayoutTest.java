package com.codename1.ui.layouts;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridLayoutTest extends UITestBase {

    @Test
    void testConstructorWithRowsAndColumns() {
        GridLayout layout = new GridLayout(3, 4);
        assertEquals(3, layout.getRows());
        assertEquals(4, layout.getColumns());
    }

    @Test
    void testConstructorWithColumnsOnly() {
        GridLayout layout = new GridLayout(5);
        assertEquals(1, layout.getRows());
        assertEquals(5, layout.getColumns());
    }

    @Test
    void testConstructorWithLandscapeMode() {
        GridLayout layout = new GridLayout(2, 3, 3, 2);
        assertEquals(2, layout.getRows());
        assertEquals(3, layout.getColumns());
    }

    @Test
    void testConstructorThrowsExceptionForInvalidRows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GridLayout(0, 5);
        });
    }

    @Test
    void testConstructorThrowsExceptionForInvalidColumns() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GridLayout(5, 0);
        });
    }

    @Test
    void testConstructorThrowsExceptionForNegativeRows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GridLayout(-1, 5);
        });
    }

    @Test
    void testConstructorThrowsExceptionForNegativeColumns() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GridLayout(5, -1);
        });
    }

    @Test
    void testAutoFitFactory() {
        GridLayout layout = GridLayout.autoFit();
        assertTrue(layout.isAutoFit());
        assertEquals(1, layout.getRows());
        assertEquals(1, layout.getColumns());
    }

    @Test
    void testSetAndGetAutoFit() {
        GridLayout layout = new GridLayout(2, 2);
        assertFalse(layout.isAutoFit());

        layout.setAutoFit(true);
        assertTrue(layout.isAutoFit());

        layout.setAutoFit(false);
        assertFalse(layout.isAutoFit());
    }

    @Test
    void testSetAndGetFillLastRow() {
        GridLayout layout = new GridLayout(2, 2);
        assertFalse(layout.isFillLastRow());

        layout.setFillLastRow(true);
        assertTrue(layout.isFillLastRow());

        layout.setFillLastRow(false);
        assertFalse(layout.isFillLastRow());
    }

    @Test
    void testSetAndGetHideZeroSized() {
        GridLayout layout = new GridLayout(2, 2);
        assertFalse(layout.isHideZeroSized());

        layout.setHideZeroSized(true);
        assertTrue(layout.isHideZeroSized());

        layout.setHideZeroSized(false);
        assertFalse(layout.isHideZeroSized());
    }

    @Test
    void testToString() {
        GridLayout layout = new GridLayout(2, 3);
        assertEquals("GridLayout", layout.toString());
    }

    @Test
    void testEquals() {
        GridLayout layout1 = new GridLayout(2, 3);
        GridLayout layout2 = new GridLayout(2, 3);
        GridLayout layout3 = new GridLayout(3, 2);

        assertTrue(layout1.equals(layout2));
        assertFalse(layout1.equals(layout3));
    }

    @Test
    void testEqualsWithAutoFit() {
        GridLayout layout1 = new GridLayout(2, 2);
        GridLayout layout2 = new GridLayout(2, 2);

        layout1.setAutoFit(true);
        assertFalse(layout1.equals(layout2));

        layout2.setAutoFit(true);
        assertTrue(layout1.equals(layout2));
    }

    @Test
    void testEncloseInAutoFit() {
        Label label1 = new Label("1");
        Label label2 = new Label("2");

        Container container = GridLayout.encloseIn(label1, label2);

        assertTrue(container.getLayout() instanceof GridLayout);
        assertTrue(((GridLayout) container.getLayout()).isAutoFit());
        assertEquals(2, container.getComponentCount());
    }

    @Test
    void testEncloseInWithColumns() {
        Label label1 = new Label("1");
        Label label2 = new Label("2");
        Label label3 = new Label("3");

        Container container = GridLayout.encloseIn(2, label1, label2, label3);

        assertTrue(container.getLayout() instanceof GridLayout);
        assertEquals(2, ((GridLayout) container.getLayout()).getColumns());
        assertEquals(3, container.getComponentCount());
    }

    @Test
    void testPreferredSizeWithEmptyContainer() {
        GridLayout layout = new GridLayout(2, 2);
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 200));

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertEquals(0, preferredSize.getWidth());
        assertEquals(0, preferredSize.getHeight());
    }

    @Test
    void testPreferredSizeWithComponents() {
        GridLayout layout = new GridLayout(2, 2);
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 200));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertTrue(preferredSize.getWidth() > 0);
        assertTrue(preferredSize.getHeight() > 0);
    }

    @FormTest
    void testLayoutContainer2x2Grid() {
        GridLayout layout = new GridLayout(2, 2);
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        Button button1 = new Button("1");
        Button button2 = new Button("2");
        Button button3 = new Button("3");
        Button button4 = new Button("4");

        container.add(button1);
        container.add(button2);
        container.add(button3);
        container.add(button4);

        container.layoutContainer();

        // All buttons should have the same size
        assertEquals(button1.getWidth(), button2.getWidth());
        assertEquals(button1.getWidth(), button3.getWidth());
        assertEquals(button1.getWidth(), button4.getWidth());
        assertEquals(button1.getHeight(), button2.getHeight());
        assertEquals(button1.getHeight(), button3.getHeight());
        assertEquals(button1.getHeight(), button4.getHeight());

        // Buttons should be arranged in a grid
        assertEquals(button1.getY(), button2.getY());  // Same row
        assertEquals(button3.getY(), button4.getY());  // Same row
        assertEquals(button1.getX(), button3.getX());  // Same column
        assertEquals(button2.getX(), button4.getX());  // Same column
    }

    @Test
    void testLayoutContainerWithExtraComponents() {
        GridLayout layout = new GridLayout(2, 2);
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        // Add 5 components to a 2x2 grid
        for (int i = 0; i < 5; i++) {
            container.add(new Button("Button " + i));
        }

        // Should not throw exception - extra components should create additional rows
        assertDoesNotThrow(() -> container.layoutContainer());
    }

    @Test
    void testLayoutContainerWithFewerComponents() {
        GridLayout layout = new GridLayout(3, 3);
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        // Add only 5 components to a 3x3 grid
        for (int i = 0; i < 5; i++) {
            container.add(new Button("Button " + i));
        }

        // Should not throw exception - empty cells should be left blank
        assertDoesNotThrow(() -> container.layoutContainer());
    }

    @FormTest
    void testLayoutContainerWithSingleColumn() {
        GridLayout layout = new GridLayout(1);
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 400));

        Button button1 = new Button("1");
        Button button2 = new Button("2");

        container.add(button1);
        container.add(button2);

        container.layoutContainer();

        // Components should be in different rows (vertically stacked)
        assertTrue(button1.getY() < button2.getY());
    }

    @FormTest
    void testLayoutContainerWithEmptyContainer() {
        GridLayout layout = new GridLayout(2, 2);
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 200));

        // Should not throw exception with empty container
        assertDoesNotThrow(() -> container.layoutContainer());
    }

    @Test
    void testAutoFitLayoutAdjustsColumns() {
        GridLayout layout = GridLayout.autoFit();
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 200));

        // Add components with known preferred width
        for (int i = 0; i < 6; i++) {
            Button button = new Button("Btn");
            container.add(button);
        }

        container.layoutContainer();

        // Columns should be auto-calculated based on available width
        assertTrue(layout.getColumns() >= 1);
    }

    @Test
    void testFillLastRowBehavior() {
        GridLayout layout = new GridLayout(2, 3);
        layout.setFillLastRow(true);

        Container container = new Container(layout);
        container.setSize(new Dimension(300, 200));

        // Add 4 components (less than 2*3)
        for (int i = 0; i < 4; i++) {
            container.add(new Button("Button " + i));
        }

        // Should not throw exception
        assertDoesNotThrow(() -> container.layoutContainer());
    }

    @Test
    void testHideZeroSizedComponents() {
        GridLayout layout = new GridLayout(2, 2);
        layout.setHideZeroSized(true);

        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        Button button1 = new Button("1");
        Button button2 = new Button("2");
        button2.setHidden(true);

        container.add(button1);
        container.add(button2);

        Dimension preferredSize = layout.getPreferredSize(container);

        // Hidden component should not contribute to preferred size
        assertNotNull(preferredSize);
    }

    @Test
    void testObscuresPotentialWithExactMatch() {
        GridLayout layout = new GridLayout(2, 2);
        Container container = new Container(layout);

        // Add exactly 4 components (2*2)
        for (int i = 0; i < 4; i++) {
            container.add(new Button("Button " + i));
        }

        assertTrue(layout.obscuresPotential(container));
    }

    @Test
    void testObscuresPotentialWithAutoFit() {
        GridLayout layout = GridLayout.autoFit();
        Container container = new Container(layout);

        container.add(new Button("Button"));

        assertTrue(layout.obscuresPotential(container));
    }

    @Test
    void testObscuresPotentialWithFewerComponents() {
        GridLayout layout = new GridLayout(3, 3);
        Container container = new Container(layout);

        // Add only 5 components (less than 3*3)
        for (int i = 0; i < 5; i++) {
            container.add(new Button("Button " + i));
        }

        assertFalse(layout.obscuresPotential(container));
    }

    @FormTest
    void test1x1Grid() {
        GridLayout layout = new GridLayout(1, 1);
        Container container = new Container(layout);
        container.setSize(new Dimension(100, 100));

        Button button = new Button("Button");
        container.add(button);

        container.layoutContainer();

        // Single component should fill the entire container
        assertTrue(button.getWidth() > 0);
        assertTrue(button.getHeight() > 0);
    }

    @Test
    void test3x3GridWith9Components() {
        GridLayout layout = new GridLayout(3, 3);
        Container container = new Container(layout);
        container.setSize(new Dimension(300, 300));

        for (int i = 0; i < 9; i++) {
            container.add(new Button("" + i));
        }

        container.layoutContainer();

        // All components should have equal size
        int expectedWidth = container.getComponentAt(0).getWidth();
        int expectedHeight = container.getComponentAt(0).getHeight();

        for (int i = 1; i < 9; i++) {
            assertEquals(expectedWidth, container.getComponentAt(i).getWidth());
            assertEquals(expectedHeight, container.getComponentAt(i).getHeight());
        }
    }

    @FormTest
    void testGridLayoutWithVariedComponentSizes() {
        GridLayout layout = new GridLayout(2, 2);
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        // Add components with different preferred sizes
        Button smallButton = new Button("S");
        Button largeButton = new Button("Large Button Text");

        container.add(smallButton);
        container.add(largeButton);

        container.layoutContainer();

        // Grid layout should give them equal size regardless of preferred size
        assertEquals(smallButton.getWidth(), largeButton.getWidth());
        assertEquals(smallButton.getHeight(), largeButton.getHeight());
    }

    @Test
    void testPreferredSizeWithMultipleComponents() {
        GridLayout layout = new GridLayout(2, 2);
        Container container = new Container(layout);

        for (int i = 0; i < 4; i++) {
            container.add(new Button("Button " + i));
        }

        Dimension preferredSize = layout.getPreferredSize(container);

        // Preferred size should be positive
        assertTrue(preferredSize.getWidth() > 0);
        assertTrue(preferredSize.getHeight() > 0);
    }
}

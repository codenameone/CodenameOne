package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTest extends UITestBase {

    @Test
    void testPutClientPropertyStoresAndRemovesValues() {
        Component component = new Component();

        assertNull(component.getClientProperty("missing"));

        component.putClientProperty("key", "value");
        assertEquals("value", component.getClientProperty("key"));

        component.putClientProperty("key", null);
        assertNull(component.getClientProperty("key"));

        component.putClientProperty("another", Integer.valueOf(42));
        assertEquals(Integer.valueOf(42), component.getClientProperty("another"));

        component.clearClientProperties();
        assertNull(component.getClientProperty("another"));
    }

    @Test
    void testStripMarginAndPaddingResetsStyleAndBorder() {
        Component component = new Component();
        Style style = component.getAllStyles();

        style.setMargin(3, 4, 5, 6);
        style.setPadding(7, 8, 9, 10);
        style.setBorder(Border.createLineBorder(2));

        Component chained = component.stripMarginAndPadding();
        assertSame(component, chained);

        Style[] states = new Style[]{
                component.getUnselectedStyle(),
                component.getSelectedStyle(),
                component.getPressedStyle(),
                component.getDisabledStyle()
        };

        for (Style state : states) {
            assertEquals(0f, state.getMarginFloatValue(false, Component.TOP));
            assertEquals(0f, state.getMarginFloatValue(false, Component.BOTTOM));
            assertEquals(0f, state.getMarginFloatValue(false, Component.LEFT));
            assertEquals(0f, state.getMarginFloatValue(false, Component.RIGHT));

            assertEquals(0f, state.getPaddingFloatValue(false, Component.TOP));
            assertEquals(0f, state.getPaddingFloatValue(false, Component.BOTTOM));
            assertEquals(0f, state.getPaddingFloatValue(false, Component.LEFT));
            assertEquals(0f, state.getPaddingFloatValue(false, Component.RIGHT));

            assertTrue(state.getBorder().isEmptyBorder());
        }
    }

    @Test
    void testSetEnabledTriggersRepaintOnlyWhenStateChanges() {
        CountingComponent component = new CountingComponent();

        component.setEnabled(false);
        assertEquals(1, component.repaintCalls, "Disabling should trigger a repaint");

        component.setEnabled(false);
        assertEquals(1, component.repaintCalls, "Setting the same state should not repaint");

        component.setEnabled(true);
        assertEquals(2, component.repaintCalls, "Re-enabling should repaint exactly once");
    }

    private static class CountingComponent extends Component {
        int repaintCalls;

        @Override
        public void repaint() {
            repaintCalls++;
        }

        @Override
        public void repaint(int x, int y, int w, int h) {
            repaintCalls++;
        }

        @Override
        public Dimension getScrollDimension() {
            return new Dimension(0, 0);
        }
    }
}

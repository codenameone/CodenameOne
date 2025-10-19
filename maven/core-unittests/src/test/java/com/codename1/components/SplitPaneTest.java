package com.codename1.components;

import com.codename1.test.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class SplitPaneTest extends UITestBase {
    @BeforeEach
    void configureDisplay() {
        when(implementation.animateImage(any(), anyLong())).thenReturn(false);
        when(implementation.isAnimation(any())).thenReturn(false);
    }

    @Test
    void testComponentGettersAndSetters() {
        Label top = new Label("Top");
        Label bottom = new Label("Bottom");
        SplitPane pane = new SplitPane(SplitPane.HORIZONTAL_SPLIT, top, bottom, "0px", "0px", "100px");

        assertSame(top, pane.getTop());
        assertSame(top, pane.getLeft());
        assertSame(bottom, pane.getBottom());
        assertSame(bottom, pane.getRight());

        Label newBottom = new Label("NewBottom");
        pane.setBottom(newBottom);
        assertSame(newBottom, pane.getBottom());

        Label newTop = new Label("NewTop");
        pane.setTop(newTop);
        assertSame(newTop, pane.getTop());
    }

    @Test
    void testExpandCollapseAndToggleStates() throws Exception {
        SplitPane pane = new SplitPane(SplitPane.HORIZONTAL_SPLIT, new Label("Top"), new Label("Bottom"), "0px", "0px", "100px");
        setComponentSize(pane, 400, 400);
        Container divider = getDivider(pane);
        setComponentSize(divider, 20, 400);

        pane.collapse(true);
        assertTrue(isCollapsed(pane));
        assertFalse(isExpanded(pane));

        pane.expand();
        assertFalse(isCollapsed(pane));

        pane.expand(true);
        assertTrue(isExpanded(pane));

        pane.collapse();
        assertFalse(isExpanded(pane));

        pane.toggleCollapsePreferred();
        assertTrue(isCollapsed(pane));

        pane.toggleExpandPreferred();
        assertTrue(isExpanded(pane));
        assertFalse(isCollapsed(pane));
    }

    @Test
    void testSetInsetWithoutClamp() throws Exception {
        SplitPane pane = new SplitPane(SplitPane.HORIZONTAL_SPLIT, new Label("Top"), new Label("Bottom"), "0px", "0px", "100px");
        pane.setInset("25%", false);

        Object inset = getDividerInset(pane);
        Method getValueAsString = inset.getClass().getDeclaredMethod("getValueAsString");
        String value = (String) getValueAsString.invoke(inset);
        assertTrue(value.startsWith("25"));
        assertFalse(isCollapsed(pane));
        assertFalse(isExpanded(pane));
    }

    @Test
    void testSettingsAppliedToDivider() throws Exception {
        TestImage icon = new TestImage(10, 10);
        SplitPane.Settings settings = new SplitPane.Settings()
                .orientation(SplitPane.VERTICAL_SPLIT)
                .buttonUIIDs("MyButton")
                .collapseIcon(icon)
                .expandIcon(icon)
                .dragHandleIcon(icon)
                .collapseMaterialIcon('c')
                .expandMaterialIcon('e')
                .dragHandleMaterialIcon('d')
                .dividerUIID("DividerUIID")
                .dividerThicknessMM(7f)
                .showDragHandle(false)
                .showExpandCollapseButtons(false)
                .insets("10px", "20px", "30px");

        SplitPane pane = new SplitPane(settings, new Label("Top"), new Label("Bottom"));
        assertEquals("10px", pane.getMinInset());
        assertEquals("20px", pane.getPreferredInset());
        assertEquals("30px", pane.getMaxInset());

        Field expandField = SplitPane.class.getDeclaredField("expandButtonUIID");
        Field collapseField = SplitPane.class.getDeclaredField("collapseButtonUIID");
        Field dragField = SplitPane.class.getDeclaredField("dragHandleUIID");
        Field dividerUIIDField = SplitPane.class.getDeclaredField("dividerUIID");
        Field orientationField = SplitPane.class.getDeclaredField("orientation");

        expandField.setAccessible(true);
        collapseField.setAccessible(true);
        dragField.setAccessible(true);
        dividerUIIDField.setAccessible(true);
        orientationField.setAccessible(true);

        assertEquals("MyButton", expandField.get(pane));
        assertEquals("MyButton", collapseField.get(pane));
        assertEquals("MyButton", dragField.get(pane));
        assertEquals("DividerUIID", dividerUIIDField.get(pane));
        assertEquals(SplitPane.VERTICAL_SPLIT, orientationField.getInt(pane));

        Container divider = getDivider(pane);
        assertEquals("DividerUIID", divider.getUIID());
        assertEquals(0, divider.getComponentCount(), "Divider should hide buttons and drag handle when disabled");
    }

    private Object getDividerInset(SplitPane pane) throws Exception {
        Method m = SplitPane.class.getDeclaredMethod("getDividerInset");
        m.setAccessible(true);
        return m.invoke(pane);
    }

    private Container getDivider(SplitPane pane) throws Exception {
        Field dividerField = SplitPane.class.getDeclaredField("divider");
        dividerField.setAccessible(true);
        return (Container) dividerField.get(pane);
    }

    private boolean isCollapsed(SplitPane pane) throws Exception {
        Field collapsedField = SplitPane.class.getDeclaredField("isCollapsed");
        collapsedField.setAccessible(true);
        return collapsedField.getBoolean(pane);
    }

    private boolean isExpanded(SplitPane pane) throws Exception {
        Field expandedField = SplitPane.class.getDeclaredField("isExpanded");
        expandedField.setAccessible(true);
        return expandedField.getBoolean(pane);
    }

    private void setComponentSize(Component component, int width, int height) {
        component.setWidth(width);
        component.setHeight(height);
        if (component instanceof SplitPane) {
            ((SplitPane) component).layoutContainer();
        }
    }

    private static class TestImage extends Image {
        private final int width;
        private final int height;

        TestImage(int width, int height) {
            super(new Object());
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public Image scaled(int width, int height) {
            return new TestImage(width, height);
        }

        @Override
        public Image scaledSmallerRatio(int width, int height) {
            return new TestImage(width, height);
        }

        @Override
        public boolean animate() {
            return false;
        }
    }
}

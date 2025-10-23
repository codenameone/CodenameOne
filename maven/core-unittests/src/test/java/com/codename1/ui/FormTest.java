package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.Painter;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.FlowLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FormTest extends UITestBase {
    private boolean originalGlobalToolbar;

    @BeforeEach
    void prepareMocks() {
        originalGlobalToolbar = Toolbar.isGlobalToolbar();
        Toolbar.setGlobalToolbar(false);
        when(implementation.isNativeTitle()).thenReturn(false);
        when(implementation.getSoftkeyCount()).thenReturn(2);
        when(implementation.isThirdSoftButton()).thenReturn(false);
    }

    @AfterEach
    void restoreToolbarFlag() {
        Toolbar.setGlobalToolbar(originalGlobalToolbar);
    }

    @Test
    void testContentPaneConfiguredWithFlowLayoutAndScrollable() {
        TestForm form = new TestForm();
        Container content = form.getContentPane();
        assertTrue(content.getLayout() instanceof FlowLayout);
        assertFalse(content.isScrollableY());
        assertEquals("ContentPane", content.getUIID());
    }

    @Test
    void testAddComponentDelegatesToContentPane() {
        TestForm form = new TestForm();
        Label label = new Label("Child");
        form.addComponent(label);
        assertSame(form.getContentPane(), label.getParent());
        assertEquals(1, form.getContentPane().getComponentCount());
    }

    @Test
    void testSetMenuBarInvokesInitialization() {
        TestForm form = new TestForm();
        TrackingMenuBar menuBar = new TrackingMenuBar();
        form.setMenuBar(menuBar);
        assertSame(menuBar, form.getMenuBar());
        assertSame(form, menuBar.getParentForm());
        assertTrue(menuBar.initCalled);
    }

    @Test
    void testSetToolbarBindsMenuBar() {
        TestForm form = new TestForm();
        TrackingMenuBar menuBar = new TrackingMenuBar();
        TrackingToolbar toolbar = new TrackingToolbar(menuBar);
        form.setToolbar(toolbar);
        assertSame(toolbar, form.getToolbar());
        assertSame(menuBar, form.getMenuBar());
        assertTrue(menuBar.initCalled);
    }

    @Test
    void testPropertyAccessorsUpdateTitleUiids() {
        TestForm form = new TestForm();
        assertArrayEquals(new String[]{"titleUIID", "titleAreaUIID"}, form.getPropertyNames());
        assertArrayEquals(new Class[]{String.class, String.class}, form.getPropertyTypes());
        assertArrayEquals(new String[]{"String", "String"}, form.getPropertyTypeNames());

        form.setPropertyValue("titleUIID", "CustomTitle");
        assertEquals("CustomTitle", form.getTitleComponent().getUIID());

        form.setPropertyValue("titleAreaUIID", "CustomTitleArea");
        assertEquals("CustomTitleArea", form.getTitleArea().getUIID());
    }

    @Test
    void testSetTitleUpdatesLabelWhenNoToolbar() {
        TestForm form = new TestForm();
        form.setTitle("Dashboard");
        assertEquals("Dashboard", form.getTitle());
    }

    @Test
    void testSetGlassPaneTriggersRepaint() {
        RepaintTrackingForm form = new RepaintTrackingForm();
        Painter painter = (g, rect) -> { };
        form.setGlassPane(painter);
        assertTrue(form.repaintTriggered);
        assertSame(painter, form.getGlassPane());
    }

    @Test
    void testSetTitleComponentWithTransitionReplacesComponent() {
        TestForm form = new TestForm();
        Label original = form.getTitleComponent();
        Label replacement = new Label("Replacement");
        Transition transition = new Transition() {
            public boolean animate() {
                return false;
            }

            public void paint(Graphics g) {
            }
        };

        form.setTitleComponent(replacement, transition);
        assertSame(replacement, form.getTitleComponent());
        assertSame(form.getTitleArea(), replacement.getParent());
        assertNull(original.getParent());
    }

    @Test
    void testAddAndRemoveKeyListenerManageInternalMap() throws Exception {
        TestForm form = new TestForm();
        ActionListener<ActionEvent> listener = evt -> { };

        form.addKeyListener(42, listener);
        HashMap<Integer, ArrayList<ActionListener>> listeners = extractKeyListeners(form);
        assertNotNull(listeners);
        assertTrue(listeners.containsKey(Integer.valueOf(42)));

        form.removeKeyListener(42, listener);
        HashMap<Integer, ArrayList<ActionListener>> after = extractKeyListeners(form);
        assertNotNull(after);
        assertFalse(after.containsKey(Integer.valueOf(42)));
        assertTrue(after.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private HashMap<Integer, ArrayList<ActionListener>> extractKeyListeners(Form form) throws Exception {
        Field field = Form.class.getDeclaredField("keyListeners");
        field.setAccessible(true);
        HashMap<Integer, ArrayList<ActionListener>> value = (HashMap<Integer, ArrayList<ActionListener>>) field.get(form);
        if (value == null) {
            return new HashMap<Integer, ArrayList<ActionListener>>();
        }
        return value;
    }

    private static class TestForm extends Form {
        TestForm() {
            super();
        }

        @Override
        protected void initGlobalToolbar() {
        }
    }

    private static class RepaintTrackingForm extends TestForm {
        boolean repaintTriggered;

        @Override
        public void repaint() {
            repaintTriggered = true;
            super.repaint();
        }
    }

    private static class TrackingMenuBar extends MenuBar {
        boolean initCalled;

        @Override
        protected void initMenuBar(Form parent) {
            initCalled = true;
            super.initMenuBar(parent);
        }
    }

    private static class TrackingToolbar extends Toolbar {
        private final MenuBar menuBar;

        TrackingToolbar(MenuBar menuBar) {
            super();
            this.menuBar = menuBar;
        }

        @Override
        public MenuBar getMenuBar() {
            return menuBar;
        }
    }
}

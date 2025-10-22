package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.layouts.FlowLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertTrue(content.isScrollableY());
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

    private static class TestForm extends Form {
        TestForm() {
            super();
        }

        @Override
        protected void initGlobalToolbar() {
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

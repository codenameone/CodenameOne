package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class MenuBarTest extends UITestBase {
    private boolean originalGlobalToolbar;
    private boolean originalTouchScreen;

    @BeforeEach
    void configureImplementation() {
        originalGlobalToolbar = Toolbar.isGlobalToolbar();
        Toolbar.setGlobalToolbar(false);
        Display display = Display.getInstance();
        originalTouchScreen = display.isTouchScreenDevice();
        display.setTouchScreenDevice(true);
    }

    @AfterEach
    void restoreGlobalToolbar() {
        Toolbar.setGlobalToolbar(originalGlobalToolbar);
        Display.getInstance().setTouchScreenDevice(originalTouchScreen);
    }

    @FormTest
    void testInitMenuBarAssignsParentAndCreatesButtons() {
        TestForm form = new TestForm();
        MenuBar menuBar = new MenuBar();
        menuBar.initMenuBar(form);

        assertSame(form, menuBar.getParentForm());
        assertTrue(menuBar.getComponentCount() >= 1);
        assertNotNull(menuBar.getMenuStyle());
        assertFalse(menuBar.isMenuShowing());
    }

    @FormTest
    void testSetSelectAndBackCommands() {
        TestForm form = new TestForm();
        MenuBar menuBar = new MenuBar();
        menuBar.initMenuBar(form);

        Command select = new Command("Select");
        menuBar.setSelectCommand(select);
        assertSame(select, menuBar.getSelectCommand());

        Command back = new Command("Back");
        menuBar.setBackCommand(back);
        assertSame(back, menuBar.getBackCommand());
    }

    @FormTest
    void testSetTransitionsStoresValues() throws Exception {
        TestForm form = new TestForm();
        MenuBar menuBar = new MenuBar();
        menuBar.initMenuBar(form);

        Transition in = CommonTransitions.createFade(300);
        Transition out = CommonTransitions.createFade(200);
        menuBar.setTransitions(in, out);

        Field inField = MenuBar.class.getDeclaredField("transitionIn");
        Field outField = MenuBar.class.getDeclaredField("transitionOut");
        inField.setAccessible(true);
        outField.setAccessible(true);

        assertSame(in, inField.get(menuBar));
        assertSame(out, outField.get(menuBar));
    }

    @FormTest
    void testMenuCellRendererSetterStoresRenderer() throws Exception {
        TestForm form = new TestForm();
        MenuBar menuBar = new MenuBar();
        menuBar.initMenuBar(form);

        ListCellRenderer renderer = new ListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(List list, Object value, int index, boolean isSelected) {
                return new Label(String.valueOf(value));
            }

            @Override
            public Component getListFocusComponent(List list) {
                return new Label("focus");
            }
        };
        menuBar.setMenuCellRenderer(renderer);

        Field rendererField = MenuBar.class.getDeclaredField("menuCellRenderer");
        rendererField.setAccessible(true);
        assertSame(renderer, rendererField.get(menuBar));
    }

    @FormTest
    void testStaticSoftKeyDetectionUsesConfiguredCodes() throws Exception {
        Field leftField = MenuBar.class.getDeclaredField("leftSK");
        Field rightField = MenuBar.class.getDeclaredField("rightSK");
        Field right2Field = MenuBar.class.getDeclaredField("rightSK2");
        Field backField = MenuBar.class.getDeclaredField("backSK");
        Field clearField = MenuBar.class.getDeclaredField("clearSK");
        Field backspaceField = MenuBar.class.getDeclaredField("backspaceSK");
        leftField.setAccessible(true);
        rightField.setAccessible(true);
        right2Field.setAccessible(true);
        backField.setAccessible(true);
        clearField.setAccessible(true);
        backspaceField.setAccessible(true);

        int originalLeft = leftField.getInt(null);
        int originalRight = rightField.getInt(null);
        int originalRight2 = right2Field.getInt(null);
        int originalBack = backField.getInt(null);
        int originalClear = clearField.getInt(null);
        int originalBackspace = backspaceField.getInt(null);

        try {
            leftField.setInt(null, 10);
            rightField.setInt(null, 11);
            right2Field.setInt(null, 11);
            backField.setInt(null, 12);
            clearField.setInt(null, 13);
            backspaceField.setInt(null, 14);

            assertTrue(MenuBar.isLSK(10));
            assertTrue(MenuBar.isRSK(11));
            assertFalse(MenuBar.isLSK(0));
            assertFalse(MenuBar.isRSK(9));
        } finally {
            leftField.setInt(null, originalLeft);
            rightField.setInt(null, originalRight);
            right2Field.setInt(null, originalRight2);
            backField.setInt(null, originalBack);
            clearField.setInt(null, originalClear);
            backspaceField.setInt(null, originalBackspace);
        }
    }

    private static class TestForm extends Form {
        TestForm() {
            super(new FlowLayout());
        }

        @Override
        protected void initGlobalToolbar() {
        }

        @Override
        public UIManager getUIManager() {
            UIManager manager = super.getUIManager();
            if (manager.getLookAndFeel() == null) {
                manager.setLookAndFeel(new DefaultLookAndFeel(manager));
            }
            return manager;
        }
    }
}

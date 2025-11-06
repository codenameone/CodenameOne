package com.codename1.ui;

import com.codename1.components.InteractionDialog;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolbarTest extends UITestBase {
    private boolean originalOnTop;
    private boolean originalCentered;

    @BeforeEach
    void captureStatics() {
        originalOnTop = Toolbar.isOnTopSideMenu();
        originalCentered = Toolbar.isCenteredDefault();
    }

    @AfterEach
    void restoreStatics() {
        Toolbar.setOnTopSideMenu(originalOnTop);
        Toolbar.setCenteredDefault(originalCentered);
    }

    @FormTest
    void openingAndClosingSideMenuUpdatesState() {
        implementation.setBuiltinSoundsEnabled(false);

        Toolbar.setOnTopSideMenu(true);

        Form form = new Form("Toolbar SideMenu", new BorderLayout());
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        toolbar.addCommandToSideMenu("Menu", null, evt -> {
        });
        form.show();
        form.getAnimationManager().flush();

        assertFalse(toolbar.isSideMenuShowing(), "Side menu should be hidden initially");

        toolbar.openSideMenu();
        form.getAnimationManager().flush();

        assertTrue(toolbar.isSideMenuShowing(), "Side menu should be showing after open");
        assertTrue(SideMenuBar.isShowing(), "SideMenuBar should report showing when toolbar menu open");

        toolbar.closeSideMenu();
        form.getAnimationManager().flush();

        assertFalse(toolbar.isSideMenuShowing(), "Side menu should be hidden after close");
        assertFalse(SideMenuBar.isShowing(), "SideMenuBar should no longer report showing");
    }

    @FormTest
    void titleCenteringFollowsStaticSetting() {
        implementation.setBuiltinSoundsEnabled(false);

        Toolbar.setCenteredDefault(false);

        Form form = new Form("Title", new BorderLayout());
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        form.show();
        form.getAnimationManager().flush();

        assertFalse(toolbar.isTitleCentered(), "Title should not be centered when centeredDefault is false");

        toolbar.setTitle("Hello");
        toolbar.setTitleCentered(true);

        assertTrue(toolbar.isTitleCentered(), "setTitleCentered(true) should center the title");
    }

    @FormTest
    void sideMenuCommandFiresActionListener() {
        implementation.setBuiltinSoundsEnabled(false);

        Toolbar.setOnTopSideMenu(true);

        Form form = new Form("Command Action", new BorderLayout());
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        final int[] invocation = {0};
        Command cmd = toolbar.addCommandToSideMenu("Execute", null, evt -> invocation[0]++);
        form.show();
        form.getAnimationManager().flush();

        toolbar.openSideMenu();
        form.getAnimationManager().flush();

        Container layered = toolbar.getComponentForm().getFormLayeredPane(Toolbar.class, false);
        InteractionDialog dialog = null;
        List<Component> children = layered.getChildrenAsList(true);
        for (Component cmp : children) {
            if (cmp instanceof InteractionDialog) {
                dialog = (InteractionDialog) cmp;
                break;
            }
        }
        assertNotNull(dialog, "Side menu dialog should be present");

        Button commandButton = findCommandButton(dialog, cmd);
        assertNotNull(commandButton, "Command button should exist in side menu");

        int x = commandButton.getAbsoluteX() + commandButton.getWidth() / 2;
        int y = commandButton.getAbsoluteY() + commandButton.getHeight() / 2;
        dialog.pointerPressed(x, y);
        commandButton.pointerPressed(x, y);
        commandButton.pointerReleased(x, y);
        dialog.pointerReleased(x, y);

        form.getAnimationManager().flush();

        assertEquals(1, invocation[0], "Command listener should be invoked when side menu button pressed");

        toolbar.closeSideMenu();
        form.getAnimationManager().flush();
    }

    private Button findCommandButton(InteractionDialog dialog, Command target) {
        Container content = dialog.getContentPane();
        for (int i = 0; i < content.getComponentCount(); i++) {
            Component cmp = content.getComponentAt(i);
            Button found = searchForButton(cmp, target);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Button searchForButton(Component cmp, Command target) {
        if (cmp instanceof Button) {
            Button button = (Button) cmp;
            if (button.getCommand() == target) {
                return button;
            }
        }
        if (cmp instanceof Container) {
            Container container = (Container) cmp;
            for (int i = 0; i < container.getComponentCount(); i++) {
                Button found = searchForButton(container.getComponentAt(i), target);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

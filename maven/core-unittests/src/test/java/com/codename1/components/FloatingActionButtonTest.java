package com.codename1.components;

import com.codename1.junit.EdtTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FloatingActionButtonTest extends UITestBase {

    private boolean originalAutoSizing;
    private float originalDefaultSize;

    @BeforeEach
    void captureDefaults() {
        originalAutoSizing = FloatingActionButton.isAutoSizing();
        originalDefaultSize = FloatingActionButton.getIconDefaultSize();
    }

    @AfterEach
    void restoreDefaults() {
        FloatingActionButton.setAutoSizing(originalAutoSizing);
        FloatingActionButton.setIconDefaultSize(originalDefaultSize);
    }

    @EdtTest
    void autoSizingUsesIconDimensions() {
        FloatingActionButton.setAutoSizing(true);
        FloatingActionButton fab = new FloatingActionButton(FontImage.MATERIAL_ADD, null, FloatingActionButton.getIconDefaultSize());
        int expectedWidth = fab.getIcon().getWidth() * 11 / 4;
        int expectedHeight = fab.getIcon().getHeight() * 11 / 4;
        assertEquals(expectedWidth, fab.getPreferredSize().getWidth());
        assertEquals(expectedHeight, fab.getPreferredSize().getHeight());
    }

    @EdtTest
    void createSubFabStoresButtonsInMenu() throws Exception {
        FloatingActionButton fab = new FloatingActionButton(FontImage.MATERIAL_ADD, null, FloatingActionButton.getIconDefaultSize());
        FloatingActionButton first = fab.createSubFAB(FontImage.MATERIAL_CAMERA, "Camera");
        FloatingActionButton second = fab.createSubFAB(FontImage.MATERIAL_CHAT, "Chat");

        List<FloatingActionButton> subMenu = getSubMenu(fab);
        assertEquals(2, subMenu.size());
        assertSame(first, subMenu.get(0));
        assertSame(second, subMenu.get(1));
    }

    @EdtTest
    void popupContentCreatesTextActionsThatTriggerSubFab() throws Exception {
        FloatingActionButton fab = new FloatingActionButton(FontImage.MATERIAL_ADD, null, FloatingActionButton.getIconDefaultSize());
        fab.setFloatingActionTextUIID("PopupText");
        fab.setWidth(120);

        TrackingSubFab subFab = new TrackingSubFab(FontImage.MATERIAL_EMAIL, "Send");
        List<FloatingActionButton> subs = new ArrayList<>();
        subs.add(subFab);
        setSubMenu(fab, subs);

        Container content = fab.createPopupContent(subs);
        assertEquals(1, content.getComponentCount());
        Button textButton = findButtonByText(content, "Send");
        assertNotNull(textButton);
        assertEquals("PopupText", textButton.getUIID());

        for (Object listenerObj : textButton.getListeners()) {
            ((com.codename1.ui.events.ActionListener) listenerObj).actionPerformed(new com.codename1.ui.events.ActionEvent(textButton));
        }

        assertTrue(subFab.pressedCalled);
        assertTrue(subFab.releasedCalled);
    }

    @EdtTest
    void badgeCreationKeepsTextAndUiid() {
        FloatingActionButton badge = FloatingActionButton.createBadge("3");
        assertEquals("Badge", badge.getUIID());
        badge.setText("7");
        assertEquals("7", badge.getText());
    }

    private Button findButtonByText(Component component, String text) {
        if (component instanceof Button) {
            Button button = (Button) component;
            if (text.equals(button.getText())) {
                return button;
            }
        }
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container) {
                Button result = findButtonByText(child, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<FloatingActionButton> getSubMenu(FloatingActionButton fab) throws Exception {
        Field field = FloatingActionButton.class.getDeclaredField("subMenu");
        field.setAccessible(true);
        return (List<FloatingActionButton>) field.get(fab);
    }

    private void setSubMenu(FloatingActionButton fab, List<FloatingActionButton> subMenu) throws Exception {
        Field field = FloatingActionButton.class.getDeclaredField("subMenu");
        field.setAccessible(true);
        field.set(fab, subMenu);
    }

    private static class TrackingSubFab extends FloatingActionButton {
        boolean pressedCalled;
        boolean releasedCalled;

        TrackingSubFab(char icon, String text) {
            super(icon, text, 2.8f);
        }

        @Override
        public void pressed() {
            pressedCalled = true;
            super.pressed();
        }

        @Override
        public void released() {
            releasedCalled = true;
            super.released();
        }
    }
}

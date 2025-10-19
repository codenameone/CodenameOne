package com.codename1.components;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class InteractionDialogTest extends ComponentTestBase {

    @BeforeEach
    void stubOrientation() {
        when(implementation.isPortrait()).thenReturn(true);
    }

    @Test
    void constructorInitializesTitleAndContentPane() {
        InteractionDialog dialog = new InteractionDialog("Hello");
        assertEquals("Hello", dialog.getTitle());
        assertEquals("Dialog", dialog.getUIID());
        assertEquals("DialogTitle", dialog.getTitleComponent().getUIID());
        assertEquals("DialogContentPane", dialog.getContentPane().getUIID());
    }

    @Test
    void addComponentDelegatesToContentPane() {
        InteractionDialog dialog = new InteractionDialog();
        Label content = new Label("Body");
        dialog.addComponent(content);
        assertEquals(1, dialog.getContentPane().getComponentCount());
        assertSame(content, dialog.getContentPane().getComponentAt(0));
    }

    @Test
    void showPlacesDialogOnLayeredPane() {
        Form form = new Form(new BorderLayout());
        when(implementation.getCurrentForm()).thenReturn(form);
        InteractionDialog dialog = new InteractionDialog("Title");
        dialog.setAnimateShow(false);
        dialog.show(10, 20, 30, 40);
        assertTrue(dialog.isShowing());
        Container layered = form.getLayeredPane(InteractionDialog.class, true);
        assertTrue(layered.contains(dialog));
        dialog.dispose();
        assertFalse(dialog.isShowing());
    }

    @Test
    void showPopupDialogUpdatesUiidsAndUsesLayeredPane() throws Exception {
        Form form = new Form(new BorderLayout());
        when(implementation.getCurrentForm()).thenReturn(form);
        InteractionDialog dialog = new InteractionDialog();
        dialog.setAnimateShow(false);
        Rectangle rect = new Rectangle(20, 30, 80, 60);
        dialog.showPopupDialog(rect);
        assertEquals("PopupDialog", dialog.getUIID());
        assertEquals("PopupDialogTitle", dialog.getTitleComponent().getUIID());
        assertEquals("PopupContentPane", dialog.getContentPane().getUIID());
        Container layered = form.getLayeredPane(InteractionDialog.class, true);
        assertTrue(layered.contains(dialog));
        dialog.dispose();
    }

    @Test
    void pointerOutOfBoundsListenersInstalledWhenEnabled() throws Exception {
        Form form = new Form(new BorderLayout());
        when(implementation.getCurrentForm()).thenReturn(form);
        InteractionDialog dialog = new InteractionDialog();
        dialog.setDisposeWhenPointerOutOfBounds(true);
        dialog.setAnimateShow(false);
        dialog.show(0, 0, 0, 0);
        assertNotNull(getPrivateField(dialog, "pressedListener", Object.class));
        assertNotNull(getPrivateField(dialog, "releasedListener", Object.class));
        dialog.dispose();
    }

    @Test
    void formModeUsesFormLayeredPane() {
        Form form = new Form(new BorderLayout());
        when(implementation.getCurrentForm()).thenReturn(form);
        InteractionDialog dialog = new InteractionDialog();
        dialog.setAnimateShow(false);
        dialog.setFormMode(true);
        Rectangle rect = new Rectangle(0, 0, 50, 50);
        dialog.showPopupDialog(rect);
        Container formLayer = form.getFormLayeredPane(InteractionDialog.class, true);
        assertTrue(formLayer.contains(dialog));
        dialog.dispose();
    }

    private <T> T getPrivateField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}

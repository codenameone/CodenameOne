package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class DialogTest extends UITestBase {

    @FormTest
    void disposeWhenPointerOutOfBoundsClosesDialog() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        Dialog dialog = new Dialog("Outside", new BorderLayout());
        dialog.getContentPane().addComponent(BorderLayout.CENTER, new Label("Body"));
        dialog.setDisposeWhenPointerOutOfBounds(true);
        dialog.showModeless();

        form.getAnimationManager().flush();
        form.revalidate();

        int outsideX = dialog.getAbsoluteX() - 10;
        int outsideY = dialog.getAbsoluteY() - 10;
        dialog.pointerPressed(outsideX, outsideY);
        dialog.pointerReleased(outsideX, outsideY);
        form.getAnimationManager().flush();

        assertTrue(dialog.isDisposed(), "Dialog should dispose when pointer released outside bounds");
    }

    @FormTest
    void commandButtonsDispatchActions() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        boolean originalCommandsAsButtons = Dialog.isCommandsAsButtons();
        Dialog.setCommandsAsButtons(true);

        Dialog dialog = new Dialog("Commands", new BorderLayout());
        dialog.getContentPane().addComponent(BorderLayout.CENTER, new Label("Body"));

        Command ok = new Command("OK");
        Command cancel = new Command("Cancel");
        dialog.placeButtonCommands(new Command[]{ok, cancel});

        final Command[] executed = new Command[1];
        dialog.addCommandListener(evt -> executed[0] = evt.getCommand());

        dialog.showModeless();
        form.getAnimationManager().flush();
        form.revalidate();

        Button okButton = findButton(dialog, ok);
        assertNotNull(okButton, "OK button should be created for command");

        int px = okButton.getAbsoluteX() + okButton.getWidth() / 2;
        int py = okButton.getAbsoluteY() + okButton.getHeight() / 2;
        dialog.pointerPressed(px, py);
        okButton.pointerPressed(px, py);
        okButton.pointerReleased(px, py);
        dialog.pointerReleased(px, py);

        assertSame(ok, executed[0], "OK command should be dispatched when button is pressed");

        dialog.dispose();
        Dialog.setCommandsAsButtons(originalCommandsAsButtons);
    }

    @FormTest
    void timeoutDisposesDialogDuringAnimation() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();

        Dialog dialog = new Dialog("Timeout", new BorderLayout());
        dialog.getContentPane().addComponent(BorderLayout.CENTER, new Label("Body"));
        dialog.showModeless();
        form.getAnimationManager().flush();

        dialog.setTimeout(0);
        dialog.animate();

        assertTrue(dialog.isDisposed(), "Dialog should be disposed after timeout");
    }

    @FormTest
    void staticConfigurationCarriesToNewDialogs() {
        boolean originalAutoAdjust = Dialog.isAutoAdjustDialogSize();
        String originalPosition = Dialog.getDefaultDialogPosition();
        boolean originalScrolling = Dialog.isDisableStaticDialogScrolling();
        boolean originalCommandsAsButtons = Dialog.isCommandsAsButtons();
        boolean originalDispose = Dialog.isDefaultDisposeWhenPointerOutOfBounds();
        float originalBlurRadius = Dialog.getDefaultBlurBackgroundRadius();

        try {
            Dialog.setAutoAdjustDialogSize(false);
            Dialog.setDefaultDialogPosition(BorderLayout.SOUTH);
            Dialog.setDisableStaticDialogScrolling(true);
            Dialog.setCommandsAsButtons(false);
            Dialog.setDefaultDisposeWhenPointerOutOfBounds(true);
            Dialog.setDefaultBlurBackgroundRadius(4.5f);

            Dialog dialog = new Dialog("Configured", new BorderLayout());
            dialog.getContentPane().addComponent(BorderLayout.CENTER, new Label("Body"));

            assertFalse(Dialog.isAutoAdjustDialogSize());
            assertTrue(Dialog.isDisableStaticDialogScrolling());
            assertFalse(Dialog.isCommandsAsButtons());
            assertTrue(Dialog.isDefaultDisposeWhenPointerOutOfBounds());
            assertEquals(BorderLayout.SOUTH, dialog.getDialogPosition());
            assertTrue(dialog.isDisposeWhenPointerOutOfBounds());
            assertEquals(4.5f, dialog.getBlurBackgroundRadius(), 0.01f);
        } finally {
            Dialog.setAutoAdjustDialogSize(originalAutoAdjust);
            Dialog.setDefaultDialogPosition(originalPosition);
            Dialog.setDisableStaticDialogScrolling(originalScrolling);
            Dialog.setCommandsAsButtons(originalCommandsAsButtons);
            Dialog.setDefaultDisposeWhenPointerOutOfBounds(originalDispose);
            Dialog.setDefaultBlurBackgroundRadius(originalBlurRadius);
        }
    }

    @FormTest
    void popupDialogCanBeDisposedFromBackground() throws Exception {
        implementation.setBuiltinSoundsEnabled(false);
        Dialog dialog = new Dialog("Popup", new BorderLayout());
        Label body = new Label("Popup Body");
        dialog.getContentPane().addComponent(BorderLayout.CENTER, body);
        dialog.setDisposeWhenPointerOutOfBounds(true);

        Thread disposer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException ignored) {
                }
                dialog.dispose();
            }
        });
        disposer.start();

        dialog.showPopupDialog(body);
        disposer.join(1000L);

        assertTrue(dialog.isDisposed(), "Popup dialog should dispose when closed from another thread");
    }

    private Button findButton(Container root, Command target) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component cmp = root.getComponentAt(i);
            if (cmp instanceof Button) {
                Button button = (Button) cmp;
                if (button.getCommand() == target) {
                    return button;
                }
            }
            if (cmp instanceof Container) {
                Button found = findButton((Container) cmp, target);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

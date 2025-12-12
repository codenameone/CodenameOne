package com.codename1.ui.spinner;

import com.codename1.components.InteractionDialog;
import com.codename1.io.Util;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.*;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.atomic.AtomicBoolean;

public class PickerCoverageTest extends UITestBase {

    private void cleanup() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        if (impl != null) {
            impl.setNativePickerTypeSupported((Boolean[]) null);
            impl.setTablet(false);
        }
    }

    private void waitForForm(Form form) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (Display.getInstance().getCurrent() == form) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        Assertions.fail("Form did not become current in time");
    }

    @FormTest
    public void testPickerNextPrevButtons() {
        cleanup();
        Form f = new Form("Picker Test", new BoxLayout(BoxLayout.Y_AXIS));
        TextField prevTf = new TextField("Prev");
        f.add(prevTf);

        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_STRINGS);
        picker.setStrings("Option 1", "Option 2", "Option 3");
        picker.setUseLightweightPopup(true);
        f.add(picker);

        TextField nextTf = new TextField("Next");
        f.add(nextTf);

        f.show();
        waitForForm(f);

        // Trigger Picker
        picker.pressed();
        picker.released();
        DisplayTest.flushEdt();

        // Find InteractionDialog
        InteractionDialog dlg = findInteractionDialog(f);
        Assertions.assertNotNull(dlg, "Should show InteractionDialog");

        // Find Next Button (Material Keyboard Arrow Down)
        Button nextButton = findButtonWithIcon(dlg, FontImage.MATERIAL_KEYBOARD_ARROW_DOWN);
        Assertions.assertNotNull(nextButton, "Next button should be present");

        // Click Next
        nextButton.pressed();
        nextButton.released();
        DisplayTest.flushEdt();

        Assertions.assertTrue(nextTf.hasFocus() || nextTf.isEditing(), "Next component should have focus/editing");

        // Re-open picker
        picker.pressed();
        picker.released();
        DisplayTest.flushEdt();

        dlg = findInteractionDialog(f);
        Assertions.assertNotNull(dlg, "Should show InteractionDialog");

        // Find Prev Button (Material Keyboard Arrow Up)
        Button prevButton = findButtonWithIcon(dlg, FontImage.MATERIAL_KEYBOARD_ARROW_UP);
        Assertions.assertNotNull(prevButton, "Prev button should be present");

        // Click Prev
        prevButton.pressed();
        prevButton.released();
        DisplayTest.flushEdt();

        Assertions.assertTrue(prevTf.hasFocus() || prevTf.isEditing(), "Prev component should have focus/editing");
    }

    private InteractionDialog findInteractionDialog(Form f) {
        if (f == null) return null;
        // In some cases, InteractionDialog logic adds directly to layered pane
        Container lp = f.getLayeredPane();
        return findInteractionDialog(lp);
    }

    private InteractionDialog findInteractionDialog(Container c) {
        if (c instanceof InteractionDialog) return (InteractionDialog) c;
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponentAt(i);
            if (child instanceof InteractionDialog) {
                return (InteractionDialog) child;
            }
            if (child instanceof Container) {
                InteractionDialog found = findInteractionDialog((Container) child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Button findButtonWithIcon(Container container, char iconChar) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component c = container.getComponentAt(i);
            if (c instanceof Button) {
                Button b = (Button) c;
                Image icon = b.getIcon();
                if (icon instanceof FontImage) {
                    if (((FontImage) icon).getText().charAt(0) == iconChar) {
                        return b;
                    }
                }
            }
            if (c instanceof Container) {
                Button b = findButtonWithIcon((Container) c, iconChar);
                if (b != null) return b;
            }
        }
        return null;
    }

    @FormTest
    public void testTabletFlushAnimation() {
        cleanup();
        TestCodenameOneImplementation.getInstance().setTablet(true);

        Form f = new Form("Tablet Test");
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_STRINGS);
        picker.setStrings("A", "B", "C");
        picker.setUseLightweightPopup(true);
        f.add(picker);
        f.show();
        waitForForm(f);

        picker.pressed();
        picker.released();
        DisplayTest.flushEdt();

        InteractionDialog dlg = findInteractionDialog(f);
        Assertions.assertNotNull(dlg, "Should show InteractionDialog on Tablet");
        Assertions.assertTrue(dlg.getUIID().contains("Tablet"), "Should use Tablet UIID");
    }

    @FormTest
    public void testLegacyDialogCancel() {
        cleanup();
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_STRINGS);
        picker.setStrings("Option 1", "Option 2");
        picker.setUseLightweightPopup(false);

        // Setup sequence: first true (to skip lightweight block), then false (to skip native block)
        TestCodenameOneImplementation.getInstance().setNativePickerTypeSupported(true, false);

        try {
            Form f = new Form("Legacy Test");
            f.add(picker);
            f.show();
            waitForForm(f);

            // Schedule interaction
            Display.getInstance().callSerially(() -> {
                Form current = Display.getInstance().getCurrent();
                // Legacy dialog is a Dialog
                if (current instanceof Dialog) {
                    Button cancelButton = findButtonWithText(current, "Cancel");
                    if (cancelButton != null) {
                        cancelButton.pressed();
                        cancelButton.released();
                    }
                }
            });

            // Trigger - this blocks until dialog closes
            picker.pressed();
            picker.released();
            DisplayTest.flushEdt();

            // Should close dialog
            Assertions.assertNotEquals(Dialog.class, Display.getInstance().getCurrent().getClass(), "Dialog should close");

        } finally {
            cleanup();
        }
    }

    private Button findButtonWithText(Container container, String text) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component c = container.getComponentAt(i);
            if (c instanceof Button) {
                Button b = (Button) c;
                if (text.equals(b.getText()) || (b.getCommand() != null && text.equals(b.getCommand().getCommandName()))) {
                    return b;
                }
            }
            if (c instanceof Container) {
                Button b = findButtonWithText((Container) c, text);
                if (b != null) return b;
            }
        }
        return null;
    }

    @FormTest
    public void testSizeChangedListenerRunnable() {
        cleanup();
        // Picker$3$1: Runnable inside sizeChanged listener (flushAnimation)
        // Only added if !isTablet.
        TestCodenameOneImplementation.getInstance().setTablet(false);

        Form f = new Form("Size Changed Test");
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_STRINGS);
        picker.setStrings("A", "B", "C");
        picker.setUseLightweightPopup(true);
        f.add(picker);
        f.show();
        waitForForm(f);

        picker.pressed();
        picker.released();
        DisplayTest.flushEdt();

        // Picker dialog is open.
        TestCodenameOneImplementation.getInstance().setWindowSize(800, 1200);
        DisplayTest.flushEdt();
    }

    @FormTest
    public void testVirtualInputDeviceCloseRunnable() throws Exception {
        cleanup();
        // Picker$4$1: Runnable inside VirtualInputDevice.close

        Form f = new Form("VID Test");
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_STRINGS);
        picker.setStrings("A", "B", "C");
        picker.setUseLightweightPopup(true);
        f.add(picker);
        f.show();
        waitForForm(f);

        picker.pressed();
        picker.released();
        DisplayTest.flushEdt();

        // Picker is open. It sets current input device.
        VirtualInputDevice vid = f.getCurrentInputDevice();
        Assertions.assertNotNull(vid, "Input device should be set");

        AtomicBoolean callbackRan = new AtomicBoolean(false);
        picker.stopEditing(() -> callbackRan.set(true));

        DisplayTest.flushEdt();

        // Wait for animation delay if necessary, but flushEdt should handle pending runnables.
        // There might be animation delays.
        Thread.sleep(100);
        DisplayTest.flushEdt();

        Assertions.assertTrue(callbackRan.get(), "Stop editing callback should run, implying Picker$4$1 ran");
    }
}

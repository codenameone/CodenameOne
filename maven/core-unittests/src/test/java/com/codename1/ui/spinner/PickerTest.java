package com.codename1.ui.spinner;

import com.codename1.components.InteractionDialog;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.junit.FormTest;

import static org.junit.jupiter.api.Assertions.*;

public class PickerTest extends UITestBase {

    @FormTest
    public void testPickerInteraction() {
        Picker picker = new Picker();
        picker.setUseLightweightPopup(true);
        picker.setType(Display.PICKER_TYPE_STRINGS);
        picker.setStrings("A", "B", "C");

        Form f = new Form(new BoxLayout(BoxLayout.Y_AXIS));
        Button b1 = new Button("Before");
        Button b2 = new Button("After");
        f.add(b1).add(picker).add(b2);
        f.show();

        // Open Picker
        picker.pointerPressed(0, 0);
        picker.pointerReleased(0, 0);

        flushSerialCalls();

        Form current = Display.getInstance().getCurrent();
        Container layeredPane = current.getLayeredPane();
        InteractionDialog dlg = findInteractionDialog(layeredPane);

        if (dlg != null) {
            Button nextBtn = findButtonWithIcon(dlg, FontImage.MATERIAL_KEYBOARD_ARROW_DOWN);
            if (nextBtn != null) {
                nextBtn.pointerPressed(0, 0);
                nextBtn.pointerReleased(0, 0);
            }

            Button prevBtn = findButtonWithIcon(dlg, FontImage.MATERIAL_KEYBOARD_ARROW_UP);
            if (prevBtn != null) {
                prevBtn.pointerPressed(0, 0);
                prevBtn.pointerReleased(0, 0);
            }

            Button doneBtn = findButtonWithText(dlg, "Done");
            if (doneBtn != null) {
                doneBtn.pointerPressed(0, 0);
                doneBtn.pointerReleased(0, 0);
            } else {
                dlg.dispose();
            }
        }

        // --- Test Tablet Mode ---
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setTablet(true);

        picker.pointerPressed(0, 0);
        picker.pointerReleased(0, 0);
        flushSerialCalls();

        impl.setTablet(false);

        // --- Test Size Changed Listener ---
        picker.pointerPressed(0, 0);
        picker.pointerReleased(0, 0);
        flushSerialCalls();

        f.setWidth(f.getWidth() + 10);
        f.setHeight(f.getHeight() + 10);
    }

    private InteractionDialog findInteractionDialog(Container parent) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component c = parent.getComponentAt(i);
            if (c instanceof InteractionDialog) {
                return (InteractionDialog) c;
            }
            if (c instanceof Container) {
                InteractionDialog found = findInteractionDialog((Container) c);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Button findButtonWithIcon(Container parent, char iconChar) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component c = parent.getComponentAt(i);
            if (c instanceof Button) {
                Button b = (Button) c;
                if (b.getIcon() instanceof FontImage) {
                    FontImage fi = (FontImage) b.getIcon();
                    if (fi.getText().charAt(0) == iconChar) {
                        return b;
                    }
                }
            }
            if (c instanceof Container) {
                Button found = findButtonWithIcon((Container) c, iconChar);
                if (found != null) return found;
            }
        }
        // Fallback for non-font images (unlikely in this context but keeping for safety if focused)
        return findButtonWithClientProperty(parent, "$$focus");
    }

    private Button findButtonWithClientProperty(Container parent, String key) {
         for (int i = 0; i < parent.getComponentCount(); i++) {
            Component c = parent.getComponentAt(i);
            if (c instanceof Button && c.getClientProperty(key) != null) {
                return (Button) c;
            }
            if (c instanceof Container) {
                Button found = findButtonWithClientProperty((Container) c, key);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Button findButtonWithText(Container parent, String text) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component c = parent.getComponentAt(i);
            if (c instanceof Button) {
                if (((Button) c).getText().equals(text)) {
                    return (Button) c;
                }
            }
            if (c instanceof Container) {
                Button found = findButtonWithText((Container) c, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}

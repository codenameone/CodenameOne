package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent;
import com.codename1.io.Storage;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class TextFieldTextComponentURLImageTest extends UITestBase {

    @FormTest
    void textFieldConfigurationAndCursorManagement() {
        implementation.setBuiltinSoundsEnabled(false);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        TextField field = new TextField();
        field.setHint("Enter value");
        field.setConstraint(TextArea.EMAILADDR);
        field.setColumns(12);
        field.setRows(2);
        field.setGrowByContent(true);
        field.setCursorPosition(0);
        field.setEditable(true);
        field.setScrollVisible(false);

        form.add(BorderLayout.CENTER, field);
        form.revalidate();

        field.setText("user@example.com");
        assertEquals("user@example.com", field.getText());
        assertEquals(TextArea.EMAILADDR, field.getConstraint());

        form.pointerPressed(field.getAbsoluteX() + 2, field.getAbsoluteY() + 2);
        form.pointerReleased(field.getAbsoluteX() + 2, field.getAbsoluteY() + 2);
        assertTrue(field.hasFocus(), "Pointer interaction should grant focus");

        field.stopEditing();
        assertFalse(field.isEditing());
    }

    @FormTest
    void textComponentOnTopModeConstructsFloatingHintUI() {
        implementation.setBuiltinSoundsEnabled(false);

        Hashtable theme = new Hashtable();
        theme.put("textComponentAnimBool", "true");
        UIManager.getInstance().setThemeProps(theme);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        TextComponent component = new TextComponent();
        component.label("Username");
        component.hint("Enter username");
        component.onTopMode(true);
        component.focusAnimation(true);
        component.text("initial");
        component.columns(15);
        component.rows(3);

        form.add(BorderLayout.CENTER, component);
        form.revalidate();

        assertEquals("Username", component.getLabel().getText());
        assertEquals("initial", component.getField().getText());
        assertTrue(component.isOnTopMode());
        assertTrue(component.isFocusAnimation());

        component.text("updated");
        assertEquals("updated", component.getField().getText());

        component.text("");
        assertEquals("", component.getField().getText());
    }

    @FormTest
    void autoCompleteTextFieldPopupAndFiltering() {
        implementation.setBuiltinSoundsEnabled(false);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(BoxLayout.y());

        ListModel<String> model = new DefaultListModel<String>(new String[]{"alpha", "beta", "gamma"});
        AutoCompleteTextField field = new AutoCompleteTextField(model);
        field.setMinimumLength(1);
        field.setMinimumElementsShownInPopup(1);
        field.setPopupPosition(AutoCompleteTextField.POPUP_POSITION_UNDER);
        field.setText("a");

        assertEquals(1, field.getMinimumLength());
        assertEquals(1, field.getMinimumElementsShownInPopup());

        form.add(field);
        form.revalidate();

        final boolean[] invoked = {false};
        ActionListener<ActionEvent> listener = new ActionListener<ActionEvent>() {
            public void actionPerformed(ActionEvent evt) {
                invoked[0] = true;
            }
        };
        field.addListListener(listener);

        field.showPopup();
        form.getAnimationManager().flush();
        flushSerialCalls();

        ComponentSelector popupSelector = ComponentSelector.$("AutoCompletePopup", form);
        assertTrue(popupSelector.size() > 0, "Popup component should be added to the layered pane");

        field.fireActionEvent();
        assertEquals("a", field.getText());

        field.removeListListener(listener);
        assertFalse(invoked[0], "List listener should not fire without selection");
    }

    @FormTest
    void urlImageFetchesFromStorageCache() {
        implementation.setBuiltinSoundsEnabled(false);

        byte[] data = new byte[]{1, 2, 3, 4};
        EncodedImage placeholder = EncodedImage.create(data, 4, 4, false);

        try (OutputStream os = Storage.getInstance().createOutputStream("urlImageKey")) {
            os.write(data);
        } catch (IOException err) {
            fail("Writing image data to storage should not throw: " + err.getMessage());
        }

        URLImage urlImage = URLImage.createToStorage(placeholder, "urlImageKey", "file://ignored");
        urlImage.fetch();

        assertArrayEquals(data, urlImage.getImageData(), "fetch should load cached image data");
        assertTrue(urlImage.isAnimation(), "Loaded image should request repaint animation");
        assertTrue(urlImage.animate(), "animate should return true when repaint is requested");
        Storage.getInstance().deleteStorageFile("urlImageKey");
    }
}

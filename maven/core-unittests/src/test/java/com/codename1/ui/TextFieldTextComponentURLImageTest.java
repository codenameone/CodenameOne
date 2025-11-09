package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.io.Storage;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.URLImage;
import com.codename1.util.Base64;
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
        flushSerialCalls();
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

        byte[] cachedData = Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAQAAAAECAIAAAAmkwkpAAAAIElEQVR42mNgYGD4z4AFwDiqgGEYBBgYGL4DRAaGAQYAAGxwAh5YQ+RtAAAAAElFTkSuQmCC".getBytes());
        assertNotNull(cachedData);
        assertTrue(cachedData.length > 0);

        Image decoded;
        try {
            decoded = Image.createImage(cachedData, 0, cachedData.length);
        } catch (IllegalArgumentException err) {
            fail("Decoded placeholder image should be created from cached data");
            return;
        }

        EncodedImage placeholder = EncodedImage.create(cachedData, decoded.getWidth(), decoded.getHeight(), decoded.isOpaque());
        assertNotNull(placeholder);

        Storage storage = Storage.getInstance();
        try {
            OutputStream output = storage.createOutputStream("urlImageKey");
            try {
                output.write(cachedData);
            } finally {
                output.close();
            }
        } catch (IOException err) {
            fail("Storage output stream should not throw in test environment: " + err.getMessage());
        }

        URLImage urlImage = URLImage.createToStorage(placeholder, "urlImageKey", "file://ignored");
        assertNotNull(urlImage, "URLImage factory should return an instance");

        assertTrue(storage.exists("urlImageKey"));
        urlImage.fetch();
        flushSerialCalls();

        byte[] result = urlImage.getImageData();
        assertNotNull(result, "URLImage should load cached image data");
        assertArrayEquals(cachedData, result);
        storage.deleteStorageFile("urlImageKey");
    }

    @FormTest
    void textFieldConstructorsAndBasicMethods() {
        TextField tf1 = new TextField();
        assertNotNull(tf1);
        assertTrue(tf1.getText() == null || tf1.getText().isEmpty());

        TextField tf2 = new TextField("Hello");
        assertEquals("Hello", tf2.getText());

        TextField tf3 = new TextField("", 20);
        assertEquals(20, tf3.getColumns());

        TextField tf4 = new TextField("Text", 15);
        assertEquals("Text", tf4.getText());
        assertEquals(15, tf4.getColumns());
    }

    @FormTest
    void textFieldDoneListener() {
        TextField field = new TextField();
        final boolean[] listenerCalled = {false};

        ActionListener listener = evt -> listenerCalled[0] = true;
        field.setDoneListener(listener);

        // Just verify listener can be set
        assertNotNull(field);
    }

    @FormTest
    void textComponentConstructors() {
        TextComponent tc1 = new TextComponent();
        assertNotNull(tc1);
    }

    @FormTest
    void textComponentLabelAndHint() {
        TextComponent tc = new TextComponent();

        tc.label("Name");
        assertEquals("Name", tc.getLabel().getText());

        tc.hint("Enter name");
        assertEquals("Enter name", tc.getField().getHint());
    }

    @FormTest
    void textComponentTextAndColumns() {
        TextComponent tc = new TextComponent();

        tc.text("Hello");
        assertEquals("Hello", tc.getField().getText());

        tc.columns(25);
        assertEquals(25, tc.getField().getColumns());

        tc.rows(5);
        assertEquals(5, tc.getField().getRows());
    }

    @FormTest
    void textComponentConstraint() {
        TextComponent tc = new TextComponent();

        tc.constraint(TextArea.NUMERIC);
        assertEquals(TextArea.NUMERIC, tc.getField().getConstraint());

        tc.constraint(TextArea.PASSWORD);
        assertEquals(TextArea.PASSWORD, tc.getField().getConstraint());
    }

    @FormTest
    void textComponentEditable() {
        TextComponent tc = new TextComponent();

        tc.getField().setEditable(true);
        assertTrue(tc.getField().isEditable());

        tc.getField().setEditable(false);
        assertFalse(tc.getField().isEditable());
    }

    @FormTest
    void textComponentErrorMessage() {
        TextComponent tc = new TextComponent();

        tc.errorMessage("Invalid input");
        assertEquals("Invalid input", tc.getErrorMessage());

        tc.errorMessage(null);
        assertNull(tc.getErrorMessage());
    }

    @FormTest
    void textComponentActionListener() {
        TextComponent tc = new TextComponent();
        final boolean[] listenerCalled = {false};

        ActionListener listener = evt -> listenerCalled[0] = true;
        tc.getField().addActionListener(listener);

        // Just verify listener can be added
        assertNotNull(tc);
    }

    @FormTest
    void textComponentDataChangeListener() {
        TextComponent tc = new TextComponent();
        final boolean[] listenerCalled = {false};

        tc.getField().addDataChangeListener((type, index) -> listenerCalled[0] = true);

        // Just verify listener can be added
        assertNotNull(tc);
    }

    @FormTest
    void autoCompleteTextFieldConstructors() {
        DefaultListModel<String> model = new DefaultListModel<String>();
        AutoCompleteTextField field = new AutoCompleteTextField(model);
        assertNotNull(field);
    }

    @FormTest
    void autoCompleteTextFieldMinimumLength() {
        DefaultListModel<String> model = new DefaultListModel<String>(
            new String[]{"Apple", "Apricot", "Banana", "Cherry"}
        );
        AutoCompleteTextField field = new AutoCompleteTextField(model);

        field.setMinimumLength(2);
        assertEquals(2, field.getMinimumLength());

        field.setMinimumLength(3);
        assertEquals(3, field.getMinimumLength());
    }

    @FormTest
    void autoCompleteTextFieldPopupPosition() {
        DefaultListModel<String> model = new DefaultListModel<String>();
        AutoCompleteTextField field = new AutoCompleteTextField(model);

        field.setPopupPosition(AutoCompleteTextField.POPUP_POSITION_OVER);
        // Just verify it can be set without crashing
        assertNotNull(field);

        field.setPopupPosition(AutoCompleteTextField.POPUP_POSITION_UNDER);
        assertNotNull(field);
    }

    @FormTest
    void urlImageCreateToStorage() {
        byte[] data = new byte[]{1, 2, 3, 4};
        EncodedImage placeholder = EncodedImage.createFromImage(Image.createImage(4, 4), false);

        URLImage urlImage = URLImage.createToStorage(placeholder, "testKey", "file://test.png");
        assertNotNull(urlImage);
    }

    @FormTest
    void urlImageCreateToFileSystem() {
        byte[] data = new byte[]{1, 2, 3, 4};
        EncodedImage placeholder = EncodedImage.createFromImage(Image.createImage(4, 4), false);

        URLImage urlImage = URLImage.createToFileSystem(placeholder, "test.png", "file://test.png", null);
        assertNotNull(urlImage);
    }
}

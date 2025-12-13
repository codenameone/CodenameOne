package com.codename1.samples;

import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class TextFieldHintStylingTestTest extends UITestBase {

    @FormTest
    public void testTextFieldHintStyling() {
        Form hi = new Form("Hi World", BoxLayout.y());
        TextField text = new TextField("", "Email or Username", 20, TextArea.ANY);

        text.getAllStyles().setBgColor(0xff0000);
        text.getAllStyles().setFgColor(0x00ff00);

        text.getHintLabel().getAllStyles().setBgColor(0xcccccc);
        text.getHintLabel().getAllStyles().setFgColor(0x0000ff);

        hi.add(text);
        hi.show();

        assertEquals(0xff0000, text.getStyle().getBgColor());
        assertEquals(0xcccccc, text.getHintLabel().getStyle().getBgColor());
    }
}

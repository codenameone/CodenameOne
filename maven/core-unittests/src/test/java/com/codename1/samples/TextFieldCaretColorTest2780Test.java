package com.codename1.samples;

import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class TextFieldCaretColorTest2780Test extends UITestBase {

    @FormTest
    public void testTextFieldCaretColor() {
        Form hi = new Form("Hi World", BoxLayout.y());
        hi.add(new Label("Hi World"));
        TextField tf1 = new TextField();
        TextField tf2 = new TextField();
        TextArea ta1 = new TextArea();
        TextArea ta2 = new TextArea();
        ta1.setRows(5);
        ta2.setRows(5);

        $(tf2, ta2).selectAllStyles()
                .setBgColor(0x0000ff)
                .setFgColor(0xffffff)
                .setBgTransparency(0xff)
                .setBackgroundType(Style.BACKGROUND_NONE);

        hi.addAll(tf1, tf2, ta1, ta2);
        hi.show();

        assertEquals(0x0000ff, tf2.getStyle().getBgColor());
        assertEquals(0xffffff, tf2.getStyle().getFgColor());
    }
}

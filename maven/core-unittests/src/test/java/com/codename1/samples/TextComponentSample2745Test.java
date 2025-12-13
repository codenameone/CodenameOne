package com.codename1.samples;

import com.codename1.ui.Form;
import com.codename1.ui.TextComponent;
import com.codename1.ui.layouts.TextModeLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class TextComponentSample2745Test extends UITestBase {

    @FormTest
    public void testTextComponent() {
        Form hi = new Form("Hi World", new TextModeLayout(2, 1));
        TextComponent cmp1 = new TextComponent().label("label1");
        TextComponent cmp2 = new TextComponent().label("label2");
        hi.add(cmp1);
        hi.add(cmp2);
        hi.show();

        cmp1.text("text1");
        cmp2.text("text2");
        hi.revalidate();

        assertEquals("text1", cmp1.getText());
        assertEquals("text2", cmp2.getText());
        assertEquals("label1", cmp1.getPropertyValue("label"));
    }
}

package com.codename1.samples;

import com.codename1.ui.Form;
import com.codename1.ui.TextComponent;
import com.codename1.ui.TextComponentPassword;
import com.codename1.ui.layouts.TextModeLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class TestTextComponentPassword2976Test extends UITestBase {

    @FormTest
    public void testTextComponentPassword() {
        TextModeLayout tl = new TextModeLayout(2, 1);
        Form f = new Form("Pixel Perfect", tl);

        TextComponent user = new TextComponent();
        TextComponentPassword pass = new TextComponentPassword();

        f.addAll(user, pass);
        f.show();

        assertTrue(f.contains(user));
        assertTrue(f.contains(pass));
    }
}

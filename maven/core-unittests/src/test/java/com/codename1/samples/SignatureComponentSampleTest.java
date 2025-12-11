package com.codename1.samples;

import com.codename1.components.SignatureComponent;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class SignatureComponentSampleTest extends UITestBase {

    @FormTest
    public void testSignatureComponent() {
        Form hi = new Form("Signature Test", BoxLayout.y());
        SignatureComponent signature = new SignatureComponent();
        hi.add(signature);
        hi.show();

        assertNotNull(signature.getParent());
        assertEquals(hi.getContentPane(), signature.getParent());
        assertTrue(hi.contains(signature));
    }
}

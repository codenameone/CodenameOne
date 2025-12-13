package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import static com.codename1.ui.CN.*;

public class NullPointerOnEDTSample2992Test extends UITestBase {

    @FormTest
    public void testNullPointerOnEDTSample() {
        Form hi = new Form("Hi World", BoxLayout.y());
        hi.add(new TextField("", "Write here"));
        hi.show();
        // Smoke test to ensure no exceptions
    }
}

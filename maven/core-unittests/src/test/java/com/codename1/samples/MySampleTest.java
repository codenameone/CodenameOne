package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import static com.codename1.ui.CN.*;

public class MySampleTest extends UITestBase {

    @FormTest
    public void testMySample() {
        Form hi = new Form("Hi World", BoxLayout.y());
        hi.add(new Label("Hi World"));
        hi.show();
        // Simple smoke test to check if it displays
    }
}

package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.layouts.BoxLayout;
import static org.junit.jupiter.api.Assertions.*;

public class UnicodeFontsSampleTest extends UITestBase {

    @FormTest
    public void testUnicodeFontsSample() {
        Form hi = new Form("Hi World", BoxLayout.y());
        Label l = new Label("重新开始重新开始");
        l.getStyle().setFont(Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, 5f));
        hi.add(l);

        String emoji = "Here is an 重新开始重新开始 alien: \uD83D\uDC7D! ";
        StringBuilder s = new StringBuilder();
        for(int i = 0 ; i < 30 ; i++) {
            s.append(emoji);
        }
        SpanLabel sl = new SpanLabel(s.toString());
        sl.getTextAllStyles().setFont(Font.createTrueTypeFont(Font.NATIVE_MAIN_REGULAR, 3f));
        hi.add(sl);
        hi.show();
        waitForForm(hi);

        assertNotNull(l.getStyle().getFont(), "Label font should be set");
        // We verify that the form is shown and components are added without crashing.
        assertEquals(2, hi.getComponentCount(), "Form should have 2 components");
    }

    private void waitForForm(Form form) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (Display.getInstance().getCurrent() == form) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        fail("Form did not become current in time");
    }
}

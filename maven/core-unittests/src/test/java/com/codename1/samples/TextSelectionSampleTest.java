package com.codename1.samples;

import com.codename1.components.SpanLabel;
import com.codename1.ui.CN;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.TextSelection;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class TextSelectionSampleTest extends UITestBase {

    @FormTest
    public void testTextSelectionSample() {
        Form hi = new Form("Hi World", BoxLayout.y());
        hi.setScrollableY(false);
        TextSelection sel = hi.getTextSelection();
        final boolean[] selectionChanged = new boolean[1];
        sel.addTextSelectionListener(e->{
            selectionChanged[0] = true;
        });

        sel.setEnabled(true);
        Label label = new Label("This label should be selectable");
        label.setTextSelectionEnabled(true);
        Label label2 = new Label("Some more text");
        label2.setTextSelectionEnabled(true);
        hi.add(label);
        hi.add(new TextField("Hello Universe"));
        hi.add(label2);
        hi.add(new Label("Hi World"));

        Container cnt = new Container(BoxLayout.x());
        cnt.setScrollableX(true);
        cnt.getStyle().setBorder(Border.createLineBorder(1, 0x0));
        cnt.setPreferredH(CN.convertToPixels(5));
        cnt.setPreferredW(CN.convertToPixels(20));

        TextArea ta = new TextArea();
        ta.setText("Lorem Ipsum is simply dummy text of the printing and typesetting industry.");
        ta.setEnabled(false);
        ta.setRows(6);
        hi.add(ta);

        SpanLabel sl = new SpanLabel();
        sl.setText(ta.getText());
        sl.setTextSelectionEnabled(true);
        hi.add(sl);

        TextField tf = new TextField();
        tf.setText("Hello World.  This is a test field");
        tf.setEnabled(false);
        hi.add(tf);

        Label l = new Label("This is a test with some long text to see if this works.");
        l.setTextSelectionEnabled(true);
        cnt.add(l);

        Container cntY = new Container(BoxLayout.y());
        cntY.setScrollableY(true);
        cntY.getStyle().setBorder(Border.createLineBorder(1, 0x0));
        for (int i=0; i<50; i++) {
            Label li = new Label("List item "+i);
            li.setTextSelectionEnabled(true);
            cntY.add(li);
        }
        hi.add(cnt);
        hi.add(cntY);

        $(cnt, cntY).selectAllStyles().setMarginMillimeters(4);

        hi.show();
        waitForForm(hi);

        // Verification
        assertTrue(label.isTextSelectionEnabled(), "Label should have text selection enabled");
        assertTrue(sl.isTextSelectionEnabled(), "SpanLabel should have text selection enabled");
        assertNotNull(hi.getTextSelection(), "Form should have a TextSelection object");
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

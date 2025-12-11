package com.codename1.samples;

import com.codename1.ui.Button;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.Toolbar;
import com.codename1.ui.UIFragment;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.RoundRectBorder;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class UIFragmentSampleTest extends UITestBase {

    @FormTest
    public void testUIFragmentSample() {
        Form f = new Form("Test Fragments", BoxLayout.y());
        TextArea ta = new TextArea();
        ta.setMaxSize(5000);

        String[] examples = new String[]{
            "<borderAbs><$button1 constraint='center'/><xng constraint='south'><$button2/><$button3/><$button4/></xng></borderAbs>",
            "{centerAbs:$button1, south:{xng:[$button2, $button3, $button4]}}"
        };

        ComboBox<String> cb = new ComboBox<>(examples);
        cb.addActionListener(e->{
            ta.setText(examples[cb.getSelectedIndex()]);
        });

        ta.setText("<borderAbs><$button1 constraint='center'/><xng constraint='south'><$button2/><$button3/><$button4/></xng></borderAbs>");
        Button b = new Button("Compile");
        final boolean[] compiled = new boolean[1];

        b.addActionListener(e->{
            Form f2 = new Form("Result", new BorderLayout());
            f2.setToolbar(new Toolbar());

            Button b1 = new Button("Button 1");
            Button b2 = new Button("Button 2");
            Button b3 = new Button("Button 3");
            Button b4 = new Button("Button 4");
            $(b1, b2, b3, b4).selectAllStyles().setBorder(RoundRectBorder.create().cornerRadius(2)).setBgColor(0x003399).setBgTransparency(0xff);

            UIFragment frag;
            try {
                if (ta.getText().charAt(0) == '<') {
                    frag = UIFragment.parseXML(ta.getText());
                } else {
                    frag = UIFragment.parseJSON(ta.getText());
                }

                f2.add(BorderLayout.CENTER,frag
                        .set("button1", b1)
                        .set("button2", b2)
                        .set("button3", b3)
                        .set("button4", b4)
                        .getView()
                );
                f2.show();
                compiled[0] = true;
            } catch (Exception ex) {
                fail("Parsing failed: " + ex.getMessage());
            }
        });
        ta.setRows(5);

        f.addAll(cb, ta, b);
        f.show();
        waitForForm(f);

        // Trigger compile with XML
        b.pressed();
        b.released();
        assertTrue(compiled[0], "Should have compiled XML fragment");

        // Reset and try JSON
        compiled[0] = false;
        ta.setText("{centerAbs:$button1, south:{xng:[$button2, $button3, $button4]}}");
        b.pressed();
        b.released();
        assertTrue(compiled[0], "Should have compiled JSON fragment");
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

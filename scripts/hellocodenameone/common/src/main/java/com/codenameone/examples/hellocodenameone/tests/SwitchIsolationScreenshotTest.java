package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.Switch;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

/// Pure-Java reproduction of the Switch portion of KotlinUiTest, stripped of
/// everything else (no Kotlin, no Slider, no Accordion, no MultiButton, no
/// TextArea / CheckBox). Three rows of switches in progressively simpler
/// layouts. Previously reproduced the ~600x300 pill bug in isolation, which
/// let us pinpoint Double.parseDouble stripping decimal points (Switch's
/// track/thumb scale theme constants "2.5" / "1.5" / "1.4" resolved to 25 /
/// 15 / 14 on the JS port, making every image 10x oversized). Kept as a
/// regression guard after the parparvm_runtime parseDblImpl fix landed.
public class SwitchIsolationScreenshotTest extends BaseTest {
    @Override
    public boolean runTest() {
        Form form = createForm("Switch Isolation", BoxLayout.y(), "switch-isolation");

        // Row 1 - single switch in FlowLayout. Flow does not stretch children;
        // the switch should appear at its own preferredSize.
        form.add(new Label("Row 1: single Switch in FlowLayout"));
        form.add(FlowLayout.encloseCenter(new Switch()));

        // Row 2 - KotlinUiTest shape exactly: BoxLayout.encloseX(Switch, Switch)
        // with the second switched on.
        Switch a = new Switch();
        Switch b = new Switch();
        b.setOn();
        form.add(new Label("Row 2: BoxLayout.encloseX(Switch, Switch)"));
        form.add(BoxLayout.encloseX(a, b));

        // Row 3 - same wrapping + a sibling non-Switch component, to see if
        // having a well-anchored preferred size sibling changes anything.
        Switch c = new Switch();
        Switch d = new Switch();
        d.setOn();
        form.add(new Label("Row 3: BoxLayout.encloseX(Button, Switch, Switch)"));
        form.add(BoxLayout.encloseX(new Button("btn"), c, d));

        form.show();
        return true;
    }
}

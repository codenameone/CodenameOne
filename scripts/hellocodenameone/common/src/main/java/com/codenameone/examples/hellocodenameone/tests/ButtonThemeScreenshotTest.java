package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

/**
 * Theme-fidelity screenshot of Button / RaisedButton / FlatButton in
 * default, pressed, and disabled states. Emits light + dark pair.
 */
public class ButtonThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "ButtonTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        Button defaultBtn = new Button("Default");
        form.add(defaultBtn);
        // Annotate the primary button so reviewers can see top/bottom
        // edges, the inner text band (inset by padding), and a measured
        // height callout. Design-system spec for a text button is ~10mm
        // (Material 40dp / iOS 44pt) with text centered vertically.
        annotateComponent(defaultBtn, "Default button: ~10mm target, text centered");

        form.add(pressed(new Button("Pressed")));
        form.add(disabled(new Button("Disabled")));

        Button raised = new Button("Raised");
        raised.setUIID("RaisedButton");
        form.add(raised);
        annotateComponent(raised, "RaisedButton: primary fill, same 10mm rhythm");

        Button raisedPressed = new Button("Raised pressed");
        raisedPressed.setUIID("RaisedButton");
        form.add(pressed(raisedPressed));

        Button flat = new Button("Flat");
        flat.setUIID("FlatButton");
        form.add(flat);
    }

    private static Button pressed(Button b) {
        // Toggle the pressed UIID state so the .pressed style is rendered.
        b.pressed();
        return b;
    }

    private static Button disabled(Button b) {
        b.setEnabled(false);
        return b;
    }
}

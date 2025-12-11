package com.codename1.samples;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class TestCSSRegression3077Test extends UITestBase {

    @FormTest
    public void testCSSRegression() {
        // Let's set up the styles manually in UIManager to mimic the CSS
        Style buttonStyle = UIManager.getInstance().getComponentStyle("Button");
        buttonStyle.setBgColor(0x16D173);

        UIManager.getInstance().setComponentStyle("Button", buttonStyle);

        // LinkButton derives from Button
        Style linkButtonStyle = new Style(buttonStyle);
        linkButtonStyle.setBorder(null);
        UIManager.getInstance().setComponentStyle("LinkButton", linkButtonStyle);

        Form hi = new Form("Hi World", BoxLayout.y());
        TextField textField = new TextField();
        Button button = new Button("Button", "Button");
        Button linkButton = new Button("Link Button", "LinkButton");

        hi.add(textField);
        hi.add(button);
        hi.add(linkButton);
        hi.show();

        assertEquals("LinkButton", linkButton.getUIID());
        // Verify style application
        assertEquals(linkButton.getStyle().getBorder(), null);
    }
}

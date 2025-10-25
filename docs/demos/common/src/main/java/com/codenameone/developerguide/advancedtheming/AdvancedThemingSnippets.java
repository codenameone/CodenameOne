package com.codenameone.developerguide.advancedtheming;

import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.TextArea;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.io.IOException;
import java.util.Hashtable;

/**
 * Small snippets that accompany the Advanced Theming guide.
 */
public class AdvancedThemingSnippets {

    public void convertTextAreaToLabelUiid() {
        // tag::textAreaLabel[]
        TextArea t = new TextArea(); // ...
        t.setUIID("Label");
        t.setEditable(false);
        // end::textAreaLabel[]
    }

    public Resources loadDefaultTheme() throws IOException {
        // tag::initFirstTheme[]
        Resources theme = UIManager.initFirstTheme("/theme");
        // end::initFirstTheme[]
        return theme;
    }

    public Resources loadNamedTheme() throws IOException {
        // tag::initNamedTheme[]
        Resources theme = UIManager.initNamedTheme("/theme", "Theme");
        // end::initNamedTheme[]
        return theme;
    }

    public void addThemeLayer(Resources theme) {
        // tag::addThemeProps[]
        UIManager.getInstance().addThemeProps(theme.getTheme("NameOfLayerTheme"));
        // end::addThemeProps[]
    }

    public void increaseFontSize(Font largeFont) {
        // tag::addThemeHashtable[]
        Hashtable h = new Hashtable();
        h.put("font", largeFont);
        UIManager.getInstance().addThemeProps(h);
        Display.getInstance().getCurrent().refreshTheme();
        // end::addThemeHashtable[]
    }

    public double pixelsPerMillimeter() {
        // tag::pixelsPerMM[]
        double pixelsPerMM = ((double) Display.getInstance().convertToPixels(10, true)) / 10.0;
        // end::pixelsPerMM[]
        return pixelsPerMM;
    }

    public void setPaddingFromMillimeters(Button myButton, double pixelsPerMM) {
        // tag::paddingFromMM[]
        myButton.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        int pixels = (int) (1.5 * pixelsPerMM);
        myButton.getAllStyles().setPadding(pixels, pixels, pixels, pixels);
        // end::paddingFromMM[]
    }

    public static final String BUTTON_FG_COLOR = createButtonFgColor();

    private static String createButtonFgColor() {
        // tag::buttonFgColor[]
        return "Button.fgColor=ffffff";
        // end::buttonFgColor[]
    }
}

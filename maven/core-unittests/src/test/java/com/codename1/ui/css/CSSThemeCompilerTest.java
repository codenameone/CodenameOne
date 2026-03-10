package com.codename1.ui.css;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Image;
import com.codename1.ui.plaf.CSSBorder;
import com.codename1.ui.util.MutableResource;
import java.util.Hashtable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CSSThemeCompilerTest extends UITestBase {

    @Test
    public void testCompilesThemeConstantsDeriveAndMutableImages() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();

        compiler.compile(
                ":root{--primary:#abc;}"
                + "@constants{spacing: 4px; primaryColor: var(--primary);}"
                + "Button{color:var(--primary);background-color:#112233;padding:1px 2px;cn1-derive:Label;}"
                + "Button:pressed{border-width:2px;border-style:solid;border-color:#ffffff;cn1-mutable-image:btnBg #ff00ff;}"
                + "Label{margin:2px 4px 6px 8px;}"
                + "Button{color:pink;text-align:center;}",
                resource,
                "Theme"
        );

        Hashtable theme = resource.getTheme("Theme");
        assertEquals("ffc0cb", theme.get("Button.fgColor"));
        assertEquals("112233", theme.get("Button.bgColor"));
        assertEquals("255", theme.get("Button.transparency"));
        assertEquals("1,2,1,2", theme.get("Button.padding"));
        assertEquals("2,4,6,8", theme.get("Label.margin"));
        assertEquals("Label", theme.get("Button.derive"));
        assertEquals("#abc", theme.get("@primary"));
        assertEquals("4px", theme.get("@spacing"));
        assertEquals("#abc", theme.get("@primarycolor"));
        assertEquals(String.valueOf(Component.CENTER), String.valueOf(theme.get("Button.align")));
        assertTrue(theme.get("Button.press#border") instanceof CSSBorder);

        Image mutable = resource.getImage("btnBg");
        assertNotNull(mutable);
        assertNotNull(theme.get("Button.press#bgImage"));
    }
    @Test
    public void testThrowsOnMalformedCss() {
        CSSThemeCompiler compiler = new CSSThemeCompiler();
        MutableResource resource = new MutableResource();

        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                compiler.compile("Button{color:#12;}", resource, "Theme")
        );
        assertThrows(CSSThemeCompiler.CSSSyntaxException.class, () ->
                compiler.compile("Button{color:#ff00ff;text-align:middle;}", resource, "Theme")
        );
    }

}

package com.codename1.guibuilder;

import com.codename1.ui.css.CSSThemeCompiler;
import com.codename1.ui.util.MutableResource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Hashtable;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * If the project's theme.css fails to compile the editor silently falls back to its own theme, so
 * the canvas shows styling that has nothing to do with the CSS being edited.
 */
class ProjectCssCompilesTest {
    @BeforeAll static void init() {
        if (!com.codename1.ui.Display.isInitialized()) com.codename1.ui.Display.init(new JPanel());
    }

    @Test
    void theDemoProjectThemeCompilesIntoAUsableTheme() throws Exception {
        File css = new File("../demo-project/src/main/css/theme.css");
        assertTrue(css.isFile(), "demo theme.css is missing at " + css.getAbsolutePath());
        MutableResource resources = new MutableResource();
        try {
            new CSSThemeCompiler().compile(
                    new String(Files.readAllBytes(css.toPath()), StandardCharsets.UTF_8),
                    resources, "ProjectTheme");
        } catch (RuntimeException ex) {
            fail("the demo theme.css does not compile, so the canvas can never show it: " + ex);
        }
        Hashtable theme = resources.getTheme("ProjectTheme");
        assertNotNull(theme, "compilation produced no theme");
        assertFalse(theme.isEmpty(), "the compiled theme is empty");
        System.out.println("PROJECT CSS keys: " + theme.size());
        int titleKeys = 0;
        for (Object key : theme.keySet()) {
            if (String.valueOf(key).startsWith("Title")) titleKeys++;
        }
        System.out.println("PROJECT CSS Title keys: " + titleKeys);
        assertTrue(titleKeys > 0, "the compiled theme has no Title styling even though theme.css sets it");
    }
}

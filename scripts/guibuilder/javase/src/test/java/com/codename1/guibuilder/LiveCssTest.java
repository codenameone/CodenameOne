package com.codename1.guibuilder;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.xml.Element;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Editing theme.css has to change what the canvas draws; that is the whole point of live CSS. */
class LiveCssTest {
    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!Display.isInitialized()) Display.init(new JPanel());
    }

    @Test
    void editingCssRestylesTheLivePreview() throws Exception {
        Path project = Files.createTempDirectory("guibuilder-css");
        Path gui = project.resolve("src/main/guibuilder/com/example");
        Files.createDirectories(gui);
        Files.write(gui.resolve("StyledForm.gui"), ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<component type=\"Form\" layout=\"BoxLayout\" name=\"StyledForm\">\n"
                + "    <component type=\"Label\" name=\"styled\" text=\"Styled\" />\n"
                + "</component>\n").getBytes(StandardCharsets.UTF_8));
        Path css = project.resolve("src/main/css/theme.css");
        Files.createDirectories(css.getParent());
        Files.write(css, "Label { color: #ff0000; }\n".getBytes(StandardCharsets.UTF_8));

        Path input = Files.createTempFile("guibuilder", ".input");
        Files.write(input, ("projectDir=" + project + "\nguiDir=" + gui.getParent().getParent() + "\n"
                + "sourceDir=" + project.resolve("src/main/java") + "\ncssFile=" + css + "\n"
                + "initialForm=com.example.StyledForm\n").getBytes(StandardCharsets.UTF_8));
        System.setProperty("guibuilder.input", input.toString());
        System.setProperty("guibuilder.canvasMode", "desktop");

        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        builder.init(null);
        builder.runApp();
        settle();
        assertNull(onEdt(() -> builder.mcpOpenForm("com.example.StyledForm")));
        settle();

        // Open the CSS editor first: that is how anyone edits CSS, and it rebuilds the canvas into
        // a split pane, which is the one thing the direct path never exercises.
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                java.lang.reflect.Method open = CodenameOneGUIBuilder.class.getDeclaredMethod("openCss");
                open.setAccessible(true);
                open.invoke(builder);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        settle();

        int before = foreground(builder, "styled");
        assertEquals(0xff0000, before, "the starting CSS colour must reach the preview");

        Files.write(css, "Label { color: #00ff00; }\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(onEdt(() -> String.valueOf(builder.reloadProjectCssForTest())).equals("true"),
                "recompiling the project CSS failed");
        settle();

        assertEquals(0x00ff00, foreground(builder, "styled"),
                "editing theme.css must restyle the canvas, not just the file");
    }

    private static int foreground(CodenameOneGUIBuilder builder, String name) {
        Component preview = find(builder.canvasHostForTest(), builder, name);
        assertNotNull(preview, name + " does not render");
        return preview.getUnselectedStyle().getFgColor();
    }

    private static Component find(Container root, CodenameOneGUIBuilder builder, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            Object element = component.getClientProperty("gui.element");
            if (element instanceof Element e && name.equals(e.getAttribute("name"))) return component;
            if (component instanceof Container container) {
                Component nested = find(container, builder, name);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static String onEdt(java.util.function.Supplier<String> work) {
        final String[] out = new String[1];
        Display.getInstance().callSeriallyAndWait(() -> out[0] = work.get());
        return out[0];
    }

    private static void settle() {
        for (int i = 0; i < 5; i++) {
            try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            Display.getInstance().callSeriallyAndWait(() -> { });
        }
    }
}

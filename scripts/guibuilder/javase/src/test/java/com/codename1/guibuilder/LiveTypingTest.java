package com.codename1.guibuilder;

import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.editor.EditorView;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Types real keystrokes into the editor the running application actually builds, through the same
 * focus and key routing a keyboard uses. Every previous editor test drove the document API instead,
 * which is why editors that were dead to the keyboard kept passing.
 */
class LiveTypingTest {
    @BeforeAll
    static void initializeCodenameOneRuntime() {
        if (!Display.isInitialized()) Display.init(new JPanel());
    }

    @Test
    void typingIntoTheJavaEditorReachesTheDocument() throws Exception {
        CodenameOneGUIBuilder builder = workspace();
        open(builder, "openSourceEditor", String.class, null);
        settle();

        CodeEditor editor = builder.activeEditorForTest();
        assertNotNull(editor, "the Java editor was never created");
        EditorView view = viewOf(editor);
        assertNotNull(view, "the Java editor has no pure editor view; the backend changed");
        assertTrue(view.isFocusable(), "the editing surface must be focusable to receive keys");

        String before = textOf(editor);
        assertTrue(before.contains("// <gui-builder-user-code>"), "the companion source lost its markers");

        assertNothingCoversTheEditor(view);
        click(view);
        assertSame(view, view.getComponentForm().getFocused(),
                "clicking the editor did not focus it, so no keystroke can ever reach it");

        type(view, "PINK");
        String after = textOf(editor);

        assertNotEquals(before, after, "typing changed nothing at all; the Java editor is dead to the keyboard");
        assertTrue(after.contains("PINK"),
                "typing must insert the whole word, not drop characters:\n" + userRegion(after));
    }

    @Test
    void typingIntoTheCssEditorReachesTheDocument() throws Exception {
        CodenameOneGUIBuilder builder = workspace();
        open(builder, "openCss", null, null);
        settle();

        CodeEditor editor = builder.activeEditorForTest();
        assertNotNull(editor, "the CSS editor was never created");
        EditorView view = viewOf(editor);
        assertNothingCoversTheEditor(view);
        click(view);
        assertSame(view, view.getComponentForm().getFocused(),
                "clicking the CSS editor did not focus it");
        String before = textOf(editor);

        type(view, "PINK");

        String after = textOf(editor);
        assertNotEquals(before, after, "the CSS editor is dead to the keyboard");
        assertTrue(after.contains("PINK"), "typing must insert the whole word, not drop characters");
    }

    // ---- harness ----------------------------------------------------------------------------

    /** Drives the real key routing: Form focus, then keyPressed/keyReleased per character. */
    private static void type(EditorView view, String text) {
        Form form = view.getComponentForm();
        for (int i = 0; i < text.length(); i++) {
            final int code = text.charAt(i);
            Display.getInstance().callSeriallyAndWait(() -> {
                form.keyPressed(code);
                form.keyReleased(code);
            });
        }
        settle();
    }

    /**
     * The designer layers a drag guide over the whole canvas area, and the editor sits inside that
     * area. If anything covering the editor claims the pointer, every click lands on the cover and
     * the editor can never be focused or typed into -- with no visible sign of why.
     */
    private static void assertNothingCoversTheEditor(EditorView view) {
        int x = view.getAbsoluteX() + view.getWidth() / 2;
        int y = view.getAbsoluteY() + Math.min(view.getHeight() - 2, 20);
        Form form = view.getComponentForm();
        final Component[] hit = new Component[1];
        Display.getInstance().callSeriallyAndWait(() -> hit[0] = form.getComponentAt(x, y));
        assertNotNull(hit[0], "nothing at all is hit testable where the editor is drawn");
        assertFalse(hit[0] instanceof com.codename1.guibuilder.ui.DragGuideOverlay,
                "the drag guide overlay claims the pointer over the editor, so clicks never reach it");
        assertTrue(hit[0] == view || isDescendantOf(hit[0], view) || isDescendantOf(view, hit[0]),
                "a pointer over the editor is claimed by " + hit[0].getClass().getName()
                        + " instead of the editing surface");
    }

    private static boolean isDescendantOf(Component candidate, Component ancestor) {
        for (Container parent = candidate.getParent(); parent != null; parent = parent.getParent()) {
            if (parent == ancestor) return true;
        }
        return false;
    }

    /**
     * Clicks into the editor exactly as a user does, instead of assigning focus by hand. Forcing
     * focus is what made the earlier tests pass while the editor was unusable in the application.
     */
    private static void click(EditorView view) {
        int x = view.getAbsoluteX() + view.getWidth() / 2;
        int y = view.getAbsoluteY() + Math.min(view.getHeight() - 2, 20);
        Form form = view.getComponentForm();
        Display.getInstance().callSeriallyAndWait(() -> {
            form.pointerPressed(x, y);
            form.pointerReleased(x, y);
        });
        settle();
    }

    private static String textOf(CodeEditor editor) {
        final String[] out = new String[1];
        Display.getInstance().callSeriallyAndWait(() -> editor.getText(value -> out[0] = value));
        settle();
        return out[0] == null ? "" : out[0];
    }

    private static EditorView viewOf(CodeEditor editor) {
        return findView(editor);
    }

    private static EditorView findView(Container root) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            if (component instanceof EditorView view) return view;
            if (component instanceof Container container) {
                EditorView nested = findView(container);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static String userRegion(String source) {
        int start = source.indexOf("// <gui-builder-user-code>");
        int end = source.indexOf("// </gui-builder-user-code>");
        return start < 0 || end < 0 ? source : source.substring(start, end + 27);
    }

    private static void open(CodenameOneGUIBuilder builder, String method, Class<?> argType, Object arg) {
        Display.getInstance().callSeriallyAndWait(() -> {
            try {
                java.lang.reflect.Method m = argType == null
                        ? CodenameOneGUIBuilder.class.getDeclaredMethod(method)
                        : CodenameOneGUIBuilder.class.getDeclaredMethod(method, argType);
                m.setAccessible(true);
                if (argType == null) m.invoke(builder); else m.invoke(builder, arg);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static CodenameOneGUIBuilder workspace() throws Exception {
        Path demo = new java.io.File("../demo-project").getCanonicalFile().toPath();
        Path input = Files.createTempFile("guibuilder", ".input");
        Files.write(input, ("projectDir=" + demo + "\nguiDir=" + demo.resolve("src/main/guibuilder")
                + "\nsourceDir=" + demo.resolve("src/main/java")
                + "\ncssFile=" + demo.resolve("src/main/css/theme.css")
                + "\ninitialForm=com.example.NestedLayoutsForm\n").getBytes(StandardCharsets.UTF_8));
        input.toFile().deleteOnExit();
        System.setProperty("guibuilder.input", input.toString());
        System.setProperty("guibuilder.canvasMode", "desktop");
        CodenameOneGUIBuilder builder = new CodenameOneGUIBuilder();
        builder.init(null);
        builder.runApp();
        settle();
        return builder;
    }

    private static void settle() {
        for (int i = 0; i < 4; i++) {
            try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            Display.getInstance().callSeriallyAndWait(() -> { });
        }
    }
}

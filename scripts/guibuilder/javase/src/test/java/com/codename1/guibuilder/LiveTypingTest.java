/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

package com.codename1.guibuilder;

import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.editor.EditorView;
import com.codename1.xml.Element;
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

    @Test
    void typingACssColourRestylesTheCanvas() throws Exception {
        CodenameOneGUIBuilder builder = workspace();
        open(builder, "openCss", null, null);
        settle();

        CodeEditor editor = builder.activeEditorForTest();
        assertNotNull(editor, "the CSS editor was never created");
        EditorView view = viewOf(editor);
        click(view);

        Component label = previewOf(builder, "nestedDescription");
        assertNotNull(label, "the demo form does not render the label under test");
        int before = label.getUnselectedStyle().getFgColor();

        // Replace the stylesheet by selecting everything and typing over it, which is what a person
        // does. The earlier CSS test called the recompile directly and so never exercised the path
        // from a keystroke to a repainted canvas -- the path that was actually broken.
        Display.getInstance().callSeriallyAndWait(() -> view.selectAll());
        type(view, "Label { color: #00ff00; }\n");
        awaitForeground(builder, "nestedDescription", 0x00ff00);

        Component after = previewOf(builder, "nestedDescription");
        assertNotNull(after, "the label vanished after the CSS edit");
        assertEquals(0x00ff00, after.getUnselectedStyle().getFgColor(),
                "typing a colour into theme.css must restyle the canvas; the editor text was: "
                        + textOf(editor));
    }

    private static Component previewOf(CodenameOneGUIBuilder builder, String name) {
        return findByElementName(builder.canvasHostForTest(), name);
    }

    private static Component findByElementName(Container root, String name) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component component = root.getComponentAt(i);
            Object element = component.getClientProperty("gui.element");
            if (element instanceof Element && name.equals(((Element) element).getAttribute("name"))) return component;
            if (component instanceof Container) {
                Component nested = findByElementName(((Container) component), name);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    /** The live CSS path debounces twice, so it needs longer than an ordinary EDT flush. */
    /**
     * Waits for the live recompile to reach the canvas, rather than sleeping a fixed span and
     * hoping. Every keystroke reschedules a 120ms debounce and a 250ms poll drives the recompile,
     * so on a loaded machine -- the whole suite running ahead of this test -- a fixed wait expired
     * before the colour arrived and the assertion failed for timing rather than behaviour. Giving
     * up after the timeout still fails the test, so a colour that never applies is still caught.
     *
     * @param builder the workspace under test
     * @param name the element whose preview is watched
     * @param expected the foreground colour the stylesheet should produce
     */
    private static void awaitForeground(CodenameOneGUIBuilder builder, String name, int expected) {
        for (int attempt = 0; attempt < 100; attempt++) {
            Display.getInstance().callSeriallyAndWait(() -> { });
            Component preview = previewOf(builder, name);
            if (preview != null && preview.getUnselectedStyle().getFgColor() == expected) return;
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
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
            if (component instanceof EditorView) return ((EditorView) component);
            if (component instanceof Container) {
                EditorView nested = findView(((Container) component));
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

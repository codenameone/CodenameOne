package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CodeEditor;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.RichTextArea;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Screenshot coverage for the pure Codename One text editors (no BrowserComponent). Renders a
 * {@link CodeEditor} whose Java source embeds a right-to-left (Hebrew) string literal and comment, plus a
 * {@link RichTextArea} with mixed left-to-right / right-to-left formatted content, so the golden exercises
 * syntax highlighting, the gutter, bidi reordering and rich styled runs in one capture.
 *
 * <p>Hebrew text is written with {@code \\u05Dx} escapes to keep the source ASCII only.</p>
 */
public class PureEditorScreenshotTest extends BaseTest {
    // "shalom" (Hebrew, strong right-to-left)
    private static final String SHALOM = "\u05E9\u05DC\u05D5\u05DD";

    @Override
    public boolean runTest() {
        Form form = createForm("Pure Editors", BoxLayout.y(), "PureEditors");

        CodeEditor code = new CodeEditor("java",
                "public class Greeting {\n"
                + "    // " + SHALOM + " means hello\n"
                + "    String msg = \"" + SHALOM + "\";\n"
                + "    int count = 42;\n"
                + "}\n");
        code.setShowLineNumbers(true);
        code.setPreferredH(Display.getInstance().getDisplayHeight() / 2);
        form.add(code);

        RichTextArea rich = new RichTextArea(
                "<p>Hello <b>world</b> and <i>" + SHALOM + "</i></p>"
                + "<ul><li>one</li><li>two</li></ul>");
        rich.setPreferredH(Display.getInstance().getDisplayHeight() / 3);
        form.add(rich);

        form.show();
        return true;
    }
}

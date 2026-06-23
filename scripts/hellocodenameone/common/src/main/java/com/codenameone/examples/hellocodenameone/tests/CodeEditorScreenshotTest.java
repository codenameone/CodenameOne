package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/**
 * Screenshot coverage for {@link CodeEditor}. Renders a read-only (caret-free, for a deterministic
 * capture) Java snippet with the line-number gutter and syntax highlighting. The editor is hosted in
 * the native web widget, so we wait for it to become ready and then settle before capturing.
 */
public class CodeEditorScreenshotTest extends BaseTest {
    private CodeEditor editor;
    private Form form;
    private Runnable readyRunnable;
    private boolean ready;

    @Override
    public boolean runTest() throws Exception {
        if (!BrowserComponent.isNativeBrowserSupported()) {
            done();
            return true;
        }
        form = createForm("Code Editor", new BorderLayout(), "CodeEditor");
        editor = new CodeEditor();
        editor.setLanguage("java");
        editor.setShowLineNumbers(true);
        // read-only -> no caret, deterministic capture of the highlighted source
        editor.setReadOnly(true);
        editor.setText("public class Main {\n"
                + "    public static void main(String[] args) {\n"
                + "        // greet the user\n"
                + "        int count = 3;\n"
                + "        for (int i = 0; i < count; i++) {\n"
                + "            System.out.println(\"hello \" + i);\n"
                + "        }\n"
                + "    }\n"
                + "}\n");
        editor.addReadyListener(evt -> {
            ready = true;
            maybeSettle();
        });
        form.add(BorderLayout.CENTER, editor);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, final Runnable run) {
        this.readyRunnable = run;
        maybeSettle();
    }

    private void maybeSettle() {
        if (!ready || readyRunnable == null) {
            return;
        }
        UITimer.timer(2000, false, form, readyRunnable);
        readyRunnable = null;
    }
}

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CodeEditor;
import com.codename1.ui.RichTextArea;

/**
 * Editor variants used by the screenshot suite to exercise the lightweight renderer directly.
 *
 * <p>Production editors fall back to {@code BrowserComponent} when a port does not expose the
 * low-level text-input contract.  That is required for editability on the JavaScript port, but the
 * HTML5 screenshot harness cannot capture nested iframe pixels.  These variants keep the visual
 * regression tests focused on the pure renderer without changing production backend selection.</p>
 */
final class ScreenshotPureEditors {
    private ScreenshotPureEditors() {
    }

    static final class Code extends CodeEditor {
        Code() {
        }

        Code(String language, String text) {
            super(language, text);
        }

        @Override
        public boolean isTextInputSupported() {
            return true;
        }
    }

    static final class Rich extends RichTextArea {
        Rich() {
        }

        Rich(String html) {
            super(html);
        }

        @Override
        public boolean isTextInputSupported() {
            return true;
        }
    }
}

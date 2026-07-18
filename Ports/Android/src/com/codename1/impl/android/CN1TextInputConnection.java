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

package com.codename1.impl.android;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;

import com.codename1.ui.Display;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;

/// A custom Android `InputConnection` that binds the platform soft keyboard / IME directly to a
/// Codename One `TextInputClient` without a shadow `EditText`. Committed text, IME composing text,
/// deletions and key commands are marshaled onto the Codename One EDT and delivered to the client, while
/// the surrounding text the IME needs for prediction and composition is answered synchronously from the
/// last editing state snapshot pushed down through `AndroidImplementation`.
class CN1TextInputConnection extends BaseInputConnection {
    private final TextInputClient client;
    // connection-local composing range for the synchronous mirror (the authoritative composing
    // state lives in the client on the EDT); -1 when no composition is being mirrored
    private int mirrorComposeStart = -1;
    private int mirrorComposeEnd = -1;

    CN1TextInputConnection(View targetView, TextInputClient client) {
        super(targetView, true);
        this.client = client;
    }

    private TextInputState state() {
        TextInputState s = AndroidImplementation.currentInputState();
        return s != null ? s : new TextInputState("", 0, 0);
    }

    private void post(final Runnable r) {
        Display.getInstance().callSerially(r);
    }

    /// Applies an in-flight edit to the synchronous mirror so the IME's immediate
    /// getTextBeforeCursor / getExtractedText reads see post-edit text instead of the state
    /// from before the EDT round trip. The mirror is replaced wholesale by the authoritative
    /// state when the EDT echoes it back.
    private int mirrorReplace(int start, int end, String inserted, boolean composing) {
        TextInputState cur = state();
        String t = cur.getText();
        int a = Math.max(0, Math.min(t.length(), Math.min(start, end)));
        int b = Math.max(a, Math.min(t.length(), Math.max(start, end)));
        String nt = t.substring(0, a) + inserted + t.substring(b);
        int caret = a + inserted.length();
        if (composing) {
            mirrorComposeStart = a;
            mirrorComposeEnd = caret;
            return AndroidImplementation.setPendingInputState(new TextInputState(nt, caret, caret, a, caret));
        }
        mirrorComposeStart = -1;
        mirrorComposeEnd = -1;
        return AndroidImplementation.setPendingInputState(new TextInputState(nt, caret, caret));
    }

    private int mirrorReplaceStart(TextInputState cur) {
        return mirrorComposeStart >= 0 ? mirrorComposeStart : cur.getSelectionStart();
    }

    private int mirrorReplaceEnd(TextInputState cur) {
        return mirrorComposeStart >= 0 ? mirrorComposeEnd : cur.getSelectionEnd();
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        final String s = text == null ? "" : text.toString();
        TextInputState cur = state();
        final int seq = mirrorReplace(mirrorReplaceStart(cur), mirrorReplaceEnd(cur), s, false);
        post(new Runnable() {
            public void run() {
                AndroidImplementation.markPendingApplied(seq);
                client.commitText(s);
            }
        });
        return true;
    }

    @Override
    public boolean setComposingText(CharSequence text, final int newCursorPosition) {
        final String s = text == null ? "" : text.toString();
        TextInputState cur = state();
        final int seq = mirrorReplace(mirrorReplaceStart(cur), mirrorReplaceEnd(cur), s, true);
        post(new Runnable() {
            public void run() {
                AndroidImplementation.markPendingApplied(seq);
                client.setComposingText(s, newCursorPosition);
            }
        });
        return true;
    }

    @Override
    public boolean finishComposingText() {
        mirrorComposeStart = -1;
        mirrorComposeEnd = -1;
        post(new Runnable() {
            public void run() {
                client.finishComposing();
            }
        });
        return true;
    }

    @Override
    public boolean deleteSurroundingText(final int beforeLength, final int afterLength) {
        TextInputState cur = state();
        String t = cur.getText();
        int selStart = Math.max(0, Math.min(t.length(), cur.getSelectionStart()));
        int selEnd = Math.max(selStart, Math.min(t.length(), cur.getSelectionEnd()));
        int a = Math.max(0, selStart - Math.max(0, beforeLength));
        int b = Math.min(t.length(), selEnd + Math.max(0, afterLength));
        String nt = t.substring(0, a) + t.substring(selStart, selEnd) + t.substring(b);
        int shift = selStart - a;
        mirrorComposeStart = -1;
        mirrorComposeEnd = -1;
        final int seq = AndroidImplementation.setPendingInputState(
                new TextInputState(nt, selStart - shift, selEnd - shift));
        post(new Runnable() {
            public void run() {
                AndroidImplementation.markPendingApplied(seq);
                client.deleteSurroundingText(beforeLength, afterLength);
            }
        });
        return true;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return true;
        }
        deliverHardwareKey(client, event, true);
        return true;
    }

    /// Shared key translation for IME-synthesized key events (`#sendKeyEvent`) and hardware
    /// keys routed from the view while a session is bound. Returns true when the key belongs
    /// to the client; a key-up of a handled key is consumed without delivering anything.
    static boolean deliverHardwareKey(final TextInputClient client, KeyEvent event, boolean down) {
        int mods = 0;
        if (event.isShiftPressed()) {
            mods |= TextInputClient.MOD_SHIFT;
        }
        if (event.isCtrlPressed()) {
            mods |= TextInputClient.MOD_CTRL;
        }
        if (event.isAltPressed()) {
            mods |= TextInputClient.MOD_ALT;
        }
        final int fmods = mods;
        int keyCode = event.getKeyCode();
        int command = mapKey(keyCode);
        if (command == 0 && (mods & TextInputClient.MOD_CTRL) != 0) {
            command = mapControlKey(keyCode, mods);
        }
        if (command > 0) {
            if (down) {
                final int cmd = command;
                postToEdt(new Runnable() {
                    public void run() {
                        client.onKeyCommand(cmd, fmods);
                    }
                });
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (down) {
                TextInputConfig cfg = AndroidImplementation.currentInputConfig();
                // multiline clients treat ACTION_DEFAULT as a newline; single-line clients get
                // the action the field was configured with (Done / Next / Search / Send)
                final int action = cfg != null && !cfg.isMultiline()
                        ? cfg.getActionType() : TextInputConfig.ACTION_DEFAULT;
                postToEdt(new Runnable() {
                    public void run() {
                        client.onEditorAction(action);
                    }
                });
            }
            return true;
        }
        final int unicode = event.getUnicodeChar();
        if (unicode > 0) {
            if (down) {
                final String s = String.valueOf((char) unicode);
                postToEdt(new Runnable() {
                    public void run() {
                        client.commitText(s);
                    }
                });
            }
            return true;
        }
        return false;
    }

    private static void postToEdt(Runnable r) {
        Display.getInstance().callSerially(r);
    }

    private static int mapKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                return TextInputClient.KEY_BACKSPACE;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                return TextInputClient.KEY_DELETE;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return TextInputClient.KEY_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return TextInputClient.KEY_RIGHT;
            case KeyEvent.KEYCODE_DPAD_UP:
                return TextInputClient.KEY_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return TextInputClient.KEY_DOWN;
            case KeyEvent.KEYCODE_MOVE_HOME:
                return TextInputClient.KEY_HOME;
            case KeyEvent.KEYCODE_MOVE_END:
                return TextInputClient.KEY_END;
            case KeyEvent.KEYCODE_PAGE_UP:
                return TextInputClient.KEY_PAGE_UP;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                return TextInputClient.KEY_PAGE_DOWN;
            case KeyEvent.KEYCODE_ESCAPE:
                return TextInputClient.KEY_ESCAPE;
            case KeyEvent.KEYCODE_TAB:
                // deliver as KEY_TAB so the editor can indent / dedent (shift-tab)
                return TextInputClient.KEY_TAB;
            default:
                return 0;
        }
    }

    /// Hardware-keyboard shortcuts arrive as Ctrl-modified letter key events rather than through the
    /// IME text pipeline; map the editing shortcuts every desktop-class keyboard user expects.
    private static int mapControlKey(int keyCode, int mods) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_A:
                return TextInputClient.KEY_SELECT_ALL;
            case KeyEvent.KEYCODE_Z:
                return (mods & TextInputClient.MOD_SHIFT) != 0
                        ? TextInputClient.KEY_REDO : TextInputClient.KEY_UNDO;
            case KeyEvent.KEYCODE_Y:
                return TextInputClient.KEY_REDO;
            case KeyEvent.KEYCODE_C:
                return TextInputClient.KEY_COPY;
            case KeyEvent.KEYCODE_X:
                return TextInputClient.KEY_CUT;
            case KeyEvent.KEYCODE_V:
                return TextInputClient.KEY_PASTE;
            default:
                return 0;
        }
    }

    @Override
    public boolean performEditorAction(int actionCode) {
        final int action = AndroidImplementation.textInputActionFor(actionCode);
        post(new Runnable() {
            public void run() {
                client.onEditorAction(action);
            }
        });
        return true;
    }

    @Override
    public boolean setComposingRegion(final int start, final int end) {
        // Gboard marks an existing word for recomposition (tap into a word, pick a
        // suggestion) and expects the next setComposingText to replace that region.
        // The client contract replaces the selection when no composition is active,
        // so mirror the region as the selection.
        mirrorComposeStart = Math.min(start, end);
        mirrorComposeEnd = Math.max(start, end);
        post(new Runnable() {
            public void run() {
                client.setSelectionRange(Math.min(start, end), Math.max(start, end));
            }
        });
        return true;
    }

    @Override
    public CharSequence getTextBeforeCursor(int length, int flags) {
        TextInputState s = state();
        int start = Math.max(0, s.getSelectionStart() - length);
        String t = s.getText();
        int end = Math.min(t.length(), s.getSelectionStart());
        if (end < start) {
            return "";
        }
        return t.substring(start, end);
    }

    @Override
    public CharSequence getTextAfterCursor(int length, int flags) {
        TextInputState s = state();
        String t = s.getText();
        int start = Math.min(t.length(), s.getSelectionEnd());
        int end = Math.min(t.length(), start + length);
        return t.substring(start, end);
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        TextInputState s = state();
        String t = s.getText();
        int a = Math.max(0, Math.min(t.length(), s.getSelectionStart()));
        int b = Math.max(a, Math.min(t.length(), s.getSelectionEnd()));
        if (b <= a) {
            return null;
        }
        return t.substring(a, b);
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        TextInputState s = state();
        ExtractedText et = new ExtractedText();
        et.text = s.getText();
        et.startOffset = 0;
        et.selectionStart = s.getSelectionStart();
        et.selectionEnd = s.getSelectionEnd();
        et.partialStartOffset = -1;
        et.partialEndOffset = -1;
        return et;
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        return 0;
    }
}

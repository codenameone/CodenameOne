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

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        final String s = text == null ? "" : text.toString();
        post(new Runnable() {
            public void run() {
                client.commitText(s);
            }
        });
        return true;
    }

    @Override
    public boolean setComposingText(CharSequence text, final int newCursorPosition) {
        final String s = text == null ? "" : text.toString();
        post(new Runnable() {
            public void run() {
                client.setComposingText(s, newCursorPosition);
            }
        });
        return true;
    }

    @Override
    public boolean finishComposingText() {
        post(new Runnable() {
            public void run() {
                client.finishComposing();
            }
        });
        return true;
    }

    @Override
    public boolean deleteSurroundingText(final int beforeLength, final int afterLength) {
        post(new Runnable() {
            public void run() {
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
        if (command > 0) {
            final int cmd = command;
            post(new Runnable() {
                public void run() {
                    client.onKeyCommand(cmd, fmods);
                }
            });
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            post(new Runnable() {
                public void run() {
                    client.onEditorAction(TextInputConfig.ACTION_DEFAULT);
                }
            });
            return true;
        }
        final int unicode = event.getUnicodeChar();
        if (unicode > 0) {
            final String s = String.valueOf((char) unicode);
            post(new Runnable() {
                public void run() {
                    client.commitText(s);
                }
            });
            return true;
        }
        return true;
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
            case KeyEvent.KEYCODE_ESCAPE:
                return TextInputClient.KEY_ESCAPE;
            default:
                return 0;
        }
    }

    @Override
    public boolean performEditorAction(int actionCode) {
        post(new Runnable() {
            public void run() {
                client.onEditorAction(TextInputConfig.ACTION_DEFAULT);
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

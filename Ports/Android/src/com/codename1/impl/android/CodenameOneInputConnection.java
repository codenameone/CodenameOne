/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.TextField;

/**
 *
 */
public class CodenameOneInputConnection extends BaseInputConnection {

    private String composingText = "";
    private Editable edit = new SpannableStringBuilder();
    private ExtractedTextRequest request;
    private View view;

    public CodenameOneInputConnection(View view) {
        super(view, true);
        this.view = view;
    }

    @Override
    public boolean performEditorAction(int actionCode) {
        if (Display.isInitialized() && Display.getInstance().getCurrent() != null) {
            Component txtCmp = Display.getInstance().getCurrent().getFocused();
            if (txtCmp != null && txtCmp instanceof TextField) {
                TextField t = (TextField) txtCmp;
                if (actionCode == EditorInfo.IME_ACTION_DONE) {
                    Display.getInstance().setShowVirtualKeyboard(false);
                } else if (actionCode == EditorInfo.IME_ACTION_NEXT) {
                    Display.getInstance().setShowVirtualKeyboard(false);
                    txtCmp.getNextFocusDown().requestFocus();
                }
            }
        }

        return super.performEditorAction(actionCode);
    }

    public Editable getEditable() {
        if (Display.isInitialized() && Display.getInstance().getCurrent() != null) {
            Component txtCmp = Display.getInstance().getCurrent().getFocused();
            if (txtCmp != null && txtCmp instanceof TextField) {
                TextField t = (TextField) txtCmp;
                String textFieldText = t.getText();
                edit.clear();
                edit.append(textFieldText);
                return edit;
            }
        }
        return super.getEditable();
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        
        if (Display.isInitialized() && Display.getInstance().getCurrent() != null) {
            Component txtCmp = Display.getInstance().getCurrent().getFocused();
            if (txtCmp != null && txtCmp instanceof TextField) {
                TextField t = (TextField) txtCmp;
                String textFieldText = t.getText();
                int cursorPosition = t.getCursorPosition();
                StringBuilder sb = new StringBuilder(textFieldText);
                if (text.equals("\n")) {
                    //System.out.println("hello backslash");
                }

                if (composingText.length() > 0) {
                    if (text.equals(" ")) {
                        return commitText(composingText + " ", newCursorPosition);
                    }
                    sb.replace(sb.length() - composingText.length(), sb.length(), text.toString());
                    composingText = "";
                } else {
                    sb.insert(cursorPosition, text);
                }

                t.setText(sb.toString());
                t.setCursorPosition(cursorPosition + text.length());

                updateExtractedText();
            }
        }
        return super.commitText(text, newCursorPosition);
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        if (Display.isInitialized() && Display.getInstance().getCurrent() != null) {
            Component txtCmp = Display.getInstance().getCurrent().getFocused();
            if (txtCmp != null && txtCmp instanceof TextField) {
                TextField t = (TextField) txtCmp;
                String textFieldText = t.getText();
                StringBuilder sb = new StringBuilder(textFieldText);
                sb.replace(sb.length() - composingText.length(), sb.length(), text.toString());
                int cursorPosition = t.getCursorPosition();
                composingText = text.toString();
                t.setText(sb.toString());
                t.setCursorPosition(cursorPosition + text.length());

                updateExtractedText();
                return true;
            }
        }
        return false;
    }

    @Override
    public CharSequence getTextBeforeCursor(int length, int flags) {
        if (Display.isInitialized() && Display.getInstance().getCurrent() != null) {
            Component txtCmp = Display.getInstance().getCurrent().getFocused();
            if (txtCmp != null && txtCmp instanceof TextField) {
                String txt = ((TextField) txtCmp).getText();
                int position = ((TextField) txtCmp).getCursorPosition();
                int start;
                if (position > 0) {
                    start = txt.subSequence(0, position).toString().lastIndexOf(" ");
                    if (start > 0) {
                        return txt.subSequence(start, position);
                    } else {
                        return txt.subSequence(0, position);
                    }
                }
            }
        }
        return "";
    }

    @Override
    public CharSequence getTextAfterCursor(int length, int flags) {
        if (Display.isInitialized() && Display.getInstance().getCurrent() != null) {
            Component txtCmp = Display.getInstance().getCurrent().getFocused();
            if (txtCmp != null && txtCmp instanceof TextField) {
                String txt = ((TextField) txtCmp).getText();
                int position = ((TextField) txtCmp).getCursorPosition();
                if (position > -1 && position < txt.length()) {
                    return txt.subSequence(position, txt.length() - 1);
                }
            }
        }
        return "";
    }

    private void updateExtractedText() {

        if (request != null) {
            InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            ExtractedText et = new ExtractedText();
            extractText(request, et);
            manager.updateExtractedText(view, request.token, et);
        }

    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        if (Display.isInitialized() && Display.getInstance().getCurrent() != null) {
            this.request = request;
            ExtractedText et = new ExtractedText();
            if (extractText(request, et)) {
                return et;
            }
        }
        return null;
    }

    public boolean extractText(ExtractedTextRequest request,
            ExtractedText outText) {
        return extractTextInternal(request, outText);
    }

    boolean extractTextInternal(ExtractedTextRequest request, ExtractedText outText) {

        Component txtCmp = Display.getInstance().getCurrent().getFocused();
        if (txtCmp != null && txtCmp instanceof TextField) {
            String txt = ((TextField) txtCmp).getText();
            int partialStartOffset = -1;
            int partialEndOffset = -1;
            final CharSequence content = txt;
            if (content != null) {
                final int N = content.length();
                outText.partialStartOffset = outText.partialEndOffset = -1;
                partialStartOffset = 0;
                partialEndOffset = N;

                if ((request.flags & InputConnection.GET_TEXT_WITH_STYLES) != 0) {
                    outText.text = content.subSequence(partialStartOffset,
                            partialEndOffset);
                } else {
                    outText.text = TextUtils.substring(content, partialStartOffset,
                            partialEndOffset);
                }

                outText.flags = 0;
                outText.flags |= ExtractedText.FLAG_SINGLE_LINE;
                outText.startOffset = 0;
                outText.selectionStart = Selection.getSelectionStart(content);
                outText.selectionEnd = Selection.getSelectionEnd(content);
                return true;
            }

        }
        return false;
    }
}

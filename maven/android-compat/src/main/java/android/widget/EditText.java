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
package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import com.codename1.androidcompat.runtime.EditTextPeer;
import com.codename1.ui.Component;

/// An editable text field. Text entry is Codename One's native editing --
/// the platform keyboard, autocorrect, accessibility -- through a text field
/// peer; Android draws the background (the underline) and the view keeps the
/// text in an [Editable] that tracks every keystroke.
public class EditText extends TextView {

    private EditTextPeer mEditPeer;
    private boolean mSyncing;
    /// The selection last requested through `setSelection`, kept until the
    /// text changes or the cursor moves away from its end. The native field
    /// has a cursor and no selection range, so a range is reported back as
    /// requested while the cursor sits at its end; typing then inserts at
    /// that end rather than replacing the range, which no Codename One
    /// field can do.
    private boolean mSelSet;
    private int mSelStart;
    private int mSelEnd;

    public EditText(Context context) {
        this(context, null);
    }

    public EditText(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public EditText(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (rawInputType() == INPUT_UNSET) {
            setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        } else if ((rawInputType() & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0) {
            super.setSingleLine(true);
        }
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
    }

    @Override
    protected Component createPeer() {
        mEditPeer = new EditTextPeer(this);
        if (mSelSet) {
            // Requested before the peer existed (onCreate() usually).
            mEditPeer.setCursor(mSelEnd);
        }
        return mEditPeer;
    }

    @Override
    public Editable getText() {
        if (!(mText instanceof Editable)) {
            mText = new SpannableStringBuilder(mText);
        }
        return (Editable) mText;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, BufferType.EDITABLE);
    }

    @Override
    void onTextReplaced(CharSequence text) {
        if (text instanceof SpannableStringBuilder) {
            ((SpannableStringBuilder) text).addChangeHook(new SpannableStringBuilder.ChangeHook() {
                @Override
                public void beforeChange(SpannableStringBuilder t, int start, int before, int after) {
                    // The Editable applies filters before reporting the actual
                    // replacement, for both native input and application edits.
                    if (t == mText) {
                        sendBeforeTextChanged(t, start, before, after);
                    }
                }

                @Override
                public void afterChange(SpannableStringBuilder t, int start, int before, int after) {
                    boolean notify = t == mText;
                    pushTextToPeer();
                    if (notify) {
                        sendOnTextChanged(t, start, before, after);
                        sendAfterTextChanged();
                    }
                    textChanged();
                }
            });
        }
        pushTextToPeer();
    }

    private void pushTextToPeer() {
        if (mEditPeer != null && !mSyncing) {
            mSyncing = true;
            try {
                mEditPeer.setTextFromView(mText.toString());
            } finally {
                mSyncing = false;
            }
        }
    }

    /// Runtime use: the user edited the native field.
    public void onPeerTextChanged(String newText) {
        if (mSyncing) {
            return;
        }
        String old = mText.toString();
        if (old.equals(newText)) {
            return;
        }
        int start = 0;
        int max = Math.min(old.length(), newText.length());
        while (start < max && old.charAt(start) == newText.charAt(start)) {
            start++;
        }
        int oldEnd = old.length();
        int newEnd = newText.length();
        while (oldEnd > start && newEnd > start && old.charAt(oldEnd - 1) == newText.charAt(newEnd - 1)) {
            oldEnd--;
            newEnd--;
        }
        mSyncing = true;
        try {
            Editable e = getText();
            e.replace(start, oldEnd, newText.substring(start, newEnd));
        } finally {
            mSyncing = false;
        }
        String filtered = mText.toString();
        if (!filtered.equals(newText) && mEditPeer != null) {
            // An input filter (maxLength) rejected part of the edit.
            mEditPeer.setTextFromView(filtered);
        }
        textChanged();
    }

    public void setSelection(int start, int stop) {
        mSelSet = true;
        mSelStart = start;
        mSelEnd = stop;
        if (mEditPeer != null) {
            mEditPeer.setCursor(stop);
        }
    }

    public void setSelection(int index) {
        setSelection(index, index);
    }

    /// An editable view's text is always saved with its state.
    @Override
    public boolean getFreezesText() {
        return true;
    }

    @Override
    void restoreSelection(int start, int end) {
        int len = length();
        setSelection(Math.max(0, Math.min(start, len)), Math.max(0, Math.min(end, len)));
    }

    public void selectAll() {
        setSelection(0, length());
    }

    public void extendSelection(int index) {
        setSelection(index);
    }

    @Override
    public int getSelectionStart() {
        return requestedSelectionHolds() ? Math.min(mSelStart, length()) : cursor();
    }

    @Override
    public int getSelectionEnd() {
        return requestedSelectionHolds() ? Math.min(mSelEnd, length()) : cursor();
    }

    private int cursor() {
        return mEditPeer == null || !mEditPeer.tracksCursor() ? length() : mEditPeer.cursor();
    }

    private boolean requestedSelectionHolds() {
        if (!mSelSet) {
            return false;
        }
        if (mEditPeer == null || !mEditPeer.tracksCursor()) {
            return true;
        }
        if (mEditPeer.cursor() != Math.min(mSelEnd, length())) {
            // The user moved the cursor.
            mSelSet = false;
            return false;
        }
        return true;
    }

    @Override
    public void setInputType(int type) {
        super.setInputType(type);
        if (mEditPeer != null) {
            mEditPeer.syncFromView();
        }
    }

    @Override
    public void setHint(CharSequence hint) {
        super.setHint(hint);
        if (mEditPeer != null) {
            mEditPeer.syncFromView();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mEditPeer != null) {
            mEditPeer.setEnabled(enabled);
        }
    }

    /// Starts native editing (shows the keyboard).
    public void startEditing() {
        requestFocus();
        if (mEditPeer != null && isEnabled()) {
            mEditPeer.startEditingAsync();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean r = super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP && isEnabled() && isFocusable()) {
            startEditing();
        }
        return r;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    /// The native field draws the text; the view draws the background and
    /// compound drawables underneath it.
    @Override
    protected void onDraw(Canvas canvas) {
        drawCompoundDrawables(canvas);
    }

    @Override
    protected void textChanged() {
        super.textChanged();
        mSelSet = false;
        if (mEditPeer != null) {
            mEditPeer.syncFromView();
        }
    }

    /// The action the keyboard's enter key performs, by Android's rules.
    public int imeAction() {
        int action = getImeOptions() & EditorInfo.IME_MASK_ACTION;
        if (action == EditorInfo.IME_ACTION_UNSPECIFIED) {
            return isSingleLine() ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_UNSPECIFIED;
        }
        return action;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return EditText.class.getName();
    }
}

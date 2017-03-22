/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lior.gonnen
 *
 */
public class InPlaceEditView extends FrameLayout{

    private static final String TAG = "InPlaceEditView";
    public static final int REASON_UNDEFINED = 0;
    public static final int REASON_IME_ACTION = 1;
    public static final int REASON_TOUCH_OUTSIDE = 2;
    public static final int REASON_SYSTEM_KEY = 3;

    static void scrollActiveTextfieldToVisible() {
        if (isEditing() && sInstance != null) {
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    if (sInstance != null && sInstance.mEditText != null && sInstance.mEditText.mTextArea != null) {
                        TextArea ta = sInstance.mEditText.mTextArea;
                        if (isScrollableParent(ta)) {
                            ta.scrollRectToVisible(0, 0, ta.getWidth(), ta.getHeight(), ta);
                            ta.getComponentForm().getAnimationManager().flushAnimation(new Runnable() {

                                @Override
                                public void run() {
                                    reLayoutEdit();
                                }
                                
                            });
                        }
                    }
                }
                
            };
        }
    }
    // The native Android edit-box to place over Codename One's edit-component
    private EditView mEditText = null;
    private EditView mLastEditText = null;
    // The Codename One edit-component we're editing
    // The EditText's layout parameters
    private FrameLayout.LayoutParams mEditLayoutParams;
    // Reference to the system's input method manager
    private InputMethodManager mInputManager;
    // True while editing is in progress
    private static boolean mIsEditing = false;
    private static Object editingLock = new Object();
    private static boolean waitingForSynchronousEditingCompletion = false;
    // Maps Codename One's input-types to Android input-types
    private SparseIntArray mInputTypeMap = new SparseIntArray(10);
    // Receives results from the InputMethodManager after calling show/hide soft-keyboard methods
    private ResultReceiver mResultReceiver;
    private int mLastEndEditReason = REASON_UNDEFINED;
    private Resources mResources;
    // Only a single instance of this class can exist
    private static InPlaceEditView sInstance = null;
    private static TextArea nextTextArea = null;
    private AndroidImplementation impl;
    private static long closedTime;
    private static boolean showVKB = false;
    private static boolean isClosing = false;

    // Flag to indicate that the text editor is currently hidden - but an async edit
    // is still in progress.  This flag is only relevant in async edit mode.
    private boolean textEditorHidden = false;

    private static boolean resizeMode;

    // Used to buffer input while the native editor is being initialized
    // This is necessary because initialization may require us to
    // asynchronously run code on the EDT to obtain the current text area
    // text, and then again asynchronously on the UI thread to set the
    // text, and, in the mean time, the user may have typed some text.
    private List<TextChange> inputBuffer;
    private static Runnable afterClose;


    /**
     * Private constructor
     * To use this class, call the static 'edit' method.
     * @param impl The current running activity
     */
    private InPlaceEditView(final AndroidImplementation impl) {
        super(impl.getActivity());
        this.impl = impl;
        mResources = impl.getActivity().getResources();
        mResultReceiver = new DebugResultReceiver(getHandler());
        mInputManager = (InputMethodManager) impl.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        // We place this view as an overlay that takes up the entire screen
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setFocusableInTouchMode(true);
        initInputTypeMap();
        setBackgroundDrawable(null);

    }






    /**
     * Prepare an int-to-int map that maps Codename One input-types to
     * Android input types
     */
    private void initInputTypeMap() {
        mInputTypeMap.append(TextArea.ANY, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        mInputTypeMap.append(TextArea.DECIMAL, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        mInputTypeMap.append(TextArea.EMAILADDR, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        mInputTypeMap.append(TextArea.INITIAL_CAPS_SENTENCE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        mInputTypeMap.append(TextArea.INITIAL_CAPS_WORD, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mInputTypeMap.append(TextArea.NON_PREDICTIVE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mInputTypeMap.append(TextArea.NUMERIC, InputType.TYPE_CLASS_NUMBER);
        mInputTypeMap.append(TextArea.PASSWORD, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mInputTypeMap.append(TextArea.PHONENUMBER, InputType.TYPE_CLASS_PHONE);
        mInputTypeMap.append(TextArea.URL, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    }

    /**
     * Get the Android equivalent input type for a given Codename One input-type
     * @param codenameOneInputType One of the com.codename1.ui.TextArea input type constants
     * @return The Android equivalent of the given input type
     */
    private int getAndroidInputType(int codenameOneInputType) {
        int type = mInputTypeMap.get(codenameOneInputType, InputType.TYPE_CLASS_TEXT);

        // If we're editing standard text, disable auto complete.
        // The name of the flag is a little misleading. From the docs:
        // the text editor is performing auto-completion of the text being entered
        // based on its own semantics, which it will present to the user as they type.
        // This generally means that the input method should not be showing candidates itself,
        // but can expect for the editor to supply its own completions/candidates from
        // InputMethodSession.displayCompletions().
        if ((type & InputType.TYPE_CLASS_TEXT) != 0) {
            type |= InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
        }
        return type;
    }



    /**
     * Shows the native text editor for the async editing session that is currently in progress.
     * This is only used when in async edit mode.
     */
    static void showActiveTextEditorAgain() {
        if (sInstance != null) {
            sInstance.showTextEditorAgain();
        }
    }


    /**
     * Allows the implementation to refresh the text field
     */
    protected final void repaintTextEditor(final boolean focus) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (mEditText != null && mEditText.mTextArea != null) {
                    mEditText.mTextArea.repaint();
                    if (focus) {
                        mEditText.mTextArea.requestFocus();
                    }
                }
            }
        });
    }


    /**
     * Shows the native text field again after it has been hidden in async edit mode.
     */
    private void showTextEditorAgain() {
        if (!mIsEditing || !isTextEditorHidden()) {
            return;
        }
        textEditorHidden = false;
        final TextArea ta = mEditText.mTextArea;

        // Set the input buffer to catch keyboard input occurring between now
        // and when we have updated the native editor's text to match the
        // current state of the textarea.
        // This is necessary in case the textarea's text has been programmatically
        // changed since the native aread was hidden.
        synchronized (this) {
            inputBuffer = new ArrayList<TextChange>();
        }

        // We are probably not on the EDT.  We need to be on the EDT to
        // safely get text from the textarea for synchronization.
        Display.getInstance().callSerially(new Runnable() {
            public void run() {

                // Double check that the state is still correct.. i.e. we are editing
                // and the editing text area hasn't changed since we issued this call.
                if (mIsEditing && mEditText != null && mEditText.mTextArea == ta) {
                    final String text = ta.getText();
                    final int cursorPos = ta.getCursorPosition();
                    // Now that we have our text from the CN1 text area, we need to be on the
                    // Android UI thread in order to set the text of the native text editor.
                    impl.getActivity().runOnUiThread(new Runnable() {
                        public void run() {

                            // Double check that the state is still correct.  I.e. we are editing
                            // and the editing text area hasn't changed since we issued this call.
                            if (mIsEditing && mEditText != null && mEditText.mTextArea == ta) {


                                // We will synchronize here mainly for the benefit of the inputBuffer
                                // so that we don't find it in an inconsistent state.
                                synchronized (InPlaceEditView.this) {

                                    // Let's record the cursor positions of the native
                                    // text editor in case we need to use them after synchronizing
                                    // with the CN1 textarea.
                                    int start = cursorPos;
                                    int end = cursorPos;

                                    /*
                                    if (!inputBuffer.isEmpty()) {
                                        // If the input buffer isn't empty, then our start
                                        // and end positions will be "wonky"
                                        start = end = inputBuffer.get(0).atPos;

                                        // If the first change was a delete, then the atPos
                                        // will point to the beginning of the deleted section
                                        // so we need to adjust the end point to be *after*
                                        // the deleted section to begin.
                                        if (inputBuffer.get(0).deleteLength > 0) {
                                            end = start = end + inputBuffer.get(0).deleteLength;
                                        }
                                    }
                                    */


                                    StringBuilder buf = new StringBuilder();
                                    buf.append(text);


                                    // Loop through any pending changes in the input buffer
                                    // (I.e. key strokes that have occurred since we initiated
                                    // this async callback hell!!)
                                    List<TextChange> tinput = inputBuffer;
                                    if(tinput != null) {
                                        for (TextChange change : tinput) {

                                            // This change is "added" text.  Try to add it
                                            // at the correct cursor position.  if not, add it at the
                                            // end.
                                            if (change.textToAppend != null) {
                                                if (end >= 0 && end <= buf.length()) {
                                                    buf.insert(end, change.textToAppend);
                                                    end += change.textToAppend.length();
                                                    start = end;
                                                } else {
                                                    buf.append(change.textToAppend);
                                                    end = buf.length();
                                                    start = end;
                                                }

                                            }

                                            // The change is "deleted" text.
                                            else if (change.deleteLength > 0) {
                                                if (end >= change.deleteLength && end <= buf.length()) {
                                                    buf.delete(end - change.deleteLength, end);
                                                    end -= change.deleteLength;
                                                    start = end;
                                                } else if (end > 0 && end < change.deleteLength) {
                                                    buf.delete(0, end);
                                                    end = 0;
                                                    start = end;
                                                }
                                            }
                                        }
                                    }

                                    // Important:  Clear the input buffer so that the TextWatcher
                                    // knows to stop filling it up.  We only need the inputBuffer
                                    // to keep input between the original showTextEditorAgain() call
                                    // and here.
                                    inputBuffer = null;
                                    mEditText.setText(buf.toString());
                                    if (start < 0 || start > mEditText.getText().length()) {
                                        start = mEditText.getText().length();
                                    }
                                    if (end < 0 || end > mEditText.getText().length()) {
                                        end = mEditText.getText().length();
                                    }

                                    // Update the caret in the edit text field so we can continue.
                                    mEditText.setSelection(start, end);

                                }
                            }
                        }
                    });
                }

            }
        });
        reLayoutEdit(true);
        repaintTextEditor(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        endEdit(true);
    }

    /**
     * Hides the native text editor while keeping the active async edit session going.
     * This will effectively hide the native text editor, and show the light-weight text area
     * with cursor still in the correct position.
     *
     * <p>This is just a static wrapper around {@link #hideTextEditor()}</p>
     */
    static void hideActiveTextEditor() {
        if (sInstance != null) {
            sInstance.hideTextEditor();
        }
    }

    /**
     * Hides the native text editor while keeping the active async edit session going.
     * This will effectively hide the native text editor, and show the light-weight text area
     * with cursor still in the correct position.
     */
    private void hideTextEditor() {
        if (!mIsEditing || textEditorHidden || mEditText == null) {
            return;
        }
        textEditorHidden = true;
        final TextArea ta = mEditText.mTextArea;

        // Since this may be called off the UI thread, we need to issue async request on UI thread
        // to hide the text area.
        impl.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (mEditText != null && mEditText.mTextArea == ta) {

                    // Note:  Setting visibility to GONE doesn't work here because the TextWatcher
                    // will stop receiving input from the keyboard, so we don't have a way to
                    // reactivate the text editor when the user starts typing again.  Using the margin
                    // to move it off screen keeps the text editor active.
                    mEditLayoutParams.setMargins(-Display.getInstance().getDisplayWidth(), 0, 0, 0);
                    InPlaceEditView.this.requestLayout();
                    final int cursorPos = mEditText.getSelectionStart();

                    // Since we are going to be displaying the CN1 text area now, we need to update
                    // the cursor.  That needs to happen on the EDT.
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            if (mEditText != null && mEditText.mTextArea == ta && mIsEditing && textEditorHidden) {
                                if (ta instanceof TextField) {
                                    ((TextField)ta).setCursorPosition(cursorPos);
                                }
                            }
                        }
                    });
                }
            }
        });


        // Repaint the CN1 text area on the EDT.  This is necessary because while the native editor
        // was shown, the cn1 text area paints only its background.  Now that the editor is hidden
        // it should paint its foreground also.
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (mEditText != null && mEditText.mTextArea != null) {
                    mEditText.mTextArea.repaint();
                }
            }
        });

        //repaintTextEditor(true);

    }


    /**
     * Checks if the native text editor is currently hidden.  Only relevant in async edit mode.
     *
     * <p>This is just a static wrapper around {@link #isTextEditorHidden()}</p>
     * @return
     */
    static boolean isActiveTextEditorHidden() {
        if (sInstance != null) {
            return sInstance.isTextEditorHidden();
        }
        return true;
    }

    /**
     * Checks if the native text editor is currently hidden.  Only relevant in async edit mode.
     * @return
     */
    private boolean isTextEditorHidden() {
        return textEditorHidden;
    }

    /*
    static void handleActiveTouchEventIfHidden(MotionEvent event) {
        if (sInstance != null && mIsEditing && isActiveTextEditorHidden()) {
            sInstance.onTouchEvent(event);
        }
    }
    */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!impl.isAsyncEditMode()) {
            boolean leaveVKBOpen = false;
            if (mEditText != null && mEditText.mTextArea != null && mEditText.mTextArea.getComponentForm() != null) {
                Component c = mEditText.mTextArea.getComponentForm().getComponentAt((int) event.getX(), (int) event.getY());
                if ( mEditText.mTextArea.getClientProperty("leaveVKBOpen") != null
                        || (c != null && c instanceof TextArea && ((TextArea) c).isEditable() && ((TextArea) c).isEnabled())) {
                    leaveVKBOpen = true;
                }
            }
            // When the user touches the screen outside the text-area, finish editing
            endEditing(REASON_TOUCH_OUTSIDE, leaveVKBOpen);
        } else {
            final int evtX = (int) event.getX();
            final int evtY = (int) event.getY();
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    if (mEditText != null && mEditText.mTextArea != null) {
                        TextArea tx = mEditText.mTextArea;
                        int x = tx.getAbsoluteX() + tx.getScrollX();
                        int y = tx.getAbsoluteY() + tx.getScrollY();
                        int w = tx.getWidth();
                        int h = tx.getHeight();
                        if (!(x <= evtX && y <= evtY && x + w >= evtX && y + h >= evtY)) {
                            hideTextEditor();
                        } else {
                            showTextEditorAgain();
                        }

                    }
                }
            });

        }

        // Return false so that the event will propagate to the underlying view
        // We don't want to consume this event
        return false;
    }

    /**
     * Show or hide the virtual keyboard if necessary
     * @param show Show the keyboard if true, hide it otherwise
     */
    private void showVirtualKeyboard(boolean show) {
        Log.i(TAG, "showVirtualKeyboard show=" + show);

        boolean result = false;
        if (show) {
            // If we're in landscape, Android will not show the soft
            // keyboard unless SHOW_FORCED is requested
            Configuration config = mResources.getConfiguration();

            boolean isLandscape = (config.orientation == Configuration.ORIENTATION_LANDSCAPE);
            int showFlags = isLandscape ? InputMethodManager.SHOW_FORCED : InputMethodManager.SHOW_IMPLICIT;

            mInputManager.restartInput(mEditText);
            result = mInputManager.showSoftInput(mEditText, showFlags, mResultReceiver);
        } else {
            if(mEditText == null){
                if(showVKB){
                    mInputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            }else{
                result = mInputManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0, mResultReceiver);
            }
            closedTime = System.currentTimeMillis();
        }
        showVKB = show;

        final boolean showKeyboard = showVKB;
        final ActionListener listener = Display.getInstance().getVirtualKeyboardListener();
        if(listener != null){
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {

                    //this is ugly but there is no real API to know if the
                    //keyboard is opened or closed
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException ex) {
                    }

                    Display.getInstance().callSerially(new Runnable() {

                        @Override
                        public void run() {
                            ActionEvent evt = new ActionEvent(showKeyboard);
                            listener.actionPerformed(evt);
                        }
                    });
                }
            });
            t.setUncaughtExceptionHandler(AndroidImplementation.exceptionHandler);
            t.start();
        }

        Log.d(TAG, "InputMethodManager returned " + Boolean.toString(result).toUpperCase());
    }

    /**
     * Returns true if the keyboard is currently on screen.
     */
    public static boolean isKeyboardShowing(){
        //There is no android API to know if the keyboard is currently showing
        //This method will return false after 2 seconds since the keyboard was
        //requested to be closed
        return showVKB || (System.currentTimeMillis() - closedTime) < 2000;
    }

    static class TextAreaData {
        final int absoluteY;
        final int absoluteX;
        final int paddingTop;
        final int paddingLeft;
        final int paddingRight;
        final int paddingBottom;
        final int scrollX;
        final int scrollY;
        final int verticalAlignment;
        final int height;
        final int width;
        final int fontHeight;
        final TextArea textArea;
        final Component nextDown;
        final boolean isRTL;
        final boolean isSingleLineTextArea;
        final String hint;
        final boolean nativeHintBool;
        final Object nativeFont;
        final int fgColor;
        final int maxSize;

        final boolean isTextField;

        int getAbsoluteY() {
            return absoluteY;
        }

        int getAbsoluteX() {
            return absoluteX;
        }

        int getScrollX() {
            return scrollX;
        }

        int getScrollY() {
            return scrollY;
        }

        int getHeight() {
            return height;
        }

        int getWidth() {
            return width;
        }

        int getVerticalAlignment() {
            return verticalAlignment;
        }

        boolean isRTL() {
            return isRTL;
        }

        boolean isSingleLineTextArea() {
            return isSingleLineTextArea;
        }

        Object getClientProperty(String key) {
            return textArea.getClientProperty(key);
        }

        void putClientProperty(String key, Object value) {
            textArea.putClientProperty(key, value);
        }

        Object getDoneListener() {
            if (isTextField) {
                return ((TextField)textArea).getDoneListener();
            }
            return null;
        }

        String getHint() {
            return hint;
        }



        TextAreaData(TextArea ta) {

            absoluteX = ta.getAbsoluteX();
            absoluteY = ta.getAbsoluteY();
            scrollX = ta.getScrollX();
            scrollY = ta.getScrollY();
            Style s = ta.getStyle();
            paddingTop = s.getPaddingTop();
            paddingLeft = s.getPaddingLeft(ta.isRTL());
            paddingRight = s.getPaddingRight(ta.isRTL());
            paddingBottom = s.getPaddingBottom();
            isTextField = (ta instanceof TextField);
            verticalAlignment = ta.getVerticalAlignment();
            height = ta.getHeight();
            width = ta.getWidth();
            fontHeight = s.getFont().getHeight();
            textArea = ta;
            isRTL = ta.isRTL();
            nextDown = textArea.getNextFocusDown() != null ? textArea.getNextFocusDown() : textArea.getComponentForm().findNextFocusVertical(true);
            isSingleLineTextArea = textArea.isSingleLineTextArea();
            hint = ta.getHint();
            nativeHintBool = textArea.getUIManager().isThemeConstant("nativeHintBool", false);
            nativeFont = s.getFont().getNativeFont();
            fgColor = s.getFgColor();
            maxSize = ta.getMaxSize();
        }

    }
    
    // Timers for manually blinking cursor on Android 4.4
    private Timer cursorTimer;
    private TimerTask cursorTimerTask;

    /**
     * Start editing the given text-area
     * This method is executed on the UI thread, so UI manipulation is safe here.
     * @param activity Current running activity
     * @param textArea The TextAreaData instance that wraps the CN1 TextArea that our internal EditText needs to overlap.  We use
     *                 a TextAreaData so that the text area properties can be accessed off the EDT safely.
     * @param codenameOneInputType One of the input type constants in com.codename1.ui.TextArea
     * @param initialText The text that appears in the Codename One text are before the call to startEditing
     * @param isEditedFieldSwitch if true, then special case for async edit mode - the native editing is already active, no need to show
     *                            native field, just change the connected field
     */
    private synchronized void startEditing(Activity activity, TextAreaData textArea, String initialText, int codenameOneInputType, final boolean isEditedFieldSwitch) {

        int txty = lastTextAreaY = textArea.getAbsoluteY() + textArea.getScrollY();
        int txtx = lastTextAreaX = textArea.getAbsoluteX() + textArea.getScrollX();
        lastTextAreaWidth = textArea.getWidth();
        lastTextAreaHeight = textArea.getHeight();
        int paddingTop = 0;
        int paddingLeft = textArea.paddingLeft;
        int paddingRight = textArea.paddingRight;
        int paddingBottom = textArea.paddingBottom;

        if (textArea.isTextField) {
            switch (textArea.getVerticalAlignment()) {
                case Component.BOTTOM:
                    paddingTop = textArea.getHeight() - textArea.paddingBottom - textArea.fontHeight;
                    break;
                case Component.CENTER:
                    paddingTop = textArea.getHeight() / 2 - textArea.fontHeight / 2;
                    break;
                default:
                    paddingTop = textArea.paddingTop;
                    break;
            }
        } else {
            paddingTop = textArea.paddingTop;
        }
        int id = activity.getResources().getIdentifier("cn1Style", "attr", activity.getApplicationInfo().packageName);
        if (!isEditedFieldSwitch) {
        mEditText = new EditView(activity, textArea.textArea, this, id);
        } else {
            mEditText.switchToTextArea(textArea.textArea);
        }
    
        if(textArea.getClientProperty("blockCopyPaste") != null || Display.getInstance().getProperty("blockCopyPaste", "false").equals("true")) {
            // The code below is taken from this stackoverflow answer: http://stackoverflow.com/a/22756538/756809
            if (android.os.Build.VERSION.SDK_INT < 11) {
                mEditText.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v,
                            ContextMenuInfo menuInfo) {
                        menu.clear();
                    }
                });
            } else {
                mEditText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    public void onDestroyActionMode(ActionMode mode) {
                    }

                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    public boolean onActionItemClicked(ActionMode mode,
                            MenuItem item) {
                        return false;
                    }
                });
            }            
        }
		else if (isEditedFieldSwitch) {
            //reset copy-paste protection
            if (android.os.Build.VERSION.SDK_INT < 11) {
                mEditText.setOnCreateContextMenuListener(null);
            } else {
                mEditText.setCustomSelectionActionModeCallback(null);
            }
		}
        if (!isEditedFieldSwitch) {
        mEditText.addTextChangedListener(mEditText.mTextWatcher);
        }
        mEditText.setBackgroundDrawable(null);

        mEditText.setFocusableInTouchMode(true);
        mEditLayoutParams = new FrameLayout.LayoutParams(0, 0);
        // Set the appropriate gravity so that the left and top margins will be
        // taken into account
        mEditLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mEditLayoutParams.setMargins(txtx, txty, 0, 0);
        mEditLayoutParams.width = textArea.getWidth();
        mEditLayoutParams.height = textArea.getHeight();

        mEditText.setLayoutParams(mEditLayoutParams);

        if(textArea.isRTL()){
            mEditText.setGravity(Gravity.RIGHT | Gravity.TOP);
        }else{
            mEditText.setGravity(Gravity.LEFT | Gravity.TOP);
        }

        mEditText.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

        Component nextDown = textArea.nextDown;
        boolean imeOptionTaken = true;
        int ime = EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        if (textArea.isSingleLineTextArea()) {
            if(textArea.getClientProperty("searchField") != null) {
                mEditText.setImeOptions(ime | EditorInfo.IME_ACTION_SEARCH);
            } else {
                if(textArea.getClientProperty("sendButton") != null) {
                    mEditText.setImeOptions(ime | EditorInfo.IME_ACTION_SEND);
                } else {
                    if(textArea.getClientProperty("goButton") != null) {
                        mEditText.setImeOptions(ime | EditorInfo.IME_ACTION_GO);
                    } else {
                        if(textArea.isTextField && textArea.getDoneListener() != null){
                            mEditText.setImeOptions(ime | EditorInfo.IME_ACTION_DONE);
                        } else if (nextDown != null && nextDown instanceof TextArea && ((TextArea)nextDown).isEditable() && ((TextArea)nextDown).isEnabled()) {
                            mEditText.setImeOptions(ime | EditorInfo.IME_ACTION_NEXT);
                        } else {
                            mEditText.setImeOptions(ime | EditorInfo.IME_ACTION_DONE);
                            imeOptionTaken = false;
                        }
                    }
                }
            }
        }

        mEditText.setSingleLine(textArea.isSingleLineTextArea());
        mEditText.setAdapter((ArrayAdapter<String>) null);
        mEditText.setText(initialText);
        if(!textArea.isSingleLineTextArea() && textArea.textArea.isGrowByContent() && textArea.textArea.getGrowLimit() > -1){
            mEditText.setMaxLines(textArea.textArea.getGrowLimit());
        }

        if(textArea.nativeHintBool && textArea.getHint() != null) {
            mEditText.setHint(textArea.getHint());
        }
        if (!isEditedFieldSwitch) {
        addView(mEditText, mEditLayoutParams);
        }
        invalidate();
        setVisibility(VISIBLE);
        bringToFront();

        mEditText.requestFocus();

        Object nativeFont = textArea.nativeFont;
        if (nativeFont == null) {
            nativeFont = impl.getDefaultFont();
        }
        Paint p = (Paint) ((AndroidImplementation.NativeFont) nativeFont).font;
        mEditText.setTypeface(p.getTypeface());
        mEditText.setTextScaleX(p.getTextScaleX());
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, p.getTextSize());

        int fgColor = textArea.fgColor;
        mEditText.setTextColor(Color.rgb(fgColor >> 16, (fgColor & 0x00ff00) >> 8, (fgColor & 0x0000ff)));
        boolean password = false;
        if((codenameOneInputType & TextArea.PASSWORD) == TextArea.PASSWORD){
            codenameOneInputType = codenameOneInputType ^ TextArea.PASSWORD;
            password = true;
        }

        if (textArea.isSingleLineTextArea()) {
            mEditText.setInputType(getAndroidInputType(codenameOneInputType));
            //if not ime was explicity requested and this is a single line textfield of type ANY add the emoji keyboard.
            if(!imeOptionTaken && codenameOneInputType == TextArea.ANY){
                mEditText.setInputType(getAndroidInputType(codenameOneInputType) | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
            }
            if(Display.getInstance().getProperty("andAddComma", "false").equals("true") &&
                    (codenameOneInputType & TextArea.DECIMAL) == TextArea.DECIMAL) {
                mEditText.setKeyListener(DigitsKeyListener.getInstance("0123456789.,"));
            }
        }
        if (password) {
            int type = mInputTypeMap.get(codenameOneInputType, InputType.TYPE_CLASS_TEXT);
            if((type & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) == InputType.TYPE_TEXT_FLAG_CAP_SENTENCES){
                type = type ^ InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
            }
            //turn off suggestions for passwords
            mEditText.setInputType(type | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            mEditText.setTransformationMethod(new MyPasswordTransformationMethod());
        }

        int maxLength = textArea.maxSize;
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(maxLength);
        mEditText.setFilters(FilterArray);
        mEditText.setSelection(mEditText.getText().length());
        showVirtualKeyboard(true);
        if (Build.VERSION.SDK_INT < 21) {
            // HACK!!!  On Android 4.4, it seems that the natural blinking cursor
            // causes text to disappear when it blinks.  Manually blinking the
            // cursor seems to work around this issue, so that's what we do here.
            // This issue is described here: http://stackoverflow.com/questions/41305052/textfields-content-disappears-during-typing?noredirect=1#comment69977316_41305052
            mEditText.setCursorVisible(false);
            final boolean[] cursorVisible = new boolean[]{false};
            if (cursorTimer != null) {
                cursorTimer.cancel();
            }
            cursorTimer = new Timer();
            cursorTimerTask = new TimerTask() {
                public void run() {
                    AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            EditView v = mEditText;
                            if (v != null) {
                                cursorVisible[0] = !cursorVisible[0];
                                v.setCursorVisible(cursorVisible[0]);
                            }
                        }
                    });

                }
            };
            cursorTimer.schedule(cursorTimerTask, 100, 500);
        }
    }

    /**
     * Calculate the font height in pixels according to the text area
     * @param style The Codename One text-area to get the font height from
     * @return The font height in pixels, or -1 if a font height could not be determined.
     */
    private int getFontHeight(Style style) {
        if (style == null) {
            return -1;
        }

        Font font = style.getFont();
        if (font == null) {
            return -1;
        }

        return font.getHeight();
    }

    static boolean activeEditorContains(int x, int y) {
        return sInstance != null && sInstance.editorContains(x, y);
    }

    private boolean editorContains(int x, int y) {
        return mIsEditing && mEditText != null && mEditText.mTextArea != null && mEditText.mTextArea.contains(x, y);
    }

    private synchronized void endEditing(int reason, boolean forceVKBOpen) {
        endEditing(reason, forceVKBOpen, false);
    }

    /**
     * Finish the in-place editing of the given text area, release the edit lock, and allow the synchronous call
     * to 'edit' to return.
     */
    private synchronized void endEditing(int reason, boolean forceVKBOpen, boolean forceVKBClose) {
        if (cursorTimer != null) {
            cursorTimer.cancel();
        }
        if (!mIsEditing || mEditText == null) {
            return;
        }
        // SJH: Setting visibility GONE causes a size change event to be fired even when the
        // input mode is adjustPan.  This causes problems and glitches with the layout because we
        // have to guess if a resize even is accurate or not.
        //setVisibility(GONE);
        mLastEndEditReason = reason;

        // If the IME action is set to NEXT, do not hide the virtual keyboard
        boolean isNextActionFlagSet = ((mEditText.getImeOptions() & 0xf) == EditorInfo.IME_ACTION_NEXT);
        boolean leaveKeyboardShowing = impl.isAsyncEditMode() || (reason == REASON_IME_ACTION) && isNextActionFlagSet || forceVKBOpen;
        if (forceVKBClose) {
            leaveKeyboardShowing = false;
        }
        if (!leaveKeyboardShowing) {
            showVirtualKeyboard(false);
        }
        int imo = mEditText.getImeOptions() & 0xf; // Get rid of flags
        if (reason == REASON_IME_ACTION
                && mEditText.mTextArea instanceof TextField
                && ((TextField) mEditText.mTextArea).getDoneListener() != null
                && ((imo & EditorInfo.IME_ACTION_DONE) != 0 || (imo & EditorInfo.IME_ACTION_SEARCH) != 0 || (imo & EditorInfo.IME_ACTION_SEND) != 0 || (imo & EditorInfo.IME_ACTION_GO) != 0)) {
            ((TextField) mEditText.mTextArea).fireDoneEvent();
            showVirtualKeyboard(false);
        }

        // Call this in onComplete instead
        //mIsEditing = false;
        mLastEditText = mEditText;
        removeView(mEditText);
        Component editingComponent = mEditText.mTextArea;
        mEditText.removeTextChangedListener(mEditText.mTextWatcher);
        mEditText = null;

        if (impl.isAsyncEditMode()) {
            Runnable onComplete = (Runnable)editingComponent.getClientProperty("android.onAsyncEditingComplete");
            editingComponent.putClientProperty("android.onAsyncEditingComplete", null);
            if (onComplete != null) {
                Display.getInstance().callSerially(onComplete);
            }
        }

        waitingForSynchronousEditingCompletion = false;
    }

    /**
     * This method waits until the user leaves the EditText
     * It must not access sInstance since it might not have been created yet.
     */
    private static void waitForEditCompletion() {
        Display.getInstance().invokeAndBlock(new Runnable() {

            public void run() {
                while (waitingForSynchronousEditingCompletion){
                    try {
                        Thread.sleep(50);
                    } catch (Throwable e) {
                    }
                };
            }
        });
        Log.d(TAG, "waitForEditCompletion - Waiting for lock");
    }

    /**
     * This method will be called by our EditText control when the action
     * key (Enter/Go/Send) on the soft keyboard will be pressed.
     * @param actionCode
     * @return task to run after call to super.onEditorAction. Returns null if action was consumed (tapped Next in async mode) and super.onEditorAction do not have to be called.
     */
    Runnable onEditorAction(int actionCode) {
        actionCode = actionCode & 0xf;
        boolean hasNext = false;
        if (EditorInfo.IME_ACTION_NEXT == actionCode && mEditText != null &&
                mEditText.mTextArea != null) {
            Component next = mEditText.mTextArea.getNextFocusDown();
            if (next == null) {
                next = mEditText.mTextArea.getComponentForm().findNextFocusVertical(true);
            }

            if (next != null && next instanceof TextArea && ((TextArea)next).isEditable() && ((TextArea)next).isEnabled()) {
                hasNext = true;
                nextTextArea = (TextArea) next;
            }
        }
		if (hasNext && nextTextArea != null && impl.isAsyncEditMode()) {
			//in async edit mode go right to next field edit to avoid hiding and showing again the native edit text
            TextArea theNext = nextTextArea;
			nextTextArea = null;
            edit(sInstance.impl, theNext, theNext.getConstraint());
			return null;
        } else {
			return new Runnable() {

				@Override
				public void run() {
        endEditing(REASON_IME_ACTION, false);
    }
			};
		}
    }

    private static int trySetEditModeCount=0;

    private static void trySetEditMode(final boolean resize) {
        trySetEditMode(resize, 10);
    }

    private static com.codename1.ui.geom.Rectangle getVisibleRect(Component c) {
        com.codename1.ui.geom.Rectangle r = new com.codename1.ui.geom.Rectangle(c.getAbsoluteX() + c.getScrollX(), c.getAbsoluteY() + c.getScrollY(), c.getWidth(), c.getHeight());
        while ((c = c.getParent()) != null) {
            com.codename1.ui.geom.Rectangle.intersection(r.getX(), r.getY(), r.getWidth(), r.getHeight(),
                    c.getAbsoluteX() + c.getScrollX(), c.getAbsoluteY() + c.getScrollY(), c.getWidth(), c.getHeight(),
                    r);

        }
        return r;

    }

    /**
     * Wrap the setEditMode method so that it is "safe" to set to resize edit mode.
     * This will try to set to the "resize" edit mode, and will spawn a thread
     * to check 200 ms later to make sure that the text area is not covered by the
     * keyboard.  If it is covered by the keyboard, it will switch to pan edit mode.
     * This is preferable since resize edit mode works better in general so we'd like
     * to use resize whenever possible.
     *
     * This change is to minimize the number of occurrences of this bug:
     * https://github.com/codenameone/CodenameOne/issues/1827
     *
     * Some attempts were made to fix this bug directly by wrapping the textarea
     * inside a ScrollView, but it caused some issues when the user tries to make text selections
     * on the textview content - which causes Android to implicitly *change* to resize edit mode
     * without actually letting us know.  This gist shows that attempt:
     * https://gist.github.com/shannah/471033878e53c1fc297680fa85f6fd20
     *
     *
     * @param resize
     */
    private static void trySetEditMode(final boolean resize, final int retriesRemaining) {
        if (trySetEditModeCount > 100) {
            trySetEditModeCount = 0;
        }
        final int thisCount = trySetEditModeCount++;
        if (resize != resizeMode) {
            setEditMode(resize);
        }
        if (resize) {
            // We would like to set pan mode, but we must do this with some protections
            // since pan mode might cover the text area

            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(100);

                    } catch (Exception ex){}
                    if (thisCount != trySetEditModeCount-1) {
                        return;
                    }
                    AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            if (thisCount != trySetEditModeCount-1) {
                                return;
                            }
                            if (sInstance == null || sInstance.mEditText == null || sInstance.mEditText.mTextArea == null) {
                                return;
                            }
                            com.codename1.ui.Font font = sInstance.mEditText.mTextArea.getStyle().getFont();
                            float fontSize = (font != null || font.getPixelSize() == 0) ? font.getPixelSize() : Display.getInstance().convertToPixels(4);
                            com.codename1.ui.geom.Rectangle visibleRect = getVisibleRect(sInstance.mEditText.mTextArea);
                            Rect r = new Rect();
                            AndroidImplementation.getInstance().relativeLayout.getGlobalVisibleRect(r);
                            int rootViewHeight = r.height();
                            int txtY = sInstance.mEditText.mTextArea.getAbsoluteY() + sInstance.mEditText.mTextArea.getScrollY();
                            if (txtY > rootViewHeight - 20 || visibleRect.getHeight() < fontSize) {
                                // We're off the page
                                //System.out.println("SETTING TO PAN MODE_______");
                                setEditMode(false);

                            } else {
                                if (retriesRemaining > 0) {
                                    trySetEditMode(resize, retriesRemaining-1);
                                }
                            }
                        }

                    });

                }
            }).start();
        }
    }



    private static void setEditMode(final boolean resize){
        resizeMode = resize;
        if (resize) {
            sInstance.impl.getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else {
            sInstance.impl.getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
    }

    public static boolean isInputResize(){
        return resizeMode;
    }

    /**
     * Returns true if an edit is currently in progress, false otherwise
     */
    public static boolean isEditing() {
        return (sInstance == null) ? false : sInstance.mIsEditing;
    }

    public static int lastEditEndReason() {
        return (sInstance == null) ? REASON_UNDEFINED : sInstance.mLastEndEditReason;
    }

    public static void endEdit() {
        endEdit(false);
    }
    // Called on Android UI thread.
    public static void endEdit(boolean forceVKBClose) {
        if (sInstance != null) {
            sInstance.endEditing(REASON_UNDEFINED, false, forceVKBClose);
            // No longer need these because end editing will allow the onComplete handler to
            // be called which will trigger a releaseEdit
            //ViewParent p = sInstance.getParent();
            //if (p != null) {
            //    ((ViewGroup) p).removeView(sInstance);
            //}
            //sInstance = null;
        }
    }

    public static void stopEdit() {
        stopEdit(false);
    }

    public static void stopEdit(boolean forceVKBClose) {
        if (sInstance != null) {
            sInstance.endEditing(REASON_UNDEFINED, false, forceVKBClose);
        }
    }

    private static void releaseEdit() {
        if (sInstance != null) {
            ViewParent p = sInstance.getParent();
            if (p != null) {
                ((ViewGroup) p).removeView(sInstance);
            }
            sInstance = null;
        }
    }

    private static int lastTextAreaX, lastTextAreaY, lastTextAreaWidth, lastTextAreaHeight;

    /*
    public static void reLayoutEdit() {
        if (sInstance != null && sInstance.mEditText != null) {

            TextArea txt = sInstance.mEditText.mTextArea;
            if (txt != null) {
                int txty = txt.getAbsoluteY() + txt.getScrollY();
                int txtx = txt.getAbsoluteX() + txt.getScrollX();
                int w = txt.getWidth();
                int h = txt.getHeight();

                sInstance.mEditLayoutParams.setMargins(txtx, txty, 0, 0);
                sInstance.mEditLayoutParams.width = w;
                sInstance.mEditLayoutParams.height = h;
                sInstance.mEditText.requestLayout();
                sInstance.invalidate();
                sInstance.setVisibility(VISIBLE);
                sInstance.bringToFront();
            }
        }

    }*/

    public static void reLayoutEdit() {
        reLayoutEdit(false);
    }

    public static void reLayoutEdit(boolean force) {

        if (mIsEditing && !isActiveTextEditorHidden() && sInstance != null && sInstance.mEditText != null) {
            final TextArea txt = sInstance.mEditText.mTextArea;
            if (!force && lastTextAreaX == txt.getAbsoluteX() + txt.getScrollX() &&
                    lastTextAreaY == txt.getAbsoluteY() + txt.getScrollY() &&
                    lastTextAreaWidth == txt.getWidth() &&
                    lastTextAreaHeight == txt.getHeight()) {
                return;
            }
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    if (mIsEditing && !isActiveTextEditorHidden() && sInstance != null && sInstance.mEditText != null) {

                        if (txt != null) {

                            if (sInstance.mEditText.mTextArea != txt) {
                                //has changed in between, skip or would change location back to old field
                                return;
                            }

                            final int txty = lastTextAreaY = txt.getAbsoluteY() + txt.getScrollY();
                            final int txtx = lastTextAreaX = txt.getAbsoluteX() + txt.getScrollX();
                            final int w = lastTextAreaWidth = txt.getWidth();
                            final int h = lastTextAreaHeight = txt.getHeight();


                            sInstance.impl.getActivity().runOnUiThread(new Runnable() {
                                public void run() {
                                    if (mIsEditing && !isActiveTextEditorHidden() && sInstance != null && sInstance.mEditText != null) {

                                        if (sInstance.mEditText.mTextArea != txt) {
                                            //has changed in between, skip or would change location back to old field
                                            return;
                                        }

                                        sInstance.mEditLayoutParams.setMargins(txtx, txty, 0, 0);
                                        sInstance.mEditLayoutParams.width = w;
                                        sInstance.mEditLayoutParams.height = h;
                                        sInstance.mEditText.requestLayout();
                                        sInstance.invalidate();
                                        sInstance.setVisibility(VISIBLE);
                                        sInstance.bringToFront();
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    /**
     * Entry point for using this class
     * @param impl The current running activity
     * @param component Any subclass of com.codename1.ui.TextArea
     * @param inputType One of the TextArea's input-type constants
     */
    public static void edit(final AndroidImplementation impl, final Component component, final int inputType) {
        if (impl.getActivity() == null) {
            throw new IllegalArgumentException("activity is null");
        }

        if (component == null) {
            throw new IllegalArgumentException("component is null");
        }

        if (!(component instanceof TextArea)) {
            throw new IllegalArgumentException("component must be instance of TextArea");
        }



        final TextArea textArea = (TextArea) component;
        final String initialText = textArea.getText();
        Dimension prefSize = textArea.getPreferredSize();



        // The very first time we try to edit a string, let's determine if the
        // system default is to do async editing.  If the system default
        // is not yet set, we set it here, and it will be used as the default from now on
        //  We do this because the nativeInstance.isAsyncEditMode() value changes
        // to reflect the currently edited field so it isn't a good way to keep a
        // system default.
        String defaultAsyncEditingSetting = Display.getInstance().getProperty("android.VKBAlwaysOpen", null);
        if (defaultAsyncEditingSetting == null) {
            defaultAsyncEditingSetting = impl.isAsyncEditMode() ? "true" : "false";
            Display.getInstance().setProperty("android.VKBAlwaysOpen", defaultAsyncEditingSetting);

        }
        boolean asyncEdit = "true".equals(defaultAsyncEditingSetting) ? true : false;

        // Check if the form has any setting for asyncEditing that should override
        // the application defaults.
        final Form parentForm = component.getComponentForm();
        if (parentForm == null) {
            com.codename1.io.Log.p("Attempt to edit text area that is not on a form.  This is not supported");
            return;
        }
        if (parentForm.getClientProperty("asyncEditing") != null) {
            Object async = parentForm.getClientProperty("asyncEditing");
            if (async instanceof Boolean) {
                asyncEdit = ((Boolean)async).booleanValue();
                //Log.p("Form overriding asyncEdit due to asyncEditing client property: "+asyncEdit);
            }
        }

        if (parentForm.getClientProperty("android.asyncEditing") != null) {
            Object async = parentForm.getClientProperty("android.asyncEditing");
            if (async instanceof Boolean) {
                asyncEdit = ((Boolean)async).booleanValue();
                //Log.p("Form overriding asyncEdit due to ios.asyncEditing client property: "+asyncEdit);
            }

        }

        if (parentForm.isFormBottomPaddingEditingMode()) {
            asyncEdit = true;
        }


        // If the field itself explicitly sets async editing behaviour
        // then this will override all other settings.
        if (component.getClientProperty("asyncEditing") != null) {
            Object async = component.getClientProperty("asyncEditing");
            if (async instanceof Boolean) {
                asyncEdit = ((Boolean)async).booleanValue();
                //Log.p("Overriding asyncEdit due to field asyncEditing client property: "+asyncEdit);
            }
        }

        if (component.getClientProperty("android.asyncEditing") != null) {
            Object async = component.getClientProperty("android.asyncEditing");
            if (async instanceof Boolean) {
                asyncEdit = ((Boolean)async).booleanValue();
                //Log.p("Overriding asyncEdit due to field ios.asyncEditing client property: "+asyncEdit);
            }

        }

		//if true, then in async mode we are currently editing and are switching to another field
		final boolean isEditedFieldSwitch;

        // If we are already editing, we need to finish that up before we proceed to edit the next field.
        synchronized(editingLock) {
            if (mIsEditing) {

                if (impl.isAsyncEditMode()) {
                    isEditedFieldSwitch = true;
                    InPlaceEditView.setEditedTextField(textArea);
                    nextTextArea = null;
                } else {
                    isEditedFieldSwitch = false;

                final InPlaceEditView instance = sInstance;
                if (instance != null && instance.mEditText != null && instance.mEditText.mTextArea == textArea) {
                    instance.showTextEditorAgain();
                    return;
                }
                if (!isClosing && sInstance != null && sInstance.mEditText != null) {
                    isClosing = true;

                    impl.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            instance.endEditing(REASON_UNDEFINED, true);
                        }
                    });
                }

                afterClose = new Runnable() {

                    @Override
                    public void run() {
                        impl.callHideTextEditor();
                        Display.getInstance().editString(component, textArea.getMaxSize(), inputType, textArea.getText());
                    }

                };
                return;
                }
            } else {
                isEditedFieldSwitch = false;
            }
            mIsEditing = true;
            isClosing = false;
            afterClose = null;
        }

        impl.setAsyncEditMode(asyncEdit);

        //textArea.setPreferredSize(prefSize);
        if (!impl.isAsyncEditMode() && textArea instanceof TextField) {
            ((TextField) textArea).setEditable(false);
        }

        final boolean scrollableParent = isScrollableParent(textArea);
        // We wrap the text area so that we can safely pass data across to the
        // android UI thread.
        final TextAreaData textAreaData = new TextAreaData(textArea);

        impl.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
				if (!isEditedFieldSwitch) {
                releaseEdit();

                if (sInstance == null) {
                    sInstance = new InPlaceEditView(impl);
                    impl.relativeLayout.addView(sInstance);
                }

                // Let's try something new here
                // We'll ALWAYS try resize edit mode (since it just works better)
                // But we'll detect whether the field is still covered by the keyboard
                // and switch to pan mode if necessary.


                if(scrollableParent || parentForm.isFormBottomPaddingEditingMode()){
                    setEditMode(true);
                }else{
                    trySetEditMode(true);
                }
				}
                sInstance.startEditing(impl.getActivity(), textAreaData, initialText, inputType, isEditedFieldSwitch);
            }
        });

        final String[] out = new String[1];


        // In order to reuse the code the runs after edit completion, we will wrap it in a runnable
        // For sync edit mode, we will just run onComplete.run() at the end of this method.  For
        // Async mode we add the Runnable to the textarea as a client property, then run it
        // when editing eventually completes.
        Runnable onComplete = new Runnable() {
            public void run() {
                if (!impl.isAsyncEditMode() && textArea instanceof TextField) {
                    ((TextField) textArea).setEditable(true);
                }
                textArea.setPreferredSize(null);

                if(sInstance != null && sInstance.mLastEditText != null && sInstance.mLastEditText.mTextArea == textArea){
                    String retVal = sInstance.mLastEditText.getText().toString();

                    if (!impl.isAsyncEditMode()) {
                        sInstance.mLastEditText = null;
                        impl.getActivity().runOnUiThread(new Runnable() {

                            public void run() {
                                releaseEdit();
                            }
                        });
                    }
                    out[0] = retVal;
                }else{
                    out[0] = initialText;
                }

                Display.getInstance().onEditingComplete(component, out[0]);
                if (impl.isAsyncEditMode()) {
                    impl.callHideTextEditor();
                } else {


                    // the call to releaseEdit above should remove the native text editor and
                    // set sInstance to null
                    // We would like to wait for that to happen before we release our isEditing
                    // lock.
                    if (sInstance != null) {
                        Display.getInstance().invokeAndBlock(new Runnable() {
                            public void run() {
                                while (sInstance != null) {
                                    com.codename1.io.Util.sleep(5);
                                }
                            }

                        });
                    }
                }


                // Release the editing flag
                synchronized (editingLock) {
                    mIsEditing = false;
                }

                // If anyone attempted to call edit() while we were still editing,
                // the last such attempt will have been added to the afterClose handler
                // as a runnable ... this should take priority over the "nextTextArea" setting
                if (afterClose != null) {
                    Display.getInstance().callSerially(afterClose);
                } else if (nextTextArea != null) {
                    final TextArea next = nextTextArea;
                    nextTextArea = null;
                    next.requestFocus();
                    Display.getInstance().callSerially(new Runnable() {

                        public void run() {
                            Display.getInstance().editString(next,
                                    next.getMaxSize(),
                                    next.getConstraint(),
                                    next.getText());
                        }
                    });
                }

            }
        };
        textArea.requestFocus();
        textArea.repaint();
        if (impl.isAsyncEditMode()) {
            component.putClientProperty("android.onAsyncEditingComplete", onComplete);
            return;
        }

        // Make this call synchronous
        // We set this flag so that waitForEditCompletion can block on it.
        // The flag will be released inside the endEditing method which will
        // allow the method to proceed.
        waitingForSynchronousEditingCompletion = true;
        waitForEditCompletion();

        onComplete.run();
    }

	public static void setEditedTextField(final TextArea textarea) {
        Display display = Display.getInstance();
        Runnable task = new Runnable(){
            public void run() {
                AndroidImplementation.getInstance().setFocusedEditingText(textarea);
            }
        };
        if (display.isEdt()) {
            task.run();
        } else {
            display.callSeriallyAndWait(task);
        }
    }

    private static boolean isScrollableParent(Component c){
        Container p = c.getParent();
        Font f = c.getStyle().getFont();
        float pixelSize = f == null ? Display.getInstance().convertToPixels(4) : f.getPixelSize();
        while( p != null){

            if(p.isScrollableY() && p.getAbsoluteY() + p.getScrollY() < Display.getInstance().getDisplayHeight() / 2 - pixelSize * 2){
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    private class DebugResultReceiver extends ResultReceiver {

        private static final String TAG = "InPlaceEditView.ResultReceiver";
        private SparseArray<String> mResultToStringMap = new SparseArray<String>();

        public DebugResultReceiver(Handler handler) {
            super(handler);
            mResultToStringMap.append(InputMethodManager.RESULT_HIDDEN, "RESULT_HIDDEN");
            mResultToStringMap.append(InputMethodManager.RESULT_SHOWN, "RESULT_SHOWN");
            mResultToStringMap.append(InputMethodManager.RESULT_UNCHANGED_HIDDEN, "RESULT_UNCHANGED_HIDDEN");
            mResultToStringMap.append(InputMethodManager.RESULT_UNCHANGED_SHOWN, "RESULT_UNCHANGED_SHOWN");
        }

        protected void onReceiveResult(int resultCode, Bundle resultData) {
            String resultStr = mResultToStringMap.get(resultCode, "Unknown");

            Log.i(TAG, "resultCode = " + resultStr);
        }
    }

    private class TextChange {
        String textToAppend;
        int atPos;
        int deleteLength;


    }

    private class EditView extends AutoCompleteTextView {

        private InPlaceEditView mInPlaceEditView;
        private TextArea mTextArea = null;
        private ResetableTextWatcher mTextWatcher = new ResetableTextWatcher() {

            private boolean started = false;
            private TextChange currChange;
            private int lastInsertStartPos;
            private int lastInsertBeforeCount;
            private int lastInsertAfterCount;

            /**
             * Reset status after connected textarea change.
             */
            @Override
            public void reset() {
                started = false;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                // We use this hook to catch keyboard strokes in async edit mode while the
                // edit text field is hidden.
                currChange = new TextChange();
                currChange.atPos = start;
                lastInsertAfterCount = after;
                lastInsertBeforeCount = count;
                lastInsertStartPos = start;
                if (mIsEditing && impl.isAsyncEditMode() && isTextEditorHidden()) {

                    // If the text editor is hidden, and the user starts typing in the
                    // keyboard (because we're in async edit mode), then we need to
                    // trigger the native editor to display again.
                    showTextEditorAgain();
                }
            }

            @Override
            public void afterTextChanged(final Editable s) {
                if (isEditing() && mTextArea != null) {
                    try {
                        final String actualString = s.toString();
                        //make sure to start send events to the cn1 textfield only
                        //when the first string equals to the initial text
                        if (!started) {
                            if (mTextArea.getText().equals(actualString)) {
                                started = true;
                            }
                            return;
                        }

                        synchronized (InPlaceEditView.this) {

                            // In Async Edit mode, we may have just triggered a "showTextEditorAgain" in the
                            // beforeTextChanged event.  However this will trigger some async stuff on the
                            // EDT and the UI thread to initialize the native editor with the contents
                            // of the CN1 TextArea.  That created an inputBuffer to catch key strokes in
                            // the mean time so that they will be added correctly to the
                            // native editor when it is ready.
                            if (inputBuffer != null) {
                                if (lastInsertBeforeCount > lastInsertAfterCount) {
                                    currChange.deleteLength = lastInsertBeforeCount - lastInsertAfterCount;
                                    inputBuffer.add(currChange);
                                    currChange = null;
                                } else if (lastInsertBeforeCount < lastInsertAfterCount) {
                                    currChange.textToAppend = actualString.substring(lastInsertStartPos, actualString.length() - lastInsertAfterCount + 1);
                                    inputBuffer.add(currChange);
                                    currChange = null;
                                }
                            }
                        }

                        Display.getInstance().callSerially(new Runnable() {

                            @Override
                            public void run() {
                                if (!actualString.equals(mTextArea.getText())) {
                                    mTextArea.setText(actualString);
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, e.toString() + " " + Log.getStackTraceString(e));
                    }
                }
            }
        };

        /**
         * Constructor
         * @param context
         * @param inPlaceEditView
         */
        public EditView(Context context, TextArea textArea, InPlaceEditView inPlaceEditView, int style) {
            super(context, null, style);
            mInPlaceEditView = inPlaceEditView;
            mTextArea = textArea;
            setBackgroundColor(Color.TRANSPARENT);
        }

		/**
		 * Connects to other textArea.
		 */
		public void switchToTextArea(TextArea other) {
            if (this.mTextArea != null && this.mTextArea != other) {
                Display.getInstance().onEditingComplete(this.mTextArea, this.mTextArea.getText());
            }
            this.mTextArea = other;
            mTextWatcher.reset();
        }

        @Override
        public void onEditorAction(int actionCode) {
			Runnable task = mInPlaceEditView.onEditorAction(actionCode);
            if (task != null) {
            super.onEditorAction(actionCode);
                task.run();
            }
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                endEditing(InPlaceEditView.REASON_SYSTEM_KEY, false, true);
                return true;
            }
            return super.onKeyPreIme(keyCode, event);
        }


        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            // If the user presses the back button, or the menu button
            // we must terminate editing, to allow EDT to handle events
            // again
            if (keyCode == KeyEvent.KEYCODE_BACK
                    || keyCode == KeyEvent.KEYCODE_MENU) {
                endEditing(InPlaceEditView.REASON_SYSTEM_KEY, false, true);
            }

            return super.onKeyDown(keyCode, event);
        }



    }

    public class MyPasswordTransformationMethod extends PasswordTransformationMethod {

        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return new PasswordCharSequence(source);
        }

        private class PasswordCharSequence implements CharSequence {

            private CharSequence mSource;

            public PasswordCharSequence(CharSequence source) {
                mSource = source; // Store char sequence
            }

            public char charAt(int index) {
                return '\u25CF'; // This is the important part
            }

            public int length() {
                return mSource.length(); // Return default
            }

            public CharSequence subSequence(int start, int end) {
                return mSource.subSequence(start, end); // Return default
            }
        }
    };
}
interface ResetableTextWatcher extends TextWatcher {

    public void reset();

}
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

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

public class CodenameOneView {

    private int width = 1;
    private int height = 1;
    private Bitmap bitmap;
    private AndroidGraphics buffy = null;
    private Canvas canvas;
    private AndroidImplementation implementation = null;
    private final KeyCharacterMap keyCharacterMap;
    private final Rect bounds = new Rect();
    private boolean fireKeyDown = false;
    private boolean drawing;

    public CodenameOneView(Activity activity, View androidView, AndroidImplementation implementation, boolean drawing) {
        this.implementation = implementation;
        this.drawing = drawing;
        initializeView(androidView);
        this.keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.BUILT_IN_KEYBOARD);
        initializeDisplay(activity);
    }

    private void initializeView(View androidView) {
        androidView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        androidView.setFocusable(true);
        androidView.setFocusableInTouchMode(true);
        androidView.setEnabled(true);
        androidView.setClickable(true);
        androidView.setLongClickable(false);

        if(!drawing) {
            androidView.setWillNotCacheDrawing(false);
            androidView.setWillNotDraw(true);
            this.buffy = new AndroidGraphics(implementation, null, false);
        }

        androidView.setScrollContainer(true);
    }

    private void initializeDisplay(Activity activity) {
        android.view.Display androidDisplay = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        width = androidDisplay.getWidth();
        height = androidDisplay.getHeight();
        initBitmaps(width, height);
    }

    public boolean isOpaque() {
        return true;
    }

    public void onSurfaceChanged(final int w, final int h) {
        if(!Display.isInitialized()) {
            return;
        }
        Display.getInstance().callSerially(() -> handleSizeChange(w, h));
    }

    public void onSurfaceCreated() {
        this.visibilityChangedTo(true);
    }

    public void onSurfaceDestroyed() {
        this.visibilityChangedTo(false);
    }

    private void initBitmaps(int w, int h) {
        if(!drawing) {
            this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            this.canvas = new Canvas(this.bitmap);
            this.buffy.setCanvas(this.canvas);
        }
    }

    public void visibilityChangedTo(boolean visible) {
        boolean changed = visible;
        if (this.implementation.getCurrentForm() != null && changed) {
            if (visible) {
                this.implementation.showNotifyPublic();
                this.implementation.getCurrentForm().repaint();
            } else {
                this.implementation.hideNotifyPublic();
            }
        }
    }

    public void handleSizeChange(int w, int h) {
        if(!drawing) {
            if ((this.width != w && (this.width < w || this.height < h))
                    || (bitmap.getHeight() < h)) {
                this.initBitmaps(w, h);
            }
        }
        if (this.width == w && this.height == h) {
            return;
        }
        this.width = w;
        this.height = h;
        Log.d("Codename One", "sizechanged: " + width + " " + height + " " + this);
        if (this.implementation.getCurrentForm() == null) {
            return;
        }

        if (InPlaceEditView.isEditing()) {
            final Form f = this.implementation.getCurrentForm();
            ActionListener sizeChanged = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    CodenameOneView.this.implementation.getActivity().runOnUiThread(() -> {
                        InPlaceEditView.reLayoutEdit();
                        InPlaceEditView.scrollActiveTextfieldToVisible();
                    });
                    f.removeSizeChangedListener(this);
                }
            };
            f.addSizeChangedListener(sizeChanged);
        }
        Display.getInstance().sizeChanged(w, h);
    }

    protected void d(Canvas canvas) {
        if(!drawing) {
            boolean empty = canvas.getClipBounds(bounds);
            if (empty) {
                canvas.drawBitmap(bitmap, 0, 0, null);
            } else {
                bounds.intersect(0, 0, width, height);
                canvas.drawBitmap(bitmap, bounds, bounds, null);
            }
        }
    }

    final static int internalKeyCodeTranslate(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return AndroidImplementation.DROID_IMPL_KEY_DOWN;
            case KeyEvent.KEYCODE_DPAD_UP:
                return AndroidImplementation.DROID_IMPL_KEY_UP;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return AndroidImplementation.DROID_IMPL_KEY_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return AndroidImplementation.DROID_IMPL_KEY_RIGHT;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                return AndroidImplementation.DROID_IMPL_KEY_FIRE;
            case KeyEvent.KEYCODE_MENU:
                return AndroidImplementation.DROID_IMPL_KEY_MENU;
            case KeyEvent.KEYCODE_CLEAR:
                return AndroidImplementation.DROID_IMPL_KEY_CLEAR;
            case KeyEvent.KEYCODE_DEL:
                return AndroidImplementation.DROID_IMPL_KEY_BACKSPACE;
            case KeyEvent.KEYCODE_BACK:
                return AndroidImplementation.DROID_IMPL_KEY_BACK;
            default:
                return keyCode;
        }
    }

    public boolean onKeyUpDown(boolean down, int keyCode, KeyEvent event) {
        keyCode = this.internalKeyCodeTranslate(keyCode);

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
            case KeyEvent.KEYCODE_SYM:
            return false;
            case KeyEvent.KEYCODE_ENTER:
              if (down) {
                    Display.getInstance().keyPressed(keyCode);
                } else {
                    Display.getInstance().keyReleased(keyCode);
                }
              return false;
            default:
        }

        if (event.getRepeatCount() > 0) {
            return true;
        }
        if (this.implementation.getCurrentForm() == null) {
            return true;
        }

        if (keyCode == AndroidImplementation.DROID_IMPL_KEY_FIRE) {
            this.fireKeyDown = down;
        } else if (keyCode == AndroidImplementation.DROID_IMPL_KEY_DOWN
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_UP
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_LEFT
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_RIGHT) {
            if (this.fireKeyDown) {
                return true;
            }
        }

        switch (keyCode) {

            case AndroidImplementation.DROID_IMPL_KEY_MENU:
                if (Display.getInstance().getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE) {
                    return false;
                }
            case AndroidImplementation.DROID_IMPL_KEY_BACK:
            case AndroidImplementation.DROID_IMPL_KEY_DOWN:
            case AndroidImplementation.DROID_IMPL_KEY_UP:
            case AndroidImplementation.DROID_IMPL_KEY_LEFT:
            case AndroidImplementation.DROID_IMPL_KEY_RIGHT:
            case AndroidImplementation.DROID_IMPL_KEY_FIRE:
            case AndroidImplementation.DROID_IMPL_KEY_CLEAR:
            case AndroidImplementation.DROID_IMPL_KEY_BACKSPACE:
                if (down) {
                    Display.getInstance().keyPressed(keyCode);
                } else {
                    Display.getInstance().keyReleased(keyCode);
                }
                return true;

            default:
                int meta = 0;
                if (event.isShiftPressed()) {
                    meta |= KeyEvent.META_SHIFT_ON;
                }
                if (event.isAltPressed()) {
                    meta |= KeyEvent.META_ALT_ON;
                }
                if (event.isSymPressed()) {
                    meta |= KeyEvent.META_SYM_ON;
                }
                final int nextchar = this.keyCharacterMap.get(keyCode, meta);
                if (down) {
                    Display.getInstance().keyPressed(nextchar);
                } else {
                    Display.getInstance().keyReleased(nextchar);
                }
                return true;

        }
    }

    private boolean cn1GrabbedPointer = false;

    public boolean onTouchEvent(MotionEvent event) {

        if (this.implementation.getCurrentForm() == null) {
            return true;
        }

        int[] x = null;
        int[] y = null;
        int size = event.getPointerCount();
        if (size > 1) {
            x = new int[size];
            y = new int[size];
            for (int i = 0; i < size; i++) {
                x[i] = (int) event.getX(i);
                y[i] = (int) event.getY(i);
            }
        }

        Component componentAt;
        try {
            if (x == null) {
                componentAt = this.implementation.getCurrentForm().getComponentAt((int)event.getX(), (int)event.getY());
            } else {
                componentAt = this.implementation.getCurrentForm().getComponentAt((int)x[0], (int)y[0]);
            }
        } catch (Throwable t) {
            componentAt = null;
        }
        boolean isPeer = (componentAt instanceof PeerComponent);
        boolean consumeEvent = !isPeer || cn1GrabbedPointer;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (x == null) {
                    this.implementation.pointerPressed((int) event.getX(), (int) event.getY());
                } else {
                    this.implementation.pointerPressed(x, y);
                }
                if (!isPeer) cn1GrabbedPointer = true;
                break;
            case MotionEvent.ACTION_UP:
                if (x == null) {
                    this.implementation.pointerReleased((int) event.getX(), (int) event.getY());
                } else {
                    this.implementation.pointerReleased(x, y);
                }
                cn1GrabbedPointer = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                cn1GrabbedPointer = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (x == null) {
                    this.implementation.pointerDragged((int) event.getX(), (int) event.getY());
                } else {
                    this.implementation.pointerDragged(x, y);
                }
                break;
        }

        return consumeEvent;
    }

    public AndroidGraphics getGraphics() {
        return buffy;
    }

    public int getViewHeight() {
        return height;
    }

    public int getViewWidth() {
        return width;
    }

    public void setInputType(EditorInfo editorInfo) {
        Component txtCmp = Display.getInstance().getCurrent().getFocused();
        if (txtCmp != null && txtCmp instanceof TextArea) {
            TextArea txt = (TextArea) txtCmp;
            if (txt.isSingleLineTextArea()) {
                editorInfo.imeOptions |= EditorInfo.IME_ACTION_DONE;

            } else {
                editorInfo.imeOptions |= EditorInfo.IME_ACTION_NONE;
            }
            int inputType = 0;
            int constraint = txt.getConstraint();
            if ((constraint & TextArea.PASSWORD) == TextArea.PASSWORD) {
                constraint = constraint ^ TextArea.PASSWORD;
            }
            switch (constraint) {
                case TextArea.NUMERIC:
                    inputType = EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_SIGNED;
                    break;
                case TextArea.DECIMAL:
                    inputType = EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_DECIMAL;
                    break;
                case TextArea.PHONENUMBER:
                    inputType = EditorInfo.TYPE_CLASS_PHONE;
                    break;
                case TextArea.EMAILADDR:
                    inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
                    break;
                case TextArea.URL:
                    inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_URI;
                    break;
                default:
                    inputType = EditorInfo.TYPE_CLASS_TEXT;
                    break;

            }

            editorInfo.inputType = inputType;
        }
    }
}
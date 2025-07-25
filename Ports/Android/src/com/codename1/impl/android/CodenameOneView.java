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
import android.os.Build;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import java.lang.reflect.Method;


/**
 *
 * @author Chen
 */
public class CodenameOneView {

    int width = 1;
    int height = 1;
    Bitmap bitmap;
    AndroidGraphics buffy = null;
    private Canvas canvas;
    private AndroidImplementation implementation = null;
    private final KeyCharacterMap keyCharacterMap;
    private final Rect bounds = new Rect();
    private boolean fireKeyDown = false;
    //private volatile boolean created = false;
    private boolean drawing;

    private final Rect safeArea = new Rect();

    private static final int VERSION_CODE_P = 28;
    private static final int VERSION_CODE_M = 23;

    public CodenameOneView(Activity activity, View androidView, AndroidImplementation implementation, boolean drawing) {

        this.implementation = implementation;
        this.drawing = drawing;
        androidView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        androidView.setFocusable(true);
        androidView.setFocusableInTouchMode(true);
        androidView.setEnabled(true);
        androidView.setClickable(true);
        androidView.setLongClickable(false);
        
        /**
         * tell the system that we do our own caching and it does not need to
         * use an extra offscreen bitmap.
         */
        if(!drawing) {
            androidView.setWillNotCacheDrawing(false);
            androidView.setWillNotDraw(true);
            this.buffy = new AndroidGraphics(implementation, null, false);
        }

        this.keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.BUILT_IN_KEYBOARD);


        /**
         * From the docs: "Change whether this view is one of the set of
         * scrollable containers in its window. This will be used to determine
         * whether the window can resize or must pan when a soft input area is
         * open -- scrollable containers allow the window to use resize mode
         * since the container will appropriately shrink. "
         */
        androidView.setScrollContainer(true);

        android.view.Display androidDisplay = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        width = androidDisplay.getWidth();
        height = androidDisplay.getHeight();
        View rootView = activity.getWindow().getDecorView();
        rootView.post(new Runnable() {
            public void run() {
                updateSafeArea();
            }
        });
        initBitmaps(width, height);
    }

    public boolean isOpaque() {
        return true;
    }

    public void onSurfaceChanged(final int w, final int h) {
        if(!Display.isInitialized()) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {

            public void run() {
                handleSizeChange(w, h);
            }
        });
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
                /**
                 * request a full repaint as our surfaceview is most likely
                 * black if this app comes back from the background.
                 */
                this.implementation.getCurrentForm().repaint();
                //android.os.Debug.startMethodTracing("calc");
            } else {
                this.implementation.hideNotifyPublic();
                //android.os.Debug.stopMethodTracing();
            }
        }
        //flushGraphics();
    }

    private void updateSafeArea() {
        final Activity activity = CodenameOneView.this.implementation.getActivity();

        final Rect rect = this.safeArea;
        final View rootView = activity.getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= VERSION_CODE_P) {
            try {
                Method getRootWindowInsetsMethod = View.class.getMethod("getRootWindowInsets");
                Object insets = getRootWindowInsetsMethod.invoke(rootView);
                if (insets != null) {
                    Class<?> windowInsetsClass = Class.forName("android.view.WindowInsets");
                    Method getDisplayCutoutMethod = windowInsetsClass.getMethod("getDisplayCutout");
                    Object cutout = getDisplayCutoutMethod.invoke(insets);

                    if (cutout != null) {
                        Class<?> displayCutoutClass = Class.forName("android.view.DisplayCutout");
                        Method getSafeInsetLeft = displayCutoutClass.getMethod("getSafeInsetLeft");
                        Method getSafeInsetTop = displayCutoutClass.getMethod("getSafeInsetTop");
                        Method getSafeInsetRight = displayCutoutClass.getMethod("getSafeInsetRight");
                        Method getSafeInsetBottom = displayCutoutClass.getMethod("getSafeInsetBottom");

                        int left = ((Integer) getSafeInsetLeft.invoke(cutout)).intValue();
                        int top = ((Integer) getSafeInsetTop.invoke(cutout)).intValue();
                        int right = ((Integer) getSafeInsetRight.invoke(cutout)).intValue();
                        int bottom = ((Integer) getSafeInsetBottom.invoke(cutout)).intValue();
                        boolean imeVisible = false;
                        try {
                            Method isVisibleMethod = insets.getClass().getMethod("isVisible", int.class);
                            Class<?> typeClass = Class.forName("android.view.WindowInsets$Type");
                            int imeType = ((Integer) typeClass.getMethod("ime").invoke(null)).intValue();
                            imeVisible = (Boolean) isVisibleMethod.invoke(insets, imeType);
                        } catch (Throwable t) {
                            // Fallback or log
                        }

                        Rect systemBarInsets = AndroidImplementation.getSystemBarInsets(rootView);
                        top = Math.max(systemBarInsets.top, top);
                        if (imeVisible) {
                            // Avoid double-counting the bottom gesture bar
                            bottom = Math.max(bottom, 0);
                        } else {
                            bottom = Math.max(systemBarInsets.bottom, bottom);
                        }
                        left = Math.max(systemBarInsets.left, left);
                        right = Math.max(systemBarInsets.right, right);

                        if (!AndroidImplementation.isImmersive()) {
                            top -= systemBarInsets.top;
                            if (!imeVisible) {
                                bottom -= systemBarInsets.bottom;
                            }
                            left -= systemBarInsets.left;
                            right -= systemBarInsets.right;
                        }

                        // Only apply if at least one is non-zero
                        if (left != 0 || top != 0 || right != 0 || bottom != 0) {
                            boolean isChanged = rect.left != left
                                    || rect.right != right
                                    || rect.top != top
                                    || rect.bottom != bottom;
                            rect.left = left;
                            rect.top = top;
                            rect.right = right;
                            rect.bottom = bottom;

                            if (isChanged) {
                                Display.getInstance().callSerially(new Runnable() {
                                    public void run() {
                                        AndroidImplementation.getInstance().revalidate();
                                    }
                                });
                            }
                        }
                    }
                }
            } catch (Exception e) {
                rect.top = 0;
                rect.left = 0;
                rect.right = 0;
                rect.bottom = 0;
            }

        } else if (Build.VERSION.SDK_INT >= VERSION_CODE_M) {
            rootView.post(new Runnable() {
                public void run() {
                    WindowInsets insets = rootView.getRootWindowInsets();
                    if (insets != null) {
                        rect.top = insets.getSystemWindowInsetTop();
                        rect.left = insets.getSystemWindowInsetLeft();;
                        rect.right = insets.getSystemWindowInsetRight();
                        rect.bottom = insets.getSystemWindowInsetBottom();
                    } else {
                        rect.top = 0;
                        rect.left = 0;
                        rect.right = 0;
                        rect.bottom = 0;
                    }
                }
            });
        } else {
            // For pre-Marshmallow (API < 23), assume full screen
            rect.top = 0;
            rect.left = 0;
            rect.right = 0;
            rect.bottom = 0;
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

        updateSafeArea();

        Log.d("Codename One", "sizechanged: " + width + " " + height + " " + this);
        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send events to the
             * EDT. if we send events before the form has been set we might
             * deadlock!
             */
            return;
        }

        if (InPlaceEditView.isEditing()) {
            final Form f = this.implementation.getCurrentForm();
            ActionListener sizeChanged = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    CodenameOneView.this.implementation.getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            InPlaceEditView.reLayoutEdit();
                            InPlaceEditView.scrollActiveTextfieldToVisible();
                            
                        }
                    });
                    f.removeSizeChangedListener(this);
                }
            };
            f.addSizeChangedListener(sizeChanged);
        }
        Display.getInstance().sizeChanged(w, h);
    }

    //@Override
    protected void d(Canvas canvas) {
        if(!drawing) {
            boolean empty = canvas.getClipBounds(bounds);
            if (empty) {
                // ??
                canvas.drawBitmap(bitmap, 0, 0, null);
            } else {
                bounds.intersect(0, 0, width, height);
                canvas.drawBitmap(bitmap, bounds, bounds, null);
            }
        }
    }

    /**
     * some info from the MIDP docs about keycodes:
     *
     * "Applications receive keystroke events in which the individual keys are
     * named within a space of key codes. Every key for which events are
     * reported to MIDP applications is assigned a key code. The key code values
     * are unique for each hardware key unless two keys are obvious synonyms for
     * each other. MIDP defines the following key codes: KEY_NUM0, KEY_NUM1,
     * KEY_NUM2, KEY_NUM3, KEY_NUM4, KEY_NUM5, KEY_NUM6, KEY_NUM7, KEY_NUM8,
     * KEY_NUM9, KEY_STAR, and KEY_POUND. (These key codes correspond to keys on
     * a ITU-T standard telephone keypad.) Other keys may be present on the
     * keyboard, and they will generally have key codes distinct from those list
     * above. In order to guarantee portability, applications should use only
     * the standard key codes.
     *
     * The standard key codes' values are equal to the Unicode encoding for the
     * character that represents the key. If the device includes any other keys
     * that have an obvious correspondence to a Unicode character, their key
     * code values should equal the Unicode encoding for that character. For
     * keys that have no corresponding Unicode character, the implementation
     * must use negative values. Zero is defined to be an invalid key code."
     *
     * Because the MIDP implementation is our reference and that implementation
     * does not interpret the given keycodes we behave alike and pass on the
     * unicode values.
     */
    final static int internalKeyCodeTranslate(int keyCode) {
        /**
         * make sure these important keys have a negative value when passed to
         * Codename One or they might be interpreted as characters.
         */
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
                if(Display.getInstance().getProperty("sendEnterKey", "false").equals("true")) {
                	if (down) {
                        Display.getInstance().keyPressed(keyCode);
                    } else {
                        Display.getInstance().keyReleased(keyCode);
                    }
               	    return false;
                }
                break;
                
            default:
        }

        if (event.getRepeatCount() > 0) {
            // skip repeats
            return true;
        }
        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send events to the
             * EDT. if we send events before the form has been set we might
             * deadlock!
             */
            return true;
        }


        if (keyCode == AndroidImplementation.DROID_IMPL_KEY_FIRE) {
            this.fireKeyDown = down;
        } else if (keyCode == AndroidImplementation.DROID_IMPL_KEY_DOWN
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_UP
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_LEFT
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_RIGHT) {
            if (this.fireKeyDown) {
                /**
                 * we keep track of trackball press/release. while it is pressed
                 * we drop directional movements. these movements are most
                 * likely not intended. if the device has no trackball i see no
                 * situation where this additional behavior could hurt.
                 */
                return true;
            }
        }

        switch (keyCode) {

            case AndroidImplementation.DROID_IMPL_KEY_MENU:
                //if the native commands are used don't handle the keycode
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
                // directly pass to display.
                if (down) {
                    Display.getInstance().keyPressed(keyCode);
                } else {
                    Display.getInstance().keyReleased(keyCode);
                }
                return true;

            default:

                /**
                 * Codename One's TextField does not seem to work well if two
                 * keyup-keydown sequences of different keys are not strictly
                 * sequential. so we pass the up event of a character right
                 * after the down event. this is exactly the behavior of the
                 * BlackBerry implementation from this repository and has worked
                 * well for me. i guess this should be changed as soon as the
                 * TextField changes.
                 */
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
    //private boolean nativePeerGrabbedPointer = false;
    
    public boolean onTouchEvent(MotionEvent event) {

        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send events to the
             * EDT. if we send events before the form has been set we might
             * deadlock!
             */
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
       /*
        if (!cn1GrabbedPointer) {
            
            if (x == null) {
                Component componentAt = this.implementation.getCurrentForm().getComponentAt((int)event.getX(), (int)event.getY());
                if (componentAt != null && (componentAt instanceof PeerComponent)) {
                    
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        //nativePeerGrabbedPointer = true;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        //nativePeerGrabbedPointer = false;
                    }
                    return false;
                }

            } else {
                Component componentAt = this.implementation.getCurrentForm().getComponentAt((int)x[0], (int)y[0]);
                if (componentAt != null && (componentAt instanceof PeerComponent)) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        nativePeerGrabbedPointer = true;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        nativePeerGrabbedPointer = false;
                    }
                    return false;
                }
            }
        }
        */
        
        //if (nativePeerGrabbedPointer) {
        //    return false;
        //}
        Component componentAt;
        try {
            if (x == null) {
                componentAt = this.implementation.getCurrentForm().getComponentAt((int)event.getX(), (int)event.getY());
            } else {
                componentAt = this.implementation.getCurrentForm().getComponentAt((int)x[0], (int)y[0]);
            }
        } catch (Throwable t) {
            // Since this is is an EDT violation, we may get an exception
            // Just consume it
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

    public Rect getSafeArea() {
        return safeArea;
    }

    public void setInputType(EditorInfo editorInfo) {

        /**
         * do not use the enter key to fire some kind of action!
         */
//        editorInfo.imeOptions |= EditorInfo.IME_ACTION_NONE;
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

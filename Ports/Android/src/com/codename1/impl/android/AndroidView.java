/*
 * Copyright 2009 Pader-Sync Ltd. & Co. KG.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  
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
 * Visit http://www.pader-sync.com/ for contact information.
 * 
 * 
 * Linking this library statically or dynamically with other modules is making a 
 * combined work based on this library. Thus, the terms and conditions of the GNU 
 * General Public License cover the whole combination.
 *
 *    As a special exception, the copyright holders of this library give you 
 *    permission to link this library with independent modules to produce an 
 *    executable, regardless of the license terms of these independent modules, and 
 *    to copy and distribute the resulting executable under terms of your choice, 
 *    provided that you also meet, for each linked independent module, the terms 
 *    and conditions of the license of that module. An independent module is a 
 *    module which is not derived from or based on this library. If you modify this 
 *    library, you may extend this exception to your version of the library, but you 
 *    are not obligated to do so. If you do not wish to do so, delete this exception 
 *    statement from your version.
 *
 * March 2009
 * Thorsten Schemm
 * http://www.pader-sync.com/
 * 
 */
package com.codename1.impl.android;

import com.codename1.ui.Display;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

public class AndroidView extends SurfaceView implements SurfaceHolder.Callback{

    private int width = 1;
    private int height = 1;
    private Bitmap bitmap;
    private AndroidGraphics buffy = null;
    private Canvas canvas;
    private AndroidImplementation implementation = null;
    private final KeyCharacterMap keyCharacterMap;
    private final Rect bounds = new Rect();
    private boolean fireKeyDown = false;
    private SurfaceHolder surfaceHolder = null;
    private volatile boolean created = false;


    public AndroidView(Activity activity, AndroidImplementation implementation) {
        super(activity);
        
        this.implementation = implementation;
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        setFocusable(true);
        setFocusableInTouchMode(true);
        setEnabled(true);
        setClickable(true);
        setLongClickable(false);
        /**
         * tell the system that we do our own caching and it does not
         * need to use an extra offscreen bitmap.
         */
        setWillNotCacheDrawing(false);

        this.buffy = new AndroidGraphics(implementation, null);
        this.keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.BUILT_IN_KEYBOARD);


        /**
         * From the docs:
         * "Change whether this view is one of the set of scrollable containers in its window.
         * This will be used to determine whether the window can resize or must pan when a soft
         * input area is open -- scrollable containers allow the window to use resize mode since the
         * container will appropriately shrink. "
         */
        setScrollContainer(true);


        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        android.view.Display androidDisplay = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        width = androidDisplay.getWidth();
        height = androidDisplay.getHeight();
        initBitmaps(width, height);
    }

    //@Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
//        if (this.implementation.editInProgress()) {
//            /**
//             * while edit is in progress a virtual keyboard might
//             * resize everything.  this is problematic because the
//             * editing (as it is implemented now) blocks the EDT.
//             * so we just drop these resize events for now.  once
//             * editing is complete we might apply the last state.
//             */
//            this.implementation.setLastSizeChangedWH(w, h);
//            return;
//        }

        this.handleSizeChange(w, h);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        created = true;
        this.visibilityChangedTo(true);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        created = false;
        this.visibilityChangedTo(false);
    }

    private void initBitmaps(int w, int h) {
        this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        this.canvas = new Canvas(this.bitmap);
        this.buffy.setCanvas(this.canvas);
    }

    private void visibilityChangedTo(boolean visible) {
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
        flushGraphics();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        // method used for View implementation. is it still
        // required with a SurfaceView?
        super.onWindowVisibilityChanged(visibility);
        this.visibilityChangedTo(visibility == View.VISIBLE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        this.handleSizeChange(w, h);
    }

    public void handleSizeChange(int w, int h) {
        if(!Display.isInitialized()) {
            return;
        }
        if ((this.width != w && (this.width < w || this.height < h)) ||
                (bitmap.getHeight() < h) ) {
            this.initBitmaps(w, h);
        }
        this.width = w;
        this.height = h;
        Log.d("Codename One", "sizechanged: " + width + " " + height + " "+ this);
        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send
             * events to the EDT.  if we send events before the
             * form has been set we might deadlock!
             */
            return;
        }
        
        if (InPlaceEditView.isEditing()) {            
            final Form f = this.implementation.getCurrentForm();
            ActionListener orientation = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    AndroidView.this.implementation.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            InPlaceEditView.reLayoutEdit();
                        }
                    });
                    f.removeOrientationListener(this);
                }
                
            };
            f.addOrientationListener(orientation);
        }
        Display.getInstance().sizeChanged(w, h);        
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean empty = canvas.getClipBounds(bounds);
        if (empty) {
            // ??
            canvas.drawBitmap(bitmap, 0, 0, null);
        } else {
            bounds.intersect(0, 0, width, height);
            canvas.drawBitmap(bitmap, bounds, bounds, null);
        }
        super.onDraw(canvas);
    }

    public void flushGraphics(Rect rect) {
        if (!created) {
            return;
        }
        Canvas c = null;
        try {
            c = this.surfaceHolder.lockCanvas(rect);
            if (c != null) {
                this.onDraw(c);
            }
        } catch (Throwable e) {
            Log.e("Codename One", "paint problem.", e);
        } finally {
            if (c != null) {
                this.surfaceHolder.unlockCanvasAndPost(c);
            }
        }
    }

    public void flushGraphics() {
        if (!created) {
            return;
        }
        Canvas c = null;
        try {
            c = this.surfaceHolder.lockCanvas();
            if (c != null) {
                this.onDraw(c);
            }
        } catch (Throwable e) {
            Log.e("Codename One", "paint problem.", e);
        } finally {
            if (c != null) {
                this.surfaceHolder.unlockCanvasAndPost(c);
            }
        }
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(InPlaceEditView.isEditing()){
            return true;
        }
        return this.onKeyUpDown(true, keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(InPlaceEditView.isEditing()){
            return true;
        }
        return this.onKeyUpDown(false, keyCode, event);
    }

    /**
     * some info from the MIDP docs about keycodes:
     *
     *  "Applications receive keystroke events in which the individual keys are named within a space of key codes.
     * Every key for which events are reported to MIDP applications is assigned a key code. The key code values are
     * unique for each hardware key unless two keys are obvious synonyms for each other. MIDP defines the following
     * key codes: KEY_NUM0, KEY_NUM1, KEY_NUM2, KEY_NUM3, KEY_NUM4, KEY_NUM5, KEY_NUM6, KEY_NUM7, KEY_NUM8, KEY_NUM9,
     * KEY_STAR, and KEY_POUND. (These key codes correspond to keys on a ITU-T standard telephone keypad.) Other
     * keys may be present on the keyboard, and they will generally have key codes distinct from those list above.
     * In order to guarantee portability, applications should use only the standard key codes.
     *
     * The standard key codes' values are equal to the Unicode encoding for the character that represents the key.
     * If the device includes any other keys that have an obvious correspondence to a Unicode character, their key
     * code values should equal the Unicode encoding for that character. For keys that have no corresponding Unicode
     * character, the implementation must use negative values. Zero is defined to be an invalid key code."
     *
     * Because the MIDP implementation is our reference and that implementation does not interpret the given keycodes
     * we behave alike and pass on the unicode values.
     */
    final static int internalKeyCodeTranslate(int keyCode) {
        /**
         * make sure these important keys have a negative value when passed
         * to Codename One or they might be interpreted as characters.
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

    private boolean onKeyUpDown(boolean down, int keyCode, KeyEvent event) {
        if (event.getRepeatCount() > 0) {
            // skip repeats
            return true;
        }
        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send
             * events to the EDT.  if we send events before the
             * form has been set we might deadlock!
             */
            return true;
        }

        keyCode = this.internalKeyCodeTranslate(keyCode);

        if (keyCode == AndroidImplementation.DROID_IMPL_KEY_FIRE) {
            this.fireKeyDown = down;
        } else if (keyCode == AndroidImplementation.DROID_IMPL_KEY_DOWN
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_UP
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_LEFT
                || keyCode == AndroidImplementation.DROID_IMPL_KEY_RIGHT) {
            if (this.fireKeyDown) {
                /**
                 * we keep track of trackball press/release.  while it is pressed we drop directional
                 * movements.  these movements are most likely not intended.  if the device has no
                 * trackball i see no situation where this additional behavior could hurt.
                 */
                return true;
            }
        }

        switch (keyCode) {

            case AndroidImplementation.DROID_IMPL_KEY_MENU:
                //if the native commands are used don't handle the keycode
                if(Display.getInstance().getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE){
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
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
            case KeyEvent.KEYCODE_SYM:
            case KeyEvent.KEYCODE_ENTER:
                // skip
                break;
            default:

                /**
                 * Codename One's TextField does not seem to work well if two keyup-keydown
                 * sequences of different keys are not strictly sequential.  so we
                 * pass the up event of a character right after the down event.  this is
                 * exactly the behavior of the BlackBerry implementation from this repository
                 * and has worked well for me.  i guess this should be changed as soon as
                 * the TextField changes.
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
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send
             * events to the EDT.  if we send events before the
             * form has been set we might deadlock!
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
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(x == null){
                    this.implementation.pointerPressed((int) event.getX(), (int) event.getY());
                }else{
                    this.implementation.pointerPressed(x, y);                
                }
                break;
            case MotionEvent.ACTION_UP:
                if(x == null){
                    this.implementation.pointerReleased((int) event.getX(), (int) event.getY());
                }else{
                    this.implementation.pointerReleased(x, y);                
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(x == null){
                    this.implementation.pointerDragged((int) event.getX(), (int) event.getY());
                }else{
                    this.implementation.pointerDragged(x, y);                
                }
                break;
        }

        return true;
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

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {

        if(!Display.isInitialized() || Display.getInstance().getCurrent() == null){
            return super.onCreateInputConnection(editorInfo);
        }

        /**
         * do not use the enter key to fire some kind of action!
         */
//        editorInfo.imeOptions |= EditorInfo.IME_ACTION_NONE;

            Component txtCmp = Display.getInstance().getCurrent().getFocused();
            if (txtCmp != null && txtCmp instanceof TextArea) {
                TextArea txt = (TextArea) txtCmp;
                if(txt.isSingleLineTextArea()){
                    editorInfo.imeOptions |= EditorInfo.IME_ACTION_DONE;

                }else{
                    editorInfo.imeOptions |= EditorInfo.IME_ACTION_NONE;
                }
                int inputType = 0;
                switch (txt.getConstraint()) {
                    case TextArea.NUMERIC:
                        inputType = EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_SIGNED;
                        break;
                    case TextArea.DECIMAL:
                        inputType = EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_FLAG_DECIMAL;
                        break;
                    case TextArea.PASSWORD:
                        inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD;
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
        return super.onCreateInputConnection(editorInfo);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        if(!Display.isInitialized() || Display.getInstance().getCurrent() == null){
            return false;
        }
        Component txtCmp = Display.getInstance().getCurrent().getFocused();
        if(txtCmp != null && txtCmp instanceof TextField){
            return true;
        }
        return false;
    }


}

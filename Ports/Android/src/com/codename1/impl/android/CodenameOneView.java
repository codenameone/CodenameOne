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
import com.codename1.security.TapjackingPolicy;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.Sheet;
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
        if (this.implementation.getCurrentForm() == null) {
            return;
        }
        if (visible) {
            this.implementation.showNotifyPublic();
            // request a full repaint as our surfaceview is most likely
            // black if this app comes back from the background.
            this.implementation.getCurrentForm().repaint();
        } else {
            this.implementation.hideNotifyPublic();
        }
    }

    private void updateSafeArea() {
        final Activity activity = CodenameOneView.this.implementation.getActivity();
        final Rect rect = this.safeArea;
        final View rootView = activity.getWindow().getDecorView();
        // Snapshotted for the single change test at the end. Each branch used to answer that
        // question for itself, and the round inset is applied AFTER all of them -- so on an API 28+
        // round watch reporting no cutout and no system bars, the branch decided nothing had
        // changed, applyRoundScreenInset then changed all four, and no revalidation was asked for.
        // The form stayed sized to the full rectangle with its corners under the bezel until some
        // unrelated change forced a pass, which is the same fault the API 23-27 branch had.
        final int wasTop = rect.top;
        final int wasLeft = rect.left;
        final int wasRight = rect.right;
        final int wasBottom = rect.bottom;
        if (Build.VERSION.SDK_INT >= VERSION_CODE_P) {
            try {
                Method getRootWindowInsetsMethod = View.class.getMethod("getRootWindowInsets");
                Object insets = getRootWindowInsetsMethod.invoke(rootView);
                if (insets != null) {
                    Class<?> windowInsetsClass = Class.forName("android.view.WindowInsets");
                    Method getDisplayCutoutMethod = windowInsetsClass.getMethod("getDisplayCutout");
                    Object cutout = getDisplayCutoutMethod.invoke(insets);

                    int left = 0;
                    int top = 0;
                    int right = 0;
                    int bottom = 0;
                    if (cutout != null) {
                        Class<?> displayCutoutClass = Class.forName("android.view.DisplayCutout");
                        Method getSafeInsetLeft = displayCutoutClass.getMethod("getSafeInsetLeft");
                        Method getSafeInsetTop = displayCutoutClass.getMethod("getSafeInsetTop");
                        Method getSafeInsetRight = displayCutoutClass.getMethod("getSafeInsetRight");
                        Method getSafeInsetBottom = displayCutoutClass.getMethod("getSafeInsetBottom");
                        left = ((Integer) getSafeInsetLeft.invoke(cutout)).intValue();
                        top = ((Integer) getSafeInsetTop.invoke(cutout)).intValue();
                        right = ((Integer) getSafeInsetRight.invoke(cutout)).intValue();
                        bottom = ((Integer) getSafeInsetBottom.invoke(cutout)).intValue();
                    }

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
                        // Assigned and left at that. Whether anything actually moved is decided
                        // once, at the end of the method, after the round inset has also been
                        // applied -- this branch cannot see that half, and testing in both places
                        // would schedule the same pass twice.
                        rect.left = left;
                        rect.top = top;
                        rect.right = right;
                        rect.bottom = bottom;
                    }
                }
            } catch (Throwable e) {
                rect.top = 0;
                rect.left = 0;
                rect.right = 0;
                rect.bottom = 0;
            }

        } else if (Build.VERSION.SDK_INT >= VERSION_CODE_M) {
            rootView.post(new Runnable() {
                public void run() {
                    // Remembered before anything is written, because this branch runs LATE and has
                    // to answer the same question the API 28 branch answers inline: did the safe
                    // area actually move? A round watch is the case where it always does -- the
                    // system reports no inset at all, so the whole rectangle comes from
                    // applyRoundScreenInset below.
                    int postedTop = rect.top;
                    int postedLeft = rect.left;
                    int postedRight = rect.right;
                    int postedBottom = rect.bottom;
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
                    // This branch assigns asynchronously, so the round inset has to be reapplied
                    // here -- applying it at the end of updateSafeArea would run first and be
                    // overwritten by the four assignments above.
                    applyRoundScreenInset(rect);
                    if (rect.top != postedTop || rect.left != postedLeft
                            || rect.right != postedRight || rect.bottom != postedBottom) {
                        // Nothing else will ask for it. The form is laid out before this callback
                        // runs, and the surface callback that would otherwise re-lay it returns
                        // early when the constructor already recorded these dimensions -- so a
                        // round Wear form stayed laid out against zero insets, with its corners
                        // under the bezel, until some unrelated change forced a pass.
                        Display.getInstance().callSerially(new Runnable() {
                            public void run() {
                                AndroidImplementation.getInstance().revalidate();
                            }
                        });
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
        applyRoundScreenInset(rect);
        // One test, after every contributor has had its say -- the platform insets above and the
        // round inset just applied. A round face is the case that needs it: the system reports no
        // inset at all there, so the whole safe area comes from applyRoundScreenInset and every
        // branch above concludes nothing changed.
        if (rect.top != wasTop || rect.left != wasLeft
                || rect.right != wasRight || rect.bottom != wasBottom) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    AndroidImplementation.getInstance().revalidate();
                }
            });
        }
    }

    /**
     * Widens the safe area to clear the curve on a round Wear OS display.
     *
     * A round watch face reports no display cutout, so everything above leaves the safe area at
     * zero and a layout drawn to the full rectangle has its corners cut off by the bezel. The
     * largest rectangle that fits inside a circle of diameter d has side d/sqrt(2), so each edge
     * loses about 14.6% -- that is what is reserved here, on top of whatever the system already
     * asked for.
     */
    private void applyRoundScreenInset(Rect rect) {
        if (!isRoundScreen()) {
            return;
        }
        int d = Math.min(this.width, this.height);
        if (d <= 0) {
            return;
        }
        int inset = (int) Math.ceil(d * (1 - 1 / Math.sqrt(2)) / 2);
        rect.left = Math.max(rect.left, inset);
        rect.top = Math.max(rect.top, inset);
        rect.right = Math.max(rect.right, inset);
        rect.bottom = Math.max(rect.bottom, inset);
    }

    /** True on a circular watch face, which is most Wear OS hardware. */
    private boolean isRoundScreen() {
        try {
            return this.implementation.getActivity().getResources()
                    .getConfiguration().isScreenRound();
        } catch (Throwable preApi23) {
            return false;
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
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                return AndroidImplementation.DROID_IMPL_KEY_ENTER;
            case KeyEvent.KEYCODE_TAB:
                return AndroidImplementation.DROID_IMPL_KEY_TAB;
            case KeyEvent.KEYCODE_ESCAPE:
                return AndroidImplementation.DROID_IMPL_KEY_ESCAPE;
            case KeyEvent.KEYCODE_MOVE_HOME:
                return AndroidImplementation.DROID_IMPL_KEY_HOME;
            case KeyEvent.KEYCODE_MOVE_END:
                return AndroidImplementation.DROID_IMPL_KEY_END;
            case KeyEvent.KEYCODE_PAGE_UP:
                return AndroidImplementation.DROID_IMPL_KEY_PAGE_UP;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                return AndroidImplementation.DROID_IMPL_KEY_PAGE_DOWN;
            case KeyEvent.KEYCODE_INSERT:
                return AndroidImplementation.DROID_IMPL_KEY_INSERT;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                return AndroidImplementation.DROID_IMPL_KEY_FORWARD_DEL;
            case KeyEvent.KEYCODE_F1:
                return AndroidImplementation.DROID_IMPL_KEY_F1;
            case KeyEvent.KEYCODE_F2:
                return AndroidImplementation.DROID_IMPL_KEY_F2;
            case KeyEvent.KEYCODE_F3:
                return AndroidImplementation.DROID_IMPL_KEY_F3;
            case KeyEvent.KEYCODE_F4:
                return AndroidImplementation.DROID_IMPL_KEY_F4;
            case KeyEvent.KEYCODE_F5:
                return AndroidImplementation.DROID_IMPL_KEY_F5;
            case KeyEvent.KEYCODE_F6:
                return AndroidImplementation.DROID_IMPL_KEY_F6;
            case KeyEvent.KEYCODE_F7:
                return AndroidImplementation.DROID_IMPL_KEY_F7;
            case KeyEvent.KEYCODE_F8:
                return AndroidImplementation.DROID_IMPL_KEY_F8;
            case KeyEvent.KEYCODE_F9:
                return AndroidImplementation.DROID_IMPL_KEY_F9;
            case KeyEvent.KEYCODE_F10:
                return AndroidImplementation.DROID_IMPL_KEY_F10;
            case KeyEvent.KEYCODE_F11:
                return AndroidImplementation.DROID_IMPL_KEY_F11;
            case KeyEvent.KEYCODE_F12:
                return AndroidImplementation.DROID_IMPL_KEY_F12;
            default:
                return keyCode;
        }
    }

    public boolean onKeyUpDown(boolean down, int keyCode, KeyEvent event) {
        // Capture the raw Android keycode before translation so we can ask the
        // KeyEvent for the unicode mapping (event.getUnicodeChar expects the
        // device's native keycode, not our negative sentinels).
        final int rawKeyCode = keyCode;
        keyCode = internalKeyCodeTranslate(keyCode);

        switch (rawKeyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
            case KeyEvent.KEYCODE_CTRL_LEFT:
            case KeyEvent.KEYCODE_CTRL_RIGHT:
            case KeyEvent.KEYCODE_META_LEFT:
            case KeyEvent.KEYCODE_META_RIGHT:
            case KeyEvent.KEYCODE_FUNCTION:
            case KeyEvent.KEYCODE_CAPS_LOCK:
            case KeyEvent.KEYCODE_NUM_LOCK:
            case KeyEvent.KEYCODE_SCROLL_LOCK:
            case KeyEvent.KEYCODE_SYM:
                return false;
            default:
        }

        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send events to the
             * EDT. if we send events before the form has been set we might
             * deadlock!
             */
            return true;
        }

        // Hardware (Bluetooth / Chromebook) keys bypass the IME and would otherwise be
        // dropped while a pure-editor input session is bound (the editor's raw key path is
        // disabled when the platform session is active). Route them through the same
        // translation the IME-synthesized keys use.
        if (AndroidImplementation.routeHardwareKeyToActiveClient(down, event)) {
            return true;
        }

        // ENTER is gated for back-compat: on touch keyboards Enter is the IME
        // "done" action, so apps historically had to opt in via sendEnterKey.
        // Default it on when a hardware (alpha) keyboard generated the event
        // so BT/Chromebook keyboards just work.
        if (keyCode == AndroidImplementation.DROID_IMPL_KEY_ENTER) {
            boolean optIn = Display.getInstance().getProperty("sendEnterKey", "false").equals("true");
            if (!optIn && !isHardwareKeyboardEvent(event)) {
                return false;
            }
        }

        if (event.getRepeatCount() > 0) {
            // skip repeats
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

        // Any key our translator mapped to a negative CN1 sentinel is forwarded
        // verbatim. The MENU sentinel still defers to the platform when native
        // commands are enabled.
        if (keyCode < 0) {
            if (keyCode == AndroidImplementation.DROID_IMPL_KEY_MENU
                    && Display.getInstance().getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE) {
                return false;
            }
            if (down) {
                Display.getInstance().keyPressed(keyCode);
            } else {
                Display.getInstance().keyReleased(keyCode);
            }
            return true;
        }

        /**
         * Codename One's TextField does not seem to work well if two
         * keyup-keydown sequences of different keys are not strictly
         * sequential. so we pass the up event of a character right
         * after the down event. this is exactly the behavior of the
         * BlackBerry implementation from this repository and has worked
         * well for me. i guess this should be changed as soon as the
         * TextField changes.
         */
        // Use the KeyEvent's own device mapping rather than the cached
        // BUILT_IN_KEYBOARD map: BT/USB keyboards on Android resolve their
        // own layout through KeyEvent.getUnicodeChar, including the full
        // meta state (SHIFT/ALT/CTRL/FN/CAPS).
        final int nextchar = event.getUnicodeChar(event.getMetaState());
        if (nextchar == 0) {
            // Non-printable key we don't translate (e.g. KEYCODE_BREAK,
            // media keys). Consume it silently rather than firing keyPressed(0).
            return true;
        }
        if (down) {
            Display.getInstance().keyPressed(nextchar);
        } else {
            Display.getInstance().keyReleased(nextchar);
        }
        return true;
    }

    private static boolean isHardwareKeyboardEvent(KeyEvent event) {
        android.view.InputDevice device = event.getDevice();
        if (device != null) {
            return device.getKeyboardType() == android.view.KeyCharacterMap.ALPHA;
        }
        return event.getDeviceId() != android.view.KeyCharacterMap.VIRTUAL_KEYBOARD;
    }

    private boolean cn1GrabbedPointer = false;
    //private boolean nativePeerGrabbedPointer = false;

    /**
     * Android's own obscured-touch filtering, which
     * {@code View.setFilterTouchesWhenObscured(true)} enables, runs inside
     * {@code View.dispatchTouchEvent} via {@code onFilterTouchEventForSecurity}.
     * {@link AndroidAsyncView#dispatchTouchEvent} -- the primary surface on every
     * modern device -- calls straight into {@link #onTouchEvent} and only falls
     * back to {@code super} when we decline the event, so that filtering never
     * runs on the path CN1 components are actually reached through. The check has
     * to be made explicitly here, or the flag would look enabled and do nothing.
     *
     * <p>Latched for the duration of a gesture rather than evaluated per event:
     * dropping only the ACTION_DOWN would deliver a pointerReleased with no
     * matching pointerPressed and leave the framework holding half a gesture.</p>
     */
    private boolean tapjackBlockedGesture = false;

    /**
     * The hover counterpart of {@link #tapjackBlockedGesture}, tracked separately
     * because hover enter/exit interleaves with touch rather than nesting inside
     * it -- a stylus can hover while a finger is down.
     */
    private boolean tapjackBlockedHover = false;

    /**
     * MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED. Added in API 21 but absent
     * from the android.jar this port compiles against, so the value is inlined --
     * the same approach the port already takes for FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
     * in AndroidImplementation.
     */
    private static final int FLAG_WINDOW_IS_PARTIALLY_OBSCURED = 0x2;

    /**
     * Reports the obscured flags on an event and answers whether the touch it
     * belongs to must be withheld from the application.
     *
     * @param event the event being handled
     * @return true when the caller must not dispatch this event
     */
    private boolean tapjacked(MotionEvent event) {
        TapjackingPolicy policy = this.implementation.getTapjackingPolicy();
        if (policy == null || !policy.isDetecting()) {
            return false;
        }
        boolean obscured;
        boolean partial;
        try {
            int flags = event.getFlags();
            obscured = (flags & MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0;
            partial = (flags & FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0;
        } catch (Throwable t) {
            // Detection must never break input handling.
            return false;
        }
        this.implementation.notifyScreenObscured(obscured || partial,
                obscured ? "obscured" : (partial ? "partiallyObscured" : null));
        return policy.blocks(obscured, partial);
    }

    public boolean onTouchEvent(MotionEvent event) {

        if (this.implementation.getCurrentForm() == null) {
            /**
             * make sure a form has been set before we can send events to the
             * EDT. if we send events before the form has been set we might
             * deadlock!
             */
            return true;
        }
        // Tapjacking has to be resolved before ANY side effect of the touch, not merely before
        // the pointer dispatch below. The keyboard re-summon that follows is one such side
        // effect: an obscured ACTION_UP reaching it would reopen the keyboard for a gesture the
        // BLOCK/STRICT policy promised to withhold. Anything added to this method later belongs
        // after this block for the same reason.
        //
        // Returns true rather than the consumeEvent computed further down. That value is false
        // when the touch landed on a native peer, and returning it would send the event back to
        // AndroidAsyncView.dispatchTouchEvent, which hands it to super.dispatchTouchEvent and
        // straight on to the peer -- delivering to a BrowserComponent or a native text field
        // precisely the touch we are withholding from everything else. Claiming the event is
        // what actually drops it.
        boolean tapjackBlocked = tapjacked(event);
        int tapjackAction = event.getAction();
        if (tapjackAction == MotionEvent.ACTION_DOWN) {
            tapjackBlockedGesture = tapjackBlocked;
        }
        if (tapjackBlockedGesture) {
            if (tapjackAction == MotionEvent.ACTION_UP || tapjackAction == MotionEvent.ACTION_CANCEL) {
                tapjackBlockedGesture = false;
                cn1GrabbedPointer = false;
            }
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            // EditText re-summons a dismissed keyboard on every tap; give the pure
            // editors the same behavior while their input session is bound
            AndroidImplementation.showSoftInputForActiveClient();
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
        if (isPeer) {
            int primaryX = x == null ? (int) event.getX() : x[0];
            int primaryY = y == null ? (int) event.getY() : y[0];
            isPeer = !Sheet.isSheetVisibleAt(primaryX, primaryY);
        }
        boolean consumeEvent = !isPeer || cn1GrabbedPointer;

        updatePointerMetadata(event, false);

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

    /**
     * Routes Android hover events (mouse / stylus moving over the surface
     * without a button pressed) into Codename One's pointerHover pipeline so
     * external pointing devices on Android (BT mouse, Chromebook trackpad,
     * stylus) drive hover-aware components.
     */
    public boolean onHoverEvent(MotionEvent event) {
        if (this.implementation.getCurrentForm() == null) {
            return false;
        }
        // Hover is a paired sequence rather than a gesture: the enter puts a component into a
        // hovered state that only the exit takes it out of. Filtering it the way touches are
        // filtered would mean an overlay appearing mid-hover swallows the exit for an enter
        // that was already delivered, leaving the component hovered permanently. So a sequence
        // that STARTS obscured is withheld in full -- no enter was delivered, so no exit is
        // owed -- while one that started clean always gets its release, whatever the flags say
        // by then.
        boolean hoverBlocked = tapjacked(event);
        int hoverAction = event.getActionMasked();
        if (hoverAction == MotionEvent.ACTION_HOVER_ENTER) {
            tapjackBlockedHover = hoverBlocked;
        }
        if (tapjackBlockedHover) {
            if (hoverAction == MotionEvent.ACTION_HOVER_EXIT) {
                tapjackBlockedHover = false;
            }
            return true;
        }
        if (hoverBlocked && hoverAction != MotionEvent.ACTION_HOVER_EXIT) {
            return true;
        }
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        updatePointerMetadata(event, true);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_ENTER:
                this.implementation.pointerHoverPressed(x, y);
                return true;
            case MotionEvent.ACTION_HOVER_MOVE:
                this.implementation.pointerHover(x, y);
                return true;
            case MotionEvent.ACTION_HOVER_EXIT:
                this.implementation.pointerHoverReleased(x, y);
                return true;
        }
        return false;
    }

    /**
     * Routes Android generic motion events into Codename One. This captures the
     * mouse wheel and trackpad scroll axes (vertical and horizontal) from
     * external pointing devices (BT mouse, Chromebook trackpad, DeX) which are
     * not delivered through onTouchEvent, and the Wear OS rotary input (the
     * rotating side button / bezel) which reports on a different axis again.
     */
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (this.implementation.getCurrentForm() == null) {
            return false;
        }
        if (tapjacked(event)) {
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_SCROLL) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            int step = this.implementation.convertToPixels(20, true);

            // Wear OS rotary input arrives from SOURCE_ROTARY_ENCODER on AXIS_SCROLL, not on the
            // mouse axes below -- a watch app that only handled those could not scroll at all. It
            // is the Digital Crown's counterpart, so it feeds the same wheel path, and Android
            // scales it by the device's own scroll factor rather than a fixed step.
            if (isRotaryEncoder(event)) {
                float rotary = event.getAxisValue(MotionEvent.AXIS_SCROLL);
                if (rotary == 0) {
                    return false;
                }
                int scrollY = Math.round(-rotary * rotaryScrollFactor(step));
                // NOT event.getX()/getY(). A rotary event is not a pointer event: it carries no
                // meaningful position and in practice reports (0,0), so feeding those coordinates
                // to pointerWheelMoved synthesized a drag over whatever occupies the top-left --
                // usually the title bar -- and the crown scrolled nothing on most screens.
                //
                // The crown scrolls what has focus, so aim at the focused component's scrollable
                // ancestor instead, falling back to the content pane when nothing is focused.
                int[] target = rotaryTarget();
                this.implementation.pointerWheelMoved(target[0], target[1], 0, scrollY, true,
                        motionModifierMask(event));
                return true;
            }

            float vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            float hscroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
            if (vscroll == 0 && hscroll == 0) {
                return false;
            }
            // A positive scrollY reveals content above (drag down); Android reports a
            // positive VSCROLL when scrolling away from the user, so negate to match.
            int scrollY = Math.round(-vscroll * step);
            int scrollX = Math.round(-hscroll * step);
            this.implementation.pointerWheelMoved(x, y, scrollX, scrollY, true, motionModifierMask(event));
            return true;
        }
        return false;
    }

    /**
     * A point inside whatever the crown should scroll.
     *
     * <p>Starts at the focused component itself -- a focused {@code TextArea} or {@code List} is
     * frequently the scrollable thing, with nothing scrollable above it -- and walks up to the
     * nearest vertically scrollable ancestor; without a focus, the content pane's centre. Any
     * point inside the right container will do -- the wheel path only uses it to decide which
     * component receives the scroll -- so the centre is chosen because it cannot land on a border
     * or a child that happens to sit at the container's origin.</p>
     */
    private int[] rotaryTarget() {
        com.codename1.ui.Form f = this.implementation.getCurrentForm();
        com.codename1.ui.Component anchor = null;
        if (f != null) {
            com.codename1.ui.Component c = f.getFocused();
            while (c != null && !c.isScrollableY()) {
                c = c.getParent();
            }
            anchor = c != null ? c : f.getContentPane();
        }
        if (anchor == null) {
            return new int[] {0, 0};
        }
        return new int[] {
            anchor.getAbsoluteX() + anchor.getWidth() / 2,
            anchor.getAbsoluteY() + anchor.getHeight() / 2
        };
    }

    /**
     * True when the event came from the Wear OS rotary input. SOURCE_ROTARY_ENCODER and AXIS_SCROLL
     * both arrived in API 23, which is also the Wear OS standalone baseline, so older devices
     * simply never match.
     */
    private static boolean isRotaryEncoder(MotionEvent event) {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            return false;
        }
        return (event.getSource() & InputDevice.SOURCE_ROTARY_ENCODER) == InputDevice.SOURCE_ROTARY_ENCODER;
    }

    /**
     * How many pixels one detent of rotary travel should scroll. Android publishes a per-device
     * factor for exactly this; fall back to the shared wheel step when it is unavailable so the
     * gesture still does something sensible.
     */
    private float rotaryScrollFactor(int fallbackStep) {
        try {
            float f = ViewConfiguration.get(this.implementation.getActivity())
                    .getScaledVerticalScrollFactor();
            if (f > 0) {
                return f;
            }
        } catch (Throwable notAvailable) {
            // Pre-API-26 or an unusual device configuration.
        }
        return fallbackStep;
    }

    /**
     * Translates the Android MotionEvent tool type, pressure, contact size, tilt
     * and button state into the cross-platform pointer metadata so the
     * multi-button mouse and stylus APIs work on Android. When hovering is true
     * the metadata is flagged as a hover (no contact).
     */
    private void updatePointerMetadata(MotionEvent event, boolean hovering) {
        int toolType;
        try {
            toolType = event.getToolType(0);
        } catch (Throwable t) {
            toolType = MotionEvent.TOOL_TYPE_UNKNOWN;
        }
        int type;
        switch (toolType) {
            case MotionEvent.TOOL_TYPE_STYLUS:
                type = com.codename1.ui.events.PointerEvent.TYPE_STYLUS;
                break;
            case MotionEvent.TOOL_TYPE_ERASER:
                type = com.codename1.ui.events.PointerEvent.TYPE_ERASER;
                break;
            case MotionEvent.TOOL_TYPE_MOUSE:
                type = com.codename1.ui.events.PointerEvent.TYPE_MOUSE;
                break;
            case MotionEvent.TOOL_TYPE_FINGER:
                type = com.codename1.ui.events.PointerEvent.TYPE_TOUCH;
                break;
            default:
                type = com.codename1.ui.events.PointerEvent.TYPE_UNKNOWN;
                break;
        }
        float pressure = event.getPressure(0);
        if (pressure <= 0) {
            pressure = 1f;
        }
        float contactSize = event.getSize(0);
        float tiltX = (float) Math.toDegrees(event.getAxisValue(MotionEvent.AXIS_TILT, 0));

        int buttonState = event.getButtonState();
        int mask = 0;
        if ((buttonState & MotionEvent.BUTTON_PRIMARY) != 0) {
            mask |= com.codename1.ui.events.PointerEvent.MASK_PRIMARY;
        }
        if ((buttonState & MotionEvent.BUTTON_SECONDARY) != 0) {
            mask |= com.codename1.ui.events.PointerEvent.MASK_SECONDARY;
        }
        if ((buttonState & MotionEvent.BUTTON_TERTIARY) != 0) {
            mask |= com.codename1.ui.events.PointerEvent.MASK_MIDDLE;
        }
        if ((buttonState & MotionEvent.BUTTON_BACK) != 0) {
            mask |= com.codename1.ui.events.PointerEvent.MASK_BACK;
        }
        if ((buttonState & MotionEvent.BUTTON_FORWARD) != 0) {
            mask |= com.codename1.ui.events.PointerEvent.MASK_FORWARD;
        }
        int button = com.codename1.ui.events.PointerEvent.BUTTON_PRIMARY;
        if ((mask & com.codename1.ui.events.PointerEvent.MASK_SECONDARY) != 0) {
            button = com.codename1.ui.events.PointerEvent.BUTTON_SECONDARY;
        } else if ((mask & com.codename1.ui.events.PointerEvent.MASK_MIDDLE) != 0) {
            button = com.codename1.ui.events.PointerEvent.BUTTON_MIDDLE;
        } else if ((mask & com.codename1.ui.events.PointerEvent.MASK_BACK) != 0) {
            button = com.codename1.ui.events.PointerEvent.BUTTON_BACK;
        } else if ((mask & com.codename1.ui.events.PointerEvent.MASK_FORWARD) != 0) {
            button = com.codename1.ui.events.PointerEvent.BUTTON_FORWARD;
        } else if (mask == 0) {
            mask = com.codename1.ui.events.PointerEvent.MASK_PRIMARY;
        }
        this.implementation.setPointerEventMetadata(button, mask, type, pressure, tiltX, 0, contactSize,
                motionModifierMask(event), hovering);
    }

    /**
     * Builds the cross-platform keyboard modifier mask from an Android MotionEvent meta state.
     */
    private int motionModifierMask(MotionEvent event) {
        int meta = event.getMetaState();
        int modifiers = 0;
        if ((meta & android.view.KeyEvent.META_SHIFT_ON) != 0) {
            modifiers |= com.codename1.ui.events.PointerEvent.MODIFIER_SHIFT;
        }
        if ((meta & android.view.KeyEvent.META_CTRL_ON) != 0) {
            modifiers |= com.codename1.ui.events.PointerEvent.MODIFIER_CONTROL;
        }
        if ((meta & android.view.KeyEvent.META_ALT_ON) != 0) {
            modifiers |= com.codename1.ui.events.PointerEvent.MODIFIER_ALT;
        }
        if ((meta & android.view.KeyEvent.META_META_ON) != 0) {
            modifiers |= com.codename1.ui.events.PointerEvent.MODIFIER_META;
        }
        return modifiers;
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

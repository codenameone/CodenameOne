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
package com.codename1.impl.javase.vkb;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.List;

/// Shows and hides the [SimulatedKeyboard] around a text edit, and gives the
/// application the screen geometry the device it imitates would give it.
///
/// The two platforms differ in what an application sees, and the simulator copies
/// each port rather than inventing a common behaviour:
///
/// - **iOS** (`IOSImplementation.keyboardWillBeShown`): the screen keeps its size.
///   `Display.getInvisibleAreaUnderVKB()` answers the keyboard height, a form in
///   bottom-padding editing mode has its root pane padded by that height, any other
///   form scrolls the field into view, and then the virtual keyboard listeners fire.
/// - **Android** (`InPlaceEditView.setEditMode`): in resize mode the window shrinks,
///   so the application gets `sizeChanged` with the height above the keyboard; in
///   pan mode the size is untouched and the whole screen is shifted up until the
///   field clears the keyboard. The port picks pan for a field inside a scrollable
///   parent and resize otherwise, and always resize in bottom-padding mode.
///
/// A hide is deferred briefly so that moving from one field to the next keeps the
/// keyboard up, as both platforms do, instead of animating it away and back.
public final class KeyboardController {

    /// What the port does for the controller. All calls arrive on the CN1 EDT.
    public interface Host {
        /// The full screen size, never reduced by the keyboard.
        int fullScreenWidth();

        int fullScreenHeight();

        boolean isTablet();

        /// Reduces the height the application sees by this many pixels (Android
        /// resize mode) and delivers the resulting size change; 0 restores it.
        void setResizeInset(int pixels);

        /// Shifts the drawn screen up by this many pixels (Android pan mode).
        void setPanOffset(int pixels);

        /// Repaints the simulator window without touching the application.
        void repaintOverlay();

        /// Delivers a key to the native text editor that is open.
        void deliverText(String text);

        void deliverBackspace();

        /// The return key: Next moves on, Done or a single line field finishes.
        void deliverReturn();

        /// Ends the edit, as the accessory bar's Done does.
        void deliverDone();
    }

    /// Keyboard emulation mode, as the Simulate menu and MCP set it.
    public enum Mode { OFF, AUTO, IOS, ANDROID }

    /// Android's soft input mode; AUTO decides per field like the port.
    public enum SoftInput { AUTO, RESIZE, PAN }

    /// Listener for keyboard state changes, used by the MCP tools.
    public interface Listener {
        void keyboardChanged(KeyboardController controller);
    }

    private static final int HIDE_DELAY_MS = 80;
    private static final int PAN_MARGIN = 8;

    private final Host host;
    private final SimulatedKeyboard keyboard = new SimulatedKeyboard();
    private final List<Listener> listeners = new ArrayList<Listener>();
    private Mode mode = Mode.OFF;
    private SoftInput softInput = SoftInput.AUTO;
    private String skinPlatform = "ios";

    private boolean showing;
    private int height;
    private Component editing;
    private boolean resizeMode;
    private int panOffset;
    private Container paddedRoot;
    private float savedPadding;
    private byte savedPaddingUnit;
    private long animationStart;
    private boolean animatingIn;
    private long hideToken;

    public KeyboardController(Host host) {
        this.host = host;
    }

    public SimulatedKeyboard getKeyboard() {
        return keyboard;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        if (mode == null) {
            mode = Mode.OFF;
        }
        if (this.mode == mode) {
            return;
        }
        if (showing) {
            hideNow();
        }
        this.mode = mode;
        fireChanged();
    }

    public SoftInput getSoftInput() {
        return softInput;
    }

    public void setSoftInput(SoftInput softInput) {
        this.softInput = softInput == null ? SoftInput.AUTO : softInput;
        fireChanged();
    }

    /// The `platformName` of the loaded skin, which AUTO mode follows.
    public void setSkinPlatform(String skinPlatform) {
        this.skinPlatform = skinPlatform;
    }

    public boolean isEnabled() {
        return mode != Mode.OFF;
    }

    /// The platform being imitated right now.
    public SimulatedKeyboard.Platform platform() {
        if (mode == Mode.ANDROID) {
            return SimulatedKeyboard.Platform.ANDROID;
        }
        if (mode == Mode.IOS) {
            return SimulatedKeyboard.Platform.IOS;
        }
        return "and".equals(skinPlatform) ? SimulatedKeyboard.Platform.ANDROID : SimulatedKeyboard.Platform.IOS;
    }

    public boolean isShowing() {
        return showing;
    }

    /// The keyboard height in screen pixels while it is showing, else 0.
    public int getHeight() {
        return showing ? height : 0;
    }

    public boolean isResizeMode() {
        return showing && resizeMode;
    }

    public int getPanOffset() {
        return showing ? panOffset : 0;
    }

    public Component getEditing() {
        return editing;
    }

    /// What `getInvisibleAreaUnderVKB()` answers: the keyboard height on iOS, as the
    /// iOS port reports it, and 0 on Android, whose window already excludes it.
    public int invisibleAreaUnderKeyboard() {
        if (!showing || platform() != SimulatedKeyboard.Platform.IOS) {
            return 0;
        }
        return height;
    }

    /// How far into its show or hide animation the keyboard is, from 0 (off screen)
    /// to 1 (fully up). iOS uses a quarter second ease out, Android a little less.
    public float visibleFraction() {
        long duration = platform() == SimulatedKeyboard.Platform.IOS ? 250 : 200;
        float t = Math.min(1f, (System.currentTimeMillis() - animationStart) / (float) duration);
        float eased = 1f - (1f - t) * (1f - t) * (1f - t);
        if (animatingIn) {
            return showing ? eased : 0f;
        }
        return showing ? 1f : 1f - eased;
    }

    /// True while a show or hide animation still needs frames.
    public boolean isAnimating() {
        long duration = platform() == SimulatedKeyboard.Platform.IOS ? 250 : 200;
        return System.currentTimeMillis() - animationStart < duration;
    }

    public void addListener(Listener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    private void fireChanged() {
        Listener[] copy;
        synchronized (listeners) {
            copy = listeners.toArray(new Listener[listeners.size()]);
        }
        for (Listener l : copy) {
            l.keyboardChanged(this);
        }
    }

    /// Called by the port, on the EDT, when native editing of a field starts.
    public void editingStarted(Component cmp, int constraint) {
        if (!isEnabled()) {
            return;
        }
        hideToken++;
        editing = cmp;
        keyboard.setPlatform(platform());
        keyboard.setTablet(host.isTablet());
        keyboard.setLayout(SimulatedKeyboard.layoutFor(constraint));
        keyboard.setShift(keyboard.getLayout() == SimulatedKeyboard.Layout.TEXT && isEmpty(cmp));
        keyboard.setReturnLabel(returnLabelFor(cmp));
        int newHeight = keyboard.preferredHeight(host.fullScreenWidth(), host.fullScreenHeight());
        boolean wasShowing = showing;
        if (wasShowing) {
            // Moving to another field: the keyboard stays, the geometry follows the
            // new field (its layout may differ, and pan must reveal the new field).
            undoGeometry(false);
        } else {
            animationStart = System.currentTimeMillis();
            animatingIn = true;
        }
        height = newHeight;
        showing = true;
        applyGeometry(cmp);
        if (!wasShowing) {
            Display.getInstance().fireVirtualKeyboardEvent(true);
        }
        host.repaintOverlay();
        fireChanged();
    }

    /// Called by the port, on any thread, when native editing ends.
    public void editingEnded() {
        if (!showing) {
            return;
        }
        final long token = ++hideToken;
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(HIDE_DELAY_MS);
                } catch (InterruptedException e) {
                    return;
                }
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        if (token == hideToken && showing) {
                            hideNow();
                        }
                    }
                });
            }
        }, "SimulatedKeyboardHide").start();
    }

    /// Hides at once, on the EDT.
    public void hideNow() {
        if (!showing) {
            return;
        }
        undoGeometry(true);
        showing = false;
        editing = null;
        animationStart = System.currentTimeMillis();
        animatingIn = false;
        Display.getInstance().fireVirtualKeyboardEvent(false);
        host.repaintOverlay();
        fireChanged();
    }

    private void applyGeometry(Component cmp) {
        if (platform() == SimulatedKeyboard.Platform.IOS) {
            resizeMode = false;
            Form f = cmp == null ? null : cmp.getComponentForm();
            if (f != null && f.isFormBottomPaddingEditingMode()) {
                Container root = f.getContentPane().getParent();
                if (root == null) {
                    root = f;
                }
                Style s = root.getUnselectedStyle();
                savedPadding = s.getPaddingFloatValue(false, Component.BOTTOM);
                byte[] units = s.getPaddingUnit();
                savedPaddingUnit = units == null ? Style.UNIT_TYPE_PIXELS : units[Component.BOTTOM];
                s.setPaddingUnitBottom(Style.UNIT_TYPE_PIXELS);
                s.setPadding(Component.BOTTOM, height);
                paddedRoot = root;
                f.revalidate();
            } else if (f != null) {
                f.revalidate();
            }
            scrollIntoView(cmp);
            return;
        }
        resizeMode = decideResize(cmp);
        if (resizeMode) {
            host.setResizeInset(height);
            scrollIntoView(cmp);
        } else {
            int visibleBottom = host.fullScreenHeight() - height;
            int fieldBottom = cmp == null ? 0 : cmp.getAbsoluteY() + cmp.getScrollY() + cmp.getHeight();
            panOffset = Math.max(0, fieldBottom + PAN_MARGIN - visibleBottom);
            host.setPanOffset(panOffset);
        }
    }

    private void undoGeometry(boolean restoreSize) {
        if (paddedRoot != null) {
            Style s = paddedRoot.getUnselectedStyle();
            s.setPaddingUnitBottom(savedPaddingUnit);
            s.setPadding(Component.BOTTOM, savedPadding);
            Form f = paddedRoot.getComponentForm();
            paddedRoot = null;
            if (f != null) {
                f.revalidate();
            }
        }
        if (panOffset != 0) {
            panOffset = 0;
            host.setPanOffset(0);
        }
        if (resizeMode && restoreSize) {
            resizeMode = false;
            host.setResizeInset(0);
        }
    }

    private boolean decideResize(Component cmp) {
        if (softInput == SoftInput.RESIZE) {
            return true;
        }
        if (softInput == SoftInput.PAN) {
            return false;
        }
        Form f = cmp == null ? null : cmp.getComponentForm();
        if (f != null && f.isFormBottomPaddingEditingMode()) {
            return true;
        }
        // InPlaceEditView picks pan for a field inside a scrollable parent.
        Container p = cmp == null ? null : cmp.getParent();
        while (p != null && p != f) {
            if (p.isScrollableY()) {
                return false;
            }
            p = p.getParent();
        }
        if (f != null && f.getContentPane().isScrollableY()) {
            return false;
        }
        return true;
    }

    private void scrollIntoView(final Component cmp) {
        if (cmp == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Form f = cmp.getComponentForm();
                if (f != null && cmp.isVisible()) {
                    f.scrollComponentToVisible(cmp);
                }
            }
        });
    }

    private static boolean isEmpty(Component cmp) {
        return cmp instanceof TextArea && ((TextArea) cmp).getText().length() == 0;
    }

    private static String returnLabelFor(Component cmp) {
        if (!(cmp instanceof TextArea)) {
            return null;
        }
        TextArea ta = (TextArea) cmp;
        if (!ta.isSingleLineTextArea()) {
            return null;
        }
        Form f = ta.getComponentForm();
        if (f != null && f.getNextComponent(ta) instanceof TextArea) {
            return "Next";
        }
        return "Done";
    }

    /// Next on both platforms moves editing to the following field and keeps the
    /// keyboard up. The simulator's native editor only ends the edit on its action
    /// event, so the following field is started here; the deferred hide is then
    /// cancelled by its [#editingStarted].
    private void moveToNextField() {
        final Component current = editing;
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Form f = current == null ? null : current.getComponentForm();
                Component next = f == null ? null : f.getNextComponent(current);
                if (next instanceof TextArea && ((TextArea) next).isEditable()) {
                    next.requestFocus();
                    ((TextArea) next).startEditingAsync();
                }
            }
        });
    }

    // ---- presses on the keyboard, from the mouse or from MCP ----

    /// Presses the key at a point in keyboard coordinates. Returns false when the
    /// point is on the keyboard but not on a key; nothing is delivered then.
    public boolean pressAt(int kx, int ky) {
        keyboard.layout(host.fullScreenWidth(), height);
        SimulatedKeyboard.Key k = keyboard.keyAt(kx, ky);
        keyboard.setPressed(k);
        host.repaintOverlay();
        return k != null;
    }

    /// Releases the pressed key, delivering it.
    public void release() {
        SimulatedKeyboard.Key k = keyboard.getPressed();
        keyboard.setPressed(null);
        if (k != null) {
            activate(k);
        }
        host.repaintOverlay();
    }

    /// Types text as if each character were tapped on the keyboard, flashing the
    /// matching key so a recording shows it. Characters with no key on the current
    /// layout are still delivered.
    public void typeChar(String ch) {
        keyboard.layout(host.fullScreenWidth(), height);
        SimulatedKeyboard.Key k = keyboard.findKey(ch);
        flash(k);
        if ("\n".equals(ch)) {
            host.deliverReturn();
            if ("Next".equals(keyboard.getReturnLabel())) {
                moveToNextField();
            }
        } else {
            host.deliverText(ch);
            if (keyboard.isShift() && Character.isLetter(ch.charAt(0))) {
                keyboard.setShift(false);
            }
        }
    }

    /// Presses a special key by name: done, return, next, backspace, shift, space.
    public boolean pressNamed(String name) {
        keyboard.layout(host.fullScreenWidth(), height);
        SimulatedKeyboard.KeyKind kind;
        if ("backspace".equals(name) || "delete".equals(name)) {
            kind = SimulatedKeyboard.KeyKind.BACKSPACE;
        } else if ("return".equals(name) || "next".equals(name) || "enter".equals(name)) {
            kind = SimulatedKeyboard.KeyKind.RETURN;
        } else if ("done".equals(name)) {
            kind = keyboard.hasAccessoryBar() ? SimulatedKeyboard.KeyKind.DONE : SimulatedKeyboard.KeyKind.RETURN;
        } else if ("shift".equals(name)) {
            kind = SimulatedKeyboard.KeyKind.SHIFT;
        } else if ("space".equals(name)) {
            kind = SimulatedKeyboard.KeyKind.SPACE;
        } else {
            return false;
        }
        SimulatedKeyboard.Key k = keyboard.findKind(kind);
        if (k == null) {
            if (kind == SimulatedKeyboard.KeyKind.RETURN) {
                host.deliverDone();
                return true;
            }
            return false;
        }
        flash(k);
        activate(k);
        return true;
    }

    private void flash(SimulatedKeyboard.Key k) {
        keyboard.setPressed(k);
        host.repaintOverlay();
        final SimulatedKeyboard.Key pressedKey = k;
        Runnable r = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(90);
                } catch (InterruptedException e) {
                    return;
                }
                if (keyboard.getPressed() == pressedKey) {
                    keyboard.setPressed(null);
                    host.repaintOverlay();
                }
            }
        };
        new Thread(r, "SimulatedKeyFlash").start();
    }

    private void activate(SimulatedKeyboard.Key k) {
        switch (k.getKind()) {
            case CHAR:
            case SPACE:
                host.deliverText(k.getOutput());
                if (keyboard.isShift() && k.getKind() == SimulatedKeyboard.KeyKind.CHAR) {
                    keyboard.setShift(false);
                }
                break;
            case SHIFT:
                keyboard.setShift(!keyboard.isShift());
                break;
            case BACKSPACE:
                host.deliverBackspace();
                break;
            case MODE:
                keyboard.setSymbols(!keyboard.isSymbols());
                break;
            case RETURN:
                host.deliverReturn();
                if ("Next".equals(keyboard.getReturnLabel())) {
                    moveToNextField();
                }
                break;
            case DONE:
                host.deliverDone();
                break;
            default:
                break;
        }
        fireChanged();
    }
}

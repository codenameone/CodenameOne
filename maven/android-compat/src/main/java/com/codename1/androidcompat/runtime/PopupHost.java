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
package com.codename1.androidcompat.runtime;

import com.codename1.components.InteractionDialog;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;

import java.util.ArrayList;
import java.util.List;

/// The Codename One window behind an `android.widget.PopupWindow`: an
/// overlay on the current form's layered pane that neither dims nor blocks
/// what is under it, placed at an exact rectangle in screen coordinates and
/// drawn with an elevation shadow around the Android content.
public final class PopupHost extends InteractionDialog {

    /// Told when the host goes away by itself (an outside tap), so the
    /// popup that owns it can run its dismiss logic.
    public interface Owner {
        void onHostDismissed();

        /// Whether the back key closes this popup.
        boolean dismissesOnBack();

        void dismissFromHost();
    }

    private static final List<PopupHost> SHOWING = new ArrayList<PopupHost>();

    private final Owner owner;
    private final int shadow;
    private final int radius;
    private boolean closing;

    /// - `content`: the Android decor's peer, sized by this host
    /// - `shadow`: pixels around the content reserved for the shadow
    /// - `radius`: the corner radius the shadow follows
    public PopupHost(Component content, int shadow, int radius, Owner owner) {
        super(new BorderLayout());
        this.owner = owner;
        this.shadow = Math.max(0, shadow);
        this.radius = Math.max(0, radius);
        setFormMode(true);
        setNativeWindowMode(false);
        setAnimateShow(false);
        setAnimationSpeed(0);
        strip(getAllStyles(), this.shadow);
        strip(getContentPane().getAllStyles(), 0);
        getTitleComponent().setHidden(true);
        getTitleComponent().setVisible(false);
        getContentPane().add(BorderLayout.CENTER, content);
    }

    private static void strip(Style s, int padding) {
        s.setBgTransparency(0);
        s.setBgImage(null);
        s.setBorder(Border.createEmpty());
        s.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        s.setPadding(padding, padding, padding, padding);
        s.setMarginUnit(Style.UNIT_TYPE_PIXELS);
        s.setMargin(0, 0, 0, 0);
    }

    /// The area popups may occupy, in screen coordinates: the current
    /// form's safe area.
    public static Rectangle displayFrame() {
        Form f = android.app.ActivityThread.visibleForm();
        if (f == null) {
            return new Rectangle(0, 0, Display.getInstance().getDisplayWidth(),
                    Display.getInstance().getDisplayHeight());
        }
        Rectangle safe = f.getSafeArea();
        int fx = f.getAbsoluteX();
        int fy = f.getAbsoluteY();
        if (safe == null || safe.getWidth() <= 0 || safe.getHeight() <= 0) {
            return new Rectangle(fx, fy, f.getWidth(), f.getHeight());
        }
        return new Rectangle(fx + safe.getX(), fy + safe.getY(), safe.getWidth(), safe.getHeight());
    }

    /// Shows or moves the host so the content occupies `x, y, w, h` on
    /// screen.
    public void place(int x, int y, int w, int h, boolean outsideDismisses) {
        setDisposeWhenPointerOutOfBounds(outsideDismisses);
        Form f = android.app.ActivityThread.visibleForm();
        int fx = f == null ? 0 : f.getAbsoluteX();
        int fy = f == null ? 0 : f.getAbsoluteY();
        int fw = f == null ? Display.getInstance().getDisplayWidth() : f.getWidth();
        int fh = f == null ? Display.getInstance().getDisplayHeight() : f.getHeight();
        int top = Math.max(0, y - fy - shadow);
        int left = Math.max(0, x - fx - shadow);
        int bottom = Math.max(0, fh - (y - fy) - h - shadow);
        int right = Math.max(0, fw - (x - fx) - w - shadow);
        if (isShowing()) {
            resize(top, bottom, left, right);
        } else {
            if (!SHOWING.contains(this)) {
                SHOWING.add(this);
            }
            show(top, bottom, left, right);
        }
    }

    private boolean passThrough;

    /// A popup that is not touchable (a toast) lets touches reach what is
    /// under it: it reports containing no point, so hit testing skips it.
    public void setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;
        setGrabsPointerEvents(!passThrough);
    }

    @Override
    public boolean contains(int x, int y) {
        return !passThrough && super.contains(x, y);
    }

    /// Closes the host on the popup's behalf, without calling it back.
    public void close() {
        closing = true;
        SHOWING.remove(this);
        if (isShowing()) {
            dispose();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        SHOWING.remove(this);
        if (!closing) {
            closing = true;
            owner.onHostDismissed();
        }
    }

    /// Back closes the most recently shown popup that asks for it, as the
    /// window manager delivers back to the top focused window on Android.
    public static boolean dismissTopOnBack() {
        for (int i = SHOWING.size() - 1; i >= 0; i--) {
            PopupHost h = SHOWING.get(i);
            if (h.owner.dismissesOnBack()) {
                h.owner.dismissFromHost();
                return true;
            }
        }
        return false;
    }

    /// The Material elevation shadow: layered translucent rounded rectangles
    /// in the reserved padding, offset slightly downward like a key light.
    @Override
    protected void paintBackground(Graphics g) {
        if (shadow <= 0) {
            return;
        }
        int x = getX() + shadow;
        int y = getY() + shadow;
        int w = getWidth() - shadow * 2;
        int h = getHeight() - shadow * 2;
        if (w <= 0 || h <= 0) {
            return;
        }
        int alpha = g.getAlpha();
        int color = g.getColor();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        g.setColor(0);
        int step = Math.max(1, Math.round(80f / shadow));
        int dy = shadow / 3;
        for (int i = shadow; i > 0; i--) {
            g.setAlpha(alpha * step / 255);
            int r = radius * 2 + i * 2;
            g.fillRoundRect(x - i, y - i + dy, w + i * 2, h + i * 2 - dy, r, r);
        }
        g.setAlpha(alpha);
        g.setColor(color);
        g.setAntiAliased(aa);
    }
}

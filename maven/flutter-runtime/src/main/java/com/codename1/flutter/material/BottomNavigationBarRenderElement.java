/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.widgets.Icon;
import com.codename1.flutter.widgets.Text;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Render element for {@link BottomNavigationBar}. Owns a background
 * Container (UIID "FlutterBottomNavigationBar", surface-colored) painted
 * behind the items, mounts per item a synthesized icon and (when a label is
 * set) a synthesized 12lp {@link Text}, and a transparent tap overlay LAST
 * that maps the release x-coordinate to an item index for {@code onTap}.
 *
 * <p>Selected-item tinting: when an item's icon is a plain {@link Icon}
 * without an explicit color, a tinted copy is mounted (primary for the
 * currentIndex item, onSurface otherwise); custom icon widgets mount
 * unchanged. Bar height: 80lp (M3 navigation bar).</p>
 */
public class BottomNavigationBarRenderElement extends RenderElement {

    /** M3 navigation bar height in logical pixels. */
    public static final double HEIGHT_LP = 80;
    /** Top padding above the icons in logical pixels. */
    public static final double TOP_PAD_LP = 12;
    /** Gap between icon and label in logical pixels. */
    public static final double LABEL_GAP_LP = 4;
    /** Label font size in logical pixels. */
    public static final double LABEL_FONT_LP = 12;
    /** Fallback slot width when the incoming width is unbounded. */
    public static final double FALLBACK_SLOT_WIDTH_LP = 80;

    private final List<Element> iconChildren = new ArrayList<Element>();
    private final List<Element> labelChildren = new ArrayList<Element>();
    private Element overlayChild;

    public BottomNavigationBarRenderElement(BottomNavigationBar widget) {
        super(widget);
    }

    private BottomNavigationBar bar() {
        return (BottomNavigationBar) widget();
    }

    private List<BottomNavigationBarItem> items() {
        List<BottomNavigationBarItem> out = new ArrayList<BottomNavigationBarItem>();
        if (bar().getItems() != null) {
            for (BottomNavigationBarItem i : bar().getItems()) {
                if (i != null) {
                    out.add(i);
                }
            }
        }
        return out;
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Container c = new Container();
        c.setUIID("FlutterBottomNavigationBar");
        c.getAllStyles().setPadding(0, 0, 0, 0);
        c.getAllStyles().setMargin(0, 0, 0, 0);
        style(c);
        return c;
    }

    @Override
    protected void updateComponent(Component c) {
        style(c);
    }

    /**
     * The bar's own {@code backgroundColor} first, the theme's surface second.
     *
     * <p>The three colours a caller can set -- background, selected item,
     * unselected item -- were all accepted and discarded, so a bar that names
     * them (the bottom-navigation demo asks for a primary-coloured bar with
     * white labels) came out surface-coloured with dark ink: the right shape in
     * entirely the wrong palette.</p>
     */
    private void style(Component c) {
        try {
            Color bg = bar().getBackgroundColor();
            if (bg == null) {
                bg = Theme.of(this).colorScheme().surface();
            }
            c.getAllStyles().setBgColor(bg.rgb());
            c.getAllStyles().setBgTransparency(255);
        } catch (Exception err) {
            // styling is best-effort; the base theme look remains
        }
    }

    private Widget iconWidgetFor(BottomNavigationBarItem item, boolean selected) {
        Widget w = item.getIcon();
        if (w instanceof Icon && ((Icon) w).getColor() == null) {
            Icon src = (Icon) w;
            Icon tinted = new Icon(src.getIcon());
            if (src.getSize() != null) {
                tinted.size(src.getSize());
            }
            tinted.color(tintFor(selected));
            return tinted;
        }
        return w;
    }

    private Widget labelWidgetFor(BottomNavigationBarItem item, boolean selected) {
        if (item.getLabel() == null) {
            return null;
        }
        Text t = new Text(item.getLabel());
        TextStyle ts = new TextStyle();
        ts.fontSize(LABEL_FONT_LP);
        ts.color(tintFor(selected));
        t.style(ts);
        return t;
    }

    private Color tintFor(boolean selected) {
        Color own = selected ? bar().getSelectedItemColor() : bar().getUnselectedItemColor();
        if (own != null) {
            return own;
        }
        ColorScheme cs = Theme.of(this).colorScheme();
        return selected ? cs.primary() : cs.onSurface();
    }

    @Override
    protected void syncChildren() {
        List<BottomNavigationBarItem> items = items();
        int n = items.size();
        // shrink leftover slots when the item count drops
        while (iconChildren.size() > n) {
            Element leftover = iconChildren.remove(iconChildren.size() - 1);
            if (leftover != null) {
                deactivateChild(leftover);
            }
        }
        while (labelChildren.size() > n) {
            Element leftover = labelChildren.remove(labelChildren.size() - 1);
            if (leftover != null) {
                deactivateChild(leftover);
            }
        }
        while (iconChildren.size() < n) {
            iconChildren.add(null);
        }
        while (labelChildren.size() < n) {
            labelChildren.add(null);
        }
        long current = bar().getCurrentIndex();
        for (int i = 0; i < n; i++) {
            boolean selected = i == current;
            BottomNavigationBarItem item = items.get(i);
            iconChildren.set(i, updateChild(iconChildren.get(i), iconWidgetFor(item, selected), 2 * i));
            labelChildren.set(i, updateChild(labelChildren.get(i), labelWidgetFor(item, selected), 2 * i + 1));
        }
        overlayChild = updateChild(overlayChild, new NavOverlay(), 2 * n);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        int n = Math.max(iconChildren.size(), labelChildren.size());
        for (int i = 0; i < n; i++) {
            if (i < iconChildren.size() && iconChildren.get(i) != null) {
                visitor.call(iconChildren.get(i));
            }
            if (i < labelChildren.size() && labelChildren.get(i) != null) {
                visitor.call(labelChildren.get(i));
            }
        }
        if (overlayChild != null) {
            visitor.call(overlayChild);
        }
    }

    /**
     * The render element of item {@code i}'s icon (test hook).
     */
    public RenderElement iconRenderElement(int i) {
        return RenderElement.findRenderElement(iconChildren.get(i));
    }

    /**
     * Fires onTap with the item index (public so tests / the overlay can
     * drive it).
     */
    public void userTapped(int index) {
        int n = items().size();
        if (index < 0 || index >= n) {
            return;
        }
        Funcs.VoidFunc1<Long> f = bar().getOnTap();
        if (f != null) {
            f.call((long) index);
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        int n = Math.max(1, items().size());
        double height = constraints.constrainHeight(Dp.px(HEIGHT_LP));
        double width = constraints.hasBoundedWidth()
                ? constraints.maxWidth()
                : n * Dp.px(FALLBACK_SLOT_WIDTH_LP);
        double slotW = width / n;
        double topPad = Dp.px(TOP_PAD_LP);
        double labelGap = Dp.px(LABEL_GAP_LP);

        for (int i = 0; i < items().size(); i++) {
            RenderElement icon = RenderElement.findRenderElement(
                    i < iconChildren.size() ? iconChildren.get(i) : null);
            RenderElement label = RenderElement.findRenderElement(
                    i < labelChildren.size() ? labelChildren.get(i) : null);
            BoxConstraints slotLoose = BoxConstraints.loose(slotW, height);
            Size is = icon != null ? icon.layout(slotLoose) : Size.ZERO;
            Size ls = label != null ? label.layout(slotLoose) : Size.ZERO;
            double slotX = i * slotW;
            if (icon != null) {
                double iconY = label != null
                        ? topPad
                        : (height - is.height()) / 2;
                setChildOffset(icon, slotX + (slotW - is.width()) / 2, iconY);
            }
            if (label != null) {
                setChildOffset(label, slotX + (slotW - ls.width()) / 2,
                        topPad + is.height() + labelGap);
            }
        }
        RenderElement overlay = RenderElement.findRenderElement(overlayChild);
        if (overlay != null) {
            overlay.layout(BoxConstraints.tight(width, height));
            setChildOffset(overlay, 0, 0);
        }
        return constraints.constrain(new Size(width, height));
    }

    // ------------------------------------------------------------------
    // Tap overlay (synthesized, mounts after the items)
    // ------------------------------------------------------------------

    static class NavOverlay extends Widget {
        @Override
        public Element createElement() {
            return new NavOverlayElement(this);
        }
    }

    static class NavOverlayElement extends RenderElement {

        NavOverlayElement(NavOverlay widget) {
            super(widget);
        }

        private BottomNavigationBarRenderElement barElement() {
            Element p = parent();
            return p instanceof BottomNavigationBarRenderElement
                    ? (BottomNavigationBarRenderElement) p : null;
        }

        @Override
        protected Component createComponent() {
            if (!Display.isInitialized()) {
                // headless unit tests: no CN1 components can exist
                return null;
            }
            return new OverlayComponent();
        }

        @Override
        protected Size performLayout(BoxConstraints constraints) {
            // the parent hands us tight constraints matching the bar bounds
            return constraints.smallest();
        }

        class OverlayComponent extends Component {

            OverlayComponent() {
                setUIID("FlutterGesture");
                setGrabsPointerEvents(true);
                setFocusable(false);
                getAllStyles().setBgTransparency(0);
                getAllStyles().setPadding(0, 0, 0, 0);
                getAllStyles().setMargin(0, 0, 0, 0);
            }

            @Override
            public void paint(Graphics g) {
                // paints nothing — pure hit area
            }

            @Override
            public void pointerReleased(int x, int y) {
                boolean wasDrag = isDragActivated();
                super.pointerReleased(x, y);
                if (wasDrag || !contains(x, y)) {
                    return;
                }
                BottomNavigationBarRenderElement bar = barElement();
                if (bar == null || getWidth() <= 0) {
                    return;
                }
                int n = bar.items().size();
                if (n == 0) {
                    return;
                }
                int index = (x - getAbsoluteX()) * n / getWidth();
                bar.userTapped(Math.max(0, Math.min(n - 1, index)));
            }
        }
    }
}

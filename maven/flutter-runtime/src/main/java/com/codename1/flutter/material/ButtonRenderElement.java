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

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.widgets.Icon;
import com.codename1.flutter.widgets.Text;
import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.RoundBorder;

/**
 * Shared leaf render box for the material buttons (ElevatedButton,
 * TextButton, OutlinedButton, IconButton), owning a CN1 {@link Button}. The
 * content widget is consumed as configuration rather than mounted: a
 * {@link Text} child becomes the button label, an {@link Icon} child the
 * material icon; any other widget falls back to its {@code toString()} with
 * a log warning. A null {@code onPressed} disables the button.
 *
 * <p>Styling is programmatic Material 3 on top of the base theme, with
 * colors derived from the nearest MaterialApp ThemeData's ColorScheme
 * (Theme.of): ElevatedButton is a primary-filled capsule, TextButton
 * borderless primary text, OutlinedButton a 1lp-outline capsule, IconButton
 * a bare icon.</p>
 */
public class ButtonRenderElement extends RenderElement {

    /** Flutter's default icon-button glyph size in logical pixels. */
    public static final double DEFAULT_ICON_SIZE_LP = 24;
    private static final double CAPSULE_HPAD_LP = 24;
    private static final double CAPSULE_VPAD_LP = 10;

    public ButtonRenderElement(Widget widget) {
        super(widget);
    }

    // ------------------------------------------------------------------
    // Configuration accessors (per widget kind)
    // ------------------------------------------------------------------

    /** Test hook: the handler a press would run, or null when the button is disabled. */
    public dart.runtime.Funcs.VoidFunc0 pressHandler() {
        return onPressed();
    }

    /** What a press does. Overridden by buttons that act rather than call back. */
    protected dart.runtime.Funcs.VoidFunc0 onPressed() {
        Widget w = widget();
        if (w instanceof ButtonBase) {
            return ((ButtonBase) w).getOnPressed();
        }
        return ((IconButton) w).getOnPressed();
    }

    /** The widget the button draws. Overridden by buttons with their own trigger. */
    protected Widget contentWidget() {
        return unwrapToLeaf(rawContentWidget());
    }

    /**
     * The content child exactly as written, without the walk to a leaf. Overridden -- not
     * {@link #contentWidget()} -- by buttons with a trigger of their own, so both the
     * consumed form and the mounted form come from one place.
     */
    protected Widget rawContentWidget() {
        Widget w = widget();
        return w instanceof ButtonBase
                ? ((ButtonBase) w).getChild()
                : ((IconButton) w).getIcon();
    }

    /**
     * Whether this button's content is a real widget subtree rather than something the
     * button can swallow into its own text or glyph.
     *
     * <p>A button draws a label and an icon, so a child that is one of those is CONSUMED:
     * cheaper, and it lets the button's own style reach the text. Anything else has to be
     * mounted and laid out like any other subtree, and that is not an exotic case -- the
     * compose page's account row is a PopupMenuButton whose child is a Row of an address
     * and a caret. Consumed, it could be reduced to neither a string nor a glyph, so the
     * button drew nothing at all and the row was a blank band on the page.</p>
     */
    protected boolean isCompositeContent() {
        Widget raw = rawContentWidget();
        if (raw == null) {
            return false;
        }
        Widget leaf = contentWidget();
        if (!(leaf instanceof Text) && !(leaf instanceof Icon)) {
            return true;
        }
        // ...and a leaf reached THROUGH a widget that has its own behaviour has
        // to be mounted too, however drawable it is.
        //
        // Consuming the glyph skips the wrapper entirely: it is never mounted,
        // so its State never initialises and whatever it exists to do never
        // happens. The gallery's demo pages wrap the options icon in a
        // FeatureDiscovery whose whole job is to insert a coach mark on first
        // view; the button rendered its icon perfectly and the coach mark was
        // never built, because the widget that builds it was never alive.
        return hasStatefulWrapper(rawContentWidget(), leaf);
    }

    /**
     * Whether reaching {@code leaf} from {@code raw} passes through a widget
     * with a State of its own. A pure layout wrapper (Padding, Center, a
     * SizedBox) has no behaviour to lose and is still consumed.
     */
    private boolean hasStatefulWrapper(Widget raw, Widget leaf) {
        // The SAME walk unwrapToLeaf makes, so the two cannot disagree about
        // what lies between the button and its glyph.
        Widget w = raw;
        for (int depth = 0; depth < 6 && w != null && w != leaf; depth++) {
            if (w instanceof com.codename1.flutter.StatefulWidget) {
                return true;
            }
            w = com.codename1.flutter.WidgetPreview.step(w, this);
        }
        return false;
    }

    /**
     * Looks THROUGH wrapper and composed widgets for the Text or Icon a button can actually
     * render.
     *
     * <p>A button consumes its content rather than mounting it, so anything that is not
     * literally a Text or an Icon used to fall through to {@code toString()} and be drawn as
     * a label — the gallery's back button ({@code IconButton(icon: BackButtonIcon())})
     * rendered the string "com.codename1.flutter.material.BackButtonIcon@1a2b3c", clipped by
     * the bar to a baffling "com.c". The same applied to the demo pages' options button,
     * whose icon is wrapped in a FeatureDiscovery.</p>
     *
     * <p>Composed widgets are built here to see what they produce. That is safe for the
     * icon-shaped widgets this reaches — they are pure {@code build} methods returning a
     * glyph — and it is bounded: the walk gives up after a few levels and any failure
     * returns the original widget, restoring the previous behaviour.</p>
     */
    private Widget unwrapToLeaf(Widget content) {
        Widget cur = content;
        for (int depth = 0; depth < 6 && cur != null; depth++) {
            if (cur instanceof Text || cur instanceof Icon) {
                return cur;
            }
            Widget next = com.codename1.flutter.WidgetPreview.step(cur, this);
            if (next == null) {
                return content;
            }
            cur = next;
        }
        return cur == null ? content : cur;
    }

    /**
     * Whether this renders as a bare glyph rather than a capsule with a label — true for
     * IconButton and for anything else whose trigger is an icon (a popup menu button).
     */
    protected boolean isIconButton() {
        return !(widget() instanceof ButtonBase);
    }

    /**
     * The label text consumed from a {@link Text} content child, a
     * {@code toString()} fallback for unsupported content (with a log
     * warning), or null when the content is an icon or absent.
     */
    /// The colour the consumed Text asked for, or null when it asked for none.
    private com.codename1.flutter.Color consumedLabelColor() {
        Widget c = contentWidget();
        if (!(c instanceof Text)) {
            return null;
        }
        com.codename1.flutter.TextStyle ts = ((Text) c).getStyle();
        return ts == null ? null : ts.getColor();
    }

    /// The colour the consumed Icon asked for, or null when it asked for none.
    ///
    /// The icon's own colour was never consulted -- only the button's, then the
    /// ambient IconTheme -- so an Icon that states one lost it and wore the
    /// bar's instead. The demo pages' settings glyph is the case that shows:
    /// it asks for the same near-black as the info and code glyphs beside it
    /// and came out in the primary red of the back chevron.
    private com.codename1.flutter.Color consumedIconColor() {
        Widget c = contentWidget();
        if (!(c instanceof Icon)) {
            return null;
        }
        return ((Icon) c).getColor();
    }

    public String consumedLabel() {
        Widget c = contentWidget();
        if (c instanceof Text) {
            String d = ((Text) c).getData();
            return d == null ? "" : d;
        }
        if (c == null || c instanceof Icon || isCompositeContent()) {
            return null;
        }
        try {
            Log.p("Flutter runtime: " + widget().getClass().getSimpleName()
                    + " child " + c.getClass().getSimpleName()
                    + " is neither a Text nor an Icon and could not be resolved to one;"
                    + " the button renders no label");
        } catch (Throwable t) {
            // headless: Log has no storage backend
        }
        // Deliberately NOT the widget's toString(). A Java class name is never a label
        // anyone meant to show, and printing one puts "com.codename1.flutter…" in the middle
        // of the app bar - which is exactly how this failure used to present.
        return null;
    }

    /**
     * The material glyph consumed from an {@link Icon} content child, or 0.
     */
    public char consumedIconChar() {
        Widget c = contentWidget();
        if (c instanceof Icon && ((Icon) c).getIcon() != null) {
            return ((Icon) c).getIcon().codePoint();
        }
        // ...or the icon a .icon factory put beside the label. See
        // ButtonBase.leadingIcon.
        Widget lead = leadingIconWidget();
        if (lead instanceof Icon && ((Icon) lead).getIcon() != null) {
            return ((Icon) lead).getIcon().codePoint();
        }
        return 0;
    }

    /** The icon set beside the label, looked through any wrapper. */
    private Widget leadingIconWidget() {
        if (!(widget() instanceof ButtonBase)) {
            return null;
        }
        return ((ButtonBase) widget()).getLeadingIcon();
    }

    private double iconSizeLp() {
        Widget c = contentWidget();
        if (c instanceof Icon && ((Icon) c).getSize() != null) {
            return ((Icon) c).getSize();
        }
        if (widget() instanceof IconButton && ((IconButton) widget()).getIconSize() != null) {
            return ((IconButton) widget()).getIconSize();
        }
        return DEFAULT_ICON_SIZE_LP;
    }

    private String uiid() {
        Widget w = widget();
        if (w instanceof ElevatedButton) {
            return "FlutterElevatedButton";
        }
        if (w instanceof TextButton) {
            return "FlutterTextButton";
        }
        if (w instanceof OutlinedButton) {
            return "FlutterOutlinedButton";
        }
        return "FlutterIconButton";
    }

    // ------------------------------------------------------------------
    // Component
    // ------------------------------------------------------------------

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        Button b = new Button();
        b.setUIID(uiid());
        // The listener reads the CURRENT widget config so onPressed updates
        // never require listener rewiring.
        b.addActionListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                dart.runtime.Funcs.VoidFunc0 f = onPressed();
                if (f != null) {
                    f.call();
                }
            }
        });
        apply(b);
        return b;
    }

    @Override
    protected void updateComponent(Component c) {
        apply((Button) c);
    }

    private void apply(Button b) {
        // style first: the material icon glyph derives its color from the
        // button's foreground style
        style(b);
        String label = consumedLabel();
        char glyph = consumedIconChar();
        b.setText(label == null ? "" : label);
        if (glyph != 0) {
            try {
                FontImage.setMaterialIcon(b, glyph, Dp.mm(iconSizeLp()));
            } catch (Exception err) {
                // missing icon font: layout still reserves the box
            }
        } else {
            b.setIcon(null);
        }
        b.setEnabled(onPressed() != null);
        // AFTER setEnabled, not inside style(): style() runs first, when the
        // button is still enabled by default, so a disabled-ink pass in there
        // returned early every time and the bottom row of the button demo
        // stayed the same purple as the top row.
        try {
            applyDisabledInk(b, Theme.of(this).colorScheme());
        } catch (Exception noTheme) {
            // an unthemed button keeps its base look
        }
    }

    private void style(Button b) {
        try {
            ColorScheme cs = Theme.of(this).colorScheme();
            Widget w = widget();
            com.codename1.ui.plaf.Style all = b.getAllStyles();
            // the derived theme style may use millimeter units; without
            // pinning to pixels our paddings get reinterpreted as mm (18x!)
            all.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            int hpad = (int) Math.round(Dp.px(CAPSULE_HPAD_LP));
            int vpad = (int) Math.round(Dp.px(CAPSULE_VPAD_LP));
            double shapeR = requestedRadiusLp();
            if (shapeR > 0) {
                // A style that NAMES a shape wins over the role's capsule.
                // Shrine's NEXT asks for a 7dp bevelled rectangle and came out
                // a full pill, because ButtonStyle.shape was held opaquely and
                // never read.
                applyRequestedShape(b, all, cs, w, shapeR, vpad, hpad);
            } else if (w instanceof ElevatedButton) {
                // Material 3's elevated button is a PALE face with a coloured label:
                // surfaceContainerLow behind, primary on it. This painted the primary
                // colour as the face and onPrimary as the label, which is the FILLED
                // button's scheme -- so every elevated button in the gallery came out a
                // solid block of accent where the reference has a tinted card. The
                // dialog demo's SHOW DIALOG is the clearest case: solid purple against
                // the reference's lavender with purple lettering.
                all.setPadding(vpad, vpad, hpad, hpad);
                all.setFgColor(cs.primary().rgb());
                all.setBorder(RoundBorder.create()
                        .rectangle(true)
                        .color(cs.surfaceContainerLow().rgb())
                        .shadowOpacity(40));
                clearBackground(all);
            } else if (w instanceof OutlinedButton) {
                all.setPadding(vpad, vpad, hpad, hpad);
                all.setFgColor(cs.primary().rgb());
                all.setBorder(RoundBorder.create()
                        .rectangle(true)
                        .opacity(0)
                        .stroke(Dp.mm(0.3), true)
                        .strokeColor(cs.primary().rgb())
                        .strokeOpacity(160));
                clearBackground(all);
            } else if (w instanceof TextButton) {
                all.setPadding(vpad, vpad, hpad / 2, hpad / 2);
                all.setFgColor(cs.primary().rgb());
                all.setBorder(Border.createEmpty());
                clearBackground(all);
            } else {
                // IconButton (and other glyph triggers): bare glyph
                int pad = (int) Math.round(Dp.px(8));
                all.setPadding(pad, pad, pad, pad);
                // Flutter's order: the button's own colour, then the ambient
                // IconTheme, then the default ink. The middle step was missing,
                // so an icon button in a themed app bar came out onSurface —
                // a black back arrow on a bar whose theme asks for white.
                com.codename1.flutter.Color tint = w instanceof IconButton
                        ? ((IconButton) w).getColor() : null;
                if (tint == null) {
                    tint = IconTheme.of(this).color();
                }
                all.setFgColor(tint != null ? tint.rgb() : cs.onSurface().rgb());
                all.setBorder(Border.createEmpty());
                clearBackground(all);
            }
            // The consumed Text's OWN colour, last, because it is the most specific
            // thing anyone said about this label.
            //
            // A button takes its child's string and drops the style that came with it, so
            // a label that states a colour lost it and wore the button role's instead.
            // Shrine's CANCEL says onSurface and came out in the pale pink its role gives
            // it, against the reference's near-black.
            com.codename1.flutter.Color own = consumedLabelColor();
            if (own == null) {
                own = consumedIconColor();
            }
            if (own != null) {
                all.setFgColor(own.rgb());
            }
        } catch (Exception err) {
            // styling is best-effort; the base theme look remains
        }
    }

    /**
     * Greys a disabled button's label.
     *
     * <p>{@code setEnabled(false)} was already being called, but the colours
     * above go on through {@code getAllStyles()}, which reaches the DISABLED
     * style too -- so a disabled button was painted in the same full-strength
     * primary as an enabled one and the button demo's bottom row was
     * indistinguishable from its top row. Material paints a disabled label as
     * onSurface at 38%, and Codename One's foreground is opaque, so the tint is
     * composited against the surface here.</p>
     */
    private static void applyDisabledInk(Button b, ColorScheme cs) {
        if (b.isEnabled() || cs == null || cs.onSurface() == null) {
            return;
        }
        com.codename1.flutter.Color ink = cs.onSurface();
        com.codename1.flutter.Color behind = cs.surface() != null
                ? cs.surface() : new com.codename1.flutter.Color(0xFFFFFFFFL);
        com.codename1.flutter.Color faded = com.codename1.flutter.Color.alphaBlend(
                new com.codename1.flutter.Color(
                        (0x61L << 24) | (ink.rgb() & 0xFFFFFFL)),
                new com.codename1.flutter.Color(0xFF000000L | (behind.rgb() & 0xFFFFFF)));
        b.getDisabledStyle().setFgColor(faded.rgb());
    }

    /**
     * Makes a style's background truly absent, in every state.
     *
     * <p>{@code setBgTransparency(0)} alone does not: it silences the background COLOUR
     * and leaves any background IMAGE painting. Codename One themes routinely give a
     * button a gradient image for its pressed and selected states, and
     * {@code getAllStyles()} reaches all four -- so an icon button on a dark bar flashed a
     * pale panel when touched or focused and kept it while that state held. Flutter's
     * icon button has no background of its own in any state; its feedback is the ink.</p>
     */
    private static void clearBackground(com.codename1.ui.plaf.Style all) {
        all.setBgTransparency(0);
        all.setBgImage(null);
        all.setBackgroundType(com.codename1.ui.plaf.Style.BACKGROUND_NONE);
    }

    /** Flutter's {@code kMinInteractiveDimension}. */
    private static final double MIN_INTERACTIVE_LP = 48;

    private com.codename1.flutter.Element contentChild;

    @Override
    protected void syncChildren() {
        // Mounted only in composite mode. Passing null the rest of the time is what
        // UNMOUNTS the subtree when a rebuild turns a composite child into a plain Text.
        contentChild = updateChild(contentChild,
                isCompositeContent() ? rawContentWidget() : null, 0);
    }

    @Override
    public void visitChildren(dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.Element> v) {
        if (contentChild != null) {
            v.call(contentChild);
        }
    }

    /**
     * Makes the mounted content transparent to touch so the press lands on the button
     * underneath it.
     *
     * <p>The host is one flat container and the content's components attach AFTER the
     * button's, so they sit above it and CN1 hands them the press -- a Label, which does
     * nothing with it. The button is then unreachable through its own face. Anything that
     * is a button in its own right is left alone: a nested one is entitled to the press.</p>
     */
    private void passPointerThrough(com.codename1.flutter.Element e) {
        if (e == null) {
            return;
        }
        if (e instanceof ButtonRenderElement) {
            return;
        }
        if (e instanceof RenderElement) {
            Component c = ((RenderElement) e).component();
            if (c != null && !(c instanceof Button)) {
                c.setIgnorePointerEvents(true);
            }
        }
        e.visitChildren(new dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.Element>() {
            @Override
            public void call(com.codename1.flutter.Element child) {
                passPointerThrough(child);
            }
        });
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Component c = component();
        if (c == null) {
            return constraints.smallest();
        }
        RenderElement composite = contentChild == null ? null : findRenderElement(contentChild);
        if (composite != null) {
            // The subtree measures itself and the button is exactly as big as it. The
            // minimum tap targets below belong to a button that draws its OWN label at its
            // own padding; a mounted child already carries whatever padding it was given,
            // and forcing 48lp onto it moved the compose page's account row off its rule.
            Size cs = composite.layout(constraints);
            setChildOffset(composite, 0, 0);
            passPointerThrough(contentChild);
            return constraints.constrain(cs);
        }
        Dimension d = c.getPreferredSize();
        double w = d.getWidth();
        double h = d.getHeight();
        if (isIconButton()) {
            // Flutter's IconButton carries BoxConstraints(minWidth: minHeight:
            // kMinInteractiveDimension) -- 48 logical pixels, the minimum touch target.
            // Without it an icon button was only as big as its glyph and its padding: 40
            // here, and every strip built out of them came up short. The 2D
            // transformations demo's footer is a row of two, so its bar measured 56
            // where the reference measures 64, and the board centred in the space that
            // left sat 16 device pixels low -- most of that route's difference was this.
            w = Math.max(w, Dp.px(MIN_INTERACTIVE_LP));
            h = Math.max(h, Dp.px(MIN_INTERACTIVE_LP));
        } else {
            // Material 3's button minimum is 64x40, not the 36 of the 2018 spec.
            w = Math.max(w, Dp.px(64));
            h = Math.max(h, Dp.px(40));
        }
        return constraints.constrain(new Size(w, h));
    }

    /**
     * The corner radius the widget's {@code ButtonStyle.shape} asks for, in
     * logical pixels, or 0 when it names none.
     *
     * <p>Both of Material's angular shapes are honoured the same way: Codename
     * One draws one rounded rectangle, so a bevelled border renders with
     * rounded corners of the same radius rather than mitred ones. Closer than a
     * capsule, which is what it was.</p>
     */
    private double requestedRadiusLp() {
        if (!(widget() instanceof ButtonBase)) {
            return 0;
        }
        ButtonStyle st = ((ButtonBase) widget()).getStyle();
        Object shape = st == null ? null : st.getShape();
        Object radius = null;
        if (shape instanceof com.codename1.flutter.RoundedRectangleBorder) {
            radius = ((com.codename1.flutter.RoundedRectangleBorder) shape).getBorderRadius();
        } else if (shape instanceof com.codename1.flutter.BeveledRectangleBorder) {
            radius = ((com.codename1.flutter.BeveledRectangleBorder) shape).getBorderRadius();
        }
        if (!(radius instanceof com.codename1.flutter.BorderRadius)) {
            return 0;
        }
        com.codename1.flutter.Radius tl =
                ((com.codename1.flutter.BorderRadius) radius).topLeft();
        return tl == null ? 0 : tl.x();
    }

    /** Paints the button with the radius its style asked for, in its role's colours. */
    private void applyRequestedShape(Button b, com.codename1.ui.plaf.Style all,
            ColorScheme cs, Widget w, double radiusLp, int vpad, int hpad) {
        all.setPadding(vpad, vpad, hpad, hpad);
        com.codename1.ui.plaf.RoundRectBorder border =
                com.codename1.ui.plaf.RoundRectBorder.create()
                        .useCache(false)
                        .cornerRadius(Dp.mm(radiusLp));
        if (w instanceof OutlinedButton) {
            all.setFgColor(cs.primary().rgb());
            border = border.stroke(Dp.mm(0.3), true).strokeColor(cs.primary().rgb())
                    .strokeOpacity(160);
            clearBackground(all);
        } else if (w instanceof TextButton) {
            all.setFgColor(cs.primary().rgb());
            clearBackground(all);
        } else {
            all.setFgColor(cs.primary().rgb());
            all.setBgColor(cs.surfaceContainerLow().rgb());
            all.setBgTransparency(255);
        }
        all.setBorder(border);
    }
}

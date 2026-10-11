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
    ///
    /// NOT consulted for family/size/weight/letterSpacing -- a button label is
    /// always painted in the "Flutter*Button" UIID's own baked-in font. Flutter
    /// resolves it from `ButtonStyle.textStyle ?? theme.textTheme.labelLarge`,
    /// which Shrine customises (GoogleFonts.rubikTextTheme, labelLarge at
    /// fontWeight.w500 with a small positive letterSpacing) -- CANCEL and NEXT
    /// both leave their own Text.style with no font of its own, so they fall
    /// through to it and come out in Rubik Medium with visible tracking, where
    /// this runtime paints the UIID's plain default. Reproducing that needs an
    /// ambient Theme.textTheme -> button-label font resolution this runtime does
    /// not have (the font-weight half could follow FabRenderElement.applyLabelFont's
    /// FontResolver chain; CN1's Button has no letterSpacing of its own the way
    /// WrappedLabel does -- see TextRenderElement -- so that half needs either a
    /// custom-painted label or a Button extension, not a style tweak), and it is
    /// an APP-WIDE resolution path with no screenshot coverage beyond Shrine to
    /// verify it against, so it is left alone here rather than guessed at.
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
        b.setEnabled(onPressed() != null);
        // AFTER setEnabled, not inside style(): style() runs first, when the
        // button is still enabled by default, so a disabled-ink pass in there
        // returned early every time and the bottom row of the button demo
        // stayed the same purple as the top row.
        //
        // Skipped when the widget's OWN style already names a foreground: that
        // colour was computed by the composer from the button's enabled state
        // already (CupertinoButton resolves placeholderText once onPressed is
        // null, per CupertinoTheme, not Material's ColorScheme), so fading it
        // AGAIN toward onSurface here repainted a disabled Cupertino button in
        // Material ink instead of its own theme's grey.
        if (explicitForeground() == null) {
            try {
                applyDisabledInk(b, Theme.of(this).colorScheme());
            } catch (Exception noTheme) {
                // an unthemed button keeps its base look
            }
        }
        // BEFORE the glyph is created, not after: FontImage.setMaterialIcon (below)
        // bakes a SEPARATE disabled-state icon only when the button's disabled style
        // already differs from its unselected one at the moment it is called (see its
        // own per-channel comparison). setEnabled/applyDisabledInk above is what makes
        // that style actually differ -- running the icon creation first always found
        // the two equal (disabled still wore the same ink as enabled) and kept
        // reusing the one enabled-coloured image for every state, so a disabled
        // TextButton.icon's glyph -- the Text Button demo's "+" -- stayed the full
        // enabled purple no matter the button's state, while its TEXT label (read
        // live from the Style at paint time, never baked) faded correctly. Flutter
        // paints both the icon and the label of a disabled button in the same
        // onSurface-38% foreground (ButtonStyleButton.defaultStyleOf resolves one
        // foregroundColor for the whole button, text and icon alike).
        String label = consumedLabel();
        char glyph = consumedIconChar();
        b.setText(label == null ? "" : label);
        applyLabelFont(b);
        if (glyph != 0) {
            try {
                FontImage.setMaterialIcon(b, glyph, Dp.mm(iconSizeLp()));
            } catch (Exception err) {
                // missing icon font: layout still reserves the box
            }
        } else {
            b.setIcon(null);
        }
    }

    /** The widget's own {@code ButtonStyle.foregroundColor}, or null when it names none. */
    private com.codename1.flutter.Color explicitForeground() {
        Widget w = widget();
        if (!(w instanceof ButtonBase)) {
            return null;
        }
        ButtonStyle st = ((ButtonBase) w).getStyle();
        return st == null ? null : st.getForegroundColor();
    }

    /**
     * Sizes the label from the widget's OWN {@code ButtonStyle.textStyle}, the same
     * FontResolver chain {@code FabRenderElement.applyLabelFont} uses for a FAB's label.
     *
     * <p>{@code ButtonStyle.textStyle} was held opaquely and never read.
     * CupertinoButton is the only caller that ever states one -- Flutter's
     * {@code CupertinoButton.build} paints its label in
     * {@code CupertinoTheme.of(context).textTheme.actionTextStyle} (17 logical pixels,
     * packages/flutter/lib/src/cupertino/button.dart), composed here onto
     * {@link TextButton}/{@link ElevatedButton} along with the colour and shape --
     * so every OTHER button, whose style states no textStyle of its own, is untouched
     * and keeps the "Flutter*Button" UIID's own baked-in font exactly as before.</p>
     */
    private void applyLabelFont(Button b) {
        Widget w = widget();
        ButtonStyle st = w instanceof ButtonBase ? ((ButtonBase) w).getStyle() : null;
        com.codename1.flutter.TextStyle ts = st == null ? null : st.getTextStyle();
        if (ts == null || ts.getFontSize() == null) {
            return;
        }
        com.codename1.flutter.FontWeight weight = ts.getFontWeight();
        com.codename1.ui.Font named = com.codename1.flutter.fonts.FontResolver.resolve(
                ts.getFontFamily(), weight, false);
        // A resolved face already carries its weight; synthesising bold on top of a
        // bold file doubles it, the same guard TextRenderElement applies.
        boolean weighted = named != null;
        com.codename1.ui.Font base = named;
        if (base == null) {
            base = com.codename1.flutter.fonts.FontResolver.platformFace(weight, false);
            weighted = base != null;
        }
        if (base == null) {
            base = b.getUnselectedStyle().getFont();
        }
        if (base == null) {
            base = com.codename1.ui.Font.getDefaultFont();
        }
        if (base == null) {
            return;
        }
        float sizePx = (float) Dp.px(ts.getFontSize().doubleValue());
        int fontStyle = !weighted && weight != null && weight.isBold()
                ? com.codename1.ui.Font.STYLE_BOLD : com.codename1.ui.Font.STYLE_PLAIN;
        try {
            b.getAllStyles().setFont(base.derive(sizePx, fontStyle));
        } catch (Exception err) {
            // a font that cannot derive keeps whatever size the UIID carries
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
                //
                // A styleFrom() that NAMES a colour wins over the role's own --
                // ButtonStyle.foregroundColor/backgroundColor were held on the widget
                // and never read, so CupertinoButton.filled, which composes onto this
                // branch and states both explicitly, kept coming out Material purple
                // instead of its CupertinoTheme fill.
                ButtonStyle st = ((ButtonBase) w).getStyle();
                int[] elevatedPad = resolvedPadding(st, vpad, hpad);
                all.setPadding(elevatedPad[0], elevatedPad[1], elevatedPad[2], elevatedPad[3]);
                all.setFgColor(styled(st == null ? null : st.getForegroundColor(), cs.primary()).rgb());
                RoundBorder border = RoundBorder.create()
                        .rectangle(true)
                        .color(styled(st == null ? null : st.getBackgroundColor(), cs.surfaceContainerLow()).rgb());
                if (!isElevationZero(st)) {
                    // Scaled by elevation, the same ElevationShadow mapping every other
                    // elevated surface in this runtime uses (Card, the shaped branch
                    // below) -- a flat shadowOpacity(40) regardless of elevation used to
                    // leave an elevation-8 button (Shrine's "NEXT", which actually takes
                    // the SHAPED branch below, not this one) wearing the same faint
                    // shadow as Material 3's elevation-1 resting default.
                    double elevation = resolvedElevation(st);
                    border = border.shadowOpacity(ElevationShadow.opacity(elevation))
                            .shadowSpread(Math.round(ElevationShadow.spreadMm(elevation)), true);
                }
                all.setBorder(border);
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
                ButtonStyle st = ((ButtonBase) w).getStyle();
                int[] textPad = resolvedPadding(st, vpad, hpad / 2);
                all.setPadding(textPad[0], textPad[1], textPad[2], textPad[3]);
                all.setFgColor(styled(st == null ? null : st.getForegroundColor(), cs.primary()).rgb());
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

    /** {@code named}, or the role's own default when the style asked for none. */
    private static com.codename1.flutter.Color styled(com.codename1.flutter.Color named,
            com.codename1.flutter.Color roleDefault) {
        return named != null ? named : roleDefault;
    }

    /** Whether a style's elevation is explicitly 0 -- Flutter's cue to drop the shadow. */
    private static boolean isElevationZero(ButtonStyle st) {
        return st != null && st.getElevation() != null && st.getElevation().doubleValue() == 0.0;
    }

    /**
     * {@code st.getElevation()}, or Material 3's ElevatedButton resting default (1) when
     * the style names none -- the same fallback {@link CardRenderElement} applies for an
     * unstated {@code Card.elevation}.
     */
    private static double resolvedElevation(ButtonStyle st) {
        return st != null && st.getElevation() != null ? st.getElevation().doubleValue() : 1;
    }

    /**
     * {@code st.getPadding()} in device pixels, as {top, bottom, left, right} -- or the
     * role's own {@code defaultV}/{@code defaultH} (full value on every side, the same
     * convention every un-shaped branch already used) when the style states none.
     *
     * <p>{@code ButtonStyle.padding} was held opaquely and never read, so CupertinoButton
     * -- the only caller that ever states one, Material's own {@code styleFrom} call
     * sites in this gallery never do -- composed onto a Material button role's capsule
     * padding (24lp/10lp, or TextButton's halved 12lp/10lp) instead of Flutter's own
     * {@code kCupertinoButtonPadding} (see CupertinoButton's class comment for the exact
     * numbers, which moved again in 3.35). An explicit padding REPLACES the role's own
     * outright -- TextButton's horizontal halving is a Material convention for an
     * UNSTATED padding, not something a stated one is still subject to.</p>
     *
     * <p>Only the top/left are read: every padding this runtime actually produces
     * (the Material role defaults above, Cupertino's two) is symmetric per axis, so a
     * truly asymmetric {@code EdgeInsets} is approximated rather than widening every
     * caller to four separate values for a case that does not arise here.</p>
     */
    private static int[] resolvedPadding(ButtonStyle st, int defaultV, int defaultH) {
        com.codename1.flutter.EdgeInsets p = st == null ? null : st.getPadding();
        if (p == null) {
            return new int[] {defaultV, defaultV, defaultH, defaultH};
        }
        int v = (int) Math.round(Dp.px(p.top()));
        int h = (int) Math.round(Dp.px(p.left()));
        return new int[] {v, v, h, h};
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
            // A style that names its OWN minimum size wins over the role's --
            // CupertinoButton pins kMinInteractiveDimensionCupertino (44, square),
            // not Material 3's 64x40. Nothing else in this runtime states one, so
            // every other button keeps the role default exactly as before.
            Widget w0 = widget();
            ButtonStyle st = w0 instanceof ButtonBase ? ((ButtonBase) w0).getStyle() : null;
            Double minSquare = st == null ? null : st.getMinimumSize();
            if (minSquare != null) {
                w = Math.max(w, Dp.px(minSquare.doubleValue()));
                h = Math.max(h, Dp.px(minSquare.doubleValue()));
            } else {
                // Material 3's button minimum is 64x40, not the 36 of the 2018 spec.
                w = Math.max(w, Dp.px(64));
                h = Math.max(h, Dp.px(40));
            }
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

    /**
     * Paints the button with the radius its style asked for, in its role's
     * colours -- overridden by the style's own foreground/background when it
     * names one, the same precedence {@link #style} gives the un-shaped
     * branches. Two callers state a shape: {@code CupertinoButton.filled}
     * (elevation pinned to 0 -- Flutter's CupertinoButton never casts a
     * shadow) and Shrine's login buttons, whose {@code BeveledRectangleBorder}
     * is what routes them here in the first place.
     */
    private void applyRequestedShape(Button b, com.codename1.ui.plaf.Style all,
            ColorScheme cs, Widget w, double radiusLp, int vpad, int hpad) {
        ButtonStyle st = w instanceof ButtonBase ? ((ButtonBase) w).getStyle() : null;
        int[] shapedPad = resolvedPadding(st, vpad, hpad);
        all.setPadding(shapedPad[0], shapedPad[1], shapedPad[2], shapedPad[3]);
        com.codename1.flutter.Color fg = styled(st == null ? null : st.getForegroundColor(), cs.primary());
        double elevation = resolvedElevation(st);
        com.codename1.ui.plaf.RoundRectBorder border =
                com.codename1.ui.plaf.RoundRectBorder.create()
                        .useCache(false)
                        .cornerRadius(Dp.mm(radiusLp))
                        // RoundRectBorder's OWN default shadowSpread is density-squared, not
                        // density-linear like every other measurement here: its constructor
                        // seeds the field with Display.convertToPixels(0.2f) -- a PIXEL count
                        // -- but paintBorderBackground() always re-reads the field through
                        // convertToPixels() AGAIN, as though it were millimetres (the class's
                        // own comment above the field says this is deliberate -- "borders that
                        // follow the CSS box model do not reserve any spread" is the one
                        // escape, which this border does not take -- and that fixing the
                        // shared default would reshadow every hand-written RoundRectBorder in
                        // the tree, so it stays). Left alone, that is what collapsed Shrine's
                        // "NEXT": measured under the test suite's own density bucket, the
                        // unset default comes back as 20 device px of spread subtracted from
                        // BOTH dimensions of an 80x42 button -- paintBorderBackground's
                        // createTargetComponentImage shrinks the painted fill to a 60x22 box
                        // offset by (10,10), eating very nearly half the button's own height,
                        // and at a higher-density bucket the SQUARED growth pushes the fill
                        // height negative outright: no pill left to see, just the shadow
                        // gradient at roughly the full button size with the native label (laid
                        // out independently, at the button's own correct, un-shrunk bounds)
                        // reading as if it had been pushed out from under it. The float (mm)
                        // overload stores its argument directly with no such re-reading, so
                        // stating an explicit millimetre value here -- via the SAME
                        // ElevationShadow mapping every other elevated surface in this runtime
                        // uses -- fixes the growth to linear without touching the shared class,
                        // same as CupertinoButton.filled already pins its own shape parameters
                        // locally rather than relying on a border's defaults. A flat 1mm here
                        // regardless of elevation used to leave Shrine's elevation-8 "NEXT" with
                        // the same faint spread as any other elevated button; ElevationShadow's
                        // own floor is exactly that prior flat value, so nothing below it
                        // regresses, and elevation 8 now reads as its capped 1.8mm (see
                        // ElevationShadow.spreadMm's own comment on why the cap is lower than
                        // RoundRectBorder's danger zone: this button's own capsule padding is
                        // the tighter budget, and theShadowsSpreadNeverOutgrowsTheButtonsOwnPadding
                        // enforces it).
                        .shadowSpread(ElevationShadow.spreadMm(elevation));
        if (w instanceof OutlinedButton) {
            all.setFgColor(fg.rgb());
            border = border.stroke(Dp.mm(0.3), true).strokeColor(fg.rgb())
                    .strokeOpacity(160);
            clearBackground(all);
        } else if (w instanceof TextButton) {
            all.setFgColor(fg.rgb());
            clearBackground(all);
        } else {
            all.setFgColor(fg.rgb());
            all.setBgColor(styled(st == null ? null : st.getBackgroundColor(), cs.surfaceContainerLow()).rgb());
            all.setBgTransparency(255);
            // Elevation, the same rule #style's own un-shaped ElevatedButton branch
            // applies: Flutter resolves each ButtonStyle property independently
            // (style ?? theme ?? defaults), so a style that names a SHAPE but no
            // elevation of its own still falls through to the default (non-zero)
            // rather than losing its shadow. Shrine's login states both:
            // ElevatedButton.styleFrom(elevation: 8, shape: BeveledRectangleBorder(...))
            // -- RoundRectBorder defaults shadowOpacity to 0, so leaving this unset
            // (as the shape-named path always had) painted its "NEXT" button flat
            // where the reference casts a clear drop shadow. A flat shadowOpacity(40)
            // here then left it only FAINTLY shadowed regardless of its stated
            // elevation: 8 -- ElevationShadow.opacity scales with elevation instead,
            // close to that same 40 at elevation 1 (35) and much stronger at Shrine's
            // 8 (min(255, round(20 + 8 * 15)) = 140).
            if (!isElevationZero(st)) {
                border = border.shadowOpacity(ElevationShadow.opacity(elevation));
            }
        }
        all.setBorder(border);
    }
}

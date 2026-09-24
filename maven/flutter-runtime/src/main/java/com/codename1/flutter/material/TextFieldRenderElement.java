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

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.Dimension;

import dart.runtime.Funcs;

/**
 * Leaf render box for {@link TextField}: owns a CN1
 * {@link com.codename1.ui.TextField} (UIID "FlutterTextField").
 *
 * <p>Controller sync is two-way: user edits (DataChangedListener) flow into
 * the bound {@link TextEditingController} and fire {@code onChanged};
 * {@code controller.setText/clear} push back into the component via
 * {@link #applyControllerText}. The {@code applying} guard stops the
 * programmatic push from re-entering the data-changed path.</p>
 *
 * <p>Material geometry: fills the available width, minimum height 48lp. The
 * decoration's labelText renders as the CN1 hint in M3 (hintText is the
 * fallback); a floating label is a later milestone.</p>
 */
public class TextFieldRenderElement extends RenderElement {

    /** Material minimum text-field height in logical pixels. */
    public static final double MIN_HEIGHT_LP = 48;
    /** Intrinsic width when the incoming width is unbounded. */
    public static final double DEFAULT_WIDTH_LP = 200;

    private boolean applying;
    private TextEditingController boundController;
    /// The editor itself. When the decoration carries a prefix icon this is a
    /// CHILD of the component this element owns, so every read and write has to
    /// go through here rather than through {@link #component()}.
    private com.codename1.ui.TextField field;
    /// The icon-plus-editor container, when the decoration has a prefix icon.
    /// The decoration's surface belongs to this, not to the editor inside it.
    private com.codename1.ui.Container decoratedRow;

    public TextFieldRenderElement(TextField widget) {
        super(widget);
    }

    private TextField textField() {
        return (TextField) widget();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        com.codename1.ui.TextField tf = new com.codename1.ui.TextField();
        tf.setUIID("FlutterTextField");
        tf.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                if (applying) {
                    return;
                }
                userEdited(componentText());
            }
        });
        // A Flutter text field does NOT take focus on arrival, and a focused
        // Codename One field blinks a caret. Every page with a field opened
        // showing a caret the reference does not draw -- the text-field demo,
        // Shrine's login, the sliders demo's editable value. Codename One gives
        // the form's initial focus to the first focusable component, so the
        // field hands it back once, after the form is up. A later tap focuses it
        // normally.
        tf.addPointerPressedListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                touched = true;
                // onTap was stored and never called, so a tap-driven field -- a
                // read-only date field that opens a picker -- focused and did nothing.
                Funcs.VoidFunc0 tap = textField().getOnTap();
                if (tap != null) {
                    try {
                        tap.call();
                    } catch (Throwable t) {
                        com.codename1.flutter.FlutterErrorReport.record(t);
                    }
                }
            }
        });
        tf.setDoneListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Funcs.VoidFunc1<String> f = textField().getOnSubmitted();
                if (f != null) {
                    f.call(componentText());
                }
            }
        });
        tf.addFocusListener(new com.codename1.ui.events.FocusListener() {
            @Override
            public void focusGained(Component cmp) {
                // The focus Codename One hands a field on arrival is not a Flutter
                // focus (releaseUnrequestedFocus hands it back), so it is not reported.
                if (boundFocus != null && (focusReleased || touched)) {
                    boundFocus.hostFocusChanged(true);
                }
            }

            @Override
            public void focusLost(Component cmp) {
                if (boundFocus != null) {
                    boundFocus.hostFocusChanged(false);
                }
            }
        });
        field = tf;
        Component out = decorated(tf);
        apply(tf);
        return out;
    }

    /**
     * The editor, or the editor beside its prefix icon.
     *
     * <p>{@code InputDecoration.prefixIcon} was stored and never read, so
     * Crane's search form -- four rows whose whole affordance is the glyph that
     * says what the row is for -- rendered as four bare capsules. Codename One's
     * text field has no icon slot, so the icon and the editor share a container
     * and the decoration's surface moves onto it: in Flutter the fill and the
     * border enclose the icon too.</p>
     */
    private Component decorated(com.codename1.ui.TextField tf) {
        com.codename1.flutter.widgets.Icon inside = iconAt(true);
        com.codename1.flutter.widgets.Icon outside = iconAt(false);
        com.codename1.flutter.widgets.Icon suffix = suffixIcon();
        com.codename1.flutter.widgets.Icon chosen = inside != null ? inside : outside;
        if ((chosen == null || chosen.getIcon() == null) && suffix == null) {
            return withHelper(tf, tf);
        }
        com.codename1.ui.Container row =
                new com.codename1.ui.Container(new com.codename1.ui.layouts.BorderLayout());
        if (chosen != null && chosen.getIcon() != null) {
            row.add(com.codename1.ui.layouts.BorderLayout.WEST, glyphLabel(chosen));
        }
        if (suffix != null) {
            // suffixIcon: the trailing glyph INSIDE the decoration -- the demo's
            // password field is a visibility toggle and had none at all.
            com.codename1.ui.Label glyph = glyphLabel(suffix);
            // An IconButton suffix keeps its button: a tap on the glyph runs the
            // CURRENT widget's onPressed, looked up at tap time so a rebuilt
            // callback is never stale. Drawing only the glyph left the password
            // visibility toggle showing and doing nothing.
            glyph.addPointerReleasedListener(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    IconButton b = suffixButton();
                    if (b != null && b.getOnPressed() != null) {
                        evt.consume();
                        try {
                            b.getOnPressed().call();
                        } catch (Throwable t) {
                            com.codename1.flutter.FlutterErrorReport.record(t);
                        }
                    }
                }
            });
            row.add(com.codename1.ui.layouts.BorderLayout.EAST, glyph);
        }
        row.add(com.codename1.ui.layouts.BorderLayout.CENTER, tf);
        if (inside != null) {
            // prefixIcon sits INSIDE the decoration, so the fill and the border
            // move onto the row and enclose the glyph too. A second surface
            // behind the editor would draw a filled block inside the filled one.
            row.setUIID("FlutterTextField");
            decoratedRow = row;
            tf.setUIID("Container");
            tf.getAllStyles().setBgTransparency(0);
            tf.getAllStyles().setBorder(com.codename1.ui.plaf.Border.createEmpty());
        } else {
            // `icon` sits OUTSIDE it: the field keeps its own surface and the
            // glyph stands clear of it, with Material's gap between them.
            row.setUIID("Container");
            row.getAllStyles().setBgTransparency(0);
        }
        return withHelper(tf, row);
    }

    /// The decoration's inside ({@code prefixIcon}) or outside ({@code icon})
    /// glyph, when it is an Icon -- the only form this can draw.
    private com.codename1.flutter.widgets.Icon iconAt(boolean inside) {
        InputDecoration d = textField().getDecoration();
        Widget w = d == null ? null : (inside ? d.getPrefixIcon() : d.getIcon());
        return w instanceof com.codename1.flutter.widgets.Icon
                ? (com.codename1.flutter.widgets.Icon) w : null;
    }

    /// One material glyph as a label, in the icon's colour or the ambient
    /// IconTheme's.
    private com.codename1.ui.Label glyphLabel(com.codename1.flutter.widgets.Icon icon) {
        com.codename1.ui.Label label = new com.codename1.ui.Label("", "Container");
        com.codename1.ui.plaf.Style glyphStyle =
                new com.codename1.ui.plaf.Style(label.getUnselectedStyle());
        com.codename1.flutter.Color tint = iconColor(icon);
        if (tint != null) {
            glyphStyle.setFgColor(tint.rgb());
            label.getAllStyles().setFgColor(tint.rgb());
        }
        glyphStyle.setBgTransparency(0);
        label.getAllStyles().setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        label.getAllStyles().setMargin(0, 0, 0, (int) Math.round(Dp.px(ICON_GAP_LP)));
        try {
            // Dp.mm, because FontImage sizes glyphs in MILLIMETRES. Handing it
            // logical pixels asked for a 24mm glyph and drew an icon taller than
            // the row it sits in.
            label.setIcon(com.codename1.ui.FontImage.createMaterial(
                    icon.getIcon().codePoint(), glyphStyle, Dp.mm(PREFIX_ICON_LP)));
        } catch (Exception headlessOrNoFont) {
            // the row still reserves the space
        }
        return label;
    }

    /** Material's gap between an outside icon and the field. */
    private static final double ICON_GAP_LP = 16;

    /** Material's prefix icon size. */
    private static final double PREFIX_ICON_LP = 24;

    /// The icon's own colour, or the ambient IconTheme's -- the same chain
    /// {@code IconRenderElement} follows. Without the fallback the glyph is
    /// painted in the default ink, which on Crane's purple rows is black.
    private com.codename1.flutter.Color iconColor(com.codename1.flutter.widgets.Icon icon) {
        if (icon != null && icon.getColor() != null) {
            return icon.getColor();
        }
        try {
            IconThemeData themed = IconTheme.of(this);
            return themed == null ? null : themed.color();
        } catch (Throwable noTheme) {
            return null;
        }
    }

    @Override
    protected void updateComponent(Component c) {
        if (field != null) {
            apply(field);
        }
    }

    @Override
    public void unmount() {
        super.unmount();
        bindFocus(null);
        if (boundController != null) {
            boundController.unbind(this);
            boundController = null;
        }
    }

    private void apply(com.codename1.ui.TextField tf) {
        applying = true;
        try {
            TextField w = textField();
            tf.setConstraint(w.isObscureText() ? TextArea.PASSWORD : TextArea.ANY);
            // maxLength is a limit, not only a counter: Flutter stops the edit.
            // Zero is a real limit (no characters at all); only null and Flutter's
            // negative noMaxLength mean "unlimited".
            if (w.getMaxLength() != null && w.getMaxLength().longValue() >= 0) {
                // The native cap counts UTF-16 units, and maxLength counts characters: a
                // single emoji can be eleven units. It is therefore only a generous upper
                // bound; userEdited enforces the real limit on character boundaries.
                tf.setMaxSize((int) Math.min(Integer.MAX_VALUE, w.getMaxLength().longValue() * 16));
            }
            // maxLines was stored and never read, so the demo's "Life story"
            // field -- which asks for three lines -- came up one line tall and
            // every line the user typed scrolled the one before it out of view.
            // Codename One's TextField IS a TextArea, so the rows are simply
            // its own; it just has to be told it is not single-line.
            long rows = w.getMaxLines() == null ? 1 : w.getMaxLines().longValue();
            if (w.getMinLines() != null && w.getMinLines().longValue() > rows) {
                rows = w.getMinLines().longValue();
            }
            if (rows > 1) {
                tf.setSingleLineTextArea(false);
                tf.setRows((int) rows);
            } else {
                tf.setSingleLineTextArea(true);
            }
            tf.setEditable(w.isEnabled());
            tf.setEnabled(w.isEnabled());
            InputDecoration d = w.getDecoration();
            if (d != null) {
                String hint = d.getLabelText() != null ? d.getLabelText() : d.getHintText();
                tf.setHint(hint == null ? "" : hint);
                applyTextStyle(tf, d);
                applyDecoration(decoratedRow != null ? (Component) decoratedRow : (Component) tf, d);
            }
            releaseUnrequestedFocus(tf);
            bindFocus(w.getFocusNode());
            if (counterLabel != null) {
                counterLabel.setText(counterText());
            }
            rebindController();
            if (boundController != null) {
                // rawValue, not text(): text() reads the bound component first,
                // which during this very call is still empty -- and reading it
                // REPLACES the controller's initial value with that emptiness.
                String want = boundController.rawValue();
                if (!eq(tf.getText(), want)) {
                    tf.setText(want);
                }
            }
        } finally {
            applying = false;
        }
    }

    /**
     * The type the field's own text is set in.
     *
     * <p>{@code TextField.style} was stored and never read, so a field rendered
     * at whatever size Codename One's default font happens to be.</p>
     *
     * <p>Deliberately NOT applied to the hint. Flutter builds the hint from
     * hintStyle over the theme's hintColor, not from the input's colour -- and
     * a field whose input colour is white, which is every row of Crane's search
     * form, would otherwise show a white placeholder on a light fill.</p>
     */
    private void applyTextStyle(com.codename1.ui.TextField tf, InputDecoration d) {
        applyOne(tf.getAllStyles(), inputStyle(), backdropRgb());
        if (tf.getHintLabel() != null) {
            applyOne(tf.getHintLabel().getAllStyles(), hintStyle(d), backdropRgb());
        }
    }

    /**
     * The type the input itself is set in: the theme's {@code bodyLarge}, with the
     * field's own style over it.
     *
     * <p>Only the field's own style was applied, so a field that states none -- which is
     * most of them -- fell back to whatever Codename One's default font happens to be
     * and ignored the theme entirely. The Rally study is where that shows: it sets
     * {@code bodyLarge} to a 40-point serif, and its login fields came up in small sans
     * where the reference reads them in large serif.</p>
     *
     * <p>{@code bodyLarge} rather than {@code titleMedium}: the latter is the Material 2
     * answer, which this used. Material 3 resolves a text field's style through
     * {@code _m3InputStyle}, and that is {@code textTheme.bodyLarge}.</p>
     */
    private com.codename1.flutter.TextStyle inputStyle() {
        com.codename1.flutter.TextStyle style = new com.codename1.flutter.TextStyle();
        try {
            ThemeData theme = Theme.of(this);
            if (theme != null && theme.textTheme() != null) {
                style = style.merge(theme.textTheme().bodyLarge());
            }
        } catch (Throwable noTheme) {
            // a field outside any theme still has to render
        }
        return textField().getStyle() == null ? style
                : style.merge(textField().getStyle());
    }

    /**
     * The type the placeholder is set in.
     *
     * <p>Flutter's InputDecorator builds it as {@code titleMedium} merged with
     * the field's own style, RECOLOURED with the theme's hintColor, then merged
     * with an explicit hintStyle. The recolour is the part that is easy to lose:
     * a placeholder that keeps the INPUT's colour is white on every row of
     * Crane's search form, where the input colour is white.</p>
     */
    private com.codename1.flutter.TextStyle hintStyle(InputDecoration d) {
        // A LABEL is not a placeholder, and they part company on colour.
        //
        // One string reaches the component either way -- Codename One shows one piece of
        // text in an empty field -- so this asked the theme for a hint colour whatever it
        // was showing. Flutter only does that for a hintText; a labelText resting in the
        // field takes onSurfaceVariant, which is a good deal darker. Shrine's login states
        // labelText and came out in pale grey where the reference reads it in the study's
        // brown: 52 dark pixels against the reference's 3297.
        boolean isLabel = d != null && d.getLabelText() != null;
        com.codename1.flutter.TextStyle style = new com.codename1.flutter.TextStyle();
        com.codename1.flutter.Color tint = null;
        try {
            ThemeData theme = Theme.of(this);
            style = style.merge(inputStyle());
            if (isLabel && theme.colorScheme() != null
                    && theme.colorScheme().onSurfaceVariant() != null) {
                tint = theme.colorScheme().onSurfaceVariant();
            } else {
                tint = theme.hintColor() != null ? theme.hintColor()
                        : defaultHintColor(theme.brightness());
            }
        } catch (Throwable noTheme) {
            tint = null;
        }
        if (tint != null) {
            style.color(tint);
        }
        // The ambient inputDecorationTheme's labelStyle, over the tint.
        //
        // It was never read, so a theme that states the colour of its labels did not get
        // it and every field fell back to the generic hint colour. The Rally study sets
        // one, and its login labels came out dark on a dark background where the
        // reference reads them in light grey.
        InputDecorationThemeData themed = inputDecorationTheme();
        if (themed != null && themed.getLabelStyle() != null) {
            style = style.merge(themed.getLabelStyle());
        }
        com.codename1.flutter.TextStyle explicit = d == null ? null : d.getHintStyle();
        return explicit == null ? style : style.merge(explicit);
    }

    /// Flutter's ThemeData default when the theme names no hintColor: black38
    /// on a light theme, white70 on a dark one.
    private static com.codename1.flutter.Color defaultHintColor(
            com.codename1.flutter.Brightness brightness) {
        return new com.codename1.flutter.Color(
                brightness == com.codename1.flutter.Brightness.dark
                        ? 0xB3FFFFFFL : 0x61000000L);
    }

    /**
     * Flutter's {@code InputDecoration.applyDefaults}: every field the widget
     * leaves unset falls back to the ambient inputDecorationTheme.
     *
     * <p>{@code filled} is a primitive on both sides, so "unset" and "false"
     * cannot be told apart; a theme that asks for a fill therefore wins over a
     * decoration that simply did not mention one, which is the case the studies
     * exercise. Rally names its dark fill once on the theme rather than on each
     * of its login fields, and without this they rendered as white blocks on a
     * dark page.</p>
     */
    static boolean resolveFilled(InputDecoration d, InputDecorationThemeData themed) {
        if (d != null && d.isFilled()) {
            return true;
        }
        return themed != null && themed.isFilled();
    }

    /** The fill colour, the decoration's before the theme's. */
    static com.codename1.flutter.Color resolveFill(InputDecoration d,
            InputDecorationThemeData themed) {
        if (d != null && d.getFillColor() != null) {
            return d.getFillColor();
        }
        return themed == null ? null : themed.getFillColor();
    }

    /** The content padding, the decoration's before the theme's. */
    static com.codename1.flutter.EdgeInsetsGeometry resolvePadding(InputDecoration d,
            InputDecorationThemeData themed) {
        if (d != null && d.getContentPadding() != null) {
            return d.getContentPadding();
        }
        return themed == null ? null : themed.getContentPadding();
    }

    /// The ambient {@code inputDecorationTheme}, or null when there is none.
    private InputDecorationThemeData inputDecorationTheme() {
        try {
            return Theme.of(this).inputDecorationTheme();
        } catch (Throwable noTheme) {
            return null;
        }
    }

    /** One style's size, weight and colour onto one Codename One style. */
    /// What a translucent ink is composited over: the field's own fill when it has an
    /// opaque one, else the surface the page is painted on.
    private int backdropRgb() {
        try {
            com.codename1.flutter.Color fill =
                    resolveFill(textField().getDecoration(), inputDecorationTheme());
            if (fill != null && fill.alpha() == 255) {
                return fill.rgb();
            }
            ColorScheme cs = Theme.of(this).colorScheme();
            if (cs != null && cs.surface() != null) {
                return cs.surface().rgb();
            }
        } catch (Throwable noTheme) {
            // fall through to white
        }
        return 0xFFFFFF;
    }

    private static void applyOne(com.codename1.ui.plaf.Style target,
            com.codename1.flutter.TextStyle ts, int backdropRgb) {
        if (ts == null) {
            return;
        }
        // Resolve the NAMED family first. Deriving from whatever the theme left
        // on the component is what silently did nothing: a system font does not
        // derive, so the size was dropped on the floor and the placeholder kept
        // rendering half again too tall. A bundled TrueType face does derive.
        com.codename1.ui.Font named = com.codename1.flutter.fonts.FontResolver.resolve(
                ts.fontFamily(), ts.getFontWeight(), false);
        if (ts.getFontSize() != null || ts.getFontWeight() != null || named != null) {
            com.codename1.ui.Font base = named != null ? named : target.getFont();
            // Whether the face already carries the weight; see TextRenderElement.
            boolean weighted = named != null;
            if (named == null && ts.getFontSize() != null) {
                // The theme's font here is a SYSTEM font, which does not derive,
                // so the requested size was being dropped. See
                // FontResolver.platformFace.
                com.codename1.ui.Font platform =
                        com.codename1.flutter.fonts.FontResolver.platformFace(
                                ts.getFontWeight(), false);
                if (platform != null) {
                    base = platform;
                    weighted = true;
                }
            }
            if (base == null) {
                base = com.codename1.ui.Font.getDefaultFont();
            }
            if (base != null) {
                float sizePx = ts.getFontSize() != null
                        ? (float) Dp.px(ts.getFontSize())
                        : (base.getPixelSize() > 0 ? base.getPixelSize() : base.getHeight());
                int weight = !weighted
                        && ts.getFontWeight() != null && ts.getFontWeight().isBold()
                        ? com.codename1.ui.Font.STYLE_BOLD : com.codename1.ui.Font.STYLE_PLAIN;
                try {
                    target.setFont(base.derive(sizePx, weight));
                } catch (Exception cannotDerive) {
                    // keep the base font
                }
            }
        }
        if (ts.getColor() != null) {
            // A TRANSLUCENT ink has to be composited here.
            //
            // Codename One's Style carries an opaque foreground, so taking rgb() off a
            // colour that stated an alpha paints it at FULL strength -- and Material states
            // one constantly. The compose page asks for its Subject placeholder in the
            // primary colour at half opacity and got it in solid navy, reading as a title
            // rather than as a hint.
            com.codename1.flutter.Color c = ts.getColor();
            if (c.alpha() < 255) {
                c = com.codename1.flutter.Color.alphaBlend(c,
                        new com.codename1.flutter.Color(0xFF000000L | (backdropRgb & 0xFFFFFF)));
            }
            target.setFgColor(c.rgb());
        }
    }

    /**
     * The decoration's SURFACE: its fill, its outline and its content padding.
     *
     * <p>All three were accepted and discarded, so a field that asks to be a
     * solid rounded block — which is what Crane's search form is, four purple
     * capsules on a purple back layer — rendered as the theme's default
     * outlined box on white. The decoration is the whole visual identity of a
     * Material text field; ignoring it leaves the field looking like no design
     * at all.</p>
     */
    private void applyDecoration(Component target, InputDecoration d) {
        com.codename1.ui.plaf.Style all = target.getAllStyles();
        // Flutter resolves a decoration through InputDecoration.applyDefaults:
        // each field the widget leaves unset falls back to the ambient
        // inputDecorationTheme. That theme was held opaquely and never read, so
        // Rally's login fields -- a dark fill named once on the theme rather
        // than on each field -- rendered as white blocks on a dark page.
        InputDecorationThemeData themed = inputDecorationTheme();
        if (resolveFilled(d, themed)) {
            com.codename1.flutter.Color fill = resolveFill(d, themed);
            if (fill == null) {
                try {
                    fill = Theme.of(this).colorScheme().surfaceVariant();
                } catch (Throwable ignore) {
                    fill = null;
                }
            }
            if (fill != null) {
                all.setBgColor(fill.rgb());
                all.setBgTransparency(255);
            }
        }
        // Which border, through the same fallbacks Flutter uses: the field's own, then the
        // theme's enabled border, then the theme's border.
        Object stated = d.getBorder();
        if (stated == null && themed != null) {
            stated = themed.getEnabledBorder() != null ? themed.getEnabledBorder() : themed.getBorder();
        }
        // A border that draws ITSELF wins over anything the runtime would
        // approximate. Shrine's fields chamfer their corners, and matching that
        // to the nearest rounded rectangle is what made them read as square.
        // A border class this runtime does not know draws ITSELF: it is an
        // application subclass with its own paint, and only it knows its shape
        // (Shrine's CutCornersBorder chamfers its corners).
        //
        // Class identity, not reflection: ParparVM has no Class.getMethod, and
        // asking for one does not fail at runtime -- it fails the iOS C compile
        // with an undeclared-function error, which is how two device builds
        // came to fail while the recording quietly used a stale app.
        if (paintsItself(stated)) {
            all.setBorder(new FlutterShapeBorderPainter(
                    (com.codename1.flutter.ShapeBorder) stated));
            com.codename1.ui.plaf.Border own = target.getUnselectedStyle().getBorder();
            if (own != null) {
                target.getSelectedStyle().setBorder(own);
                target.getPressedStyle().setBorder(own);
                target.getDisabledStyle().setBorder(own);
            }
            return;
        }
        int radiusPx = stated instanceof com.codename1.flutter.InputBorder
                ? outlineRadiusPx((com.codename1.flutter.InputBorder) stated) : 0;
        if (radiusPx > 0) {
            com.codename1.ui.plaf.RoundRectBorder b = com.codename1.ui.plaf.RoundRectBorder.create()
                    .cornerRadius(radiusPx / com.codename1.ui.Display.getInstance().convertToPixels(1f))
                    .shadowOpacity(0);
            // An OUTLINE, which is what the name says. It was built with strokeOpacity 0 --
            // a rounded background and no line -- so a field whose whole decoration IS its
            // outline had none: Shrine's login states a CutCornersBorder on its theme and
            // rendered as two labels loose on a white page.
            com.codename1.flutter.BorderSide side = sideOf(stated);
            if (side != null && side.color() != null && side.width() > 0) {
                b = b.strokeColor(side.color().rgb())
                        .strokeOpacity(side.color().alpha())
                        .stroke((float) Math.max(1, Math.round(
                                com.codename1.flutter.rendering.Dp.px(side.width()))), false);
            } else {
                b = b.strokeOpacity(0);
            }
            all.setBorder(b);
        } else if (stated == com.codename1.flutter.InputBorder.none) {
            all.setBorder(com.codename1.ui.plaf.Border.createEmpty());
        } else if (stated == null || stated instanceof com.codename1.flutter.UnderlineInputBorder) {
            // Flutter's DEFAULT is a rule under the field, not a box around it. Leaving the
            // Codename One theme's own border in place drew a full outline on every field
            // that states no border of its own -- Rally's login fields are a dark fill with
            // a hairline beneath, and they came out as bright rectangles.
            all.setBorder(com.codename1.ui.plaf.Border.createUnderlineBorder(
                    Math.max(1, (int) Math.round(com.codename1.flutter.rendering.Dp.px(1))),
                    underlineRgb()));
        }
        // The SAME border in every state. A Codename One theme gives a text field one
        // border unselected and a different one -- often none -- selected, which is a
        // reasonable default for a native-looking field and wrong for this one: a
        // reference text field keeps its decoration whether it has focus or not, and only
        // its caret and the highlight colour change.
        //
        // Left to the theme it looked like the box belonged to whichever field was NOT
        // being used. Measured on Shrine's login, the outline's top and bottom edges were
        // at y=1191 and y=1363 on arrival, and moved to y=979 and y=1151 -- the other
        // field entirely -- on tapping into the password box.
        com.codename1.ui.plaf.Border rest = target.getUnselectedStyle().getBorder();
        if (rest != null) {
            target.getSelectedStyle().setBorder(rest);
            target.getPressedStyle().setBorder(rest);
        }
        com.codename1.flutter.EdgeInsets pad = insetsOf(resolvePadding(d, themed));
        if (pad != null) {
            all.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            all.setPadding((int) Math.round(com.codename1.flutter.rendering.Dp.px(pad.top())),
                    (int) Math.round(com.codename1.flutter.rendering.Dp.px(pad.bottom())),
                    (int) Math.round(com.codename1.flutter.rendering.Dp.px(pad.left())),
                    (int) Math.round(com.codename1.flutter.rendering.Dp.px(pad.right())));
        }
    }

    /// The default underline's colour: onSurface at 38%, composited onto what it is drawn
    /// over, because a Codename One border colour carries no alpha.
    private int underlineRgb() {
        // Whatever the decoration's own border side states, then BLACK.
        //
        // Not onSurface at 38%: Flutter's default BorderSide is opaque black,
        // and an InputDecorator recolours it only when the field is focused,
        // disabled or in error. Fading it made every resting underline in the
        // app a mid grey where the reference draws a black rule -- measured
        // (150,150,150) against (0,0,0) on the text-field demo.
        InputDecoration d = textField().getDecoration();
        com.codename1.flutter.BorderSide side = sideOf(d == null ? null : d.getBorder());
        if (side == null) {
            InputDecorationThemeData themed = inputDecorationTheme();
            if (themed != null) {
                Object b = themed.getEnabledBorder() != null
                        ? themed.getEnabledBorder() : themed.getBorder();
                side = sideOf(b);
            }
        }
        if (side != null && side.color() != null) {
            return side.color().rgb();
        }
        return 0x000000;
    }

    /// The side an input border draws itself with, or null when it states none.
    private static com.codename1.flutter.BorderSide sideOf(Object border) {
        if (!(border instanceof com.codename1.flutter.InputBorder)) {
            return null;
        }
        com.codename1.flutter.BorderSide side =
                ((com.codename1.flutter.InputBorder) border).getBorderSide();
        // BorderSide.none is our default, and it states nothing -- Flutter's
        // default is an ordinary black side. Treat "none" as unstated.
        return side == null || side.color() == null || side.width() <= 0 ? null : side;
    }

    /** The outline's corner radius in device pixels, or 0 when it has none. */
    private static int outlineRadiusPx(com.codename1.flutter.InputBorder border) {
        if (!(border instanceof com.codename1.flutter.OutlineInputBorder)) {
            return 0;
        }
        com.codename1.flutter.BorderRadius r =
                ((com.codename1.flutter.OutlineInputBorder) border).borderRadius();
        if (r == null || r.topLeft() == null) {
            return 0;
        }
        return (int) Math.round(com.codename1.flutter.rendering.Dp.px(r.topLeft().x()));
    }

    private static com.codename1.flutter.EdgeInsets insetsOf(
            com.codename1.flutter.EdgeInsetsGeometry g) {
        return g instanceof com.codename1.flutter.EdgeInsets
                ? (com.codename1.flutter.EdgeInsets) g : null;
    }

    private void rebindController() {
        TextEditingController c = textField().getController();
        if (c != boundController) {
            if (boundController != null) {
                boundController.unbind(this);
            }
            boundController = c;
            if (c != null) {
                c.bind(this);
            }
        }
    }

    /**
     * A user edit arrived: sync the controller (which notifies its
     * listeners) and fire onChanged with the new string. Public so headless
     * tests can drive the flow without a component.
     */
    public void userEdited(String newText) {
        // Enforced here too, not only through the native field's max size: text can
        // arrive by paste or a platform editor that ignores it, and the value handed
        // on must never exceed the limit. It used to reach the controller and
        // onChanged whole, so a 14-character field accepted a 15th.
        // Counted in user-perceived characters, as Flutter's maxLength is: cutting UTF-16
        // units split an emoji at the limit and handed half a surrogate pair on.
        Long max = textField().getMaxLength();
        if (newText != null && max != null && max.longValue() >= 0
                && com.codename1.flutter.foundation.Characters.count(newText) > max.longValue()) {
            newText = com.codename1.flutter.foundation.Characters.take(newText, max.longValue());
            if (field != null && !applying) {
                applying = true;
                try {
                    field.setText(newText);
                } finally {
                    applying = false;
                }
            }
        }
        if (counterLabel != null) {
            counterLabel.setText(com.codename1.flutter.foundation.Characters.count(newText)
                    + "/" + textField().getMaxLength());
        }
        rebindController();
        if (boundController != null) {
            boundController.valueFromComponent(newText, this);
        }
        Funcs.VoidFunc1<String> f = textField().getOnChanged();
        if (f != null) {
            f.call(newText);
        }
    }

    /**
     * The component's live text, or null when headless.
     */
    String componentText() {
        return field == null ? null : field.getText();
    }

    /**
     * Push a programmatic controller value into the component (no
     * data-changed feedback loop).
     */
    void applyControllerText(String v) {
        if (field == null) {
            return;
        }
        applying = true;
        try {
            field.setText(v == null ? "" : v);
        } finally {
            applying = false;
        }
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Component c = component();
        double prefW = Dp.px(DEFAULT_WIDTH_LP);
        double prefH = Dp.px(MIN_HEIGHT_LP);
        if (c != null) {
            Dimension d = c.getPreferredSize();
            prefW = Math.max(prefW, d.getWidth());
            prefH = Math.max(prefH, d.getHeight());
        }
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : prefW;
        double h = Math.max(prefH, Dp.px(MIN_HEIGHT_LP));
        return constraints.constrain(new Size(w, h));
    }

    /** The FocusNode the widget gave this field, bridged to the editor through focusHost. */
    private com.codename1.flutter.FocusNode boundFocus;

    /**
     * Moves the real focus when the app moves its FocusNode. The node used to keep
     * a private flag only, so requestFocus neither placed the caret nor opened the
     * keyboard while hasFocus reported true.
     */
    private final com.codename1.flutter.FocusNode.Host focusHost = new com.codename1.flutter.FocusNode.Host() {
        @Override
        public void focusHost() {
            // An asked-for focus is not the arrival focus: never hand it back.
            focusReleased = true;
            focusField(2);
        }

        @Override
        public void blurHost() {
            com.codename1.ui.TextField tf = field;
            if (tf == null) {
                return;
            }
            if (tf.isEditing()) {
                tf.stopEditing();
            }
            com.codename1.ui.Form f = tf.getComponentForm();
            if (f != null && f.getFocused() == tf) {
                f.setFocused(null);
            }
        }
    };

    private void bindFocus(com.codename1.flutter.FocusNode node) {
        if (node == boundFocus) {
            return;
        }
        if (boundFocus != null) {
            boundFocus.detachHost(focusHost);
        }
        boundFocus = node;
        if (node != null) {
            node.attachHost(focusHost);
        }
    }

    /**
     * Focuses the editor and starts editing it. A field that is not on a form yet
     * -- focus requested in initState, before the page is shown -- cannot take the
     * focus, so it tries again after the current event, a bounded number of times.
     */
    private void focusField(final int retries) {
        com.codename1.ui.TextField tf = field;
        if (tf == null || !Display.isInitialized()) {
            return;
        }
        if (tf.getComponentForm() == null) {
            if (retries > 0) {
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        focusField(retries - 1);
                    }
                });
            }
            return;
        }
        tf.requestFocus();
        tf.startEditingAsync();
    }

    /** True once the user has actually pressed on this field. */
    private boolean touched;
    /** True once the arrival focus has been handed back. */
    private boolean focusReleased;

    /**
     * Hands back the focus Codename One hands a field on arrival.
     *
     * <p>Done ONCE, and never after the user has touched the field, so tapping
     * into it still focuses it and typing still works.</p>
     */
    private void releaseUnrequestedFocus(com.codename1.ui.TextField tf) {
        if (focusReleased || touched) {
            return;
        }
        try {
            com.codename1.ui.Form f = tf.getComponentForm();
            if (f == null) {
                return;
            }
            focusReleased = true;
            if (f.getFocused() == tf) {
                f.setFocused(null);
            }
        } catch (Throwable noForm) {
            // not on a form yet: the next apply tries again
        }
    }

    /** The decoration's trailing glyph, or null. */
    /** The IconButton the suffix is, or wraps, or null. */
    private IconButton suffixButton() {
        InputDecoration d = textField().getDecoration();
        Widget w = d == null ? null : d.getSuffixIcon();
        for (int depth = 0; depth < 6 && w != null; depth++) {
            if (w instanceof IconButton) {
                return (IconButton) w;
            }
            if (w instanceof com.codename1.flutter.widgets.Icon) {
                return null;
            }
            w = com.codename1.flutter.WidgetPreview.step(w, this);
        }
        return null;
    }

    private com.codename1.flutter.widgets.Icon suffixIcon() {
        InputDecoration d = textField().getDecoration();
        Widget w = d == null ? null : d.getSuffixIcon();
        // Looked THROUGH: the demo's password toggle is an IconButton, not a
        // bare Icon, and matching only the bare form left it with no glyph.
        for (int depth = 0; depth < 6 && w != null; depth++) {
            if (w instanceof com.codename1.flutter.widgets.Icon) {
                return (com.codename1.flutter.widgets.Icon) w;
            }
            if (w instanceof IconButton) {
                w = ((IconButton) w).getIcon();
                continue;
            }
            w = com.codename1.flutter.WidgetPreview.step(w, this);
        }
        return null;
    }

    /**
     * The field with Material's supporting line under it: the helper text on the
     * leading side, the character counter on the trailing side.
     *
     * <p>Both were stored and never drawn, so the demo showed neither the
     * {@code 0/14} under the phone number nor "Keep it short, this is just a
     * demo." under the life story -- and everything below sat that line's height
     * too high.</p>
     */
    private Component withHelper(com.codename1.ui.TextField tf, Component field) {
        InputDecoration d = textField().getDecoration();
        String help = d == null ? null : d.getHelperText();
        Long max = textField().getMaxLength();
        if ((help == null || help.length() == 0) && max == null) {
            return field;
        }
        com.codename1.ui.Container stack = new com.codename1.ui.Container(
                new com.codename1.ui.layouts.BoxLayout(
                        com.codename1.ui.layouts.BoxLayout.Y_AXIS));
        stack.setUIID("Container");
        stack.getAllStyles().setBgTransparency(0);
        stack.getAllStyles().setPadding(0, 0, 0, 0);
        stack.getAllStyles().setMargin(0, 0, 0, 0);
        stack.add(field);
        com.codename1.ui.Container line = new com.codename1.ui.Container(
                new com.codename1.ui.layouts.BorderLayout());
        line.setUIID("Container");
        line.getAllStyles().setBgTransparency(0);
        line.getAllStyles().setPadding(
                (int) Math.round(com.codename1.flutter.rendering.Dp.px(4)), 0, 0, 0);
        line.getAllStyles().setMargin(0, 0, 0, 0);
        if (help != null && help.length() > 0) {
            helperLabel = supportLabel(help);
            // CENTER, not WEST: a WEST cell takes its preferred width and the
            // sentence was cut off mid-word ("...just a demc").
            line.add(com.codename1.ui.layouts.BorderLayout.CENTER, helperLabel);
        }
        if (max != null) {
            counterLabel = supportLabel(counterText());
            line.add(com.codename1.ui.layouts.BorderLayout.EAST, counterLabel);
        }
        stack.add(line);
        supportLine = stack;
        return stack;
    }

    private com.codename1.ui.Label supportLabel(String text) {
        com.codename1.ui.Label l = new com.codename1.ui.Label(text == null ? "" : text);
        l.setUIID("Container");
        l.getAllStyles().setBgTransparency(0);
        l.getAllStyles().setPadding(0, 0, 0, 0);
        l.getAllStyles().setMargin(0, 0, 0, 0);
        applyOne(l.getAllStyles(), supportStyle(), backdropRgb());
        return l;
    }

    /** Material's supporting text: bodySmall in onSurfaceVariant. */
    private com.codename1.flutter.TextStyle supportStyle() {
        com.codename1.flutter.TextStyle st = new com.codename1.flutter.TextStyle();
        try {
            ThemeData t = Theme.of(this);
            if (t != null && t.textTheme() != null && t.textTheme().bodySmall() != null) {
                st = st.merge(t.textTheme().bodySmall());
            }
            ColorScheme cs = t == null ? null : t.colorScheme();
            if (cs != null && cs.onSurfaceVariant() != null) {
                st.color(cs.onSurfaceVariant());
            }
        } catch (Throwable noTheme) {
            // an unthemed field still draws its supporting line
        }
        return st;
    }

    private String counterText() {
        Long max = textField().getMaxLength();
        if (max == null) {
            return "";
        }
        String t = componentText();
        return com.codename1.flutter.foundation.Characters.count(t) + "/" + max.longValue();
    }

    private com.codename1.ui.Container supportLine;
    private com.codename1.ui.Label helperLabel;
    private com.codename1.ui.Label counterLabel;

    /**
     * Whether {@code border} is an application subclass that paints itself,
     * rather than one of the shapes this runtime draws through a Codename One
     * border.
     */
    private static boolean paintsItself(Object border) {
        if (!(border instanceof com.codename1.flutter.ShapeBorder)
                || border == com.codename1.flutter.InputBorder.none) {
            return false;
        }
        Class<?> c = border.getClass();
        return c != com.codename1.flutter.OutlineInputBorder.class
                && c != com.codename1.flutter.UnderlineInputBorder.class
                && c != com.codename1.flutter.InputBorder.class;
    }
}

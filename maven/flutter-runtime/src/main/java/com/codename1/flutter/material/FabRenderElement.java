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
import com.codename1.flutter.widgets.Icon;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.FontImage;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;

/**
 * Leaf render box for the material {@link FloatingActionButton}, owning a
 * real CN1 {@code com.codename1.components.FloatingActionButton} created via
 * {@code createFAB(char)} and positioned absolutely by the parent Scaffold
 * (no bindFabToContainer — the flat Flutter layout places it directly).
 *
 * <p>The Icon child is consumed as configuration rather than mounted as a
 * child element; {@code tooltip} is stored but not rendered in M1 (CN1 has
 * no hover tooltips on touch platforms).</p>
 */
public class FabRenderElement extends RenderElement {

    public FabRenderElement(FloatingActionButton widget) {
        super(widget);
    }

    private FloatingActionButton fab() {
        return (FloatingActionButton) widget();
    }

    private char iconChar() {
        char c = iconOf(fab().isExtended() ? fab().getIcon() : fab().getChild());
        return c == 0 ? FontImage.MATERIAL_ADD : c;
    }

    /** The glyph inside {@code w}, descending through the wrappers a theme adds. */
    private static char iconOf(com.codename1.flutter.Widget w) {
        for (int depth = 0; w != null && depth < 6; depth++) {
            if (w instanceof Icon) {
                Icon ic = (Icon) w;
                return ic.getIcon() == null ? 0 : ic.getIcon().codePoint();
            }
            if (w instanceof com.codename1.flutter.widgets.HasIcon) {
                com.codename1.flutter.IconData d =
                        ((com.codename1.flutter.widgets.HasIcon) w).iconData();
                return d == null ? 0 : d.codePoint();
            }
            if (!(w instanceof com.codename1.flutter.widgets.HasChild)) {
                return 0;
            }
            w = ((com.codename1.flutter.widgets.HasChild) w).getChild();
        }
        return 0;
    }

    /** The label of an extended FAB, or null when it has none. */
    private String labelText() {
        if (!fab().isExtended()) {
            return null;
        }
        com.codename1.flutter.Widget w = fab().getChild();
        for (int depth = 0; w != null && depth < 6; depth++) {
            if (w instanceof com.codename1.flutter.widgets.Text) {
                return ((com.codename1.flutter.widgets.Text) w).getData();
            }
            if (!(w instanceof com.codename1.flutter.widgets.HasChild)) {
                return null;
            }
            w = ((com.codename1.flutter.widgets.HasChild) w).getChild();
        }
        return null;
    }

    /** The {@code TextStyle} of the labelled Text child, or null when it states none. */
    private com.codename1.flutter.TextStyle labelTextStyle() {
        if (!fab().isExtended()) {
            return null;
        }
        com.codename1.flutter.Widget w = fab().getChild();
        for (int depth = 0; w != null && depth < 6; depth++) {
            if (w instanceof com.codename1.flutter.widgets.Text) {
                return ((com.codename1.flutter.widgets.Text) w).getStyle();
            }
            if (!(w instanceof com.codename1.flutter.widgets.HasChild)) {
                return null;
            }
            w = ((com.codename1.flutter.widgets.HasChild) w).getChild();
        }
        return null;
    }

    /**
     * Material 3's extended-FAB label default: {@code textTheme.labelLarge}, 14
     * logical pixels at {@code FontWeight.w500}
     * (packages/flutter/lib/src/material/floating_action_button.dart --
     * {@code extendedTextStyle ?? theme.textTheme.labelLarge}; see this
     * runtime's own default in {@link Typography}'s {@code labelLarge}).
     */
    private static final double LABEL_SIZE_LP = 14;

    /**
     * Sizes an extended FAB's label from the Text child's OWN style, falling back to
     * {@link #LABEL_SIZE_LP}/w500 when it states none.
     *
     * <p>The label's string was consumed by {@link #labelText()}, but the
     * {@code TextStyle} that came with it -- the gallery's "Back" chip states
     * {@code textTheme.labelLarge!.apply(color: ...)} -- was discarded along with the
     * rest of the Text widget, so the button wore the "FloatingActionButton" UIID's
     * native default instead: several times too large next to the reference. Sizing
     * follows the same face-then-size resolution {@code TextRenderElement} uses for an
     * ordinary Text, so a gallery study naming its own font family gets it here too.</p>
     */
    private void applyLabelFont(Component c, String label) {
        if (label == null || !(c instanceof com.codename1.ui.Button)) {
            return;
        }
        com.codename1.flutter.TextStyle ts = labelTextStyle();
        double sizeLp = ts != null && ts.getFontSize() != null
                ? ts.getFontSize().doubleValue() : LABEL_SIZE_LP;
        com.codename1.flutter.FontWeight weight =
                ts != null && ts.getFontWeight() != null
                        ? ts.getFontWeight() : com.codename1.flutter.FontWeight.w500;
        com.codename1.ui.Font named = com.codename1.flutter.fonts.FontResolver.resolve(
                ts == null ? null : ts.getFontFamily(), weight, false);
        // A resolved face already carries its weight; synthesising bold on top of a
        // bold file doubles it, the same guard TextRenderElement applies.
        boolean weighted = named != null;
        com.codename1.ui.Font base = named;
        if (base == null) {
            base = com.codename1.flutter.fonts.FontResolver.platformFace(weight, false);
            weighted = base != null;
        }
        if (base == null) {
            base = c.getUnselectedStyle().getFont();
        }
        if (base == null) {
            base = com.codename1.ui.Font.getDefaultFont();
        }
        if (base == null) {
            return;
        }
        float sizePx = (float) com.codename1.flutter.rendering.Dp.px(sizeLp);
        int style = !weighted && weight.isBold()
                ? com.codename1.ui.Font.STYLE_BOLD : com.codename1.ui.Font.STYLE_PLAIN;
        try {
            c.getAllStyles().setFont(base.derive(sizePx, style));
        } catch (Exception err) {
            // a font that cannot derive keeps whatever size the UIID carries
        }
    }

    @Override
    protected Component createComponent() {
        // A plain Button wearing the FAB's UIID, in BOTH forms.
        //
        // An extended fab is a labelled pill, and Codename One's
        // FloatingActionButton cannot be one: its setText stores the string for
        // the text-badge popup and only forwards it to the Button when the
        // instance IS a badge, so the label never rendered. Every study's "Back"
        // button is one of these, and each drew a bare round plus sign.
        //
        // The regular form cannot use it either, for a subtler reason: that
        // class re-installs its own circular border from styleChanged() every
        // time the background colour is set, so the Material 3 shape put on it
        // here was replaced the moment the colour followed, and the button
        // painted 96 device pixels of surface inside the 168 pixel box this
        // element had laid out for it.
        com.codename1.ui.Button b = new com.codename1.ui.Button();
        b.setUIID("FloatingActionButton");
        // The listener reads the CURRENT widget config so onPressed updates
        // never require listener rewiring.
        b.addActionListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                dart.runtime.Funcs.VoidFunc0 f = fab().getOnPressed();
                if (f != null) {
                    f.call();
                }
            }
        });
        applyStyle(b);
        // The glyph is rasterised in the style's CURRENT foreground, so it has to
        // come after the style is applied. Setting it first burned the theme's
        // default ink into the image and the button then wore a dark plus sign
        // on a purple surface where the reference has a white one.
        setGlyph(b);
        return b;
    }

    @Override
    protected void updateComponent(Component c) {
        applyStyle(c);
        setGlyph(c);
    }

    /** Material 3's default floating action button elevation. */
    private static final double FAB_ELEVATION_DEFAULT = 6;

    /**
     * Gives the FAB's border a shadow scaled by elevation -- the same
     * {@link ElevationShadow} mapping {@link CardRenderElement} and
     * {@link ButtonRenderElement} use.
     *
     * <p>The packaged theme's own "FloatingActionButton" UIID border ships with
     * {@code shadowOpacity} 0 (measured directly off {@code
     * CN1FlutterMaterialTheme.res}): Codename One's own {@code
     * com.codename1.components.FloatingActionButton} component class sets its
     * own non-zero shadow from {@code styleChanged()}, and the theme's border
     * was authored to lean on that -- but this element deliberately uses a
     * plain {@code Button} instead (see {@link #createComponent}'s comment on
     * why), so nothing ever supplied one. That is why the gallery's floating
     * "Back" chip (Shrine's login, Reply's inbox) cast no shadow at all despite
     * {@link #stripBorderShadowIfDocked} already correctly leaving an undocked
     * FAB's shadow alone -- there was never a shadow there to leave alone.</p>
     *
     * <p>Both border types this element's own shape code can produce are
     * handled, read the same way {@link #stripBorderShadowIfDocked} does (from
     * {@code c.getUnselectedStyle()}, never the {@code getAllStyles()} proxy
     * whose own getters are blank). Runs BEFORE {@link
     * #stripBorderShadowIfDocked}, which still zeroes this back out for a
     * docked FAB.</p>
     */
    private void applyElevationShadow(Component c) {
        Double stated = fab().getElevation();
        double elevation = stated != null ? stated.doubleValue() : FAB_ELEVATION_DEFAULT;
        if (elevation <= 0) {
            return;
        }
        int opacity = ElevationShadow.opacity(elevation);
        float spreadMm = ElevationShadow.spreadMm(elevation);
        com.codename1.ui.plaf.Border b = c.getUnselectedStyle().getBorder();
        if (b instanceof com.codename1.ui.plaf.RoundBorder) {
            c.getAllStyles().setBorder(((com.codename1.ui.plaf.RoundBorder) b)
                    .shadowOpacity(opacity).shadowSpread(Math.round(spreadMm), true));
        } else if (b instanceof com.codename1.ui.plaf.RoundRectBorder) {
            c.getAllStyles().setBorder(((com.codename1.ui.plaf.RoundRectBorder) b)
                    .shadowOpacity(opacity).shadowSpread(spreadMm));
        }
    }

    /**
     * Removes the drop shadow a Codename One round border draws for itself, for a
     * DOCKED floating action button only.
     *
     * <p>A docked button's elevation belongs to the Material surface under it, which is
     * what Flutter shades and what a notched bar already accounts for -- Reply's compose
     * button (centerDocked) casts no shadow of its own in the reference, and a border
     * that draws one anyway produces a second, harder ring that reads as a smudge around
     * the bar's cut-out rather than as lift. This used to run UNCONDITIONALLY, for every
     * FAB regardless of docking, which also stripped the shadow off Shrine's and Reply's
     * floating "Back" chip -- a plain {@code FloatingActionButton.extended} the gallery
     * positions itself (StudyWrapper's own Align/Padding, not a Scaffold fab slot), which
     * the reference shows with a clear soft shadow beneath it, like any other floating,
     * undocked FAB.</p>
     *
     * <p>Both round borders are handled, and only the shadow is touched: the theme still
     * owns the shape and the stroke. Read from {@code c.getUnselectedStyle()}, never from
     * {@link Component#getAllStyles()} -- that proxy's setters fan out to the four real
     * per-state styles, but its OWN getters stay the blank {@code Style} it was created
     * with (see its javadoc: "the getters for this style will be meaningless"), so a read
     * through it is always null and silently no-ops. The write goes back out through the
     * proxy, which is the one direction it works, so the zeroed shadow reaches every state
     * in a single call.</p>
     */
    private void stripBorderShadowIfDocked(Component c) {
        FloatingActionButtonLocation where = ScaffoldRenderElement.fabLocationOf(this);
        if (where == null || where.name().indexOf("Docked") < 0) {
            return;
        }
        com.codename1.ui.plaf.Border b = c.getUnselectedStyle().getBorder();
        if (b instanceof com.codename1.ui.plaf.RoundBorder) {
            c.getAllStyles().setBorder(((com.codename1.ui.plaf.RoundBorder) b).shadowOpacity(0));
        } else if (b instanceof com.codename1.ui.plaf.RoundRectBorder) {
            c.getAllStyles().setBorder(((com.codename1.ui.plaf.RoundRectBorder) b).shadowOpacity(0));
        }
    }

    /// The material glyph, in whatever foreground the style now carries.
    private void setGlyph(Component c) {
        if (c instanceof com.codename1.ui.Button) {
            FontImage.setMaterialIcon((com.codename1.ui.Button) c, iconChar(),
                    com.codename1.components.FloatingActionButton.getIconDefaultSize());
        }
    }

    /**
     * The extended (pill) form, plus the FAB's own colours.
     *
     * <p>A round FAB grows a circular border; an extended one is a capsule with
     * its label beside the glyph. Codename One's RoundBorder defaults to circle
     * growth, so a pill needs {@code rectangle(true)} — without it the label
     * either vanished or ballooned the button into a disc.</p>
     */
    /** Material 3's FAB corner radius. */
    private static final double FAB_CORNER_LP = 16;

    /**
     * Whether the widget's own {@code icon:} constructor parameter is set -- Flutter's
     * {@code FloatingActionButtonThemeData.extendedPadding}'s {@code hasChild} for an
     * extended FAB, where {@code child} IS that {@code icon} parameter (see Dart's
     * {@code FloatingActionButton.extended}, which forwards {@code icon} as {@code child}
     * to the base constructor). Distinct from {@link #iconChar()}, which always falls
     * back to a '+' glyph so this runtime never draws a blank extended FAB -- the
     * padding asymmetry below is about what the WIDGET stated, not about whatever glyph
     * ends up on screen (Reply's compose FAB states no {@code icon:} at all and embeds
     * its own inside {@code label}'s Row instead, so {@code hasChild} is false for it).
     */
    private boolean hasIconParam() {
        return fab().isExtended() && fab().getIcon() != null;
    }

    /** Flutter 3.35's {@code extendedPadding} end inset, on every extended FAB. */
    private static final double EXTENDED_PADDING_END_LP = 20;
    /** The start inset WITH an icon; without one it is the same as the end inset. */
    private static final double EXTENDED_PADDING_START_WITH_ICON_LP = 16;
    /** {@code extendedIconLabelSpacing}: the gap between the icon and the label. */
    private static final double EXTENDED_ICON_LABEL_SPACING_LP = 8;

    private void applyStyle(Component c) {
        String label = labelText();
        if (c instanceof com.codename1.ui.Button) {
            ((com.codename1.ui.Button) c).setText(label == null ? "" : label);
        }
        applyLabelFont(c, label);
        com.codename1.ui.plaf.Style all = c.getAllStyles();
        // No margin. A Codename One button carries one from its theme, and it
        // insets the surface INSIDE the box this element lays out, so a FAB
        // given Material's 56 logical pixels painted 96 device pixels of colour
        // in a 168 pixel box. Flutter's FAB has no margin of its own; the
        // Scaffold positions it.
        all.setMarginUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
        all.setMargin(0, 0, 0, 0);
        if (label != null) {
            all.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            // Material 3's extendedPadding (packages/flutter/lib/src/material/
            // floating_action_button.dart, _FABDefaultsM3.extendedPadding):
            // EdgeInsetsDirectional.only(start: 16, end: 20) with an icon, 20/20
            // without -- never the symmetric 16/16 this used to paint, which is why
            // the gallery's floating "Back" pill (which DOES state an icon:) measured
            // narrower than the reference on its leading edge.
            int start = (int) Math.round(com.codename1.flutter.rendering.Dp.px(
                    hasIconParam() ? EXTENDED_PADDING_START_WITH_ICON_LP : EXTENDED_PADDING_END_LP));
            int end = (int) Math.round(com.codename1.flutter.rendering.Dp.px(EXTENDED_PADDING_END_LP));
            int v = (int) Math.round(com.codename1.flutter.rendering.Dp.px(12));
            all.setPadding(v, v, start, end);
            // extendedIconLabelSpacing: Flutter inserts this as a SizedBox between the
            // icon and the label inside the pill's Row; this runtime draws both as one
            // CN1 Button's native icon+text, so the equivalent is the Button's own gap.
            if (c instanceof com.codename1.ui.Button) {
                ((com.codename1.ui.Button) c).setGap((int) Math.round(
                        com.codename1.flutter.rendering.Dp.px(EXTENDED_ICON_LABEL_SPACING_LP)));
            }
            // Read from getUnselectedStyle(), never from the getAllStyles() proxy `all`:
            // its OWN getBorder() always answers null (see stripBorderShadowIfDocked's
            // javadoc on the same trap), so this test -- and the label==null one right
            // below, which had the identical bug -- always missed, and an extended FAB
            // whose theme border happened to be a RoundBorder never became a pill.
            com.codename1.ui.plaf.Border b = c.getUnselectedStyle().getBorder();
            if (b instanceof com.codename1.ui.plaf.RoundBorder) {
                all.setBorder(((com.codename1.ui.plaf.RoundBorder) b).rectangle(true));
            }
        }
        if (label == null
                && !(c.getUnselectedStyle().getBorder() instanceof com.codename1.ui.plaf.RoundRectBorder)) {
            // A Material 3 FAB is a ROUNDED SQUARE -- a 16 logical pixel corner
            // radius -- not the disc Material 2 used. Codename One's own
            // FloatingActionButton takes its shape from its theme entry, so a
            // theme carrying no entry for the UIID leaves the button an ordinary
            // rectangle and a background colour fills a hard-cornered block.
            all.setBorder(com.codename1.ui.plaf.RoundRectBorder.create()
                    .cornerRadius((float) (com.codename1.flutter.rendering.Dp.px(FAB_CORNER_LP)
                            / com.codename1.ui.Display.getInstance().convertToPixels(1f)))
                    .strokeOpacity(0)
                    .shadowOpacity(0));
        }
        applyElevationShadow(c);
        stripBorderShadowIfDocked(c);
        com.codename1.flutter.Color bg = fab().getBackgroundColor();
        com.codename1.flutter.Color fgDefault = null;
        if (bg == null) {
            // Material 3's default FAB colours are primaryContainer over
            // onPrimaryContainer. Resolving neither left the button wearing
            // whatever the Codename One theme happened to carry, which is how
            // the bottom-app-bar demo drew a pale lavender button with a purple
            // glyph where the reference is purple with a white one -- the two
            // roles exactly inverted.
            try {
                ColorScheme scheme = Theme.of(this).colorScheme();
                if (scheme != null) {
                    bg = scheme.primaryContainer();
                    fgDefault = scheme.onPrimaryContainer();
                }
            } catch (Throwable ignore) {
                // no ambient theme
            }
        }
        if (bg != null) {
            com.codename1.ui.plaf.Border b = all.getBorder();
            if (b instanceof com.codename1.ui.plaf.RoundBorder) {
                all.setBorder(((com.codename1.ui.plaf.RoundBorder) b).color(bg.rgb()));
            } else {
                ThemeDataAdapter.paintColor(all, bg);
            }
        }
        com.codename1.flutter.Color fg = fab().getForegroundColor();
        if (fg == null) {
            fg = fgDefault;
        }
        if (fg != null) {
            all.setFgColor(fg.rgb());
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Component c = component();
        if (c == null) {
            return constraints.smallest();
        }
        return constraints.constrain(
                materialSize(fab().isExtended(), c.getPreferredSize().getWidth()));
    }

    /**
     * The size Material gives a floating action button, in device pixels.
     *
     * <p>A regular one is a FIXED square. Taking the component's preferred size
     * instead made it as small as its glyph plus whatever padding the theme in
     * force happened to carry -- 83 device pixels against the reference's 168,
     * less than half. An extended one is a capsule: the height is fixed too and
     * only the width follows the label, and never below the minimum.</p>
     */
    static Size materialSize(boolean extended, double preferredWidth) {
        if (!extended) {
            double side = com.codename1.flutter.rendering.Dp.px(FAB_SIZE_LP);
            return new Size(side, side);
        }
        return new Size(
                Math.max(com.codename1.flutter.rendering.Dp.px(EXTENDED_MIN_WIDTH_LP),
                        preferredWidth),
                com.codename1.flutter.rendering.Dp.px(EXTENDED_HEIGHT_LP));
    }

    /** Material's regular FAB is this many logical pixels on a side. */
    public static final double FAB_SIZE_LP = 56;
    /** The extended form's fixed height -- 56 in Material 3, not M2's 48. */
    public static final double EXTENDED_HEIGHT_LP = 56;
    /** The extended form's minimum width. */
    public static final double EXTENDED_MIN_WIDTH_LP = 80;
}

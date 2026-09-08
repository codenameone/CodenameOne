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

    @Override
    protected Component createComponent() {
        // An EXTENDED fab is a labelled pill, and Codename One's
        // FloatingActionButton cannot be one: its setText stores the string for
        // the text-badge popup and only forwards it to the Button when the
        // instance IS a badge, so the label never rendered. The extended form
        // is therefore a plain Button wearing the same UIID -- same surface,
        // same elevation, and a label that appears. Every study's "Back" button
        // is one of these, and each drew a bare round plus sign.
        com.codename1.ui.Button b;
        if (fab().isExtended()) {
            b = new com.codename1.ui.Button();
            b.setUIID("FloatingActionButton");
            FontImage.setMaterialIcon(b, iconChar(),
                    com.codename1.components.FloatingActionButton.getIconDefaultSize());
        } else {
            b = com.codename1.components.FloatingActionButton.createFAB(iconChar());
        }
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
        return b;
    }

    @Override
    protected void updateComponent(Component c) {
        if (c instanceof com.codename1.ui.Button) {
            FontImage.setMaterialIcon((com.codename1.ui.Button) c, iconChar(),
                    com.codename1.components.FloatingActionButton.getIconDefaultSize());
        }
        applyStyle(c);
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

    private void applyStyle(Component c) {
        String label = labelText();
        if (c instanceof com.codename1.ui.Button) {
            ((com.codename1.ui.Button) c).setText(label == null ? "" : label);
        }
        com.codename1.ui.plaf.Style all = c.getAllStyles();
        if (label != null) {
            all.setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS);
            int h = (int) Math.round(com.codename1.flutter.rendering.Dp.px(16));
            int v = (int) Math.round(com.codename1.flutter.rendering.Dp.px(12));
            all.setPadding(v, v, h, h);
            com.codename1.ui.plaf.Border b = all.getBorder();
            if (b instanceof com.codename1.ui.plaf.RoundBorder) {
                all.setBorder(((com.codename1.ui.plaf.RoundBorder) b).rectangle(true));
            }
        }
        if (label == null && !(all.getBorder() instanceof com.codename1.ui.plaf.RoundRectBorder)) {
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
        Dimension d = c.getPreferredSize();
        return constraints.constrain(new Size(d.getWidth(), d.getHeight()));
    }
}

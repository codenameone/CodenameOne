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
        Widget w = widget();
        Widget content = w instanceof ButtonBase
                ? ((ButtonBase) w).getChild()
                : ((IconButton) w).getIcon();
        return unwrapToLeaf(content);
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
        for (int depth = 0; depth < 4 && cur != null; depth++) {
            if (cur instanceof Text || cur instanceof Icon) {
                return cur;
            }
            try {
                if (cur instanceof com.codename1.flutter.widgets.HasChild) {
                    cur = ((com.codename1.flutter.widgets.HasChild) cur).getChild();
                } else if (cur instanceof com.codename1.flutter.StatelessWidget) {
                    cur = ((com.codename1.flutter.StatelessWidget) cur).build(this);
                } else {
                    return content;
                }
            } catch (Throwable t) {
                return content;
            }
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
    public String consumedLabel() {
        Widget c = contentWidget();
        if (c instanceof Text) {
            String d = ((Text) c).getData();
            return d == null ? "" : d;
        }
        if (c == null || c instanceof Icon) {
            return null;
        }
        try {
            Log.p("Flutter runtime: " + widget().getClass().getSimpleName()
                    + " child " + c.getClass().getSimpleName()
                    + " is not a Text or Icon; using its toString() as the label");
        } catch (Throwable t) {
            // headless: Log has no storage backend
        }
        return String.valueOf(c);
    }

    /**
     * The material glyph consumed from an {@link Icon} content child, or 0.
     */
    public char consumedIconChar() {
        Widget c = contentWidget();
        if (c instanceof Icon && ((Icon) c).getIcon() != null) {
            return ((Icon) c).getIcon().codePoint();
        }
        return 0;
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
            if (w instanceof ElevatedButton) {
                all.setPadding(vpad, vpad, hpad, hpad);
                all.setFgColor(cs.onPrimary().rgb());
                all.setBorder(RoundBorder.create()
                        .rectangle(true)
                        .color(cs.primary().rgb())
                        .shadowOpacity(40));
                all.setBgTransparency(0);
            } else if (w instanceof OutlinedButton) {
                all.setPadding(vpad, vpad, hpad, hpad);
                all.setFgColor(cs.primary().rgb());
                all.setBorder(RoundBorder.create()
                        .rectangle(true)
                        .opacity(0)
                        .stroke(Dp.mm(0.3), true)
                        .strokeColor(cs.primary().rgb())
                        .strokeOpacity(160));
                all.setBgTransparency(0);
            } else if (w instanceof TextButton) {
                all.setPadding(vpad, vpad, hpad / 2, hpad / 2);
                all.setFgColor(cs.primary().rgb());
                all.setBorder(Border.createEmpty());
                all.setBgTransparency(0);
            } else {
                // IconButton (and other glyph triggers): bare glyph
                int pad = (int) Math.round(Dp.px(8));
                all.setPadding(pad, pad, pad, pad);
                com.codename1.flutter.Color tint = w instanceof IconButton
                        ? ((IconButton) w).getColor() : null;
                all.setFgColor(tint != null ? tint.rgb() : cs.onSurface().rgb());
                all.setBorder(Border.createEmpty());
                all.setBgTransparency(0);
            }
        } catch (Exception err) {
            // styling is best-effort; the base theme look remains
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        Component c = component();
        if (c == null) {
            return constraints.smallest();
        }
        Dimension d = c.getPreferredSize();
        double w = d.getWidth();
        double h = d.getHeight();
        if (!isIconButton()) {
            // Material spec: text buttons have a 64x36lp minimum tap target
            w = Math.max(w, Dp.px(64));
            h = Math.max(h, Dp.px(36));
        }
        return constraints.constrain(new Size(w, h));
    }
}

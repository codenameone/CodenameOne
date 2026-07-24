package com.codename1.flutter.material;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.SingleChildRenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;

/**
 * Render element for {@link AppBar} with two modes:
 * <ul>
 *   <li><b>Toolbar mode</b> (host is a root Scaffold's toolbar title host):
 *       owns no strip component; sizes to the title subtree and applies the
 *       backgroundColor to the CN1 Toolbar's style. The Form's Toolbar does
 *       the actual bar chrome.</li>
 *   <li><b>Strip mode</b> (embedded/non-root): owns a background Container
 *       (UIID "FlutterAppBar") covering a 56lp-high strip, with the title
 *       laid out inside (16lp leading inset, or centered when centerTitle).</li>
 * </ul>
 */
public class AppBarRenderElement extends SingleChildRenderElement {

    /** Material toolbar height in logical pixels. */
    public static final double TOOLBAR_HEIGHT_LP = 56;
    private static final double TITLE_INSET_LP = 16;

    public AppBarRenderElement(AppBar widget) {
        super(widget);
    }

    private AppBar appBar() {
        return (AppBar) widget();
    }

    private boolean toolbarMode() {
        return host() != null && host().isToolbarTitleHost();
    }

    @Override
    protected Widget childWidget() {
        return appBar().getTitle();
    }

    @Override
    protected Component createComponent() {
        if (toolbarMode()) {
            applyToolbarStyle();
            return null;
        }
        Container strip = new Container();
        strip.setUIID("FlutterAppBar");
        strip.getAllStyles().setPadding(0, 0, 0, 0);
        strip.getAllStyles().setMargin(0, 0, 0, 0);
        applyStripStyle(strip);
        return strip;
    }

    @Override
    protected void updateComponent(Component c) {
        applyStripStyle(c);
    }

    @Override
    public void update(Widget newWidget) {
        super.update(newWidget);
        if (toolbarMode()) {
            applyToolbarStyle();
        }
    }

    /**
     * The bar background actually in effect: the explicit
     * {@code AppBar.backgroundColor} when given, else the M3 ThemeData
     * default — colorScheme.surface (matching Flutter's Material 3 AppBar,
     * which sits on the surface with an elevation tint rather than a
     * saturated fill).
     */
    private com.codename1.flutter.Color effectiveBackground() {
        if (appBar().getBackgroundColor() != null) {
            return appBar().getBackgroundColor();
        }
        try {
            return Theme.of(this).colorScheme().surface();
        } catch (Throwable t) {
            return null;
        }
    }

    private void applyStripStyle(Component strip) {
        com.codename1.flutter.Color bg = effectiveBackground();
        if (bg != null) {
            ThemeDataAdapter.paintSolid(strip.getAllStyles(), bg.rgb());
        }
    }

    /**
     * Root mode: style the CN1 Toolbar itself. BACKGROUND_NONE (inside
     * paintSolid) is essential — the Material base theme's Toolbar style can
     * carry a background image/gradient that paints OVER a bare setBgColor,
     * which is why AppBar.backgroundColor used to be ignored here. Applied
     * to getAllStyles so focus/scroll state changes can't swap the color
     * back (state-metric invariance).
     */
    private void applyToolbarStyle() {
        com.codename1.ui.Toolbar tb = host() == null ? null : host().toolbar();
        if (tb == null) {
            return;
        }
        com.codename1.flutter.Color bg = effectiveBackground();
        if (bg != null) {
            ThemeDataAdapter.paintSolid(tb.getAllStyles(), bg.rgb());
        }
    }

    /**
     * Theme change: recompute the ThemeData-driven default background in
     * BOTH modes (the base class only re-applies the strip component's
     * config; the Toolbar is not our component).
     */
    @Override
    public void themeChanged() {
        if (toolbarMode()) {
            applyToolbarStyle();
        }
        super.themeChanged();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RenderElement title = renderChild();
        if (toolbarMode()) {
            // Size to the title; the Toolbar provides the bar itself.
            if (title == null) {
                return constraints.smallest();
            }
            Size ts = title.layout(constraints.loosen());
            setChildOffset(title, 0, 0);
            return constraints.constrain(ts);
        }
        double height = constraints.constrainHeight(Dp.px(TOOLBAR_HEIGHT_LP));
        double inset = Dp.px(TITLE_INSET_LP);
        double width;
        if (constraints.hasBoundedWidth()) {
            width = constraints.maxWidth();
        } else {
            width = inset * 2;
        }
        if (title != null) {
            double avail = Math.max(0, width - inset * 2);
            Size ts = title.layout(BoxConstraints.loose(avail, height));
            if (!constraints.hasBoundedWidth()) {
                width = ts.width() + inset * 2;
            }
            double tx = appBar().getCenterTitle()
                    ? (width - ts.width()) / 2
                    : inset;
            setChildOffset(title, tx, (height - ts.height()) / 2);
        }
        return constraints.constrain(new Size(width, height));
    }
}

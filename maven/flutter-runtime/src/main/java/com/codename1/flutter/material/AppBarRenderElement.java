package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Container;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Render element for {@link AppBar}: Flutter's {@code NavigationToolbar} arrangement of
 * <b>leading, title and actions</b> across the bar, in two modes:
 * <ul>
 *   <li><b>Toolbar mode</b> (host is a root Scaffold's toolbar title host):
 *       owns no strip component and applies the backgroundColor to the CN1 Toolbar's
 *       style. The Form's Toolbar draws the bar chrome; the row is laid out inside the
 *       Toolbar's title component, which spans its full width.</li>
 *   <li><b>Strip mode</b> (embedded/non-root): owns a background Container
 *       (UIID "FlutterAppBar") covering a 56lp-high strip.</li>
 * </ul>
 *
 * <p><b>Only the title used to be laid out.</b> {@code leading} and {@code actions} were
 * read into the widget and then dropped on the floor, which is a quiet way to lose a lot
 * of an app: every gallery demo page carries its <i>back button</i> as
 * {@code AppBar.leading} and its options/info/code/documentation buttons as
 * {@code actions}, so each one rendered as a bare title with no way out and no controls —
 * a page that looked like a stub of itself.</p>
 */
public class AppBarRenderElement extends RenderElement {

    /** Material toolbar height in logical pixels. */
    public static final double TOOLBAR_HEIGHT_LP = 56;
    /** NavigationToolbar.kMiddleSpacing — the gap either side of the title. */
    private static final double TITLE_SPACING_LP = 16;
    /** Flutter's _kLeadingWidth: the leading slot is a square the height of the bar. */
    private static final double LEADING_WIDTH_LP = 56;

    private List<Element> children = new ArrayList<Element>();
    /** Index into {@link #children} of each slot, or -1 when absent. */
    private int leadingIndex = -1;
    private int titleIndex = -1;
    private int firstActionIndex = -1;

    public AppBarRenderElement(AppBar widget) {
        super(widget);
    }

    private AppBar appBar() {
        return (AppBar) widget();
    }

    private boolean toolbarMode() {
        return host() != null && host().isToolbarTitleHost();
    }

    // ------------------------------------------------------------------
    // Children: leading, title, actions - in that order
    // ------------------------------------------------------------------

    @Override
    protected void syncChildren() {
        List<Widget> slots = new ArrayList<Widget>();
        leadingIndex = -1;
        titleIndex = -1;
        firstActionIndex = -1;

        Widget leading = effectiveLeading();
        if (leading != null) {
            leadingIndex = slots.size();
            slots.add(leading);
        }
        if (appBar().getTitle() != null) {
            titleIndex = slots.size();
            slots.add(appBar().getTitle());
        }
        if (appBar().getActions() != null) {
            for (Widget a : appBar().getActions()) {
                if (a == null) {
                    continue;
                }
                if (firstActionIndex < 0) {
                    firstActionIndex = slots.size();
                }
                slots.add(a);
            }
        }
        children = updateChildren(children, slots);
    }

    /**
     * The leading widget, or the back button Flutter would imply in its place.
     *
     * <p>{@code automaticallyImplyLeading} defaults to true, and a route that can be popped
     * gets a back button for free — which is how most Flutter pages get theirs. Without it
     * a Scaffold that never names a leading is a page with no way back.</p>
     */
    private Widget effectiveLeading() {
        if (appBar().getLeading() != null) {
            return appBar().getLeading();
        }
        if (!appBar().getAutomaticallyImplyLeading()) {
            return null;
        }
        return canPop() ? new BackButton() : null;
    }

    private boolean canPop() {
        try {
            com.codename1.flutter.navigation.NavigatorState nav =
                    com.codename1.flutter.navigation.Navigator.of(this, Boolean.FALSE);
            return nav != null && nav.canPop();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        for (Element c : children) {
            if (c != null) {
                visitor.call(c);
            }
        }
    }

    private RenderElement renderAt(int index) {
        if (index < 0 || index >= children.size()) {
            return null;
        }
        return findRenderElement(children.get(index));
    }

    // ------------------------------------------------------------------
    // Component and styling
    // ------------------------------------------------------------------

    @Override
    protected Component createComponent() {
        if (toolbarMode()) {
            applyToolbarStyle();
            return null;
        }
        if (!com.codename1.ui.Display.isInitialized()) {
            // headless unit tests: the layout is exercised without any CN1 components
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
     * The bar background actually in effect — Flutter's resolution order:
     * {@code AppBar.backgroundColor}, then the ambient {@code AppBarTheme},
     * then the M3 default of colorScheme.surface (an AppBar sitting on the
     * surface with an elevation tint rather than a saturated fill).
     *
     * <p>The AppBarTheme step was missing, and the gallery leans on it hard: it
     * themes every demo page's bar with {@code AppBarTheme(color: primary)} and
     * its own chrome with {@code AppBarTheme(backgroundColor: background)}, so
     * both came out the same default grey and the demos lost their purple bar.</p>
     */
    private com.codename1.flutter.Color effectiveBackground() {
        if (appBar().getBackgroundColor() != null) {
            return appBar().getBackgroundColor();
        }
        try {
            ThemeData theme = Theme.of(this);
            AppBarTheme bar = theme.appBarTheme();
            if (bar != null && bar.backgroundColor() != null) {
                return bar.backgroundColor();
            }
            return theme.colorScheme().surface();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * The colour for the bar's title and icons: {@code AppBar.foregroundColor},
     * then the ambient {@code AppBarTheme}'s icon theme, then null for the
     * default. The gallery pairs its purple bar with white icons this way.
     */
    com.codename1.flutter.Color effectiveForeground() {
        if (appBar().getForegroundColor() != null) {
            return appBar().getForegroundColor();
        }
        try {
            AppBarTheme bar = Theme.of(this).appBarTheme();
            if (bar != null && bar.iconTheme() != null) {
                return bar.iconTheme().color();
            }
        } catch (Throwable t) {
            // no ambient theme
        }
        return null;
    }

    private void applyStripStyle(Component strip) {
        com.codename1.flutter.Color bg = effectiveBackground();
        if (bg != null) {
            ThemeDataAdapter.paintColor(strip.getAllStyles(), bg);
        }
        // The default ink for anything in the bar that does not pick its own.
        // A purple bar with black-by-default glyphs on it is unreadable, and
        // that is exactly what the demo pages' AppBarTheme asks for.
        com.codename1.flutter.Color fg = effectiveForeground();
        if (fg != null) {
            strip.getAllStyles().setFgColor(fg.value() & 0xFFFFFF);
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
            ThemeDataAdapter.paintColor(tb.getAllStyles(), bg);
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

    // ------------------------------------------------------------------
    // Layout - Flutter's NavigationToolbar
    // ------------------------------------------------------------------

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double barHeight = barHeight(constraints);
        double spacing = Dp.px(titleSpacing());

        // Leading first: it fixes where the title may start.
        RenderElement leading = renderAt(leadingIndex);
        double leadingWidth = 0;
        Size leadingSize = null;
        if (leading != null) {
            leadingSize = leading.layout(BoxConstraints.loose(
                    Math.min(Dp.px(LEADING_WIDTH_LP), maxWidthFor(constraints)), barHeight));
            leadingWidth = leadingSize.width();
        }

        // Actions next, packed at the end. Each is laid out against what is still free, so
        // a long row degrades by shrinking rather than by overflowing the bar.
        List<RenderElement> actions = new ArrayList<RenderElement>();
        List<Size> actionSizes = new ArrayList<Size>();
        double actionsWidth = 0;
        if (firstActionIndex >= 0) {
            for (int i = firstActionIndex; i < children.size(); i++) {
                RenderElement a = renderAt(i);
                if (a == null) {
                    continue;
                }
                double free = Math.max(0, maxWidthFor(constraints) - leadingWidth - actionsWidth);
                Size as = a.layout(BoxConstraints.loose(free, barHeight));
                actions.add(a);
                actionSizes.add(as);
                actionsWidth += as.width();
            }
        }

        RenderElement title = renderAt(titleIndex);
        Size titleSize = null;

        double width;
        if (constraints.hasBoundedWidth()) {
            width = constraints.maxWidth();
            if (title != null) {
                double avail = Math.max(0,
                        width - leadingWidth - actionsWidth - spacing * 2);
                titleSize = title.layout(BoxConstraints.loose(avail, barHeight));
            }
        } else {
            // Unbounded (the dry pass that yields a preferred size): the bar is as wide as
            // its contents, so the title is measured free and everything is summed.
            if (title != null) {
                titleSize = title.layout(BoxConstraints.loose(Double.POSITIVE_INFINITY, barHeight));
            }
            width = leadingWidth + actionsWidth
                    + (titleSize == null ? 0 : titleSize.width() + spacing * 2);
        }

        // Place: leading at the start, actions flush to the end, title between.
        if (leading != null) {
            setChildOffset(leading, 0, centreY(leadingSize, barHeight));
        }
        double actionX = width - actionsWidth;
        for (int i = 0; i < actions.size(); i++) {
            Size as = actionSizes.get(i);
            setChildOffset(actions.get(i), actionX, centreY(as, barHeight));
            actionX += as.width();
        }
        if (title != null) {
            double tx;
            if (appBar().getCenterTitle()) {
                tx = (width - titleSize.width()) / 2;
                // A centred title still may not slide under the leading or the actions.
                tx = Math.max(leadingWidth + spacing,
                        Math.min(tx, width - actionsWidth - spacing - titleSize.width()));
                tx = Math.max(0, tx);
            } else {
                tx = leadingWidth + spacing;
            }
            setChildOffset(title, tx, centreY(titleSize, barHeight));
        }

        return constraints.constrain(new Size(width, barHeight));
    }

    /** Vertical centring of one slot within the bar. */
    private static double centreY(Size child, double barHeight) {
        if (child == null) {
            return 0;
        }
        return Math.max(0, (barHeight - child.height()) / 2);
    }

    private static double maxWidthFor(BoxConstraints constraints) {
        return constraints.hasBoundedWidth() ? constraints.maxWidth() : Double.POSITIVE_INFINITY;
    }

    /**
     * The bar's height. In toolbar mode the CN1 Toolbar owns the chrome and has already
     * been given a height, so the row fills whatever box it was handed rather than forcing
     * a second 56lp on top of it.
     */
    private double barHeight(BoxConstraints constraints) {
        double preferred = Dp.px(appBar().getToolbarHeight() == null
                ? TOOLBAR_HEIGHT_LP : appBar().getToolbarHeight().doubleValue());
        if (toolbarMode() && constraints.hasBoundedHeight() && constraints.maxHeight() > 0) {
            return constraints.maxHeight();
        }
        return constraints.constrainHeight(preferred);
    }

    private double titleSpacing() {
        Double s = appBar().getTitleSpacing();
        return s == null ? TITLE_SPACING_LP : s.doubleValue();
    }
}

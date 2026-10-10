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
    private int flexibleSpaceIndex = -1;
    private int leadingIndex = -1;
    private int titleIndex = -1;
    private int firstActionIndex = -1;
    private int bottomIndex = -1;

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
        flexibleSpaceIndex = -1;
        leadingIndex = -1;
        titleIndex = -1;
        firstActionIndex = -1;
        bottomIndex = -1;

        // First, so it mounts and paints underneath the row: flexibleSpace is
        // Flutter's background layer for the bar, not a fourth slot in it.
        if (appBar().getFlexibleSpace() != null) {
            flexibleSpaceIndex = slots.size();
            slots.add(appBar().getFlexibleSpace());
        }

        Widget leading = effectiveLeading();
        if (leading != null) {
            leadingIndex = slots.size();
            slots.add(styled(leading, false));
        }
        if (appBar().getTitle() != null) {
            titleIndex = slots.size();
            slots.add(styled(appBar().getTitle(), true));
        }
        if (appBar().getActions() != null) {
            for (Widget a : appBar().getActions()) {
                if (a == null) {
                    continue;
                }
                if (firstActionIndex < 0) {
                    firstActionIndex = slots.size();
                }
                slots.add(styled(a, false));
            }
        }
        // Last, and below the row: Flutter's AppBar.bottom is a band under the
        // toolbar, not a slot in it. It was read into the widget and dropped,
        // which is how the colors demo lost its whole palette tab bar and the
        // tabs demo lost its tabs.
        if (appBar().getBottom() != null) {
            bottomIndex = slots.size();
            slots.add(appBar().getBottom());
        }
        children = updateChildren(children, slots);
    }

    /**
     * Wraps a toolbar slot in the bar's ambient icon theme and text style.
     *
     * <p>This is how Flutter tints an app bar's contents, and why it works
     * without every {@code Text} and {@code Icon} naming a colour: the bar
     * publishes one {@code IconTheme} and one {@code DefaultTextStyle} and the
     * subtree reads them. Setting a foreground on the strip container instead
     * does nothing, because a Codename One style does not inherit its ink from
     * an ancestor — which is why themed bars rendered black glyphs on purple.</p>
     *
     * <p>Deliberately NOT applied to {@code flexibleSpace}: in Flutter the
     * flexible space sits in a Stack beneath the toolbar, outside these two
     * wrappers, so it keeps whatever style its own subtree establishes. Crane's
     * bar depends on that — its tab labels are white by its own theme.</p>
     */
    private Widget styled(Widget slot, boolean isTitle) {
        Widget out = slot;
        com.codename1.flutter.TextStyle text = isTitle ? titleTextStyle() : toolbarTextStyle();
        if (text != null) {
            out = com.codename1.flutter.widgets.DefaultTextStyle.wrap(text, out);
        }
        if (!isTitle) {
            IconThemeData icons = effectiveIconTheme();
            if (icons != null) {
                IconTheme t = new IconTheme();
                t.data(icons);
                t.child(out);
                out = t;
            }
        }
        return out;
    }

    /**
     * The icon styling for the bar's glyphs: {@code AppBar.iconTheme}, then the
     * ambient {@code AppBarTheme}'s, then the bar's foreground colour.
     *
     * <p>One-directional on purpose. An {@code AppBarTheme.iconTheme} colours
     * the ICONS and nothing else — reading it as the bar's foreground turns the
     * title white too, which is wrong wherever a theme tints its glyphs against
     * a bar whose title is meant to stay default ink. The gallery's demo pages
     * are exactly that case: white icons, black title, on purple.</p>
     */
    private IconThemeData effectiveIconTheme() {
        if (appBar().getIconTheme() != null) {
            return appBar().getIconTheme();
        }
        try {
            AppBarTheme bar = Theme.of(this).appBarTheme();
            if (bar != null && bar.iconTheme() != null) {
                return bar.iconTheme();
            }
        } catch (Throwable t) {
            // no ambient theme
        }
        com.codename1.flutter.Color fg = effectiveForeground();
        if (fg == null) {
            return null;
        }
        IconThemeData d = new IconThemeData();
        d.color(fg);
        return d;
    }

    /** The style for the title: {@code AppBarTheme.titleTextStyle}, tinted with the foreground. */
    private com.codename1.flutter.TextStyle titleTextStyle() {
        if (appBar().getTitleTextStyle() != null) {
            return tinted(appBar().getTitleTextStyle());
        }
        com.codename1.flutter.TextStyle fromBarTheme = null;
        TextTheme textTheme = null;
        try {
            ThemeData theme = Theme.of(this);
            AppBarTheme bar = theme.appBarTheme();
            if (bar != null) {
                fromBarTheme = bar.titleTextStyle();
            }
            textTheme = theme.textTheme();
        } catch (Throwable t) {
            // no ambient theme
        }
        return tinted(chooseTitleStyle(fromBarTheme, textTheme));
    }

    /**
     * Flutter's chain for the title style:
     * {@code AppBar.titleTextStyle ?? AppBarTheme.titleTextStyle ??
     * textTheme.titleLarge}. The bar's own style is handled by the caller,
     * which returns before reaching here.
     *
     * <p>The last link was missing, so a bar whose theme names no title style
     * -- which is most of them -- fell through to whatever size a bare
     * {@code Text} picks. That is about 16 logical pixels against titleLarge's
     * 22, and every title in the gallery rendered at roughly seven tenths of
     * its size.</p>
     */
    static com.codename1.flutter.TextStyle chooseTitleStyle(
            com.codename1.flutter.TextStyle fromBarTheme, TextTheme textTheme) {
        if (fromBarTheme != null) {
            return fromBarTheme;
        }
        return textTheme == null ? null : textTheme.titleLarge();
    }

    /** The style for everything else on the bar (Flutter's toolbarTextStyle). */
    private com.codename1.flutter.TextStyle toolbarTextStyle() {
        com.codename1.flutter.TextStyle themed = null;
        try {
            AppBarTheme bar = Theme.of(this).appBarTheme();
            if (bar != null) {
                themed = bar.toolbarTextStyle();
            }
        } catch (Throwable t) {
            // no ambient theme
        }
        return tinted(themed);
    }

    /** {@code base} with the bar's foreground applied when it names no colour of its own. */
    private com.codename1.flutter.TextStyle tinted(com.codename1.flutter.TextStyle base) {
        com.codename1.flutter.Color fg = effectiveForeground();
        if (base == null) {
            if (fg == null) {
                return null;
            }
            com.codename1.flutter.TextStyle t = new com.codename1.flutter.TextStyle();
            t.color(fg);
            return t;
        }
        if (base.getColor() != null || fg == null) {
            return base;
        }
        return base.copyWith(null, fg, null, null, null, null, null, null,
                null, null, null, null, null);
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
            if (bar != null && bar.foregroundColor() != null) {
                return bar.foregroundColor();
            }
            return Theme.of(this).colorScheme().onSurface();
        } catch (Throwable t) {
            return null;
        }
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
            strip.getAllStyles().setFgColor((int) (fg.value() & 0xFFFFFFL));
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

    /**
     * The status-bar strip this bar has to clear, in pixels.
     *
     * <p>Flutter's app bar is {@code toolbarHeight + MediaQuery.padding.top}
     * tall and puts its row below the inset, while the flexible space fills the
     * whole thing. Ours was just {@code toolbarHeight}, so a bar at the top of
     * the screen came out a notch short and any safe area inside its flexible
     * space pushed that content clean out of the bar — which is why Crane's
     * logo and tab bar rendered below their own app bar.</p>
     */
    private double topInset() {
        if (!appBar().isPrimary() || toolbarMode()) {
            return 0;
        }
        try {
            return Dp.px(com.codename1.flutter.MediaQuery.of(this).padding().top());
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double totalHeight = barHeight(constraints);
        double topInset = Math.min(topInset(), totalHeight);
        // The row occupies the toolbar band; `bottom` takes the rest.
        RenderElement bottom = renderAt(bottomIndex);
        double bottomHeight = bottomHeight(constraints, bottom);
        double barHeight = Math.max(0, totalHeight - bottomHeight - topInset);
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
            int lastAction = bottomIndex >= 0 ? bottomIndex : children.size();
            for (int i = firstActionIndex; i < lastAction; i++) {
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
            // A bar whose only content is its background layer still has a width.
            // Crane's bar has no title, leading or actions at all, so summing the
            // row alone would measure it as zero and collapse the strip.
            RenderElement flexDry = renderAt(flexibleSpaceIndex);
            if (flexDry != null) {
                width = Math.max(width, flexDry.layout(
                        BoxConstraints.loose(Double.POSITIVE_INFINITY, barHeight)).width());
            }
        }

        // The background layer last, once the bar's width is settled: it fills the
        // whole bar rather than taking a share of it, so it must not contribute to
        // the width the row was measured against (that is what makes it a
        // background and not a fourth slot).
        RenderElement flexible = renderAt(flexibleSpaceIndex);
        if (flexible != null) {
            flexible.layout(BoxConstraints.tight(width, totalHeight));
            setChildOffset(flexible, 0, 0);
        }
        if (bottom != null) {
            bottom.layout(BoxConstraints.tight(width, bottomHeight));
            setChildOffset(bottom, 0, topInset + barHeight);
        }

        // Place: leading at the start, actions flush to the end, title between.
        if (leading != null) {
            setChildOffset(leading, 0, topInset + centreY(leadingSize, barHeight));
        }
        double actionX = width - actionsWidth;
        for (int i = 0; i < actions.size(); i++) {
            Size as = actionSizes.get(i);
            setChildOffset(actions.get(i), actionX, topInset + centreY(as, barHeight));
            actionX += as.width();
        }
        if (title != null) {
            double tx;
            if (centerTitle()) {
                tx = (width - titleSize.width()) / 2;
                // A centred title still may not slide under the leading or the actions.
                tx = Math.max(leadingWidth + spacing,
                        Math.min(tx, width - actionsWidth - spacing - titleSize.width()));
                tx = Math.max(0, tx);
            } else {
                tx = leadingWidth + spacing;
            }
            setChildOffset(title, tx, topInset + centreY(titleSize, barHeight));
        }

        return constraints.constrain(new Size(width, totalHeight));
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
    /**
     * The height {@code bottom} wants, measured against an UNBOUNDED height.
     *
     * <p>Offering it the bar's height instead invites a greedy child to take
     * all of it — a scrollable tab strip does exactly that — which leaves the
     * toolbar row nothing and stacks the two on top of each other. It also has
     * to be the same number {@link #barHeight} used, or the two passes disagree
     * about where the row ends.</p>
     */
    private double bottomHeight(BoxConstraints constraints, RenderElement bottom) {
        if (bottom == null) {
            return 0;
        }
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth()
                : Double.POSITIVE_INFINITY;
        return bottom.layout(BoxConstraints.loose(w, Double.POSITIVE_INFINITY)).height();
    }

    private double barHeight(BoxConstraints constraints) {
        double preferred = Dp.px(appBar().getToolbarHeight() == null
                ? TOOLBAR_HEIGHT_LP : appBar().getToolbarHeight().doubleValue())
                + topInset();
        RenderElement bottom = renderAt(bottomIndex);
        if (bottom != null && !(toolbarMode() && constraints.hasBoundedHeight()
                && constraints.maxHeight() > 0)) {
            preferred += bottomHeight(constraints, bottom);
        }
        if (toolbarMode() && constraints.hasBoundedHeight() && constraints.maxHeight() > 0) {
            return constraints.maxHeight();
        }
        return constraints.constrainHeight(preferred);
    }

    /**
     * Whether the title is centred — {@code AppBar.centerTitle}, then the
     * ambient theme's, then the platform default.
     *
     * <p>Flutter centres app bar titles on iOS and macOS and left-aligns them
     * everywhere else. Defaulting to left on every platform puts the title in
     * the wrong place on every iOS screen in the app.</p>
     */
    private boolean centerTitle() {
        if (appBar().isCenterTitleSet()) {
            return appBar().getCenterTitle();
        }
        try {
            AppBarTheme bar = Theme.of(this).appBarTheme();
            if (bar != null && bar.centerTitle() != null) {
                return bar.centerTitle().booleanValue();
            }
        } catch (Throwable t) {
            // no ambient theme
        }
        com.codename1.flutter.TargetPlatform p =
                com.codename1.flutter.foundation.FoundationLib.defaultTargetPlatform;
        return p == com.codename1.flutter.TargetPlatform.iOS
                || p == com.codename1.flutter.TargetPlatform.macOS;
    }

    private double titleSpacing() {
        Double s = appBar().getTitleSpacing();
        return s == null ? TITLE_SPACING_LP : s.doubleValue();
    }
}

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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.Widget;
import com.codename1.flutter.animation.Animation;
import com.codename1.flutter.animation.AlwaysStoppedAnimation;

import dart.core.DartList;

/**
 * A vertical Material navigation rail — Flutter's {@code NavigationRail}, the
 * desktop/tablet counterpart of a BottomNavigationBar. Signature-only this
 * pass: destinations and styling are captured; {@link #extendedAnimation} hands
 * back a settled 1.0 animation so descendants that drive off the extend state
 * render in their extended layout.
 */
public class NavigationRail extends StatelessWidget {

    private Color backgroundColor;
    private boolean extended;
    private Widget leading;
    private Widget trailing;
    private DartList<NavigationRailDestination> destinations;
    private long selectedIndex;
    private Object onDestinationSelected;
    private double elevation;
    private double groupAlignment;
    private NavigationRailLabelType labelType;
    private TextStyle unselectedLabelTextStyle;
    private TextStyle selectedLabelTextStyle;
    private IconThemeData unselectedIconTheme;
    private IconThemeData selectedIconTheme;
    private double minWidth;
    private double minExtendedWidth;
    /// Material 3 draws the selection indicator by default, so this starts true
    /// -- reading it as false left the selected destination with no pill behind
    /// its icon at all.
    private boolean useIndicator = true;
    private Color indicatorColor;
    private Object indicatorShape;

    public void backgroundColor(Color v) { this.backgroundColor = v; }
    public void extended(boolean v) { this.extended = v; }
    public void leading(Widget v) { this.leading = v; }
    public void trailing(Widget v) { this.trailing = v; }
    public void destinations(DartList<NavigationRailDestination> v) { this.destinations = v; }
    public void selectedIndex(long v) { this.selectedIndex = v; }
    public void onDestinationSelected(dart.runtime.Funcs.VoidFunc1<Long> v) { this.onDestinationSelected = v; }
    public void elevation(double v) { this.elevation = v; }
    public void groupAlignment(double v) { this.groupAlignment = v; }
    public void labelType(NavigationRailLabelType v) { this.labelType = v; }
    public void unselectedLabelTextStyle(TextStyle v) { this.unselectedLabelTextStyle = v; }
    public void selectedLabelTextStyle(TextStyle v) { this.selectedLabelTextStyle = v; }
    public void unselectedIconTheme(IconThemeData v) { this.unselectedIconTheme = v; }
    public void selectedIconTheme(IconThemeData v) { this.selectedIconTheme = v; }
    public void minWidth(double v) { this.minWidth = v; }
    public void minExtendedWidth(double v) { this.minExtendedWidth = v; }
    public void useIndicator(boolean v) { this.useIndicator = v; }
    public void indicatorColor(Color v) { this.indicatorColor = v; }
    public void indicatorShape(Object v) { this.indicatorShape = v; }

    /**
     * Dart's {@code NavigationRail.extendedAnimation(context)}: the 0..1
     * animation of the rail's extended state. Deferred rendering supplies a
     * settled (1.0) animation.
     */
    public static Animation<Double> extendedAnimation(BuildContext context) {
        return new AlwaysStoppedAnimation<Double>(1.0);
    }

    /** Flutter's default {@code minWidth}. */
    private static final double DEFAULT_WIDTH_LP = 72;
    /** Flutter's default {@code minExtendedWidth}. */
    private static final double DEFAULT_EXTENDED_WIDTH_LP = 256;
    /** The gap above and below {@code leading} -- Flutter's _verticalSpacer. */
    private static final double VERTICAL_SPACER_LP = 8;
    /** The Material 3 selection indicator behind the icon. */
    private static final double INDICATOR_WIDTH_LP = 56;
    private static final double INDICATOR_HEIGHT_LP = 32;
    /** Vertical padding around each destination. */
    private static final double DESTINATION_PADDING_LP = 12;
    /** The gap between an icon and the label under it. */
    private static final double LABEL_GAP_LP = 4;

    /**
     * The rail: a fixed-width column of the leading widget, the destinations and
     * the trailing widget.
     *
     * <p>This used to return {@code leading} and nothing else, so a rail whose
     * destinations ARE its content rendered as a single floating button --
     * the navigation-rail demo drew its create button in the middle of an
     * otherwise empty page, with no rail behind it and no destinations at
     * all.</p>
     */
    @Override
    public Widget build(BuildContext context) {
        double width = extended
                ? (minExtendedWidth > 0 ? minExtendedWidth : DEFAULT_EXTENDED_WIDTH_LP)
                : (minWidth > 0 ? minWidth : DEFAULT_WIDTH_LP);
        DartList<Widget> kids = new DartList<Widget>();
        if (leading != null) {
            kids.add(gap(VERTICAL_SPACER_LP));
            kids.add(leading);
            kids.add(gap(VERTICAL_SPACER_LP));
        }
        if (destinations != null) {
            for (int i = 0; i < destinations.size(); i++) {
                NavigationRailDestination d = destinations.get(i);
                if (d != null) {
                    kids.add(destinationTile(context, d, i == selectedIndex, width));
                }
            }
        }
        if (trailing != null) {
            kids.add(trailing);
        }
        com.codename1.flutter.widgets.Column column = new com.codename1.flutter.widgets.Column();
        column.mainAxisSize(com.codename1.flutter.MainAxisSize.max);
        column.mainAxisAlignment(com.codename1.flutter.MainAxisAlignment.start);
        column.crossAxisAlignment(com.codename1.flutter.CrossAxisAlignment.center);
        column.children(kids);

        com.codename1.flutter.widgets.Container rail =
                new com.codename1.flutter.widgets.Container();
        rail.width(width);
        // A rail is a SURFACE, and it is what separates it from the page beside
        // it. Flutter falls back to colorScheme.surface when the rail names no
        // colour, and without that the demo's rail was invisible: white
        // destinations on a white page.
        Color surface = backgroundColor != null ? backgroundColor : surfaceTint(context);
        if (surface != null) {
            rail.color(surface);
        }
        rail.child(column);
        return rail;
    }

    /// One destination: its icon, under the Material 3 indicator when selected,
    /// with the label beneath it when the label type asks for one.
    private Widget destinationTile(BuildContext context, NavigationRailDestination d,
            boolean selected, double width) {
        Widget icon = selected && d.getSelectedIcon() != null
                ? d.getSelectedIcon() : d.getIcon();
        Widget top = icon;
        if (selected && useIndicator && icon != null) {
            com.codename1.flutter.BoxDecoration pill = new com.codename1.flutter.BoxDecoration();
            pill.color(indicatorColor != null ? indicatorColor : indicatorTint(context));
            pill.borderRadius(com.codename1.flutter.BorderRadius.circular(INDICATOR_HEIGHT_LP / 2));
            com.codename1.flutter.widgets.Container box =
                    new com.codename1.flutter.widgets.Container();
            box.width(INDICATOR_WIDTH_LP);
            box.height(INDICATOR_HEIGHT_LP);
            box.decoration(pill);
            box.alignment(com.codename1.flutter.Alignment.center);
            box.child(icon);
            top = box;
        }
        DartList<Widget> parts = new DartList<Widget>();
        if (top != null) {
            parts.add(top);
        }
        boolean showLabel = labelType == NavigationRailLabelType.all
                || (labelType == NavigationRailLabelType.selected && selected);
        if (showLabel && d.getLabel() != null) {
            parts.add(gap(LABEL_GAP_LP));
            parts.add(d.getLabel());
        }
        com.codename1.flutter.widgets.Column tile = new com.codename1.flutter.widgets.Column();
        tile.mainAxisSize(com.codename1.flutter.MainAxisSize.min);
        tile.mainAxisAlignment(com.codename1.flutter.MainAxisAlignment.center);
        tile.crossAxisAlignment(com.codename1.flutter.CrossAxisAlignment.center);
        tile.children(parts);

        com.codename1.flutter.widgets.Container slot =
                new com.codename1.flutter.widgets.Container();
        slot.width(width);
        slot.padding(com.codename1.flutter.EdgeInsets.symmetric(0, DESTINATION_PADDING_LP));
        slot.alignment(com.codename1.flutter.Alignment.center);
        slot.child(tile);
        return slot;
    }

    /// The rail's own surface colour.
    private static Color surfaceTint(BuildContext context) {
        try {
            ColorScheme scheme = Theme.of(context).colorScheme();
            return scheme == null ? null : scheme.surface();
        } catch (Throwable noTheme) {
            return null;
        }
    }

    /// Material 3 tints the indicator with secondaryContainer.
    private static Color indicatorTint(BuildContext context) {
        try {
            ColorScheme scheme = Theme.of(context).colorScheme();
            return scheme == null ? null : scheme.secondaryContainer();
        } catch (Throwable noTheme) {
            return null;
        }
    }

    private static Widget gap(double heightLp) {
        com.codename1.flutter.widgets.SizedBox b = new com.codename1.flutter.widgets.SizedBox();
        b.height(heightLp);
        return b;
    }
}

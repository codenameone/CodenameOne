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
    private boolean useIndicator;
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

    @Override
    public Widget build(BuildContext context) {
        return leading;
    }
}

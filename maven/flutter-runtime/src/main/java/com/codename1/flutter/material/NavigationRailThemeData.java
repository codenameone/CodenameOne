package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.TextStyle;

/**
 * Theming values for descendant {@link NavigationRail}s — Flutter's
 * {@code NavigationRailThemeData}. Reached both as a constructed theme value and
 * via {@code Theme.of(context).navigationRailTheme}; the reply study reads
 * {@link #unselectedLabelTextStyle()} for its folder-section colors.
 */
public class NavigationRailThemeData {

    private Color backgroundColor;
    private double elevation;
    private TextStyle unselectedLabelTextStyle;
    private TextStyle selectedLabelTextStyle;
    private IconThemeData unselectedIconTheme;
    private IconThemeData selectedIconTheme;
    private double groupAlignment;
    private NavigationRailLabelType labelType;
    private boolean useIndicator;
    private Color indicatorColor;
    private Object indicatorShape;
    private double minWidth;
    private double minExtendedWidth;

    public void backgroundColor(Color v) { this.backgroundColor = v; }
    public void elevation(double v) { this.elevation = v; }
    public void unselectedLabelTextStyle(TextStyle v) { this.unselectedLabelTextStyle = v; }
    public void selectedLabelTextStyle(TextStyle v) { this.selectedLabelTextStyle = v; }
    public void unselectedIconTheme(IconThemeData v) { this.unselectedIconTheme = v; }
    public void selectedIconTheme(IconThemeData v) { this.selectedIconTheme = v; }
    public void groupAlignment(double v) { this.groupAlignment = v; }
    public void labelType(NavigationRailLabelType v) { this.labelType = v; }
    public void useIndicator(boolean v) { this.useIndicator = v; }
    public void indicatorColor(Color v) { this.indicatorColor = v; }
    public void indicatorShape(Object v) { this.indicatorShape = v; }
    public void minWidth(double v) { this.minWidth = v; }
    public void minExtendedWidth(double v) { this.minExtendedWidth = v; }

    public Color backgroundColor() { return backgroundColor; }
    public double elevation() { return elevation; }
    public TextStyle unselectedLabelTextStyle() { return unselectedLabelTextStyle; }
    public TextStyle selectedLabelTextStyle() { return selectedLabelTextStyle; }
    public IconThemeData unselectedIconTheme() { return unselectedIconTheme; }
    public IconThemeData selectedIconTheme() { return selectedIconTheme; }
}

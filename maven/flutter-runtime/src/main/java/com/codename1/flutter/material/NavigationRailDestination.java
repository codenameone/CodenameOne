package com.codename1.flutter.material;

import com.codename1.flutter.EdgeInsetsGeometry;
import com.codename1.flutter.Widget;

/**
 * A single selectable entry in a {@link NavigationRail} — Flutter's
 * {@code NavigationRailDestination}. Signature-only: the icon/label widgets are
 * captured for the rail to lay out; disabled/tooltip are recorded but unused.
 */
public class NavigationRailDestination {

    private Widget icon;
    private Widget selectedIcon;
    private Widget label;
    private EdgeInsetsGeometry padding;
    private boolean disabled;
    private String indicatorColorTooltip;

    public void icon(Widget v) { this.icon = v; }
    public void selectedIcon(Widget v) { this.selectedIcon = v; }
    public void label(Widget v) { this.label = v; }
    public void padding(EdgeInsetsGeometry v) { this.padding = v; }
    public void disabled(boolean v) { this.disabled = v; }
    public void indicatorColorTooltip(String v) { this.indicatorColorTooltip = v; }

    public Widget getIcon() { return icon; }
    public Widget getSelectedIcon() { return selectedIcon; }
    public Widget getLabel() { return label; }
}

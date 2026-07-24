package com.codename1.flutter.material;

import com.codename1.flutter.Widget;

/**
 * One destination of a {@link BottomNavigationBar}: an icon widget and an
 * optional text label. Configuration only — not itself a widget.
 */
public class BottomNavigationBarItem {

    private Widget icon;
    private String label;

    public void icon(Widget v) {
        this.icon = v;
    }

    public void label(String v) {
        this.label = v;
    }

    public Widget getIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }

    /** Dart {@code item.icon} getter. */
    public Widget icon() {
        return icon;
    }

    /** Dart {@code item.label} getter. */
    public String label() {
        return label;
    }
}

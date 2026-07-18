package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * The material icon button: a bare tappable icon, backed by a CN1 Button
 * (UIID "FlutterIconButton"). The {@code icon} widget is consumed as
 * configuration (an {@link com.codename1.flutter.widgets.Icon Icon}'s glyph
 * becomes the material icon).
 */
public class IconButton extends Widget {

    private Funcs.VoidFunc0 onPressed;
    private Widget icon;
    private Double iconSize;
    private Color color;

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void icon(Widget v) {
        this.icon = v;
    }

    public void iconSize(double v) {
        this.iconSize = v;
    }

    public void color(Color v) {
        this.color = v;
    }

    public Funcs.VoidFunc0 getOnPressed() {
        return onPressed;
    }

    public Widget getIcon() {
        return icon;
    }

    public Double getIconSize() {
        return iconSize;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public Element createElement() {
        return new ButtonRenderElement(this);
    }
}

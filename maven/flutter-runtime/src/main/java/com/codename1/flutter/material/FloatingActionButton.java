package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.Element;
import com.codename1.flutter.Key;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A material floating action button, backed by the real CN1
 * {@code com.codename1.components.FloatingActionButton}. M1 consumes an
 * {@link com.codename1.flutter.widgets.Icon Icon} child as configuration
 * (its glyph becomes the FAB icon); other child widgets are not mounted.
 */
public class FloatingActionButton extends Widget {

    private Funcs.VoidFunc0 onPressed;
    private String tooltip;
    private Widget child;
    private Object heroTag;
    private com.codename1.flutter.Color backgroundColor;
    private com.codename1.flutter.Color foregroundColor;
    private Double elevation;

    public void heroTag(Object v) {
        this.heroTag = v;
    }

    public void backgroundColor(com.codename1.flutter.Color v) {
        this.backgroundColor = v;
    }

    public void foregroundColor(com.codename1.flutter.Color v) {
        this.foregroundColor = v;
    }

    public void elevation(double v) {
        this.elevation = v;
    }

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void tooltip(String v) {
        this.tooltip = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Funcs.VoidFunc0 getOnPressed() {
        return onPressed;
    }

    public String getTooltip() {
        return tooltip;
    }

    public Widget getChild() {
        return child;
    }

    /**
     * {@code FloatingActionButton.extended}: a pill-shaped FAB with a label
     * (and optional leading icon). The label is consumed as the FAB content;
     * the leading icon is used when no label is supplied.
     */
    public static FloatingActionButton extended(Key key, Funcs.VoidFunc0 onPressed, Widget label,
            Widget icon, String tooltip, Object heroTag, Color backgroundColor) {
        FloatingActionButton f = new FloatingActionButton();
        f.key(key);
        f.onPressed(onPressed);
        f.tooltip(tooltip);
        f.child(label != null ? label : icon);
        return f;
    }

    @Override
    public Element createElement() {
        return new FabRenderElement(this);
    }
}

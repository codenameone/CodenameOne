package com.codename1.flutter.material;

import com.codename1.flutter.Element;
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

    @Override
    public Element createElement() {
        return new FabRenderElement(this);
    }
}

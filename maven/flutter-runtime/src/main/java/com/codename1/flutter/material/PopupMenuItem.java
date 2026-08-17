package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.HasChild;
import com.codename1.flutter.widgets.PassThroughRenderElement;

import dart.runtime.Funcs;

/**
 * An item in a {@link PopupMenuButton}'s menu — Flutter's
 * {@code PopupMenuItem}. Carries the selection {@code value} and a child
 * widget. Rendered as its child when materialized (the menu presentation is
 * deferred). See {@link PassThroughRenderElement}.
 */
public class PopupMenuItem<T> extends PopupMenuEntry<T> implements HasChild {

    private Object value;
    private boolean enabled = true;
    private double height = 48;
    private Object padding;
    private Object textStyle;
    private Object mouseCursor;
    private Funcs.VoidFunc0 onTap;
    private Widget child;

    public void value(Object v) {
        this.value = v;
    }

    public void enabled(boolean v) {
        this.enabled = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void padding(Object v) {
        this.padding = v;
    }

    public void textStyle(Object v) {
        this.textStyle = v;
    }

    public void mouseCursor(Object v) {
        this.mouseCursor = v;
    }

    public void onTap(Funcs.VoidFunc0 v) {
        this.onTap = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Object getValue() {
        return value;
    }

    public Funcs.VoidFunc0 getOnTap() {
        return onTap;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new PassThroughRenderElement(this);
    }
}

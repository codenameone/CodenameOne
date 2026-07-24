package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A low-level pointer-event listener — Flutter's {@code Listener}. Structural
 * pass-through: the single {@code child} renders unchanged; the pointer
 * callbacks are held for a later input pass.
 */
public class Listener extends Widget implements HasChild {

    private Funcs.VoidFunc1<Object> onPointerDown;
    private Funcs.VoidFunc1<Object> onPointerMove;
    private Funcs.VoidFunc1<Object> onPointerUp;
    private Funcs.VoidFunc1<Object> onPointerCancel;
    private Funcs.VoidFunc1<Object> onPointerHover;
    private Funcs.VoidFunc1<Object> onPointerSignal;
    private Object behavior;
    private Widget child;

    public void onPointerDown(Funcs.VoidFunc1<Object> v) { this.onPointerDown = v; }
    public void onPointerMove(Funcs.VoidFunc1<Object> v) { this.onPointerMove = v; }
    public void onPointerUp(Funcs.VoidFunc1<Object> v) { this.onPointerUp = v; }
    public void onPointerCancel(Funcs.VoidFunc1<Object> v) { this.onPointerCancel = v; }
    public void onPointerHover(Funcs.VoidFunc1<Object> v) { this.onPointerHover = v; }
    public void onPointerSignal(Funcs.VoidFunc1<Object> v) { this.onPointerSignal = v; }
    public void behavior(Object v) { this.behavior = v; }

    public void child(Widget v) {
        this.child = v;
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

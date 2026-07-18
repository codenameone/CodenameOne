package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Detects taps and long presses on its child. There is no CN1 component for
 * the child itself; a transparent overlay component (UIID "FlutterGesture")
 * is positioned exactly over the child's bounds in the flat container and
 * receives the pointer events.
 *
 * <p>Known limitation: interactive widgets INSIDE a GestureDetector (a
 * button in the detected subtree) are shadowed by the overlay, which sits on
 * top of them — rare in practice.</p>
 */
public class GestureDetector extends Widget {

    private Funcs.VoidFunc0 onTap;
    private Funcs.VoidFunc0 onLongPress;
    private Widget child;

    public void onTap(Funcs.VoidFunc0 v) {
        this.onTap = v;
    }

    public void onLongPress(Funcs.VoidFunc0 v) {
        this.onLongPress = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Funcs.VoidFunc0 getOnTap() {
        return onTap;
    }

    public Funcs.VoidFunc0 getOnLongPress() {
        return onLongPress;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Element createElement() {
        return new GestureRenderElement(this);
    }
}

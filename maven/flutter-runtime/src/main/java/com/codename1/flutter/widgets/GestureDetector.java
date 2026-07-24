package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.gestures.GestureDragEndCallback;
import com.codename1.flutter.gestures.GestureDragStartCallback;
import com.codename1.flutter.gestures.GestureDragUpdateCallback;
import com.codename1.flutter.gestures.GestureTapDownCallback;
import com.codename1.flutter.gestures.GestureTapUpCallback;
import com.codename1.flutter.rendering.HitTestBehavior;

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
    private Funcs.VoidFunc0 onDoubleTap;
    private Widget child;
    private HitTestBehavior behavior;
    private GestureTapDownCallback onTapDown;
    private GestureTapUpCallback onTapUp;
    private GestureDragStartCallback onVerticalDragStart;
    private GestureDragUpdateCallback onVerticalDragUpdate;
    private GestureDragEndCallback onVerticalDragEnd;
    private GestureDragStartCallback onHorizontalDragStart;
    private GestureDragUpdateCallback onHorizontalDragUpdate;
    private GestureDragEndCallback onHorizontalDragEnd;
    private GestureDragStartCallback onPanStart;
    private GestureDragUpdateCallback onPanUpdate;
    private GestureDragEndCallback onPanEnd;

    public void onTap(Funcs.VoidFunc0 v) {
        this.onTap = v;
    }

    public void onLongPress(Funcs.VoidFunc0 v) {
        this.onLongPress = v;
    }

    public void onDoubleTap(Funcs.VoidFunc0 v) {
        this.onDoubleTap = v;
    }

    public void behavior(HitTestBehavior v) {
        this.behavior = v;
    }

    public void excludeFromSemantics(boolean v) {
    }

    public void dragStartBehavior(Object v) {
    }

    public void onTapDown(GestureTapDownCallback v) {
        this.onTapDown = v;
    }

    public void onTapUp(GestureTapUpCallback v) {
        this.onTapUp = v;
    }

    public void onVerticalDragStart(GestureDragStartCallback v) {
        this.onVerticalDragStart = v;
    }

    public void onVerticalDragUpdate(GestureDragUpdateCallback v) {
        this.onVerticalDragUpdate = v;
    }

    public void onVerticalDragEnd(GestureDragEndCallback v) {
        this.onVerticalDragEnd = v;
    }

    public void onHorizontalDragStart(GestureDragStartCallback v) {
        this.onHorizontalDragStart = v;
    }

    public void onHorizontalDragUpdate(GestureDragUpdateCallback v) {
        this.onHorizontalDragUpdate = v;
    }

    public void onHorizontalDragEnd(GestureDragEndCallback v) {
        this.onHorizontalDragEnd = v;
    }

    public void onPanStart(GestureDragStartCallback v) {
        this.onPanStart = v;
    }

    public void onPanUpdate(GestureDragUpdateCallback v) {
        this.onPanUpdate = v;
    }

    public void onPanEnd(GestureDragEndCallback v) {
        this.onPanEnd = v;
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

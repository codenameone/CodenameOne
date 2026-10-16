/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
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

    /// The vertical drag-end callback, or null.
    ///
    /// #### Returns
    ///
    /// the callback given to {@code onVerticalDragEnd}
    public com.codename1.flutter.gestures.GestureDragEndCallback getOnVerticalDragEnd() {
        return onVerticalDragEnd;
    }

    public com.codename1.flutter.gestures.GestureDragStartCallback getOnVerticalDragStart() {
        return onVerticalDragStart;
    }

    public com.codename1.flutter.gestures.GestureDragUpdateCallback getOnVerticalDragUpdate() {
        return onVerticalDragUpdate;
    }

    public com.codename1.flutter.gestures.GestureDragStartCallback getOnHorizontalDragStart() {
        return onHorizontalDragStart;
    }

    public com.codename1.flutter.gestures.GestureDragUpdateCallback getOnHorizontalDragUpdate() {
        return onHorizontalDragUpdate;
    }

    public com.codename1.flutter.gestures.GestureDragEndCallback getOnHorizontalDragEnd() {
        return onHorizontalDragEnd;
    }

    public com.codename1.flutter.gestures.GestureDragStartCallback getOnPanStart() {
        return onPanStart;
    }

    public com.codename1.flutter.gestures.GestureDragUpdateCallback getOnPanUpdate() {
        return onPanUpdate;
    }

    public com.codename1.flutter.gestures.GestureDragEndCallback getOnPanEnd() {
        return onPanEnd;
    }

    /** Whether any vertical-drag callback is set. */
    boolean handlesVerticalDrag() {
        return onVerticalDragStart != null || onVerticalDragUpdate != null || onVerticalDragEnd != null;
    }

    /** Whether any horizontal-drag callback is set. */
    boolean handlesHorizontalDrag() {
        return onHorizontalDragStart != null || onHorizontalDragUpdate != null || onHorizontalDragEnd != null;
    }

    /** Whether any pan callback is set. */
    boolean handlesPan() {
        return onPanStart != null || onPanUpdate != null || onPanEnd != null;
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

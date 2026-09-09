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

import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * A pan/zoom viewport for its {@code child} — Flutter's {@code InteractiveViewer}.
 * Structural pass-through for this milestone: the {@code child} renders
 * unchanged; the {@link TransformationController} and interaction callbacks are
 * captured for a later render pass that applies the live matrix.
 */
public class InteractiveViewer extends Widget implements HasChild {

    private TransformationController transformationController;
    private EdgeInsets boundaryMargin;
    private double minScale;
    private double maxScale;
    private Boolean constrained;
    private Boolean panEnabled;
    private Boolean scaleEnabled;
    private double scaleFactor;
    private Object onInteractionStart;
    private Object onInteractionUpdate;
    private Object onInteractionEnd;
    private Object clipBehavior;
    private Boolean alignPanAxis;
    private Widget child;

    public void transformationController(TransformationController v) { this.transformationController = v; }
    public void boundaryMargin(EdgeInsets v) { this.boundaryMargin = v; }
    public void minScale(double v) { this.minScale = v; }
    public void maxScale(double v) { this.maxScale = v; }
    public void constrained(Boolean v) { this.constrained = v; }
    public void panEnabled(Boolean v) { this.panEnabled = v; }
    public void scaleEnabled(Boolean v) { this.scaleEnabled = v; }
    public void scaleFactor(double v) { this.scaleFactor = v; }
    public void onInteractionStart(dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.gestures.ScaleStartDetails> v) { this.onInteractionStart = v; }
    public void onInteractionUpdate(dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.gestures.ScaleUpdateDetails> v) { this.onInteractionUpdate = v; }
    public void onInteractionEnd(dart.runtime.Funcs.VoidFunc1<com.codename1.flutter.gestures.ScaleEndDetails> v) { this.onInteractionEnd = v; }
    public void clipBehavior(Object v) { this.clipBehavior = v; }
    public void alignPanAxis(Boolean v) { this.alignPanAxis = v; }

    public void child(Widget v) {
        this.child = v;
    }

    public TransformationController getTransformationController() {
        return transformationController;
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

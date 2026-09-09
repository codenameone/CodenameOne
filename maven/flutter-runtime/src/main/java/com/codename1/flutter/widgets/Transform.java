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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Key;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * Applies a geometric transform to its {@code child} before painting —
 * Flutter's {@code Transform}. The default constructor takes a 4x4 matrix; the
 * {@code .rotate}, {@code .scale} and {@code .translate} named constructors are
 * convenience factories. This pass records the transform parameters and renders
 * the child untransformed; applying the matrix at paint time is deferred.
 */
public class Transform extends Widget {

    private Object transform;
    private Object origin;
    private Object alignment;
    private boolean transformHitTests = true;
    private Object filterQuality;
    private Double angle;
    private Double scale;
    private Double scaleX;
    private Double scaleY;
    private Object offset;
    private Widget child;

    // --- default constructor: named-param setters -----------------------

    public void transform(Object v) {
        this.transform = v;
    }

    public void origin(Object v) {
        this.origin = v;
    }

    public void alignment(Object v) {
        this.alignment = v;
    }

    public void transformHitTests(boolean v) {
        this.transformHitTests = v;
    }

    public void filterQuality(Object v) {
        this.filterQuality = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getChild() {
        return child;
    }

    // --- named constructors ---------------------------------------------

    public static Transform rotate(Key key, double angle, Object origin, Object alignment,
            Boolean transformHitTests, Object filterQuality, Widget child) {
        Transform t = new Transform();
        t.key(key);
        t.angle = angle;
        t.origin = origin;
        t.alignment = alignment;
        if (transformHitTests != null) {
            t.transformHitTests = transformHitTests;
        }
        t.filterQuality = filterQuality;
        t.child = child;
        return t;
    }

    public static Transform scale(Key key, Double scale, Double scaleX, Double scaleY, Object origin,
            Object alignment, Boolean transformHitTests, Object filterQuality, Widget child) {
        Transform t = new Transform();
        t.key(key);
        t.scale = scale;
        t.scaleX = scaleX;
        t.scaleY = scaleY;
        t.origin = origin;
        t.alignment = alignment;
        if (transformHitTests != null) {
            t.transformHitTests = transformHitTests;
        }
        t.filterQuality = filterQuality;
        t.child = child;
        return t;
    }

    public static Transform translate(Key key, Object offset, Boolean transformHitTests,
            Object filterQuality, Widget child) {
        Transform t = new Transform();
        t.key(key);
        t.offset = offset;
        if (transformHitTests != null) {
            t.transformHitTests = transformHitTests;
        }
        t.filterQuality = filterQuality;
        t.child = child;
        return t;
    }

    /// The horizontal scale in effect: scaleX when given, else the uniform scale, else 1.
    public double effectiveScaleX() {
        if (scaleX != null) {
            return scaleX.doubleValue();
        }
        return scale != null ? scale.doubleValue() : 1.0;
    }

    /// The vertical scale in effect: scaleY when given, else the uniform scale, else 1.
    public double effectiveScaleY() {
        if (scaleY != null) {
            return scaleY.doubleValue();
        }
        return scale != null ? scale.doubleValue() : 1.0;
    }

    /// The rotation in radians, or null when this is not a rotation.
    public Double effectiveAngle() {
        return angle;
    }

    /// The translation, or null when this is not a translation.
    public com.codename1.flutter.Offset effectiveOffset() {
        return offset instanceof com.codename1.flutter.Offset
                ? (com.codename1.flutter.Offset) offset : null;
    }

    @Override
    public com.codename1.flutter.Element createElement() {
        return new TransformRenderElement(this);
    }
}

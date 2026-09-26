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
package com.codename1.flutter.gestures;

import com.codename1.flutter.Offset;

/**
 * The incremental details of a scale/pan gesture — Flutter's
 * {@code ScaleUpdateDetails}.
 */
public final class ScaleUpdateDetails {

    private Offset focalPoint = Offset.zero;
    private Offset localFocalPoint = Offset.zero;
    private double scale = 1.0;
    private double horizontalScale = 1.0;
    private double verticalScale = 1.0;
    private double rotation;
    private Offset focalPointDelta = Offset.zero;

    public ScaleUpdateDetails() {
    }

    public ScaleUpdateDetails(Offset focalPoint, Offset localFocalPoint, double scale, double rotation) {
        this.focalPoint = focalPoint == null ? Offset.zero : focalPoint;
        this.localFocalPoint = localFocalPoint == null ? Offset.zero : localFocalPoint;
        this.scale = scale;
        this.rotation = rotation;
    }

    public Offset focalPoint() {
        return focalPoint;
    }

    public void focalPoint(Offset v) {
        this.focalPoint = v;
    }

    public Offset localFocalPoint() {
        return localFocalPoint;
    }

    public void localFocalPoint(Offset v) {
        this.localFocalPoint = v;
    }

    public Offset globalPosition() {
        return focalPoint;
    }

    public Offset localPosition() {
        return localFocalPoint;
    }

    public Offset focalPointDelta() {
        return focalPointDelta;
    }

    public void focalPointDelta(Offset v) {
        this.focalPointDelta = v;
    }

    public double scale() {
        return scale;
    }

    public void scale(double v) {
        this.scale = v;
    }

    public double horizontalScale() {
        return horizontalScale;
    }

    public void horizontalScale(double v) {
        this.horizontalScale = v;
    }

    public double verticalScale() {
        return verticalScale;
    }

    public void verticalScale(double v) {
        this.verticalScale = v;
    }

    public double rotation() {
        return rotation;
    }

    public void rotation(double v) {
        this.rotation = v;
    }
}

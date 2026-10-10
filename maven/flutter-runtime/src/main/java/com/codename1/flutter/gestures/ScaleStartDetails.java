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
 * The details at the start of a scale/pan gesture — Flutter's
 * {@code ScaleStartDetails}.
 */
public final class ScaleStartDetails {

    private Offset focalPoint = Offset.zero;
    private Offset localFocalPoint = Offset.zero;
    private long pointerCount;

    public ScaleStartDetails() {
    }

    public ScaleStartDetails(Offset focalPoint, Offset localFocalPoint, long pointerCount) {
        this.focalPoint = focalPoint == null ? Offset.zero : focalPoint;
        this.localFocalPoint = localFocalPoint == null ? Offset.zero : localFocalPoint;
        this.pointerCount = pointerCount;
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

    public long pointerCount() {
        return pointerCount;
    }

    public void pointerCount(long v) {
        this.pointerCount = v;
    }
}

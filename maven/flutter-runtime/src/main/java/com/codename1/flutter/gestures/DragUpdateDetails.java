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
 * The incremental details of a drag — Flutter's {@code DragUpdateDetails}. The
 * reply bottom-drawer reads {@link #primaryDelta()} to drive its
 * AnimationController.
 */
public final class DragUpdateDetails {

    private Offset globalPosition = Offset.zero;
    private Offset localPosition = Offset.zero;
    private Offset delta = Offset.zero;
    private Double primaryDelta;

    public DragUpdateDetails() {
    }

    public DragUpdateDetails(Offset globalPosition, Offset localPosition, Offset delta, Double primaryDelta) {
        this.globalPosition = globalPosition == null ? Offset.zero : globalPosition;
        this.localPosition = localPosition == null ? Offset.zero : localPosition;
        this.delta = delta == null ? Offset.zero : delta;
        this.primaryDelta = primaryDelta;
    }

    public Offset delta() {
        return delta;
    }

    public void delta(Offset v) {
        this.delta = v;
    }

    public Double primaryDelta() {
        return primaryDelta;
    }

    public void primaryDelta(Double v) {
        this.primaryDelta = v;
    }

    public Offset globalPosition() {
        return globalPosition;
    }

    public void globalPosition(Offset v) {
        this.globalPosition = v;
    }

    public Offset localPosition() {
        return localPosition;
    }

    public void localPosition(Offset v) {
        this.localPosition = v;
    }
}

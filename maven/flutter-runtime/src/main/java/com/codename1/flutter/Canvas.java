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
package com.codename1.flutter;

/**
 * The drawing surface handed to a CustomPainter — Flutter's dart:ui
 * {@code Canvas}. This milestone provides the API surface (so painters can be
 * transpiled and their draw/transform calls resolve); the concrete backend
 * that binds these calls to a Codename One {@code Graphics} is supplied by the
 * render layer.
 */
public class Canvas {

    public void drawPath(Path path, Paint paint) {
    }

    public void drawRect(Rect rect, Paint paint) {
    }

    public void drawRRect(RRect rrect, Paint paint) {
    }

    public void drawCircle(Offset c, double radius, Paint paint) {
    }

    public void drawOval(Rect rect, Paint paint) {
    }

    public void drawLine(Offset p1, Offset p2, Paint paint) {
    }

    public void drawArc(Rect rect, double startAngle, double sweepAngle, boolean useCenter, Paint paint) {
    }

    public void drawPoints(Object pointMode, Object points, Paint paint) {
    }

    public void drawColor(Color color, Object blendMode) {
    }

    public void drawShadow(Path path, Color color, double elevation, boolean transparentOccluder) {
    }

    public void drawVertices(Object vertices, Object blendMode, Paint paint) {
    }

    public void drawImage(Object image, Offset offset, Paint paint) {
    }

    public void translate(double dx, double dy) {
    }

    public void scale(double sx, double sy) {
    }

    public void rotate(double radians) {
    }

    public void skew(double sx, double sy) {
    }

    public void save() {
    }

    public void saveLayer(Rect bounds, Paint paint) {
    }

    public void restore() {
    }

    public void clipRect(Rect rect) {
    }

    public void clipRRect(RRect rrect) {
    }

    public void clipPath(Path path) {
    }
}

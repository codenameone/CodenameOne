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

import com.codename1.flutter.rendering.Size;

/**
 * An immutable axis-aligned rectangle given by its four edges (left, top,
 * right, bottom) in logical pixels — Flutter's dart:ui {@code Rect}.
 */
public final class Rect {

    public static final Rect zero = new Rect(0, 0, 0, 0);
    public static final Rect largest = fromLTRB(
            -1.0E9, -1.0E9, 1.0E9, 1.0E9);

    private final double left;
    private final double top;
    private final double right;
    private final double bottom;

    private Rect(double left, double top, double right, double bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static Rect fromLTWH(double left, double top, double width, double height) {
        return new Rect(left, top, left + width, top + height);
    }

    public static Rect fromLTRB(double left, double top, double right, double bottom) {
        return new Rect(left, top, right, bottom);
    }

    public static Rect fromCircle(Offset center, double radius) {
        return new Rect(center.dx() - radius, center.dy() - radius,
                center.dx() + radius, center.dy() + radius);
    }

    public static Rect fromCenter(Offset center, double width, double height) {
        return new Rect(center.dx() - width / 2, center.dy() - height / 2,
                center.dx() + width / 2, center.dy() + height / 2);
    }

    public static Rect fromPoints(Offset a, Offset b) {
        return new Rect(Math.min(a.dx(), b.dx()), Math.min(a.dy(), b.dy()),
                Math.max(a.dx(), b.dx()), Math.max(a.dy(), b.dy()));
    }

    public double left() {
        return left;
    }

    public double top() {
        return top;
    }

    public double right() {
        return right;
    }

    public double bottom() {
        return bottom;
    }

    public double width() {
        return right - left;
    }

    public double height() {
        return bottom - top;
    }

    public double shortestSide() {
        return Math.min(Math.abs(width()), Math.abs(height()));
    }

    public double longestSide() {
        return Math.max(Math.abs(width()), Math.abs(height()));
    }

    public boolean isEmpty() {
        return left >= right || top >= bottom;
    }

    public boolean isFinite() {
        return !Double.isInfinite(left) && !Double.isInfinite(top)
                && !Double.isInfinite(right) && !Double.isInfinite(bottom);
    }

    public boolean hasNaN() {
        return Double.isNaN(left) || Double.isNaN(top)
                || Double.isNaN(right) || Double.isNaN(bottom);
    }

    public Offset center() {
        return new Offset((left + right) / 2, (top + bottom) / 2);
    }

    public Offset topLeft() {
        return new Offset(left, top);
    }

    public Offset topCenter() {
        return new Offset((left + right) / 2, top);
    }

    public Offset topRight() {
        return new Offset(right, top);
    }

    public Offset centerLeft() {
        return new Offset(left, (top + bottom) / 2);
    }

    public Offset centerRight() {
        return new Offset(right, (top + bottom) / 2);
    }

    public Offset bottomLeft() {
        return new Offset(left, bottom);
    }

    public Offset bottomCenter() {
        return new Offset((left + right) / 2, bottom);
    }

    public Offset bottomRight() {
        return new Offset(right, bottom);
    }

    public Size size() {
        return new Size(width(), height());
    }

    public boolean contains(Offset offset) {
        return offset.dx() >= left && offset.dx() < right
                && offset.dy() >= top && offset.dy() < bottom;
    }

    public Rect translate(double translateX, double translateY) {
        return new Rect(left + translateX, top + translateY,
                right + translateX, bottom + translateY);
    }

    public Rect shift(Offset offset) {
        return translate(offset.dx(), offset.dy());
    }

    public Rect inflate(double delta) {
        return new Rect(left - delta, top - delta, right + delta, bottom + delta);
    }

    public Rect deflate(double delta) {
        return inflate(-delta);
    }

    public Rect intersect(Rect other) {
        return new Rect(Math.max(left, other.left), Math.max(top, other.top),
                Math.min(right, other.right), Math.min(bottom, other.bottom));
    }

    public Rect expandToInclude(Rect other) {
        return new Rect(Math.min(left, other.left), Math.min(top, other.top),
                Math.max(right, other.right), Math.max(bottom, other.bottom));
    }

    public boolean overlaps(Rect other) {
        return right > other.left && other.right > left
                && bottom > other.top && other.bottom > top;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rect)) {
            return false;
        }
        Rect r = (Rect) o;
        return r.left == left && r.top == top && r.right == right && r.bottom == bottom;
    }

    @Override
    public int hashCode() {
        long bits = ValueHash.bits(left);
        bits = bits * 31 + ValueHash.bits(top);
        bits = bits * 31 + ValueHash.bits(right);
        bits = bits * 31 + ValueHash.bits(bottom);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public String toString() {
        return "Rect.fromLTRB(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
}

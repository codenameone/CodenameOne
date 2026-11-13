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
package com.codename1.flutter.vectormath;

/**
 * A 2-component double vector from {@code package:vector_math}
 * ({@code Vector2}). Mutating operations ({@code add}, {@code setValues},
 * {@code multiply}, ...) change this vector in place, as vector_math's do, so
 * a cascade such as {@code v.clone()..add(w)} leaves {@code v} untouched.
 */
public class Vector2 {
    private double x;
    private double y;

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Vector2 zero() {
        return new Vector2(0, 0);
    }

    public static Vector2 all(double value) {
        return new Vector2(value, value);
    }

    public static Vector2 copy(Vector2 other) {
        return new Vector2(other.x, other.y);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public void x(double v) {
        x = v;
    }

    public void y(double v) {
        y = v;
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double length2() {
        return x * x + y * y;
    }

    public void length(double value) {
        if (value == 0) {
            setZero();
            return;
        }
        double l = length();
        if (l == 0) {
            return;
        }
        double f = value / l;
        x *= f;
        y *= f;
    }

    public void setValues(double nx, double ny) {
        x = nx;
        y = ny;
    }

    public void setZero() {
        x = 0;
        y = 0;
    }

    public void setFrom(Vector2 other) {
        x = other.x;
        y = other.y;
    }

    public Vector2 clone() {
        return new Vector2(x, y);
    }

    public void add(Vector2 o) {
        x += o.x;
        y += o.y;
    }

    public void sub(Vector2 o) {
        x -= o.x;
        y -= o.y;
    }

    public void multiply(Vector2 o) {
        x *= o.x;
        y *= o.y;
    }

    public void divide(Vector2 o) {
        x /= o.x;
        y /= o.y;
    }

    public void scale(double f) {
        x *= f;
        y *= f;
    }

    public void negate() {
        x = -x;
        y = -y;
    }

    public Vector2 scaled(double f) {
        return new Vector2(x * f, y * f);
    }

    /** Normalizes in place and answers the length before normalizing, as vector_math does. */
    public double normalize() {
        double l = length();
        if (l != 0) {
            x /= l;
            y /= l;
        }
        return l;
    }

    public Vector2 normalized() {
        Vector2 v = clone();
        v.normalize();
        return v;
    }

    public double dot(Vector2 o) {
        return x * o.x + y * o.y;
    }

    public double distanceTo(Vector2 o) {
        double dx = x - o.x;
        double dy = y - o.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public Vector2 $plus(Vector2 o) {
        return new Vector2(x + o.x, y + o.y);
    }

    public Vector2 $minus(Vector2 o) {
        return new Vector2(x - o.x, y - o.y);
    }

    public Vector2 $times(double f) {
        return new Vector2(x * f, y * f);
    }

    /** vector_math's top-level {@code radians(degrees)}. */
    public static double radians(double degrees) {
        return degrees * (Math.PI / 180.0);
    }

    /** vector_math's top-level {@code degrees(radians)}. */
    public static double degrees(double radians) {
        return radians * (180.0 / Math.PI);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vector2)) {
            return false;
        }
        Vector2 v = (Vector2) o;
        return v.x == x && v.y == y;
    }

    @Override
    public int hashCode() {
        long b = Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(y);
        return (int) (b ^ (b >>> 32));
    }

    @Override
    public String toString() {
        return "[" + x + "," + y + "]";
    }
}

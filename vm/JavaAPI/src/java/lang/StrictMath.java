/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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

package java.lang;

/// The JDK's StrictMath. Every member answers exactly as the `Math` member
/// of the same name: this runtime has one implementation of each function,
/// so the results are the same on every platform it runs on, which is what
/// StrictMath promises.
public final class StrictMath {
    private StrictMath() {
    }

    /// As `Math.E`.
    public static final double E = Math.E;

    /// As `Math.PI`.
    public static final double PI = Math.PI;

    /// As `Math.TAU`.
    public static final double TAU = Math.TAU;

    /// As `Math.abs`.
    public static double abs(double a) {
        return Math.abs(a);
    }

    /// As `Math.abs`.
    public static float abs(float a) {
        return Math.abs(a);
    }

    /// As `Math.abs`.
    public static int abs(int a) {
        return Math.abs(a);
    }

    /// As `Math.abs`.
    public static long abs(long a) {
        return Math.abs(a);
    }

    /// As `Math.ceil`.
    public static double ceil(double a) {
        return Math.ceil(a);
    }

    /// As `Math.floor`.
    public static double floor(double a) {
        return Math.floor(a);
    }

    /// As `Math.max`.
    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    /// As `Math.pow`.
    public static double pow(double a, double b) {
        return Math.pow(a, b);
    }

    /// As `Math.max`.
    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    /// As `Math.max`.
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    /// As `Math.max`.
    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    /// As `Math.min`.
    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    /// As `Math.min`.
    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    /// As `Math.min`.
    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    /// As `Math.min`.
    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    /// As `Math.cos`.
    public static double cos(double a) {
        return Math.cos(a);
    }

    /// As `Math.sin`.
    public static double sin(double a) {
        return Math.sin(a);
    }

    /// As `Math.sqrt`.
    public static double sqrt(double a) {
        return Math.sqrt(a);
    }

    /// As `Math.tan`.
    public static double tan(double a) {
        return Math.tan(a);
    }

    /// As `Math.atan`.
    public static double atan(double a) {
        return Math.atan(a);
    }

    /// As `Math.acos`.
    public static double acos(double a) {
        return Math.acos(a);
    }

    /// As `Math.asin`.
    public static double asin(double a) {
        return Math.asin(a);
    }

    /// As `Math.atan2`.
    public static double atan2(double a, double b) {
        return Math.atan2(a, b);
    }

    /// As `Math.exp`.
    public static double exp(double a) {
        return Math.exp(a);
    }

    /// As `Math.log`.
    public static double log(double a) {
        return Math.log(a);
    }

    /// As `Math.log10`.
    public static double log10(double a) {
        return Math.log10(a);
    }

    /// As `Math.toDegrees`.
    public static double toDegrees(double a) {
        return Math.toDegrees(a);
    }

    /// As `Math.toRadians`.
    public static double toRadians(double a) {
        return Math.toRadians(a);
    }

    /// As `Math.round`.
    public static long round(double a) {
        return Math.round(a);
    }

    /// As `Math.round`.
    public static int round(float a) {
        return Math.round(a);
    }

    /// As `Math.nextDown`.
    public static double nextDown(double a) {
        return Math.nextDown(a);
    }

    /// As `Math.nextAfter`.
    public static double nextAfter(double a, double b) {
        return Math.nextAfter(a, b);
    }

    /// As `Math.nextAfter`.
    public static float nextAfter(float a, double b) {
        return Math.nextAfter(a, b);
    }

    /// As `Math.random`.
    public static double random() {
        return Math.random();
    }

    /// As `Math.signum`.
    public static double signum(double a) {
        return Math.signum(a);
    }

    /// As `Math.signum`.
    public static float signum(float a) {
        return Math.signum(a);
    }

    /// As `Math.copySign`.
    public static double copySign(double a, double b) {
        return Math.copySign(a, b);
    }

    /// As `Math.copySign`.
    public static float copySign(float a, float b) {
        return Math.copySign(a, b);
    }

    /// As `Math.getExponent`.
    public static int getExponent(double a) {
        return Math.getExponent(a);
    }

    /// As `Math.getExponent`.
    public static int getExponent(float a) {
        return Math.getExponent(a);
    }

    /// As `Math.ulp`.
    public static double ulp(double a) {
        return Math.ulp(a);
    }

    /// As `Math.ulp`.
    public static float ulp(float a) {
        return Math.ulp(a);
    }

    /// As `Math.nextUp`.
    public static double nextUp(double a) {
        return Math.nextUp(a);
    }

    /// As `Math.nextUp`.
    public static float nextUp(float a) {
        return Math.nextUp(a);
    }

    /// As `Math.nextDown`.
    public static float nextDown(float a) {
        return Math.nextDown(a);
    }

    /// As `Math.scalb`.
    public static double scalb(double a, int b) {
        return Math.scalb(a, b);
    }

    /// As `Math.scalb`.
    public static float scalb(float a, int b) {
        return Math.scalb(a, b);
    }

    /// As `Math.hypot`.
    public static double hypot(double a, double b) {
        return Math.hypot(a, b);
    }

    /// As `Math.cbrt`.
    public static double cbrt(double a) {
        return Math.cbrt(a);
    }

    /// As `Math.rint`.
    public static double rint(double a) {
        return Math.rint(a);
    }

    /// As `Math.sinh`.
    public static double sinh(double a) {
        return Math.sinh(a);
    }

    /// As `Math.cosh`.
    public static double cosh(double a) {
        return Math.cosh(a);
    }

    /// As `Math.tanh`.
    public static double tanh(double a) {
        return Math.tanh(a);
    }

    /// As `Math.floorDiv`.
    public static int floorDiv(int a, int b) {
        return Math.floorDiv(a, b);
    }

    /// As `Math.floorDiv`.
    public static long floorDiv(long a, int b) {
        return Math.floorDiv(a, b);
    }

    /// As `Math.floorDiv`.
    public static long floorDiv(long a, long b) {
        return Math.floorDiv(a, b);
    }

    /// As `Math.floorMod`.
    public static int floorMod(int a, int b) {
        return Math.floorMod(a, b);
    }

    /// As `Math.floorMod`.
    public static int floorMod(long a, int b) {
        return Math.floorMod(a, b);
    }

    /// As `Math.floorMod`.
    public static long floorMod(long a, long b) {
        return Math.floorMod(a, b);
    }

    /// As `Math.ceilDiv`.
    public static int ceilDiv(int a, int b) {
        return Math.ceilDiv(a, b);
    }

    /// As `Math.ceilDiv`.
    public static long ceilDiv(long a, int b) {
        return Math.ceilDiv(a, b);
    }

    /// As `Math.ceilDiv`.
    public static long ceilDiv(long a, long b) {
        return Math.ceilDiv(a, b);
    }

    /// As `Math.ceilMod`.
    public static int ceilMod(int a, int b) {
        return Math.ceilMod(a, b);
    }

    /// As `Math.ceilMod`.
    public static int ceilMod(long a, int b) {
        return Math.ceilMod(a, b);
    }

    /// As `Math.ceilMod`.
    public static long ceilMod(long a, long b) {
        return Math.ceilMod(a, b);
    }

    /// As `Math.divideExact`.
    public static int divideExact(int a, int b) {
        return Math.divideExact(a, b);
    }

    /// As `Math.divideExact`.
    public static long divideExact(long a, long b) {
        return Math.divideExact(a, b);
    }

    /// As `Math.floorDivExact`.
    public static int floorDivExact(int a, int b) {
        return Math.floorDivExact(a, b);
    }

    /// As `Math.floorDivExact`.
    public static long floorDivExact(long a, long b) {
        return Math.floorDivExact(a, b);
    }

    /// As `Math.ceilDivExact`.
    public static int ceilDivExact(int a, int b) {
        return Math.ceilDivExact(a, b);
    }

    /// As `Math.ceilDivExact`.
    public static long ceilDivExact(long a, long b) {
        return Math.ceilDivExact(a, b);
    }

    /// As `Math.addExact`.
    public static int addExact(int a, int b) {
        return Math.addExact(a, b);
    }

    /// As `Math.addExact`.
    public static long addExact(long a, long b) {
        return Math.addExact(a, b);
    }

    /// As `Math.subtractExact`.
    public static int subtractExact(int a, int b) {
        return Math.subtractExact(a, b);
    }

    /// As `Math.subtractExact`.
    public static long subtractExact(long a, long b) {
        return Math.subtractExact(a, b);
    }

    /// As `Math.multiplyExact`.
    public static int multiplyExact(int a, int b) {
        return Math.multiplyExact(a, b);
    }

    /// As `Math.multiplyExact`.
    public static long multiplyExact(long a, int b) {
        return Math.multiplyExact(a, b);
    }

    /// As `Math.multiplyExact`.
    public static long multiplyExact(long a, long b) {
        return Math.multiplyExact(a, b);
    }

    /// As `Math.multiplyFull`.
    public static long multiplyFull(int a, int b) {
        return Math.multiplyFull(a, b);
    }

    /// As `Math.multiplyHigh`.
    public static long multiplyHigh(long a, long b) {
        return Math.multiplyHigh(a, b);
    }

    /// As `Math.unsignedMultiplyHigh`.
    public static long unsignedMultiplyHigh(long a, long b) {
        return Math.unsignedMultiplyHigh(a, b);
    }

    /// As `Math.incrementExact`.
    public static int incrementExact(int a) {
        return Math.incrementExact(a);
    }

    /// As `Math.incrementExact`.
    public static long incrementExact(long a) {
        return Math.incrementExact(a);
    }

    /// As `Math.decrementExact`.
    public static int decrementExact(int a) {
        return Math.decrementExact(a);
    }

    /// As `Math.decrementExact`.
    public static long decrementExact(long a) {
        return Math.decrementExact(a);
    }

    /// As `Math.negateExact`.
    public static int negateExact(int a) {
        return Math.negateExact(a);
    }

    /// As `Math.negateExact`.
    public static long negateExact(long a) {
        return Math.negateExact(a);
    }

    /// As `Math.toIntExact`.
    public static int toIntExact(long a) {
        return Math.toIntExact(a);
    }

    /// As `Math.absExact`.
    public static int absExact(int a) {
        return Math.absExact(a);
    }

    /// As `Math.absExact`.
    public static long absExact(long a) {
        return Math.absExact(a);
    }

    /// As `Math.clamp`.
    public static int clamp(long a, int b, int c) {
        return Math.clamp(a, b, c);
    }

    /// As `Math.clamp`.
    public static long clamp(long a, long b, long c) {
        return Math.clamp(a, b, c);
    }

    /// As `Math.clamp`.
    public static double clamp(double a, double b, double c) {
        return Math.clamp(a, b, c);
    }

    /// As `Math.clamp`.
    public static float clamp(float a, float b, float c) {
        return Math.clamp(a, b, c);
    }
}

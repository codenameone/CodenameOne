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
package com.codename1.art.effects;

/// Perlin noise as SVG's `feTurbulence` defines it, so the same seed gives the same texture
/// a browser draws.
final class Turbulence {
    private static final int SIZE = 0x100;
    private static final int MASK = 0xff;
    private static final int RAND_M = 2147483647;
    private static final int RAND_A = 16807;
    private static final int RAND_Q = 127773;
    private static final int RAND_R = 2836;
    private static final int PERLIN_N = 0x1000;

    private final int[] lattice = new int[SIZE + SIZE + 2];
    private final double[][][] gradient = new double[4][SIZE + SIZE + 2][2];

    Turbulence(int seed) {
        long s = seed;
        if (s <= 0) {
            s = -(s % (RAND_M - 1)) + 1;
        }
        if (s > RAND_M - 1) {
            s = RAND_M - 1;
        }
        for (int k = 0; k < 4; k++) {
            for (int i = 0; i < SIZE; i++) {
                lattice[i] = i;
                for (int j = 0; j < 2; j++) {
                    s = random(s);
                    gradient[k][i][j] = (double) ((s % (SIZE + SIZE)) - SIZE) / SIZE;
                }
                double len = Math.sqrt(gradient[k][i][0] * gradient[k][i][0] + gradient[k][i][1] * gradient[k][i][1]);
                if (len != 0) {
                    gradient[k][i][0] /= len;
                    gradient[k][i][1] /= len;
                }
            }
        }
        for (int i = SIZE - 1; i > 0; i--) {
            int k = lattice[i];
            s = random(s);
            int j = (int) (s % SIZE);
            lattice[i] = lattice[j];
            lattice[j] = k;
        }
        for (int i = 0; i < SIZE + 2; i++) {
            lattice[SIZE + i] = lattice[i];
            for (int k = 0; k < 4; k++) {
                gradient[k][SIZE + i][0] = gradient[k][i][0];
                gradient[k][SIZE + i][1] = gradient[k][i][1];
            }
        }
    }

    private static long random(long s) {
        long r = RAND_A * (s % RAND_Q) - RAND_R * (s / RAND_Q);
        if (r <= 0) {
            r += RAND_M;
        }
        return r;
    }

    private static double sCurve(double t) {
        return t * t * (3 - 2 * t);
    }

    private double noise2(int ch, double vx, double vy) {
        double t = vx + PERLIN_N;
        int bx0 = ((int) t) & MASK;
        int bx1 = (bx0 + 1) & MASK;
        double rx0 = t - (int) t;
        double rx1 = rx0 - 1;
        t = vy + PERLIN_N;
        int by0 = ((int) t) & MASK;
        int by1 = (by0 + 1) & MASK;
        double ry0 = t - (int) t;
        double ry1 = ry0 - 1;
        int i = lattice[bx0];
        int j = lattice[bx1];
        int b00 = lattice[i + by0];
        int b10 = lattice[j + by0];
        int b01 = lattice[i + by1];
        int b11 = lattice[j + by1];
        double sx = sCurve(rx0);
        double sy = sCurve(ry0);
        double[] q = gradient[ch][b00];
        double u = rx0 * q[0] + ry0 * q[1];
        q = gradient[ch][b10];
        double v = rx1 * q[0] + ry0 * q[1];
        double a = u + sx * (v - u);
        q = gradient[ch][b01];
        u = rx0 * q[0] + ry1 * q[1];
        q = gradient[ch][b11];
        v = rx1 * q[0] + ry1 * q[1];
        double b = u + sx * (v - u);
        return a + sy * (b - a);
    }

    double turbulence(int ch, double[] point, double fx, double fy, int octaves, boolean fractal) {
        double sum = 0;
        double vx = point[0] * fx;
        double vy = point[1] * fy;
        double ratio = 1;
        for (int o = 0; o < octaves; o++) {
            double n = noise2(ch, vx, vy);
            sum += fractal ? n / ratio : Math.abs(n) / ratio;
            vx *= 2;
            vy *= 2;
            ratio *= 2;
        }
        return sum;
    }
}

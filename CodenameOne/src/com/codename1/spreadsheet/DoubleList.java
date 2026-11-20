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
package com.codename1.spreadsheet;

/// A growing list of numbers, what the statistical functions gather their arguments into.
final class DoubleList {
    double[] data = new double[16];
    int size;

    void add(double d) {
        if (size == data.length) {
            double[] bigger = new double[size * 2];
            System.arraycopy(data, 0, bigger, 0, size);
            data = bigger;
        }
        data[size++] = d;
    }

    double sum() {
        // Kahan summation: a long column of cents must not drift in the last digits
        double sum = 0;
        double carry = 0;
        for (int i = 0; i < size; i++) {
            double y = data[i] - carry;
            double t = sum + y;
            carry = t - sum - y;
            sum = t;
        }
        return sum;
    }

    double mean() {
        if (size == 0) {
            throw EvalError.DIV0;
        }
        return sum() / size;
    }

    /// Sum of squared deviations from the mean.
    double devSq() {
        double m = mean();
        double total = 0;
        for (int i = 0; i < size; i++) {
            double d = data[i] - m;
            total += d * d;
        }
        return total;
    }

    /// A sorted copy; the list keeps the order the values were read in.
    double[] sorted() {
        double[] copy = new double[size];
        System.arraycopy(data, 0, copy, 0, size);
        java.util.Arrays.sort(copy);
        return copy;
    }
}

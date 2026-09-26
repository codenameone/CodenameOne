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
 * A point within a rectangle expressed with a text-direction-relative
 * horizontal axis (start/end) — Flutter's {@code AlignmentDirectional}.
 *
 * <p>The {@code start} coordinate is -1 and {@code end} is +1; resolving to a
 * concrete {@link Alignment} assumes left-to-right text (start = left) for
 * this milestone.</p>
 */
public class AlignmentDirectional extends Alignment {

    public static final AlignmentDirectional topStart = new AlignmentDirectional(-1, -1);
    public static final AlignmentDirectional topCenter = new AlignmentDirectional(0, -1);
    public static final AlignmentDirectional topEnd = new AlignmentDirectional(1, -1);
    public static final AlignmentDirectional centerStart = new AlignmentDirectional(-1, 0);
    public static final AlignmentDirectional center = new AlignmentDirectional(0, 0);
    public static final AlignmentDirectional centerEnd = new AlignmentDirectional(1, 0);
    public static final AlignmentDirectional bottomStart = new AlignmentDirectional(-1, 1);
    public static final AlignmentDirectional bottomCenter = new AlignmentDirectional(0, 1);
    public static final AlignmentDirectional bottomEnd = new AlignmentDirectional(1, 1);

    public AlignmentDirectional(double start, double y) {
        // start maps to the x axis under the LTR assumption of this milestone.
        super(start, y);
    }

    public double start() {
        return x();
    }

    /**
     * Resolves to a concrete {@link Alignment} assuming left-to-right text.
     */
    public Alignment resolve() {
        return new Alignment(x(), y());
    }
}

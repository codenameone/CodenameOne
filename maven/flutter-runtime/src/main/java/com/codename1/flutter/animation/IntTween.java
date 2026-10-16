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
package com.codename1.flutter.animation;

/**
 * A {@link Tween} that interpolates integers, ROUNDING to the nearest like Flutter's
 * {@code IntTween} ({@code (begin + (end - begin) * t).round()}). It truncated, which
 * is neither IntTween nor StepTween (that one floors), so a 0 to 1 tween stayed 0 until
 * the very end and every threshold landed late.
 */
public class IntTween extends Tween<Integer> {

    @Override
    public Integer lerp(double t) {
        Integer b = begin();
        Integer e = end();
        int bi = b == null ? 0 : b.intValue();
        int ei = e == null ? 0 : e.intValue();
        return (int) dart.runtime.DartRuntime.round(bi + (ei - bi) * t);
    }
}

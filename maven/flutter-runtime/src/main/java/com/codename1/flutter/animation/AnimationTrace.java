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
 * Public window onto the frame clock's counters, for the diagnostics in
 * {@code BuildOwner.frameStats()}. {@link FrameDriver} itself stays package-private:
 * nothing outside this package should be able to reach the clock, only to read it.
 */
public final class AnimationTrace {

    private AnimationTrace() {
    }

    /** Starts or stops recording clock ticks; resets the counters either way. */
    public static void trace(boolean on) {
        FrameDriver.trace(on);
    }

    /** The recorded numbers as JSON object members, without the enclosing braces. */
    public static String stats() {
        return FrameDriver.stats();
    }
}

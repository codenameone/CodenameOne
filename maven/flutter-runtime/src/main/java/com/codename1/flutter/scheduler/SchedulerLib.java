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
package com.codename1.flutter.scheduler;

/**
 * Top-level members of Flutter's {@code package:flutter/scheduler.dart} library
 * that the app references directly, mirrored as Java statics.
 *
 * <p>{@code timeDilation} slows every {@code AnimationController} in the app by
 * the given factor. It is a mutable top-level {@code double} in Flutter
 * (default {@code 1.0}); the transpiler routes both reads and writes of the
 * bare {@code timeDilation} identifier to this field.</p>
 */
public final class SchedulerLib {

    private SchedulerLib() {
    }

    /** Flutter's {@code scheduler.timeDilation}; 1.0 == real time. */
    public static double timeDilation = 1.0;
}

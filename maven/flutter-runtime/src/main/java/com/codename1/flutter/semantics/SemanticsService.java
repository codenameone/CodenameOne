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
package com.codename1.flutter.semantics;

import com.codename1.flutter.TextDirection;

/**
 * Fires accessibility announcements — Flutter's {@code SemanticsService}. The
 * settings panel announces its open/close state; this runtime records the last
 * announcement so the platform accessibility layer (or a test) can observe it,
 * but performs no screen-reader I/O in this pass.
 */
public abstract class SemanticsService {

    private static volatile String lastAnnouncement;
    private static volatile String lastTooltip;

    private SemanticsService() {
    }

    /**
     * Dart's {@code SemanticsService.announce(message, textDirection,
     * {assertiveness})}. Records the message for observation.
     */
    public static void announce(String message, TextDirection textDirection, Object assertiveness) {
        lastAnnouncement = message;
    }

    /** Dart's {@code SemanticsService.tooltip(message)}. */
    public static void tooltip(String message) {
        lastTooltip = message;
    }

    /** The most recently announced message, or null. */
    public static String lastAnnouncement() {
        return lastAnnouncement;
    }

    /** The most recently announced tooltip, or null. */
    public static String lastTooltip() {
        return lastTooltip;
    }
}

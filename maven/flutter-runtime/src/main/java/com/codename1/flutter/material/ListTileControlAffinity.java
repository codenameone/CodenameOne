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
package com.codename1.flutter.material;

/**
 * Which edge a {@code CheckboxListTile} / {@code RadioListTile} /
 * {@code SwitchListTile} puts its control on — Flutter's
 * {@code ListTileControlAffinity}.
 *
 * <p>The default, {@code platform}, is not "whatever looks reasonable": Flutter
 * resolves it to the LEADING edge for a checkbox or a radio and the TRAILING
 * edge for a switch, on every platform. Every settings list in the gallery is
 * laid out that way, and putting the control on the wrong edge mirrors the
 * whole row.</p>
 */
public enum ListTileControlAffinity {
    leading, trailing, platform;

    /**
     * Whether a control with this affinity trails.
     *
     * @param affinity      the value the app supplied, possibly null or an
     *                      unrecognised object (the transpiler hands enums
     *                      through as values, but a stub may pass anything)
     * @param platformTrails what {@code platform} means for this control —
     *                      true for a switch, false for a checkbox or radio
     */
    public static boolean isTrailing(Object affinity, boolean platformTrails) {
        if (affinity == trailing) {
            return true;
        }
        if (affinity == leading) {
            return false;
        }
        return platformTrails;
    }
}

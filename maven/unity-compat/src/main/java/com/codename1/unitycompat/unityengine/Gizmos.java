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
package com.codename1.unitycompat.unityengine;

import UnityEngine.Color;
import UnityEngine.Vector3;

/// `UnityEngine.Gizmos`: the shapes a script draws in the editor's scene
/// view. A player draws none of them, and neither does anything here: the
/// class exists so that a script with an `OnDrawGizmos` builds, and that
/// method is never called.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Gizmos {
    private static final Color color = new Color();

    private Gizmos() {
    }

    public static Color get_color(Color ret) {
        ret.$assign(color);
        return ret;
    }

    public static void set_color(Color value) {
        color.$assign(value);
    }

    public static void DrawLine(Vector3 from, Vector3 to) {
    }

    public static void DrawRay(Vector3 from, Vector3 direction) {
    }

    public static void DrawCube(Vector3 center, Vector3 size) {
    }

    public static void DrawWireCube(Vector3 center, Vector3 size) {
    }

    public static void DrawSphere(Vector3 center, float radius) {
    }

    public static void DrawWireSphere(Vector3 center, float radius) {
    }
}

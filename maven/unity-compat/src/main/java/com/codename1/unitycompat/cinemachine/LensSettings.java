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
package com.codename1.unitycompat.cinemachine;

import com.codename1.unitycompat.system.Struct;

/// `Cinemachine.LensSettings`: what a virtual camera asks of the camera's
/// lens. Only [#OrthographicSize] is used, the runtime's camera being
/// orthographic; the rest are kept so that a script which sets them
/// compiles and reads back what it wrote.
@SuppressWarnings({"PMD.MethodNamingConventions", "PMD.FieldNamingConventions"}) // C# member names
public final class LensSettings implements Struct {
    public float FieldOfView = 60f;
    public float OrthographicSize = 5f;
    public float NearClipPlane = 0.3f;
    public float FarClipPlane = 1000f;
    public float Dutch;

    public LensSettings $copy() {
        LensSettings l = new LensSettings();
        l.$assign(this);
        return l;
    }

    public void $assign(LensSettings other) {
        FieldOfView = other.FieldOfView;
        OrthographicSize = other.OrthographicSize;
        NearClipPlane = other.NearClipPlane;
        FarClipPlane = other.FarClipPlane;
        Dutch = other.Dutch;
    }

    public static void $store(LensSettings[] array, int index, LensSettings value) {
        array[index].$assign(value);
    }

    public static LensSettings[] $newArray(int length) {
        LensSettings[] a = new LensSettings[length];
        for (int i = 0; i < length; i++) {
            a[i] = new LensSettings();
        }
        return a;
    }

    @Override
    public Object $copyValue() {
        return $copy();
    }

    @Override
    public void $clear() {
        FieldOfView = 0f;
        OrthographicSize = 0f;
        NearClipPlane = 0f;
        FarClipPlane = 0f;
        Dutch = 0f;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LensSettings)) {
            return false;
        }
        LensSettings l = (LensSettings) o;
        return FieldOfView == l.FieldOfView && OrthographicSize == l.OrthographicSize
                && NearClipPlane == l.NearClipPlane && FarClipPlane == l.FarClipPlane && Dutch == l.Dutch;
    }

    @Override
    public int hashCode() {
        return (int) (OrthographicSize * 1000f) + (int) FieldOfView * 31;
    }
}

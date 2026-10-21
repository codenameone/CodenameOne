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

import com.codename1.unitycompat.system.Struct;

/// `UnityEngine.LayerMask`: a set of layers, one bit each. A struct, written
/// by hand because it needs the project's layer names, which only the
/// runtime has; it follows the protocol the translator expects of one.
///
/// A scene stores a field of this type as `m_Bits`, and the generated code
/// assigns that field by name.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class LayerMask implements Struct {
    private static final String[] names = new String[32];

    public int m_Bits;

    static void reset() {
        for (int i = 0; i < 32; i++) {
            names[i] = "";
        }
        // The layers every Unity project starts with.
        names[0] = "Default";
        names[1] = "TransparentFX";
        names[2] = "Ignore Raycast";
        names[4] = "Water";
        names[5] = "UI";
    }

    /// The project's layer names, in layer order, as its tag settings list
    /// them; an empty string for a layer that has none. Generated code
    /// calls this.
    public static void $layerNames(String[] layers) {
        for (int i = 0; i < 32; i++) {
            names[i] = i < layers.length && layers[i] != null ? layers[i] : "";
        }
    }

    public int get_value() {
        return m_Bits;
    }

    public void set_value(int value) {
        m_Bits = value;
    }

    public static int op_Implicit(LayerMask mask) {
        return mask.m_Bits;
    }

    public static LayerMask op_Implicit(int intVal, LayerMask ret) {
        ret.m_Bits = intVal;
        return ret;
    }

    /// The bits of the named layers; a name no layer has adds nothing.
    public static int GetMask(String[] layerNames) {
        if (layerNames == null) {
            throw new NullPointerException("layerNames");
        }
        int mask = 0;
        for (int i = 0; i < layerNames.length; i++) { // NOPMD ForLoopCanBeForeach
            int layer = NameToLayer(layerNames[i]);
            if (layer >= 0) {
                mask |= 1 << layer;
            }
        }
        return mask;
    }

    /// The number of the layer of a name, or -1.
    public static int NameToLayer(String layerName) {
        if (layerName == null || layerName.length() == 0) {
            return -1;
        }
        for (int i = 0; i < 32; i++) {
            if (layerName.equals(names[i])) {
                return i;
            }
        }
        return -1;
    }

    /// The name of a layer, empty for one that has none.
    public static String LayerToName(int layer) {
        return layer < 0 || layer > 31 ? "" : names[layer];
    }

    public LayerMask $copy() {
        LayerMask m = new LayerMask();
        m.m_Bits = m_Bits;
        return m;
    }

    @Override
    public java.lang.Object $copyValue() {
        return $copy();
    }

    public void $assign(LayerMask other) {
        m_Bits = other.m_Bits;
    }

    @Override
    public void $clear() {
        m_Bits = 0;
    }

    public static void $store(LayerMask[] array, int index, LayerMask value) {
        array[index].$assign(value);
    }

    public static LayerMask[] $newArray(int length) {
        LayerMask[] array = new LayerMask[length];
        for (int i = 0; i < length; i++) {
            array[i] = new LayerMask();
        }
        return array;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        return o instanceof LayerMask && ((LayerMask) o).m_Bits == m_Bits;
    }

    @Override
    public int hashCode() {
        return m_Bits;
    }

    @Override
    public String toString() {
        return String.valueOf(m_Bits);
    }
}

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

/// `UnityEngine.AnimatorStateInfo`: where an [Animator] was in its state
/// machine when it was asked.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class AnimatorStateInfo implements Struct {
    int nameHash;
    float normalizedTime;
    float length;
    float speed;
    boolean loop;
    String stateName;

    public int get_shortNameHash() {
        return nameHash;
    }

    public int get_fullPathHash() {
        return nameHash;
    }

    /// How many times over the state has played: the whole part counts the
    /// loops and the rest is how far into this one.
    public float get_normalizedTime() {
        return normalizedTime;
    }

    public float get_length() {
        return length;
    }

    public float get_speed() {
        return speed;
    }

    public boolean get_loop() {
        return loop;
    }

    public boolean IsName(String name) {
        if (stateName == null || name == null) {
            return false;
        }
        int dot = name.lastIndexOf('.');
        return stateName.equals(dot >= 0 ? name.substring(dot + 1) : name);
    }

    public AnimatorStateInfo $copy() {
        AnimatorStateInfo c = new AnimatorStateInfo();
        c.$assign(this);
        return c;
    }

    @Override
    public java.lang.Object $copyValue() {
        return $copy();
    }

    public void $assign(AnimatorStateInfo other) {
        nameHash = other.nameHash;
        normalizedTime = other.normalizedTime;
        length = other.length;
        speed = other.speed;
        loop = other.loop;
        stateName = other.stateName;
    }

    @Override
    public void $clear() {
        nameHash = 0;
        normalizedTime = 0f;
        length = 0f;
        speed = 0f;
        loop = false;
        stateName = null;
    }

    public static void $store(AnimatorStateInfo[] array, int index, AnimatorStateInfo value) {
        array[index].$assign(value);
    }

    public static AnimatorStateInfo[] $newArray(int length) {
        AnimatorStateInfo[] array = new AnimatorStateInfo[length];
        for (int i = 0; i < length; i++) {
            array[i] = new AnimatorStateInfo();
        }
        return array;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (!(o instanceof AnimatorStateInfo)) {
            return false;
        }
        AnimatorStateInfo s = (AnimatorStateInfo) o;
        return s.nameHash == nameHash && s.normalizedTime == normalizedTime && s.length == length
                && s.speed == speed && s.loop == loop;
    }

    @Override
    public int hashCode() {
        return nameHash * 31 + (int) (normalizedTime * 1000f);
    }
}

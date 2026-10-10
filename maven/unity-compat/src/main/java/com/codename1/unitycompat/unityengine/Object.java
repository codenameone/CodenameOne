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

import UnityEngine.Quaternion;
import UnityEngine.Vector3;

/// `UnityEngine.Object`: anything the engine owns and can destroy.
///
/// Every class in this package sees this one where it writes `Object`, so
/// the root of the Java hierarchy is spelled `java.lang.Object` throughout.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Object {
    private static int nextId;
    String name = "";
    boolean destroyed;
    /// Asked to be destroyed, and still alive until the frame lets go of it.
    boolean doomed;
    private int id;

    public String get_name() {
        return name;
    }

    public void set_name(String value) {
        name = value;
    }

    private static int newId() {
        return ++nextId;
    }

    public int GetInstanceID() {
        if (id == 0) {
            id = newId();
        }
        return id;
    }

    /// Destruction waits, as in Unity, until the code that asked for it has
    /// returned: the object stays usable for the rest of the frame.
    public static void Destroy(Object obj) {
        if (obj != null) {
            UnityRuntime.destroyLater(obj, 0f);
        }
    }

    public static void Destroy(Object obj, float t) {
        if (obj != null) {
            UnityRuntime.destroyLater(obj, t);
        }
    }

    public static void DestroyImmediate(Object obj) {
        if (obj != null) {
            UnityRuntime.destroyNow(obj);
        }
    }

    /// Keeps a root object, with everything below it, when another scene
    /// is loaded. `target` is the object or a component on it.
    public static void DontDestroyOnLoad(Object target) {
        if (target != null) {
            UnityRuntime.keepOnLoad(target);
        }
    }

    // The generic overloads carry the class of `T`, which the result is
    // cast to by the caller; nothing here needs it.

    public static Object Instantiate(Object original) {
        return UnityRuntime.instantiate(original, null, false, null, null, true);
    }

    public static Object Instantiate(Object original, Transform parent) {
        return UnityRuntime.instantiate(original, parent, true, null, null, false);
    }

    public static Object Instantiate(Object original, Transform parent, boolean instantiateInWorldSpace) {
        return UnityRuntime.instantiate(original, parent, true, null, null, instantiateInWorldSpace);
    }

    public static Object Instantiate(Object original, Vector3 position, Quaternion rotation) {
        return UnityRuntime.instantiate(original, null, false, position, rotation, true);
    }

    public static Object Instantiate(Object original, Vector3 position, Quaternion rotation, Transform parent) {
        return UnityRuntime.instantiate(original, parent, true, position, rotation, true);
    }

    public static java.lang.Object Instantiate(java.lang.Object original, Class type) {
        return Instantiate((Object) original);
    }

    public static java.lang.Object Instantiate(java.lang.Object original, Transform parent, Class type) {
        return Instantiate((Object) original, parent);
    }

    public static java.lang.Object Instantiate(java.lang.Object original, Transform parent,
            boolean worldPositionStays, Class type) {
        return Instantiate((Object) original, parent, worldPositionStays);
    }

    public static java.lang.Object Instantiate(java.lang.Object original, Vector3 position, Quaternion rotation,
            Class type) {
        return Instantiate((Object) original, position, rotation);
    }

    public static java.lang.Object Instantiate(java.lang.Object original, Vector3 position, Quaternion rotation,
            Transform parent, Class type) {
        return Instantiate((Object) original, position, rotation, parent);
    }

    /// The first active object of the type: a component, or a game object.
    public static java.lang.Object FindObjectOfType(Class type) {
        return UnityRuntime.findOfType(type, null);
    }

    public static java.lang.Object[] FindObjectsOfType(Class type) { // NOPMD UnnecessaryFullyQualifiedName
        java.util.ArrayList found = new java.util.ArrayList();
        UnityRuntime.findOfType(type, found);
        return found.toArray();
    }

    /// `if (thing)`: true for an object that exists and was not destroyed.
    public static boolean op_Implicit(Object exists) {
        return exists != null && !exists.destroyed;
    }

    /// A destroyed object compares equal to null, as in Unity.
    public static boolean op_Equality(Object x, Object y) {
        boolean xGone = x == null || x.destroyed;
        boolean yGone = y == null || y.destroyed;
        if (xGone || yGone) {
            return xGone && yGone;
        }
        return x == y; // NOPMD CompareObjectsWithEquals
    }

    public static boolean op_Inequality(Object x, Object y) {
        return !op_Equality(x, y);
    }
}

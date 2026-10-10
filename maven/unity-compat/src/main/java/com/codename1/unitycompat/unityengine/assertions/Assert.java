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
package com.codename1.unitycompat.unityengine.assertions;

import com.codename1.unitycompat.system.Interop;
import com.codename1.unitycompat.unityengine.Debug;

/// `UnityEngine.Assertions.Assert`.
///
/// Unity compiles every call to this class out of a release player and
/// runs them in the editor and in a development build, where a failure is
/// logged as an error and the script carries on. That second behaviour is
/// the one here: a project is developed with its assertions on, so running
/// them cannot break one, and a failure is worth a line of log.
///
/// The generic methods are erased; the class of `T` arrives last and is
/// not needed.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Assert {
    private Assert() {
    }

    private static void failed(String what, String message) {
        Debug.LogError("Assertion failed. " + what + (message == null ? "" : "\n" + message));
    }

    public static void IsTrue(boolean condition) {
        IsTrue(condition, null);
    }

    public static void IsTrue(boolean condition, String message) {
        if (!condition) {
            failed("Value was False", message);
        }
    }

    public static void IsFalse(boolean condition) {
        IsFalse(condition, null);
    }

    public static void IsFalse(boolean condition, String message) {
        if (condition) {
            failed("Value was True", message);
        }
    }

    private static boolean isNull(Object value) {
        return value == null || (value instanceof com.codename1.unitycompat.unityengine.Object
                && !com.codename1.unitycompat.unityengine.Object.op_Implicit(
                        (com.codename1.unitycompat.unityengine.Object) value));
    }

    public static void IsNull(Object value, Class type) {
        IsNull(value, null, type);
    }

    public static void IsNull(Object value, String message, Class type) {
        if (!isNull(value)) {
            failed("Value was not Null", message);
        }
    }

    public static void IsNotNull(Object value, Class type) {
        IsNotNull(value, null, type);
    }

    public static void IsNotNull(Object value, String message, Class type) {
        if (isNull(value)) {
            failed("Value was Null", message);
        }
    }

    public static void AreEqual(Object expected, Object actual, Class type) {
        AreEqual(expected, actual, null, type);
    }

    public static void AreEqual(Object expected, Object actual, String message, Class type) {
        if (!Interop.areEqual(expected, actual)) {
            failed("Values are not equal.", message);
        }
    }

    public static void AreNotEqual(Object expected, Object actual, Class type) {
        AreNotEqual(expected, actual, null, type);
    }

    public static void AreNotEqual(Object expected, Object actual, String message, Class type) {
        if (Interop.areEqual(expected, actual)) {
            failed("Values are equal.", message);
        }
    }

    private static boolean near(float a, float b, float tolerance) {
        float d = a - b;
        return (d < 0f ? -d : d) <= tolerance;
    }

    public static void AreApproximatelyEqual(float expected, float actual) {
        AreApproximatelyEqual(expected, actual, 0.00001f);
    }

    public static void AreApproximatelyEqual(float expected, float actual, String message) {
        if (!near(expected, actual, 0.00001f)) {
            failed("Values are not approximately equal.", message);
        }
    }

    public static void AreApproximatelyEqual(float expected, float actual, float tolerance) {
        if (!near(expected, actual, tolerance)) {
            failed("Values are not approximately equal.", null);
        }
    }

    public static void AreNotApproximatelyEqual(float expected, float actual) {
        AreNotApproximatelyEqual(expected, actual, 0.00001f);
    }

    public static void AreNotApproximatelyEqual(float expected, float actual, String message) {
        if (near(expected, actual, 0.00001f)) {
            failed("Values are approximately equal.", message);
        }
    }

    public static void AreNotApproximatelyEqual(float expected, float actual, float tolerance) {
        if (near(expected, actual, tolerance)) {
            failed("Values are approximately equal.", null);
        }
    }
}

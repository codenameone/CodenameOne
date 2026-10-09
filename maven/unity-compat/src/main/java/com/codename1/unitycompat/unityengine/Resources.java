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

import com.codename1.unitycompat.system.Type;

/// `UnityEngine.Resources`: the assets under a folder named `Resources`,
/// loaded by the path they have there.
///
/// Nothing is searched for when a script asks. The scene compiler knows
/// every file under those folders when the project is built and writes the
/// lookup as code, one comparison per asset, so the path is a name and the
/// answer is an object that is already part of the application.
///
/// A path is what Unity takes: relative to the `Resources` folder, with
/// `/` between folders, without the extension, and compared without regard
/// to case. A text file is a [TextAsset], an image imported as a sprite a
/// [Sprite], a sound an [AudioClip] and a prefab a [GameObject]; anything
/// else was left out when the project was built, with a line saying so.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class Resources {
    private Resources() {
    }

    /// The asset at `path`, or null.
    public static Object Load(String path) {
        return (Object) UnityRuntime.$resource(path, null);
    }

    /// `Load(path, typeof(T))`.
    public static Object Load(String path, Type type) {
        return (Object) UnityRuntime.$resource(path, type == null ? null : type.$class());
    }

    /// `Load<T>(path)`: the type argument arrives as a class.
    public static java.lang.Object Load(String path, Class type) {
        return UnityRuntime.$resource(path, type);
    }

    /// Nothing is held that could be let go of: an asset is code.
    public static void UnloadAsset(Object asset) {
    }
}

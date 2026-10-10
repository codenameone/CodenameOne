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
package com.codename1.unitycompat.unityengine.scenemanagement;

import UnityEngine.SceneManagement.Scene;
import com.codename1.unitycompat.unityengine.UnityRuntime;

/// `UnityEngine.SceneManagement.SceneManager`: the scenes of the build,
/// one loaded at a time.
///
/// The scenes are those of the project's build settings, in that order,
/// each compiled to a method. Loading one is asked for and happens at the
/// end of the frame, as in Unity: every object of the current scene is
/// destroyed, but for the roots `DontDestroyOnLoad` was called for, and the
/// new scene's are created. Additive loading is not implemented.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class SceneManager {
    private SceneManager() {
    }

    public static int get_sceneCount() {
        return UnityRuntime.$activeScene() < 0 ? 0 : 1;
    }

    public static int get_sceneCountInBuildSettings() {
        return UnityRuntime.$sceneCount();
    }

    public static void LoadScene(int sceneBuildIndex) {
        UnityRuntime.$loadScene(sceneBuildIndex);
    }

    /// By name, or by the path the build settings list it under.
    public static void LoadScene(String sceneName) {
        int n = UnityRuntime.$sceneCount();
        for (int i = 0; i < n; i++) {
            String path = UnityRuntime.$scenePath(i);
            if (path.equals(sceneName) || name(path).equals(sceneName)) {
                UnityRuntime.$loadScene(i);
                return;
            }
        }
        throw new IllegalArgumentException("Scene '" + sceneName + "' is not in the build settings");
    }

    private static String name(String path) {
        int slash = path.lastIndexOf('/');
        String file = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = file.lastIndexOf('.');
        return dot > 0 ? file.substring(0, dot) : file;
    }

    public static Scene GetActiveScene(Scene ret) {
        int index = UnityRuntime.$activeScene();
        ret.m_BuildIndex = index;
        ret.m_Name = index < 0 ? "" : name(UnityRuntime.$scenePath(index));
        return ret;
    }
}

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
package com.codename1.unity.scenecompiler;

import com.codename1.cil.UnityToolchain;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/// Which scenes make the build, for each thing the build settings of a
/// project can say: nothing because there is no such file, nothing because
/// the list is empty, and a list whose entries are switched off, gone, or
/// in part usable.
public class BuildScenesTest {
    private static final String HEAD = "%YAML 1.1\n%TAG !u! tag:unity3d.com,2011:\n--- !u!1045 &1\n"
            + "EditorBuildSettings:\n  m_ObjectHideFlags: 0\n  serializedVersion: 2\n";

    private final List<String> warnings = new ArrayList<String>();
    private final List<String> notes = new ArrayList<String>();

    private static String entry(boolean enabled, String path) {
        return "  - enabled: " + (enabled ? 1 : 0) + "\n    path: " + path
                + "\n    guid: 0123456789abcdef0123456789abcdef\n";
    }

    /// A project with the scenes `A.unity` and `B.unity` under Assets, and
    /// the build settings given, or none for null.
    private static File project(String name, String settings) throws IOException {
        File project = UnityToolchain.workDir("build-scenes-" + name);
        File scenes = new File(project, "Assets/Scenes");
        Files.createDirectories(scenes.toPath());
        Files.write(new File(scenes, "A.unity").toPath(), new byte[0]);
        Files.write(new File(scenes, "B.unity").toPath(), new byte[0]);
        if (settings != null) {
            File file = new File(project, "ProjectSettings/EditorBuildSettings.asset");
            Files.createDirectories(file.getParentFile().toPath());
            Files.write(file.toPath(), settings.getBytes(StandardCharsets.UTF_8));
        }
        return project;
    }

    private static List<File> found(File project) {
        return Arrays.asList(new File(project, "Assets/Scenes/A.unity"), new File(project, "Assets/Scenes/B.unity"));
    }

    private List<File> build(File project) throws IOException {
        return SceneCompiler.buildScenes(project, found(project), warnings, notes);
    }

    @Test
    public void aProjectWithoutBuildSettingsBuildsEveryScene() throws IOException {
        File project = project("absent", null);
        Assert.assertEquals(found(project), build(project));
        Assert.assertTrue(warnings.toString(), warnings.isEmpty());
        Assert.assertTrue(notes.toString(), notes.isEmpty());
    }

    @Test
    public void anEmptyListBuildsEverySceneAndSaysWhichStarts() throws IOException {
        String[] empty = {HEAD + "  m_Scenes: []\n  m_configObjects: {}\n", HEAD + "  m_Scenes:\n  m_configObjects: {}\n"};
        for (int i = 0; i < empty.length; i++) {
            notes.clear();
            File project = project("empty" + i, empty[i]);
            Assert.assertEquals(found(project), build(project));
            Assert.assertTrue(warnings.toString(), warnings.isEmpty());
            Assert.assertEquals(notes.toString(), 1, notes.size());
            Assert.assertTrue(notes.get(0), notes.get(0).endsWith("starts with Assets/Scenes/A.unity"));
        }
    }

    @Test
    public void aListOfScenesAllSwitchedOffStopsTheBuild() throws IOException {
        File project = project("disabled", HEAD + "  m_Scenes:\n" + entry(false, "Assets/Scenes/A.unity")
                + entry(false, "Assets/Scenes/B.unity") + "  m_configObjects: {}\n");
        try {
            build(project);
            Assert.fail("built with every listed scene switched off");
        } catch (SceneCompiler.SceneException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Assets/Scenes/A.unity (switched off)"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Assets/Scenes/B.unity (switched off)"));
        }
    }

    @Test
    public void aListOfScenesAllGoneStopsTheBuild() throws IOException {
        File project = project("missing", HEAD + "  m_Scenes:\n" + entry(true, "Assets/Scenes/SampleScene.unity")
                + entry(false, "Assets/Scenes/B.unity") + "  m_configObjects: {}\n");
        try {
            build(project);
            Assert.fail("built with no listed scene in the project");
        } catch (SceneCompiler.SceneException e) {
            Assert.assertTrue(e.getMessage(),
                    e.getMessage().contains("Assets/Scenes/SampleScene.unity (not in the project)"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("Assets/Scenes/B.unity (switched off)"));
        }
    }

    @Test
    public void theScenesSwitchedOnAreBuiltInTheOrderOfTheList() throws IOException {
        File project = project("some", HEAD + "  m_Scenes:\n" + entry(true, "Assets/Scenes/B.unity")
                + entry(true, "Assets/Scenes/Gone.unity") + entry(false, "Assets/Scenes/A.unity")
                + "  m_configObjects: {}\n");
        Assert.assertEquals(Arrays.asList(new File(project, "Assets/Scenes/B.unity")), build(project));
        Assert.assertEquals(warnings.toString(), 1, warnings.size());
        Assert.assertTrue(warnings.get(0), warnings.get(0).contains("Assets/Scenes/Gone.unity"));
        Assert.assertTrue(notes.toString(), notes.isEmpty());
    }
}

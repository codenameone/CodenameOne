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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

/// `AddComponent<T>()` can make every component a script can name.
///
/// The runtime creates its own components from a list of them in
/// `UnityRuntime.newComponent`, written by hand because nothing may look a
/// class up by name on a device. A component added to the runtime and not to
/// that list compiles, loads from a scene, and throws the first time a script
/// adds one in code. This reads the sources of both sides -- the runtime's
/// classes and the reference assembly scripts compile against -- and holds
/// the list to them: every class below `Component` that is not a base type is
/// on it, and the base types are named here, so that a new one is a decision.
public class ComponentRegistryTest {
    /// What nothing is an instance of by itself. `AddComponent` of one of
    /// these is refused by name in `GameObject.AddComponent`.
    private static final Set<String> BASE_TYPES = new HashSet<String>(Arrays.asList(
            // A script is created by the factory the scene compiler generates.
            "MonoBehaviour", "Behaviour",
            // Every object has the one it was made with.
            "Transform", "RectTransform",
            "Collider2D", "Effector2D", "Renderer", "GridLayout", "TilemapBase", "TilemapColliderBase"));

    private static final Pattern JAVA_CLASS = Pattern.compile(
            "(?m)^public ((?:abstract |final )*)class (\\w+)(?:<[^>]*>)? extends ([\\w.]+)");
    private static final Pattern CSHARP_CLASS = Pattern.compile(
            "public ((?:abstract |sealed |partial )*)class (\\w+)(?:<[^>]*>)?\\s*:\\s*([\\w.]+)");
    private static final Pattern REGISTERED = Pattern.compile("type == ([\\w.]+)\\.class");

    private static void walk(File dir, List<File> out) {
        File[] files = dir.listFiles();
        Assert.assertNotNull("not a directory: " + dir, files);
        for (File f : files) {
            if (f.isDirectory()) {
                walk(f, out);
            } else if (f.getName().endsWith(".java")) {
                out.add(f);
            }
        }
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static String simple(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /// The classes of `text` by simple name, each with the class it extends;
    /// the abstract ones are added to `abstracts`.
    private static void classes(Pattern declaration, String text, Map<String, String> parents, Set<String> abstracts) {
        Matcher m = declaration.matcher(text);
        while (m.find()) {
            parents.put(m.group(2), simple(m.group(3)));
            if (m.group(1).contains("abstract")) {
                abstracts.add(m.group(2));
            }
        }
    }

    /// The classes that can be added: below `Component`, not abstract and
    /// not a base type.
    private static Set<String> addable(Map<String, String> parents, Set<String> abstracts) {
        Set<String> out = new TreeSet<String>();
        for (String name : parents.keySet()) {
            String at = parents.get(name);
            while (at != null && !"Component".equals(at)) {
                at = parents.get(at);
            }
            if (at != null && !abstracts.contains(name) && !BASE_TYPES.contains(name)) {
                out.add(name);
            }
        }
        return out;
    }

    @Test
    public void everyComponentOfTheRuntimeCanBeAdded() throws IOException {
        File module = UnityToolchain.runtimeModule();
        File sources = new File(module, "src/main/java/com/codename1/unitycompat");
        Map<String, String> parents = new HashMap<String, String>();
        Set<String> abstracts = new HashSet<String>();
        List<File> files = new ArrayList<File>();
        walk(sources, files);
        for (File f : files) {
            classes(JAVA_CLASS, read(f), parents, abstracts);
        }
        Set<String> runtime = addable(parents, abstracts);

        Map<String, String> referenceParents = new HashMap<String, String>();
        Set<String> referenceAbstracts = new HashSet<String>();
        classes(CSHARP_CLASS, read(new File(module, "src/main/csharp/UnityEngine/Engine.cs")), referenceParents,
                referenceAbstracts);
        Set<String> reference = addable(referenceParents, referenceAbstracts);

        String text = read(new File(sources, "unityengine/UnityRuntime.java"));
        int from = text.indexOf("static Component newComponent(Class type) {");
        Assert.assertTrue("UnityRuntime.newComponent was not found", from >= 0);
        int to = text.indexOf("\n    }\n", from);
        Set<String> registered = new TreeSet<String>();
        Matcher m = REGISTERED.matcher(text.substring(from, to));
        while (m.find()) {
            registered.add(simple(m.group(1)));
        }

        // A sanity floor: a pattern that stopped matching would otherwise
        // compare three empty sets and pass.
        Assert.assertTrue("only " + runtime.size() + " components were found in the runtime", runtime.size() >= 30);
        Assert.assertTrue(runtime.toString(), runtime.contains("AudioSource") && runtime.contains("TextMeshPro"));
        Assert.assertEquals("the components scripts compile against (Engine.cs) and the runtime's classes", reference,
                runtime);
        Assert.assertEquals("UnityRuntime.newComponent and the runtime's components", runtime, registered);
    }
}

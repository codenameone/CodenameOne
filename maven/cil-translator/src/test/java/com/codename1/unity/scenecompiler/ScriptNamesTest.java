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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

/// Two scripts with one file name. Unity tells them apart by the file's
/// GUID; a class is found by the file's name, which here two classes have,
/// each in the namespace of its folder. The compiler's debug information
/// says which file each class was written in, and a component gets the
/// class of the file its GUID names. Without that information the build
/// warns and says which class it took.
public class ScriptNamesTest {
    private static final String NEAR = "e1000000000000000000000000000001";
    private static final String FAR = "e1000000000000000000000000000002";

    private static void write(File file, String text) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    private static void script(File assets, String folder, String guid, String body) throws IOException {
        File file = new File(assets, folder + "/Mover.cs");
        write(file, "using UnityEngine;\nnamespace " + folder + "\n{\n    public class Mover : MonoBehaviour\n    {\n"
                + body + "    }\n}\n");
        write(new File(assets, folder + "/Mover.cs.meta"), "fileFormatVersion: 2\nguid: " + guid + "\nMonoImporter:\n"
                + "  userData:\n");
    }

    private static String object(int id, String name, String guid, String field) {
        return "--- !u!1 &" + id + "\nGameObject:\n  m_Component:\n  - component: {fileID: " + (id + 1) + "}\n"
                + "  - component: {fileID: " + (id + 2) + "}\n  m_Layer: 0\n  m_Name: " + name + "\n"
                + "  m_TagString: Untagged\n  m_IsActive: 1\n"
                + "--- !u!4 &" + (id + 1) + "\nTransform:\n  m_GameObject: {fileID: " + id + "}\n"
                + "  m_LocalRotation: {x: 0, y: 0, z: 0, w: 1}\n  m_LocalPosition: {x: 0, y: 0, z: 0}\n"
                + "  m_LocalScale: {x: 1, y: 1, z: 1}\n  m_Children: []\n  m_Father: {fileID: 0}\n"
                + "--- !u!114 &" + (id + 2) + "\nMonoBehaviour:\n  m_GameObject: {fileID: " + id + "}\n  m_Enabled: 1\n"
                + "  m_Script: {fileID: 11500000, guid: " + guid + ", type: 3}\n  " + field + "\n";
    }

    /// The variable the generated source keeps a component of a class in.
    private static String component(String source, String type) {
        Matcher m = Pattern.compile(Pattern.quote(type) + " (\\w+) = new " + Pattern.quote(type) + "\\(\\);")
                .matcher(source);
        return m.find() ? m.group(1) : null;
    }

    @Test
    public void aComponentGetsTheClassOfItsOwnFile() throws Exception {
        UnityToolchain.dotnet();
        File work = UnityToolchain.workDir("script-names");
        File project = new File(work, "project");
        File assets = new File(project, "Assets");
        // The one in Near has a method, so a source position; the one in
        // Far has an initializer, whose position its constructor carries.
        script(assets, "Near", NEAR, "        public int steps;\n        private void Start()\n        {\n"
                + "            Debug.Log(\"near \" + steps);\n        }\n");
        script(assets, "Far", FAR, "        public float reach = 2.5f;\n");
        write(new File(assets, "Scenes/Main.unity"), "%YAML 1.1\n%TAG !u! tag:unity3d.com,2011:\n"
                + object(100, "FarOne", FAR, "reach: 7.5") + object(200, "NearOne", NEAR, "steps: 3"));
        File csharp = new File(UnityToolchain.runtimeModule(), "src/main/csharp");
        File csproj = new File(project, "Names.csproj");
        write(csproj, "<Project Sdk=\"Microsoft.NET.Sdk\">\n  <PropertyGroup>\n"
                + "    <OutputType>Library</OutputType>\n    <TargetFramework>netstandard2.1</TargetFramework>\n"
                + "    <LangVersion>9.0</LangVersion>\n    <AssemblyName>Assembly-CSharp</AssemblyName>\n"
                + "    <Deterministic>true</Deterministic>\n    <DebugType>portable</DebugType>\n"
                + "    <GenerateAssemblyInfo>false</GenerateAssemblyInfo>\n"
                + "    <EnableDefaultCompileItems>false</EnableDefaultCompileItems>\n  </PropertyGroup>\n"
                + "  <ItemGroup>\n    <Compile Include=\"Assets/**/*.cs\" />\n"
                + "    <ProjectReference Include=\"" + new File(csharp, "UnityEngine/UnityEngine.csproj") + "\" />\n"
                + "    <ProjectReference Include=\"" + new File(csharp, "UnityEngine.Values/UnityEngine.Values.csproj")
                + "\" />\n  </ItemGroup>\n</Project>\n");
        UnityToolchain.dotnetBuild(csproj, work);
        File valuesBin = new File(csharp, "UnityEngine.Values/bin/Release/netstandard2.1");
        File netstandard = new File(valuesBin, "netstandard-ref/netstandard.dll");
        File values = new File(valuesBin, "Codename1.UnityValues.dll");
        File engine = new File(csharp, "UnityEngine/bin/Release/netstandard2.1/UnityEngine.dll");
        File scripts = new File(project, "bin/Release/netstandard2.1/Assembly-CSharp.dll");
        File pdb = new File(project, "bin/Release/netstandard2.1/Assembly-CSharp.pdb");
        Assert.assertTrue("the compiler wrote no " + pdb, pdb.isFile());

        SceneCompiler compiler = new SceneCompiler();
        String source = compiler.compile(project, Arrays.asList(scripts, values), Arrays.asList(engine, netstandard));
        Assert.assertEquals("warnings", "[]", compiler.warnings().toString());
        // Each field is written to the class that declares it: a component
        // given the other file's class would have no such field.
        Assert.assertTrue(source, component(source, "Far.Mover") != null && component(source, "Near.Mover") != null);
        Assert.assertTrue(source, source.contains(component(source, "Far.Mover") + ".reach = 7.5f;"));
        Assert.assertTrue(source, source.contains(component(source, "Near.Mover") + ".steps = "));

        // Without the debug information nothing says which is which: the
        // build takes the first, as it used to, and no longer in silence.
        Files.delete(pdb.toPath());
        compiler = new SceneCompiler();
        compiler.compile(project, Arrays.asList(scripts, values), Arrays.asList(engine, netstandard));
        String warnings = compiler.warnings().toString();
        // Which is first is the order the compiler was given the files in.
        Assert.assertTrue(warnings, warnings.contains("Assets/Near/Mover.cs: 2 classes are named Mover ("));
        Assert.assertTrue(warnings, warnings.contains(") and the debug information of the scripts does not say which of"
                + " them this file declares; its components were given "));
        Assert.assertTrue(warnings, warnings.contains("Assets/Far/Mover.cs: 2 classes are named Mover"));
    }
}

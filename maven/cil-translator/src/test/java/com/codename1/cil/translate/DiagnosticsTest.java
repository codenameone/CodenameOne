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
package com.codename1.cil.translate;

import com.codename1.cil.UnityToolchain;
import com.codename1.cil.metadata.PortablePdb;
import com.codename1.unity.scenecompiler.SceneCompiler;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/// What a build says about a project it cannot carry over whole: the lines
/// a developer reads, held to their text.
///
/// A line that names a file ID, or a method of the base library with nothing
/// to say who called it, sends its reader to search a project by hand. Each
/// of these says where: the GameObject by its name, the script by its file
/// and line.
public class DiagnosticsTest {
    private static final String TRANSLATOR = "com.codename1.cil.translate.Translator";

    private static File csharp() {
        return new File(UnityToolchain.runtimeModule(), "src/main/csharp");
    }

    /// A project file that compiles sources where they are, against the
    /// reference assemblies, with a portable PDB beside the assembly.
    private static File project(File work, String sources, boolean unity) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<Project Sdk=\"Microsoft.NET.Sdk\">\n  <PropertyGroup>\n")
                .append("    <OutputType>Library</OutputType>\n")
                .append("    <TargetFramework>netstandard2.1</TargetFramework>\n")
                .append("    <LangVersion>9.0</LangVersion>\n    <Nullable>disable</Nullable>\n")
                .append("    <ImplicitUsings>disable</ImplicitUsings>\n")
                .append("    <AssemblyName>Assembly-CSharp</AssemblyName>\n")
                .append("    <DebugType>portable</DebugType>\n    <Optimize>true</Optimize>\n")
                .append("    <GenerateAssemblyInfo>false</GenerateAssemblyInfo>\n")
                .append("    <EnableDefaultCompileItems>false</EnableDefaultCompileItems>\n")
                .append("  </PropertyGroup>\n  <ItemGroup>\n")
                .append("    <Compile Include=\"").append(sources).append("\" />\n");
        if (unity) {
            sb.append("    <ProjectReference Include=\"").append(new File(csharp(),
                    "UnityEngine/UnityEngine.csproj").getAbsolutePath()).append("\" />\n");
        }
        sb.append("    <ProjectReference Include=\"").append(new File(csharp(),
                "UnityEngine.Values/UnityEngine.Values.csproj").getAbsolutePath()).append("\" />\n")
                .append("  </ItemGroup>\n</Project>\n");
        File dir = new File(work, "cs");
        Files.createDirectories(dir.toPath());
        File file = new File(dir, "Assembly-CSharp.csproj");
        Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static File values() {
        return new File(csharp(), "UnityEngine.Values/bin/Release/netstandard2.1/Codename1.UnityValues.dll");
    }

    private static File netstandard() {
        return new File(csharp(), "UnityEngine.Values/bin/Release/netstandard2.1/netstandard-ref/netstandard.dll");
    }

    private static File engine() {
        return new File(csharp(), "UnityEngine/bin/Release/netstandard2.1/UnityEngine.dll");
    }

    private static void assertHas(List<String> lines, String line) {
        Assert.assertTrue("no line \"" + line + "\" in:\n" + lines, lines.contains(line));
    }

    /// A member the base library lacks is reported with the file and line
    /// of each use, read from the PDB the C# compiler wrote beside the
    /// assembly, and once however often it is used.
    @Test
    public void missingMemberNamesTheFileAndLineOfEachUse() throws Exception {
        UnityToolchain.dotnet();
        File work = UnityToolchain.workDir("diagnostics-missing");
        File source = new File(work, "src/Lacking.cs");
        Files.createDirectories(source.getParentFile().toPath());
        Files.write(source.toPath(), ("public static class Lacking\n"
                + "{\n"
                + "    public static string First()\n"
                + "    {\n"
                + "        return System.Environment.MachineName;\n"
                + "    }\n"
                + "\n"
                + "    public static int Second(int n)\n"
                + "    {\n"
                + "        int twice = n * 2;\n"
                + "        return twice + System.Environment.MachineName.Length;\n"
                + "    }\n"
                + "}\n").getBytes(StandardCharsets.UTF_8));
        UnityToolchain.dotnetBuild(project(work, source.getAbsolutePath(), false), work);
        File assembly = new File(work, "cs/bin/Release/netstandard2.1/Assembly-CSharp.dll");
        Assert.assertTrue("the C# compiler wrote no PDB beside the assembly",
                new File(assembly.getParentFile(), "Assembly-CSharp.pdb").isFile());
        File runtime = new File(work, "runtime");
        UnityToolchain.javac(work, "javac-base-library", runtime, null, new File(UnityToolchain.runtimeModule(),
                "src/main/java/com/codename1/unitycompat/system"));

        Translator t = new Translator();
        t.index.addDirectory(runtime);
        t.run(Arrays.asList(assembly), Arrays.asList(netstandard()));
        List<String> missing = t.missingMembers();
        Assert.assertEquals("one member, used twice:\n" + missing, 3, missing.size());
        Assert.assertTrue(missing.get(0), missing.get(0).contains("get_MachineName"));
        assertHas(missing, "    used by Lacking.cs:5, in Lacking::First() : string");
        assertHas(missing, "    used by Lacking.cs:11, in Lacking::Second(i4) : i4");
    }

    /// An assembly with no PDB beside it still says which method.
    @Test
    public void missingMemberWithoutDebugInformationNamesTheMethod() throws Exception {
        UnityToolchain.dotnet();
        File work = UnityToolchain.workDir("diagnostics-nopdb");
        File source = new File(work, "src/Lacking.cs");
        Files.createDirectories(source.getParentFile().toPath());
        Files.write(source.toPath(), ("public static class Lacking\n{\n    public static string First()\n    {\n"
                + "        return System.Environment.MachineName;\n    }\n}\n").getBytes(StandardCharsets.UTF_8));
        UnityToolchain.dotnetBuild(project(work, source.getAbsolutePath(), false), work);
        File assembly = new File(work, "cs/bin/Release/netstandard2.1/Assembly-CSharp.dll");
        Files.delete(new File(assembly.getParentFile(), "Assembly-CSharp.pdb").toPath());
        File runtime = new File(work, "runtime");
        UnityToolchain.javac(work, "javac-base-library", runtime, null, new File(UnityToolchain.runtimeModule(),
                "src/main/java/com/codename1/unitycompat/system"));
        Translator t = new Translator();
        t.index.addDirectory(runtime);
        t.run(Arrays.asList(assembly), Arrays.asList(netstandard()));
        assertHas(t.missingMembers(), "    used by Lacking::First() : string");
    }

    /// Bytes that are not a portable PDB are no debug information, not an
    /// error: a Windows PDB can sit under the same name.
    @Test
    public void whatIsNotAPortablePdbIsIgnored() {
        Assert.assertNull(PortablePdb.read(new byte[0]));
        Assert.assertNull(PortablePdb.read("Microsoft C/C++ MSF 7.00\r\n".getBytes(StandardCharsets.US_ASCII)));
        byte[] truncated = {0x42, 0x53, 0x4a, 0x42, 1, 0, 1, 0, 0, 0, 0, 0, 100, 0, 0, 0};
        Assert.assertNull(PortablePdb.read(truncated));
    }

    /// The host2d sample: a message nothing sends is named with its script
    /// and line; a component whose script is not in the project is left out
    /// with a warning that names its GameObject, and by its type where it
    /// is one of Unity's own; and a field that pointed at it is null.
    @Test
    public void unsentMessagesAndUnknownScriptsAreWarnings() throws Exception {
        UnityToolchain.dotnet();
        File work = UnityToolchain.workDir("diagnostics-host2d");
        File sample = new File(UnityToolchain.samples(), "host2d");
        UnityToolchain.dotnetBuild(project(work, new File(sample, "Assets").getAbsolutePath() + "/**/*.cs", true),
                work);
        File scripts = new File(work, "cs/bin/Release/netstandard2.1/Assembly-CSharp.dll");
        File javaSources = new File(UnityToolchain.runtimeModule(), "src/main/java/com/codename1/unitycompat");
        File runtime = new File(work, "runtime");
        UnityToolchain.javac(work, "javac-base-library", runtime, null, new File(javaSources, "system"));
        File valueClasses = new File(work, "values");
        UnityToolchain.tool(work, "translate-values", TRANSLATOR, Arrays.asList("--out",
                valueClasses.getAbsolutePath(), "--runtime", runtime.getAbsolutePath(), "--ref",
                netstandard().getAbsolutePath(), values().getAbsolutePath()));
        UnityToolchain.javac(work, "javac-engine", runtime, UnityToolchain.path(runtime, valueClasses,
                UnityToolchain.core()), new File(javaSources, "unityengine"),
                new File(javaSources, "tmpro"), new File(javaSources, "cinemachine"));

        Translator t = new Translator();
        t.index.addDirectory(runtime);
        t.run(Arrays.asList(values(), scripts), Arrays.asList(engine(), netstandard()));
        Assert.assertEquals("the sample uses nothing the runtime lacks", Arrays.asList(), t.missingMembers());
        List<String> expected = UnityToolchain.lines(UnityToolchain.read(new File(sample, "expected-warnings.txt")));
        // OnMouseDown, OnMouseUp, OnApplicationPause and OnApplicationFocus
        // are declared by the same scripts and sent, so they are not here.
        Assert.assertEquals(Arrays.asList(expected.get(0).substring("warning: ".length())), t.warnings());

        SceneCompiler compiler = new SceneCompiler();
        String source = compiler.compile(sample, Arrays.asList(scripts, values()), Arrays.asList(engine(),
                netstandard()));
        List<String> warnings = compiler.warnings();
        Assert.assertEquals(warnings.toString(), expected.size() - 1, warnings.size());
        for (int i = 1; i < expected.size(); i++) {
            assertHas(warnings, expected.get(i).substring("warning: ".length()));
        }
        Assert.assertTrue("the field that pointed at the slider is not null", source.contains(".gauge = null;"));
        Assert.assertTrue("the field beside it was lost", source.contains(".total = (int) 5L;"));
        Assert.assertFalse("a component nothing implements was built", source.contains("Slider"));
    }
}

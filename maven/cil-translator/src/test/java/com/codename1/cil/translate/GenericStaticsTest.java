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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/// The static state of a generic type belongs to each constructed type, and
/// a program for which that cannot be arranged does not build.
///
/// What works is held by the console sample, whose output is compared with
/// .NET's. These are the cases with no translation: each must stop the build
/// and name the member and the type, because the alternative is one variable
/// shared by every constructed type, which runs and answers wrongly.
public class GenericStaticsTest {
    private static File netstandard() {
        return new File(UnityToolchain.runtimeModule(),
                "src/main/csharp/UnityEngine.Values/bin/Release/netstandard2.1/netstandard-ref/netstandard.dll");
    }

    private static File compile(File work, String code) throws IOException, InterruptedException {
        File source = new File(work, "src/Code.cs");
        Files.createDirectories(source.getParentFile().toPath());
        Files.write(source.toPath(), code.getBytes(StandardCharsets.UTF_8));
        String project = "<Project Sdk=\"Microsoft.NET.Sdk\">\n  <PropertyGroup>\n"
                + "    <OutputType>Library</OutputType>\n    <TargetFramework>netstandard2.1</TargetFramework>\n"
                + "    <LangVersion>9.0</LangVersion>\n    <Nullable>disable</Nullable>\n"
                + "    <ImplicitUsings>disable</ImplicitUsings>\n    <AssemblyName>Assembly-CSharp</AssemblyName>\n"
                + "    <Optimize>true</Optimize>\n    <GenerateAssemblyInfo>false</GenerateAssemblyInfo>\n"
                + "    <EnableDefaultCompileItems>false</EnableDefaultCompileItems>\n  </PropertyGroup>\n"
                + "  <ItemGroup>\n    <Compile Include=\"" + source.getAbsolutePath() + "\" />\n"
                + "  </ItemGroup>\n</Project>\n";
        File file = new File(work, "cs/Assembly-CSharp.csproj");
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), project.getBytes(StandardCharsets.UTF_8));
        UnityToolchain.dotnetBuild(file, work);
        return new File(work, "cs/bin/Release/netstandard2.1/Assembly-CSharp.dll");
    }

    /// Translates `code` and answers what the build said, or fails when it
    /// said nothing.
    private static String refusal(String name, String code) throws Exception {
        UnityToolchain.dotnet();
        File work = UnityToolchain.workDir(name);
        File assembly = compile(work, code);
        File runtime = new File(work, "runtime");
        UnityToolchain.javac(work, "javac-base-library", runtime, null, new File(UnityToolchain.runtimeModule(),
                "src/main/java/com/codename1/unitycompat/system"));
        Translator t = new Translator();
        t.index.addDirectory(runtime);
        try {
            t.run(Arrays.asList(assembly), Arrays.asList(netstandard()));
        } catch (TranslationException e) {
            return e.getMessage();
        }
        Assert.fail("the translation went through:\n" + code);
        return null;
    }

    private static void assertNames(String message, String... parts) {
        for (String part : parts) {
            Assert.assertTrue("\"" + part + "\" is not in: " + message, message.contains(part));
        }
    }

    /// A struct has no room for the reference to its type's state: a field
    /// would be copied with every assignment and missing from `default`.
    @Test
    public void instanceMethodOfAGenericStructIsRefused() throws Exception {
        String message = refusal("generic-statics-struct", "public struct Tally<T>\n{\n"
                + "    public static int Count;\n    public int Mine;\n"
                + "    public int Bump()\n    {\n        Count++;\n        return Count + Mine;\n    }\n}\n"
                + "public static class Use\n{\n    public static int Run()\n    {\n"
                + "        return new Tally<int>().Bump() + new Tally<string>().Bump();\n    }\n}\n");
        assertNames(message, "Tally`1::Bump", "Code.cs:7", "generic struct Tally`1");
    }

    /// An override has the signature of what it overrides, so a virtual
    /// generic method cannot be handed the state its type argument selects.
    @Test
    public void virtualGenericMethodNamingAStaticIsRefused() throws Exception {
        String message = refusal("generic-statics-virtual", "public static class Slot<T>\n{\n"
                + "    public static T Value;\n}\n"
                + "public class Reader\n{\n    public virtual T Read<T>()\n    {\n        return Slot<T>.Value;\n"
                + "    }\n}\n");
        assertNames(message, "Reader", "Read", "Slot`1", "virtual generic method");
    }

    /// `Chain<T>` names `Chain<List<T>>`: every constructed type needs the
    /// next one, and there is no last.
    @Test
    public void endlessFamilyOfConstructedTypesIsRefused() throws Exception {
        String message = refusal("generic-statics-endless", "using System.Collections.Generic;\n"
                + "public class Chain<T>\n{\n    public static int Made;\n"
                + "    public static int Next(int depth)\n    {\n        Made++;\n"
                + "        return depth == 0 ? Made : Chain<List<T>>.Next(depth - 1);\n    }\n}\n"
                + "public static class Use\n{\n    public static int Run()\n    {\n"
                + "        return Chain<int>.Next(3);\n    }\n}\n");
        assertNames(message, "Chain`1", "endless family");
    }

    /// A program with generic types and no static state in any of them is
    /// translated as it always was: no context class, no hidden parameter.
    @Test
    public void genericTypesWithoutStaticStateCostNothing() throws Exception {
        UnityToolchain.dotnet();
        File work = UnityToolchain.workDir("generic-statics-none");
        File assembly = compile(work, "public class Box<T>\n{\n    public T Item;\n"
                + "    public static Box<T> Of(T item)\n    {\n        Box<T> b = new Box<T>();\n"
                + "        b.Item = item;\n        return b;\n    }\n}\n"
                + "public static class Use\n{\n    public static int Total;\n    public static int Run()\n    {\n"
                + "        Total += Box<int>.Of(3).Item;\n        return Total;\n    }\n}\n");
        File runtime = new File(work, "runtime");
        UnityToolchain.javac(work, "javac-base-library", runtime, null, new File(UnityToolchain.runtimeModule(),
                "src/main/java/com/codename1/unitycompat/system"));
        File out = new File(work, "classes");
        UnityToolchain.tool(work, "translate", "com.codename1.cil.translate.Translator", Arrays.asList("--out",
                out.getAbsolutePath(), "--runtime", runtime.getAbsolutePath(), "--ref",
                netstandard().getAbsolutePath(), assembly.getAbsolutePath()));
        String[] classes = new File(out, "global").list();
        Assert.assertNotNull("no classes in package global", classes);
        Arrays.sort(classes);
        Assert.assertEquals(Arrays.asList("Box_1.class", "Use.class"), Arrays.asList(classes));
    }
}

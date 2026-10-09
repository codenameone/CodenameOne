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
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/// The console sample, a program written to exercise the translator one
/// language feature at a time, compiled by the C# compiler, translated, and run
/// on a JVM that verifies every class. Its output must equal
/// `expected-output.txt`, which is what the same program prints under
/// `dotnet run`.
public class ConsoleSampleTest {
    @Test
    public void translatedProgramPrintsWhatDotnetPrints() throws Exception {
        UnityToolchain.dotnet();
        File work = UnityToolchain.workDir("console");
        File sample = new File(UnityToolchain.samples(), "console");
        UnityToolchain.dotnetBuild(new File(sample, "Console.csproj"), work);
        File bin = new File(sample, "bin/Release/netstandard2.1");

        File runtime = new File(work, "runtime");
        UnityToolchain.javac(work, "javac-runtime", runtime, null,
                new File(UnityToolchain.runtimeModule(), "src/main/java/com/codename1/unitycompat/system"));

        File classes = new File(work, "classes");
        UnityToolchain.tool(work, "translate", "com.codename1.cil.translate.Translator", Arrays.asList(
                "--out", classes.getAbsolutePath(), "--runtime", runtime.getAbsolutePath(),
                "--ref", new File(bin, "netstandard-ref/netstandard.dll").getAbsolutePath(),
                new File(bin, "SpikeConsole.dll").getAbsolutePath()));

        UnityToolchain.Result run = UnityToolchain.java(work, "run", UnityToolchain.path(classes, runtime),
                "Spike.Program");
        run.assertOk("the translated program");
        Assert.assertEquals("the translated program wrote to stderr", "", run.err);
        UnityToolchain.assertSameLines("console sample output",
                UnityToolchain.read(new File(sample, "expected-output.txt")), run.out);
    }
}

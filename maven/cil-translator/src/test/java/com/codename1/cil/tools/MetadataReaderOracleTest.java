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
package com.codename1.cil.tools;

import com.codename1.cil.UnityToolchain;
import com.codename1.cil.metadata.CilAssembly;
import com.codename1.cil.metadata.Universe;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/// The metadata reader against an implementation that shares no code with it:
/// the console sample's assembly, printed by [MetadataDump] through our reader
/// and by `scripts/unity-compat-samples/metadata-dump` through .NET's own
/// `System.Reflection.Metadata`, must come out line for line the same.
public class MetadataReaderOracleTest {
    @Test
    public void readerAgreesWithSystemReflectionMetadata() throws Exception {
        UnityToolchain.dotnet();
        File work = UnityToolchain.workDir("metadata");
        File samples = UnityToolchain.samples();
        UnityToolchain.dotnetBuild(new File(samples, "console/Console.csproj"), work);
        File dumper = new File(samples, "metadata-dump/MetadataDump.csproj");
        UnityToolchain.dotnetBuild(dumper, work);
        File assembly = new File(samples, "console/bin/Release/netstandard2.1/SpikeConsole.dll");
        Assert.assertTrue("the build produced no " + assembly, assembly.isFile());

        UnityToolchain.Result oracle = UnityToolchain.dotnetRun(dumper, work, assembly.getAbsolutePath());
        oracle.assertOk("the .NET metadata dump");

        // Our reader resolves the base types the assembly names; the oracle prints them unresolved.
        File netstandard = new File(assembly.getParentFile(), "netstandard-ref/netstandard.dll");
        CilAssembly read = new Universe().load(Arrays.asList(assembly, netstandard)).get(0);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, false, "UTF-8");
        MetadataDump.dump(read, out);
        out.flush();
        String ours = new String(bytes.toByteArray(), "UTF-8");

        Assert.assertTrue("the oracle printed nothing worth comparing:\n" + oracle.out,
                UnityToolchain.lines(oracle.out).size() > 50);
        UnityToolchain.assertSameLines("metadata of " + assembly.getName(), oracle.out, ours);
    }
}

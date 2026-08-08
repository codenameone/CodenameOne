/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.retrace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

/** Retrace of obfuscated frames through a ProGuard mapping, single and chained. */
public class MappingFileTest {

    private static final String MAPPING =
            "# Codename One App Hardening mapping\n"
            + "# engine: 1.0.0\n"
            + "com.example.MyForm -> zqaaaa:\n"
            + "    int counter -> a\n"
            + "    void onClick() -> b\n"
            + "    142:145:java.lang.String render(int) -> c\n"
            + "com.example.util.Helper -> zqaaab:\n"
            + "    int square(int) -> a\n";

    @Test
    public void retracesClassAndMethod() throws Exception {
        MappingFile mf = MappingFile.parse(MAPPING);
        assertEquals(2, mf.size());
        Frame in = new Frame("zqaaaa", "b", "zqaaaa.java", 5);
        Frame out = mf.retrace(in);
        assertEquals("com.example.MyForm", out.getClassName());
        assertEquals("onClick", out.getMethodName());
        assertEquals("MyForm.java", out.getFileName());
    }

    private static final String INIT_MAPPING =
            "com.example.MyForm -> zqaaaa:\n"
            + "    50:55:void <init>() -> <init>\n"
            + "    70:72:void <clinit>() -> <clinit>\n";

    @Test
    public void usesR8SourceFileMetadataWhenSourceFileStripped() throws Exception {
        // A hardened Android build strips SourceFile, so the device reports no real filename; R8's
        // sourceFile metadata comment carries the true name (Screen.kt) and must be used instead of a
        // synthesized Screen.java.
        String mapping =
                "com.example.Screen -> a.b:\n"
                + "    # {\"id\":\"sourceFile\",\"fileName\":\"Screen.kt\"}\n"
                + "    142:145:void onClick() -> a\n";
        MappingFile mf = MappingFile.parse(mapping);
        // Reported file is the obfuscated class placeholder (b.java) -- a stripped-SourceFile symptom.
        Frame placeholder = mf.retrace(new Frame("a.b", "a", "b.java", 143));
        assertEquals("com.example.Screen", placeholder.getClassName());
        assertEquals("onClick", placeholder.getMethodName());
        assertEquals("Screen.kt", placeholder.getFileName());
        // Empty reported file (the other stripped-SourceFile symptom) resolves the same way.
        Frame empty = mf.retrace(new Frame("a.b", "a", "", 143));
        assertEquals("Screen.kt", empty.getFileName());
    }

    @Test
    public void synthesizesSourceFileWhenMappingHasNoMetadata() throws Exception {
        // Without sourceFile metadata, a stripped-SourceFile frame still synthesizes <Class>.java.
        String mapping =
                "com.example.Screen -> a.b:\n"
                + "    142:145:void onClick() -> a\n";
        Frame out = MappingFile.parse(mapping).retrace(new Frame("a.b", "a", "b.java", 143));
        assertEquals("Screen.java", out.getFileName());
    }

    @Test
    public void normalizesParparVmConstructorSentinel() throws Exception {
        // ParparVM records a constructor frame under the runtime sentinel __INIT__; the mapping keys it
        // as <init>. Without normalization the lookup misses and the frame keeps __INIT__.
        MappingFile mf = MappingFile.parse(INIT_MAPPING);
        Frame out = mf.retrace(new Frame("zqaaaa", "__INIT__", "zqaaaa.java", 52));
        assertEquals("com.example.MyForm", out.getClassName());
        assertEquals("<init>", out.getMethodName());
    }

    @Test
    public void normalizesParparVmStaticInitializerSentinel() throws Exception {
        MappingFile mf = MappingFile.parse(INIT_MAPPING);
        Frame out = mf.retrace(new Frame("zqaaaa", "__CLINIT__", "zqaaaa.java", 71));
        assertEquals("com.example.MyForm", out.getClassName());
        assertEquals("<clinit>", out.getMethodName());
    }

    @Test
    public void retracesMethodByLineRange() throws Exception {
        MappingFile mf = MappingFile.parse(MAPPING);
        Frame out = mf.retrace(new Frame("zqaaaa", "c", "zqaaaa.java", 143));
        assertEquals("render", out.getMethodName());
        assertEquals("com.example.MyForm", out.getClassName());
    }

    @Test
    public void mapsDistinctOriginalLineRange() throws Exception {
        // R8 / optimized ProGuard: obfuscated lines 1:2 map to original lines 40:41.
        MappingFile mf = MappingFile.parse(
                "com.example.MyForm -> zqaaaa:\n"
                + "    1:2:void f():40:41 -> a\n");
        Frame out = mf.retrace(new Frame("zqaaaa", "a", "zqaaaa.java", 2));
        assertEquals("f", out.getMethodName());
        assertEquals(41, out.getLineNumber());
    }

    @Test
    public void singleLineOriginalRangeCollapses() throws Exception {
        // Obfuscated lines 1:3 all map to original line 40 (a single-line original range).
        MappingFile mf = MappingFile.parse(
                "com.example.MyForm -> zqaaaa:\n"
                + "    1:3:void f():40:40 -> a\n");
        assertEquals(40, mf.retrace(new Frame("zqaaaa", "a", "zqaaaa.java", 3)).getLineNumber());
        assertEquals(40, mf.retrace(new Frame("zqaaaa", "a", "zqaaaa.java", 1)).getLineNumber());
    }

    @Test
    public void inlinedFramesAreAllEmittedInOrder() throws Exception {
        // R8 inlining: two method records share the obfuscated name 'a' and obfuscated line 1 --
        // the inlined callee and the caller it was inlined into. retraceAll must emit BOTH, innermost
        // first, or the reconstructed stack loses the inlined call path.
        MappingFile mf = MappingFile.parse(
                "com.example.Outer -> x:\n"
                + "    1:1:void inlinedCallee():10:10 -> a\n"
                + "    1:1:void caller():20:20 -> a\n");
        java.util.List<Frame> frames = mf.retraceAll(new Frame("x", "a", "x.java", 1));
        assertEquals(2, frames.size());
        assertEquals("inlinedCallee", frames.get(0).getMethodName());
        assertEquals(10, frames.get(0).getLineNumber());
        assertEquals("caller", frames.get(1).getMethodName());
        assertEquals(20, frames.get(1).getLineNumber());
        // The single-frame retrace() stays backward compatible: it returns the innermost frame.
        assertEquals("inlinedCallee", mf.retrace(new Frame("x", "a", "x.java", 1)).getMethodName());
    }

    @Test
    public void inlinedMethodFromAnotherClassKeepsItsOwnClass() throws Exception {
        // The inlinee 'a' at obf line 1 is Callee.run from a DIFFERENT class; the retraced frame must
        // report Callee/Callee.java, not the enclosing Outer with Callee.run glued on as the method.
        MappingFile mf = MappingFile.parse(
                "com.example.Outer -> x:\n"
                + "    1:1:void com.example.Callee.run():12:12 -> a\n"
                + "    1:1:void outerMethod():30:30 -> a\n");
        java.util.List<Frame> frames = mf.retraceAll(new Frame("x", "a", "x.java", 1));
        assertEquals(2, frames.size());
        assertEquals("com.example.Callee", frames.get(0).getClassName());
        assertEquals("run", frames.get(0).getMethodName());
        assertEquals("Callee.java", frames.get(0).getFileName());
        assertEquals("com.example.Outer", frames.get(1).getClassName());
        assertEquals("outerMethod", frames.get(1).getMethodName());
    }

    @Test
    public void keepsRealReportedSourceFileButNotObfuscatedPlaceholder() throws Exception {
        MappingFile mf = MappingFile.parse(
                "com.example.Screen -> a:\n"
                + "    void onClick() -> b\n");
        // A real Kotlin source name the class name can't reconstruct is kept.
        assertEquals("Screen.kt",
                mf.retrace(new Frame("a", "b", "Screen.kt", 5)).getFileName());
        // A placeholder equal to the obfuscated class name is replaced by the retraced class's file.
        assertEquals("Screen.java",
                mf.retrace(new Frame("a", "b", "a.java", 5)).getFileName());
        // No reported name -> synthesized from the retraced class.
        assertEquals("Screen.java",
                mf.retrace(new Frame("a", "b", "", 5)).getFileName());
    }

    @Test
    public void ambiguousFrameWithNoLineEmitsAllCandidates() throws Exception {
        // Two unrelated methods share one obfuscated name with no obfuscated line ranges. A frame
        // with no usable line can't disambiguate, so retraceAll must emit BOTH originals rather than
        // fabricating the first.
        MappingFile mf = MappingFile.parse(
                "com.example.C -> x:\n"
                + "    void alpha() -> a\n"
                + "    void beta() -> a\n");
        java.util.List<Frame> frames = mf.retraceAll(new Frame("x", "a", "", 0));
        assertEquals(2, frames.size());
        java.util.Set<String> names = new java.util.HashSet<String>();
        for (Frame f : frames) {
            names.add(f.getMethodName());
        }
        assertTrue(names.contains("alpha"));
        assertTrue(names.contains("beta"));
    }

    @Test
    public void unknownClassPassesThroughUnchanged() throws Exception {
        MappingFile mf = MappingFile.parse(MAPPING);
        Frame in = new Frame("some.Other", "x", "Other.java", 9);
        assertEquals(in, mf.retrace(in));
    }

    @Test
    public void chainAppliesInOrder() throws Exception {
        // Stage 1 (device-nearest, e.g. R8): b0 -> zqaaaa ; Stage 2 (cross-platform): zqaaaa -> MyForm.
        MappingFile stage1 = MappingFile.parse("zqaaaa -> b0:\n    void b() -> a\n");
        MappingFile stage2 = MappingFile.parse(MAPPING);
        MappingChain chain = new MappingChain(Arrays.asList(stage1, stage2));
        Frame device = new Frame("b0", "a", "b0.java", 5);
        Frame out = chain.retrace(device);
        assertEquals("com.example.MyForm", out.getClassName());
        assertEquals("onClick", out.getMethodName());
    }
}

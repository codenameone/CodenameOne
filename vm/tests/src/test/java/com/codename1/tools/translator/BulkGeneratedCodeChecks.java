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

package com.codename1.tools.translator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// The assertions a folded fixture used to make on its own translation and
/// output, beyond "HotSpot and ParparVM print the same", run on the bulk
/// program's shared translation. Each adds to `failures` instead of throwing,
/// so one run reports all of them.
final class BulkGeneratedCodeChecks {
    private BulkGeneratedCodeChecks() {
    }

    /// Assertions on the generated C, made before it is built.
    static void check(Path generated, List<String> groups, List<String> failures) throws IOException {
        if (groups.contains("StreamEdgeChecks")) {
            streamEdgeFusion(read(generated.resolve("StreamEdgeApp.c")), failures);
        }
        if (groups.contains("StringFormatChecks")) {
            stringFormatOwnership(generated, failures);
        }
        if (groups.contains("CollectionSemanticsChecks")) {
            collectionTraversal(read(generated.resolve("CollectionSemanticsApp.c")), failures);
        }
    }

    /// Rewrites compiled fixture classes before both runs, for a bytecode
    /// shape javac does not reliably produce.
    ///
    /// CollectionSemanticsApp.stringFromCollection calls `toString` on a
    /// Collection. Both owners are legal for that Object method and some
    /// compilers emit `Collection.toString` as an INVOKEINTERFACE, as in the
    /// Windows ARM64 migration-test crash; this exercises that form on both
    /// runtimes whichever javac compiled the fixture.
    static void patchClasses(Path classesDir) throws IOException {
        Path appClass = classesDir.resolve("CollectionSemanticsApp.class");
        if (!Files.exists(appClass)) {
            return;
        }
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(Files.readAllBytes(appClass)).accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"stringFromCollection".equals(name)) {
                    return method;
                }
                return new MethodVisitor(Opcodes.ASM9, method) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String called, String desc,
                            boolean isInterface) {
                        if ("toString".equals(called)) {
                            super.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Collection", called, desc,
                                    true);
                        } else {
                            super.visitMethodInsn(opcode, owner, called, desc, isInterface);
                        }
                    }
                };
            }
        }, 0);
        Files.write(appClass, writer.toByteArray());
    }

    /// StringBuilder's native primitive storage needs a finalizer and no
    /// tracing, String still traces, and StringFormatApp's builder helpers
    /// keep stack allocation only where the builder provably does not escape.
    private static void stringFormatOwnership(Path generated, List<String> failures) throws IOException {
        if (!read(generated.resolve("java_lang_StringBuilder.c")).contains(
                "&__FINALIZER_java_lang_StringBuilder ,0 , 0,")) {
            failures.add("StringFormatApp: native primitive storage needs cleanup but no tracing callback");
        }
        if (!read(generated.resolve("java_lang_String.c")).contains("&__GC_MARK_java_lang_String,")) {
            failures.add("StringFormatApp: Strings still trace their backing storage");
        }
        String app = read(generated.resolve("StringFormatApp.c"));
        if (!app.contains("cleanup(cn1StackBufferLeave)")) {
            failures.add("StringFormatApp: the exception/return cases must exercise stack buffer ownership");
        }
        String borrowed = function(app, "JAVA_OBJECT StringFormatApp_borrowedBuilderResult___R_java_lang_String(", "\n}");
        if (borrowed == null || !borrowed.contains("cleanup(cn1StackBufferLeave)")) {
            failures.add("StringFormatApp: a proved borrowing helper must preserve stack allocation");
        }
        String escaped = function(app, "JAVA_VOID StringFormatApp_escapingBuilderResult__(", "\n}");
        if (escaped == null || escaped.contains("cleanup(cn1StackBufferLeave)")) {
            failures.add("StringFormatApp: a retaining helper must keep heap ownership");
        }
    }

    /// CollectionSemanticsApp's list traversals use native cursors and never
    /// a map or set layout; direct `List.get` calls are inlined behind a
    /// receiver test and keep the virtual call for mixed receivers.
    private static void collectionTraversal(String generated, List<String> failures) {
        String walk = function(generated, "JAVA_VOID CollectionSemanticsApp_fieldTraversalCases__(", null);
        if (walk == null) {
            failures.add("CollectionSemanticsApp: no generated fieldTraversalCases");
        } else {
            expect(walk.contains("cn1RefBlockGet"), "field and call-result loops use native cursors", failures);
            expect(!walk.contains("java_util_HashMap"), "List receivers cannot use map-view layouts", failures);
            expect(!walk.contains("java_util_HashSet"), "List receivers cannot use set layouts", failures);
            expect(!walk.contains("java_util_IdentityHashMap"),
                    "List receivers cannot use identity-map layouts", failures);
        }
        String direct = function(generated, "JAVA_VOID CollectionSemanticsApp_directCallCases__(", null);
        if (direct == null) {
            failures.add("CollectionSemanticsApp: no generated directCallCases");
        } else {
            expect(direct.contains("cn1InlListGet"), "direct List.get is inlined", failures);
            expect(direct.contains("__cn1DirectReceiver"), "direct List.get tests its receiver", failures);
            expect(direct.contains("cn1ThrowNullPointerOrDie"), "direct List.get checks null", failures);
            expect(direct.contains("virtual_java_util_List_get___int_R_java_lang_Object"),
                    "mixed receivers must retain dispatch", failures);
        }
    }

    private static void expect(boolean ok, String what, List<String> failures) {
        if (!ok) {
            failures.add("CollectionSemanticsApp: " + what);
        }
    }

    /// The generated C from the definition starting with `signature` up to
    /// `end` (or the end of the file); null if absent. With a null `end` it
    /// runs to the next function definition: a statement expression can put
    /// a brace, or a `JAVA_` declaration, at column 0 inside a function.
    private static String function(String c, String signature, String end) {
        int start = c.indexOf(signature);
        if (start < 0) {
            return null;
        }
        int from = start + signature.length();
        int stop;
        if (end != null) {
            stop = c.indexOf(end, from);
        } else {
            stop = c.indexOf("(CODENAME_ONE_THREAD_STATE", from);
            if (stop >= 0) {
                stop = c.lastIndexOf('\n', stop);
            }
        }
        return c.substring(start, stop < 0 ? c.length() : stop);
    }

    /// StreamEdgeApp's eight immediate helper pipelines must lower to fused C
    /// loops: no callback objects, no dispatch through the stream stages.
    /// Equal output cannot show this, since the unfused pipeline answers the
    /// same.
    private static void streamEdgeFusion(String generated, List<String> failures) {
        if (generated.split("fused array stream", -1).length < 9) {
            failures.add("StreamEdgeApp: all eight immediate helper pipelines must lower to C");
        }
        for (String helper : Arrays.asList("fusedMatch", "fusedCount", "fusedForEach", "fusedAll",
                "fusedNone", "fusedZero", "fusedThrow", "fusedMutation")) {
            int start = generated.indexOf(" StreamEdgeApp_" + helper + "_");
            if (start < 0) {
                failures.add("StreamEdgeApp: no generated function for " + helper);
                continue;
            }
            int end = generated.indexOf("\nJAVA_", start);
            String body = generated.substring(start, end < 0 ? generated.length() : end);
            if (!body.contains("fused array stream")) {
                failures.add("StreamEdgeApp." + helper + " was not fused");
            }
            if (body.contains("lambda$factory")) {
                failures.add("StreamEdgeApp." + helper + " must not allocate callback objects");
            }
            if (body.contains("virtual_java_util_stream")) {
                failures.add("StreamEdgeApp." + helper + " must not dispatch stream stages");
            }
        }
    }

    /// Assertions on what particular checks printed on ParparVM.
    static void checkOutput(Map<String, List<String>> parparvm, List<String> failures) {
        List<String> collections = parparvm.get("CollectionSemanticsApp");
        if (collections != null) {
            // Named explicitly so a regression points at the change rather
            // than at a generic diff.
            caseValue(collections, "empty.size", "0", "a list never added to must be empty", failures);
            caseValue(collections, "empty.get0", "IndexOutOfBounds", "get(0) on an empty list must still throw",
                    failures);
            caseValue(collections, "ihm.keyNulls", "1",
                    "the key iterator must hand back the null key as null, not the table's sentinel", failures);
            caseValue(collections, "ihm.valNulls", "1", "the value iterator must hand back a null value as null",
                    failures);
            caseValue(collections, "ihm.bigSeen", "500", "key iteration must survive a rehash", failures);
            // A `?:` receiver joins two branches right before the loop. Each
            // must walk the branch that ran -- the else branch's load used to
            // be folded in as the receiver of both, so a true condition with a
            // null else threw. Without the fix trueNullElse, trueOther,
            // localsFirst, localsFirstNullElse, keySetFirst,
            // keySetFirstNullElse and valuesFirst fail; the other cases pin the
            // branches that were right by construction. Equal output catches
            // them too; named so the failure says what broke.
            String[][] ternary = {
                {"ternary.trueNullElse", "o:1"}, {"ternary.trueOther", "o:2"},
                {"ternary.falseOther", "uv:2"}, {"ternary.falseNull", "THREW java.lang.NullPointerException"},
                {"ternary.localsFirst", "a1a2"}, {"ternary.localsSecond", "uv"},
                {"ternary.localsFirstNullElse", "a1a2"},
                {"ternary.keySetFirst", "1"}, {"ternary.keySetSecond", "6"},
                {"ternary.keySetFirstNullElse", "1"},
                {"ternary.valuesFirst", "3"}, {"ternary.valuesSecond", "6"},
                {"ternary.iteratorFirst", "a1"}, {"ternary.iteratorSecond", "u"},
            };
            for (String[] c : ternary) {
                caseValue(collections, c[0], c[1], "a ?: receiver must use the branch that ran", failures);
            }
        }
        List<String> rethrow = parparvm.get("CatchRethrowApp");
        if (rethrow != null && !rethrow.contains("RESULT=127")) {
            // Formerly CatchRethrowIntegrationTest's own assertion.
            failures.add("CatchRethrowApp: an exception thrown from a catch handler was caught by a sibling"
                    + " handler: " + rethrow);
        }
    }

    private static void caseValue(List<String> lines, String key, String expected, String what,
            List<String> failures) {
        String prefix = "CASE|" + key + "|";
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                if (!line.substring(prefix.length()).equals(expected)) {
                    failures.add("CollectionSemanticsApp: " + what + " (" + line + ")");
                }
                return;
            }
        }
        failures.add("CollectionSemanticsApp: no " + key + " case (" + what + ")");
    }

    static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}

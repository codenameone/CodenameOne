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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Every non-static object field a class declares must be traced by that class's
 * {@code __GC_MARK_} function.
 *
 * A field the collector cannot see is a live object it will reclaim, and the
 * failure is neither an exception nor a null dereference: the slot is recycled,
 * some later object moves in, and the next method called through the stale
 * reference reads ITS OWN field layout out of an unrelated object. That is
 * indistinguishable from the unchecked-CHECKCAST hazard and just as invisible --
 * a SIGSEGV a long way from the cause, with no Java frame that could catch it.
 *
 * The shape is not hypothetical. A Linux suite core showed
 * java_util_ArrayList_add running on an object whose class word said
 * com_codename1_charts_compat_Canvas: the list's backing-array slot held two of
 * Canvas's int fields, so the length load faulted. The list was
 * Display.pendingIdleSerialCalls, reachable from a static root through a
 * private final instance field, and the Canvas in its place was itself live
 * (mark epoch 18, not the -1 that means fresh) -- a recycled slot, not garbage.
 *
 * The emitted mark callback must cover the complete flattened layout, including
 * inherited fields. Weak referents must be registered rather than marked strongly.
 */
class GcMarkCompletenessTest {

    /** struct obj__X { ... } -- the layout the mark function has to cover. */
    private static final Pattern STRUCT =
            Pattern.compile("struct obj__(\\w+)\\s*\\{(.*?)\\n\\};", Pattern.DOTALL);
    /** void __GC_MARK_X(...) { ... } */
    private static final Pattern MARKFN =
            Pattern.compile("void __GC_MARK_(\\w+)\\(CODENAME_ONE_THREAD_STATE[^)]*\\)\\s*\\{(.*?)\\n\\}",
                    Pattern.DOTALL);
    /** A JAVA_OBJECT member, i.e. exactly what the collector must follow. */
    private static final Pattern OBJ_FIELD =
            Pattern.compile("^\\s*JAVA_OBJECT\\s+(\\w+)\\s*;", Pattern.MULTILINE);

    @Test
    void everyDeclaredObjectFieldIsTracedByItsMarkFunction() throws Exception {
        Path classes = Files.createTempDirectory("gcmark-classes");
        Path out = Files.createTempDirectory("gcmark-out");
        Path src = Files.createTempDirectory("gcmark-src");

        // Deliberately covers the shapes that have gone wrong or could: a field
        // declared on a BASE class and inherited, a collection field like the one
        // the core implicated, an array field, an interface-typed field, and a
        // class whose object fields sit among primitives so an offset mistake is
        // visible.
        Path app = src.resolve("GcMarkApp.java");
        Files.write(app, ("import java.util.*;\n" +
                "class MarkList extends ArrayList<Object> {}\n" +
                "class MarkBase { Object baseRef; private Object privateRef = new Object(); int basePrim;\n" +
                "    int reads() { return (baseRef != null ? 1 : 0) + (privateRef != null ? 1 : 0) + basePrim; } }\n" +
                "class MarkMid extends MarkBase { String midRef; }\n" +
                "class MarkLeaf extends MarkMid {\n" +
                "    final ArrayList<Runnable> pending = new ArrayList<Runnable>();\n" +
                "    int a; Object mixedOne; long b; String[] arrayRef; int c;\n" +
                "    Runnable iface; Map<String,String> mapRef;\n" +
                "}\n" +
                "public class GcMarkApp {\n" +
                "    static MarkLeaf keep; static MarkList list; static java.lang.ref.WeakReference<Object> weak;\n" +
                "    public static void main(String[] args) {\n" +
                "        keep = new MarkLeaf(); list = new MarkList(); list.add(keep); weak = new java.lang.ref.WeakReference<Object>(keep);\n" +
                "        keep.pending.add(new Runnable(){ public void run(){} });\n" +
                "        keep.mixedOne = new Object();\n" +
                "        keep.arrayRef = new String[2];\n" +
                "        keep.iface = new Runnable(){ public void run(){} };\n" +
                "        keep.mapRef = new HashMap<String,String>();\n" +
                "        keep.baseRef = new Object();\n" +
                "        keep.midRef = \"x\";\n" +
                // Every field is READ as well as written: the translator removes an
                // instance field nothing reads (DeadFieldElimination), and a removed
                // field has no mark-function entry for this test to find.
                "        System.out.println(keep.pending.size() + keep.reads() + (keep.midRef != null ? 1 : 0)\n" +
                "            + (keep.mixedOne != null ? 1 : 0) + (keep.arrayRef != null ? 1 : 0) + keep.a + keep.c + (int) keep.b\n" +
                "            + (keep.iface != null ? 1 : 0) + (keep.mapRef != null ? 1 : 0));\n" +
                "    }\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        org.junit.jupiter.api.Assumptions.assumeTrue(config != null,
                "no compiler available that targets a JavaAPI-compatible bytecode level");

        Path javaApi = Files.createTempDirectory("gcmark-java-api");
        CompilerHelper.compileJavaAPI(javaApi, config);

        List<String> args = new ArrayList<String>();
        args.add("-source"); args.add(config.targetVersion);
        args.add("-target"); args.add(config.targetVersion);
        if (CompilerHelper.useClasspath(config)) {
            args.add("-classpath"); args.add(javaApi.toString());
        } else {
            args.add("-bootclasspath"); args.add(javaApi.toString());
            args.add("-Xlint:-options");
        }
        args.add("-nowarn");
        args.add("-d"); args.add(classes.toString());
        args.add(app.toString());
        assertTrue(CompilerHelper.compile(config.jdkHome, args) == 0,
                "the fixture must compile against JavaAPI");

        // The translator needs the class library beside the app, as every other
        // integration test here stages it.
        CompilerHelper.copyDirectory(javaApi, classes);
        CleanTargetIntegrationTest.runTranslator(classes, out, "GcMarkApp");
        Path srcRoot = findSrcRoot(out);

        List<String> missing = new ArrayList<String>();
        int classesChecked = 0;
        int fieldsChecked = 0;

        try (Stream<Path> files = Files.walk(srcRoot)) {
            for (Path c : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".c"))::iterator) {
                String body = new String(Files.readAllBytes(c), StandardCharsets.ISO_8859_1);
                Path header = c.resolveSibling(c.getFileName().toString().replaceAll("\\.c$", ".h"));
                if (!Files.exists(header)) {
                    continue;
                }
                String head = new String(Files.readAllBytes(header), StandardCharsets.ISO_8859_1);

                Matcher mf = MARKFN.matcher(body);
                while (mf.find()) {
                    String cls = mf.group(1);
                    String markBody = mf.group(2);
                    Set<String> declared = declaredObjectFields(head, cls);
                    if (declared.isEmpty()) {
                        continue;
                    }
                    classesChecked++;
                    for (String f : declared) {
                        fieldsChecked++;
                        // The emitted body names the field directly, whether it goes
                        // through cn1GcMarkField or
                        // cn1GcDiscoverReference (the WeakReference referent, which is
                        // deliberately not traced but IS handed to the collector).
                        if (!tracesField(markBody, f)) {
                            missing.add(cls + "." + f);
                        }
                    }
                }
            }
        }

        String leaf = new String(Files.readAllBytes(srcRoot.resolve("MarkLeaf.c")), StandardCharsets.ISO_8859_1);
        Matcher leafMark = MARKFN.matcher(leaf);
        assertTrue(leafMark.find());
        assertFalse(leafMark.group(2).contains("__GC_MARK_MarkMid"));
        assertTrue(leafMark.group(2).contains("objInstance->MarkBase_baseRef"));
        assertTrue(leafMark.group(2).contains("objInstance->MarkBase_privateRef"));
        assertTrue(leafMark.group(2).contains("objInstance->MarkMid_midRef"));
        String list = new String(Files.readAllBytes(srcRoot.resolve("MarkList.c")), StandardCharsets.ISO_8859_1);
        Matcher listMark = MARKFN.matcher(list);
        assertTrue(listMark.find());
        assertTrue(listMark.group(2).contains("java_util_ArrayList_cn1Storage"),
                "subclass must trace its inherited native reference block");
        String weak = new String(Files.readAllBytes(srcRoot.resolve("java_lang_ref_WeakReference.c")), StandardCharsets.ISO_8859_1);
        Matcher weakMark = MARKFN.matcher(weak);
        assertTrue(weakMark.find());
        assertTrue(weakMark.group(2).contains("cn1GcDiscoverReference"));
        assertFalse(weakMark.group(2).contains("cn1GcMarkField(threadStateData, objInstance->java_lang_ref_Reference_objReference"));

        // A pass that inspected nothing is not a pass. The fixture alone declares
        // eight object fields across three classes in one hierarchy.
        assertTrue(classesChecked >= 3,
                "expected to inspect several classes, saw " + classesChecked);
        assertTrue(fieldsChecked >= 8,
                "expected to inspect the fixture's object fields, saw " + fieldsChecked);
        assertTrue(missing.isEmpty(),
                "object field(s) declared but never traced by the class's __GC_MARK_ function -- "
                        + "the collector cannot see them, so it will reclaim live objects and "
                        + "recycle their slots: " + missing);
    }

    /**
     * Proves the check can fail, by deleting one field's mark from a body and
     * confirming the comparison notices. A gate nobody has watched fail is not a
     * gate, and this one is a string search over generated code -- exactly the kind
     * that silently matches everything or nothing.
     */
    @Test
    void theCheckDetectsAnUntracedField() {
        String head = "struct obj__Foo {\n    JAVA_OBJECT Foo_kept;\n    JAVA_OBJECT Foo_dropped;\n};";
        Set<String> declared = declaredObjectFields(head, "Foo");
        assertTrue(declared.contains("Foo_kept") && declared.contains("Foo_dropped"),
                "fixture parse: " + declared);
        String markBody = "    cn1GcMarkField(threadStateData, objInstance->Foo_kept, force, epoch);\n"
                + "    cn1GcVerifyFieldType(threadStateData, obj, objInstance->Foo_dropped, 1, \"Foo.dropped\");";
        List<String> missing = new ArrayList<String>();
        for (String f : declared) {
            if (!tracesField(markBody, f)) {
                missing.add(f);
            }
        }
        assertFalse(missing.isEmpty(), "the check must notice a field that is not marked");
        assertTrue(missing.contains("Foo_dropped") && missing.size() == 1,
                "it must name exactly the untraced field, got " + missing);
    }

    private static boolean tracesField(String body, String field) {
        return Pattern.compile("(?:cn1GcMarkField|cn1GcDiscoverReference)\\([^\\n]*\\b"
                + Pattern.quote(field) + "\\b").matcher(body).find();
    }

    private CompilerHelper.CompilerConfig selectCompiler() {
        String[] preferredTargets = {"11", "17", "21", "25", "1.8"};
        for (String target : preferredTargets) {
            for (CompilerHelper.CompilerConfig c : CompilerHelper.getAvailableCompilers(target)) {
                if (CompilerHelper.isJavaApiCompatible(c)) {
                    return c;
                }
            }
        }
        return null;
    }

    private static Set<String> declaredObjectFields(String header, String cls) {
        Set<String> out = new LinkedHashSet<String>();
        Matcher s = STRUCT.matcher(header);
        while (s.find()) {
            if (!s.group(1).equals(cls)) {
                continue;
            }
            Matcher f = OBJ_FIELD.matcher(s.group(2));
            while (f.find()) {
                String name = f.group(1);
                // The object header's own slots are not Java fields.
                if (name.startsWith("__codenameOne") || name.equals("__heapPosition")) {
                    continue;
                }
                out.add(name);
            }
        }
        return out;
    }

    private static Path findSrcRoot(Path out) throws IOException {
        try (Stream<Path> w = Files.walk(out)) {
            return w.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().endsWith("-src"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("no generated -src directory under " + out));
        }
    }
}

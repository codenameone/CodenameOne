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
package com.codename1.maven;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The stream rewrites, differentially: an application javac compiled against
/// `java.util.stream` is run as it is, on the JDK, and run again after the
/// remap, on the shared JDK classes and nothing else -- and has to answer the
/// same. The compliance check runs over the remapped build in between, so the
/// stream classes themselves are held to what a device has.
public class CompatStreamRewritesTest {

    private static final String[] FIXTURE = {
        "s/Bag.java",
            "package s;\n"
            + "import java.util.ArrayList;\n"
            + "public class Bag<T> extends ArrayList<T> {\n"
            + "}\n",
        "s/Items.java",
            "package s;\n"
            + "import java.util.List;\n"
            + "public interface Items<T> extends List<T> {\n"
            + "}\n",
        "s/ItemList.java",
            "package s;\n"
            + "import java.util.ArrayList;\n"
            + "public class ItemList<T> extends ArrayList<T> implements Items<T> {\n"
            + "}\n",
        "s/Loud.java",
            "package s;\n"
            + "import java.util.ArrayList;\n"
            + "import java.util.stream.Stream;\n"
            + "public class Loud<T> extends ArrayList<T> {\n"
            + "    public int calls;\n"
            + "    @Override\n"
            + "    public Stream<T> stream() {\n"
            + "        calls++;\n"
            + "        return super.stream().skip(1);\n"
            + "    }\n"
            + "}\n",
        "s/Repo.java",
            "package s;\n"
            + "import java.util.stream.Stream;\n"
            + "class Repo {\n"
            + "    Stream<String> stream() {\n"
            + "        return Stream.of(\"r1\", \"r2\");\n"
            + "    }\n"
            + "    protected Stream<String> parallelStream() {\n"
            + "        return Stream.of(\"p1\");\n"
            + "    }\n"
            + "}\n",
        "s/Source.java",
            "package s;\n"
            + "import java.util.stream.Stream;\n"
            + "interface Source {\n"
            + "    Stream<String> stream();\n"
            + "}\n",
        "s/Ui.java",
            "package s;\n"
            + "public class Ui {\n"
            + "    public static Object label() {\n"
            + "        return new javax.swing.JLabel(\"streams\");\n"
            + "    }\n"
            + "}\n",
        "s/S.java",
            "package s;\n"
            + "import java.util.ArrayList;\n"
            + "import java.util.Arrays;\n"
            + "import java.util.Collection;\n"
            + "import java.util.Comparator;\n"
            + "import java.util.IntSummaryStatistics;\n"
            + "import java.util.Iterator;\n"
            + "import java.util.LinkedHashMap;\n"
            + "import java.util.List;\n"
            + "import java.util.Map;\n"
            + "import java.util.Optional;\n"
            + "import java.util.TreeMap;\n"
            + "import java.util.function.Function;\n"
            + "import java.util.function.Predicate;\n"
            + "import java.util.function.Supplier;\n"
            + "import java.util.stream.Collectors;\n"
            + "import java.util.stream.IntStream;\n"
            + "import java.util.stream.Stream;\n"
            + "public class S {\n"
            + "    public static Object[] run() {\n"
            + "        List<Object> out = new ArrayList<>();\n"
            + "        List<String> words = new ArrayList<>(Arrays.asList(\"pear\", \"apple\", \"fig\", \"banana\", \"apple\"));\n"
            + "        out.add(words.stream().filter(w -> w.length() > 3).map(String::toUpperCase).collect(Collectors.toList()));\n"
            + "        out.add(words.stream().collect(Collectors.groupingBy(String::length, TreeMap::new, Collectors.counting()))\n"
            + "                .toString());\n"
            + "        out.add(words.stream().mapToInt(String::length).sum());\n"
            + "        Comparator<String> byLength = Comparator.comparingInt(String::length);\n"
            + "        out.add(words.parallelStream().sorted(byLength.thenComparing(Comparator.<String>naturalOrder()))\n"
            + "                .collect(Collectors.joining(\",\")));\n"
            + "        out.add(words.stream().sorted(Comparator.<String>naturalOrder().reversed()).findFirst().get());\n"
            + "        out.add(words.stream().max(byLength.thenComparing(Function.<String>identity())).get());\n"
            + "        out.add(words.stream().sorted(Comparator.comparing(String::length, Comparator.<Integer>reverseOrder()))\n"
            + "                .collect(Collectors.toList()));\n"
            + "\n"
            + "        out.add(Arrays.stream(new String[] {\"b\", \"a\"}).sorted().collect(Collectors.toList()));\n"
            + "        out.add(Arrays.stream(new int[] {3, 4, 5}).map(n -> n * n).boxed().collect(Collectors.toList()));\n"
            + "        out.add(Arrays.stream(new int[] {3, 4, 5}, 1, 3).sum());\n"
            + "        out.add(Arrays.stream(new long[] {3L, 4L}).max().getAsLong());\n"
            + "        out.add(Arrays.stream(new double[] {1.5, 2.5}).average().getAsDouble());\n"
            + "        out.add(IntStream.rangeClosed(1, 5).mapToObj(Integer::toString).collect(Collectors.joining(\"+\")));\n"
            + "        out.add(IntStream.range(0, 10).filter(n -> n % 3 == 0).boxed().collect(Collectors.toList()));\n"
            + "        out.add(Stream.of(\"x\", \"y\").map(s -> s + s).collect(Collectors.toList()));\n"
            + "        out.add(Stream.iterate(1, n -> n * 2).limit(6).reduce(0, Integer::sum));\n"
            + "\n"
            + "        // The method references: unbound on two interfaces, and bound.\n"
            + "        Function<Collection<String>, Stream<String>> ofCollection = Collection::stream;\n"
            + "        out.add(ofCollection.apply(words).count());\n"
            + "        Function<List<String>, Stream<String>> ofList = List::stream;\n"
            + "        out.add(ofList.apply(words).skip(3).findFirst().get());\n"
            + "        Supplier<Stream<String>> bound = words::stream;\n"
            + "        out.add(bound.get().distinct().count());\n"
            + "        out.add(Stream.of(words, words).flatMap(Collection::stream).count());\n"
            + "\n"
            + "        // A class of the application's own that inherits stream().\n"
            + "        Bag<String> bag = new Bag<>();\n"
            + "        bag.addAll(words);\n"
            + "        out.add(bag.stream().map(String::length).collect(Collectors.toList()));\n"
            + "        out.add(bag.parallelStream().count());\n"
            + "        Function<Bag<String>, Stream<String>> ofBag = Bag::stream;\n"
            + "        out.add(ofBag.apply(bag).limit(2).collect(Collectors.toList()));\n"
            + "        Items<String> items = new ItemList<>();\n"
            + "        items.addAll(words);\n"
            + "        out.add(items.stream().anyMatch(\"fig\"::equals));\n"
            + "\n"
            + "        // One that overrides it: reached through its own type and through\n"
            + "        // List, and not by parallelStream(), which it left alone.\n"
            + "        Loud<String> loud = new Loud<>();\n"
            + "        loud.addAll(words);\n"
            + "        out.add(loud.stream().collect(Collectors.toList()));\n"
            + "        List<String> asList = loud;\n"
            + "        out.add(asList.stream().collect(Collectors.toList()));\n"
            + "        out.add(asList.parallelStream().collect(Collectors.toList()));\n"
            + "        Collection<String> asCollection = loud;\n"
            + "        out.add(asCollection.stream().count());\n"
            + "        out.add(loud.calls);\n"
            + "\n"
            + "        // And classes that have a stream() without being collections.\n"
            + "        Repo repo = new Repo();\n"
            + "        out.add(repo.stream().collect(Collectors.joining(\"/\")));\n"
            + "        out.add(repo.parallelStream().collect(Collectors.joining(\"/\")));\n"
            + "        Source source = () -> Stream.of(\"lambda\");\n"
            + "        out.add(source.stream().findFirst().get());\n"
            + "\n"
            + "        out.add(\"a,b;c\".split(\";\")[0]);\n"
            + "        out.add(Arrays.asList(\"a1b22c333d\".split(\"[0-9]+\", 3)));\n"
            + "        out.add(Arrays.asList(\" a  b \".split(\" \")));\n"
            + "        Predicate<String> isShort = w -> w.length() < 4;\n"
            + "        out.add(words.stream().filter(isShort.negate().and(w -> w.startsWith(\"a\")).or(w -> w.equals(\"fig\"))).count());\n"
            + "        out.add(\"hello\".chars().filter(c -> c == 'l').count());\n"
            + "        out.add(new StringBuilder(\"abc\").chars().sum());\n"
            + "        out.add(words.stream().collect(Collectors.toMap(w -> w, String::length, Integer::sum, LinkedHashMap::new))\n"
            + "                .toString());\n"
            + "        Map<String, Integer> map = new TreeMap<>();\n"
            + "        map.put(\"b\", 1);\n"
            + "        map.put(\"a\", 2);\n"
            + "        out.add(map.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey)\n"
            + "                .collect(Collectors.toList()));\n"
            + "        out.add(map.keySet().stream().collect(Collectors.joining()));\n"
            + "        out.add(map.values().stream().mapToInt(Integer::intValue).max().getAsInt());\n"
            + "        out.add(words.stream().mapToDouble(String::length).average().getAsDouble());\n"
            + "        Optional<String> longest = words.stream().max(byLength);\n"
            + "        out.add(longest.map(String::length).orElse(-1));\n"
            + "        Iterator<String> it = words.iterator();\n"
            + "        it.next();\n"
            + "        List<String> rest = new ArrayList<>();\n"
            + "        it.forEachRemaining(rest::add);\n"
            + "        out.add(rest);\n"
            + "        out.add(words.stream().toArray(String[]::new).length);\n"
            + "        IntSummaryStatistics stats = words.stream().mapToInt(String::length).summaryStatistics();\n"
            + "        out.add(stats.getMax() + \"/\" + stats.getMin() + \"/\" + stats.getCount());\n"
            + "        out.add(words.stream().collect(Collectors.partitioningBy(w -> w.length() > 4)).toString());\n"
            + "        try {\n"
            + "            words.stream().collect(Collectors.toMap(w -> w, String::length));\n"
            + "            out.add(\"no exception\");\n"
            + "        } catch (IllegalStateException e) {\n"
            + "            out.add(\"duplicate\");\n"
            + "        }\n"
            + "        return out.toArray();\n"
            + "    }\n"
            + "}\n"
    };

    private static final String JDK = Relocation.JDK_PACKAGE;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File scratch;

    /// The fixture as javac wrote it.
    private File compile() throws Exception {
        File classes = tmp.newFolder();
        CompatFixtures.compile(tmp.newFolder(), classes, FIXTURE);
        return classes;
    }

    /// The fixture remapped as a build remaps it -- the runtime extracted
    /// beside it -- and passed by the compliance check.
    private File remapped(File compiled) throws Exception {
        scratch = tmp.newFolder("jars");
        File classes = tmp.newFolder("classes");
        SwingGallerySampleTest.copy(compiled, classes);
        List<File> cp = new ArrayList<File>(Arrays.asList(RealCompatJars.swing(scratch), RealCompatJars.javafx(scratch),
                RealCompatJars.jdk(scratch), RealCompatJars.core(scratch)));
        assertTrue(new CompatRemapper(classes, cp, null, CompatRemapperTest.LOG).run());
        // Throws on a single finding: in the application, or in a class of
        // the stream shims the remap copied in.
        new BytecodeCompliance(RealCompatJars.host(classes, tmp.newFolder(), scratch)).execute();
        return classes;
    }

    private static Object[] run(ClassLoader loader) throws Exception {
        try {
            return (Object[]) loader.loadClass("s.S").getMethod("run").invoke(null);
        } catch (InvocationTargetException e) {
            throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        }
    }

    private static byte[] read(File classes, String name) throws Exception {
        return Files.readAllBytes(new File(classes, name).toPath());
    }

    @Test
    public void aStreamingApplicationAnswersTheSameOnceRemapped() throws Exception {
        File compiled = compile();
        Object[] theirs;
        URLClassLoader jdk = new URLClassLoader(new URL[] {compiled.toURI().toURL()},
                ClassLoader.getSystemClassLoader().getParent());
        try {
            theirs = run(jdk);
        } finally {
            jdk.close();
        }

        File classes = remapped(compiled);
        assertTrue("The stream runtime was not extracted", new File(classes, JDK + "ObjPipeline.class").isFile());
        // Nothing of this JVM's classpath but the JDK: the application, the
        // runtime the remap put beside it, and the framework.
        Object[] mine;
        URLClassLoader device = new URLClassLoader(
                new URL[] {classes.toURI().toURL(), RealCompatJars.core(scratch).toURI().toURL()},
                ClassLoader.getSystemClassLoader().getParent());
        try {
            mine = run(device);
        } finally {
            device.close();
        }
        assertEquals(Arrays.asList(theirs), Arrays.asList(mine));
        assertEquals(49, mine.length);
        // The override ran for its own type, for List and for Collection,
        // and not for parallelStream().
        assertEquals(Arrays.asList("apple", "fig", "banana", "apple"), mine[24]);
        assertEquals(mine[24], mine[25]);
        assertEquals(Arrays.asList("pear", "apple", "fig", "banana", "apple"), mine[26]);
        assertEquals(Integer.valueOf(3), mine[28]);
        assertEquals("r1/r2", mine[29]);
        assertEquals("p1", mine[30]);
        assertEquals("lambda", mine[31]);
    }

    @Test
    public void noStreamCallSurvivesAndASecondPassChangesNothing() throws Exception {
        File compiled = compile();
        ClassRelocator swing = new ClassRelocator(CompatLayers.SWING);
        Set<String> after = new TreeSet<String>();
        for (String name : new String[] {"s/S.class", "s/Loud.class", "s/Repo.class", "s/Source.class",
            "s/Bag.class", "s/Items.class", "s/ItemList.class"}) {
            byte[] original = read(compiled, name);
            byte[] relocated = swing.remap(original);
            assertArrayEquals(name, relocated, swing.remap(relocated));
            after.addAll(CompatFixtures.members(relocated));
        }
        Set<String> before = CompatFixtures.members(read(compiled, "s/S.class"));
        before.addAll(CompatFixtures.members(read(compiled, "s/Loud.class")));
        for (String gone : new String[] {"java/util/List.stream", "java/util/List.parallelStream",
            "java/util/Collection.stream", "java/util/ArrayList.stream", "java/util/Set.stream",
            "java/util/Arrays.stream", "java/util/stream/Stream.of", "java/util/stream/IntStream.range",
            "java/util/stream/Collectors.toList", "java/lang/String.split", "java/lang/String.chars",
            "java/lang/StringBuilder.chars", "java/util/Comparator.comparingInt", "java/util/Comparator.reversed",
            "java/util/Comparator.thenComparing", "java/util/Comparator.naturalOrder",
            "java/util/function/Predicate.negate", "java/util/function/Predicate.and",
            "java/util/function/Function.identity", "java/util/Map$Entry.comparingByValue",
            "java/util/Iterator.forEachRemaining", "s/Bag.stream", "s/Bag.parallelStream", "s/Items.stream",
            "s/Loud.stream", "s/Repo.stream", "s/Repo.parallelStream", "s/Source.stream"}) {
            assertTrue("The fixture does not use " + gone, before.contains(gone));
            assertFalse(gone + " survived: " + after, after.contains(gone));
        }
        for (String member : after) {
            assertFalse(member, member.startsWith("java/util/stream/"));
        }
        for (String there : new String[] {JDK + "JdkCollections.stream", JDK + "JdkCollections.parallelStream",
            JDK + "JdkCollections.streamOf", JDK + "JdkCollections.parallelStreamOf",
            JDK + "JdkCollections.defaultStream", JDK + "JdkCollections.forEachRemaining",
            JDK + "JdkCollections.comparingByValue", JDK + "JdkRegex.split", JDK + "JdkStrings.chars",
            JDK + "JdkFunctions.reversed", JDK + "JdkFunctions.thenComparing", JDK + "JdkFunctions.comparingInt",
            JDK + "JdkFunctions.negate", JDK + "JdkFunctions.identity", JDK + "Stream.of", JDK + "Stream.filter",
            JDK + "IntStream.range", JDK + "Collectors.groupingBy",
            // What the device has is left exactly as written.
            "java/util/List.iterator", "java/util/Map.entrySet", "java/util/List.add"}) {
            assertTrue(there + " is missing: " + after, after.contains(there));
        }
    }

    private static List<String> interfaces(byte[] cls) {
        return Arrays.asList(new ClassReader(cls).getInterfaces());
    }

    private static int access(byte[] cls, final String method) {
        final int[] access = {-1};
        new ClassReader(cls).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int a, String name, String descriptor, String signature,
                                             String[] exceptions) {
                if (name.equals(method)) {
                    access[0] = a;
                }
                return null;
            }
        }, 0);
        return access[0];
    }

    @Test
    public void aClassThatDeclaresStreamIsMarkedAndNoOther() throws Exception {
        File compiled = compile();
        ClassRelocator swing = new ClassRelocator(CompatLayers.SWING);
        byte[] loud = swing.remap(read(compiled, "s/Loud.class"));
        assertTrue(interfaces(loud).contains(JDK + "StreamSource"));
        assertFalse(interfaces(loud).contains(JDK + "ParallelStreamSource"));
        // The generic signature still says what the class is.
        String signature = signatureOf(loud);
        assertTrue(signature, signature.startsWith("<T:Ljava/lang/Object;>Ljava/util/ArrayList<TT;>;"));
        assertTrue(signature, signature.endsWith("L" + JDK + "StreamSource;"));

        byte[] repo = swing.remap(read(compiled, "s/Repo.class"));
        assertEquals(Arrays.asList(JDK + "StreamSource", JDK + "ParallelStreamSource"), interfaces(repo));
        // An interface method is public; the package's and the protected
        // one were widened to be.
        assertEquals(Opcodes.ACC_PUBLIC, access(repo, "stream"));
        assertEquals(Opcodes.ACC_PUBLIC, access(repo, "parallelStream"));

        byte[] source = swing.remap(read(compiled, "s/Source.class"));
        assertEquals(Arrays.asList(JDK + "StreamSource"), interfaces(source));

        assertTrue(interfaces(swing.remap(read(compiled, "s/Bag.class"))).isEmpty());
        assertEquals(Arrays.asList("s/Items"), interfaces(swing.remap(read(compiled, "s/ItemList.class"))));
        assertTrue(interfaces(swing.remap(read(compiled, "s/S.class"))).isEmpty());
    }

    private static String signatureOf(byte[] cls) {
        final String[] signature = {null};
        new ClassReader(cls).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String s, String superName, String[] interfaces) {
                signature[0] = s;
            }
        }, 0);
        return signature[0];
    }

    /// The Android layer's applications are not given these rules, as they
    /// are given none of the others.
    @Test
    public void theAndroidLayerAloneRewritesNoStreamCall() throws Exception {
        File compiled = compile();
        ClassRelocator android = new ClassRelocator(AndroidRemapper.RELOCATION);
        Set<String> after = CompatFixtures.members(android.remap(read(compiled, "s/S.class")));
        assertTrue(after.toString(), after.contains("java/util/List.stream"));
        assertTrue(after.toString(), after.contains("s/Bag.stream"));
        for (String member : after) {
            assertFalse(member, member.startsWith(JDK + "Jdk"));
        }
        assertTrue(interfaces(android.remap(read(compiled, "s/Loud.class"))).isEmpty());
    }

    // ---- what a newer javac writes: emitted, since this one cannot ----

    /// A class `x/Probe` with one static `run()` answering an object.
    private interface Body {
        void emit(MethodVisitor mv);
    }

    private static byte[] probe(Body body) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "x/Probe", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()Ljava/lang/Object;",
                null, null);
        mv.visitCode();
        body.emit(mv);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private Object runProbe(Body body) throws Exception {
        byte[] relocated = new ClassRelocator(CompatLayers.SWING).remap(probe(body));
        for (String member : CompatFixtures.members(relocated)) {
            assertTrue(member + " was not redirected", member.startsWith(JDK) || member.startsWith("java/lang/Object")
                    || member.startsWith("java/lang/Boolean") || member.startsWith("java/util/List.")
                    || member.startsWith("java/util/Map.get") || member.startsWith("java/util/Set.size")
                    || member.startsWith("java/lang/Integer"));
        }
        Class<?> cls = new CompatFixtures.Defining(getClass().getClassLoader()).define(relocated);
        try {
            return cls.getMethod("run").invoke(null);
        } catch (InvocationTargetException e) {
            throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        }
    }

    private static final String O = "Ljava/lang/Object;";

    @Test
    public void listOfIsImmutableOnceRewritten() throws Exception {
        Body listOf = new Body() {
            @Override
            public void emit(MethodVisitor mv) {
                mv.visitLdcInsn("a");
                mv.visitLdcInsn("b");
                mv.visitLdcInsn("c");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/List", "of", "(" + O + O + O + ")Ljava/util/List;",
                        true);
            }
        };
        List<?> list = (List<?>) runProbe(listOf);
        assertEquals(Arrays.asList("a", "b", "c"), list);
        try {
            list.remove(0);
            fail();
        } catch (UnsupportedOperationException expected) {
            // List.of's contract.
        }
        try {
            runProbe(new Body() {
                @Override
                public void emit(MethodVisitor mv) {
                    mv.visitLdcInsn("a");
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/List", "of", "(" + O + O + ")Ljava/util/List;",
                            true);
                }
            });
            fail();
        } catch (NullPointerException expected) {
            // And no nulls.
        }
    }

    @Test
    public void theOtherFactoriesAndTheLaterMembers() throws Exception {
        // Set.of(1, 2).size()
        assertEquals(Integer.valueOf(2), runProbe(new Body() {
            @Override
            public void emit(MethodVisitor mv) {
                mv.visitLdcInsn("a");
                mv.visitLdcInsn("b");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Set", "of", "(" + O + O + ")Ljava/util/Set;", true);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Set", "size", "()I", true);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;",
                        false);
            }
        }));
        // Map.of("k", "v").get("k")
        assertEquals("v", runProbe(new Body() {
            @Override
            public void emit(MethodVisitor mv) {
                mv.visitLdcInsn("k");
                mv.visitLdcInsn("v");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Map", "of", "(" + O + O + ")Ljava/util/Map;", true);
                mv.visitLdcInsn("k");
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "get", "(" + O + ")" + O, true);
            }
        }));
        // List.copyOf(List.of("z"))
        assertEquals(Arrays.asList("z"), runProbe(new Body() {
            @Override
            public void emit(MethodVisitor mv) {
                mv.visitLdcInsn("z");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/List", "of", "(" + O + ")Ljava/util/List;", true);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/List", "copyOf",
                        "(Ljava/util/Collection;)Ljava/util/List;", true);
            }
        }));
        // " \t".isBlank()
        assertEquals(Boolean.TRUE, runProbe(new Body() {
            @Override
            public void emit(MethodVisitor mv) {
                mv.visitLdcInsn(" \t");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "isBlank", "()Z", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",
                        false);
            }
        }));
        // " ab ".strip().repeat(3)
        assertEquals("ababab", runProbe(new Body() {
            @Override
            public void emit(MethodVisitor mv) {
                mv.visitLdcInsn(" ab ");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "strip", "()Ljava/lang/String;", false);
                mv.visitInsn(Opcodes.ICONST_3);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "repeat", "(I)Ljava/lang/String;",
                        false);
            }
        }));
        // Stream.of("p").toList() -- a member of the stream itself, which is
        // replaced whole and needs no rule.
        assertEquals(Arrays.asList("p"), runProbe(new Body() {
            @Override
            public void emit(MethodVisitor mv) {
                mv.visitLdcInsn("p");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/stream/Stream", "of",
                        "(" + O + ")Ljava/util/stream/Stream;", true);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "toList", "()Ljava/util/List;",
                        true);
            }
        }));
        // Optional.empty().isEmpty()
        assertEquals(Boolean.TRUE, runProbe(new Body() {
            @Override
            public void emit(MethodVisitor mv) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;",
                        false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isEmpty", "()Z", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;",
                        false);
            }
        }));
    }

    // ---- the rule for a type nothing names ----

    private static Set<String> membersAfter(final int opcode, final String owner, final String name,
                                            final String descriptor) {
        byte[] cls = probe(new Body() {
            @Override
            public void emit(MethodVisitor mv) {
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitMethodInsn(opcode, owner, name, descriptor, opcode == Opcodes.INVOKEINTERFACE);
            }
        });
        return CompatFixtures.members(new ClassRelocator(CompatLayers.SWING).remap(cls));
    }

    @Test
    public void aCallThatArrivesAlreadyRelocatedIsRecognised() {
        String moved = "()L" + JDK + "Stream;";
        assertEquals(new TreeSet<String>(Arrays.asList(JDK + "JdkCollections.streamOf")),
                membersAfter(Opcodes.INVOKEVIRTUAL, "x/Bag", "stream", moved));
        assertEquals(new TreeSet<String>(Arrays.asList(JDK + "JdkCollections.parallelStreamOf")),
                membersAfter(Opcodes.INVOKEINTERFACE, "x/Items", "parallelStream", moved));
        // Another layer's list type, and a JDK collection no row names.
        assertEquals(new TreeSet<String>(Arrays.asList(JDK + "JdkCollections.streamOf")),
                membersAfter(Opcodes.INVOKEINTERFACE, "javafx/collections/ObservableList", "stream",
                        "()Ljava/util/stream/Stream;"));
        assertEquals(new TreeSet<String>(Arrays.asList(JDK + "JdkCollections.streamOf")),
                membersAfter(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/CopyOnWriteArrayList", "stream",
                        "()Ljava/util/stream/Stream;"));
    }

    @Test
    public void whatIsNotACollectionsStreamIsLeftAlone() {
        String stream = "()Ljava/util/stream/Stream;";
        // JDK classes that have a stream() and are no collection: left for
        // the compliance check to name.
        assertEquals(new TreeSet<String>(Arrays.asList("java/util/zip/ZipFile.stream")),
                membersAfter(Opcodes.INVOKEVIRTUAL, "java/util/zip/ZipFile", "stream", stream));
        assertEquals(new TreeSet<String>(Arrays.asList("java/util/ServiceLoader.stream")),
                membersAfter(Opcodes.INVOKEVIRTUAL, "java/util/ServiceLoader", "stream", stream));
        // Optional is replaced whole, stream() and all.
        assertEquals(new TreeSet<String>(Arrays.asList(JDK + "Optional.stream")),
                membersAfter(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "stream", stream));
        // Another name, other arguments, another return type.
        assertEquals(new TreeSet<String>(Arrays.asList("x/Bag.streams")),
                membersAfter(Opcodes.INVOKEVIRTUAL, "x/Bag", "streams", stream));
        assertEquals(new TreeSet<String>(Arrays.asList("x/Bag.stream")),
                membersAfter(Opcodes.INVOKEVIRTUAL, "x/Bag", "stream", "()Ljava/util/List;"));
        assertEquals(new TreeSet<String>(Arrays.asList("java/util/BitSet.stream")),
                membersAfter(Opcodes.INVOKEVIRTUAL, "java/util/BitSet", "stream", "()Ljava/util/stream/IntStream;"));
    }
}

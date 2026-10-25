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

/// Streams through the remap: an application javac compiled against
/// `java.util.stream` keeps every stream call as written, since the device's
/// class library has the package under the JDK's name. It is run as it is
/// and again after the remap -- the regular expressions it uses then on the
/// framework's matcher -- and has to answer the same, and the compliance check
/// runs over the remapped build in between, so every stream member it calls
/// is held to what a device has.
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
                RealCompatJars.core(scratch)));
        assertTrue(new CompatRemapper(classes, cp, null, CompatRemapperTest.LOG).run());
        // Throws on a single finding: a stream member the device's class
        // library lacks is one.
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
        // The device's class library has java.util.stream under its own
        // name, so the remap leaves every stream call as javac wrote it.
        Set<String> members = CompatFixtures.members(read(classes, "s/S.class"));
        assertTrue(members.toString(), members.contains("java/util/stream/Collectors.toList"));
        assertTrue(members.toString(), members.contains("java/util/List.stream"));
        // Nothing of this JVM's classpath but the JDK: the application, the
        // runtime the remap put beside it, and the framework.
        Object[] mine;
        URLClassLoader device = new URLClassLoader(
                new URL[] {classes.toURI().toURL(), RealCompatJars.core(scratch).toURI().toURL(),
                    RealCompatJars.jdkHost(scratch).toURI().toURL()},
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
}

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
package com.codename1.impl.interp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The interpreter must produce the same observable behaviour as a real JVM
 * running the same bytecode.
 *
 * <p>Each case is compiled once and run twice -- on this JVM and through the
 * interpreter -- and the two outputs must match, including how the program
 * fails. Nothing here asserts an expected string: the JVM is the oracle, so a
 * case cannot encode the author's mistaken idea of what Java does.</p>
 *
 * <p>The corpus targets the places an interpreter actually gets wrong: value
 * widths and overflow, NaN's asymmetric comparisons, the stack-shuffling
 * opcodes whose meaning is in slots rather than values, which exception handler
 * wins and whether {@code finally} still runs, and virtual dispatch through an
 * interpreted hierarchy.</p>
 *
 * @author Shai Almog
 */
class InterpConformanceTest {

    static Stream<Arguments> cases() {
        List<Arguments> out = new ArrayList<Arguments>();
        out.add(c("Arithmetic",
                "public class Arithmetic { public static void main(String[] a) {"
                + " int x = 7, y = -3;"
                + " System.out.println(x + y); System.out.println(x - y);"
                + " System.out.println(x * y); System.out.println(x / y);"
                + " System.out.println(x % y); System.out.println(-x);"
                + " System.out.println(Integer.MAX_VALUE + 1);"
                + " System.out.println(Integer.MIN_VALUE - 1);"
                + " System.out.println(Integer.MIN_VALUE / -1);"
                + " long big = 9000000000L;"
                + " System.out.println(big * 3); System.out.println(big / 7);"
                + " System.out.println(Long.MAX_VALUE + 1);"
                + "}}"));
        out.add(c("Shifts",
                "public class Shifts { public static void main(String[] a) {"
                + " int x = -16;"
                + " System.out.println(x << 2); System.out.println(x >> 2);"
                + " System.out.println(x >>> 2);"
                // Shift counts are masked to 5 bits for int and 6 for long --
                // a detail an interpreter gets wrong by using the raw count.
                + " System.out.println(x << 33); System.out.println(x >>> 33);"
                + " long y = -16L;"
                + " System.out.println(y << 65); System.out.println(y >>> 65);"
                + " System.out.println(x & 0xff); System.out.println(x | 0xf0);"
                + " System.out.println(x ^ 0x0f);"
                + "}}"));
        out.add(c("Conversions",
                "public class Conversions { public static void main(String[] a) {"
                + " int i = 300;"
                + " System.out.println((byte) i); System.out.println((char) i);"
                + " System.out.println((short) i);"
                + " System.out.println((int) 3.99); System.out.println((int) -3.99);"
                + " System.out.println((long) 1e19); System.out.println((int) 1e19);"
                + " System.out.println((int) Double.NaN);"
                + " System.out.println((float) 0.1);"
                + " System.out.println((double) 1 / 3);"
                + " System.out.println((char) 65);"
                + "}}"));
        out.add(c("FloatEdge",
                "public class FloatEdge { public static void main(String[] a) {"
                + " double nan = Double.NaN;"
                + " System.out.println(nan < 1.0); System.out.println(nan > 1.0);"
                + " System.out.println(nan == nan);"
                + " System.out.println(1.0 / 0.0); System.out.println(-1.0 / 0.0);"
                + " System.out.println(0.0 / 0.0);"
                + " System.out.println(0.1 + 0.2);"
                + " System.out.println(Math.min(-0.0, 0.0));"
                + " float f = 1.1f; System.out.println(f * 3);"
                + "}}"));
        out.add(c("ControlFlow",
                "public class ControlFlow { public static void main(String[] a) {"
                + " int total = 0;"
                + " for (int i = 0; i < 20; i++) { if (i % 3 == 0) continue;"
                + "   if (i == 17) break; total += i; }"
                + " System.out.println(total);"
                + " int j = 0; while (j < 5) { total += j++; }"
                + " do { total--; } while (total > 20);"
                + " System.out.println(total);"
                + " for (int i = 0; i < 3; i++) for (int k = 0; k < 3; k++)"
                + "   if (k == 2) total += i * k;"
                + " System.out.println(total);"
                + "}}"));
        out.add(c("Switches",
                "public class Switches { public static void main(String[] a) {"
                // Dense -> tableswitch, sparse -> lookupswitch; both encodings
                // are variable length and are the two the writer synthesises.
                + " for (int i = 0; i < 6; i++) { switch (i) {"
                + "   case 0: System.out.println(\"zero\"); break;"
                + "   case 1: System.out.println(\"one\"); break;"
                + "   case 2: System.out.println(\"two\"); break;"
                + "   case 3: System.out.println(\"three\"); break;"
                + "   default: System.out.println(\"many\"); } }"
                + " for (int i = 0; i < 3; i++) { switch (i * 1000) {"
                + "   case 0: System.out.println(\"a\"); break;"
                + "   case 1000: System.out.println(\"b\"); break;"
                + "   default: System.out.println(\"c\"); } }"
                + " String s = \"beta\"; switch (s) {"
                + "   case \"alpha\": System.out.println(1); break;"
                + "   case \"beta\": System.out.println(2); break;"
                + "   default: System.out.println(3); }"
                + "}}"));
        out.add(c("Arrays",
                "public class Arrays { public static void main(String[] a) {"
                + " int[] xs = new int[5];"
                + " for (int i = 0; i < xs.length; i++) xs[i] = i * i;"
                + " System.out.println(java.util.Arrays.toString(xs));"
                + " byte[] bs = {1, -2, 3}; System.out.println(java.util.Arrays.toString(bs));"
                + " boolean[] flags = new boolean[2]; flags[1] = true;"
                + " System.out.println(flags[0] + \",\" + flags[1]);"
                + " char[] cs = {'h','i'}; System.out.println(new String(cs));"
                + " double[] ds = {1.5, 2.5}; System.out.println(ds[0] + ds[1]);"
                + " long[] ls = {1L << 40}; System.out.println(ls[0]);"
                + " int[][] grid = new int[3][4]; grid[2][3] = 9;"
                + " System.out.println(grid.length + \",\" + grid[0].length + \",\" + grid[2][3]);"
                + " String[] ss = new String[2]; ss[0] = \"x\";"
                + " System.out.println(ss[0] + \",\" + ss[1]);"
                + "}}"));
        out.add(c("ArrayFailures",
                "public class ArrayFailures { public static void main(String[] a) {"
                + " int[] xs = new int[2];"
                + " try { int v = xs[5]; System.out.println(v); }"
                + "   catch (ArrayIndexOutOfBoundsException e) { System.out.println(\"aioobe\"); }"
                + " try { int[] bad = new int[-1]; System.out.println(bad.length); }"
                + "   catch (NegativeArraySizeException e) { System.out.println(\"nase\"); }"
                + " int[] nil = null;"
                + " try { System.out.println(nil.length); }"
                + "   catch (NullPointerException e) { System.out.println(\"npe\"); }"
                + " try { System.out.println(1 / (xs.length - 2)); }"
                + "   catch (ArithmeticException e) { System.out.println(\"div0\"); }"
                + "}}"));
        out.add(c("Exceptions",
                "public class Exceptions {"
                + " static int f(int n) { if (n == 0) throw new IllegalStateException(\"boom\");"
                + "   return 10 / n; }"
                + " public static void main(String[] a) {"
                + " try { System.out.println(f(2)); } catch (RuntimeException e) {"
                + "   System.out.println(\"unexpected\"); }"
                + " try { f(0); } catch (IllegalStateException e) {"
                + "   System.out.println(\"caught \" + e.getMessage()); }"
                // The nearest enclosing handler must win, and finally must run
                // on both the normal and the exceptional path.
                + " try { try { f(0); } finally { System.out.println(\"inner finally\"); } }"
                + "   catch (Exception e) { System.out.println(\"outer\"); }"
                + " try { System.out.println(\"body\"); } finally { System.out.println(\"finally\"); }"
                + " StringBuilder order = new StringBuilder();"
                + " try { try { throw new java.io.IOException(\"io\"); }"
                + "       catch (RuntimeException e) { order.append(\"wrong\"); }"
                + "       finally { order.append(\"f1\"); } }"
                + "   catch (Exception e) { order.append(\"|\").append(e.getMessage()); }"
                + " System.out.println(order);"
                + "}}"));
        out.add(c("UncaughtPropagates",
                "public class UncaughtPropagates { public static void main(String[] a) {"
                + " System.out.println(\"before\");"
                + " throw new IllegalArgumentException(\"stop here\");"
                + "}}"));
        out.add(c("StringsAndBoxing",
                "public class StringsAndBoxing { public static void main(String[] a) {"
                + " String s = \"abc\";"
                + " System.out.println(s.length() + s);"
                + " System.out.println(s.toUpperCase() + s.substring(1) + s.indexOf('b'));"
                + " System.out.println(\"x\" + 1 + 2 + 'c' + 1.5 + true + null);"
                + " Integer boxed = 42; int unboxed = boxed;"
                + " System.out.println(boxed + unboxed);"
                + " System.out.println(Integer.valueOf(7).equals(7));"
                + " System.out.println(String.valueOf(3.0) + Integer.parseInt(\"12\"));"
                + " StringBuilder sb = new StringBuilder();"
                + " for (int i = 0; i < 4; i++) sb.append(i).append(',');"
                + " System.out.println(sb.toString());"
                + " System.out.println(\"a,b,,c\".split(\",\").length);"
                + "}}"));
        out.add(c("Collections",
                "import java.util.*;"
                + "public class Collections { public static void main(String[] a) {"
                + " List<String> l = new ArrayList<String>();"
                + " l.add(\"b\"); l.add(\"a\"); l.add(\"c\");"
                + " java.util.Collections.sort(l);"
                + " System.out.println(l);"
                + " Map<String,Integer> m = new HashMap<String,Integer>();"
                + " m.put(\"one\", 1); m.put(\"two\", 2);"
                + " System.out.println(m.get(\"one\") + m.get(\"two\"));"
                + " Iterator<String> it = l.iterator();"
                + " int n = 0; while (it.hasNext()) { it.next(); n++; }"
                + " System.out.println(n);"
                + " for (String s : l) System.out.print(s);"
                + " System.out.println();"
                + "}}"));
        out.add(c("Inheritance",
                "public class Inheritance {"
                + " static class Base { int v = 1;"
                + "   String who() { return \"base\"; }"
                + "   String describe() { return who() + \":\" + v; } }"
                + " static class Mid extends Base { int v = 2;"
                + "   String who() { return \"mid\"; } }"
                + " static class Leaf extends Mid {"
                + "   String who() { return \"leaf/\" + super.who(); } }"
                + " public static void main(String[] a) {"
                + " Base b = new Leaf();"
                // describe() is inherited code calling an overridden method on
                // itself; the override has to win there too.
                + " System.out.println(b.describe());"
                + " System.out.println(b.who());"
                // Fields are not virtual: the static type picks the field.
                + " System.out.println(b.v + \",\" + ((Mid) b).v);"
                + " System.out.println(b instanceof Mid);"
                + " System.out.println(b instanceof Leaf);"
                + " Object o = \"str\";"
                + " System.out.println(o instanceof String);"
                + " try { Mid m = (Mid) (Object) \"nope\"; System.out.println(m); }"
                + "   catch (ClassCastException e) { System.out.println(\"cce\"); }"
                + "}}"));
        out.add(c("InterfacesAndStatics",
                "public class InterfacesAndStatics {"
                + " interface Greeter { String greet(); }"
                + " static class En implements Greeter { public String greet() { return \"hello\"; } }"
                + " static class Fr implements Greeter { public String greet() { return \"bonjour\"; } }"
                + " static int counter;"
                + " static final String NAME;"
                + " static { NAME = \"static-init\"; counter = 10; }"
                + " static int bump() { return ++counter; }"
                + " public static void main(String[] a) {"
                + " Greeter[] gs = { new En(), new Fr() };"
                + " for (Greeter g : gs) System.out.println(g.greet());"
                + " System.out.println(NAME);"
                + " System.out.println(bump() + bump() + counter);"
                + "}}"));
        out.add(c("Recursion",
                "public class Recursion {"
                + " static long fib(int n) { return n < 2 ? n : fib(n - 1) + fib(n - 2); }"
                + " static int depth(int n) { return n == 0 ? 0 : 1 + depth(n - 1); }"
                + " public static void main(String[] a) {"
                + " System.out.println(fib(20));"
                + " System.out.println(depth(100));"
                + "}}"));
        out.add(c("StackShuffles",
                "public class StackShuffles {"
                + " static int sideEffect(StringBuilder sb, int v) { sb.append(v); return v; }"
                + " public static void main(String[] a) {"
                + " StringBuilder sb = new StringBuilder();"
                // Compound assignment on an array element compiles to
                // dup2/dup_x2 shapes; long ones exercise the category-2 cases.
                + " int[] xs = new int[3];"
                + " xs[1] += 5; xs[1] *= 3; xs[sideEffect(sb, 2)] -= 4;"
                + " System.out.println(java.util.Arrays.toString(xs) + sb);"
                + " long[] ls = new long[2];"
                + " ls[0] += 7L; ls[0] *= 6L; ls[1] = ls[0]--;"
                + " System.out.println(ls[0] + \",\" + ls[1]);"
                + " double[] ds = new double[1]; ds[0] += 1.5; ds[0] /= 0.5;"
                + " System.out.println(ds[0]);"
                + " int i = 0; i = i++ + ++i; System.out.println(i);"
                + "}}"));
        out.add(c("TernaryAndLogic",
                "public class TernaryAndLogic { public static void main(String[] a) {"
                + " int x = 5;"
                + " System.out.println(x > 3 ? \"big\" : \"small\");"
                + " boolean t = true, f = false;"
                + " System.out.println(t && f); System.out.println(t || f);"
                + " System.out.println(!t); System.out.println(t ^ f);"
                + " Object nil = null;"
                + " System.out.println(nil == null ? \"null\" : nil.toString());"
                + " System.out.println(x > 1 && x < 10 || x == 100);"
                + " int count = 0;"
                + " if (f && ++count > 0) { count += 100; }"
                + " System.out.println(count);"
                + "}}"));
        // Lambdas and method references. These do not survive as bytecode:
        // InterpLambdaDesugar rewrites each invokedynamic into a real class
        // before the bundle is written, because neither device target can spin
        // one at run time. The JVM runs the original indy, so this is a direct
        // check that the rewrite means the same thing.
        out.add(c("Lambdas",
                "import java.util.*;"
                + "public class Lambdas {"
                + " interface Op { int apply(int v); default Op twice() { return v -> apply(apply(v)); } }"
                + " interface Two { String join(String a, int b); }"
                + " static int half(int v) { return v / 2; }"
                + " private final int base = 10;"
                + " int bound(int v) { return base + v; }"
                + " public static void main(String[] a) {"
                + " Op inc = v -> v + 1;"
                + " System.out.println(inc.apply(41));"
                + " System.out.println(inc.twice().apply(1));"
                + " Op ref = Lambdas::half;"
                + " System.out.println(ref.apply(9));"
                + " Lambdas self = new Lambdas();"
                + " Op boundRef = self::bound;"
                + " System.out.println(boundRef.apply(5));"
                + " Two t = (s, n) -> s + n;"
                + " System.out.println(t.join(\"x\", 3));"
                + " List<String> l = new ArrayList<String>();"
                + " l.add(\"b\"); l.add(\"a\"); l.add(\"c\");"
                + " Collections.sort(l, (p, q) -> p.compareTo(q));"
                + " System.out.println(l);"
                + " Runnable r = () -> System.out.println(\"ran\");"
                + " r.run();"
                + "}}"));
        // A lambda that captures a long and returns a boxed value: the
        // metafactory's adaptation rules -- widen, box, unbox, cast -- are
        // where a hand-written desugarer goes wrong.
        out.add(c("LambdaAdaptation",
                "public class LambdaAdaptation {"
                + " interface Boxed { Object get(Integer v); }"
                + " interface Prim { long get(long v); }"
                + " public static void main(String[] a) {"
                + " final long captured = 1L << 40;"
                + " Prim p = v -> v + captured;"
                + " System.out.println(p.get(2));"
                + " Boxed b = v -> Integer.valueOf(v.intValue() * 2);"
                + " System.out.println(b.get(Integer.valueOf(21)));"
                + "}}"));
        // Enums. java.lang.Enum cannot be subclassed from source, so no shim
        // for it exists or can; the interpreter answers name/ordinal/compareTo
        // itself and rewrites valueOf against the bundle.
        out.add(c("Enums",
                "public class Enums {"
                + " enum Color { RED, GREEN, BLUE;"
                + "   String low() { return name().toLowerCase(); } }"
                + " public static void main(String[] a) {"
                + " for (Color c : Color.values()) System.out.println(c + \" \" + c.ordinal());"
                + " Color g = Color.GREEN;"
                + " switch (g) { case GREEN: System.out.println(\"green\"); break;"
                + "              default: System.out.println(\"other\"); }"
                + " System.out.println(Color.valueOf(\"BLUE\"));"
                + " System.out.println(g.low());"
                + " System.out.println(g.compareTo(Color.RED));"
                + " System.out.println(g.equals(Color.GREEN));"
                + " System.out.println(Color.values().length);"
                + " try { Color.valueOf(\"PINK\"); } catch (IllegalArgumentException e) {"
                + "   System.out.println(\"no PINK\"); }"
                + "}}"));
        // isAssignableFrom over pushed types: the hierarchy is in the bundle,
        // and it is the other type test Java offers without reflection.
        out.add(c("ClassAssignability",
                "public class ClassAssignability {"
                + " interface Marker {}"
                + " static class Base implements Marker {}"
                + " static class Child extends Base {}"
                + " static class Other {}"
                + " static class Task implements Runnable { public void run() {} }"
                + " public static void main(String[] a) {"
                + " System.out.println(Base.class.isAssignableFrom(Child.class));"
                + " System.out.println(Child.class.isAssignableFrom(Base.class));"
                + " System.out.println(Base.class.isAssignableFrom(Base.class));"
                + " System.out.println(Marker.class.isAssignableFrom(Child.class));"
                + " System.out.println(Base.class.isAssignableFrom(Other.class));"
                + " System.out.println(Base.class.isAssignableFrom(String.class));"
                + " System.out.println(Base[].class.isAssignableFrom(Base[].class));"
                + " System.out.println(Base[].class.isAssignableFrom(Child[].class));"
                + " System.out.println(Runnable.class.isAssignableFrom(Task.class));"
                + " System.out.println(Object.class.isAssignableFrom(Base.class));"
                + " System.out.println(Runnable.class.isAssignableFrom(Base.class));"
                + " System.out.println(Object.class.isAssignableFrom(Base[].class));"
                + " System.out.println(Cloneable.class.isAssignableFrom(Base[].class));"
                + " System.out.println(java.io.Serializable.class.isAssignableFrom(Base[].class));"
                + " System.out.println(Object[].class.isAssignableFrom(Base[].class));"
                + " System.out.println(Runnable.class.isAssignableFrom(Base[].class));"
                + "}}"));
        // An enum constant that overrides toString. The interpreter answers
        // name() itself, and answering it for toString as well made the
        // override apply to interpreted callers and not to host ones -- so the
        // same constant printed two ways depending on who asked.
        out.add(c("EnumToStringOverride",
                "public class EnumToStringOverride {"
                + " enum Color { RED, GREEN;"
                + "   public String toString() { return name().toLowerCase(); } }"
                + " public static void main(String[] a) {"
                + " System.out.println(Color.RED);"
                + " System.out.println(\"v=\" + Color.GREEN);"
                + " System.out.println(String.valueOf(Color.RED));"
                + " StringBuilder sb = new StringBuilder(); sb.append(Color.GREEN);"
                + " System.out.println(sb.toString());"
                + " System.out.println(Color.RED.name() + \" \" + Color.valueOf(\"GREEN\"));"
                + "}}"));
        // An `assert` compiles to a <clinit> that reads
        // ThisClass.class.desiredAssertionStatus(), so a class containing one
        // failed to initialize before that question had an answer -- the push
        // died before the program ran.
        out.add(c("Assertions",
                "public class Assertions {"
                + " static int sum(int a, int b) { assert a >= 0; return a + b; }"
                + " public static void main(String[] a) {"
                + " int n = 1;"
                + " assert n == 1 : \"never\";"
                + " System.out.println(\"ran \" + sum(n, 2));"
                + "}}"));
        // A class token passed where the host declares Object is stored, not
        // converted: substituting the nearest host ancestor's Class there put
        // Object.class in the collection, and reading it back no longer equalled
        // the literal the program still held.
        out.add(c("ClassTokenIdentity",
                "import java.util.*;"
                + "public class ClassTokenIdentity {"
                + " static class Thing {}"
                + " public static void main(String[] a) {"
                + " List list = new ArrayList();"
                + " list.add(Thing.class);"
                + " list.add(String.class);"
                + " System.out.println(list.get(0) == Thing.class);"
                + " System.out.println(list.get(1) == String.class);"
                + " Map m = new HashMap(); m.put(Thing.class, \"t\");"
                + " System.out.println(m.get(Thing.class));"
                + "}}"));
        // An intersection cast names extra interfaces and extra erasures of the
        // same method through altMetafactory. A synthesized class carrying only
        // the first interface fails the call site's own cast, and a call through
        // the other interface's erased signature resolves to nothing.
        out.add(c("IntersectionLambda",
                "import java.io.Serializable;"
                + "public class IntersectionLambda {"
                + " interface StringSupplier { String get(); }"
                + " interface ObjectSupplier { Object get(); }"
                + " public static void main(String[] a) {"
                + " StringSupplier s = (StringSupplier & ObjectSupplier) () -> \"x\";"
                + " System.out.println(s.get());"
                + " System.out.println(((ObjectSupplier) s).get());"
                + " System.out.println(s instanceof ObjectSupplier);"
                + " Runnable r = (Runnable & Serializable) () -> System.out.println(\"ran\");"
                + " r.run();"
                + " System.out.println(r instanceof Serializable);"
                + "}}"));
        // An interpreted class whose toString the host has to reach when it
        // converts the object to a string.
        out.add(c("ToStringOverride",
                "public class ToStringOverride {"
                + " static class P { public String toString() { return \"P!\"; } }"
                + " public static void main(String[] a) {"
                + " System.out.println(new P());"
                + " System.out.println(\"\" + new P());"
                + " System.out.println(String.valueOf(new P()));"
                + " StringBuilder sb = new StringBuilder(); sb.append(new P());"
                + " System.out.println(sb.toString());"
                + "}}"));
        // Array clone and casts of arrays of an interpreted type -- both are
        // what an enum's generated values() does, and neither has a host class
        // to resolve against.
        out.add(c("ArrayCloneAndCast",
                "public class ArrayCloneAndCast {"
                + " static class E { final int v; E(int v) { this.v = v; } }"
                + " public static void main(String[] a) {"
                + " E[] src = new E[]{ new E(1), new E(2) };"
                + " E[] copy = (E[])src.clone();"
                + " System.out.println(copy.length + \" \" + copy[1].v + \" \" + (copy == src));"
                + " int[] nums = new int[]{1,2,3};"
                + " int[] n2 = (int[])nums.clone(); n2[0] = 9;"
                + " System.out.println(nums[0] + \" \" + n2[0]);"
                + " Object o = src;"
                + " System.out.println(o instanceof E[]);"
                + "}}"));
        // `new Object[0] instanceof E[]` is deliberately absent: the
        // interpreter represents every reference array as Object[] and has no
        // component type to compare, so it answers by inspecting elements and
        // an empty array satisfies anything. That deviation is asserted
        // explicitly in InterpRuntimeContractTest rather than hidden here.
        // Boolean and other narrow fields: the value on the stack is an int,
        // the field is declared Z, and the two have to agree about how it is
        // boxed on the way in and out.
        out.add(c("NarrowFields",
                "public class NarrowFields {"
                + " private boolean flag; private byte b; private short s; private char ch;"
                + " boolean once() { boolean was = flag; flag = true; return was; }"
                + " public static void main(String[] a) {"
                + " NarrowFields n = new NarrowFields();"
                + " System.out.println(n.once());"
                + " System.out.println(n.once());"
                + " n.b = 7; n.s = 300; n.ch = 'q';"
                + " System.out.println(n.b + \" \" + n.s + \" \" + n.ch);"
                + " boolean[] arr = new boolean[2]; arr[1] = true;"
                + " System.out.println(arr[0] + \" \" + arr[1]);"
                + " System.out.println(n.flag ? \"set\" : \"unset\");"
                + "}}"));
        // Monitors. Contended, so ignoring them loses increments; nested and
        // re-entrant, so structured locking is exercised; and with a throw out
        // of a guarded region, so the lock has to be released on that path too
        // -- a leak there hangs the next acquirer rather than failing.
        //
        // Each counter is guarded by exactly one monitor. Guarding one counter
        // with two different monitors -- a static synchronized method takes the
        // class's, a synchronized block takes whatever it names -- is a race,
        // and the first version of this case had it: the JVM's own answer moved
        // between runs, which is the oracle telling you the test is wrong.
        out.add(c("Monitors",
                "public class Monitors {"
                + " static int byLock; static int byMethod; int byInstance;"
                + " static final Object LOCK = new Object();"
                + " static synchronized void bumpStatic() { byMethod++; }"
                + " synchronized void bumpInstance() { byInstance++; }"
                + " static void guarded() { synchronized (LOCK) { byLock++; } }"
                + " static void nested() { synchronized (LOCK) { synchronized (LOCK) { byLock++; } } }"
                + " static void thrower() {"
                + "   try { synchronized (LOCK) { throw new IllegalStateException(\"x\"); } }"
                + "   catch (IllegalStateException e) { byLock++; } }"
                + " public static void main(String[] a) throws Exception {"
                + " final Monitors shared = new Monitors();"
                + " Thread[] t = new Thread[4];"
                + " for (int i = 0; i < t.length; i++) {"
                + "   t[i] = new Thread(new Runnable() { public void run() {"
                + "     for (int j = 0; j < 2000; j++) {"
                + "       bumpStatic(); shared.bumpInstance(); guarded(); nested(); } } }); }"
                + " for (int i = 0; i < t.length; i++) { t[i].start(); }"
                + " for (int i = 0; i < t.length; i++) { t[i].join(); }"
                + " thrower();"
                + " synchronized (LOCK) { byLock++; }"
                + " System.out.println(byMethod + \" \" + shared.byInstance + \" \" + byLock);"
                + "}}"));
        // wait/notify, which is the reason the monitors are the objects' own.
        // A private lock table keyed by identity would give mutual exclusion
        // and nothing else: wait() demands the caller own that object's
        // monitor, and would throw IllegalMonitorStateException here.
        out.add(c("WaitNotify",
                "public class WaitNotify {"
                + " static final Object LOCK = new Object();"
                + " static int value = -1; static boolean ready;"
                + " public static void main(String[] a) throws Exception {"
                + " Thread consumer = new Thread(new Runnable() { public void run() {"
                + "   synchronized (LOCK) {"
                + "     while (!ready) {"
                + "       try { LOCK.wait(); } catch (InterruptedException e) { } }"
                + "     System.out.println(\"got \" + value); } } });"
                + " Thread producer = new Thread(new Runnable() { public void run() {"
                + "   synchronized (LOCK) { value = 42; ready = true; LOCK.notifyAll(); } } });"
                + " consumer.start();"
                + " Thread.sleep(100);"
                + " producer.start();"
                + " consumer.join(); producer.join();"
                + " System.out.println(\"done\");"
                + "}}"));
        return out.stream();
    }

    private static Arguments c(String name, String source) {
        return Arguments.of(name, source);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    @DisplayName("interpreted execution matches the JVM")
    void interpreterMatchesTheJvm(String className, String source) throws Exception {
        InterpTestHarness.Result[] r = InterpTestHarness.runBoth(className, source);
        InterpTestHarness.Result jvm = r[0];
        InterpTestHarness.Result interp = r[1];

        assertEquals(jvm.output, interp.output,
                "stdout differs for " + className
                        + "\n--- jvm ---\n" + jvm
                        + "\n--- interpreter ---\n" + interp);
        assertEquals(String.valueOf(jvm.failure), String.valueOf(interp.failure),
                "failure differs for " + className
                        + "\n--- jvm ---\n" + jvm
                        + "\n--- interpreter ---\n" + interp);
    }
}

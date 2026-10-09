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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

/// Redirects single members of JDK classes the device HAS, but with fewer
/// members than the JDK's, to the static methods that stand in for them.
///
/// A class the device lacks altogether is moved whole by a row of
/// [Relocation#JDK_SHIMS]. That cannot work for `java.lang.Class`,
/// `ClassLoader`, `Thread`, `java.util.Objects` or `java.util.Locale`: the
/// device has each of them, every other class is compiled against the
/// device's, and only some of their members are missing or -- for the
/// resource lookups -- mean something else where there is no classpath. So
/// the call is rewritten instead and the class is left alone:
///
/// - an instance method becomes a static one taking the receiver first:
///   `cls.getResource(name)` is `Resources.getResource(cls, name)`;
/// - a static method or a static field read moves to the member of the same
///   name: `Locale.FRANCE` is `JdkLocale.FRANCE`;
/// - a constructor the device lacks becomes the one it has, with the
///   arguments adjusted in place.
///
/// The same is done to a method reference (`Objects::isNull`), which a class
/// file holds as a method handle among the arguments of an `invokedynamic`.
///
/// The interfaces are treated the same way. The device's `Comparator`,
/// `Predicate`, `Function` and collection interfaces are there, without the
/// default and static methods the JDK's gained later: `comparator.reversed()`
/// is `JdkFunctions.reversed(comparator)`, `list.stream()` is
/// `JdkCollections.stream(list)`, `List.of(a, b)` is
/// `JdkCollections.listOf(a, b)`. What a stream call answers is not the
/// device's own `java.util.stream.Stream` but the one the shared JDK classes
/// carry, by a row of [Relocation#JDK_SHIMS].
///
/// #### Where it runs
///
/// Inside [ClassRelocator#remap], BEFORE the names are relocated, so every
/// rule is written in the names an application is compiled against and the
/// relocation that follows moves the `java/net/URL` in a rewritten
/// descriptor like any other. It runs only when a desktop layer is active:
/// the targets live in the shared JDK classes, and an application of the
/// Android layer alone was never compiled against a JDK that has these
/// members. The shared JDK classes themselves are not rewritten --
/// `Resources` reads the bundle through the very `Class.getResourceAsStream`
/// an application's call is redirected away from -- and neither is the
/// Android runtime, which already asks for resources by their flat names.
///
/// A rewritten class holds nothing a rule matches, so a second run over it
/// changes nothing.
///
/// #### What is deliberately absent
///
/// Anything else of `ClassLoader` -- `defineClass`, a `URLClassLoader` -- has
/// no rule and so reaches the compliance check as the build error it is: a
/// device loads no classes at run time. `loadClass(String)` is the one
/// exception, because all it does with the one loader there is, is what
/// `Class.forName` does: answer a class the application already has.
final class CompatRewrites {

    private static final String RESOURCES = Relocation.JDK_PACKAGE + "Resources";
    private static final String OBJECTS = Relocation.JDK_PACKAGE + "JdkObjects";
    private static final String LOCALE = Relocation.JDK_PACKAGE + "JdkLocale";

    private static final String JAVA_LOCALE = "java/util/Locale";
    private static final String STRING = "Ljava/lang/String;";

    private static final String SYSTEM = Relocation.JDK_PACKAGE + "JdkSystem";
    /// The methods of String that take a regular expression, in the JDK's
    /// syntax.
    private static final String REGEX = Relocation.JDK_PACKAGE + "JdkRegex";
    private static final String STRINGS = Relocation.JDK_PACKAGE + "JdkStrings";
    private static final String NUMBERS = Relocation.JDK_PACKAGE + "JdkNumbers";
    private static final String FUNCTIONS = Relocation.JDK_PACKAGE + "JdkFunctions";
    private static final String COLLECTIONS = Relocation.JDK_PACKAGE + "JdkCollections";
    private static final String TIME = Relocation.JDK_PACKAGE + "JdkTime";
    private static final String OBJECT = "Ljava/lang/Object;";

    /// What `stream()` and `parallelStream()` are declared to return, as an
    /// application names it and as it reads once relocated.
    private static final String STREAM = "Ljava/util/stream/Stream;";
    private static final String STREAM_MOVED = "L" + Relocation.JDK_PACKAGE + "Stream;";
    /// The JDK's collection types an application is likely to hold a
    /// reference of when it asks for a stream. Any other type is covered by
    /// [#anyOwner].
    private static final String[] COLLECTION_OWNERS = {"Collection", "List", "Set", "ArrayList", "LinkedList",
        "HashSet", "LinkedHashSet", "TreeSet", "SortedSet", "NavigableSet", "Queue", "Deque", "ArrayDeque", "Vector",
        "Stack", "PriorityQueue", "AbstractList", "AbstractCollection", "AbstractSet", "AbstractQueue",
        "AbstractSequentialList"};
    /// The framework's own math class: the device's `Math` has no
    /// transcendental functions, and this one has them under the JDK's names.
    private static final String MATH_UTIL = "com/codename1/util/MathUtil";

    /// Instance methods, as `owner.name descriptor`, to the class holding
    /// the static method that takes the receiver as its first parameter.
    ///
    /// In all three tables a target of the form `class#name` names a member
    /// called something else than the one it stands in for, which is how two
    /// methods that differ only in their return type -- `Long.decode` and
    /// `Integer.decode` -- share one class. `class#name(descriptor)` gives
    /// the stand-in's descriptor as well, for an instance method whose
    /// stand-in takes the receiver as a wider type than the one the call
    /// names: `ArrayList.stream()` and `Set.stream()` are both
    /// `stream(Collection)`.
    ///
    /// An instance method is looked up here whether it is called on a class
    /// or on an interface.
    private static final Map<String, String> VIRTUAL = new HashMap<String, String>();
    /// Static methods, as `owner.name descriptor`, to the class holding the
    /// static method of the same name and descriptor.
    private static final Map<String, String> STATIC = new HashMap<String, String>();
    /// Static fields, as `owner.name`, to the class holding the field of the
    /// same name and type.
    private static final Map<String, String> FIELDS = new HashMap<String, String>();

    static {
        String url = "(Ljava/lang/String;)Ljava/net/URL;";
        String stream = "(Ljava/lang/String;)Ljava/io/InputStream;";
        String urls = "(Ljava/lang/String;)Ljava/util/Enumeration;";
        VIRTUAL.put("java/lang/Class.getResource" + url, RESOURCES);
        VIRTUAL.put("java/lang/Class.getResourceAsStream" + stream, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.getResource" + url, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.getResourceAsStream" + stream, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.getResources" + urls, RESOURCES);
        VIRTUAL.put("java/lang/ClassLoader.loadClass(Ljava/lang/String;)Ljava/lang/Class;", RESOURCES);
        STATIC.put("java/lang/ClassLoader.getSystemResource" + url, RESOURCES);
        STATIC.put("java/lang/ClassLoader.getSystemResourceAsStream" + stream, RESOURCES);
        STATIC.put("java/lang/ClassLoader.getSystemResources" + urls, RESOURCES);
        VIRTUAL.put("java/lang/Thread.getContextClassLoader()Ljava/lang/ClassLoader;", RESOURCES);
        VIRTUAL.put("java/lang/Thread.setContextClassLoader(Ljava/lang/ClassLoader;)V", RESOURCES);

        String supplier = "Ljava/util/function/Supplier;";
        String object = "Ljava/lang/Object;";
        STATIC.put("java/util/Objects.isNull(" + object + ")Z", OBJECTS);
        STATIC.put("java/util/Objects.requireNonNull(" + object + supplier + ")" + object, OBJECTS);
        STATIC.put("java/util/Objects.requireNonNullElse(" + object + object + ")" + object, OBJECTS);
        STATIC.put("java/util/Objects.requireNonNullElseGet(" + object + supplier + ")" + object, OBJECTS);
        STATIC.put("java/util/Objects.checkIndex(II)I", OBJECTS);
        STATIC.put("java/util/Objects.checkFromToIndex(III)I", OBJECTS);
        STATIC.put("java/util/Objects.checkFromIndexSize(III)I", OBJECTS);

        STATIC.put(JAVA_LOCALE + ".forLanguageTag(" + STRING + ")Ljava/util/Locale;", LOCALE);
        STATIC.put(JAVA_LOCALE + ".getAvailableLocales()[Ljava/util/Locale;", LOCALE);
        for (String getter : new String[] {"toLanguageTag", "getVariant", "getScript", "getDisplayName",
            "getDisplayLanguage", "getDisplayCountry", "toString"}) {
            VIRTUAL.put(JAVA_LOCALE + "." + getter + "()" + STRING, LOCALE);
        }
        VIRTUAL.put(JAVA_LOCALE + ".equals(" + object + ")Z", LOCALE);
        VIRTUAL.put(JAVA_LOCALE + ".hashCode()I", LOCALE);
        for (String constant : new String[] {"ENGLISH", "FRENCH", "GERMAN", "ITALIAN", "JAPANESE", "KOREAN",
            "CHINESE", "SIMPLIFIED_CHINESE", "TRADITIONAL_CHINESE", "FRANCE", "GERMANY", "ITALY", "JAPAN", "KOREA",
            "CHINA", "PRC", "TAIWAN", "UK", "US", "CANADA", "CANADA_FRENCH", "ROOT"}) {
            FIELDS.put(JAVA_LOCALE + "." + constant, LOCALE);
        }
    }

    static {
        String sys = "java/lang/System.";
        // getProperty(String) exists on the device and answers null for
        // every key; the rule is what gives user.home a value.
        STATIC.put(sys + "getProperty(" + STRING + ")" + STRING, SYSTEM);
        STATIC.put(sys + "getProperty(" + STRING + STRING + ")" + STRING, SYSTEM);
        STATIC.put(sys + "setProperty(" + STRING + STRING + ")" + STRING, SYSTEM);
        STATIC.put(sys + "clearProperty(" + STRING + ")" + STRING, SYSTEM);
        STATIC.put(sys + "getenv(" + STRING + ")" + STRING, SYSTEM);
        STATIC.put(sys + "getenv()Ljava/util/Map;", SYSTEM);
        STATIC.put(sys + "lineSeparator()" + STRING, SYSTEM);
        // The device's exit ends the process under the framework; this one
        // runs the shutdown hooks and leaves through Display.
        STATIC.put(sys + "exit(I)V", SYSTEM);

        String runtime = "java/lang/Runtime.";
        String thread = "Ljava/lang/Thread;";
        VIRTUAL.put(runtime + "exit(I)V", SYSTEM);
        VIRTUAL.put(runtime + "halt(I)V", SYSTEM);
        VIRTUAL.put(runtime + "addShutdownHook(" + thread + ")V", SYSTEM);
        VIRTUAL.put(runtime + "removeShutdownHook(" + thread + ")Z", SYSTEM);
        VIRTUAL.put(runtime + "availableProcessors()I", SYSTEM);
        VIRTUAL.put(runtime + "maxMemory()J", SYSTEM);

        String thr = "java/lang/Thread.";
        String handler = "Ljava/lang/Thread$UncaughtExceptionHandler;";
        VIRTUAL.put(thr + "setDaemon(Z)V", SYSTEM);
        VIRTUAL.put(thr + "isDaemon()Z", SYSTEM);
        VIRTUAL.put(thr + "setName(" + STRING + ")V", SYSTEM);
        VIRTUAL.put(thr + "isInterrupted()Z", SYSTEM);
        VIRTUAL.put(thr + "getId()J", SYSTEM);
        VIRTUAL.put(thr + "join(J)V", SYSTEM);
        VIRTUAL.put(thr + "join(JI)V", SYSTEM);
        VIRTUAL.put(thr + "setUncaughtExceptionHandler(" + handler + ")V", SYSTEM);
        VIRTUAL.put(thr + "getUncaughtExceptionHandler()" + handler, SYSTEM);
        STATIC.put(thr + "interrupted()Z", SYSTEM);
        STATIC.put(thr + "sleep(JI)V", SYSTEM);
        STATIC.put(thr + "setDefaultUncaughtExceptionHandler(" + handler + ")V", SYSTEM);
        STATIC.put(thr + "getDefaultUncaughtExceptionHandler()" + handler, SYSTEM);
        VIRTUAL.put("java/lang/Throwable.printStackTrace(Ljava/io/PrintWriter;)V", SYSTEM);

        String str = "java/lang/String.";
        String seq = "Ljava/lang/CharSequence;";
        STATIC.put(str + "join(" + seq + "[" + seq + ")" + STRING, STRINGS);
        STATIC.put(str + "join(" + seq + "Ljava/lang/Iterable;)" + STRING, STRINGS);
        STATIC.put(str + "valueOf([C)" + STRING, STRINGS);
        STATIC.put(str + "format(Ljava/util/Locale;" + STRING + "[Ljava/lang/Object;)" + STRING, STRINGS);
        VIRTUAL.put(str + "matches(" + STRING + ")Z", REGEX);
        VIRTUAL.put(str + "toLowerCase(Ljava/util/Locale;)" + STRING, STRINGS);
        for (String builder : new String[] {"java/lang/StringBuilder", "java/lang/StringBuffer"}) {
            VIRTUAL.put(builder + ".indexOf(" + STRING + ")I", STRINGS);
            VIRTUAL.put(builder + ".indexOf(" + STRING + "I)I", STRINGS);
            VIRTUAL.put(builder + ".lastIndexOf(" + STRING + ")I", STRINGS);
            VIRTUAL.put(builder + ".lastIndexOf(" + STRING + "I)I", STRINGS);
            VIRTUAL.put(builder + ".substring(I)" + STRING, STRINGS);
            VIRTUAL.put(builder + ".substring(II)" + STRING, STRINGS);
            VIRTUAL.put(builder + ".replace(II" + STRING + ")L" + builder + ";", STRINGS);
        }
        VIRTUAL.put("java/lang/StringBuilder.insert(I[C)Ljava/lang/StringBuilder;", STRINGS);

        String chr = "java/lang/Character.";
        STATIC.put(chr + "toString(C)" + STRING, STRINGS);
        for (String test : new String[] {"isLetter", "isLetterOrDigit", "isISOControl"}) {
            STATIC.put(chr + test + "(C)Z", STRINGS);
            STATIC.put(chr + test + "(I)Z", STRINGS);
        }
        for (String test : new String[] {"isAlphabetic", "isDigit", "isLowerCase", "isUpperCase", "isSpaceChar"}) {
            STATIC.put(chr + test + "(I)Z", STRINGS);
        }
        STATIC.put(chr + "toLowerCase(I)I", STRINGS);
        STATIC.put(chr + "toUpperCase(I)I", STRINGS);
        STATIC.put(chr + "forDigit(II)C", STRINGS);
        STATIC.put(chr + "digit(II)I", STRINGS);
        STATIC.put(chr + "getNumericValue(C)I", STRINGS);
        STATIC.put(chr + "getNumericValue(I)I", STRINGS);

        String integer = "java/lang/Integer.";
        for (String op : new String[] {"max", "min", "sum", "rotateLeft", "rotateRight"}) {
            STATIC.put(integer + op + "(II)I", NUMBERS);
        }
        for (String op : new String[] {"bitCount", "highestOneBit", "lowestOneBit", "numberOfTrailingZeros",
            "reverse"}) {
            STATIC.put(integer + op + "(I)I", NUMBERS);
        }
        STATIC.put(integer + "decode(" + STRING + ")Ljava/lang/Integer;", NUMBERS + "#decodeInteger");

        String lng = "java/lang/Long.";
        for (String op : new String[] {"max", "min", "sum"}) {
            STATIC.put(lng + op + "(JJ)J", NUMBERS);
        }
        STATIC.put(lng + "rotateLeft(JI)J", NUMBERS);
        STATIC.put(lng + "rotateRight(JI)J", NUMBERS);
        for (String op : new String[] {"signum", "bitCount", "numberOfLeadingZeros", "numberOfTrailingZeros"}) {
            STATIC.put(lng + op + "(J)I", NUMBERS);
        }
        for (String op : new String[] {"highestOneBit", "lowestOneBit", "reverse"}) {
            STATIC.put(lng + op + "(J)J", NUMBERS);
        }
        for (String op : new String[] {"toHexString", "toOctalString", "toBinaryString"}) {
            STATIC.put(lng + op + "(J)" + STRING, NUMBERS);
        }
        STATIC.put(lng + "decode(" + STRING + ")Ljava/lang/Long;", NUMBERS + "#decodeLong");
        STATIC.put(lng + "valueOf(" + STRING + ")Ljava/lang/Long;", NUMBERS + "#longValueOf");
        STATIC.put(lng + "valueOf(" + STRING + "I)Ljava/lang/Long;", NUMBERS + "#longValueOf");
        VIRTUAL.put(lng + "shortValue()S", NUMBERS);

        String shrt = "java/lang/Short.";
        STATIC.put(shrt + "decode(" + STRING + ")Ljava/lang/Short;", NUMBERS + "#decodeShort");
        STATIC.put(shrt + "valueOf(" + STRING + ")Ljava/lang/Short;", NUMBERS + "#shortValueOf");
        STATIC.put(shrt + "valueOf(" + STRING + "I)Ljava/lang/Short;", NUMBERS + "#shortValueOf");
        STATIC.put(shrt + "toString(S)" + STRING, NUMBERS);
        VIRTUAL.put(shrt + "byteValue()B", NUMBERS);
        String bte = "java/lang/Byte.";
        STATIC.put(bte + "decode(" + STRING + ")Ljava/lang/Byte;", NUMBERS + "#decodeByte");
        STATIC.put(bte + "valueOf(" + STRING + ")Ljava/lang/Byte;", NUMBERS + "#byteValueOf");
        STATIC.put(bte + "valueOf(" + STRING + "I)Ljava/lang/Byte;", NUMBERS + "#byteValueOf");
        STATIC.put(bte + "toString(B)" + STRING, NUMBERS);
        VIRTUAL.put(bte + "shortValue()S", NUMBERS);

        STATIC.put("java/lang/Double.isFinite(D)Z", NUMBERS);
        STATIC.put("java/lang/Float.isFinite(F)Z", NUMBERS);
        for (String op : new String[] {"max", "min", "sum"}) {
            STATIC.put("java/lang/Double." + op + "(DD)D", NUMBERS);
            STATIC.put("java/lang/Float." + op + "(FF)F", NUMBERS);
        }

        String bool = "java/lang/Boolean.";
        STATIC.put(bool + "toString(Z)" + STRING, NUMBERS);
        STATIC.put(bool + "getBoolean(" + STRING + ")Z", NUMBERS);
        for (String op : new String[] {"logicalAnd", "logicalOr", "logicalXor"}) {
            STATIC.put(bool + op + "(ZZ)Z", NUMBERS);
        }

        String math = "java/lang/Math.";
        for (String fn : new String[] {"exp", "log", "log10", "asin", "acos", "atan", "ulp"}) {
            STATIC.put(math + fn + "(D)D", MATH_UTIL);
        }
        STATIC.put(math + "pow(DD)D", MATH_UTIL);
        STATIC.put(math + "atan2(DD)D", MATH_UTIL);
        STATIC.put(math + "copySign(DD)D", MATH_UTIL);
        STATIC.put(math + "scalb(DI)D", MATH_UTIL);
        STATIC.put(math + "copySign(FF)F", NUMBERS);
        STATIC.put(math + "random()D", NUMBERS);
        STATIC.put(math + "signum(D)D", NUMBERS);
        STATIC.put(math + "signum(F)F", NUMBERS);
        STATIC.put(math + "hypot(DD)D", NUMBERS);
        for (String fn : new String[] {"cbrt", "rint", "sinh", "cosh", "tanh"}) {
            STATIC.put(math + fn + "(D)D", NUMBERS);
        }
        for (String op : new String[] {"floorDiv", "floorMod", "addExact", "subtractExact", "multiplyExact"}) {
            STATIC.put(math + op + "(II)I", NUMBERS);
            STATIC.put(math + op + "(JJ)J", NUMBERS);
        }
        for (String op : new String[] {"incrementExact", "decrementExact", "negateExact"}) {
            STATIC.put(math + op + "(I)I", NUMBERS);
            STATIC.put(math + op + "(J)J", NUMBERS);
        }
        STATIC.put(math + "toIntExact(J)I", NUMBERS);
        // StrictMath is not on a device at all; its results are Math's here.
        String[] keys = STATIC.keySet().toArray(new String[0]);
        for (String key : keys) {
            if (key.startsWith(math)) {
                STATIC.put("java/lang/StrictMath." + key.substring(math.length()), STATIC.get(key));
            }
        }
    }

    static {
        // The device's Comparator and function interfaces are their one
        // abstract method each; the defaults and factories are stand-ins.
        String cmp = "java/util/Comparator.";
        String cmpT = "Ljava/util/Comparator;";
        String fn = "java/util/function/";
        String fnT = "L" + fn + "Function;";
        STATIC.put(cmp + "naturalOrder()" + cmpT, FUNCTIONS);
        STATIC.put(cmp + "reverseOrder()" + cmpT, FUNCTIONS);
        STATIC.put(cmp + "nullsFirst(" + cmpT + ")" + cmpT, FUNCTIONS);
        STATIC.put(cmp + "nullsLast(" + cmpT + ")" + cmpT, FUNCTIONS);
        STATIC.put(cmp + "comparing(" + fnT + ")" + cmpT, FUNCTIONS);
        STATIC.put(cmp + "comparing(" + fnT + cmpT + ")" + cmpT, FUNCTIONS);
        VIRTUAL.put(cmp + "reversed()" + cmpT, FUNCTIONS);
        VIRTUAL.put(cmp + "thenComparing(" + cmpT + ")" + cmpT, FUNCTIONS);
        VIRTUAL.put(cmp + "thenComparing(" + fnT + ")" + cmpT, FUNCTIONS);
        VIRTUAL.put(cmp + "thenComparing(" + fnT + cmpT + ")" + cmpT, FUNCTIONS);
        for (String kind : new String[] {"Int", "Long", "Double"}) {
            String to = "L" + fn + "To" + kind + "Function;";
            STATIC.put(cmp + "comparing" + kind + "(" + to + ")" + cmpT, FUNCTIONS);
            VIRTUAL.put(cmp + "thenComparing" + kind + "(" + to + ")" + cmpT, FUNCTIONS);
        }

        String predT = "L" + fn + "Predicate;";
        VIRTUAL.put(fn + "Predicate.and(" + predT + ")" + predT, FUNCTIONS);
        VIRTUAL.put(fn + "Predicate.or(" + predT + ")" + predT, FUNCTIONS);
        VIRTUAL.put(fn + "Predicate.negate()" + predT, FUNCTIONS);
        STATIC.put(fn + "Predicate.not(" + predT + ")" + predT, FUNCTIONS);
        STATIC.put(fn + "Predicate.isEqual(" + OBJECT + ")" + predT, FUNCTIONS);

        String unaryT = "L" + fn + "UnaryOperator;";
        String biFnT = "L" + fn + "BiFunction;";
        String binaryT = "L" + fn + "BinaryOperator;";
        String consumerT = "L" + fn + "Consumer;";
        String biConsumerT = "L" + fn + "BiConsumer;";
        STATIC.put(fn + "Function.identity()" + fnT, FUNCTIONS);
        STATIC.put(fn + "UnaryOperator.identity()" + unaryT, FUNCTIONS + "#unaryIdentity");
        for (String op : new String[] {"andThen", "compose"}) {
            VIRTUAL.put(fn + "Function." + op + "(" + fnT + ")" + fnT, FUNCTIONS);
            // A UnaryOperator is a Function on the device as well.
            VIRTUAL.put(fn + "UnaryOperator." + op + "(" + fnT + ")" + fnT,
                    FUNCTIONS + "#" + op + "(" + fnT + fnT + ")" + fnT);
        }
        VIRTUAL.put(fn + "BiFunction.andThen(" + fnT + ")" + biFnT, FUNCTIONS);
        STATIC.put(fn + "BinaryOperator.minBy(" + cmpT + ")" + binaryT, FUNCTIONS);
        STATIC.put(fn + "BinaryOperator.maxBy(" + cmpT + ")" + binaryT, FUNCTIONS);
        VIRTUAL.put(fn + "Consumer.andThen(" + consumerT + ")" + consumerT, FUNCTIONS);
        VIRTUAL.put(fn + "BiConsumer.andThen(" + biConsumerT + ")" + biConsumerT, FUNCTIONS);
    }

    static {
        // Into a stream. The device's collections have no stream() at all.
        String util = "java/util/";
        String collectionT = "Ljava/util/Collection;";
        String intFunctionT = "Ljava/util/function/IntFunction;";
        for (String owner : COLLECTION_OWNERS) {
            VIRTUAL.put(util + owner + ".stream()" + STREAM, COLLECTIONS + "#stream(" + collectionT + ")" + STREAM);
            VIRTUAL.put(util + owner + ".parallelStream()" + STREAM,
                    COLLECTIONS + "#parallelStream(" + collectionT + ")" + STREAM);
            VIRTUAL.put(util + owner + ".toArray(" + intFunctionT + ")[" + OBJECT,
                    COLLECTIONS + "#toArray(" + collectionT + intFunctionT + ")[" + OBJECT);
        }
        String arrays = util + "Arrays.stream(";
        STATIC.put(arrays + "[" + OBJECT + ")" + STREAM, COLLECTIONS);
        STATIC.put(arrays + "[" + OBJECT + "II)" + STREAM, COLLECTIONS);
        for (String[] kind : new String[][] {{"I", "IntStream"}, {"J", "LongStream"}, {"D", "DoubleStream"}}) {
            STATIC.put(arrays + "[" + kind[0] + ")Ljava/util/stream/" + kind[1] + ";", COLLECTIONS);
            STATIC.put(arrays + "[" + kind[0] + "II)Ljava/util/stream/" + kind[1] + ";", COLLECTIONS);
        }

        String consumerT = "Ljava/util/function/Consumer;";
        String iteratorT = "Ljava/util/Iterator;";
        VIRTUAL.put(util + "Iterator.forEachRemaining(" + consumerT + ")V", COLLECTIONS);
        VIRTUAL.put(util + "ListIterator.forEachRemaining(" + consumerT + ")V",
                COLLECTIONS + "#forEachRemaining(" + iteratorT + consumerT + ")V");
        STATIC.put(util + "Collections.emptyIterator()" + iteratorT, COLLECTIONS);

        // The immutable factories. Three interfaces declare an "of" that
        // differs in nothing but what it returns, so each has its own name.
        String listT = "Ljava/util/List;";
        String setT = "Ljava/util/Set;";
        String mapT = "Ljava/util/Map;";
        String entryT = "Ljava/util/Map$Entry;";
        String cmpT = "Ljava/util/Comparator;";
        StringBuilder objects = new StringBuilder();
        for (int n = 0; n <= 10; n++) {
            STATIC.put(util + "List.of(" + objects + ")" + listT, COLLECTIONS + "#listOf");
            STATIC.put(util + "Set.of(" + objects + ")" + setT, COLLECTIONS + "#setOf");
            STATIC.put(util + "Map.of(" + objects + objects + ")" + mapT, COLLECTIONS + "#mapOf");
            objects.append(OBJECT);
        }
        STATIC.put(util + "List.of([" + OBJECT + ")" + listT, COLLECTIONS + "#listOf");
        STATIC.put(util + "Set.of([" + OBJECT + ")" + setT, COLLECTIONS + "#setOf");
        STATIC.put(util + "List.copyOf(" + collectionT + ")" + listT, COLLECTIONS + "#listCopyOf");
        STATIC.put(util + "Set.copyOf(" + collectionT + ")" + setT, COLLECTIONS + "#setCopyOf");
        STATIC.put(util + "Map.copyOf(" + mapT + ")" + mapT, COLLECTIONS + "#mapCopyOf");
        STATIC.put(util + "Map.ofEntries([" + entryT + ")" + mapT, COLLECTIONS + "#mapOfEntries");
        STATIC.put(util + "Map.entry(" + OBJECT + OBJECT + ")" + entryT, COLLECTIONS);
        for (String by : new String[] {"comparingByKey", "comparingByValue"}) {
            STATIC.put(util + "Map$Entry." + by + "()" + cmpT, COLLECTIONS);
            STATIC.put(util + "Map$Entry." + by + "(" + cmpT + ")" + cmpT, COLLECTIONS);
        }
    }

    static {
        String str = "java/lang/String.";
        VIRTUAL.put(str + "split(" + STRING + ")[" + STRING, REGEX);
        VIRTUAL.put(str + "split(" + STRING + "I)[" + STRING, REGEX);
        VIRTUAL.put(str + "replaceAll(" + STRING + STRING + ")" + STRING, REGEX);
        VIRTUAL.put(str + "replaceFirst(" + STRING + STRING + ")" + STRING, REGEX);
        VIRTUAL.put(str + "formatted([" + OBJECT + ")" + STRING, STRINGS);
        VIRTUAL.put(str + "isBlank()Z", STRINGS);
        VIRTUAL.put(str + "repeat(I)" + STRING, STRINGS);
        VIRTUAL.put(str + "lines()" + STREAM, STRINGS);
        for (String strip : new String[] {"strip", "stripLeading", "stripTrailing"}) {
            VIRTUAL.put(str + strip + "()" + STRING, STRINGS);
        }
        String ints = "Ljava/util/stream/IntStream;";
        for (String sequence : new String[] {"String", "CharSequence", "StringBuilder", "StringBuffer"}) {
            VIRTUAL.put("java/lang/" + sequence + ".chars()" + ints,
                    STRINGS + "#chars(Ljava/lang/CharSequence;)" + ints);
        }

        // java.time. The device's classes add and compare, and leave the
        // subtracting and the reading out of a parsed value to these.
        String time = "java/time/LocalTime.";
        String timeT = "Ljava/time/LocalTime;";
        for (String op : new String[] {"plusNanos", "minusNanos", "minusSeconds", "minusMinutes", "minusHours"}) {
            VIRTUAL.put(time + op + "(J)" + timeT, TIME);
        }
        VIRTUAL.put(time + "isBefore(" + timeT + ")Z", TIME);
        VIRTUAL.put(time + "isAfter(" + timeT + ")Z", TIME);
        String date = "java/time/LocalDate.";
        String dateT = "Ljava/time/LocalDate;";
        // The device has no java.time.chrono. The stand-ins take the other
        // date as the one interface the device's LocalDate implements; an
        // interface-typed argument is assignable from whatever the
        // application pushed, and the stand-in accepts a LocalDate alone.
        String chrono = "Ljava/time/chrono/ChronoLocalDate;";
        String accessorT = "Ljava/time/temporal/TemporalAccessor;";
        for (String test : new String[] {"isBefore", "isAfter", "isEqual"}) {
            VIRTUAL.put(date + test + "(" + chrono + ")Z", TIME + "#" + test + "(" + dateT + accessorT + ")Z");
        }
        VIRTUAL.put(date + "compareTo(" + chrono + ")I", TIME + "#compareTo(" + dateT + accessorT + ")I");
        for (String op : new String[] {"minusMonths", "minusYears", "plusWeeks", "minusWeeks"}) {
            VIRTUAL.put(date + op + "(J)" + dateT, TIME);
        }
        String accessor = "(Ljava/time/temporal/TemporalAccessor;)";
        STATIC.put(date + "from" + accessor + dateT, TIME + "#localDateFrom");
        STATIC.put(time + "from" + accessor + timeT, TIME + "#localTimeFrom");
        STATIC.put("java/time/LocalDateTime.from" + accessor + "Ljava/time/LocalDateTime;",
                TIME + "#localDateTimeFrom");
        VIRTUAL.put("java/time/format/DateTimeFormatter.parse(Ljava/lang/CharSequence;"
                + "Ljava/time/temporal/TemporalQuery;)" + OBJECT, TIME);
        // The device's formatter cannot carry the zone of withZone, so the
        // shared classes keep it beside the formatter: every way a
        // formatter is made from a pattern and every way a zone could show
        // goes through them.
        String fmt = "java/time/format/DateTimeFormatter.";
        String fmtT = "Ljava/time/format/DateTimeFormatter;";
        String zoneT = "Ljava/time/ZoneId;";
        String zonedT = "Ljava/time/ZonedDateTime;";
        STATIC.put(fmt + "ofPattern(" + STRING + ")" + fmtT, TIME);
        STATIC.put(fmt + "ofPattern(" + STRING + "Ljava/util/Locale;)" + fmtT, TIME);
        VIRTUAL.put(fmt + "withZone(" + zoneT + ")" + fmtT, TIME);
        VIRTUAL.put(fmt + "getZone()" + zoneT, TIME);
        VIRTUAL.put(fmt + "format(" + accessorT + ")" + STRING, TIME);
        VIRTUAL.put("java/time/ZonedDateTime.format(" + fmtT + ")" + STRING, TIME);
        VIRTUAL.put("java/time/OffsetDateTime.format(" + fmtT + ")" + STRING, TIME);
        STATIC.put("java/time/ZonedDateTime.parse(Ljava/lang/CharSequence;" + fmtT + ")" + zonedT,
                TIME + "#parseZoned");
    }

    /// Constructors the device lacks whose last argument only has to be
    /// converted for one it has: `owner.<init> descriptor` to the static
    /// method that converts the argument on top of the stack, and the
    /// descriptor of the constructor called instead.
    private static final Map<String, String[]> CONSTRUCTORS = new HashMap<String, String[]>();

    private static final String CHARSETS = Relocation.JDK_PACKAGE + "JdkCharsets";
    private static final String CHARSET = "Ljava/nio/charset/Charset;";

    static {
        // The device encodes and decodes by the NAME of an encoding. Its
        // Charset is that name, so an overload taking a Charset becomes the
        // one taking a name.
        String[] byName = {CHARSETS, "name", "(" + CHARSET + ")" + STRING};
        CONSTRUCTORS.put("java/lang/String.<init>([BII" + CHARSET + ")V",
                new String[] {byName[0], byName[1], byName[2], "([BII" + STRING + ")V"});
        CONSTRUCTORS.put("java/io/InputStreamReader.<init>(Ljava/io/InputStream;" + CHARSET + ")V",
                new String[] {byName[0], byName[1], byName[2], "(Ljava/io/InputStream;" + STRING + ")V"});
        CONSTRUCTORS.put("java/io/OutputStreamWriter.<init>(Ljava/io/OutputStream;" + CHARSET + ")V",
                new String[] {byName[0], byName[1], byName[2], "(Ljava/io/OutputStream;" + STRING + ")V"});
        String cs = "java/nio/charset/Charset.";
        STATIC.put(cs + "forName(" + STRING + ")" + CHARSET, CHARSETS);
        STATIC.put(cs + "defaultCharset()" + CHARSET, CHARSETS);
        STATIC.put(cs + "isSupported(" + STRING + ")Z", CHARSETS);
        VIRTUAL.put(cs + "name()" + STRING, CHARSETS);
        VIRTUAL.put(cs + "toString()" + STRING, CHARSETS);
        VIRTUAL.put("java/io/ByteArrayOutputStream.toString(" + CHARSET + ")" + STRING, CHARSETS);
    }

    private CompatRewrites() {
    }

    /// Whether `name` and `descriptor` are those of `Collection.stream()` or
    /// `parallelStream()`. The return type is matched in both spellings, so
    /// a class that reaches this already relocated is read like one that is
    /// not.
    private static boolean isStreamMethod(String name, String descriptor) {
        return ("stream".equals(name) || "parallelStream".equals(name))
                && (("()" + STREAM).equals(descriptor) || ("()" + STREAM_MOVED).equals(descriptor));
    }

    /// The stand-in for `stream()` or `parallelStream()` called on a type no
    /// row of [#VIRTUAL] names, or null when the call is not one of those.
    ///
    /// The rows cover the JDK's own collection types, by name. They cannot
    /// cover an application's own class, or a list type of another layer
    /// (`javafx.collections.ObservableList`), because the class file names
    /// the type the call was compiled against and nothing here knows what
    /// that type extends. So a call is recognised by what it is instead: no
    /// arguments, called `stream` or `parallelStream`, declared to return
    /// `java.util.stream.Stream`. The stand-in takes the receiver as an
    /// `Object`, which every verifier accepts for any type, and finds out
    /// when it runs what it was handed: a class that declares the method
    /// ([#declareStreamSources]) is called, any other collection gives its
    /// elements.
    ///
    /// A JDK type is only taken for a collection where it can be one --
    /// `java.util` and `java.util.concurrent` -- and the two there that have
    /// a `stream()` without being one are left out. A call on any other JDK
    /// class stays as it is, for the compliance check to report.
    private static String anyOwner(String owner, String name, String descriptor) {
        if (!isStreamMethod(name, descriptor)) {
            return null;
        }
        if (owner.startsWith(Relocation.JDK_PACKAGE) || owner.startsWith("[")) {
            return null;
        }
        if (owner.startsWith("java/") || owner.startsWith("javax/") || owner.startsWith("jdk/")) {
            String simple = owner.substring(owner.lastIndexOf('/') + 1);
            String pkg = owner.substring(0, owner.length() - simple.length());
            if (!"java/util/".equals(pkg) && !"java/util/concurrent/".equals(pkg)) {
                return null;
            }
            if ("java/util/Optional".equals(owner) || "java/util/ServiceLoader".equals(owner)) {
                return null;
            }
        }
        return COLLECTIONS + "#" + name + "Of(" + OBJECT + ")" + descriptor.substring(2);
    }

    /// The stand-in for `super.name(...)` -- or `List.super.name(...)` --
    /// where the method asked for is one of the JDK's defaults the device
    /// lacks, or null. The default's behaviour is what the static method
    /// implements, so the call goes straight to it; for `stream()` that has
    /// to be the one that never calls back into the receiver's own method.
    private static String superCall(String owner, String name, String descriptor) {
        String target = VIRTUAL.get(owner + "." + name + descriptor);
        if (target == null) {
            return null;
        }
        if (isStreamMethod(name, descriptor)) {
            return COLLECTIONS + "#defaultStream(Ljava/util/Collection;)" + STREAM;
        }
        String to = targetOwner(target);
        return COLLECTIONS.equals(to) || FUNCTIONS.equals(to) ? target : null;
    }

    /// Marks a class that declares `stream()` or `parallelStream()` as a
    /// `StreamSource` or `ParallelStreamSource`, so that the stand-ins can
    /// call the method the application wrote. See [#anyOwner].
    ///
    /// The method is made public, as an interface method has to be. It is a
    /// method javac let every caller in the application reach already, and
    /// each class that overrides it is widened by this same rule.
    private static void declareStreamSources(ClassNode cls) {
        for (MethodNode method : cls.methods) {
            if ((method.access & Opcodes.ACC_STATIC) != 0 || !isStreamMethod(method.name, method.desc)) {
                continue;
            }
            String marker = Relocation.JDK_PACKAGE
                    + ("stream".equals(method.name) ? "StreamSource" : "ParallelStreamSource");
            if (!cls.interfaces.contains(marker)) {
                cls.interfaces.add(marker);
                // The generic signature lists the interfaces too, and ends
                // with them: the new one goes there as a raw type.
                if (cls.signature != null) {
                    cls.signature = cls.signature + "L" + marker + ";";
                }
            }
            method.access = (method.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
        }
    }

    /// The rule for an instance method, or null.
    private static String virtual(String owner, String name, String descriptor) {
        String target = VIRTUAL.get(owner + "." + name + descriptor);
        return target != null ? target : anyOwner(owner, name, descriptor);
    }

    /// The class of a rule's target.
    private static String targetOwner(String target) {
        int hash = target.indexOf('#');
        return hash < 0 ? target : target.substring(0, hash);
    }

    /// The member a rule's target names, `name` unless it says otherwise.
    private static String targetName(String target, String name) {
        int hash = target.indexOf('#');
        if (hash < 0) {
            return name;
        }
        int paren = target.indexOf('(', hash);
        return paren < 0 ? target.substring(hash + 1) : target.substring(hash + 1, paren);
    }

    /// The descriptor of a rule's target, `descriptor` unless it gives one.
    private static String targetDescriptor(String target, String descriptor) {
        int paren = target.indexOf('(');
        return paren < 0 ? descriptor : target.substring(paren);
    }

    /// Whether the class named `internalName` is one whose calls are left as
    /// they are; see the class description.
    private static boolean exempt(String internalName) {
        return internalName.startsWith(Relocation.JDK_PACKAGE)
                || internalName.startsWith(AndroidRemapper.RELOCATION.target());
    }

    /// A visitor that applies the rules to the class it is shown and passes
    /// the result to `next`.
    static ClassVisitor visitor(final ClassVisitor next) {
        // The whole class is read before any of it is passed on: whether it
        // declares a stream() is known from its methods, and the interface
        // that says so belongs in the header, which comes first.
        return new ClassNode(Opcodes.ASM9) {
            @Override
            public void visitEnd() {
                super.visitEnd();
                if (exempt(name)) {
                    accept(next);
                    return;
                }
                declareStreamSources(this);
                accept(new ClassVisitor(Opcodes.ASM9, next) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                                     String[] exceptions) {
                        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                        return mv == null ? null : new Rewriter(mv);
                    }
                });
            }
        };
    }

    /// The descriptor of the static method standing in for the instance
    /// method `descriptor` of `owner`.
    private static String receiverFirst(String owner, String descriptor) {
        return "(L" + owner + ";" + descriptor.substring(1);
    }

    private static final class Rewriter extends MethodVisitor {
        /// How much deeper than the compiler counted the operand stack gets:
        /// one slot, once a one-argument `Locale` constructor was widened.
        private int extraStack;

        Rewriter(MethodVisitor next) {
            super(Opcodes.ASM9, next);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (opcode == Opcodes.INVOKESTATIC) {
                String target = STATIC.get(owner + "." + name + descriptor);
                if (target != null) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, targetOwner(target), targetName(target, name),
                            descriptor, false);
                    return;
                }
            } else if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) {
                String target = virtual(owner, name, descriptor);
                if (target != null) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, targetOwner(target), targetName(target, name),
                            targetDescriptor(target, receiverFirst(owner, descriptor)), false);
                    return;
                }
            } else if (opcode == Opcodes.INVOKESPECIAL && !"<init>".equals(name)) {
                String target = superCall(owner, name, descriptor);
                if (target != null) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, targetOwner(target), targetName(target, name),
                            targetDescriptor(target, receiverFirst(owner, descriptor)), false);
                    return;
                }
            } else if (opcode == Opcodes.INVOKESPECIAL && CONSTRUCTORS.containsKey(owner + "." + name + descriptor)) {
                // The argument to convert is the last one, so it is on top.
                String[] rule = CONSTRUCTORS.get(owner + "." + name + descriptor);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, rule[0], rule[1], rule[2], false);
                super.visitMethodInsn(opcode, owner, name, rule[3], false);
                return;
            } else if (opcode == Opcodes.INVOKESPECIAL && JAVA_LOCALE.equals(owner)) {
                // The device's Locale is a language and a country. A locale
                // of a language alone has the empty country; a variant has
                // nowhere to go and is dropped.
                if (("(" + STRING + ")V").equals(descriptor)) {
                    super.visitLdcInsn("");
                    extraStack = 1;
                    super.visitMethodInsn(opcode, owner, name, "(" + STRING + STRING + ")V", false);
                    return;
                }
                if (("(" + STRING + STRING + STRING + ")V").equals(descriptor)) {
                    super.visitInsn(Opcodes.POP);
                    super.visitMethodInsn(opcode, owner, name, "(" + STRING + STRING + ")V", false);
                    return;
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (opcode == Opcodes.GETSTATIC) {
                String target = FIELDS.get(owner + "." + name);
                if (target != null) {
                    super.visitFieldInsn(opcode, targetOwner(target), targetName(target, name), descriptor);
                    return;
                }
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap, Object... arguments) {
            Object[] rewritten = arguments;
            String site = descriptor;
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] instanceof Handle) {
                    Handle moved = rewrite((Handle) arguments[i]);
                    if (moved != arguments[i]) {
                        if (rewritten == arguments) {
                            rewritten = arguments.clone();
                        }
                        rewritten[i] = moved;
                        site = boundReceiver(site, (Handle) arguments[i], moved);
                    }
                }
            }
            super.visitInvokeDynamicInsn(name, site, bootstrap, rewritten);
        }

        /// The call site's descriptor for a method reference whose target
        /// moved from `from` to `to`.
        ///
        /// A reference bound to its receiver (`words::stream`) captures the
        /// receiver as the call site's one argument, typed as the method's
        /// owner, and the JVM's lambda factory requires a captured argument
        /// to have exactly the type the target takes it as. A stand-in that
        /// takes its receiver as something wider -- `stream(Collection)` for
        /// `List.stream()` -- therefore has the capture declared as that
        /// wider type, which is what the value on the stack is anyway.
        private static String boundReceiver(String site, Handle from, Handle to) {
            if (from.getTag() == Opcodes.H_INVOKESTATIC || site.startsWith("()")) {
                return site;
            }
            Type[] captured = Type.getArgumentTypes(site);
            Type[] taken = Type.getArgumentTypes(to.getDesc());
            if (captured.length != 1 || taken.length == 0 || captured[0].equals(taken[0])) {
                return site;
            }
            return Type.getMethodDescriptor(Type.getReturnType(site), taken[0]);
        }

        /// The handle a method reference should hold instead of `h`, or `h`.
        private static Handle rewrite(Handle h) {
            if (h.getTag() == Opcodes.H_INVOKESTATIC) {
                String target = STATIC.get(h.getOwner() + "." + h.getName() + h.getDesc());
                if (target != null) {
                    return new Handle(Opcodes.H_INVOKESTATIC, targetOwner(target), targetName(target, h.getName()),
                            h.getDesc(), false);
                }
            } else if (h.getTag() == Opcodes.H_INVOKEVIRTUAL || h.getTag() == Opcodes.H_INVOKEINTERFACE) {
                // Collection::stream, Comparator::reversed: the reference
                // takes its receiver as the first argument either way.
                String target = virtual(h.getOwner(), h.getName(), h.getDesc());
                if (target != null) {
                    return new Handle(Opcodes.H_INVOKESTATIC, targetOwner(target), targetName(target, h.getName()),
                            targetDescriptor(target, receiverFirst(h.getOwner(), h.getDesc())), false);
                }
            }
            return h;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack + extraStack, maxLocals);
        }
    }
}

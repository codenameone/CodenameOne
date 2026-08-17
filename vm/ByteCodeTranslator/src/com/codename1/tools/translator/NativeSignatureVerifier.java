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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Checks that every {@code native} method in a translated project has a C
 * implementation with the name AND the prototype ParparVM will call.
 *
 * <h2>Why this needs a gate of its own</h2>
 * <p>ParparVM encodes the whole Java signature in the C function NAME. A method
 * {@code boolean isBiometricsSupported()} on {@code com.codename1.impl.ios.IOSNative}
 * becomes</p>
 *
 * <pre>
 *   JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isBiometricsSupported___R_boolean(
 *           CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject)
 * </pre>
 *
 * <p>Two separator rules make this easy to get wrong by hand, and both have shipped
 * broken:</p>
 * <ul>
 *   <li>The argument list is introduced by {@code __} and every argument then adds
 *       its own leading {@code _}, so a no-argument method ends in {@code __} and a
 *       one-int method ends in {@code ___int} -- three underscores, not two.</li>
 *   <li>A non-void return appends {@code _R} plus the same per-type {@code _},
 *       i.e. {@code _R_boolean} / {@code _R_java_lang_String}. <b>Omitting it
 *       entirely still compiles</b>, and multi-dimensional arrays collapse to one
 *       token: {@code byte[][]} is {@code byte_2ARRAY}, never
 *       {@code byte_1ARRAY_1ARRAY}.</li>
 * </ul>
 *
 * <h2>Why the compiler and the linker do not catch it</h2>
 * <p>A misspelled name is simply a different function. Nothing references it, so it
 * compiles and links clean. Meanwhile the correctly named symbol is absent, and the
 * dead-code pass reads that absence as "nobody needs this method" -- native methods
 * are kept alive precisely BY being mentioned in the native sources (see
 * {@link BytecodeMethod#isMethodUsedByNative}). The method is dropped from the
 * build and the feature is silently inert on the device.</p>
 *
 * <p>C is worse than that for the prototype: the linker matches on the name alone.
 * An implementation with the right name and the wrong parameter list -- a missing
 * {@code JAVA_OBJECT me}, {@code JAVA_INT} where the Java side declares
 * {@code long} -- links successfully and then reads its arguments out of the wrong
 * registers at runtime. That is a crash or silent corruption on device with nothing
 * to point at.</p>
 *
 * <h2>What is reported</h2>
 * <dl>
 *   <dt>{@code MISSING}</dt><dd>no C function of that name is defined anywhere in the
 *   project's native sources.</dd>
 *   <dt>{@code SIGNATURE}</dt><dd>the name matches but the C prototype does not:
 *   wrong arity, or a parameter/return type of a different machine type.</dd>
 *   <dt>{@code ORPHAN}</dt><dd>a C function whose name is a near miss for one of this
 *   project's native methods -- same class and method, different suffix. Fatal when
 *   the correct symbol is absent (this is the typo that broke it); reported as a
 *   warning when the correct symbol is also present, because then the orphan is
 *   merely dead code.</dd>
 * </dl>
 *
 * <p>Type comparison is by machine type, not by spelling: {@code void} and
 * {@code JAVA_VOID} are the same type, as are {@code JAVA_OBJECT} and any other
 * pointer. Anything the table below does not know is treated as its own type and
 * therefore as a mismatch, so an unrecognised spelling is reported rather than
 * waved through.</p>
 *
 * <h2>Opt-in: this runs in OUR CI, not in customer builds</h2>
 * <p>A translation performs no check at all unless {@code -Dparparvm.nativeVerify}
 * or the {@code CN1_NATIVE_VERIFY} environment variable says {@code strict} or
 * {@code warn}; {@link Mode#OFF} is the default. That is deliberate. Turning the
 * old soft failure into a hard one changes the outcome of builds that succeed
 * today: an app carrying a vestigial {@code native} declaration nobody implements
 * builds now and would stop building, and it is not Codename One's place to break
 * a customer's release over a method their app never calls. The environment
 * variable exists so one setting on a CI job reaches every translation that job
 * forks -- the Maven plugin, the ParparVM integration tests, the build scripts --
 * without each of them plumbing a {@code -D} of its own.</p>
 *
 * <p>The offline entry point below has no such default: it is a CI tool, it is not
 * part of anyone's build, and it is always strict. It checks compiled classes
 * against native source directories, which is how CI verifies Codename One's own
 * ports without a device build:</p>
 *
 * <pre>
 *   java -cp ByteCodeTranslator.jar com.codename1.tools.translator.NativeSignatureVerifier \
 *       --classes DIR_OR_JAR [--classes ...] --natives DIR [--natives ...]
 * </pre>
 *
 * <h2>Escape hatch</h2>
 * <p>Even with the check on, a native implementation can legitimately live
 * somewhere this scan cannot see -- a prebuilt {@code .a} or {@code .framework}
 * shipped by a cn1lib. List those symbols in {@code cn1-native-verify-ignore.txt}
 * beside the native sources (one symbol or {@code prefix*} glob per line,
 * {@code #} comments allowed).</p>
 */
public class NativeSignatureVerifier {
    /** File name, looked up beside the native sources, listing symbols to skip. */
    public static final String IGNORE_FILE = "cn1-native-verify-ignore.txt";

    /** System property selecting the mode for a translation. */
    public static final String MODE_PROPERTY = "parparvm.nativeVerify";

    /** Environment variable read when {@link #MODE_PROPERTY} is not set. */
    public static final String MODE_ENVIRONMENT = "CN1_NATIVE_VERIFY";

    /**
     * What a translation does with the findings. {@link #OFF} is the default:
     * see the "opt-in" note on the class.
     */
    public enum Mode { STRICT, WARN, OFF }

    public enum Kind { MISSING, SIGNATURE, ORPHAN }

    /**
     * Thrown to abort a translation. A distinct type so the caller can report it as
     * the finished diagnosis it is, rather than as an unexpected failure part-way
     * through some class.
     */
    public static class VerificationFailedException extends IOException {
        private static final long serialVersionUID = 1L;

        public VerificationFailedException(String message) {
            super(message);
        }
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String[] SOURCE_EXTENSIONS = {
        ".m", ".mm", ".c", ".cc", ".cpp", ".cxx", ".h", ".hh", ".hpp", ".metal"
    };

    /**
     * Machine type per C spelling. Two types compare equal iff they land on the same
     * token here; an unknown spelling maps to itself, so it never silently passes.
     *
     * <p>The JAVA_* names are all typedefs in cn1_globals.h -- JAVA_BOOLEAN, JAVA_CHAR,
     * JAVA_BYTE, JAVA_SHORT and JAVA_INT are every one of them {@code int}, which is why
     * they share a class. Their JAVA_ARRAY_* counterparts are the narrow storage types
     * and are deliberately NOT listed: those are element types inside an array, never a
     * parameter type, and treating them as interchangeable with JAVA_INT would wave
     * through a real ABI mismatch.</p>
     */
    private static final Map<String, String> MACHINE_TYPES = new HashMap<String, String>();
    static {
        MACHINE_TYPES.put("void", "void");
        MACHINE_TYPES.put("JAVA_VOID", "void");
        for (String i32 : new String[]{"JAVA_BOOLEAN", "JAVA_CHAR", "JAVA_BYTE",
                "JAVA_SHORT", "JAVA_INT", "int", "signed int", "unsigned int"}) {
            MACHINE_TYPES.put(i32, "int32");
        }
        MACHINE_TYPES.put("JAVA_LONG", "int64");
        MACHINE_TYPES.put("long long", "int64");
        MACHINE_TYPES.put("JAVA_FLOAT", "float32");
        MACHINE_TYPES.put("float", "float32");
        MACHINE_TYPES.put("JAVA_DOUBLE", "float64");
        MACHINE_TYPES.put("double", "float64");
        MACHINE_TYPES.put("JAVA_OBJECT", "pointer");
        MACHINE_TYPES.put("JAVA_ARRAY", "pointer");
        MACHINE_TYPES.put("CODENAME_ONE_THREAD_STATE", "threadState");
    }

    /** The C prototype ParparVM will emit a call to for one native method. */
    public static final class Signature {
        public final String symbol;
        public final String className;
        public final String methodName;
        /** Everything up to and including {@code __}; shared by all overloads. */
        public final String overloadPrefix;
        public final String returnType;
        public final List<String> paramTypes;
        /** The full prototype, for printing in a diagnostic. */
        public final String prototype;

        Signature(String symbol, String className, String methodName, String overloadPrefix,
                  String returnType, List<String> paramTypes, String prototype) {
            this.symbol = symbol;
            this.className = className;
            this.methodName = methodName;
            this.overloadPrefix = overloadPrefix;
            this.returnType = returnType;
            this.paramTypes = Collections.unmodifiableList(paramTypes);
            this.prototype = prototype;
        }

        /** {@code com.codename1.impl.ios.IOSNative.isBiometricsSupported} */
        public String javaName() {
            return className.replace('_', '.') + "." + methodName;
        }
    }

    /** A C function definition found in the native sources. */
    public static final class Definition {
        public final String symbol;
        public final File file;
        public final int line;
        public final String returnType;
        public final List<String> paramTypes;
        /** Declared {@code static}: internal linkage, so no other TU can call it. */
        public final boolean internalLinkage;

        Definition(String symbol, File file, int line, String returnType,
                   List<String> paramTypes, boolean internalLinkage) {
            this.symbol = symbol;
            this.file = file;
            this.line = line;
            this.returnType = returnType;
            this.paramTypes = Collections.unmodifiableList(paramTypes);
            this.internalLinkage = internalLinkage;
        }

        public String where() {
            return file.getName() + ":" + line;
        }
    }

    public static final class Problem {
        public final Kind kind;
        public final boolean fatal;
        public final String symbol;
        public final String message;

        Problem(Kind kind, boolean fatal, String symbol, String message) {
            this.kind = kind;
            this.fatal = fatal;
            this.symbol = symbol;
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    // ---------------------------------------------------------------- mangling

    /**
     * The prototype for a native method, taken from the translator's own model so
     * the two can never drift: {@link BytecodeMethod#getNativeSignature} builds it
     * with the same calls that emit the call site.
     */
    public static Signature signatureOf(BytecodeMethod method) {
        return method.getNativeSignature();
    }

    /**
     * The prototype for a native method read straight out of a class file. Routes
     * through {@link BytecodeMethod} for exactly the reason above -- this path is
     * used by the offline CI gate, and a second mangler here would be a second
     * thing to keep correct.
     */
    public static Signature signatureOf(String internalClassName, int access, String name, String desc) {
        String clsName = internalClassName.replace('/', '_').replace('$', '_');
        return new BytecodeMethod(clsName, access, name, desc, null, null).getNativeSignature();
    }

    // ------------------------------------------------------------ source index

    /** Scan order, so a report never depends on filesystem order. */
    private static final Comparator<File> BY_PATH = new Comparator<File>() {
        @Override
        public int compare(File a, File b) {
            return a.getAbsolutePath().compareTo(b.getAbsolutePath());
        }
    };

    /** Fatal problems first, then by symbol. */
    private static final Comparator<Problem> BY_SEVERITY = new Comparator<Problem>() {
        @Override
        public int compare(Problem a, Problem b) {
            if (a.fatal != b.fatal) {
                return a.fatal ? -1 : 1;
            }
            return a.symbol.compareTo(b.symbol);
        }
    };

    /** Every C function definition in a set of native sources, keyed by name. */
    public static final class SourceIndex {
        private final Map<String, List<Definition>> definitions =
                new LinkedHashMap<String, List<Definition>>();
        /** Every appearance of each name, definitions included. */
        private final Map<String, int[]> occurrences = new HashMap<String, int[]>();
        private final Set<String> ignored = new LinkedHashSet<String>();

        public SourceIndex(Collection<File> files) throws IOException {
            this(files, Collections.<String, String>emptyMap());
        }

        /**
         * @param files on-disk native sources
         * @param additionalSources name -&gt; content for native sources the project
         *        will contain but that are not on disk yet. The translator writes
         *        some of its own runtime C after the point this check runs (on the
         *        clean target, {@code java_io_File.m} lands only once the header it
         *        is conditional on exists), and those implementations count.
         */
        public SourceIndex(Collection<File> files, Map<String, String> additionalSources)
                throws IOException {
            List<File> sorted = new ArrayList<File>(files);
            Collections.sort(sorted, BY_PATH);
            for (File file : sorted) {
                if (IGNORE_FILE.equals(file.getName())) {
                    readIgnoreFile(file, ignored);
                    continue;
                }
                if (!isNativeSource(file)) {
                    continue;
                }
                scan(file, readFile(file), definitions, occurrences);
            }
            for (Map.Entry<String, String> source : new java.util.TreeMap<String, String>(
                    additionalSources).entrySet()) {
                scan(new File(source.getKey()), source.getValue(), definitions, occurrences);
            }
        }

        public List<Definition> get(String symbol) {
            List<Definition> found = definitions.get(symbol);
            return found == null ? Collections.<Definition>emptyList() : found;
        }

        public Set<String> symbols() {
            return definitions.keySet();
        }

        /**
         * How many times the native sources mention this name other than to define
         * it. A wrapper that forwards to a differently-named helper is the normal
         * shape in the iOS port, and the helper is not dead code just because
         * ParparVM never calls it directly.
         */
        public int referenceCount(String symbol) {
            int[] total = occurrences.get(symbol);
            return (total == null ? 0 : total[0]) - get(symbol).size();
        }

        public boolean isIgnored(String symbol) {
            for (String pattern : ignored) {
                if (pattern.endsWith("*")
                        ? symbol.startsWith(pattern.substring(0, pattern.length() - 1))
                        : pattern.equals(symbol)) {
                    return true;
                }
            }
            return false;
        }

        public int size() {
            return definitions.size();
        }
    }

    /** Reads {@link #IGNORE_FILE}: one symbol or {@code prefix*} per line. */
    static void readIgnoreFile(File file, Set<String> into) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), UTF8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                int hash = line.indexOf('#');
                if (hash >= 0) {
                    line = line.substring(0, hash);
                }
                line = line.trim();
                if (line.length() > 0) {
                    into.add(line);
                }
            }
        } finally {
            reader.close();
        }
    }

    public static boolean isNativeSource(File file) {
        String name = file.getName();
        for (String extension : SOURCE_EXTENSIONS) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every file in {@code dir} the scan can read, plus the ignore file if present.
     * Not recursive: the generated project is flat, and recursing would drag in
     * unrelated vendored sources.
     */
    public static List<File> listNativeSources(File dir) {
        List<File> found = new ArrayList<File>();
        File[] children = dir.listFiles();
        if (children == null) {
            return found;
        }
        for (File child : children) {
            if (child.isFile() && (isNativeSource(child) || IGNORE_FILE.equals(child.getName()))) {
                found.add(child);
            }
        }
        return found;
    }

    /** As {@link #listNativeSources(File)} but walking the whole tree. */
    public static List<File> listNativeSourcesRecursive(File dir) {
        List<File> found = new ArrayList<File>();
        collectRecursive(dir, found);
        return found;
    }

    private static void collectRecursive(File dir, List<File> into) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) {
                collectRecursive(child, into);
            } else if (isNativeSource(child) || IGNORE_FILE.equals(child.getName())) {
                into.add(child);
            }
        }
    }

    private static void scan(File file, String raw, Map<String, List<Definition>> into,
                             Map<String, int[]> occurrences) {
        String code = blankNonCode(raw);
        int length = code.length();
        for (int iter = 0; iter < length; iter++) {
            if (!isIdentifierStart(code.charAt(iter))) {
                continue;
            }
            int nameStart = iter;
            int nameEnd = nameStart;
            while (nameEnd < length && isIdentifierPart(code.charAt(nameEnd))) {
                nameEnd++;
            }
            iter = nameEnd - 1;
            String name = code.substring(nameStart, nameEnd);
            // Every ParparVM symbol has the "__" that introduces its argument list,
            // so this cheap test drops the overwhelming majority of identifiers
            // (locals, ObjC selectors, macros) before any of the work below.
            if (name.indexOf("__") < 0) {
                continue;
            }
            int[] seen = occurrences.get(name);
            if (seen == null) {
                occurrences.put(name, new int[]{1});
            } else {
                seen[0]++;
            }
            int open = skipSpace(code, nameEnd);
            if (open >= length || code.charAt(open) != '(') {
                continue;
            }
            int close = matchParen(code, open);
            if (close < 0) {
                continue;
            }
            int brace = skipAttributes(code, close + 1);
            if (brace >= length || code.charAt(brace) != '{') {
                continue; // a declaration, or a call -- not a definition
            }
            Declaration declared = returnTypeBefore(code, nameStart);
            if (declared == null) {
                continue;
            }
            List<String> params = parseParameters(code.substring(open + 1, close));
            Definition definition = new Definition(name, file, lineOf(raw, nameStart),
                    declared.returnType, params, declared.internalLinkage);
            List<Definition> list = into.get(name);
            if (list == null) {
                list = new ArrayList<Definition>();
                into.put(name, list);
            }
            list.add(definition);
            iter = brace;
        }
    }

    /**
     * Replaces comments, string/char literals and preprocessor directives with
     * spaces, leaving every other character at its original offset so reported line
     * numbers stay true.
     *
     * <p>One pass, because the orders interact: {@code "http://x"} is a string, not
     * a comment, and {@code /* } inside a string does not open one. Doing it with
     * three independent regular expressions is what made an earlier version of this
     * scan swallow half of IOSNative.m and report its contents as missing.</p>
     */
    static String blankNonCode(String source) {
        char[] out = source.toCharArray();
        int length = out.length;
        boolean atLineStart = true;
        int iter = 0;
        while (iter < length) {
            char c = source.charAt(iter);
            if (c == '/' && iter + 1 < length && source.charAt(iter + 1) == '/') {
                while (iter < length && source.charAt(iter) != '\n') {
                    out[iter++] = ' ';
                }
            } else if (c == '/' && iter + 1 < length && source.charAt(iter + 1) == '*') {
                out[iter++] = ' ';
                out[iter++] = ' ';
                while (iter < length
                        && !(source.charAt(iter) == '*' && iter + 1 < length
                             && source.charAt(iter + 1) == '/')) {
                    if (source.charAt(iter) != '\n') {
                        out[iter] = ' ';
                    }
                    iter++;
                }
                if (iter < length) {
                    out[iter++] = ' ';
                    if (iter < length) {
                        out[iter++] = ' ';
                    }
                }
            } else if (c == '"' || c == '\'') {
                out[iter++] = ' ';
                while (iter < length && source.charAt(iter) != c) {
                    if (source.charAt(iter) == '\\') {
                        out[iter++] = ' ';
                        if (iter < length) {
                            if (source.charAt(iter) != '\n') {
                                out[iter] = ' ';
                            }
                            iter++;
                        }
                        continue;
                    }
                    if (source.charAt(iter) == '\n') {
                        break; // unterminated literal; do not eat the rest of the file
                    }
                    out[iter++] = ' ';
                }
                if (iter < length && source.charAt(iter) == c) {
                    out[iter++] = ' ';
                }
            } else if (c == '#' && atLineStart) {
                // A directive can look exactly like a definition:
                // "#define CN1_WRAP__(x) { ... }". Blank the whole logical line.
                while (iter < length) {
                    char d = source.charAt(iter);
                    if (d == '\n') {
                        break;
                    }
                    if (d == '\\') {
                        out[iter++] = ' ';
                        while (iter < length && source.charAt(iter) != '\n') {
                            out[iter++] = ' ';
                        }
                        if (iter < length) {
                            iter++; // keep the newline, continue onto the next line
                        }
                        continue;
                    }
                    out[iter++] = ' ';
                }
            } else {
                if (!Character.isWhitespace(c)) {
                    atLineStart = false;
                } else if (c == '\n') {
                    atLineStart = true;
                }
                iter++;
                continue;
            }
            // A blanked run always ends at or past a newline boundary or mid-line;
            // recompute rather than guess.
            atLineStart = iter > 0 && iter <= length && source.charAt(iter - 1) == '\n';
        }
        return new String(out);
    }

    private static int skipSpace(String code, int from) {
        int iter = from;
        while (iter < code.length() && Character.isWhitespace(code.charAt(iter))) {
            iter++;
        }
        return iter;
    }

    /** Index of the ')' closing the '(' at {@code open}, or -1. */
    private static int matchParen(String code, int open) {
        int depth = 0;
        for (int iter = open; iter < code.length(); iter++) {
            char c = code.charAt(iter);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return iter;
                }
            }
        }
        return -1;
    }

    /**
     * Skips whitespace and any {@code __attribute__((...))} / {@code __asm(...)}
     * between the parameter list and the body.
     */
    private static int skipAttributes(String code, int from) {
        int iter = skipSpace(code, from);
        while (iter < code.length() && isIdentifierStart(code.charAt(iter))) {
            int end = iter;
            while (end < code.length() && isIdentifierPart(code.charAt(end))) {
                end++;
            }
            String token = code.substring(iter, end);
            if (!token.startsWith("__") && !"CN1_UNUSED".equals(token)) {
                return iter;
            }
            int open = skipSpace(code, end);
            if (open < code.length() && code.charAt(open) == '(') {
                int close = matchParen(code, open);
                if (close < 0) {
                    return code.length();
                }
                iter = skipSpace(code, close + 1);
            } else {
                iter = open;
            }
        }
        return iter;
    }

    /**
     * The full return type before a function name, or null when what precedes it
     * cannot be one (so the match is a call, not a definition).
     *
     * <p>Reads the WHOLE type specifier, not just the token nearest the name: a
     * native returning Java {@code long} may spell its C type {@code long long},
     * and stopping at {@code long} would map to an unknown type and reject an
     * ABI-correct implementation. Storage classes and cv-qualifiers are dropped --
     * they do not change the machine type -- while {@code long}/{@code short}/
     * {@code unsigned}/{@code signed} are kept, because they are the type.</p>
     */
    private static Declaration returnTypeBefore(String code, int nameStart) {
        int cursor = nameStart - 1;
        boolean pointer = false;
        boolean internalLinkage = false;
        List<String> tokens = new ArrayList<String>();
        while (cursor >= 0) {
            char c = code.charAt(cursor);
            if (Character.isWhitespace(c)) {
                cursor--;
                continue;
            }
            if (c == '*') {
                pointer = true;
                cursor--;
                continue;
            }
            if (!isIdentifierPart(c)) {
                // ';' '}' '{' ')' ',' '@' -- the declaration starts here
                break;
            }
            int end = cursor;
            while (cursor >= 0 && isIdentifierPart(code.charAt(cursor))) {
                cursor--;
            }
            String token = code.substring(cursor + 1, end + 1);
            if (RESERVED_BEFORE_CALL.contains(token)) {
                return null;
            }
            if (DECLARATION_QUALIFIERS.contains(token)) {
                internalLinkage |= "static".equals(token);
                continue;
            }
            // Keep reading left only while the tokens can still be part of ONE type.
            // Nothing separates a return type from whatever precedes the declaration
            // -- an @end, a macro, the tail of a #define -- so the type keywords are
            // the only reliable signal that a second word belongs to it.
            if (!tokens.isEmpty() && !MULTIWORD_TYPE_PARTS.contains(token)) {
                break;
            }
            tokens.add(0, token);
        }
        if (tokens.isEmpty()) {
            return null;
        }
        StringBuilder type = new StringBuilder();
        for (String token : tokens) {
            if (type.length() > 0) {
                type.append(' ');
            }
            type.append(token);
        }
        if (pointer) {
            type.append('*');
        }
        return new Declaration(type.toString(), internalLinkage);
    }

    /** What precedes a function name: its return type, and whether it is static. */
    private static final class Declaration {
        final String returnType;
        final boolean internalLinkage;

        Declaration(String returnType, boolean internalLinkage) {
            this.returnType = returnType;
            this.internalLinkage = internalLinkage;
        }
    }

    /** Keywords that can precede a '(' without the match being a definition. */
    private static final Set<String> RESERVED_BEFORE_CALL = new HashSet<String>(Arrays.asList(
            "return", "if", "else", "while", "for", "switch", "case", "do", "sizeof",
            "typedef", "goto", "break", "continue", "new", "delete", "throw"));

    /** Precedes a return type without being part of it. */
    private static final Set<String> DECLARATION_QUALIFIERS = new HashSet<String>(Arrays.asList(
            "static", "extern", "inline", "__inline", "__inline__", "register", "auto",
            "const", "volatile", "restrict", "__restrict", "_Noreturn", "CN1_UNUSED"));

    /** Words that can extend a type leftwards: {@code long long}, {@code unsigned int}. */
    private static final Set<String> MULTIWORD_TYPE_PARTS = new HashSet<String>(Arrays.asList(
            "long", "short", "unsigned", "signed", "struct", "enum", "union"));

    /**
     * The parameter types of a C parameter list, with the ParparVM thread-state
     * macros expanded. A spelling this cannot reduce to a known type is kept
     * verbatim and compares unequal to everything, so an unrecognised parameter
     * is reported rather than waved through.
     */
    private static List<String> parseParameters(String text) {
        String expanded = text
                .replace("CN1_THREAD_STATE_MULTI_ARG", "CODENAME_ONE_THREAD_STATE,")
                .replace("CN1_THREAD_STATE_SINGLE_ARG", "CODENAME_ONE_THREAD_STATE");
        List<String> types = new ArrayList<String>();
        for (String part : splitTopLevel(expanded)) {
            String trimmed = part.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            if ("void".equals(trimmed)) {
                continue; // f(void) declares no parameters
            }
            types.add(normalizeParameter(trimmed));
        }
        return types;
    }

    private static List<String> splitTopLevel(String text) {
        List<String> parts = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c == '(' || c == '[' || c == '<') {
                depth++;
            } else if (c == ')' || c == ']' || c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(text.substring(start, iter));
                start = iter + 1;
            }
        }
        parts.add(text.substring(start));
        return parts;
    }

    /** Drops the parameter name and normalizes spacing: "JAVA_OBJECT me" -&gt; "JAVA_OBJECT". */
    private static String normalizeParameter(String declaration) {
        String text = declaration.replace("*", " * ").trim();
        List<String> tokens = new ArrayList<String>(
                Arrays.asList(text.split("\\s+")));
        // "CODENAME_ONE_THREAD_STATE" is a macro that expands to a full declaration
        // and carries no separate name to strip.
        if (tokens.size() > 1 && !"CODENAME_ONE_THREAD_STATE".equals(tokens.get(0))) {
            String last = tokens.get(tokens.size() - 1);
            if (isIdentifier(last) && !MACHINE_TYPES.containsKey(last)) {
                tokens.remove(tokens.size() - 1);
            }
        }
        StringBuilder joined = new StringBuilder();
        for (String token : tokens) {
            if (DECLARATION_QUALIFIERS.contains(token)) {
                continue;
            }
            if (joined.length() > 0 && !"*".equals(token)) {
                joined.append(' ');
            }
            joined.append(token);
        }
        return joined.toString();
    }

    // ------------------------------------------------------------- comparison

    /** The machine type of a C type spelling; unknown spellings map to themselves. */
    static String machineType(String cType) {
        String type = cType.trim();
        // The macro spelled out by hand: "struct ThreadLocalData* threadStateData".
        // Checked before the pointer rule, which would otherwise swallow it.
        if (type.startsWith("struct ThreadLocalData")) {
            return "threadState";
        }
        if (type.endsWith("*")) {
            return "pointer";
        }
        String known = MACHINE_TYPES.get(type);
        return known == null ? type : known;
    }

    private static boolean sameType(String expected, String actual) {
        return machineType(expected).equals(machineType(actual));
    }

    /**
     * Compares one definition against the prototype, returning null when they agree
     * or a human-readable reason when they do not.
     */
    static String mismatch(Signature expected, Definition actual) {
        if (actual.internalLinkage) {
            // The call ParparVM emits lives in the generated .c for the declaring
            // class, never in the hand-written .m, so a static definition is not a
            // definition as far as that reference is concerned -- the name resolves
            // to nothing and the link fails. Reporting it here beats an "undefined
            // symbol" at the end of an Xcode build that names no Java method.
            return "is declared static: internal linkage cannot satisfy the call"
                    + " ParparVM emits from another translation unit";
        }
        if (!sameType(expected.returnType, actual.returnType)) {
            return "returns " + actual.returnType + " where " + expected.returnType + " is required";
        }
        if (expected.paramTypes.size() != actual.paramTypes.size()) {
            return "takes " + actual.paramTypes.size() + " parameter(s) where "
                    + expected.paramTypes.size() + " are required ("
                    + join(actual.paramTypes) + " vs " + join(expected.paramTypes) + ")";
        }
        for (int iter = 0; iter < expected.paramTypes.size(); iter++) {
            String want = expected.paramTypes.get(iter);
            String got = actual.paramTypes.get(iter);
            if (!sameType(want, got)) {
                return "parameter " + (iter + 1) + " is " + got + " where " + want
                        + " is required";
            }
        }
        return null;
    }

    private static String join(List<String> parts) {
        StringBuilder b = new StringBuilder();
        for (String part : parts) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(part);
        }
        return b.toString();
    }

    // ------------------------------------------------------------------ verify

    /**
     * Checks every required prototype against the index.
     *
     * @param required   one entry per native method in the translated project
     * @param index      the project's native sources
     * @return the problems found, fatal ones first
     */
    public static List<Problem> verify(Collection<Signature> required, SourceIndex index) {
        List<Problem> problems = new ArrayList<Problem>();

        // overload prefix -> the symbols that are legitimately spelled that way, so a
        // near-miss can name the symbol its author meant to write
        Map<String, List<Signature>> byPrefix = new LinkedHashMap<String, List<Signature>>();
        Set<String> requiredSymbols = new HashSet<String>();
        for (Signature signature : required) {
            requiredSymbols.add(signature.symbol);
            List<Signature> list = byPrefix.get(signature.overloadPrefix);
            if (list == null) {
                list = new ArrayList<Signature>();
                byPrefix.put(signature.overloadPrefix, list);
            }
            list.add(signature);
        }
        // Definitions that no native method asks for, grouped by the overload they
        // are nearest to. Computed once so a missing symbol can point straight at
        // the misspelling instead of being reported twice from opposite ends.
        Map<String, List<Definition>> nearMisses = new HashMap<String, List<Definition>>();
        for (String symbol : index.symbols()) {
            if (requiredSymbols.contains(symbol)) {
                continue;
            }
            String prefix = longestPrefix(byPrefix.keySet(), symbol);
            if (prefix == null) {
                continue;
            }
            List<Definition> list = nearMisses.get(prefix);
            if (list == null) {
                list = new ArrayList<Definition>();
                nearMisses.put(prefix, list);
            }
            list.addAll(index.get(symbol));
        }

        for (Signature signature : required) {
            if (index.isIgnored(signature.symbol)) {
                continue;
            }
            List<Definition> found = index.get(signature.symbol);
            if (found.isEmpty()) {
                problems.add(new Problem(Kind.MISSING, true, signature.symbol,
                        signature.javaName() + " is declared native but nothing implements it.\n"
                        + "    Required: " + signature.prototype + "\n"
                        + describeNearMisses(nearMisses.get(signature.overloadPrefix))
                        + "    Add that exact prototype to a native source, or list the symbol in "
                        + IGNORE_FILE + "\n"
                        + "    if a prebuilt library provides it."));
                continue;
            }
            // Several definitions of one symbol are normal -- #if TARGET_OS_WATCH and
            // its #else branch both define it -- and EVERY one has to match. They are
            // the same C symbol under mutually exclusive branches, so their prototypes
            // cannot legitimately differ; accepting the symbol as soon as one branch
            // agreed would pass a build whose selected branch is the ABI-incompatible
            // one, which links by name and then reads its arguments wrong.
            String reason = null;
            Definition offender = null;
            for (Definition definition : found) {
                reason = mismatch(signature, definition);
                if (reason != null) {
                    offender = definition;
                    break;
                }
            }
            if (reason != null) {
                problems.add(new Problem(Kind.SIGNATURE, true, signature.symbol,
                        offender.where() + ": " + signature.symbol + " " + reason + "\n"
                        + "    C links on the name alone, so this compiles and then reads its"
                        + " arguments out of the wrong registers at runtime.\n"
                        + "    Required: " + signature.prototype));
            }
        }

        // A near miss whose correct symbol IS present is not the bug above -- but it
        // is a function ParparVM will never call, so it is worth naming as long as
        // nothing else in the native sources calls it either. The iOS port is full
        // of legitimate cases: a thin `..._R_boolean` wrapper forwarding to the
        // pre-suffix implementation, which is a helper rather than dead weight.
        for (Map.Entry<String, List<Definition>> entry : nearMisses.entrySet()) {
            List<Signature> siblings = byPrefix.get(entry.getKey());
            boolean allImplemented = true;
            for (Signature signature : siblings) {
                allImplemented &= !index.get(signature.symbol).isEmpty();
            }
            if (!allImplemented) {
                continue; // already reported against the missing symbol
            }
            for (Definition definition : entry.getValue()) {
                if (index.isIgnored(definition.symbol)
                        || index.referenceCount(definition.symbol) > 0) {
                    continue;
                }
                problems.add(new Problem(Kind.ORPHAN, false, definition.symbol,
                        definition.where() + ": " + definition.symbol
                        + " is never called -- neither by ParparVM nor by other native code.\n"
                        + "    " + siblings.get(0).javaName() + " resolves to "
                        + describeSymbols(siblings) + ", which is defined elsewhere."));
            }
        }

        Collections.sort(problems, BY_SEVERITY);
        return problems;
    }

    private static String longestPrefix(Collection<String> prefixes, String symbol) {
        String best = null;
        for (String prefix : prefixes) {
            if (symbol.startsWith(prefix)
                    && (best == null || prefix.length() > best.length())) {
                best = prefix;
            }
        }
        return best;
    }

    private static String describeNearMisses(List<Definition> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (Definition candidate : candidates) {
            b.append("    Found instead at ").append(candidate.where()).append(": ")
             .append(candidate.symbol).append('\n');
        }
        b.append("      -- that name differs only in its suffix, so nothing links against it and"
                + " the\n");
        b.append("         dead-code pass then drops the Java method entirely (native methods are"
                + " kept\n");
        b.append("         alive BY being named in the native sources). The feature goes silently"
                + " inert.\n");
        return b.toString();
    }

    private static String describeSymbols(List<Signature> signatures) {
        StringBuilder b = new StringBuilder();
        for (Signature signature : signatures) {
            if (b.length() > 0) {
                b.append(" / ");
            }
            b.append(signature.symbol);
        }
        return b.toString();
    }

    /**
     * The translator's own bundled runtime C, read off the classpath.
     *
     * <p>These implement the {@code java.*} natives and are copied into every
     * generated project -- but not all of them before this check runs. On the clean
     * target {@code java_io_File.m} is copied only AFTER {@code Parser.writeOutput},
     * because it is conditional on a header that pass generates, so reading it from
     * disk would report all 23 {@code java.io.File} natives as unimplemented.
     * Reading the resource instead sees what the project is about to contain.</p>
     *
     * <p>Indexing one that IS already on disk is harmless: a symbol may be defined
     * more than once (the ports do it with {@code #if}/{@code #else}), and both the
     * definition count and the occurrence count move together, so the
     * "is anything calling this" arithmetic is unaffected.</p>
     */
    public static Map<String, String> bundledRuntimeSources() {
        Map<String, String> sources = new LinkedHashMap<String, String>();
        for (String name : new String[]{"nativeMethods.m", "cn1_globals.m", "java_io_File.m"}) {
            InputStream in = NativeSignatureVerifier.class.getResourceAsStream("/" + name);
            if (in == null) {
                continue;
            }
            try {
                try {
                    sources.put(name, new String(readAll(in), UTF8));
                } finally {
                    in.close();
                }
            } catch (IOException ignored) {
                // A runtime source we cannot read just is not indexed; the worst
                // outcome is a MISSING report the developer can see through, which
                // beats aborting a translation over it.
            }
        }
        return sources;
    }

    /** The mode this translation runs in; see the opt-in note on the class. */
    public static Mode mode() {
        String value = System.getProperty(MODE_PROPERTY);
        if (value == null) {
            // Environment, not just the property, so one setting on a CI job reaches
            // every translation it forks -- the Maven plugin, the integration tests
            // and the build scripts alike -- without each having to plumb a -D.
            value = System.getenv(MODE_ENVIRONMENT);
        }
        return modeOf(value);
    }

    /** {@link #mode()} without the lookup, so it can be tested. */
    static Mode modeOf(String value) {
        if (value == null || value.trim().length() == 0) {
            return Mode.OFF;
        }
        String normalized = value.trim();
        if ("strict".equalsIgnoreCase(normalized) || "true".equalsIgnoreCase(normalized)) {
            return Mode.STRICT;
        }
        if ("warn".equalsIgnoreCase(normalized)) {
            return Mode.WARN;
        }
        return Mode.OFF;
    }

    /**
     * Prints the problems and returns the number that should fail the build.
     *
     * @param detailWarnings print non-fatal findings in full. The offline gate
     *        does; an app build does not, because dead code in a port the app
     *        author did not write is not theirs to act on and would bury the
     *        findings that are.
     */
    public static int report(List<Problem> problems, Mode mode, String context,
                             boolean detailWarnings) {
        int fatal = 0;
        List<String> quiet = new ArrayList<String>();
        for (Problem problem : problems) {
            boolean fails = problem.fatal && mode == Mode.STRICT;
            if (fails) {
                fatal++;
            }
            // Only genuinely non-fatal findings collapse. A fatal one demoted by
            // warn mode still prints in full: it is a missing implementation, and
            // filing it under "nothing calls this" would describe it as the
            // opposite of what it is.
            if (!problem.fatal && !detailWarnings) {
                quiet.add(problem.symbol);
                continue;
            }
            System.err.println("  " + (fails ? "ERROR" : "WARNING") + ": " + problem.message);
        }
        if (!quiet.isEmpty()) {
            System.err.println("  WARNING: " + quiet.size() + " C function(s) nothing calls: "
                    + join(quiet));
        }
        if (!problems.isEmpty()) {
            System.err.println();
            System.err.println("Native signature check (" + context + "): "
                    + problems.size() + " problem(s), " + fatal + " fatal.");
            System.err.println("ParparVM encodes the Java signature in the C function name; see"
                    + " NativeSignatureVerifier for the rules.");
        }
        return fatal;
    }

    // ---------------------------------------------------------------- offline

    /** Collects the native methods declared by every class under a directory or jar. */
    public static List<Signature> collectFromClasses(File root) throws IOException {
        List<Signature> found = new ArrayList<Signature>();
        if (root.isDirectory()) {
            collectClassesFromDirectory(root, found);
        } else if (root.getName().endsWith(".jar") || root.getName().endsWith(".zip")) {
            collectClassesFromArchive(root, found);
        } else if (root.getName().endsWith(".class")) {
            collectFromClassBytes(readAll(root), found);
        }
        return found;
    }

    private static void collectClassesFromDirectory(File dir, List<Signature> into) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children);
        for (File child : children) {
            if (child.isDirectory()) {
                collectClassesFromDirectory(child, into);
            } else if (child.getName().endsWith(".class")
                    && !"module-info.class".equals(child.getName())) {
                collectFromClassBytes(readAll(child), into);
            }
        }
    }

    private static void collectClassesFromArchive(File archive, List<Signature> into) throws IOException {
        ZipFile zip = new ZipFile(archive);
        try {
            List<String> names = new ArrayList<String>();
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
                ZipEntry entry = e.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")
                        && !entry.getName().endsWith("module-info.class")) {
                    names.add(entry.getName());
                }
            }
            Collections.sort(names);
            for (String name : names) {
                InputStream in = zip.getInputStream(zip.getEntry(name));
                try {
                    collectFromClassBytes(readAll(in), into);
                } finally {
                    in.close();
                }
            }
        } finally {
            zip.close();
        }
    }

    private static void collectFromClassBytes(byte[] bytes, final List<Signature> into) {
        final String[] owner = new String[1];
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                owner[0] = name;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                if ((access & Opcodes.ACC_NATIVE) != 0) {
                    into.add(signatureOf(owner[0], access, name, desc));
                }
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static String readFile(File file) throws IOException {
        return new String(readAll(file), UTF8);
    }

    private static byte[] readAll(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            return readAll(in);
        } finally {
            in.close();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static int lineOf(String text, int offset) {
        int line = 1;
        for (int iter = 0; iter < offset && iter < text.length(); iter++) {
            if (text.charAt(iter) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static boolean isIdentifierStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || (c >= '0' && c <= '9');
    }

    private static boolean isIdentifier(String s) {
        if (s.length() == 0 || !isIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int iter = 1; iter < s.length(); iter++) {
            if (!isIdentifierPart(s.charAt(iter))) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        List<File> classRoots = new ArrayList<File>();
        List<File> nativeRoots = new ArrayList<File>();
        boolean orphans = true;
        for (int iter = 0; iter < args.length; iter++) {
            if ("--classes".equals(args[iter]) && iter + 1 < args.length) {
                classRoots.add(new File(args[++iter]));
            } else if ("--natives".equals(args[iter]) && iter + 1 < args.length) {
                nativeRoots.add(new File(args[++iter]));
            } else if ("--no-orphans".equals(args[iter])) {
                orphans = false;
            } else {
                System.err.println("unrecognised argument: " + args[iter]);
                usage();
                System.exit(2);
            }
        }
        if (classRoots.isEmpty() || nativeRoots.isEmpty()) {
            usage();
            System.exit(2);
        }

        List<Signature> required = new ArrayList<Signature>();
        for (File root : classRoots) {
            if (!root.exists()) {
                System.err.println("NativeSignatureVerifier: no such path: " + root);
                System.exit(2);
            }
            required.addAll(collectFromClasses(root));
        }
        List<File> sources = new ArrayList<File>();
        for (File root : nativeRoots) {
            if (!root.exists()) {
                System.err.println("NativeSignatureVerifier: no such path: " + root);
                System.exit(2);
            }
            sources.addAll(root.isDirectory()
                    ? listNativeSourcesRecursive(root) : Collections.singletonList(root));
        }

        SourceIndex index = new SourceIndex(sources);
        List<Problem> problems = verify(required, index);
        if (!orphans) {
            List<Problem> filtered = new ArrayList<Problem>();
            for (Problem problem : problems) {
                if (problem.kind != Kind.ORPHAN) {
                    filtered.add(problem);
                }
            }
            problems = filtered;
        }

        if (problems.isEmpty()) {
            System.out.println("NativeSignatureVerifier: " + required.size()
                    + " native method(s) all resolve against " + index.size()
                    + " C definition(s) in " + sources.size() + " file(s).");
            return;
        }
        int fatal = report(problems, Mode.STRICT,
                required.size() + " native methods, " + sources.size() + " native sources", true);
        System.exit(fatal > 0 ? 1 : 0);
    }

    private static void usage() {
        System.err.println("usage: NativeSignatureVerifier --classes DIR_OR_JAR [--classes ...]"
                + " --natives DIR [--natives ...] [--no-orphans]");
    }

    private NativeSignatureVerifier() {
    }
}

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
package com.codename1.maven.processors;

import com.codename1.build.shared.BuildHintAnnotationBinding;
import com.codename1.build.shared.BuildHints;
import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/// Turns the `com.codename1.annotations.buildhints` annotations into the
/// `codename1.arg.*` key/value pairs the builders already consume.
///
/// A build hint used to be a properties line that nothing checked, so a
/// misspelled name reached the build request, was never read, and was silently
/// dropped -- a green build with the setting simply not applied. Written as an
/// annotation the compiler catches the same mistake, and this processor is what
/// turns the checked form back into the wire form.
///
/// The result is written to `META-INF/codenameone/build-hints.properties` in
/// `target/classes`, which puts it both on the simulator's classpath and inside
/// the jar uploaded to the build server.
public class BuildHintAnnotationProcessor extends AbstractAnnotationProcessor {

    /// Where the emitted hints land. Read by `CN1BuildMojo` before it writes the
    /// build request, and by `Simulator` on startup.
    public static final String MANIFEST_RESOURCE = "META-INF/codenameone/build-hints.properties";

    /// Records which annotation attribute supplied each hint, so a later stage
    /// -- the conflict message, the simulator's hint editor -- can name it.
    private static final String ORIGIN_PREFIX = "cn1.buildHints.origin.";

    /// Other names for the same setting, for a consumer that cannot reach the
    /// catalog to resolve an alias itself.
    private static final String ALIAS_PREFIX = "cn1.buildHints.alias.";

    /// Stamps the emitted file with the main class it came from, so a stale or
    /// foreign copy on the classpath can be recognised rather than merged.
    private static final String MAIN_CLASS_KEY = "cn1.buildHints.mainClass";

    /// Digest of the annotations this file was generated from.
    ///
    /// The main-class stamp only says *which* class produced it, which is the
    /// same class an out-of-date copy names. Nothing removes `target/classes`
    /// between builds, so a project that ran this processor once and then stopped
    /// -- the goal unbound, skipped, or bound to a phase that no longer runs --
    /// keeps a manifest that looks entirely valid while the annotations beside it
    /// have moved on. Recording what it was built from lets the consumer compare
    /// it against the class file actually on the classpath and refuse instead of
    /// shipping last week's configuration.
    public static final String SOURCE_DIGEST_KEY = "cn1.buildHints.sourceDigest";

    /// hint name to value, sorted so the emitted bytes are stable.
    private final Map<String, String> hints = new TreeMap<String, String>();
    /// hint name to "@Ios(pods)".
    private final Map<String, String> origins = new TreeMap<String, String>();
    /// Classes carrying a build hint annotation, in discovery order.
    private final List<AnnotatedClass> annotated = new ArrayList<AnnotatedClass>();

    @Override
    public Set<String> getAnnotationDescriptors() {
        return new LinkedHashSet<String>(BuildHintAnnotationBinding.descriptors());
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        hints.clear();
        origins.clear();
        annotated.clear();
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        Set<String> descriptors = getAnnotationDescriptors();

        // @Target(TYPE) already rejects a method or field placement at compile
        // time, but @Target is a front-end check and this reads bytecode: a
        // class produced another way could still carry one, and silently
        // ignoring it would be the exact failure this feature removes.
        for (String d : cls.getAllAnnotationDescriptors()) {
            if (descriptors.contains(d) && !cls.getClassAnnotations().containsKey(d)) {
                ctx.error(cls, "@" + simpleName(d) + " is a build hint annotation and belongs on "
                        + "the class itself, not on one of its members.");
            }
        }

        boolean carriesAny = false;
        for (Map.Entry<String, AnnotationValues> e : cls.getClassAnnotations().entrySet()) {
            if (descriptors.contains(e.getKey())) {
                carriesAny = true;
            }
        }
        if (!carriesAny) {
            return;
        }
        // An output directory keeps class files whose source is gone. Rename the
        // main class, update codename1.mainName and skip the clean, and the old
        // annotated .class is still sitting there -- so every incremental build
        // failed with a placement error naming a class the developer had already
        // deleted, and the orphan's hints were merged in besides.
        //
        // Only a class that is NOT the main one is dropped this way. The main
        // class is processed whatever its source layout, because failing to find
        // its source would otherwise mean silently applying none of its hints,
        // which is worse than any placement message.
        if (!isMainClass(cls, ctx) && !hasBackingSource(cls, ctx)) {
            ctx.getLog().debug("cn1: ignoring " + cls.getBinaryName()
                    + " -- annotated, but no source for it; stale output from an earlier build");
            return;
        }
        annotated.add(cls);

        for (Map.Entry<String, AnnotationValues> e : cls.getClassAnnotations().entrySet()) {
            String descriptor = e.getKey();
            if (!descriptors.contains(descriptor)) {
                continue;
            }
            AnnotationValues values = e.getValue();
            // Only what the developer actually wrote: javac omits a member left
            // at its default from the class file, and that absence is how an
            // unset attribute is distinguished from one set to the default
            // value. Reading through a getXxxOrDefault here would write a hint
            // for every attribute of every annotation used.
            for (Map.Entry<String, Object> member : values.all().entrySet()) {
                String hint = BuildHintAnnotationBinding.hintFor(descriptor, member.getKey());
                if (hint == null) {
                    ctx.error(cls, "@" + simpleName(descriptor) + "(" + member.getKey()
                            + ") is not a known build hint. The catalog and the annotation "
                            + "have drifted; regenerate with "
                            + "scripts/gen-build-hint-annotations.sh.");
                    continue;
                }
                String value = wireValue(cls, descriptor, member.getKey(), member.getValue(),
                        hint, ctx);
                if (value == null) {
                    continue;
                }
                String origin = "@" + simpleName(descriptor) + "(" + member.getKey() + ")";
                String previous = hints.put(hint, value);
                if (previous != null && !previous.equals(value)) {
                    ctx.error(cls, "Build hint " + hint + " is set twice with different values: "
                            + origins.get(hint) + " and " + origin + ".");
                }
                origins.put(hint, origin);
            }
        }
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (annotated.isEmpty()) {
            // The last annotation was removed. The Mojo only writes emitted
            // resources, it never deletes ones a processor stopped emitting, so
            // without this yesterday's hints would stay in target/classes and
            // ship inside the jar.
            deleteGenerated(ctx);
            return;
        }
        checkPlacement(ctx);
        checkConflicts(ctx);
        if (ctx.hasErrors()) {
            return;
        }
        ctx.emitResource(MANIFEST_RESOURCE, serialize(ctx));
        ctx.getLog().info("cn1: " + hints.size() + " build hint(s) from annotations on "
                + annotated.get(0).getBinaryName());
    }

    private static boolean isMainClass(AnnotatedClass cls, ProcessorContext ctx) {
        String main = ctx.getMainClassBinaryName();
        return main != null && main.equals(cls.getBinaryName());
    }

    /// Whether a source file for `cls` still exists under the module.
    ///
    /// Answered "yes" whenever the question cannot actually be put, because the
    /// only thing this decides is whether to IGNORE an annotated class, and
    /// ignoring a live one applies none of its hints and reports nothing. Only a
    /// class this can positively show has no source is treated as an orphan.
    ///
    /// Searched by the file name the compiler recorded, anywhere under the
    /// module's configured source roots, rather than by the package path. Both
    /// halves matter: a module may add `generated-sources` or replace
    /// `src/main/java` outright, and Kotlin lets a file's name and directory
    /// differ from the class it declares, so a package-path lookup would call a
    /// perfectly live class orphaned.
    private static boolean hasBackingSource(AnnotatedClass cls, ProcessorContext ctx) {
        List<String> roots = ctx.getCompileSourceRoots();
        if (roots == null || roots.isEmpty()) {
            return true;
        }
        String sourceFile = cls.getSourceFile();
        if (sourceFile == null || sourceFile.length() == 0) {
            // Compiled without debug information; nothing to look for.
            return true;
        }
        String pkg = packageOf(cls.getBinaryName());
        String simpleName = simpleNameOf(cls.getBinaryName());
        String[] nestedName = nestedNameOf(cls.getBinaryName());
        boolean sawARoot = false;
        for (String root : roots) {
            File dir = new File(root);
            if (!dir.isDirectory()) {
                continue;
            }
            sawARoot = true;
            if (declaresPackage(dir, sourceFile, pkg, simpleName, nestedName, 0)) {
                return true;
            }
        }
        return !sawARoot;
    }

    private static String packageOf(String binaryName) {
        int dot = binaryName.lastIndexOf('.');
        return dot < 0 ? "" : binaryName.substring(0, dot);
    }

    /// The OUTERMOST simple name, so a nested type is looked for by the type its
    /// file actually declares.
    ///
    /// A nested class's binary name is Main$Wrong, and no source declares a type
    /// spelled that way -- searching for it found nothing, the class was read as
    /// an orphan and dropped, and the placement check that would have said
    /// "annotations belong on the main class" never ran. The build then succeeded
    /// with the requested hints silently absent, which is the failure this whole
    /// feature exists to remove.
    private static String simpleNameOf(String binaryName) {
        int dot = binaryName.lastIndexOf('.');
        String simple = dot < 0 ? binaryName : binaryName.substring(dot + 1);
        int nested = simple.indexOf('$');
        return nested < 0 ? simple : simple.substring(0, nested);
    }

    /// Whether a file called `name` declaring package `pkg` exists under `dir`.
    ///
    /// The package matters as well as the name: moving a class to another package
    /// without a clean leaves an orphan whose SourceFile is, say, App.java, and
    /// the NEW App.java would otherwise answer for it -- so the stale class stays
    /// and fails the placement check on every incremental build, which is the bug
    /// this whole guard exists to prevent.
    ///
    /// The package is read from the file rather than inferred from its directory,
    /// because Kotlin does not require the two to agree. Depth-limited: this runs
    /// per annotated class and a source tree is not a search index.
    private static boolean declaresPackage(File dir, String name, String pkg, String simple,
                                           String[] nested, int depth) {
        if (depth > 24) {
            return false;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return false;
        }
        for (File f : children) {
            if (f.isFile()) {
                if (f.getName().equals(name) && matches(f, pkg, simple, nested)) {
                    return true;
                }
            } else if (f.isDirectory() && declaresPackage(f, name, pkg, simple, nested, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    /// Whether `f` declares type `simple` in package `pkg`.
    ///
    /// The declaration is checked, not just the file's name and package. Kotlin
    /// lets a class be renamed without renaming its file, and one file can hold
    /// several types, so the surviving source would otherwise answer for the
    /// class that used to be in it -- keeping a stale annotation owner and
    /// failing the placement check on every incremental build, which is the very
    /// thing this guard was added to stop.
    private static boolean matches(File f, String pkg, String simple, String[] nested) {
        String text = readHead(f);
        if (text == null) {
            // Unreadable: answer yes, as everywhere else here, because the only
            // thing this decides is whether to IGNORE an annotated class.
            return true;
        }
        // The file names its own language, and a triple-quoted literal is read
        // differently in each.
        boolean kotlin = f.getName().endsWith(".kt");
        if (!pkg.equals(declaredPackageIn(text, kotlin))
                || !declaresType(text, simple, kotlin)) {
            return false;
        }
        // The whole nesting PATH has to be there, in order. Checking only the
        // innermost name let an unrelated Main.B.Wrong vouch for a deleted
        // Main.A.Wrong, so the orphan stayed and failed the placement check on
        // every incremental build. Checking only the outer class was the same bug
        // one level out.
        return nested == null || declaresNestedPath(text, nested, kotlin);
    }

    /// Whether `text` declares the chain `path` -- {"Main", "A", "Wrong"} -- each
    /// inside the body of the one before it.
    ///
    /// Braces are counted on the blanked text, where every comment and string
    /// literal is already spaces, so a brace inside either cannot throw the
    /// nesting off.
    static boolean declaresNestedPath(String text, String[] path) {
        return declaresNestedPath(text, path, false);
    }

    static boolean declaresNestedPath(String text, String[] path, boolean kotlin) {
        String code = blankNonCode(text, kotlin);
        int from = 0;
        int end = code.length();
        for (String segment : path) {
            int at = declarationOf(code, segment, from, end);
            if (at < 0) {
                return false;
            }
            int open = code.indexOf('{', at);
            if (open < 0 || open >= end) {
                // A body-less declaration -- Kotlin's `class Foo` -- can only be
                // the last segment, and if it is not, nothing is nested in it.
                return segment.equals(path[path.length - 1]);
            }
            from = open + 1;
            end = matchingBrace(code, open);
        }
        return true;
    }

    /// The offset of a `class`/`interface`/`enum`/`object`/`record` declaration of
    /// `simple` declared DIRECTLY between `from` and `end`, or -1.
    ///
    /// Directly: at brace depth zero within that range. A match at any depth
    /// would let Main.B.Wrong answer for Main.Wrong, which is the same
    /// wrong-identity bug one level along -- and Main.Wrong not existing is
    /// exactly when its .class is an orphan.
    private static int declarationOf(String code, String simple, int from, int end) {
        return declarationOf(code, simple, from, end, true);
    }

    /// As above; `directOnly` restricts the match to brace depth zero within the
    /// range, which is what nesting identity needs and what a plain "does this
    /// file declare X" question does not.
    private static int declarationOf(String code, String simple, int from, int end,
                                     boolean directOnly) {
        int depth = 0;
        int i = from;
        while (i < end && i < code.length()) {
            char c = code.charAt(i);
            if (c == '{') {
                depth++;
                i++;
                continue;
            }
            if (c == '}') {
                depth--;
                i++;
                continue;
            }
            if ((directOnly && depth != 0) || !Character.isJavaIdentifierStart(c)
                    || (i > 0 && Character.isJavaIdentifierPart(code.charAt(i - 1)))) {
                i++;
                continue;
            }
            int wordEnd = i;
            while (wordEnd < code.length() && Character.isJavaIdentifierPart(code.charAt(wordEnd))) {
                wordEnd++;
            }
            String word = code.substring(i, wordEnd);
            if (isTypeKeyword(word)) {
                int n = wordEnd;
                while (n < end && Character.isWhitespace(code.charAt(n))) {
                    n++;
                }
                int stop = n;
                while (stop < end && Character.isJavaIdentifierPart(code.charAt(stop))) {
                    stop++;
                }
                if (code.substring(n, stop).equals(simple)) {
                    return i;
                }
            }
            i = wordEnd;
        }
        return -1;
    }

    private static boolean isTypeKeyword(String word) {
        return "class".equals(word) || "interface".equals(word) || "enum".equals(word)
                || "object".equals(word) || "record".equals(word);
    }

    /// The index just past the brace closing the one at `open`.
    private static int matchingBrace(String code, int open) {
        int depth = 0;
        for (int i = open; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return code.length();
    }

    /// The innermost named segment of a nested binary name, or null.
    ///
    /// Null for an unnamed segment -- Main$1 is an anonymous class, which no
    /// source declares and which cannot carry an annotation in the first place,
    /// so there is nothing to look for and nothing to conclude from not finding
    /// it.
    static String[] nestedNameOf(String binaryName) {
        int dot = binaryName.lastIndexOf('.');
        String simple = dot < 0 ? binaryName : binaryName.substring(dot + 1);
        if (simple.indexOf('$') < 0) {
            return null;
        }
        String[] path = simple.split("\\$");
        for (String segment : path) {
            // A segment BEGINNING with a digit is javac's, not the developer's:
            // $1 for an anonymous class and $1Wrong for a named local one. No
            // source declares either spelling, so looking for it finds nothing --
            // and concluding "orphan" from that dropped a live annotated local
            // class before the placement check could reject it, which let the
            // build succeed with the requested hints silently discarded.
            //
            // Checking for wholly-numeric missed $1Wrong exactly.
            if (segment.length() == 0 || Character.isDigit(segment.charAt(0))) {
                return null;
            }
        }
        return path;
    }

    /// Whether `text` declares a type called `simple`.
    ///
    /// Comments and string literals are blanked first: a commented-out
    /// `// class Wrong` left behind by the very edit that deleted the type would
    /// otherwise vouch for its own orphan.
    public static boolean declaresType(String text, String simple) {
        return declaresType(text, simple, false);
    }

    /// As above, reading the source by `kotlin`'s rules.
    public static boolean declaresType(String text, String simple, boolean kotlin) {
        String code = blankNonCode(text, kotlin);
        return declarationOf(code, simple, 0, code.length(), false) >= 0;
    }

    /// `text` with every comment and string literal replaced by spaces, so a
    /// declaration can be looked for without a quoted or commented mention of one
    /// answering for it. Lengths and line breaks are preserved.
    public static String blankNonCode(String text) {
        return blankNonCode(text, false);
    }

    /// As above, reading triple-quoted literals by the rules of `kotlin`'s
    /// language.
    ///
    /// The two differ and reading one as the other over-consumes: a Kotlin raw
    /// string ends at the LAST three quotes of a run, while a Java text block
    /// processes escapes so `\"""` is not a delimiter. Getting it wrong blanks
    /// the declaration that follows, so a live class reads as an orphan and its
    /// misplaced annotation is never reported.
    public static String blankNonCode(String text, boolean kotlin) {
        char[] out = text.toCharArray();
        int i = 0;
        while (i < out.length) {
            char c = out[i];
            if (c == '/' && i + 1 < out.length && out[i + 1] == '/') {
                while (i < out.length && out[i] != '\n') {
                    out[i++] = ' ';
                }
            } else if (c == '/' && i + 1 < out.length && out[i + 1] == '*') {
                out[i++] = ' ';
                out[i++] = ' ';
                while (i < out.length && !(out[i] == '*' && i + 1 < out.length
                        && out[i + 1] == '/')) {
                    if (out[i] != '\n') {
                        out[i] = ' ';
                    }
                    i++;
                }
                if (i < out.length) {
                    out[i++] = ' ';
                }
                if (i < out.length) {
                    out[i++] = ' ';
                }
            } else if (c == '\'') {
                // A char literal. '{' is legal and would otherwise be counted as
                // syntax, so the nesting scan loses its place and reads a live
                // class as an orphan.
                out[i++] = ' ';
                while (i < out.length) {
                    if (out[i] == '\\' && i + 1 < out.length) {
                        out[i++] = ' ';
                        out[i++] = ' ';
                        continue;
                    }
                    boolean closing = out[i] == '\'';
                    if (out[i] != '\n') {
                        out[i] = ' ';
                    }
                    i++;
                    if (closing) {
                        break;
                    }
                }
            } else if (c == '"' && i + 2 < out.length && out[i + 1] == '"' && out[i + 2] == '"') {
                int close = kotlin ? endOfKotlinRawString(out, i) : endOfJavaTextBlock(out, i);
                while (i < close && i < out.length) {
                    if (out[i] != '\n') {
                        out[i] = ' ';
                    }
                    i++;
                }
            } else if (c == '"') {
                out[i++] = ' ';
                while (i < out.length) {
                    if (out[i] == '\\' && i + 1 < out.length) {
                        out[i++] = ' ';
                        out[i++] = ' ';
                        continue;
                    }
                    boolean closing = out[i] == '"';
                    if (out[i] != '\n') {
                        out[i] = ' ';
                    }
                    i++;
                    if (closing) {
                        break;
                    }
                }
            } else {
                i++;
            }
        }
        return new String(out);
    }

    /// Index just past a Java text block opening at `i`. Escapes apply, so a
    /// backslash consumes the next character and cannot start the delimiter.
    private static int endOfJavaTextBlock(char[] c, int i) {
        int j = i + 3;
        while (j < c.length) {
            if (c[j] == '\\') {
                j += 2;
                continue;
            }
            if (c[j] == '"' && j + 2 < c.length && c[j + 1] == '"' && c[j + 2] == '"') {
                return j + 3;
            }
            j++;
        }
        return c.length;
    }

    /// Index just past a Kotlin raw string opening at `i`. No escapes, and a run
    /// of quotes closes at its LAST three, so the extra ones belong to the value.
    private static int endOfKotlinRawString(char[] c, int i) {
        int j = i + 3;
        while (j < c.length) {
            if (c[j] != '"') {
                j++;
                continue;
            }
            int run = j;
            while (run < c.length && c[run] == '"') {
                run++;
            }
            if (run - j >= 3) {
                return run;
            }
            j = run;
        }
        return c.length;
    }

    /// The whole of `f`, or null when it cannot be read or is implausibly large.
    ///
    /// Not a prefix. A line bound looked harmless and was not: a type declared
    /// below it -- after a long generated header, or a big import block -- was
    /// not found, so a live class read as an orphan and its misplaced annotation
    /// was skipped instead of reported. A nesting scan also cannot start in the
    /// middle of a file and still count braces.
    ///
    /// The size cap is a guard against something that is not source at all, not
    /// a budget: 4MB is far past any hand-written Java or Kotlin file, and
    /// exceeding it returns null, which the caller reads as "cannot tell" and so
    /// keeps the class.
    private static String readHead(File f) {
        if (f.length() > 4L * 1024 * 1024) {
            return null;
        }
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException ex) {
            return null;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignored) {
                    // read-only stream
                }
            }
        }
    }

    /// The package `text` declares, or "" for the default package.
    public static String declaredPackageIn(String text) {
        return declaredPackageIn(text, false);
    }

    /// As above, reading the source by `kotlin`'s rules.
    public static String declaredPackageIn(String text, boolean kotlin) {
        // Tokens, not lines. `package\ncom.example;` is valid Java, and reading
        // one physical line saw an empty remainder and reported the default
        // package -- so a live class looked like it belonged somewhere else, read
        // as an orphan, and its misplaced annotation went unreported.
        String code = blankNonCode(text);
        int i = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
            if (!Character.isJavaIdentifierStart(c)
                    || (i > 0 && Character.isJavaIdentifierPart(code.charAt(i - 1)))) {
                i++;
                continue;
            }
            int wordEnd = i;
            while (wordEnd < code.length()
                    && Character.isJavaIdentifierPart(code.charAt(wordEnd))) {
                wordEnd++;
            }
            if (!"package".equals(code.substring(i, wordEnd))) {
                i = wordEnd;
                continue;
            }
            int n = wordEnd;
            while (n < code.length() && Character.isWhitespace(code.charAt(n))) {
                n++;
            }
            StringBuilder name = new StringBuilder();
            while (n < code.length()
                    && (Character.isJavaIdentifierPart(code.charAt(n)) || code.charAt(n) == '.')) {
                name.append(code.charAt(n));
                n++;
            }
            return name.toString();
        }
        return "";
    }

    /// Build hints configure the application, so they belong on the class the    /// Build hints configure the application, so they belong on the class the
    /// project already names as its entry point.
    ///
    /// Accepting them anywhere would mean two classes could set the same hint
    /// and the winner would depend on the order `File.listFiles` happened to
    /// return -- and it would scatter the effective build configuration across
    /// the source tree, which is the problem the properties file already had.
    private void checkPlacement(ProcessorContext ctx) {
        String main = ctx.getMainClassBinaryName();
        if (main == null) {
            ctx.error(annotated.get(0),
                    "Build hint annotations are only supported in a Codename One application, "
                            + "and this module declares no codename1.mainName.");
            return;
        }
        for (AnnotatedClass cls : annotated) {
            if (!main.equals(cls.getBinaryName())) {
                ctx.error(cls, "Build hint annotations belong on the application's main class, "
                        + main + ", but this one carries them. Move them there, or set the hint "
                        + "in codenameone_settings.properties.");
            }
        }
    }

    /// A hint has one source of truth. Setting it in both places means the two
    /// can disagree, and nothing would say which won.
    private void checkConflicts(ProcessorContext ctx) {
        Properties settings = ctx.getProjectSettings();
        if (settings == null) {
            return;
        }
        Map<String, Integer> lines = propertyLines(ctx);
        for (Map.Entry<String, String> e : hints.entrySet()) {
            Set<String> names = spellingsOf(e.getKey());
            for (String name : names) {
                String key = BuildHints.ARG_PREFIX + name;
                if (settings.getProperty(key) == null) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                sb.append(key).append(" is declared twice.\n");
                sb.append("    annotation : ").append(origins.get(e.getKey()))
                  .append(" on ").append(annotated.get(0).getBinaryName()).append('\n');
                sb.append("    properties : ");
                File f = settingsFile(ctx);
                sb.append(f == null ? "codenameone_settings.properties" : f.getPath());
                Integer line = lines.get(key);
                if (line != null) {
                    sb.append(':').append(line);
                }
                sb.append('\n');
                sb.append("                 ").append(key).append('=')
                  .append(settings.getProperty(key)).append('\n');
                sb.append("    A build hint has one source of truth. Delete the properties line "
                        + "and keep the annotation, or delete the annotation attribute and keep "
                        + "the line. (-D").append(key).append("=... overrides either and is not "
                        + "a conflict.)");
                ctx.error(annotated.get(0), sb.toString());
            }
        }
    }

    private File settingsFile(ProcessorContext ctx) {
        File dir = ctx.getProjectDir();
        if (dir == null) {
            return null;
        }
        File f = new File(dir, "codenameone_settings.properties");
        return f.exists() ? f : null;
    }

    /// Best-effort key to line number, so the conflict message can point at the
    /// offending line. Properties escaping means an exotic key may not match;
    /// the message then names the file only rather than guessing.
    private Map<String, Integer> propertyLines(ProcessorContext ctx) {
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        File f = settingsFile(ctx);
        if (f == null) {
            return out;
        }
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "ISO-8859-1"));
            String line;
            int n = 0;
            while ((line = r.readLine()) != null) {
                n++;
                String t = line.trim();
                if (t.length() == 0 || t.charAt(0) == '#' || t.charAt(0) == '!') {
                    continue;
                }
                int eq = t.indexOf('=');
                int colon = t.indexOf(':');
                int split = eq < 0 ? colon : (colon < 0 ? eq : Math.min(eq, colon));
                if (split <= 0) {
                    continue;
                }
                String key = t.substring(0, split).trim();
                if (!out.containsKey(key)) {
                    out.put(key, Integer.valueOf(n));
                }
            }
        } catch (IOException ex) {
            ctx.getLog().debug("cn1: could not read " + f + " for line numbers: " + ex.getMessage());
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ignored) {
                    // read-only stream; nothing useful to do
                }
            }
        }
        return out;
    }

    /// Converts one annotation member value to the string the build receives.
    ///
    /// Returns null when the value could not be converted, having reported it.
    private String wireValue(AnnotatedClass cls, String descriptor, String member, Object raw,
                             String hint, ProcessorContext ctx) {
        if (raw instanceof Boolean || raw instanceof Number || raw instanceof Character) {
            return String.valueOf(raw);
        }
        if (raw instanceof String) {
            return (String) raw;
        }
        // ASM reports an enum constant as { descriptor, CONSTANT_NAME }. The
        // constant name is not the value the builder compares against, and a
        // builder silently falls back to its default on a value it does not
        // recognise, so guessing here would fail invisibly.
        if (raw instanceof String[]) {
            String[] pair = (String[]) raw;
            if (pair.length == 2) {
                String wire = BuildHintAnnotationBinding.wireValue(pair[0], pair[1]);
                if (wire == null) {
                    ctx.error(cls, "@" + simpleName(descriptor) + "(" + member + ") uses the "
                            + "constant " + pair[1] + ", which the build hint catalog does not "
                            + "map to a value. Regenerate with "
                            + "scripts/gen-build-hint-annotations.sh.");
                    return null;
                }
                return wire;
            }
        }
        if (raw instanceof List) {
            String separator = BuildHints.separatorFor(hint);
            if (separator.length() == 0) {
                ctx.error(cls, "@" + simpleName(descriptor) + "(" + member + ") is a list but the "
                        + "catalog gives " + hint + " no separator, so its values would run "
                        + "together.");
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Object item : (List<?>) raw) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
                String itemValue = wireValue(cls, descriptor, member, item, hint, ctx);
                if (itemValue == null) {
                    return null;
                }
                sb.append(itemValue);
            }
            return sb.toString();
        }
        ctx.error(cls, "@" + simpleName(descriptor) + "(" + member + ") has a value this "
                + "processor cannot convert: " + raw);
        return null;
    }

    /// Serializes deterministically.
    ///
    /// Not `Properties.store`: it writes a timestamp comment, so the bytes would
    /// differ on every build. That churns the resource in every incremental
    /// build and defeats the staged-jar staleness comparison in `CN1BuildMojo`.
    /// Every name that denotes the same setting as `hint`, itself included.
    ///
    /// An alias and its target are one setting -- the builder reads
    /// `android.captureRecord` and then lets `and.captureRecord` override it --
    /// so declaring either in the properties file collides with the annotation.
    static Set<String> spellingsOf(String hint) {
        Set<String> names = new LinkedHashSet<String>();
        names.add(hint);
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (hint.equals(h.aliasOf()) || hint.equals(BuildHints.canonicalName(h.name()))) {
                names.add(h.name());
            }
        }
        return names;
    }

    /// A stable fingerprint of every build hint annotation on `cls`.    /// A stable fingerprint of every build hint annotation on `cls`.
    ///
    /// Taken over the raw annotation members rather than over the hints they
    /// convert into, so it changes for anything the developer can change: a
    /// different value, an added or removed attribute, a whole annotation
    /// gained or lost. Two builds of the same source produce the same string;
    /// there is no timestamp or path in it.
    public static String sourceDigest(AnnotatedClass cls) throws ProcessingException {
        StringBuilder sb = new StringBuilder();
        Set<String> known = new HashSet<String>(BuildHintAnnotationBinding.descriptors());
        // Sorted, because the class file's annotation order is the source's and a
        // reordering is not a change.
        for (String descriptor : new TreeMap<String, AnnotationValues>(
                cls.getClassAnnotations()).keySet()) {
            if (!known.contains(descriptor)) {
                continue;
            }
            emit(sb, descriptor);
            AnnotationValues values = cls.getClassAnnotation(descriptor);
            emit(sb, String.valueOf(values.all().size()));
            for (Map.Entry<String, Object> e
                    : new TreeMap<String, Object>(values.all()).entrySet()) {
                emit(sb, e.getKey());
                renderForDigest(e.getValue(), sb);
            }
        }
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sb.toString().getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xf, 16));
                hex.append(Character.forDigit(b & 0xf, 16));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            throw new ProcessingException("Could not fingerprint the build hint annotations", ex);
        }
    }

    /// Appends `s` length-prefixed, so nothing it contains can be read as
    /// structure.
    ///
    /// A value IS a place an attacker -- or an unlucky developer -- writes
    /// arbitrary text: with plain delimiters,
    /// `@Ios(bundleVersion = "1;teamId=java.lang.String:X")` rendered exactly
    /// like `@Ios(bundleVersion = "1", teamId = "X")`, so a stale manifest was
    /// accepted for a different configuration and the build silently kept the old
    /// values.
    private static void emit(StringBuilder sb, String s) {
        sb.append(s.length()).append(':').append(s).append(';');
    }

    /// The type is part of the rendering, so an int 1 and the string "1" -- which
    /// print alike but are different annotations -- do not fingerprint alike.
    ///
    /// Every variable-length piece goes through `emit`, so no value can forge the
    /// structure around it.
    private static void renderForDigest(Object value, StringBuilder sb) {
        if (value == null) {
            emit(sb, "null");
        } else if (value instanceof String[]) {
            // How ASM delivers an enum member: {descriptor, CONSTANT_NAME}.
            String[] e = (String[]) value;
            emit(sb, "enum");
            emit(sb, e.length > 0 ? e[0] : "");
            emit(sb, e.length > 1 ? e[1] : "");
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            emit(sb, "list");
            emit(sb, String.valueOf(list.size()));
            for (Object item : list) {
                renderForDigest(item, sb);
            }
        } else if (value instanceof AnnotationValues) {
            AnnotationValues nested = (AnnotationValues) value;
            emit(sb, "annotation");
            emit(sb, nested.getDescriptor());
            emit(sb, String.valueOf(nested.all().size()));
            for (Map.Entry<String, Object> e
                    : new TreeMap<String, Object>(nested.all()).entrySet()) {
                emit(sb, e.getKey());
                renderForDigest(e.getValue(), sb);
            }
        } else {
            emit(sb, value.getClass().getName());
            emit(sb, String.valueOf(value));
        }
    }

    private byte[] serialize(ProcessorContext ctx) throws ProcessingException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Generated from build hint annotations by the Codename One Maven plugin.\n");
        sb.append("# Edit the annotations on the main class, not this file.\n");
        String main = ctx.getMainClassBinaryName();
        if (main != null) {
            sb.append(MAIN_CLASS_KEY).append('=').append(escape(main)).append('\n');
        }
        sb.append(SOURCE_DIGEST_KEY).append('=')
          .append(sourceDigest(annotated.get(0))).append('\n');
        for (Map.Entry<String, String> e : hints.entrySet()) {
            sb.append(escape(BuildHints.ARG_PREFIX + e.getKey())).append('=')
              .append(escape(e.getValue())).append('\n');
        }
        for (Map.Entry<String, String> e : origins.entrySet()) {
            sb.append(escape(ORIGIN_PREFIX + e.getKey())).append('=')
              .append(escape(e.getValue())).append('\n');
        }
        // The other spellings of each hint, for a consumer that has to collapse
        // them and cannot reach the catalog -- the simulator, which lives in the
        // JavaSE port. Written only where there is more than one, so the common
        // hint costs nothing.
        for (String hint : hints.keySet()) {
            Set<String> spellings = spellingsOf(hint);
            spellings.remove(hint);
            if (spellings.isEmpty()) {
                continue;
            }
            StringBuilder joined = new StringBuilder();
            for (String name : spellings) {
                if (joined.length() > 0) {
                    joined.append(',');
                }
                joined.append(name);
            }
            sb.append(escape(ALIAS_PREFIX + hint)).append('=')
              .append(escape(joined.toString())).append('\n');
        }
        try {
            return sb.toString().getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException ex) {
            throw new ProcessingException("ISO-8859-1 is unavailable", ex);
        }
    }

    /// Applies the escaping `java.util.Properties` expects, so a value holding a
    /// newline -- `gradleDependencies` legitimately does -- survives the round
    /// trip.
    static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '=': sb.append("\\="); break;
                case ':': sb.append("\\:"); break;
                case '#': sb.append("\\#"); break;
                case '!': sb.append("\\!"); break;
                case ' ': sb.append(i == 0 ? "\\ " : " "); break;
                default:
                    if (c < 0x20 || c > 0x7e) {
                        sb.append(String.format("\\u%04x", Integer.valueOf(c)));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private void deleteGenerated(ProcessorContext ctx) {
        File f = new File(ctx.getOutputClassDir(), MANIFEST_RESOURCE);
        if (f.exists() && !f.delete()) {
            ctx.getLog().warn("cn1: could not remove stale " + f + "; it would be packaged "
                    + "with hints the project no longer declares");
        }
    }

    private static String simpleName(String descriptor) {
        String s = descriptor;
        if (s.startsWith("L") && s.endsWith(";")) {
            s = s.substring(1, s.length() - 1);
        }
        int slash = s.lastIndexOf('/');
        return slash >= 0 ? s.substring(slash + 1) : s;
    }
}

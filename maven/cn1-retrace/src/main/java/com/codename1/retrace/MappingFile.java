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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a ProGuard-format {@code mapping.txt} and inverts an obfuscated frame back
 * to its original class and method. This is the same format the hardening engine's
 * cross-platform mapping and Android's R8 mapping both use, so one parser serves
 * every port.
 *
 * <p>Comment lines (the engine's provenance header, {@code # ...}) are ignored.
 */
public final class MappingFile {

    private static final class MethodMapping {
        final String originalName;
        final String declaringClass; // FQ class an inlined method came from, or null for this class
        final int startLine;         // obfuscated range start (0 if none)
        final int endLine;           // obfuscated range end
        final int originalStartLine; // original range start (0 if none / same)
        final int originalEndLine;   // original range end (== start for a single line)

        MethodMapping(String originalName, String declaringClass, int startLine, int endLine,
                      int originalStartLine, int originalEndLine) {
            this.originalName = originalName;
            this.declaringClass = declaringClass;
            this.startLine = startLine;
            this.endLine = endLine;
            this.originalStartLine = originalStartLine;
            this.originalEndLine = originalEndLine;
        }

        /**
         * Maps an observed obfuscated line into the original source line. A single-line original
         * range ({@code originalStart == originalEnd}) collapses every covered line to that line;
         * otherwise the offset is applied but clamped to the original range end so a shorter
         * original range never overshoots.
         */
        int mapLine(int observed) {
            if (startLine == 0 || originalStartLine == 0 || observed < startLine || observed > endLine) {
                return observed;
            }
            if (originalEndLine <= originalStartLine) {
                return originalStartLine;
            }
            int mapped = originalStartLine + (observed - startLine);
            return mapped > originalEndLine ? originalEndLine : mapped;
        }
    }

    private static final class ClassMapping {
        final String originalName;
        // obfuscated member name -> candidate original methods (multiple when line ranges differ)
        final Map<String, List<MethodMapping>> methods = new HashMap<String, List<MethodMapping>>();
        // R8's recorded source file (e.g. Screen.kt) from a "# {"id":"sourceFile",...}" comment; null
        // when the mapping carries no such metadata. Lets a hardened build -- which strips SourceFile
        // from the binary, so the device reports no filename -- still name the real source file.
        String sourceFile;

        ClassMapping(String originalName) {
            this.originalName = originalName;
        }
    }

    // obfuscated class binary name -> mapping
    private final Map<String, ClassMapping> byObfuscated = new HashMap<String, ClassMapping>();
    // Same ClassMapping objects keyed by their ORIGINAL (deobfuscated) FQCN, so an inlined method's
    // declaring class -- named by its original name on the member line -- can recover its sourceFile.
    private final Map<String, ClassMapping> byOriginal = new HashMap<String, ClassMapping>();

    public static MappingFile parse(String text) throws IOException {
        return parse(new StringReader(text));
    }

    public static MappingFile parse(Reader reader) throws IOException {
        MappingFile mf = new MappingFile();
        BufferedReader r = new BufferedReader(reader);
        String line;
        ClassMapping current = null;
        while ((line = r.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            if (!Character.isWhitespace(line.charAt(0))) {
                // Class line: "original -> obfuscated:"
                current = mf.parseClassLine(line);
            } else if (current != null) {
                String trimmed = line.trim();
                // R8 records per-class metadata as an INDENTED comment, e.g.
                // # {"id":"sourceFile","fileName":"Screen.kt"}. It is not a member line (no " -> "),
                // so capture the source file here rather than dropping it in parseMemberLine.
                if (trimmed.startsWith("#")) {
                    String sf = parseSourceFileMetadata(trimmed);
                    if (sf != null) {
                        current.sourceFile = sf;
                    }
                } else {
                    mf.parseMemberLine(current, trimmed);
                }
            }
        }
        return mf;
    }

    /**
     * Extracts the {@code fileName} from an R8 {@code sourceFile} metadata comment such as
     * {@code # {"id":"sourceFile","fileName":"Screen.kt"}}, or {@code null} when the comment is not
     * one. Deliberately a small hand scan rather than a JSON dependency (this module is zero-dep), but it
     * DOES honor JSON string escapes: the engine writes the value with backslash-escaped {@code "} and
     * {@code \}, so a filename containing either (a Unix path can) must be decoded back rather than
     * truncated at the first escaped quote.
     */
    private static String parseSourceFileMetadata(String comment) {
        if (comment.indexOf("\"id\":\"sourceFile\"") < 0) {
            return null;
        }
        String key = "\"fileName\":\"";
        int at = comment.indexOf(key);
        if (at < 0) {
            return null;
        }
        int i = at + key.length();
        int n = comment.length();
        StringBuilder name = new StringBuilder();
        while (i < n) {
            char c = comment.charAt(i);
            if (c == '\\' && i + 1 < n) {
                // A JSON escape: the next character is literal (covers the \" and \\ the writer emits).
                name.append(comment.charAt(i + 1));
                i += 2;
            } else if (c == '"') {
                String s = name.toString().trim();
                return s.length() == 0 ? null : s;
            } else {
                name.append(c);
                i++;
            }
        }
        return null;   // unterminated JSON string
    }

    private ClassMapping parseClassLine(String line) {
        int arrow = line.indexOf(" -> ");
        if (arrow < 0 || !line.endsWith(":")) {
            return null;
        }
        String original = line.substring(0, arrow).trim();
        String obf = line.substring(arrow + 4, line.length() - 1).trim();
        ClassMapping cm = new ClassMapping(original);
        byObfuscated.put(obf, cm);
        // Also index by original FQCN so an inlined method's declaring class (recorded by its ORIGINAL
        // name on the member line) can recover its own sourceFile metadata for the inline frame.
        byOriginal.put(original, cm);
        return cm;
    }

    private void parseMemberLine(ClassMapping cm, String line) {
        int arrow = line.indexOf(" -> ");
        if (arrow < 0) {
            return;
        }
        String left = line.substring(0, arrow);
        String obfName = line.substring(arrow + 4).trim();
        // Fields have no '(' ; only methods matter for frame retrace.
        if (left.indexOf('(') < 0) {
            return;
        }
        int startLine = 0;
        int endLine = 0;
        // Optional "start:end:" prefix.
        int firstColon = left.indexOf(':');
        if (firstColon >= 0) {
            int secondColon = left.indexOf(':', firstColon + 1);
            if (secondColon > firstColon) {
                startLine = parseIntSafe(left.substring(0, firstColon));
                endLine = parseIntSafe(left.substring(firstColon + 1, secondColon));
                left = left.substring(secondColon + 1);
            }
        }
        // left is now "returnType methodName(args)" optionally followed by ":origStart[:origEnd]"
        // (R8 / optimized ProGuard maps the obfuscated range to a distinct original range).
        int originalStartLine = 0;
        int originalEndLine = 0;
        int closeParen = left.indexOf(')');
        if (closeParen >= 0) {
            String afterParen = left.substring(closeParen + 1);
            if (afterParen.startsWith(":")) {
                String[] parts = afterParen.substring(1).split(":");
                if (parts.length >= 1) {
                    originalStartLine = parseIntSafe(parts[0]);
                }
                originalEndLine = parts.length >= 2 ? parseIntSafe(parts[1]) : originalStartLine;
            }
            left = left.substring(0, closeParen + 1);
        }
        // Extract the method name from "returnType methodName(args)".
        int paren = left.indexOf('(');
        String beforeParen = left.substring(0, paren).trim();
        int sp = beforeParen.lastIndexOf(' ');
        String qualifiedMethod = sp < 0 ? beforeParen : beforeParen.substring(sp + 1);
        // An R8 inline record can name a method from ANOTHER class, fully qualified
        // ("com.example.Callee.run"). Split the declaring class off so the retraced frame reports
        // Callee.run / Callee.java rather than gluing the callee's FQ name onto the enclosing class.
        String declaringClass = null;
        String originalMethod = qualifiedMethod;
        int lastDot = qualifiedMethod.lastIndexOf('.');
        if (lastDot > 0) {
            declaringClass = qualifiedMethod.substring(0, lastDot);
            originalMethod = qualifiedMethod.substring(lastDot + 1);
        }
        List<MethodMapping> list = cm.methods.get(obfName);
        if (list == null) {
            list = new ArrayList<MethodMapping>();
            cm.methods.put(obfName, list);
        }
        list.add(new MethodMapping(originalMethod, declaringClass, startLine, endLine, originalStartLine, originalEndLine));
    }

    /**
     * Inverts one frame. If the class is unknown, the frame is returned unchanged (an unmapped
     * frame is better than a dropped one). Line numbers pass through -- ParparVM reports true
     * source lines on real frames.
     */
    public Frame retrace(Frame obfuscated) {
        // The first (innermost) frame; retraceAll is the full expansion including inlined callers.
        return retraceAll(obfuscated).get(0);
    }

    /**
     * Inverts one obfuscated frame into one or more original frames. An optimized R8 mapping records
     * several methods for the same obfuscated name and line range -- the inlined callee(s) and the
     * caller they were inlined into -- and all of them describe that single physical frame. Returning
     * only the first would silently drop the inlined callers and mis-identify the call path, so this
     * emits every record whose range covers the line, in R8's order (innermost first). Always returns
     * at least one frame (the input unchanged when the class is unknown).
     */
    public List<Frame> retraceAll(Frame obfuscated) {
        ClassMapping cm = byObfuscated.get(obfuscated.getClassName());
        if (cm == null) {
            return java.util.Collections.singletonList(obfuscated);
        }
        int observed = obfuscated.getLineNumber();
        String originalClass = cm.originalName;
        // For the enclosing class, keep the filename the frame actually reported when it is a real
        // source name that renaming can't reconstruct -- a Kotlin frame carries Screen.kt, and a
        // package-private class carries the file it was declared in (Main.java). But when the reported
        // name is just the obfuscated class name with an extension (SourceFile renamed to match the
        // obfuscated class, or a ParparVM synthesized <obfClass>.java), it carries no information, so
        // synthesize <OriginalClass>.java from the retraced class instead.
        String file = preferredSourceFile(obfuscated.getFileName(), obfuscated.getClassName(),
                originalClass, cm.sourceFile);
        // ParparVM records a constructor / static initializer under the runtime sentinel names
        // __INIT__ / __CLINIT__ (BytecodeMethod), but a ProGuard mapping keys them as <init>/<clinit>.
        // Normalize before the lookup, or the frame misses its method record and keeps the sentinel
        // name with no line mapping.
        String methodName = normalizeInitializer(obfuscated.getMethodName());
        List<MethodMapping> candidates = cm.methods.get(methodName);
        List<Frame> out = new ArrayList<Frame>();
        if (candidates != null && !candidates.isEmpty()) {
            for (MethodMapping m : candidates) {
                if (m.startLine != 0 && observed >= m.startLine && observed <= m.endLine) {
                    out.add(frameFor(m, originalClass, file, observed));
                }
            }
            if (out.isEmpty()) {
                // No line to disambiguate (Unknown Source, or the mapping records omit obfuscated
                // ranges). ProGuard/R8 can reuse one obfuscated name for several overloads, so picking
                // the first would name the wrong original method. Emit every candidate to preserve the
                // ambiguity rather than fabricate a single answer.
                for (MethodMapping m : candidates) {
                    out.add(frameFor(m, originalClass, file, observed));
                }
            }
        } else {
            out.add(new Frame(originalClass, methodName, file, observed));
        }
        return out;
    }

    /**
     * Maps ParparVM's runtime initializer sentinels to the JVM names a ProGuard mapping uses, so a
     * crash inside a constructor or static initializer resolves its method record. Any other name is
     * returned unchanged.
     */
    private static String normalizeInitializer(String methodName) {
        if ("__INIT__".equals(methodName)) {
            return "<init>";
        }
        if ("__CLINIT__".equals(methodName)) {
            return "<clinit>";
        }
        return methodName;
    }

    /** Builds a frame for one method record, honoring an inlinee's own declaring class/source file. */
    private Frame frameFor(MethodMapping m, String enclosingClass, String enclosingFile, int observed) {
        String cls = m.declaringClass != null ? m.declaringClass : enclosingClass;
        String file;
        if (m.declaringClass != null) {
            // An inlined method comes from another class (Helper.kt, a package-private class in a
            // differently named file). Prefer that class's own recorded sourceFile metadata when the
            // mapping has it, so the inline frame points at Helper.kt rather than a fabricated
            // Helper.java; fall back to the synthesized <DeclaringClass>.java only when it has none.
            ClassMapping dc = byOriginal.get(m.declaringClass);
            file = synthesizedSourceFile(m.declaringClass, dc != null ? dc.sourceFile : null);
        } else {
            file = enclosingFile;
        }
        return new Frame(cls, m.originalName, file, m.mapLine(observed));
    }

    /**
     * The source file to report for the enclosing class. Keeps a real reported name (Screen.kt,
     * Main.java); otherwise -- when the reported name is empty or is just the obfuscated class name with
     * an extension (a renamed/synthesized placeholder) -- prefers R8's recorded {@code sourceFile}
     * metadata when the mapping has it (so a hardened build that stripped SourceFile still names
     * Screen.kt), and only falls back to synthesizing {@code <OriginalClass>.java} when it does not.
     */
    private static String preferredSourceFile(String reported, String obfClassName, String originalClass,
            String mappedSourceFile) {
        if (reported == null || reported.length() == 0) {
            return synthesizedSourceFile(originalClass, mappedSourceFile);
        }
        int dot = reported.lastIndexOf('.');
        String reportedBase = dot > 0 ? reported.substring(0, dot) : reported;
        String obfSimple = obfClassName;
        int sep = Math.max(obfSimple.lastIndexOf('.'), obfSimple.lastIndexOf('/'));
        if (sep >= 0) {
            obfSimple = obfSimple.substring(sep + 1);
        }
        int dollar = obfSimple.indexOf('$');
        if (dollar > 0) {
            obfSimple = obfSimple.substring(0, dollar);
        }
        if (reportedBase.equals(obfSimple)) {
            return synthesizedSourceFile(originalClass, mappedSourceFile);
        }
        return reported;
    }

    /** R8's recorded source file when the mapping has it, else a synthesized {@code <Class>.java}. */
    private static String synthesizedSourceFile(String originalClass, String mappedSourceFile) {
        if (mappedSourceFile != null && mappedSourceFile.length() > 0) {
            return mappedSourceFile;
        }
        return simpleSourceFile(originalClass);
    }

    private static String simpleSourceFile(String fqcn) {
        int d = fqcn.lastIndexOf('.');
        String simple = d < 0 ? fqcn : fqcn.substring(d + 1);
        int dollar = simple.indexOf('$');
        if (dollar > 0) {
            simple = simple.substring(0, dollar);
        }
        return simple + ".java";
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Number of classes in the mapping. */
    public int size() {
        return byObfuscated.size();
    }
}

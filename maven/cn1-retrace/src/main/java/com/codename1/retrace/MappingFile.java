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

        ClassMapping(String originalName) {
            this.originalName = originalName;
        }
    }

    // obfuscated class binary name -> mapping
    private final Map<String, ClassMapping> byObfuscated = new HashMap<String, ClassMapping>();

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
                mf.parseMemberLine(current, line.trim());
            }
        }
        return mf;
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
        String file = simpleSourceFile(originalClass);
        List<MethodMapping> candidates = cm.methods.get(obfuscated.getMethodName());
        List<Frame> out = new ArrayList<Frame>();
        if (candidates != null && !candidates.isEmpty()) {
            for (MethodMapping m : candidates) {
                if (m.startLine != 0 && observed >= m.startLine && observed <= m.endLine) {
                    out.add(frameFor(m, originalClass, file, observed));
                }
            }
            if (out.isEmpty()) {
                out.add(frameFor(candidates.get(0), originalClass, file, observed));
            }
        } else {
            out.add(new Frame(originalClass, obfuscated.getMethodName(), file, observed));
        }
        return out;
    }

    /** Builds a frame for one method record, honoring an inlinee's own declaring class/source file. */
    private Frame frameFor(MethodMapping m, String enclosingClass, String enclosingFile, int observed) {
        String cls = m.declaringClass != null ? m.declaringClass : enclosingClass;
        String file = m.declaringClass != null ? simpleSourceFile(m.declaringClass) : enclosingFile;
        return new Frame(cls, m.originalName, file, m.mapLine(observed));
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

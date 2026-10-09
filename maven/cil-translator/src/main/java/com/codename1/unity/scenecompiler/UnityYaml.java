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
package com.codename1.unity.scenecompiler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Reads the subset of YAML that Unity writes for scenes, as its manual
/// describes the format: a stream of documents, each introduced by
/// `--- !u!<class id> &<file id>` and holding one mapping whose single key
/// is the object's type.
///
/// It is not a YAML parser. Unity's serializer emits block mappings, block
/// sequences whose dashes sit at the indentation of their key -- and, for a
/// list of lists such as a polygon collider's paths, a sequence that starts
/// on the dash of another -- one-line flow mappings such as `{x: 0, y: 1}`,
/// and plain or quoted scalars, a quoted one possibly folded over several
/// lines. That is all this reads.
final class UnityYaml {
    /// One object of a scene file.
    static final class Document {
        int classId;
        long fileId;
        /// A placeholder for an object that lives in a prefab: the scene
        /// file holds only what it overrides.
        boolean stripped;
        String type;
        Map<String, Object> properties;
        /// For an object a prefab instance brought in: every name it has
        /// gone by, each the GUID of a prefab and its id in that file. An
        /// override names its object by one of them.
        final List<String> sources = new ArrayList<String>();
    }

    private final List<String> lines = new ArrayList<String>();
    private int at;

    private UnityYaml() {
    }

    static List<Document> parse(String text) {
        UnityYaml y = new UnityYaml();
        List<Document> out = new ArrayList<Document>();
        Document current = null;
        for (String raw : text.split("\n")) {
            String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            if (line.startsWith("---")) {
                y.finish(current);
                current = header(line);
                if (current != null) {
                    out.add(current);
                }
                y.lines.clear();
            } else if (current != null && !line.startsWith("%")) {
                // Blank lines are kept: inside a quoted string they mean a
                // line break.
                y.lines.add(line);
            }
        }
        y.finish(current);
        return out;
    }

    /// A `.meta` file: the same notation with no document header, one
    /// mapping from top to bottom.
    static Map<String, Object> parseMeta(String text) {
        UnityYaml y = new UnityYaml();
        for (String raw : text.split("\n")) {
            String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            if (!line.startsWith("%") && !line.startsWith("---")) {
                y.lines.add(line);
            }
        }
        return y.mapping(0);
    }

    /// `--- !u!114 &105`, possibly followed by ` stripped`.
    private static Document header(String line) {
        int tag = line.indexOf("!u!");
        int amp = line.indexOf('&');
        if (tag < 0 || amp < tag) {
            return null;
        }
        Document d = new Document();
        d.classId = Integer.parseInt(line.substring(tag + 3, amp).trim());
        String id = line.substring(amp + 1).trim();
        int space = id.indexOf(' ');
        d.fileId = Long.parseLong(space < 0 ? id : id.substring(0, space));
        d.stripped = space > 0 && "stripped".equals(id.substring(space).trim());
        return d;
    }

    @SuppressWarnings("unchecked")
    private void finish(Document d) {
        at = 0;
        skipBlank();
        if (d == null || at >= lines.size()) {
            return;
        }
        Map<String, Object> root = mapping(0);
        Iterator<Map.Entry<String, Object>> first = root.entrySet().iterator();
        if (first.hasNext()) {
            Map.Entry<String, Object> e = first.next();
            d.type = e.getKey();
            d.properties = e.getValue() instanceof Map ? (Map<String, Object>) e.getValue()
                    : new LinkedHashMap<String, Object>();
        }
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private void skipBlank() {
        while (at < lines.size() && lines.get(at).trim().length() == 0) {
            at++;
        }
    }

    /// Whether a scalar opens a quoted string that this line does not
    /// close. In single quotes a quote is written twice; in double quotes
    /// it is escaped with a backslash.
    private static boolean unclosed(String text) {
        if (text.length() == 0) {
            return false;
        }
        char q = text.charAt(0);
        if (q != '"' && q != '\'') {
            return false;
        }
        for (int i = 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (q == '"' && c == '\\') {
                i++;
            } else if (c == q) {
                if (q == '\'' && i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                    i++;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /// Joins the lines of a quoted string that runs over several, the way
    /// YAML folds them: a line break becomes a space, an empty line a line
    /// break, and a double-quoted line that ends in a backslash continues
    /// with nothing between.
    private String folded(String first) {
        StringBuilder sb = new StringBuilder(first);
        boolean glue = first.charAt(0) == '"' && first.endsWith("\\") && !first.endsWith("\\\\");
        if (glue) {
            sb.setLength(sb.length() - 1);
        }
        int blanks = 0;
        while (at < lines.size()) {
            String next = lines.get(at++).trim();
            if (next.length() == 0) {
                blanks++;
                continue;
            }
            if (blanks > 0) {
                for (int i = 0; i < blanks; i++) {
                    sb.append(first.charAt(0) == '"' ? "\\n" : "\n");
                }
            } else if (!glue) {
                sb.append(' ');
            }
            blanks = 0;
            glue = first.charAt(0) == '"' && next.endsWith("\\") && !next.endsWith("\\\\");
            sb.append(glue ? next.substring(0, next.length() - 1) : next);
            if (!unclosed(sb.toString())) {
                break;
            }
        }
        return sb.toString();
    }

    /// A flow mapping in full. Unity breaks one that would pass eighty
    /// columns -- a reference with a long id and a GUID ends up with its
    /// `type: 3}` on a line of its own -- and the rest is taken from the
    /// lines that follow, up to the closing brace.
    private String wholeFlow(String first) {
        if (!first.startsWith("{") || first.endsWith("}")) {
            return first;
        }
        StringBuilder sb = new StringBuilder(first);
        while (at < lines.size()) {
            String next = lines.get(at++).trim();
            sb.append(' ').append(next);
            if (next.endsWith("}")) {
                break;
            }
        }
        return sb.toString();
    }

    /// A plain scalar in full. One that would pass eighty columns is
    /// broken at a space and goes on, indented further than its key, on
    /// the lines that follow: the assembly-qualified type name of an event
    /// is the usual one, in files older editors wrote. Read as the end of
    /// the mapping, such a line would end every mapping around it too, and
    /// the rest of the object would be lost without a word.
    private String continued(String first, int indent) {
        if (first.length() == 0 || "{['\"|>&*!".indexOf(first.charAt(0)) >= 0) {
            return first;
        }
        StringBuilder sb = null;
        while (at < lines.size()) {
            String line = lines.get(at);
            if (line.trim().length() == 0 || indentOf(line) <= indent) {
                break;
            }
            if (sb == null) {
                sb = new StringBuilder(first);
            }
            sb.append(' ').append(line.trim());
            at++;
        }
        return sb == null ? first : sb.toString();
    }

    private Map<String, Object> mapping(int indent) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        while (true) {
            skipBlank();
            if (at >= lines.size()) {
                break;
            }
            String line = lines.get(at);
            int in = indentOf(line);
            String body = line.substring(in);
            if (in != indent || body.startsWith("- ") || "-".equals(body)) {
                break;
            }
            int colon = keyEnd(body);
            if (colon < 0) {
                at++;
                continue;
            }
            String key = body.substring(0, colon).trim();
            String rest = body.substring(colon + 1).trim();
            at++;
            if (unclosed(rest)) {
                rest = folded(rest);
            }
            rest = continued(wholeFlow(rest), indent);
            map.put(key, rest.length() > 0 ? scalarOrFlow(rest) : nested(indent));
        }
        return map;
    }

    /// The value of a key that had nothing after its colon.
    private Object nested(int indent) {
        skipBlank();
        if (at >= lines.size()) {
            return "";
        }
        String next = lines.get(at);
        int in = indentOf(next);
        String body = next.substring(in);
        if (in >= indent && (body.startsWith("- ") || "-".equals(body))) {
            return sequence(in);
        }
        return in > indent ? mapping(in) : "";
    }

    private List<Object> sequence(int indent) {
        List<Object> list = new ArrayList<Object>();
        while (true) {
            skipBlank();
            if (at >= lines.size()) {
                break;
            }
            String line = lines.get(at);
            int in = indentOf(line);
            String body = line.substring(in);
            if (in != indent || !(body.startsWith("- ") || "-".equals(body))) {
                break;
            }
            String item = body.length() > 2 ? body.substring(2).trim() : "";
            if (item.startsWith("- ") || "-".equals(item)) {
                // `- - x`: a sequence whose first item shares the dash's
                // line. Read it as if the outer dash were indentation.
                lines.set(at, line.substring(0, in) + "  " + body.substring(2));
                list.add(sequence(in + 2));
                continue;
            }
            if (item.startsWith("{") || keyEnd(item) < 0) {
                at++;
                list.add(scalarOrFlow(continued(wholeFlow(item), in)));
                continue;
            }
            // `- key: value`: a mapping whose first key shares the dash's line.
            // Read it as if the dash were indentation.
            lines.set(at, line.substring(0, in) + "  " + body.substring(2));
            list.add(mapping(in + 2));
        }
        return list;
    }

    /// Position of the colon that ends a key, or -1 for a line that is not
    /// `key: value`.
    private static int keyEnd(String body) {
        if (body.startsWith("{") || body.startsWith("'") || body.startsWith("\"")) {
            return -1;
        }
        for (int i = 0; i < body.length(); i++) {
            if (body.charAt(i) == ':' && (i + 1 == body.length() || body.charAt(i + 1) == ' ')) {
                return i;
            }
        }
        return -1;
    }

    private static Object scalarOrFlow(String text) {
        if ("[]".equals(text)) {
            return new ArrayList<Object>();
        }
        if (text.startsWith("{") && text.endsWith("}")) {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            String inner = text.substring(1, text.length() - 1);
            int depth = 0;
            int start = 0;
            for (int i = 0; i <= inner.length(); i++) {
                char c = i < inner.length() ? inner.charAt(i) : ',';
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    String part = inner.substring(start, i).trim();
                    start = i + 1;
                    int colon = part.indexOf(':');
                    if (colon > 0) {
                        map.put(part.substring(0, colon).trim(), scalarOrFlow(part.substring(colon + 1).trim()));
                    }
                }
            }
            return map;
        }
        if (text.length() >= 2 && text.charAt(0) == '\'' && text.charAt(text.length() - 1) == '\'') {
            return text.substring(1, text.length() - 1).replace("''", "'");
        }
        if (text.length() >= 2 && text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"') {
            return unescape(text.substring(1, text.length() - 1));
        }
        return text;
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                int digits = n == 'u' ? 4 : n == 'x' ? 2 : 0;
                if (digits > 0 && i + digits < s.length()) {
                    try {
                        sb.append((char) Integer.parseInt(s.substring(i + 1, i + 1 + digits), 16));
                        i += digits;
                        continue;
                    } catch (NumberFormatException e) {
                        // Not an escape after all; keep the letter.
                    }
                }
                sb.append(n == 'n' ? '\n' : n == 't' ? '\t' : n == 'r' ? '\r' : n);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// GraphQL SDL (Schema Definition Language) model plus a hand-written
/// recursive-descent parser. Mirrors the `ProtoParser` approach in
/// [GenerateGrpcMojo] -- no third-party dependency, tolerant of the
/// constructs `cn1:generate-graphql` does not need (descriptions,
/// directives) by skipping them.
///
/// Supported: `type`, `input`, `interface`, `enum`, `union`, `scalar`,
/// and the root `schema { query/mutation/subscription }` block.
/// `extend`, custom `directive` definitions and descriptions are
/// skipped.
final class GraphQLSchemaModel {

    /// A reference to a type with its list / non-null wrappers, e.g.
    /// `[Episode!]!`. Either `name` is set (a named type) or `list` is
    /// true and `element` holds the inner reference.
    static final class TypeRef {
        boolean list;
        boolean nonNull;
        String name;       // when !list
        TypeRef element;   // when list

        /// The innermost named type, unwrapping all list levels.
        String baseName() {
            TypeRef t = this;
            while (t.list) {
                t = t.element;
            }
            return t.name;
        }

        boolean isList() {
            return list;
        }
    }

    static final class ArgDef {
        String name;
        TypeRef type;
    }

    static final class FieldDef {
        String name;
        TypeRef type;
        final List<ArgDef> args = new ArrayList<ArgDef>();
    }

    /// An object, interface or input type. Input types reuse this with
    /// `input == true` (their fields never have args).
    static final class ObjectTypeDef {
        String name;
        boolean input;
        final List<FieldDef> fields = new ArrayList<FieldDef>();
    }

    static final class EnumDef {
        String name;
        final List<String> values = new ArrayList<String>();
    }

    static final class Schema {
        final Map<String, ObjectTypeDef> objects = new LinkedHashMap<String, ObjectTypeDef>();
        final Map<String, ObjectTypeDef> inputs = new LinkedHashMap<String, ObjectTypeDef>();
        final Map<String, EnumDef> enums = new LinkedHashMap<String, EnumDef>();
        final Map<String, List<String>> unions = new LinkedHashMap<String, List<String>>();
        final java.util.Set<String> scalars = new java.util.LinkedHashSet<String>();
        String queryType = "Query";
        String mutationType = "Mutation";
        String subscriptionType = "Subscription";

        ObjectTypeDef object(String name) {
            return objects.get(name);
        }

        boolean isEnum(String name) {
            return enums.containsKey(name);
        }

        boolean isInput(String name) {
            return inputs.containsKey(name);
        }

        boolean isObject(String name) {
            return objects.containsKey(name);
        }
    }

    static final class ParseException extends RuntimeException {
        ParseException(String msg) {
            super(msg);
        }
    }

    private GraphQLSchemaModel() {
    }

    // ----------------------------------------------------------------
    // Parser
    // ----------------------------------------------------------------

    static final class Parser {
        private final String src;
        private final String file;
        private int pos;
        private int line = 1;

        Parser(String src, String file) {
            this.src = src;
            this.file = file;
        }

        Schema parse() {
            Schema schema = new Schema();
            while (true) {
                skipIgnored();
                if (pos >= src.length()) break;
                skipDescription();
                skipDirectivesAndKeywords();
                skipIgnored();
                if (pos >= src.length()) break;
                if (peekWord("schema")) {
                    parseSchemaBlock(schema);
                } else if (peekWord("type")) {
                    ObjectTypeDef o = parseObject("type");
                    schema.objects.put(o.name, o);
                } else if (peekWord("input")) {
                    ObjectTypeDef o = parseObject("input");
                    o.input = true;
                    schema.inputs.put(o.name, o);
                } else if (peekWord("interface")) {
                    ObjectTypeDef o = parseObject("interface");
                    schema.objects.put(o.name, o);
                } else if (peekWord("enum")) {
                    EnumDef e = parseEnum();
                    schema.enums.put(e.name, e);
                } else if (peekWord("union")) {
                    parseUnion(schema);
                } else if (peekWord("scalar")) {
                    consumeWord("scalar");
                    String n = readName();
                    schema.scalars.add(n);
                    skipDirectives();
                } else if (peekWord("directive")) {
                    skipDirectiveDefinition();
                } else if (peekWord("extend")) {
                    consumeWord("extend");
                    // Skip the extend keyword and re-loop to parse the
                    // following definition as if standalone (fields are
                    // merged into the map; good enough for codegen).
                } else {
                    throw err("Unexpected token at top level: '" + preview() + "'");
                }
            }
            return schema;
        }

        private void parseSchemaBlock(Schema schema) {
            consumeWord("schema");
            skipDirectives();
            expect('{');
            while (true) {
                skipIgnored();
                if (peekChar('}')) { pos++; break; }
                String role = readName();
                expect(':');
                String typeName = readName();
                if ("query".equals(role)) schema.queryType = typeName;
                else if ("mutation".equals(role)) schema.mutationType = typeName;
                else if ("subscription".equals(role)) schema.subscriptionType = typeName;
            }
        }

        private ObjectTypeDef parseObject(String keyword) {
            consumeWord(keyword);
            ObjectTypeDef o = new ObjectTypeDef();
            o.name = readName();
            // Optional `implements A & B`.
            skipIgnored();
            if (peekWord("implements")) {
                consumeWord("implements");
                while (true) {
                    skipIgnored();
                    if (peekChar('&')) { pos++; continue; }
                    if (peekChar('{') || peekChar('@')) break;
                    if (!isNameStart(peekRaw())) break;
                    readName(); // discard implemented interface name
                }
            }
            skipDirectives();
            skipIgnored();
            if (!peekChar('{')) {
                // Type with no field block (rare but legal).
                return o;
            }
            expect('{');
            while (true) {
                skipIgnored();
                if (peekChar('}')) { pos++; break; }
                skipDescription();
                FieldDef f = parseField();
                o.fields.add(f);
            }
            return o;
        }

        private FieldDef parseField() {
            FieldDef f = new FieldDef();
            f.name = readName();
            skipIgnored();
            if (peekChar('(')) {
                parseArgDefs(f.args);
            }
            expect(':');
            f.type = parseType();
            skipDirectives();
            return f;
        }

        private void parseArgDefs(List<ArgDef> out) {
            expect('(');
            while (true) {
                skipIgnored();
                if (peekChar(')')) { pos++; break; }
                skipDescription();
                ArgDef a = new ArgDef();
                a.name = readName();
                expect(':');
                a.type = parseType();
                // Optional default value.
                skipIgnored();
                if (peekChar('=')) {
                    pos++;
                    skipValue();
                }
                skipDirectives();
                skipIgnored();
                if (peekChar(',')) pos++;
                out.add(a);
            }
        }

        private TypeRef parseType() {
            skipIgnored();
            TypeRef t = new TypeRef();
            if (peekChar('[')) {
                pos++;
                t.list = true;
                t.element = parseType();
                skipIgnored();
                expect(']');
            } else {
                t.name = readName();
            }
            skipIgnored();
            if (peekChar('!')) {
                pos++;
                t.nonNull = true;
            }
            return t;
        }

        private EnumDef parseEnum() {
            consumeWord("enum");
            EnumDef e = new EnumDef();
            e.name = readName();
            skipDirectives();
            expect('{');
            while (true) {
                skipIgnored();
                if (peekChar('}')) { pos++; break; }
                skipDescription();
                String v = readName();
                e.values.add(v);
                skipDirectives();
            }
            return e;
        }

        private void parseUnion(Schema schema) {
            consumeWord("union");
            String name = readName();
            skipDirectives();
            expect('=');
            List<String> members = new ArrayList<String>();
            while (true) {
                skipIgnored();
                if (peekChar('|')) { pos++; continue; }
                if (!isNameStart(peekRaw())) break;
                members.add(readName());
                skipIgnored();
                if (!peekChar('|')) break;
            }
            schema.unions.put(name, members);
        }

        // -- skipping helpers ----------------------------------------

        /// A no-op slot kept so the top-level loop reads cleanly; the
        /// real skipping happens in [#skipIgnored()] and
        /// [#skipDescription()].
        private void skipDirectivesAndKeywords() {
        }

        private void skipDirectiveDefinition() {
            consumeWord("directive");
            // `directive @name(args) on LOC | LOC` -- skip to the next
            // top-level definition by scanning until a newline-led
            // keyword. Simplest robust approach: consume through the
            // `on` location list (identifiers, `|`, `@`, parens).
            while (pos < src.length()) {
                skipIgnored();
                if (pos >= src.length()) return;
                char c = peekRaw();
                if (c == '@' ) { pos++; continue; }
                if (c == '(') { skipBalanced('(', ')'); continue; }
                if (c == '|') { pos++; continue; }
                if (isNameStart(c)) {
                    String w = readName();
                    if (isTopLevelKeyword(w)) {
                        // We overran into the next definition; rewind.
                        pos -= w.length();
                        return;
                    }
                    continue;
                }
                return;
            }
        }

        private boolean isTopLevelKeyword(String w) {
            return "type".equals(w) || "input".equals(w) || "interface".equals(w)
                    || "enum".equals(w) || "union".equals(w) || "scalar".equals(w)
                    || "schema".equals(w) || "directive".equals(w) || "extend".equals(w);
        }

        private void skipDescription() {
            skipIgnored();
            if (pos >= src.length()) return;
            if (peekChar('"')) {
                if (src.regionMatches(pos, "\"\"\"", 0, 3)) {
                    pos += 3;
                    int idx = src.indexOf("\"\"\"", pos);
                    pos = idx < 0 ? src.length() : idx + 3;
                } else {
                    pos++;
                    while (pos < src.length() && src.charAt(pos) != '"') {
                        if (src.charAt(pos) == '\\') pos++;
                        pos++;
                    }
                    if (pos < src.length()) pos++;
                }
            }
        }

        private void skipDirectives() {
            while (true) {
                skipIgnored();
                if (peekChar('@')) {
                    pos++;
                    readName();
                    skipIgnored();
                    if (peekChar('(')) {
                        skipBalanced('(', ')');
                    }
                } else {
                    break;
                }
            }
        }

        /// Skips a GraphQL value (used for default values): scalars,
        /// strings, enums, lists `[...]` and objects `{...}`.
        private void skipValue() {
            skipIgnored();
            if (pos >= src.length()) return;
            char c = peekRaw();
            if (c == '[') { skipBalanced('[', ']'); return; }
            if (c == '{') { skipBalanced('{', '}'); return; }
            if (c == '"') { skipDescription(); return; }
            // Scalar / enum / boolean / null token.
            while (pos < src.length()) {
                char d = src.charAt(pos);
                if (Character.isWhitespace(d) || d == ',' || d == ')' || d == ']'
                        || d == '}' || d == '@') break;
                pos++;
            }
        }

        private void skipBalanced(char open, char close) {
            skipIgnored();
            if (!peekChar(open)) return;
            int depth = 0;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == '"') { skipDescription(); continue; }
                if (c == open) depth++;
                else if (c == close) { depth--; pos++; if (depth == 0) return; else continue; }
                if (c == '\n') line++;
                pos++;
            }
        }

        // -- token primitives ----------------------------------------

        private boolean peekWord(String kw) {
            skipIgnored();
            int n = kw.length();
            if (pos + n > src.length()) return false;
            if (!src.regionMatches(pos, kw, 0, n)) return false;
            if (pos + n < src.length() && isNamePart(src.charAt(pos + n))) return false;
            return true;
        }

        private void consumeWord(String kw) {
            if (!peekWord(kw)) {
                throw err("Expected '" + kw + "' but got '" + preview() + "'");
            }
            pos += kw.length();
        }

        private boolean peekChar(char c) {
            skipIgnored();
            return pos < src.length() && src.charAt(pos) == c;
        }

        private char peekRaw() {
            return pos < src.length() ? src.charAt(pos) : '\0';
        }

        private void expect(char c) {
            skipIgnored();
            if (pos >= src.length() || src.charAt(pos) != c) {
                throw err("Expected '" + c + "' but got '" + preview() + "'");
            }
            pos++;
        }

        private String readName() {
            skipIgnored();
            int start = pos;
            if (pos >= src.length() || !isNameStart(src.charAt(pos))) {
                throw err("Expected name, got '" + preview() + "'");
            }
            while (pos < src.length() && isNamePart(src.charAt(pos))) pos++;
            return src.substring(start, pos);
        }

        private void skipIgnored() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == '\n') { line++; pos++; continue; }
                if (Character.isWhitespace(c) || c == ',') { pos++; continue; }
                if (c == '#') {
                    while (pos < src.length() && src.charAt(pos) != '\n') pos++;
                    continue;
                }
                return;
            }
        }

        private static boolean isNameStart(char c) {
            return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }

        private static boolean isNamePart(char c) {
            return isNameStart(c) || (c >= '0' && c <= '9');
        }

        private String preview() {
            if (pos >= src.length()) return "<EOF>";
            return src.substring(pos, Math.min(pos + 16, src.length()));
        }

        private ParseException err(String msg) {
            return new ParseException(file + ":" + line + ": " + msg);
        }
    }
}

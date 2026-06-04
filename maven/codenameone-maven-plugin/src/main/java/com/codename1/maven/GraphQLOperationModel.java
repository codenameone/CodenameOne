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

import com.codename1.maven.GraphQLSchemaModel.TypeRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// GraphQL executable-document model (queries, mutations,
/// subscriptions, fragments) plus a hand-written parser. Used by
/// [GenerateGraphQLMojo]'s operations mode to synthesise precise typed
/// response models from the selection sets a client actually requests.
///
/// The parser captures each definition's raw source slice so the
/// generator can emit the operation document verbatim (minified, with
/// referenced fragment definitions appended) into the `@Query` /
/// `@Mutation` / `@Subscription` annotation.
final class GraphQLOperationModel {

    static final String OP_QUERY = "query";
    static final String OP_MUTATION = "mutation";
    static final String OP_SUBSCRIPTION = "subscription";

    /// One member of a selection set.
    abstract static class Selection {
    }

    static final class Field extends Selection {
        String alias;          // null when unaliased
        String name;
        final List<Selection> selections = new ArrayList<Selection>();

        /// The response key this field appears under (alias if present).
        String responseKey() {
            return alias != null ? alias : name;
        }
    }

    static final class FragmentSpread extends Selection {
        String fragmentName;
    }

    static final class InlineFragment extends Selection {
        String typeCondition;  // null for `... { ... }`
        final List<Selection> selections = new ArrayList<Selection>();
    }

    static final class VarDef {
        String name;           // without leading '$'
        TypeRef type;
    }

    static final class OperationDef {
        String kind;           // OP_QUERY / OP_MUTATION / OP_SUBSCRIPTION
        String name;           // null for anonymous operations
        final List<VarDef> vars = new ArrayList<VarDef>();
        final List<Selection> selections = new ArrayList<Selection>();
        final Set<String> directSpreads = new LinkedHashSet<String>();
        String rawText;
    }

    static final class FragmentDef {
        String name;
        String typeCondition;
        final List<Selection> selections = new ArrayList<Selection>();
        final Set<String> directSpreads = new LinkedHashSet<String>();
        String rawText;
    }

    static final class Document {
        final List<OperationDef> operations = new ArrayList<OperationDef>();
        final Map<String, FragmentDef> fragments = new LinkedHashMap<String, FragmentDef>();
    }

    static final class ParseException extends RuntimeException {
        ParseException(String msg) {
            super(msg);
        }
    }

    private GraphQLOperationModel() {
    }

    // ----------------------------------------------------------------
    // Parser
    // ----------------------------------------------------------------

    static final class Parser {
        private final String src;
        private final String file;
        private int pos;
        private int line = 1;
        private Set<String> currentSpreads;

        Parser(String src, String file) {
            this.src = src;
            this.file = file;
        }

        Document parse() {
            Document doc = new Document();
            while (true) {
                skipIgnored();
                if (pos >= src.length()) break;
                if (peekWord("fragment")) {
                    FragmentDef f = parseFragment();
                    doc.fragments.put(f.name, f);
                } else if (peekWord(OP_QUERY) || peekWord(OP_MUTATION) || peekWord(OP_SUBSCRIPTION)) {
                    doc.operations.add(parseOperation());
                } else if (peekChar('{')) {
                    doc.operations.add(parseAnonymousOperation());
                } else {
                    throw err("Unexpected token in operation document: '" + preview() + "'");
                }
            }
            return doc;
        }

        private OperationDef parseOperation() {
            int start = pos;
            OperationDef op = new OperationDef();
            currentSpreads = op.directSpreads;
            if (peekWord(OP_QUERY)) { consumeWord(OP_QUERY); op.kind = OP_QUERY; }
            else if (peekWord(OP_MUTATION)) { consumeWord(OP_MUTATION); op.kind = OP_MUTATION; }
            else { consumeWord(OP_SUBSCRIPTION); op.kind = OP_SUBSCRIPTION; }
            skipIgnored();
            if (isNameStart(peekRaw())) {
                op.name = readName();
            }
            skipIgnored();
            if (peekChar('(')) {
                parseVarDefs(op.vars);
            }
            skipDirectives();
            parseSelectionSet(op.selections);
            op.rawText = src.substring(start, pos);
            return op;
        }

        private OperationDef parseAnonymousOperation() {
            int start = pos;
            OperationDef op = new OperationDef();
            op.kind = OP_QUERY;
            currentSpreads = op.directSpreads;
            parseSelectionSet(op.selections);
            op.rawText = src.substring(start, pos);
            return op;
        }

        private FragmentDef parseFragment() {
            int start = pos;
            consumeWord("fragment");
            FragmentDef f = new FragmentDef();
            currentSpreads = f.directSpreads;
            f.name = readName();
            consumeWord("on");
            f.typeCondition = readName();
            skipDirectives();
            parseSelectionSet(f.selections);
            f.rawText = src.substring(start, pos);
            return f;
        }

        private void parseVarDefs(List<VarDef> out) {
            expect('(');
            while (true) {
                skipIgnored();
                if (peekChar(')')) { pos++; break; }
                expect('$');
                VarDef v = new VarDef();
                v.name = readName();
                expect(':');
                v.type = parseType();
                skipIgnored();
                if (peekChar('=')) {
                    pos++;
                    skipValue();
                }
                skipDirectives();
                out.add(v);
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

        private void parseSelectionSet(List<Selection> out) {
            expect('{');
            while (true) {
                skipIgnored();
                if (peekChar('}')) { pos++; break; }
                if (src.regionMatches(pos, "...", 0, 3)) {
                    pos += 3;
                    parseFragmentOrInline(out);
                } else {
                    out.add(parseFieldSelection());
                }
            }
        }

        private void parseFragmentOrInline(List<Selection> out) {
            skipIgnored();
            if (peekWord("on")) {
                consumeWord("on");
                InlineFragment inline = new InlineFragment();
                inline.typeCondition = readName();
                skipDirectives();
                parseSelectionSet(inline.selections);
                out.add(inline);
            } else if (peekChar('{')) {
                InlineFragment inline = new InlineFragment();
                parseSelectionSet(inline.selections);
                out.add(inline);
            } else if (peekChar('@')) {
                // `... @directive { ... }` inline fragment with no type
                // condition.
                skipDirectives();
                InlineFragment inline = new InlineFragment();
                parseSelectionSet(inline.selections);
                out.add(inline);
            } else {
                FragmentSpread fs = new FragmentSpread();
                fs.fragmentName = readName();
                skipDirectives();
                if (currentSpreads != null) currentSpreads.add(fs.fragmentName);
                out.add(fs);
            }
        }

        private Field parseFieldSelection() {
            Field f = new Field();
            String first = readName();
            skipIgnored();
            if (peekChar(':')) {
                pos++;
                f.alias = first;
                f.name = readName();
            } else {
                f.name = first;
            }
            skipIgnored();
            if (peekChar('(')) {
                skipBalanced('(', ')'); // arguments do not affect response typing
            }
            skipDirectives();
            skipIgnored();
            if (peekChar('{')) {
                parseSelectionSet(f.selections);
            }
            return f;
        }

        // -- skipping helpers ----------------------------------------

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

        private void skipValue() {
            skipIgnored();
            if (pos >= src.length()) return;
            char c = peekRaw();
            if (c == '[') { skipBalanced('[', ']'); return; }
            if (c == '{') { skipBalanced('{', '}'); return; }
            if (c == '"') { skipString(); return; }
            if (c == '$') { pos++; readName(); return; }
            while (pos < src.length()) {
                char d = src.charAt(pos);
                if (Character.isWhitespace(d) || d == ',' || d == ')' || d == ']'
                        || d == '}' || d == '@') break;
                pos++;
            }
        }

        private void skipString() {
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

        private void skipBalanced(char open, char close) {
            skipIgnored();
            if (!peekChar(open)) return;
            int depth = 0;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == '"') { skipString(); continue; }
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

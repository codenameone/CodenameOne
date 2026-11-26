/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The small subset of GraphQL the key API reads: one query or mutation of
/// nested field selections over the JSON the REST routes answer.
///
/// ```
/// { products(first: 5) { id title variants { sku priceCents } } }
/// query Recent { latest: orders(first: 2) { number totalCents } }
/// mutation($in: ProductInput!) { productCreate(input: $in) { id } }
/// ```
///
/// Read: the optional `query` or `mutation` keyword, name and variable
/// declarations (their types are not checked: a value is whatever JSON the
/// request's `variables` carries), fields, aliases (`a: b`) and arguments --
/// whole numbers, strings, booleans, null, enum words (read as strings),
/// lists, objects and `$variables`. Refused, with a GraphQL error:
/// subscriptions, fragments, directives, fractions, and a field the objects
/// do not have. There is no schema to introspect; the fields are the REST
/// resources' members, so the REST documentation is the schema.
///
/// Pure, so it is tested without a server.
public final class MiniGraphQl {
    /// How deep selections may nest, and how many fields a query may name.
    static final int MAX_DEPTH = 8;
    static final int MAX_FIELDS = 200;

    private MiniGraphQl() {
    }

    /// A field of a selection: its name, the key it is answered under, its
    /// integer arguments and its own selection.
    public static final class Field {
        public final String name;
        public final String alias;
        public final Map<String, Object> args = new LinkedHashMap<String, Object>();
        public final List<Field> children = new ArrayList<Field>();

        Field(String name, String alias) {
            this.name = name;
            this.alias = alias;
        }

        public int intArg(String arg, int fallback) {
            Object v = args.get(arg);
            return v instanceof Number ? ((Number) v).intValue() : fallback;
        }

        /// A string argument, or null when absent or not a string.
        public String stringArg(String arg) {
            Object v = args.get(arg);
            return v instanceof String ? (String) v : null;
        }
    }

    /// A parsed operation: its kind and its root selection.
    public static final class Operation {
        /// True for a `mutation`.
        public final boolean mutation;
        public final Field root;

        Operation(boolean mutation, Field root) {
            this.mutation = mutation;
            this.root = root;
        }
    }

    /// `{"errors":[{"message":...}]}`.
    public static Map<String, Object> error(String message) {
        return error(message, null);
    }

    /// `{"errors":[{"message":...,"extensions":{"code":...}}]}`, without the
    /// extensions when `code` is null.
    public static Map<String, Object> error(String message, String code) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("message", message);
        if (code != null) {
            Map<String, Object> ext = new LinkedHashMap<String, Object>();
            ext.put("code", code);
            m.put("extensions", ext);
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("errors", Collections.singletonList(m));
        return out;
    }

    /// The query's root selection; throws IllegalArgumentException, with a
    /// message for the client, for anything outside the subset, and for a
    /// mutation, which [#parseOperation] reads.
    public static Field parse(String query) {
        Operation op = parseOperation(query, null);
        if (op.mutation) {
            throw new IllegalArgumentException("Only queries are supported here");
        }
        return op.root;
    }

    /// A query or a mutation, its `$variables` read from `variables` (absent
    /// ones are null).
    public static Operation parseOperation(String query, Map variables) {
        Parser p = new Parser(query == null ? "" : query, variables);
        p.skip();
        if (p.word("subscription")) {
            throw new IllegalArgumentException("Subscriptions are not supported");
        }
        boolean mutation = false;
        if (p.word("query") || p.word("mutation")) {
            mutation = p.word("mutation");
            p.pos += mutation ? 8 : 5;
            p.skip();
            if (p.peek() != '{' && p.peek() != '(') {
                p.name();
                p.skip();
            }
            if (p.peek() == '(') {
                p.skipDeclarations();
                p.skip();
            }
        }
        Field root = new Field("", "");
        p.selection(root, 0);
        p.skip();
        if (p.pos < p.text.length()) {
            throw new IllegalArgumentException("Only one operation per request");
        }
        if (root.children.isEmpty()) {
            throw new IllegalArgumentException("Select at least one field");
        }
        return new Operation(mutation, root);
    }

    /// `value` cut down to the fields `selection` asks for; lists are
    /// projected element by element.
    public static Object project(Object value, Field selection) {
        if (selection.children.isEmpty()) {
            return value;
        }
        if (value instanceof List) {
            List list = (List) value;
            List<Object> out = new ArrayList<Object>();
            for (int iter = 0; iter < list.size(); iter++) {
                out.add(project(list.get(iter), selection));
            }
            return out;
        }
        if (!(value instanceof Map)) {
            return null;
        }
        Map source = (Map) value;
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (int iter = 0; iter < selection.children.size(); iter++) {
            Field f = selection.children.get(iter);
            if (!source.containsKey(f.name)) {
                throw new IllegalArgumentException("No field " + f.name);
            }
            out.put(f.alias, project(source.get(f.name), f));
        }
        return out;
    }

    private static final class Parser {
        final String text;
        final Map variables;
        int pos;
        int fields;

        Parser(String text, Map variables) {
            this.text = text;
            this.variables = variables;
        }

        /// Steps over `($a: T!, $b: [T] = 1)`; the types are not read.
        void skipDeclarations() {
            int depth = 0;
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == '"') {
                    string();
                    continue;
                }
                pos++;
                if (c == '(') {
                    depth++;
                } else if (c == ')' && --depth == 0) {
                    return;
                }
            }
            throw new IllegalArgumentException("Unclosed variable declarations");
        }

        char peek() {
            return pos < text.length() ? text.charAt(pos) : '\0';
        }

        void skip() {
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == '#') {
                    while (pos < text.length() && text.charAt(pos) != '\n') {
                        pos++;
                    }
                } else if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == ',') {
                    pos++;
                } else {
                    return;
                }
            }
        }

        boolean word(String w) {
            if (!text.startsWith(w, pos)) {
                return false;
            }
            int end = pos + w.length();
            return end >= text.length() || !isNameChar(text.charAt(end));
        }

        static boolean isNameChar(char c) {
            return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9');
        }

        String name() {
            int start = pos;
            while (pos < text.length() && isNameChar(text.charAt(pos))) {
                pos++;
            }
            if (start == pos || (text.charAt(start) >= '0' && text.charAt(start) <= '9')) {
                throw new IllegalArgumentException("Expected a name at " + start);
            }
            return text.substring(start, pos);
        }

        void expect(char c) {
            skip();
            if (peek() != c) {
                throw new IllegalArgumentException("Expected '" + c + "' at " + pos);
            }
            pos++;
        }

        void selection(Field parent, int depth) {
            if (depth >= MAX_DEPTH) {
                throw new IllegalArgumentException("The query nests too deeply");
            }
            expect('{');
            while (true) {
                skip();
                char c = peek();
                if (c == '}') {
                    pos++;
                    return;
                }
                if (c == '\0') {
                    throw new IllegalArgumentException("Unclosed selection");
                }
                if (c == '.' || c == '@' || c == '$') {
                    throw new IllegalArgumentException("Fragments and directives "
                            + "are not supported");
                }
                if (++fields > MAX_FIELDS) {
                    throw new IllegalArgumentException("The query names too many fields");
                }
                String first = name();
                skip();
                Field f;
                if (peek() == ':') {
                    pos++;
                    skip();
                    f = new Field(name(), first);
                    skip();
                } else {
                    f = new Field(first, first);
                }
                if (peek() == '(') {
                    pos++;
                    arguments(f);
                    skip();
                }
                if (peek() == '{') {
                    selection(f, depth + 1);
                }
                parent.children.add(f);
            }
        }

        void arguments(Field f) {
            while (true) {
                skip();
                if (peek() == ')') {
                    pos++;
                    return;
                }
                String arg = name();
                expect(':');
                f.args.put(arg, value(0));
            }
        }

        /// One argument value.
        Object value(int depth) {
            if (depth >= MAX_DEPTH) {
                throw new IllegalArgumentException("An argument nests too deeply");
            }
            skip();
            char c = peek();
            if (c == '$') {
                pos++;
                String v = name();
                return variables == null ? null : variables.get(v);
            }
            if (c == '"') {
                return string();
            }
            if (c == '[') {
                pos++;
                List<Object> list = new ArrayList<Object>();
                while (true) {
                    skip();
                    if (peek() == ']') {
                        pos++;
                        return list;
                    }
                    if (peek() == '\0') {
                        throw new IllegalArgumentException("Unclosed list");
                    }
                    list.add(value(depth + 1));
                }
            }
            if (c == '{') {
                pos++;
                Map<String, Object> object = new LinkedHashMap<String, Object>();
                while (true) {
                    skip();
                    if (peek() == '}') {
                        pos++;
                        return object;
                    }
                    String key = name();
                    expect(':');
                    object.put(key, value(depth + 1));
                }
            }
            if (c == '-' || (c >= '0' && c <= '9')) {
                int start = pos;
                pos++;
                while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9') {
                    pos++;
                }
                String digits = text.substring(start, pos);
                if ("-".equals(digits) || digits.length() > 18 || peek() == '.'
                        || peek() == 'e' || peek() == 'E') {
                    throw new IllegalArgumentException("Numbers are whole and under 18 digits");
                }
                long n = Long.parseLong(digits);
                if (n >= Integer.MIN_VALUE && n <= Integer.MAX_VALUE) {
                    return Integer.valueOf((int) n);
                }
                return Long.valueOf(n);
            }
            String word = name();
            if ("true".equals(word)) {
                return Boolean.TRUE;
            }
            if ("false".equals(word)) {
                return Boolean.FALSE;
            }
            if ("null".equals(word)) {
                return null;
            }
            return word;
        }

        /// A quoted string, with JSON's escapes.
        String string() {
            pos++;
            StringBuilder b = new StringBuilder();
            while (pos < text.length()) {
                char c = text.charAt(pos++);
                if (c == '"') {
                    return b.toString();
                }
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                if (pos >= text.length()) {
                    break;
                }
                char e = text.charAt(pos++);
                if (e == 'n') {
                    b.append('\n');
                } else if (e == 't') {
                    b.append('\t');
                } else if (e == 'r') {
                    b.append('\r');
                } else if (e == 'u' && pos + 4 <= text.length()) {
                    int code = 0;
                    for (int iter = 0; iter < 4; iter++) {
                        int d = Character.digit(text.charAt(pos++), 16);
                        if (d < 0) {
                            throw new IllegalArgumentException("A bad escape in a string");
                        }
                        code = code * 16 + d;
                    }
                    b.append((char) code);
                } else {
                    b.append(e);
                }
            }
            throw new IllegalArgumentException("Unclosed string");
        }
    }
}

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
package com.codename1.backend.graphql;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The GraphQL lexer and recursive-descent parser, for both an executable
/// document (operations and fragments) and the schema definition language.
///
/// Hand written rather than generated, and free of regular expressions, because
/// the backend is translated by ParparVM, whose class library has no
/// `java.util.regex`. Values parse to plain Java objects: `String`, `Long`,
/// `Double`, `Boolean`, `null`, `List` and `LinkedHashMap`, with [Variable] and
/// [EnumLiteral] for the two kinds that have no plain equivalent. An explicit
/// `null` is a Java null stored under its key, so a map's `containsKey` tells
/// it apart from an argument that was left out.
final class Parser {
    // Token kinds.
    static final int EOF = 0;
    static final int PUNCT = 1;
    static final int NAME = 2;
    static final int INT = 3;
    static final int FLOAT = 4;
    static final int STRING = 5;

    private final String src;
    private int pos;
    /// How deeply selection sets, values and type references are nested here.
    /// Bounded so a short document of `{a{a{a...` or `[[[...` is an error
    /// rather than a stack overflow: the parser runs before any depth rule.
    private int nesting;
    static final int MAX_NESTING = 128;
    private int line = 1;
    private int lineStart;

    private int kind;
    private String text;
    private int tokLine;
    private int tokColumn;
    /// The description string read before a definition, consumed by the next one.
    private String description;

    Parser(String src) {
        this.src = src == null ? "" : src;
        advance();
    }

    // ------------------------------------------------------------------ lexer

    private void advance() {
        skipIgnored();
        tokLine = line;
        tokColumn = pos - lineStart + 1;
        if (pos >= src.length()) {
            kind = EOF;
            text = "<EOF>";
            return;
        }
        char c = src.charAt(pos);
        if (c == '.' && src.startsWith("...", pos)) {
            pos += 3;
            kind = PUNCT;
            text = "...";
            return;
        }
        if ("!$&()[]{}:=@|".indexOf(c) >= 0) {
            pos++;
            kind = PUNCT;
            text = String.valueOf(c);
            return;
        }
        if (c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            int start = pos;
            while (pos < src.length() && isNameChar(src.charAt(pos))) {
                pos++;
            }
            kind = NAME;
            text = src.substring(start, pos);
            return;
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            readNumber();
            return;
        }
        if (c == '"') {
            if (src.startsWith("\"\"\"", pos)) {
                readBlockString();
            } else {
                readString();
            }
            return;
        }
        throw error("Unexpected character '" + c + "'");
    }

    private static boolean isNameChar(char c) {
        return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9');
    }

    private void skipIgnored() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\n') {
                pos++;
                line++;
                lineStart = pos;
            } else if (c == '\r') {
                pos++;
                if (pos < src.length() && src.charAt(pos) == '\n') {
                    pos++;
                }
                line++;
                lineStart = pos;
            } else if (c == ' ' || c == '\t' || c == ',' || c == 0xFEFF) {
                pos++;
            } else if (c == '#') {
                while (pos < src.length() && src.charAt(pos) != '\n'
                        && src.charAt(pos) != '\r') {
                    pos++;
                }
            } else {
                return;
            }
        }
    }

    private void readNumber() {
        int start = pos;
        boolean isFloat = false;
        if (src.charAt(pos) == '-') {
            pos++;
        }
        int digits = digits();
        if (digits == 0) {
            throw error("Invalid number");
        }
        if (digits > 1 && src.charAt(start + (src.charAt(start) == '-' ? 1 : 0)) == '0') {
            throw error("Invalid number, unexpected digit after 0");
        }
        if (pos < src.length() && src.charAt(pos) == '.') {
            isFloat = true;
            pos++;
            if (digits() == 0) {
                throw error("Invalid number, expected digit after '.'");
            }
        }
        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            isFloat = true;
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                pos++;
            }
            if (digits() == 0) {
                throw error("Invalid number, expected digit in exponent");
            }
        }
        if (pos < src.length() && (isNameChar(src.charAt(pos)) || src.charAt(pos) == '.')) {
            throw error("Invalid number, unexpected '" + src.charAt(pos) + "'");
        }
        kind = isFloat ? FLOAT : INT;
        text = src.substring(start, pos);
    }

    private int digits() {
        int n = 0;
        while (pos < src.length() && src.charAt(pos) >= '0' && src.charAt(pos) <= '9') {
            pos++;
            n++;
        }
        return n;
    }

    private void readString() {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw error("Unterminated string");
            }
            char c = src.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\n' || c == '\r') {
                throw error("Unterminated string");
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= src.length()) {
                throw error("Unterminated string");
            }
            char e = src.charAt(pos++);
            switch (e) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u': {
                    if (pos + 4 > src.length()) {
                        throw error("Invalid unicode escape");
                    }
                    int v = 0;
                    for (int i = 0; i < 4; i++) {
                        int d = Character.digit(src.charAt(pos + i), 16);
                        if (d < 0) {
                            throw error("Invalid unicode escape");
                        }
                        v = v * 16 + d;
                    }
                    pos += 4;
                    sb.append((char) v);
                    break;
                }
                default:
                    throw error("Invalid escape \\" + e);
            }
        }
        kind = STRING;
        text = sb.toString();
    }

    private void readBlockString() {
        pos += 3;
        StringBuilder raw = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw error("Unterminated block string");
            }
            if (src.startsWith("\"\"\"", pos)) {
                pos += 3;
                break;
            }
            if (src.startsWith("\\\"\"\"", pos)) {
                raw.append("\"\"\"");
                pos += 4;
                continue;
            }
            char c = src.charAt(pos++);
            if (c == '\n') {
                line++;
                lineStart = pos;
            }
            raw.append(c);
        }
        kind = STRING;
        text = blockStringValue(raw.toString());
    }

    /// The spec's BlockStringValue: common indentation and blank first and last
    /// lines removed.
    static String blockStringValue(String raw) {
        List<String> lines = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\n' || c == '\r') {
                lines.add(raw.substring(start, i));
                if (c == '\r' && i + 1 < raw.length() && raw.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;
            }
        }
        lines.add(raw.substring(start));
        int common = -1;
        for (int i = 1; i < lines.size(); i++) {
            String l = lines.get(i);
            int indent = 0;
            while (indent < l.length() && (l.charAt(indent) == ' ' || l.charAt(indent) == '\t')) {
                indent++;
            }
            if (indent < l.length() && (common < 0 || indent < common)) {
                common = indent;
            }
        }
        if (common > 0) {
            for (int i = 1; i < lines.size(); i++) {
                String l = lines.get(i);
                lines.set(i, l.length() >= common ? l.substring(common) : "");
            }
        }
        while (!lines.isEmpty() && isBlank(lines.get(0))) {
            lines.remove(0);
        }
        while (!lines.isEmpty() && isBlank(lines.get(lines.size() - 1))) {
            lines.remove(lines.size() - 1);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private static boolean isBlank(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ' && s.charAt(i) != '\t') {
                return false;
            }
        }
        return true;
    }

    GraphQLException error(String message) {
        return new GraphQLException("Syntax Error: " + message + " at line " + tokLine
                + ", column " + tokColumn, tokLine, tokColumn);
    }

    private boolean peek(String punct) {
        return kind == PUNCT && text.equals(punct);
    }

    private boolean peekName(String name) {
        return kind == NAME && text.equals(name);
    }

    private void expect(String punct) {
        if (!peek(punct)) {
            throw error("Expected '" + punct + "', found '" + text + "'");
        }
        advance();
    }

    private boolean skip(String punct) {
        if (peek(punct)) {
            advance();
            return true;
        }
        return false;
    }

    private String name() {
        if (kind != NAME) {
            throw error("Expected a name, found '" + text + "'");
        }
        String n = text;
        advance();
        return n;
    }

    private void keyword(String k) {
        if (!peekName(k)) {
            throw error("Expected '" + k + "', found '" + text + "'");
        }
        advance();
    }

    // -------------------------------------------------------- executable docs

    /// Parses an executable document: operations and fragments only.
    Document parseDocument() {
        Document doc = new Document();
        if (kind == EOF) {
            throw error("Unexpected <EOF>");
        }
        while (kind != EOF) {
            if (peek("{")) {
                Operation op = new Operation();
                op.type = "query";
                op.line = tokLine;
                op.column = tokColumn;
                op.selections = selectionSet();
                doc.operations.add(op);
            } else if (peekName("query") || peekName("mutation") || peekName("subscription")) {
                doc.operations.add(operation());
            } else if (peekName("fragment")) {
                Fragment f = fragment();
                if (doc.fragments.containsKey(f.name)) {
                    throw new GraphQLException("There can be only one fragment named \""
                            + f.name + "\".", f.line, f.column);
                }
                doc.fragments.put(f.name, f);
            } else {
                throw error("Unexpected '" + text + "'");
            }
        }
        return doc;
    }

    private Operation operation() {
        Operation op = new Operation();
        op.line = tokLine;
        op.column = tokColumn;
        op.type = name();
        if (kind == NAME) {
            op.name = name();
        }
        if (skip("(")) {
            while (!skip(")")) {
                VariableDef v = new VariableDef();
                v.line = tokLine;
                v.column = tokColumn;
                expect("$");
                v.name = name();
                expect(":");
                v.type = typeRef();
                if (skip("=")) {
                    v.hasDefault = true;
                    v.defaultValue = value(true);
                }
                directives();
                op.variables.add(v);
            }
        }
        op.directives = directives();
        op.selections = selectionSet();
        return op;
    }

    private Fragment fragment() {
        Fragment f = new Fragment();
        f.line = tokLine;
        f.column = tokColumn;
        keyword("fragment");
        if (peekName("on")) {
            throw error("Unexpected 'on'");
        }
        f.name = name();
        keyword("on");
        f.typeCondition = name();
        f.directives = directives();
        f.selections = selectionSet();
        return f;
    }

    private void enter() {
        nesting++;
        if (nesting > MAX_NESTING) {
            throw error("The document nests deeper than " + MAX_NESTING + " levels");
        }
    }

    private List<Selection> selectionSet() {
        expect("{");
        enter();
        List<Selection> out = new ArrayList<Selection>();
        while (!skip("}")) {
            out.add(selection());
        }
        nesting--;
        if (out.isEmpty()) {
            throw error("A selection set cannot be empty");
        }
        return out;
    }

    private Selection selection() {
        int l = tokLine;
        int c = tokColumn;
        if (skip("...")) {
            if (kind == NAME && !peekName("on")) {
                FragmentSpread s = new FragmentSpread();
                s.line = l;
                s.column = c;
                s.name = name();
                s.directives = directives();
                return s;
            }
            InlineFragment f = new InlineFragment();
            f.line = l;
            f.column = c;
            if (peekName("on")) {
                advance();
                f.typeCondition = name();
            }
            f.directives = directives();
            f.selections = selectionSet();
            return f;
        }
        Field f = new Field();
        f.line = l;
        f.column = c;
        String first = name();
        if (skip(":")) {
            f.alias = first;
            f.name = name();
        } else {
            f.name = first;
        }
        f.arguments = arguments(false);
        f.directives = directives();
        if (peek("{")) {
            f.selections = selectionSet();
        }
        return f;
    }

    private Map<String, Object> arguments(boolean isConst) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        if (skip("(")) {
            while (!skip(")")) {
                String n = name();
                expect(":");
                if (out.containsKey(n)) {
                    throw error("There can be only one argument named \"" + n + "\"");
                }
                out.put(n, value(isConst));
            }
        }
        return out;
    }

    private List<Directive> directives() {
        List<Directive> out = null;
        while (peek("@")) {
            advance();
            Directive d = new Directive();
            d.name = name();
            d.arguments = arguments(false);
            if (out == null) {
                out = new ArrayList<Directive>();
            }
            out.add(d);
        }
        return out;
    }

    Object value(boolean isConst) {
        switch (kind) {
            case INT: {
                String t = text;
                advance();
                try {
                    return Long.valueOf(Long.parseLong(t));
                } catch (NumberFormatException e) {
                    return Double.valueOf(Double.parseDouble(t));
                }
            }
            case FLOAT: {
                String t = text;
                advance();
                return Double.valueOf(Double.parseDouble(t));
            }
            case STRING: {
                String t = text;
                advance();
                return t;
            }
            case NAME: {
                String t = text;
                advance();
                if ("true".equals(t)) {
                    return Boolean.TRUE;
                }
                if ("false".equals(t)) {
                    return Boolean.FALSE;
                }
                if ("null".equals(t)) {
                    return null;
                }
                return new EnumLiteral(t);
            }
            default:
                break;
        }
        if (peek("$")) {
            if (isConst) {
                throw error("Unexpected variable in a constant value");
            }
            advance();
            return new Variable(name());
        }
        if (skip("[")) {
            enter();
            List<Object> list = new ArrayList<Object>();
            while (!skip("]")) {
                list.add(value(isConst));
            }
            nesting--;
            return list;
        }
        if (skip("{")) {
            enter();
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            while (!skip("}")) {
                String n = name();
                expect(":");
                map.put(n, value(isConst));
            }
            nesting--;
            return map;
        }
        throw error("Unexpected '" + text + "'");
    }

    TypeRef typeRef() {
        TypeRef t;
        if (skip("[")) {
            enter();
            t = TypeRef.list(typeRef());
            expect("]");
            nesting--;
        } else {
            t = TypeRef.named(name());
        }
        if (skip("!")) {
            t = TypeRef.nonNull(t);
        }
        return t;
    }

    // -------------------------------------------------------------------- SDL

    /// Parses schema definition language into `schema`. May be called once per
    /// file; extensions merge into what earlier files defined.
    void parseSchema(GraphQLSchema schema) {
        while (kind != EOF) {
            description = null;
            if (kind == STRING) {
                description = text;
                advance();
            }
            boolean extend = false;
            if (peekName("extend")) {
                extend = true;
                advance();
            }
            if (kind != NAME) {
                throw error("Unexpected '" + text + "'");
            }
            String k = text;
            int l = tokLine;
            if ("schema".equals(k)) {
                advance();
                directives();
                expect("{");
                while (!skip("}")) {
                    String op = name();
                    expect(":");
                    schema.rootTypeNames.put(op, name());
                }
            } else if ("scalar".equals(k)) {
                advance();
                SchemaType t = defineType(schema, SchemaType.SCALAR, extend, l);
                t.directives = directives();
            } else if ("type".equals(k) || "interface".equals(k)) {
                advance();
                SchemaType t = defineType(schema, "type".equals(k) ? SchemaType.OBJECT
                        : SchemaType.INTERFACE, extend, l);
                if (peekName("implements")) {
                    advance();
                    skip("&");
                    do {
                        t.interfaces.add(name());
                    } while (skip("&"));
                }
                t.directives = directives();
                if (skip("{")) {
                    while (!skip("}")) {
                        FieldDef f = fieldDef();
                        if (t.fields.containsKey(f.name)) {
                            throw new GraphQLException("Field \"" + t.name + "." + f.name
                                    + "\" can only be defined once.", f.line, 0);
                        }
                        t.fields.put(f.name, f);
                    }
                }
            } else if ("union".equals(k)) {
                advance();
                SchemaType t = defineType(schema, SchemaType.UNION, extend, l);
                t.directives = directives();
                if (skip("=")) {
                    skip("|");
                    do {
                        t.unionMembers.add(name());
                    } while (skip("|"));
                }
            } else if ("enum".equals(k)) {
                advance();
                SchemaType t = defineType(schema, SchemaType.ENUM, extend, l);
                t.directives = directives();
                if (skip("{")) {
                    while (!skip("}")) {
                        String d = null;
                        if (kind == STRING) {
                            d = text;
                            advance();
                        }
                        EnumValueDef v = new EnumValueDef();
                        v.description = d;
                        v.name = name();
                        v.deprecationReason = deprecation(directives());
                        t.enumValues.put(v.name, v);
                    }
                }
            } else if ("input".equals(k)) {
                advance();
                SchemaType t = defineType(schema, SchemaType.INPUT_OBJECT, extend, l);
                t.directives = directives();
                if (skip("{")) {
                    while (!skip("}")) {
                        InputValueDef v = inputValueDef();
                        t.inputFields.put(v.name, v);
                    }
                }
            } else if ("directive".equals(k)) {
                advance();
                DirectiveDef d = new DirectiveDef();
                d.description = description;
                expect("@");
                d.name = name();
                if (skip("(")) {
                    while (!skip(")")) {
                        InputValueDef v = inputValueDef();
                        d.args.put(v.name, v);
                    }
                }
                if (peekName("repeatable")) {
                    advance();
                    d.repeatable = true;
                }
                keyword("on");
                skip("|");
                do {
                    d.locations.add(name());
                } while (skip("|"));
                schema.directives.put(d.name, d);
            } else {
                throw error("Unexpected '" + k + "'");
            }
        }
    }

    private SchemaType defineType(GraphQLSchema schema, String typeKind, boolean extend, int l) {
        String n = name();
        SchemaType existing = schema.types.get(n);
        if (extend) {
            if (existing == null) {
                // Files are read in name order, so an extension may come
                // before its type; the definition adopts it, and parse()
                // fails on one that never gets defined.
                existing = new SchemaType(typeKind, n);
                schema.types.put(n, existing);
                schema.undefinedExtensions.put(n, Integer.valueOf(l));
            } else if (!existing.kind.equals(typeKind)) {
                throw new GraphQLException("\"" + n + "\" is extended as a different kind "
                        + "of type than it is.", l, 0);
            }
            return existing;
        }
        if (existing != null && schema.undefinedExtensions.remove(n) != null) {
            if (!existing.kind.equals(typeKind)) {
                throw new GraphQLException("\"" + n + "\" is extended as a different kind "
                        + "of type than it is defined.", l, 0);
            }
            existing.description = description;
            return existing;
        }
        if (existing != null) {
            throw new GraphQLException("There can be only one type named \"" + n + "\".", l, 0);
        }
        SchemaType t = new SchemaType(typeKind, n);
        t.description = description;
        schema.types.put(n, t);
        return t;
    }

    private FieldDef fieldDef() {
        FieldDef f = new FieldDef();
        if (kind == STRING) {
            f.description = text;
            advance();
        }
        f.line = tokLine;
        f.name = name();
        if (skip("(")) {
            while (!skip(")")) {
                InputValueDef v = inputValueDef();
                f.args.put(v.name, v);
            }
        }
        expect(":");
        f.type = typeRef();
        f.deprecationReason = deprecation(directives());
        return f;
    }

    private InputValueDef inputValueDef() {
        InputValueDef v = new InputValueDef();
        if (kind == STRING) {
            v.description = text;
            advance();
        }
        v.name = name();
        expect(":");
        v.type = typeRef();
        if (skip("=")) {
            v.hasDefault = true;
            v.defaultValue = value(true);
        }
        v.deprecationReason = deprecation(directives());
        return v;
    }

    private static String deprecation(List<Directive> directives) {
        if (directives != null) {
            for (Directive d : directives) {
                if ("deprecated".equals(d.name)) {
                    Object r = d.arguments.get("reason");
                    return r instanceof String ? (String) r : "No longer supported";
                }
            }
        }
        return null;
    }

    // -------------------------------------------------------------------- AST

    /// A parsed executable document.
    static final class Document {
        final List<Operation> operations = new ArrayList<Operation>();
        final Map<String, Fragment> fragments = new LinkedHashMap<String, Fragment>();
    }

    abstract static class Node { // NOPMD AbstractClassWithoutAnyMethod - the base of the AST nodes, for their positions
        int line;
        int column;
    }

    static final class Operation extends Node {
        String type;
        String name;
        final List<VariableDef> variables = new ArrayList<VariableDef>();
        List<Directive> directives;
        List<Selection> selections;
    }

    static final class VariableDef extends Node {
        String name;
        TypeRef type;
        boolean hasDefault;
        Object defaultValue;
    }

    abstract static class Selection extends Node { // NOPMD AbstractClassWithoutAnyMethod - a field, spread or inline fragment
        List<Directive> directives;
    }

    static final class Field extends Selection {
        String alias;
        String name;
        Map<String, Object> arguments;
        List<Selection> selections;

        String responseKey() {
            return alias != null ? alias : name;
        }
    }

    static final class FragmentSpread extends Selection {
        String name;
    }

    static final class InlineFragment extends Selection {
        String typeCondition;
        List<Selection> selections;
    }

    static final class Fragment extends Node {
        String name;
        String typeCondition;
        List<Directive> directives;
        List<Selection> selections;
    }

    static final class Directive {
        String name;
        Map<String, Object> arguments;
    }

    /// A `$name` reference inside an argument value.
    static final class Variable {
        final String name;

        Variable(String name) {
            this.name = name;
        }
    }

    /// An unquoted enum value inside an argument value.
    static final class EnumLiteral {
        final String name;

        EnumLiteral(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

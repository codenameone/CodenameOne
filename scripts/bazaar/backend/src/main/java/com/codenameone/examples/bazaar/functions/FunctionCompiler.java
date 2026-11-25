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
package com.codenameone.examples.bazaar.functions;

import java.util.ArrayList;
import java.util.List;

/// Compiles a checkout function's source to a [Program], once, when an app
/// uploads it. A hand-written lexer and a recursive descent parser that
/// emits code as it parses; the result then goes through [Verifier] like
/// any stored program.
///
/// The language, in full:
///
/// ```
/// # a comment runs to the end of the line
/// let off = 0;                          # a long, a string or a boolean
/// for line in lines {                   # each list of the target, in order
///   if line.quantity >= 3 && starts_with(line.sku, "BULK-") { ... }
///   off = off + line.total / 10;
/// }
/// if off > 0 { emit discount(off, "Bulk 10%"); } else if cart.code == "X" { return; }
/// ```
///
/// - Values: long integers (minor units), strings and booleans; no
///   conversion between them. `+ - * / %` on longs, `== != < <= > >=`,
///   `&& || !` on booleans (both sides are evaluated), `==`/`!=` on strings.
/// - Built-ins: `min(a, b)`, `max(a, b)`, `contains(s, part)`,
///   `starts_with(s, prefix)`.
/// - `cart.<scalar>` reads the cart ([Target]); `<loopvar>.<field>` reads
///   the row a `for` is on. Lists are only walked by `for`, so every loop
///   ends; fuel bounds the nesting.
/// - `emit <op>(args);` records an operation of the target; `return;` stops.
/// - A `let` lives until the end of its block, and its type is its first
///   value's.
public final class FunctionCompiler {
    /// The longest source accepted.
    public static final int MAX_SOURCE = 16000;
    private static final long MAX_LITERAL = 1000000000000000L;
    private static final int MAX_STRING = 500;
    private static final int MAX_DEPTH = 24;

    private static final int T_EOF = 0;
    private static final int T_IDENT = 1;
    private static final int T_INT = 2;
    private static final int T_STRING = 3;
    private static final int T_PUNCT = 4;

    private final String src;
    private final Target target;
    private int pos;
    private int line = 1;

    private int kind;
    private String text;
    private long number;
    private int tokenLine;

    private int[] code = new int[256];
    private int length;
    private final List<Long> longs = new ArrayList<Long>();
    private final List<String> strings = new ArrayList<String>();
    private final StringBuilder localTypes = new StringBuilder();

    private final List<String> names = new ArrayList<String>();
    private final List<int[]> slots = new ArrayList<int[]>();
    private int depth;

    private FunctionCompiler(String src, Target target) {
        this.src = src;
        this.target = target;
    }

    /// Compiles `source` for `target`; the exception says what is wrong and
    /// on which line.
    public static Program compile(String source, Target target) throws FunctionException {
        if (target == null) {
            throw new FunctionException("The target is one of " + Target.names());
        }
        if (source == null || source.trim().length() == 0) {
            throw new FunctionException("The function has no source");
        }
        if (source.length() > MAX_SOURCE) {
            throw new FunctionException("The source is longer than " + MAX_SOURCE + " characters");
        }
        FunctionCompiler c = new FunctionCompiler(source, target);
        c.next();
        while (c.kind != T_EOF) {
            c.statement();
        }
        c.op(Program.HALT);
        int[] code = new int[c.length];
        System.arraycopy(c.code, 0, code, 0, c.length);
        long[] longs = new long[c.longs.size()];
        for (int iter = 0; iter < longs.length; iter++) {
            longs[iter] = c.longs.get(iter).longValue();
        }
        String[] strings = c.strings.toArray(new String[c.strings.size()]);
        Program p = new Program(target, code, longs, strings, c.localTypes.toString());
        Verifier.verify(p);
        return p;
    }

    // ------------------------------------------------------------- lexer

    private FunctionException error(String message) {
        return new FunctionException("line " + tokenLine + ": " + message);
    }

    private void next() throws FunctionException {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\n') {
                line++;
                pos++;
            } else if (c == ' ' || c == '\t' || c == '\r') {
                pos++;
            } else if (c == '#') {
                while (pos < src.length() && src.charAt(pos) != '\n') {
                    pos++;
                }
            } else {
                break;
            }
        }
        tokenLine = line;
        if (pos >= src.length()) {
            kind = T_EOF;
            text = "end of source";
            return;
        }
        char c = src.charAt(pos);
        if (letter(c)) {
            int start = pos;
            while (pos < src.length() && (letter(src.charAt(pos)) || digit(src.charAt(pos)))) {
                pos++;
            }
            kind = T_IDENT;
            text = src.substring(start, pos);
            if (text.length() > 40) {
                throw error("A name is longer than 40 characters");
            }
            return;
        }
        if (digit(c)) {
            long v = 0;
            while (pos < src.length() && digit(src.charAt(pos))) {
                v = v * 10 + (src.charAt(pos) - '0');
                if (v > MAX_LITERAL) {
                    throw error("A number is larger than " + MAX_LITERAL);
                }
                pos++;
            }
            if (pos < src.length() && letter(src.charAt(pos))) {
                throw error("A number runs into a name");
            }
            kind = T_INT;
            number = v;
            text = Long.toString(v);
            return;
        }
        if (c == '"') {
            StringBuilder b = new StringBuilder();
            pos++;
            while (true) {
                if (pos >= src.length() || src.charAt(pos) == '\n') {
                    throw error("A string is not closed");
                }
                char s = src.charAt(pos++);
                if (s == '"') {
                    break;
                }
                if (s == '\\') {
                    if (pos >= src.length()) {
                        throw error("A string is not closed");
                    }
                    char e = src.charAt(pos++);
                    if (e == 'n') {
                        b.append('\n');
                    } else if (e == '"' || e == '\\') {
                        b.append(e);
                    } else {
                        throw error("Unknown escape in a string");
                    }
                } else {
                    b.append(s);
                }
                if (b.length() > MAX_STRING) {
                    throw error("A string is longer than " + MAX_STRING + " characters");
                }
            }
            kind = T_STRING;
            text = b.toString();
            return;
        }
        String two = pos + 1 < src.length() ? src.substring(pos, pos + 2) : "";
        if ("==".equals(two) || "!=".equals(two) || "<=".equals(two) || ">=".equals(two)
                || "&&".equals(two) || "||".equals(two)) {
            kind = T_PUNCT;
            text = two;
            pos += 2;
            return;
        }
        if ("+-*/%<>!=(){};,.".indexOf(c) >= 0) {
            kind = T_PUNCT;
            text = String.valueOf(c);
            pos++;
            return;
        }
        throw error("Unexpected character '" + (c < 0x20 || c > 0x7e ? "?" : String.valueOf(c))
                + "'");
    }

    private static boolean letter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean digit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean is(String punct) {
        return kind == T_PUNCT && text.equals(punct);
    }

    private boolean word(String w) {
        return kind == T_IDENT && text.equals(w);
    }

    private void expect(String punct) throws FunctionException {
        if (!is(punct)) {
            throw error("Expected '" + punct + "' but found " + describe());
        }
        next();
    }

    private String describe() {
        if (kind == T_EOF) {
            return "the end of the source";
        }
        return kind == T_STRING ? "a string" : "'" + text + "'";
    }

    private String ident(String what) throws FunctionException {
        if (kind != T_IDENT) {
            throw error("Expected " + what + " but found " + describe());
        }
        String name = text;
        next();
        return name;
    }

    private static boolean reserved(String name) {
        return "let".equals(name) || "if".equals(name) || "else".equals(name)
                || "for".equals(name) || "in".equals(name) || "emit".equals(name)
                || "return".equals(name) || "true".equals(name) || "false".equals(name)
                || "cart".equals(name) || "min".equals(name) || "max".equals(name)
                || "contains".equals(name) || "starts_with".equals(name);
    }

    // ------------------------------------------------------------- code

    private void op(int value) throws FunctionException {
        if (length >= Program.MAX_CODE) {
            throw error("The function is too long");
        }
        if (length == code.length) {
            int[] bigger = new int[code.length * 2];
            System.arraycopy(code, 0, bigger, 0, length);
            code = bigger;
        }
        code[length++] = value;
    }

    private void op(int opcode, int a) throws FunctionException {
        op(opcode);
        op(a);
    }

    private int longConstant(long v) {
        for (int iter = 0; iter < longs.size(); iter++) {
            if (longs.get(iter).longValue() == v) {
                return iter;
            }
        }
        longs.add(Long.valueOf(v));
        return longs.size() - 1;
    }

    private int stringConstant(String s) {
        int at = strings.indexOf(s);
        if (at >= 0) {
            return at;
        }
        strings.add(s);
        return strings.size() - 1;
    }

    /// A new local of `type`; `list` is the list of a loop variable, or -1.
    private int declare(String name, char type, int list) throws FunctionException {
        if (reserved(name)) {
            throw error("'" + name + "' is a reserved word");
        }
        if (find(name) != null) {
            throw error("'" + name + "' is already declared");
        }
        if (Target.indexOf(target.lists, name) >= 0) {
            throw error("'" + name + "' is the name of a list");
        }
        if (localTypes.length() >= Program.MAX_LOCALS) {
            throw error("Too many variables");
        }
        int slot = localTypes.length();
        localTypes.append(type);
        names.add(name);
        slots.add(new int[] {slot, type, list});
        return slot;
    }

    private int[] find(String name) {
        for (int iter = names.size() - 1; iter >= 0; iter--) {
            if (names.get(iter).equals(name)) {
                return slots.get(iter);
            }
        }
        return null;
    }

    private void endScope(int mark) {
        while (names.size() > mark) {
            names.remove(names.size() - 1);
            slots.remove(slots.size() - 1);
        }
    }

    // ------------------------------------------------------------- statements

    private void block() throws FunctionException {
        if (++depth > MAX_DEPTH) {
            throw error("Blocks are nested too deeply");
        }
        expect("{");
        int mark = names.size();
        while (!is("}")) {
            if (kind == T_EOF) {
                throw error("A block is not closed");
            }
            statement();
        }
        next();
        endScope(mark);
        depth--;
    }

    private void statement() throws FunctionException {
        if (word("let")) {
            next();
            String name = ident("a variable name");
            expect("=");
            char type = expression();
            expect(";");
            op(Program.STORE, declare(name, type, -1));
            return;
        }
        if (word("if")) {
            next();
            ifRest();
            return;
        }
        if (word("for")) {
            next();
            String name = ident("a loop variable");
            if (!word("in")) {
                throw error("Expected 'in' but found " + describe());
            }
            next();
            String listName = ident("a list");
            int list = Target.indexOf(target.lists, listName);
            if (list < 0) {
                throw error("There is no list '" + listName + "' in a " + target.name
                        + " function");
            }
            int mark = names.size();
            int slot = declare(name, Target.LONG, list);
            op(Program.FOR_INIT, slot);
            int top = length;
            op(Program.LOOP);
            op(list);
            op(slot);
            int exit = length;
            op(0);
            block();
            op(Program.INC, slot);
            op(Program.JMP, top);
            code[exit] = length;
            endScope(mark);
            return;
        }
        if (word("emit")) {
            next();
            String name = ident("an operation");
            int e = target.emit(name);
            if (e < 0) {
                throw error("A " + target.name + " function cannot emit '" + name + "'");
            }
            String sig = target.signature(e);
            expect("(");
            for (int a = 0; a < sig.length(); a++) {
                if (a > 0) {
                    expect(",");
                }
                char type = expression();
                if (type != sig.charAt(a)) {
                    throw error("Argument " + (a + 1) + " of " + name + " should be "
                            + typeName(sig.charAt(a)));
                }
            }
            expect(")");
            expect(";");
            op(Program.EMIT, e);
            return;
        }
        if (word("return")) {
            next();
            expect(";");
            op(Program.HALT);
            return;
        }
        if (kind == T_IDENT && !reserved(text)) {
            String name = text;
            int[] local = find(name);
            next();
            if (local == null) {
                throw error("'" + name + "' is not declared; use let");
            }
            if (local[2] >= 0) {
                throw error("The loop variable '" + name + "' cannot be assigned");
            }
            expect("=");
            char type = expression();
            if (type != (char) local[1]) {
                throw error("'" + name + "' holds " + typeName((char) local[1]));
            }
            expect(";");
            op(Program.STORE, local[0]);
            return;
        }
        throw error("Expected a statement but found " + describe());
    }

    private void ifRest() throws FunctionException {
        condition();
        op(Program.JZ);
        int otherwise = length;
        op(0);
        block();
        if (word("else")) {
            next();
            op(Program.JMP);
            int end = length;
            op(0);
            code[otherwise] = length;
            if (word("if")) {
                next();
                ifRest();
            } else {
                block();
            }
            code[end] = length;
        } else {
            code[otherwise] = length;
        }
    }

    private void condition() throws FunctionException {
        if (expression() != Target.BOOL) {
            throw error("A condition must be true or false");
        }
    }

    private static String typeName(char type) {
        if (type == Target.LONG) {
            return "a number";
        }
        return type == Target.STRING ? "a string" : "true or false";
    }

    // ------------------------------------------------------------- expressions

    private char expression() throws FunctionException {
        if (++depth > MAX_DEPTH * 2) {
            throw error("An expression is nested too deeply");
        }
        char t = and();
        while (is("||")) {
            next();
            bools(t, and());
            op(Program.OR);
            t = Target.BOOL;
        }
        depth--;
        return t;
    }

    private char and() throws FunctionException {
        char t = comparison();
        while (is("&&")) {
            next();
            bools(t, comparison());
            op(Program.AND);
            t = Target.BOOL;
        }
        return t;
    }

    private void bools(char a, char b) throws FunctionException {
        if (a != Target.BOOL || b != Target.BOOL) {
            throw error("&& and || take true or false on both sides");
        }
    }

    private char comparison() throws FunctionException {
        char a = sum();
        if (is("==") || is("!=") || is("<") || is("<=") || is(">") || is(">=")) {
            String o = text;
            next();
            char b = sum();
            if (a != b) {
                throw error("'" + o + "' compares " + typeName(a) + " with " + typeName(b));
            }
            boolean eq = "==".equals(o);
            if (a == Target.STRING) {
                if (!eq && !"!=".equals(o)) {
                    throw error("Strings are compared with == and != only");
                }
                op(eq ? Program.SEQ : Program.SNE);
            } else if (a == Target.BOOL) {
                if (!eq && !"!=".equals(o)) {
                    throw error("true and false are compared with == and != only");
                }
                op(eq ? Program.EQ : Program.NE);
            } else {
                op(eq ? Program.EQ : "!=".equals(o) ? Program.NE : "<".equals(o) ? Program.LT
                        : "<=".equals(o) ? Program.LE : ">".equals(o) ? Program.GT : Program.GE);
            }
            return Target.BOOL;
        }
        return a;
    }

    private char sum() throws FunctionException {
        char t = product();
        while (is("+") || is("-")) {
            int o = is("+") ? Program.ADD : Program.SUB;
            next();
            numbers(t, product());
            op(o);
        }
        return t;
    }

    private char product() throws FunctionException {
        char t = unary();
        while (is("*") || is("/") || is("%")) {
            int o = is("*") ? Program.MUL : is("/") ? Program.DIV : Program.MOD;
            next();
            numbers(t, unary());
            op(o);
        }
        return t;
    }

    private void numbers(char a, char b) throws FunctionException {
        if (a != Target.LONG || b != Target.LONG) {
            throw error("Arithmetic takes numbers on both sides");
        }
    }

    private char unary() throws FunctionException {
        if (is("-")) {
            next();
            if (unary() != Target.LONG) {
                throw error("'-' takes a number");
            }
            op(Program.NEG);
            return Target.LONG;
        }
        if (is("!")) {
            next();
            if (unary() != Target.BOOL) {
                throw error("'!' takes true or false");
            }
            op(Program.NOT);
            return Target.BOOL;
        }
        return primary();
    }

    private char primary() throws FunctionException {
        if (kind == T_INT) {
            op(Program.PUSH_L, longConstant(number));
            next();
            return Target.LONG;
        }
        if (kind == T_STRING) {
            op(Program.PUSH_S, stringConstant(text));
            next();
            return Target.STRING;
        }
        if (is("(")) {
            next();
            char t = expression();
            expect(")");
            return t;
        }
        if (kind != T_IDENT) {
            throw error("Expected a value but found " + describe());
        }
        String name = text;
        next();
        if ("true".equals(name) || "false".equals(name)) {
            op(Program.PUSH_L, longConstant("true".equals(name) ? 1 : 0));
            return Target.BOOL;
        }
        if ("min".equals(name) || "max".equals(name)) {
            call(name, Target.LONG);
            op("min".equals(name) ? Program.MIN : Program.MAX);
            return Target.LONG;
        }
        if ("contains".equals(name) || "starts_with".equals(name)) {
            call(name, Target.STRING);
            op("contains".equals(name) ? Program.CONTAINS : Program.STARTS);
            return Target.BOOL;
        }
        if ("cart".equals(name)) {
            expect(".");
            String field = ident("a cart field");
            int slot = Target.indexOf(Target.SCALARS, field);
            if (slot < 0) {
                throw error("The cart has no field '" + field + "'");
            }
            op(Program.SCALAR, slot);
            return Target.scalarType(slot);
        }
        int[] local = find(name);
        if (local == null) {
            if (Target.indexOf(target.lists, name) >= 0) {
                throw error("A list is read with for: for x in " + name + " { ... }");
            }
            throw error("'" + name + "' is not declared");
        }
        if (local[2] >= 0) {
            int list = local[2];
            if (!is(".")) {
                throw error("Read a field of the loop variable: " + name + ".<field>");
            }
            next();
            String field = ident("a field");
            int f = Target.indexOf(target.fields[list], field);
            if (f < 0) {
                throw error("'" + target.lists[list] + "' has no field '" + field + "'");
            }
            op(Program.FIELD);
            op(list);
            op(f);
            op(local[0]);
            return target.fieldType(list, f);
        }
        op(Program.LOAD, local[0]);
        return (char) local[1];
    }

    private void call(String name, char type) throws FunctionException {
        expect("(");
        char a = expression();
        expect(",");
        char b = expression();
        expect(")");
        if (a != type || b != type) {
            throw error(name + " takes two " + (type == Target.LONG ? "numbers" : "strings"));
        }
    }
}

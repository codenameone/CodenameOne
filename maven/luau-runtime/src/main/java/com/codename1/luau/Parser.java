/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.luau;

import java.util.ArrayList;

/// Recursive descent parser for Luau. Type annotations, type aliases,
/// casts and attributes are parsed and dropped: they do not change what a
/// program does at run time.
final class Parser {
    private final ArrayList<Lexer.Token> toks;
    private final String chunk;
    private int p;

    Parser(String src, String chunk) {
        this.toks = new Lexer(src, chunk).tokenize();
        this.chunk = chunk;
    }

    Ast.Function parseChunk() {
        Ast.Function f = new Ast.Function();
        f.vararg = true;
        f.name = "main chunk";
        f.body = block();
        if (cur().type != Lexer.EOF) {
            throw err("'<eof>' expected near " + cur());
        }
        return f;
    }

    private Lexer.Token cur() {
        return toks.get(p);
    }

    private Lexer.Token peekTok(int off) {
        int i = Math.min(p + off, toks.size() - 1);
        return toks.get(i);
    }

    private boolean is(String s) {
        return cur().is(s);
    }

    private boolean accept(String s) {
        if (is(s)) {
            p++;
            return true;
        }
        return false;
    }

    private Lexer.Token expect(String s) {
        if (!is(s)) {
            throw err("'" + s + "' expected near " + cur());
        }
        return toks.get(p++);
    }

    private void expectMatch(String close, String open, int line) {
        if (!is(close)) {
            if (line == cur().line) {
                throw err("'" + close + "' expected near " + cur());
            }
            throw err("'" + close + "' expected (to close '" + open + "' at line " + line + ") near " + cur());
        }
        p++;
    }

    private String name() {
        Lexer.Token t = cur();
        if (t.type != Lexer.NAME) {
            throw err("expected identifier near " + t);
        }
        p++;
        return t.text;
    }

    private LuaError err(String msg) {
        return new LuaError(chunk + ":" + cur().line + ": " + msg);
    }

    private boolean blockEnd() {
        Lexer.Token t = cur();
        return t.type == Lexer.EOF || t.is("end") || t.is("else") || t.is("elseif") || t.is("until");
    }

    private Ast.Block block() {
        Ast.Block b = new Ast.Block();
        while (!blockEnd()) {
            if (is("return")) {
                b.stats.add(returnStat());
                break;
            }
            Ast.Stat s = statement();
            if (s != null) {
                b.stats.add(s);
            }
        }
        return b;
    }

    private Ast.Stat returnStat() {
        int line = cur().line;
        p++;
        Ast.Return r = new Ast.Return();
        r.line = line;
        if (!blockEnd() && !is(";")) {
            exprList(r.values);
        }
        accept(";");
        return r;
    }

    private boolean isContextual(String word) {
        Lexer.Token t = cur();
        return t.type == Lexer.NAME && t.text.equals(word);
    }

    private Ast.Stat statement() {
        int line = cur().line;
        Ast.Stat s = statementInner();
        if (s != null) {
            s.line = line;
        }
        return s;
    }

    private Ast.Stat statementInner() {
        if (accept(";")) {
            return null;
        }
        while (is("@")) {
            p++;
            name();
            if (is("[")) {
                skipBalanced("[", "]");
            }
        }
        Lexer.Token t = cur();
        if (t.is("if")) {
            return ifStat();
        }
        if (t.is("while")) {
            p++;
            Ast.Expr c = expr();
            expect("do");
            Ast.Block b = block();
            expectMatch("end", "while", t.line);
            return new Ast.While(c, b);
        }
        if (t.is("do")) {
            p++;
            Ast.Block b = block();
            expectMatch("end", "do", t.line);
            return new Ast.Do(b);
        }
        if (t.is("for")) {
            return forStat();
        }
        if (t.is("repeat")) {
            p++;
            Ast.Block b = block();
            expectMatch("until", "repeat", t.line);
            return new Ast.Repeat(b, expr());
        }
        if (t.is("function")) {
            p++;
            return functionStat(t.line);
        }
        if (t.is("local")) {
            p++;
            if (accept("function")) {
                String n = name();
                Ast.Function f = funcBody(false, t.line);
                f.name = n;
                return new Ast.LocalFunction(n, f);
            }
            Ast.Local l = new Ast.Local();
            do {
                l.names.add(name());
                if (is("<")) {
                    p++;
                    name();
                    expect(">");
                }
                if (accept(":")) {
                    type();
                }
            } while (accept(","));
            if (accept("=")) {
                exprList(l.values);
            }
            return l;
        }
        if (t.is("break")) {
            p++;
            return new Ast.Break();
        }
        if (isContextual("continue") && !continuesExpression(peekTok(1))) {
            p++;
            return new Ast.Continue();
        }
        if (isContextual("export") && peekTok(1).type == Lexer.NAME && "type".equals(peekTok(1).text)) {
            p++;
        }
        if (isContextual("type") && peekTok(1).type == Lexer.NAME
                && (peekTok(2).is("=") || peekTok(2).is("<"))) {
            p += 2;
            if (is("<")) {
                skipBalanced("<", ">");
            }
            expect("=");
            type();
            return null;
        }
        return exprStat();
    }

    private static boolean continuesExpression(Lexer.Token t) {
        return t.is("(") || t.is("=") || t.is(".") || t.is("[") || t.is(":") || t.is("{")
                || t.is(",") || t.type == Lexer.STRING || t.is("+=") || t.is("-=");
    }

    private Ast.Stat ifStat() {
        int line = cur().line;
        p++;
        Ast.If s = new Ast.If();
        s.conds.add(expr());
        expect("then");
        s.blocks.add(block());
        while (true) {
            if (accept("elseif")) {
                s.conds.add(expr());
                expect("then");
                s.blocks.add(block());
            } else if (accept("else")) {
                s.orElse = block();
                expectMatch("end", "if", line);
                return s;
            } else {
                expectMatch("end", "if", line);
                return s;
            }
        }
    }

    private Ast.Stat forStat() {
        int line = cur().line;
        p++;
        String first = name();
        if (accept(":")) {
            type();
        }
        if (accept("=")) {
            Ast.NumFor f = new Ast.NumFor();
            f.var = first;
            f.start = expr();
            expect(",");
            f.limit = expr();
            if (accept(",")) {
                f.step = expr();
            }
            expect("do");
            f.body = block();
            expectMatch("end", "for", line);
            return f;
        }
        Ast.GenFor f = new Ast.GenFor();
        f.vars.add(first);
        while (accept(",")) {
            f.vars.add(name());
            if (accept(":")) {
                type();
            }
        }
        expect("in");
        exprList(f.exprs);
        expect("do");
        f.body = block();
        expectMatch("end", "for", line);
        return f;
    }

    private Ast.Stat functionStat(int line) {
        String n = name();
        Ast.Expr target = named(new Ast.Name(n), line);
        StringBuilder full = new StringBuilder(n);
        boolean method = false;
        while (is(".") || is(":")) {
            boolean colon = is(":");
            p++;
            String k = name();
            full.append(colon ? ':' : '.').append(k);
            target = named(new Ast.Index(target, str(k)), line);
            if (colon) {
                method = true;
                break;
            }
        }
        Ast.Function f = funcBody(method, line);
        f.name = full.toString();
        Ast.Assign a = new Ast.Assign();
        a.targets.add(target);
        a.values.add(f);
        return a;
    }

    private static Ast.Expr named(Ast.Expr e, int line) {
        e.line = line;
        return e;
    }

    private static Ast.Str str(String s) {
        return new Ast.Str(s);
    }

    private Ast.Function funcBody(boolean method, int line) {
        Ast.Function f = new Ast.Function();
        f.line = line;
        if (method) {
            f.params.add("self");
        }
        if (is("<")) {
            skipBalanced("<", ">");
        }
        expect("(");
        if (!is(")")) {
            do {
                if (accept("...")) {
                    f.vararg = true;
                    if (accept(":")) {
                        typeOrPack();
                    }
                    break;
                }
                f.params.add(name());
                if (accept(":")) {
                    type();
                }
            } while (accept(","));
        }
        expect(")");
        if (accept(":")) {
            returnType();
        }
        f.body = block();
        expectMatch("end", "function", line);
        return f;
    }

    private Ast.Stat exprStat() {
        int line = cur().line;
        Ast.Expr e = suffixedExpr();
        String op = cur().text;
        if (cur().type == Lexer.OP && op.length() >= 2 && op.endsWith("=") && !"==".equals(op)
                && !"~=".equals(op) && !"<=".equals(op) && !">=".equals(op)) {
            p++;
            checkAssignable(e);
            return new Ast.CompoundAssign(op.substring(0, op.length() - 1), e, expr());
        }
        if (is("=") || is(",")) {
            Ast.Assign a = new Ast.Assign();
            checkAssignable(e);
            a.targets.add(e);
            while (accept(",")) {
                Ast.Expr t = suffixedExpr();
                checkAssignable(t);
                a.targets.add(t);
            }
            expect("=");
            exprList(a.values);
            return a;
        }
        if (!(e instanceof Ast.Call)) {
            throw new LuaError(chunk + ":" + line + ": syntax error near " + cur());
        }
        return new Ast.ExprStat((Ast.Call) e);
    }

    private void checkAssignable(Ast.Expr e) {
        if (!(e instanceof Ast.Name) && !(e instanceof Ast.Index)) {
            throw err("syntax error near " + cur());
        }
    }

    private void exprList(ArrayList<Ast.Expr> out) {
        out.add(expr());
        while (accept(",")) {
            out.add(expr());
        }
    }

    Ast.Expr expr() {
        return subExpr(0);
    }

    private static int leftPri(String op) {
        switch (op) {
            case "or":
                return 1;
            case "and":
                return 2;
            case "<":
            case ">":
            case "<=":
            case ">=":
            case "~=":
            case "==":
                return 3;
            case "..":
                return 5;
            case "+":
            case "-":
                return 6;
            case "*":
            case "/":
            case "//":
            case "%":
                return 7;
            case "^":
                return 10;
            default:
                return -1;
        }
    }

    private static int rightPri(String op) {
        if ("..".equals(op)) {
            return 4;
        }
        if ("^".equals(op)) {
            return 9;
        }
        return leftPri(op);
    }

    private static final int UNARY_PRI = 8;

    private Ast.Expr subExpr(int limit) {
        Ast.Expr e;
        int line = cur().line;
        if (is("not") || is("-") || is("#")) {
            String op = cur().text;
            p++;
            e = new Ast.Unop(op, subExpr(UNARY_PRI));
            e.line = line;
        } else {
            e = simpleExpr();
        }
        while (true) {
            Lexer.Token t = cur();
            if (t.type != Lexer.OP && t.type != Lexer.KEYWORD) {
                return e;
            }
            int lp = leftPri(t.text);
            if (lp <= limit) {
                return e;
            }
            p++;
            Ast.Expr r = subExpr(rightPri(t.text));
            e = new Ast.Binop(t.text, e, r);
            e.line = t.line;
        }
    }

    private Ast.Expr simpleExpr() {
        Ast.Expr e = simpleExprInner();
        while (accept("::")) {
            type();
        }
        return e;
    }

    private Ast.Expr simpleExprInner() {
        Lexer.Token t = cur();
        Ast.Expr e;
        switch (t.type) {
            case Lexer.NUMBER:
                p++;
                e = new Ast.Num(t.num);
                break;
            case Lexer.STRING:
            case Lexer.ISIMPLE:
                p++;
                e = new Ast.Str(t.text);
                break;
            case Lexer.IBEGIN:
                e = interp();
                break;
            default:
                if (t.is("nil")) {
                    p++;
                    e = new Ast.Nil();
                } else if (t.is("true")) {
                    p++;
                    e = new Ast.Bool(true);
                } else if (t.is("false")) {
                    p++;
                    e = new Ast.Bool(false);
                } else if (t.is("...")) {
                    p++;
                    e = new Ast.Vararg();
                } else if (t.is("{")) {
                    e = table();
                } else if (t.is("function")) {
                    p++;
                    e = funcBody(false, t.line);
                } else if (t.is("if")) {
                    e = ifExpr();
                } else {
                    return suffixedExpr();
                }
        }
        e.line = t.line;
        return e;
    }

    private Ast.Expr interp() {
        Ast.Interp in = new Ast.Interp();
        in.line = cur().line;
        in.parts.add(cur().text);
        p++;
        while (true) {
            in.exprs.add(expr());
            Lexer.Token t = cur();
            if (t.type == Lexer.IMID) {
                in.parts.add(t.text);
                p++;
            } else if (t.type == Lexer.IEND) {
                in.parts.add(t.text);
                p++;
                return in;
            } else {
                throw err("malformed interpolated string near " + t);
            }
        }
    }

    private Ast.Expr ifExpr() {
        Ast.IfExpr e = new Ast.IfExpr();
        expect("if");
        e.conds.add(expr());
        expect("then");
        e.values.add(expr());
        while (accept("elseif")) {
            e.conds.add(expr());
            expect("then");
            e.values.add(expr());
        }
        expect("else");
        e.orElse = expr();
        return e;
    }

    private Ast.Expr primaryExpr() {
        Lexer.Token t = cur();
        if (t.type == Lexer.NAME) {
            p++;
            Ast.Expr e = new Ast.Name(t.text);
            e.line = t.line;
            return e;
        }
        if (t.is("(")) {
            p++;
            Ast.Expr inner = expr();
            expectMatch(")", "(", t.line);
            Ast.Expr e = new Ast.Paren(inner);
            e.line = t.line;
            return e;
        }
        throw err("unexpected symbol near " + t);
    }

    private Ast.Expr suffixedExpr() {
        Ast.Expr e = primaryExpr();
        while (true) {
            Lexer.Token t = cur();
            if (t.is(".")) {
                p++;
                e = new Ast.Index(e, str(name()));
            } else if (t.is("[")) {
                p++;
                Ast.Expr k = expr();
                expect("]");
                e = new Ast.Index(e, k);
            } else if (t.is(":")) {
                p++;
                String m = name();
                e = new Ast.Call(e, m, callArgs());
            } else if (t.is("(") || t.is("{") || t.type == Lexer.STRING || t.type == Lexer.ISIMPLE
                    || t.type == Lexer.IBEGIN) {
                if (t.is("(") && t.line != previousLine()) {
                    throw err("ambiguous syntax (function call x new statement) near '('");
                }
                e = new Ast.Call(e, null, callArgs());
            } else {
                return e;
            }
            e.line = t.line;
        }
    }

    private int previousLine() {
        return p > 0 ? toks.get(p - 1).line : 0;
    }

    private ArrayList<Ast.Expr> callArgs() {
        ArrayList<Ast.Expr> args = new ArrayList<Ast.Expr>();
        Lexer.Token t = cur();
        if (t.type == Lexer.STRING || t.type == Lexer.ISIMPLE) {
            p++;
            args.add(str(t.text));
            return args;
        }
        if (t.type == Lexer.IBEGIN) {
            args.add(interp());
            return args;
        }
        if (t.is("{")) {
            args.add(table());
            return args;
        }
        expect("(");
        if (!is(")")) {
            exprList(args);
        }
        expectMatch(")", "(", t.line);
        return args;
    }

    private Ast.Expr table() {
        int line = cur().line;
        expect("{");
        Ast.TableCons tc = new Ast.TableCons();
        tc.line = line;
        while (!is("}")) {
            if (is("[")) {
                p++;
                Ast.Expr k = expr();
                expect("]");
                expect("=");
                tc.keys.add(k);
                tc.values.add(expr());
            } else if (cur().type == Lexer.NAME && peekTok(1).is("=")) {
                tc.keys.add(str(name()));
                p++;
                tc.values.add(expr());
            } else {
                tc.keys.add(null);
                tc.values.add(expr());
            }
            if (!accept(",") && !accept(";")) {
                break;
            }
        }
        expectMatch("}", "{", line);
        return tc;
    }

    // ---- types: parsed and discarded ----

    private void skipBalanced(String open, String close) {
        expect(open);
        int depth = 1;
        while (depth > 0) {
            if (cur().type == Lexer.EOF) {
                throw err("'" + close + "' expected near <eof>");
            }
            if (is(open)) {
                depth++;
            } else if (is(close)) {
                depth--;
            } else if (">".equals(close) && is("->")) {
                p++;
                continue;
            }
            p++;
        }
    }

    private void returnType() {
        typeOrPack();
    }

    private void typeOrPack() {
        if (is("(")) {
            // either a tuple of return types or a function type / parenthesized type
            int save = p;
            skipBalanced("(", ")");
            if (accept("->")) {
                returnType();
            } else {
                // a tuple; allow unions after a parenthesized single type
                p = save;
                type();
            }
            return;
        }
        if (accept("...")) {
            type();
            return;
        }
        type();
    }

    private void type() {
        if (accept("?")) {
            return;
        }
        accept("|");
        accept("&");
        simpleType();
        while (true) {
            if (accept("?")) {
                continue;
            }
            if (accept("|") || accept("&")) {
                simpleType();
                continue;
            }
            return; // NOPMD AvoidBranchingStatementAsLastInLoop - loop over a union of types
        }
    }

    private void simpleType() {
        Lexer.Token t = cur();
        if (t.is("nil") || t.is("true") || t.is("false") || t.type == Lexer.STRING) {
            p++;
            return;
        }
        if (t.is("...")) {
            p++;
            type();
            return;
        }
        if (t.is("{")) {
            skipBalanced("{", "}");
            return;
        }
        if (t.is("<")) {
            skipBalanced("<", ">");
            simpleType();
            return;
        }
        if (t.is("(")) {
            skipBalanced("(", ")");
            if (accept("->")) {
                returnType();
            }
            return;
        }
        if (t.is("function")) {
            p++;
            skipBalanced("(", ")");
            expect("->");
            returnType();
            return;
        }
        if (t.type == Lexer.NAME) {
            p++;
            if ("typeof".equals(t.text) && is("(")) {
                skipBalanced("(", ")");
                return;
            }
            if (accept(".")) {
                name();
            }
            if (is("<")) {
                skipBalanced("<", ">");
            }
            return;
        }
        throw err("expected type near " + t);
    }
}

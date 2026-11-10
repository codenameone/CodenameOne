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
package com.codename1.python.transpiler.parser;

import com.codename1.python.transpiler.api.PythonSyntaxError;
import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// A recursive-descent parser for Python 3.13, following the precedence and
/// statement structure of the language reference's grammar.
///
/// Soft keywords (`match`, `case`, `type`, `_`) are recognised by context, as
/// CPython's PEG parser does, so they stay usable as ordinary names.
public final class Parser {
    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue",
            "def", "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in",
            "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"));
    private static final Set<String> AUG_OPS = new HashSet<String>(Arrays.asList(
            "+=", "-=", "*=", "/=", "//=", "%=", "**=", ">>=", "<<=", "&=", "|=", "^=", "@="));

    private final List<Token> toks;
    private final String file;
    private int p;

    public Parser(List<Token> toks, String file) {
        this.toks = toks;
        this.file = file;
    }

    public static Node.Module parse(String source, String file) {
        return new Parser(new Lexer(source, file).tokenize(), file).module();
    }

    // ---- token helpers ---------------------------------------------------

    private Token peek() {
        return toks.get(p);
    }

    private Token peek(int k) {
        return toks.get(Math.min(p + k, toks.size() - 1));
    }

    private Token next() {
        return toks.get(p++);
    }

    private boolean at(String s) {
        Token t = peek();
        return (t.kind == Token.Kind.OP || t.kind == Token.Kind.NAME) && t.text.equals(s);
    }

    private boolean atKind(Token.Kind k) {
        return peek().kind == k;
    }

    private boolean accept(String s) {
        if (at(s)) {
            p++;
            return true;
        }
        return false;
    }

    private Token expect(String s) {
        if (!at(s)) {
            throw err(peek(), "expected '" + s + "'");
        }
        return next();
    }

    private void expectKind(Token.Kind k) {
        if (peek().kind != k) {
            throw err(peek(), "expected " + k.name().toLowerCase(java.util.Locale.ROOT));
        }
        p++;
    }

    private PythonSyntaxError err(Token t, String msg) {
        String near = t.kind == Token.Kind.EOF ? "end of file" : t.kind == Token.Kind.NEWLINE ? "end of line"
                : "'" + t.text + "'";
        return new PythonSyntaxError(file, t.line, t.col, "invalid syntax: " + msg + " near " + near);
    }

    private String name() {
        Token t = peek();
        if (t.kind != Token.Kind.NAME || KEYWORDS.contains(t.text)) {
            throw err(t, "expected a name");
        }
        p++;
        return t.text;
    }

    private boolean atName() {
        Token t = peek();
        return t.kind == Token.Kind.NAME && !KEYWORDS.contains(t.text);
    }

    // ---- module and blocks -----------------------------------------------

    public Node.Module module() {
        List<Stmt> body = new ArrayList<Stmt>();
        while (!atKind(Token.Kind.EOF)) {
            if (atKind(Token.Kind.NEWLINE)) {
                p++;
                continue;
            }
            statement(body);
        }
        return new Node.Module(body).at(1, 1);
    }

    private List<Stmt> block() {
        List<Stmt> body = new ArrayList<Stmt>();
        if (atKind(Token.Kind.NEWLINE)) {
            p++;
            if (!atKind(Token.Kind.INDENT)) {
                throw err(peek(), "expected an indented block");
            }
            p++;
            while (!atKind(Token.Kind.DEDENT) && !atKind(Token.Kind.EOF)) {
                if (atKind(Token.Kind.NEWLINE)) {
                    p++;
                    continue;
                }
                statement(body);
            }
            accept0(Token.Kind.DEDENT);
        } else {
            simpleStatements(body);
        }
        return body;
    }

    private void accept0(Token.Kind k) {
        if (atKind(k)) {
            p++;
        }
    }

    private void statement(List<Stmt> out) {
        Token t = peek();
        if (t.kind == Token.Kind.NAME) {
            switch (t.text) {
                case "def":
                    out.add(funcDef(new ArrayList<Expr>(), false));
                    return;
                case "class":
                    out.add(classDef(new ArrayList<Expr>()));
                    return;
                case "if":
                    out.add(ifStmt());
                    return;
                case "while":
                    out.add(whileStmt());
                    return;
                case "for":
                    out.add(forStmt(false));
                    return;
                case "try":
                    out.add(tryStmt());
                    return;
                case "with":
                    out.add(withStmt(false));
                    return;
                case "async": {
                    p++;
                    Token k = peek();
                    if (k.is("def")) {
                        out.add(funcDef(new ArrayList<Expr>(), true));
                    } else if (k.is("for")) {
                        out.add(forStmt(true));
                    } else if (k.is("with")) {
                        out.add(withStmt(true));
                    } else {
                        throw err(k, "expected def, for or with after async");
                    }
                    return;
                }
                case "match":
                    if (isMatchStatement()) {
                        out.add(matchStmt());
                        return;
                    }
                    break;
                default:
                    break;
            }
        }
        if (t.is("@")) {
            List<Expr> decos = new ArrayList<Expr>();
            while (accept("@")) {
                decos.add(namedExprTest());
                expectKind(Token.Kind.NEWLINE);
            }
            if (at("class")) {
                out.add(classDef(decos));
            } else if (at("async")) {
                p++;
                out.add(funcDef(decos, true));
            } else {
                out.add(funcDef(decos, false));
            }
            return;
        }
        simpleStatements(out);
    }

    private void simpleStatements(List<Stmt> out) {
        out.add(simpleStatement());
        while (accept(";")) {
            if (atKind(Token.Kind.NEWLINE)) {
                break;
            }
            out.add(simpleStatement());
        }
        expectKind(Token.Kind.NEWLINE);
    }

    private Stmt simpleStatement() {
        Token t = peek();
        int l = t.line;
        int c = t.col;
        if (t.kind == Token.Kind.NAME) {
            switch (t.text) {
                case "pass":
                    p++;
                    return new Pass().at(l, c);
                case "break":
                    p++;
                    return new Break().at(l, c);
                case "continue":
                    p++;
                    return new Continue().at(l, c);
                case "return": {
                    p++;
                    Expr v = atStmtEnd() ? null : starExpressions();
                    return new Return(v).at(l, c);
                }
                case "raise": {
                    p++;
                    if (atStmtEnd()) {
                        return new Raise(null, null).at(l, c);
                    }
                    Expr e = test();
                    Expr cause = accept("from") ? test() : null;
                    return new Raise(e, cause).at(l, c);
                }
                case "global":
                case "nonlocal": {
                    p++;
                    List<String> names = new ArrayList<String>();
                    names.add(name());
                    while (accept(",")) {
                        names.add(name());
                    }
                    return new Global(names, "nonlocal".equals(t.text)).at(l, c);
                }
                case "del": {
                    p++;
                    List<Expr> targets = new ArrayList<Expr>();
                    targets.add(bitOr());
                    while (accept(",")) {
                        if (atStmtEnd()) {
                            break;
                        }
                        targets.add(bitOr());
                    }
                    return new Delete(targets).at(l, c);
                }
                case "assert": {
                    p++;
                    Expr test = test();
                    Expr msg = accept(",") ? test() : null;
                    return new Assert(test, msg).at(l, c);
                }
                case "import":
                    return importStmt();
                case "from":
                    return fromImport();
                case "type":
                    if (peek(1).kind == Token.Kind.NAME && (peek(2).is("=") || peek(2).is("["))) {
                        p++;
                        String n = name();
                        if (accept("[")) {
                            int depth = 1;
                            while (depth > 0) {
                                Token k = next();
                                if (k.is("[")) {
                                    depth++;
                                } else if (k.is("]")) {
                                    depth--;
                                }
                            }
                        }
                        expect("=");
                        return new TypeAlias(n, test()).at(l, c);
                    }
                    break;
                default:
                    break;
            }
        }
        return exprStatement();
    }

    private boolean atStmtEnd() {
        return atKind(Token.Kind.NEWLINE) || at(";") || atKind(Token.Kind.EOF);
    }

    private Stmt exprStatement() {
        Token t = peek();
        Expr first = at("yield") ? yieldExpr() : starExpressions();
        if (at(":") ) {
            p++;
            Expr ann = test();
            Expr value = null;
            if (accept("=")) {
                value = at("yield") ? yieldExpr() : starExpressions();
            }
            checkTarget(first, t);
            return new AnnAssign(first, ann, value).at(t.line, t.col);
        }
        if (peek().kind == Token.Kind.OP && AUG_OPS.contains(peek().text)) {
            String op = next().text;
            op = op.substring(0, op.length() - 1);
            Expr value = at("yield") ? yieldExpr() : starExpressions();
            if (!(first instanceof Name || first instanceof Attribute || first instanceof Subscript)) {
                throw err(t, "illegal expression for augmented assignment");
            }
            return new AugAssign(first, op, value).at(t.line, t.col);
        }
        if (at("=")) {
            List<Expr> targets = new ArrayList<Expr>();
            targets.add(first);
            Expr value = null;
            while (accept("=")) {
                value = at("yield") ? yieldExpr() : starExpressions();
                targets.add(value);
            }
            targets.remove(targets.size() - 1);
            for (Expr e : targets) {
                checkTarget(e, t);
            }
            return new Assign(targets, value).at(t.line, t.col);
        }
        return new ExprStmt(first).at(t.line, t.col);
    }

    private void checkTarget(Expr e, Token t) {
        if (e instanceof Name || e instanceof Attribute || e instanceof Subscript) {
            return;
        }
        if (e instanceof Starred) {
            checkTarget(((Starred) e).value, t);
            return;
        }
        if (e instanceof TupleExpr || e instanceof ListExpr) {
            List<Expr> elts = e instanceof TupleExpr ? ((TupleExpr) e).elts : ((ListExpr) e).elts;
            for (Expr x : elts) {
                checkTarget(x, t);
            }
            return;
        }
        throw new PythonSyntaxError(file, e.line, e.col, "cannot assign to expression");
    }

    // ---- imports ---------------------------------------------------------

    private String dottedName() {
        StringBuilder sb = new StringBuilder(name());
        while (accept(".")) {
            sb.append('.').append(name());
        }
        return sb.toString();
    }

    private Stmt importStmt() {
        Token t = next();
        List<Alias> names = new ArrayList<Alias>();
        do {
            Token a = peek();
            String n = dottedName();
            String as = accept("as") ? name() : null;
            names.add(new Alias(n, as).at(a.line, a.col));
        } while (accept(","));
        return new Import(names).at(t.line, t.col);
    }

    private Stmt fromImport() {
        Token t = next();
        int level = 0;
        while (at(".") || at("...")) {
            level += next().text.length();
        }
        String module = null;
        if (!at("import")) {
            module = dottedName();
        }
        expect("import");
        List<Alias> names = new ArrayList<Alias>();
        if (accept("*")) {
            names.add(new Alias("*", null).at(t.line, t.col));
        } else {
            boolean paren = accept("(");
            do {
                if (paren && at(")")) {
                    break;
                }
                Token a = peek();
                String n = name();
                String as = accept("as") ? name() : null;
                names.add(new Alias(n, as).at(a.line, a.col));
            } while (accept(","));
            if (paren) {
                expect(")");
            }
        }
        return new ImportFrom(module, level, names).at(t.line, t.col);
    }

    // ---- compound statements ---------------------------------------------

    private Stmt ifStmt() {
        Token t = next();
        Expr test = namedExprTest();
        expect(":");
        List<Stmt> body = block();
        List<Stmt> orelse = new ArrayList<Stmt>();
        if (at("elif")) {
            orelse.add(ifStmt());
        } else if (accept("else")) {
            expect(":");
            orelse = block();
        }
        return new If(test, body, orelse).at(t.line, t.col);
    }

    private Stmt whileStmt() {
        Token t = next();
        Expr test = namedExprTest();
        expect(":");
        List<Stmt> body = block();
        List<Stmt> orelse = new ArrayList<Stmt>();
        if (accept("else")) {
            expect(":");
            orelse = block();
        }
        return new While(test, body, orelse).at(t.line, t.col);
    }

    private Stmt forStmt(boolean isAsync) {
        Token t = next();
        Expr target = targetList();
        expect("in");
        Expr iter = starExpressions();
        expect(":");
        List<Stmt> body = block();
        List<Stmt> orelse = new ArrayList<Stmt>();
        if (accept("else")) {
            expect(":");
            orelse = block();
        }
        return new For(target, iter, body, orelse, isAsync).at(t.line, t.col);
    }

    /// The target of `for` and comprehensions: stops before `in`.
    private Expr targetList() {
        Token t = peek();
        List<Expr> elts = new ArrayList<Expr>();
        boolean tuple = false;
        elts.add(targetAtom());
        while (accept(",")) {
            tuple = true;
            if (at("in") || at("=")) {
                break;
            }
            elts.add(targetAtom());
        }
        Expr r = tuple ? new TupleExpr(elts).at(t.line, t.col) : elts.get(0);
        checkTarget(r, t);
        return r;
    }

    private Expr targetAtom() {
        if (at("*")) {
            Token s = next();
            return new Starred(bitOr()).at(s.line, s.col);
        }
        return bitOr();
    }

    private Stmt tryStmt() {
        Token t = next();
        expect(":");
        List<Stmt> body = block();
        List<ExceptHandler> handlers = new ArrayList<ExceptHandler>();
        boolean star = false;
        while (at("except")) {
            Token h = next();
            if (accept("*")) {
                star = true;
            }
            Expr type = null;
            String name = null;
            if (!at(":")) {
                type = test();
                if (at(",")) {
                    List<Expr> elts = new ArrayList<Expr>();
                    elts.add(type);
                    while (accept(",")) {
                        elts.add(test());
                    }
                    type = new TupleExpr(elts).at(type.line, type.col);
                }
                if (accept("as")) {
                    name = name();
                }
            }
            expect(":");
            handlers.add(new ExceptHandler(type, name, block()).at(h.line, h.col));
        }
        List<Stmt> orelse = new ArrayList<Stmt>();
        List<Stmt> fin = new ArrayList<Stmt>();
        if (accept("else")) {
            expect(":");
            orelse = block();
        }
        if (accept("finally")) {
            expect(":");
            fin = block();
        }
        if (handlers.isEmpty() && fin.isEmpty()) {
            throw err(peek(), "expected 'except' or 'finally' block");
        }
        return new Try(body, handlers, orelse, fin, star).at(t.line, t.col);
    }

    private Stmt withStmt(boolean isAsync) {
        Token t = next();
        List<WithItem> items = new ArrayList<WithItem>();
        // `with (a as b, c as d):` - parenthesised items (3.10+).
        boolean paren = false;
        if (at("(")) {
            int save = p;
            int depth = 0;
            int q = p;
            boolean hasAs = false;
            for (; q < toks.size(); q++) {
                Token k = toks.get(q);
                if (k.is("(") || k.is("[") || k.is("{")) {
                    depth++;
                } else if (k.is(")") || k.is("]") || k.is("}")) {
                    depth--;
                    if (depth == 0) {
                        break;
                    }
                } else if (depth == 1 && k.is("as")) {
                    hasAs = true;
                }
            }
            paren = hasAs && q + 1 < toks.size() && toks.get(q + 1).is(":");
            p = save;
            if (paren) {
                p++;
            }
        }
        do {
            if (paren && at(")")) {
                break;
            }
            Token it = peek();
            Expr ctx = test();
            Expr vars = null;
            if (accept("as")) {
                vars = targetAtom();
                checkTarget(vars, it);
            }
            items.add(new WithItem(ctx, vars).at(it.line, it.col));
        } while (accept(","));
        if (paren) {
            expect(")");
        }
        expect(":");
        return new With(items, block(), isAsync).at(t.line, t.col);
    }

    private Stmt funcDef(List<Expr> decorators, boolean isAsync) {
        Token t = expect("def");
        String n = name();
        if (at("[")) {
            skipTypeParams();
        }
        expect("(");
        Arguments args = parameters(")", true);
        expect(")");
        Expr returns = accept("->") ? test() : null;
        expect(":");
        List<Stmt> body = block();
        return new FunctionDef(n, args, body, decorators, returns, isAsync).at(t.line, t.col);
    }

    private void skipTypeParams() {
        expect("[");
        int depth = 1;
        while (depth > 0) {
            Token k = next();
            if (k.is("[")) {
                depth++;
            } else if (k.is("]")) {
                depth--;
            } else if (k.kind == Token.Kind.EOF) {
                throw err(k, "unterminated type parameter list");
            }
        }
    }

    private Arguments parameters(String end, boolean annotations) {
        Arguments a = new Arguments();
        boolean kwOnly = false;
        while (!at(end)) {
            Token t = peek();
            if (accept("/")) {
                a.positionalOnly = a.positional.size();
            } else if (accept("**")) {
                a.kwarg = param(annotations, true);
            } else if (accept("*")) {
                kwOnly = true;
                if (!at(",") && !at(end)) {
                    a.vararg = param(annotations, true);
                }
            } else {
                Arg arg = param(annotations, false);
                Expr def = accept("=") ? test() : null;
                if (kwOnly) {
                    a.kwonly.add(arg);
                    a.kwDefaults.add(def);
                } else {
                    a.positional.add(arg);
                    if (def != null) {
                        a.defaults.add(def);
                    } else if (!a.defaults.isEmpty()) {
                        throw err(t, "parameter without a default follows parameter with a default");
                    }
                }
            }
            if (!accept(",")) {
                break;
            }
        }
        return a;
    }

    private Arg param(boolean annotations, boolean star) {
        Token t = peek();
        String n = name();
        Expr ann = null;
        if (annotations && accept(":")) {
            ann = star && at("*") ? starredAnnotation() : test();
        }
        return new Arg(n, ann).at(t.line, t.col);
    }

    private Expr starredAnnotation() {
        Token s = next();
        return new Starred(test()).at(s.line, s.col);
    }

    private Stmt classDef(List<Expr> decorators) {
        Token t = expect("class");
        String n = name();
        if (at("[")) {
            skipTypeParams();
        }
        List<Expr> bases = new ArrayList<Expr>();
        List<Keyword> kws = new ArrayList<Keyword>();
        if (accept("(")) {
            Call c = new Call(null);
            callArgs(c);
            expect(")");
            bases.addAll(c.args);
            kws.addAll(c.keywords);
        }
        expect(":");
        return new ClassDef(n, bases, kws, block(), decorators).at(t.line, t.col);
    }

    // ---- match -----------------------------------------------------------

    private boolean isMatchStatement() {
        // `match` is a statement only when followed by an expression and ':' NEWLINE.
        Token n1 = peek(1);
        if (n1.kind == Token.Kind.NEWLINE || n1.is("=") || n1.is(".")
                || n1.kind == Token.Kind.OP && AUG_OPS.contains(n1.text) || n1.is(":") || n1.is(",")
                || n1.is(")")) {
            return false;
        }
        int depth = 0;
        for (int q = p + 1; q < toks.size(); q++) {
            Token k = toks.get(q);
            if (k.is("(") || k.is("[") || k.is("{")) {
                depth++;
            } else if (k.is(")") || k.is("]") || k.is("}")) {
                depth--;
            } else if (depth == 0 && k.is(":")) {
                return toks.get(q + 1).kind == Token.Kind.NEWLINE;
            } else if (k.kind == Token.Kind.NEWLINE || depth == 0 && k.is("=")) {
                return false;
            }
        }
        return false;
    }

    private Stmt matchStmt() {
        Token t = next();
        Expr subject = starExpressions();
        expect(":");
        expectKind(Token.Kind.NEWLINE);
        expectKind(Token.Kind.INDENT);
        List<MatchCase> cases = new ArrayList<MatchCase>();
        while (!atKind(Token.Kind.DEDENT) && !atKind(Token.Kind.EOF)) {
            Token c = peek();
            if (!c.is("case")) {
                throw err(c, "expected 'case'");
            }
            p++;
            Pattern pat = openSequencePattern();
            Expr guard = accept("if") ? namedExprTest() : null;
            expect(":");
            cases.add(new MatchCase(pat, guard, block()).at(c.line, c.col));
        }
        accept0(Token.Kind.DEDENT);
        return new Match(subject, cases).at(t.line, t.col);
    }

    private Pattern openSequencePattern() {
        Token t = peek();
        Pattern first = asPattern();
        if (!at(",")) {
            return first;
        }
        Pattern seq = new Pattern(Pattern.Kind.SEQUENCE).at(t.line, t.col);
        seq.patterns.add(first);
        while (accept(",")) {
            if (at(":") || at("if")) {
                break;
            }
            seq.patterns.add(asPattern());
        }
        return seq;
    }

    private Pattern asPattern() {
        Token t = peek();
        Pattern pat = orPattern();
        if (accept("as")) {
            Pattern as = new Pattern(Pattern.Kind.AS).at(t.line, t.col);
            as.patterns.add(pat);
            as.name = name();
            return as;
        }
        return pat;
    }

    private Pattern orPattern() {
        Token t = peek();
        Pattern first = closedPattern();
        if (!at("|")) {
            return first;
        }
        Pattern or = new Pattern(Pattern.Kind.OR).at(t.line, t.col);
        or.patterns.add(first);
        while (accept("|")) {
            or.patterns.add(closedPattern());
        }
        return or;
    }

    private Pattern closedPattern() {
        Token t = peek();
        if (accept("*")) {
            Pattern s = new Pattern(Pattern.Kind.STAR).at(t.line, t.col);
            String n = name();
            s.name = "_".equals(n) ? null : n;
            return s;
        }
        if (at("(") || at("[")) {
            String close = next().is("(") ? ")" : "]";
            Pattern seq = new Pattern(Pattern.Kind.SEQUENCE).at(t.line, t.col);
            boolean comma = false;
            while (!at(close)) {
                seq.patterns.add(asPattern());
                if (!accept(",")) {
                    break;
                }
                comma = true;
            }
            expect(close);
            if (")".equals(close) && seq.patterns.size() == 1 && !comma) {
                return seq.patterns.get(0);
            }
            return seq;
        }
        if (at("{")) {
            p++;
            Pattern m = new Pattern(Pattern.Kind.MAPPING).at(t.line, t.col);
            while (!at("}")) {
                if (accept("**")) {
                    m.name = name();
                } else {
                    m.keys.add(patternValue());
                    expect(":");
                    m.patterns.add(asPattern());
                }
                if (!accept(",")) {
                    break;
                }
            }
            expect("}");
            return m;
        }
        if (t.kind == Token.Kind.NAME && !KEYWORDS.contains(t.text) && !peek(1).is(".") && !peek(1).is("(")) {
            p++;
            if ("_".equals(t.text)) {
                return new Pattern(Pattern.Kind.WILDCARD).at(t.line, t.col);
            }
            Pattern c = new Pattern(Pattern.Kind.CAPTURE).at(t.line, t.col);
            c.name = t.text;
            return c;
        }
        if (t.is("None") || t.is("True") || t.is("False")) {
            Pattern s = new Pattern(Pattern.Kind.SINGLETON).at(t.line, t.col);
            s.value = atom();
            return s;
        }
        if (t.kind == Token.Kind.NAME && !KEYWORDS.contains(t.text)) {
            Expr v = new Name(name()).at(t.line, t.col);
            while (accept(".")) {
                Token a = peek();
                v = new Attribute(v, name()).at(a.line, a.col);
            }
            if (accept("(")) {
                Pattern c = new Pattern(Pattern.Kind.CLASS).at(t.line, t.col);
                c.value = v;
                while (!at(")")) {
                    if (peek().kind == Token.Kind.NAME && peek(1).is("=")) {
                        c.kwdAttrs.add(name());
                        expect("=");
                        c.kwdPatterns.add(asPattern());
                    } else {
                        c.patterns.add(asPattern());
                    }
                    if (!accept(",")) {
                        break;
                    }
                }
                expect(")");
                return c;
            }
            Pattern val = new Pattern(Pattern.Kind.VALUE).at(t.line, t.col);
            val.value = v;
            return val;
        }
        Pattern val = new Pattern(Pattern.Kind.VALUE).at(t.line, t.col);
        val.value = patternValue();
        return val;
    }

    /// A literal in a pattern: numbers (optionally signed, or `a + bj`), strings.
    private Expr patternValue() {
        return sum();
    }

    // ---- expressions -----------------------------------------------------

    /// `a, *b, c` as an expression statement / right-hand side.
    private Expr starExpressions() {
        Token t = peek();
        Expr first = starExpr();
        if (!at(",")) {
            return first;
        }
        List<Expr> elts = new ArrayList<Expr>();
        elts.add(first);
        while (accept(",")) {
            if (atStmtEnd() || at("=") || at(")") || at(":") || peek().kind == Token.Kind.OP
                    && AUG_OPS.contains(peek().text)) {
                break;
            }
            elts.add(starExpr());
        }
        return new TupleExpr(elts).at(t.line, t.col);
    }

    private Expr starExpr() {
        if (at("*")) {
            Token s = next();
            return new Starred(bitOr()).at(s.line, s.col);
        }
        return namedExprTest();
    }

    private Expr namedExprTest() {
        if (atName() && peek(1).is(":=")) {
            Token t = next();
            p++;
            Name target = new Name(t.text).at(t.line, t.col);
            return new NamedExpr(target, test()).at(t.line, t.col);
        }
        return test();
    }

    /// `expression` in the reference grammar.
    public Expr test() {
        if (at("lambda")) {
            return lambda(false);
        }
        Token t = peek();
        Expr e = orTest();
        if (at("if") && !atComprehensionIf()) {
            p++;
            Expr cond = orTest();
            expect("else");
            Expr other = test();
            return new IfExp(cond, e, other).at(t.line, t.col);
        }
        return e;
    }

    private boolean atComprehensionIf() {
        return false;
    }

    private Expr testNoCond() {
        if (at("lambda")) {
            return lambda(true);
        }
        return orTest();
    }

    private Expr lambda(boolean noCond) {
        Token t = expect("lambda");
        Arguments args = parameters(":", false);
        expect(":");
        Expr body = noCond ? testNoCond() : test();
        return new Lambda(args, body).at(t.line, t.col);
    }

    private Expr orTest() {
        Token t = peek();
        Expr e = andTest();
        if (!at("or")) {
            return e;
        }
        List<Expr> vals = new ArrayList<Expr>();
        vals.add(e);
        while (accept("or")) {
            vals.add(andTest());
        }
        return new BoolOp(false, vals).at(t.line, t.col);
    }

    private Expr andTest() {
        Token t = peek();
        Expr e = notTest();
        if (!at("and")) {
            return e;
        }
        List<Expr> vals = new ArrayList<Expr>();
        vals.add(e);
        while (accept("and")) {
            vals.add(notTest());
        }
        return new BoolOp(true, vals).at(t.line, t.col);
    }

    private Expr notTest() {
        if (at("not")) {
            Token t = next();
            return new UnaryOp("not", notTest()).at(t.line, t.col);
        }
        return comparison();
    }

    private String compOp() {
        Token t = peek();
        if (t.kind == Token.Kind.OP) {
            switch (t.text) {
                case "<":
                case ">":
                case "==":
                case ">=":
                case "<=":
                case "!=":
                    p++;
                    return t.text;
                default:
                    return null;
            }
        }
        if (t.is("in")) {
            p++;
            return "in";
        }
        if (t.is("not") && peek(1).is("in")) {
            p += 2;
            return "not in";
        }
        if (t.is("is")) {
            p++;
            if (accept("not")) {
                return "is not";
            }
            return "is";
        }
        return null;
    }

    private Expr comparison() {
        Token t = peek();
        Expr e = bitOr();
        String op = compOp();
        if (op == null) {
            return e;
        }
        Compare c = new Compare(e).at(t.line, t.col);
        while (op != null) {
            c.ops.add(op);
            c.comparators.add(bitOr());
            op = compOp();
        }
        return c;
    }

    private Expr bitOr() {
        Expr e = bitXor();
        while (at("|")) {
            Token t = next();
            e = new BinOp(e, "|", bitXor()).at(t.line, t.col);
        }
        return e;
    }

    private Expr bitXor() {
        Expr e = bitAnd();
        while (at("^")) {
            Token t = next();
            e = new BinOp(e, "^", bitAnd()).at(t.line, t.col);
        }
        return e;
    }

    private Expr bitAnd() {
        Expr e = shift();
        while (at("&")) {
            Token t = next();
            e = new BinOp(e, "&", shift()).at(t.line, t.col);
        }
        return e;
    }

    private Expr shift() {
        Expr e = sum();
        while (at("<<") || at(">>")) {
            Token t = next();
            e = new BinOp(e, t.text, sum()).at(t.line, t.col);
        }
        return e;
    }

    private Expr sum() {
        Expr e = term();
        while (at("+") || at("-")) {
            Token t = next();
            e = new BinOp(e, t.text, term()).at(t.line, t.col);
        }
        return e;
    }

    private Expr term() {
        Expr e = factor();
        while (at("*") || at("/") || at("//") || at("%") || at("@")) {
            Token t = next();
            e = new BinOp(e, t.text, factor()).at(t.line, t.col);
        }
        return e;
    }

    private Expr factor() {
        if (at("+") || at("-") || at("~")) {
            Token t = next();
            return new UnaryOp(t.text, factor()).at(t.line, t.col);
        }
        return power();
    }

    private Expr power() {
        Expr e = awaitPrimary();
        if (at("**")) {
            Token t = next();
            return new BinOp(e, "**", factor()).at(t.line, t.col);
        }
        return e;
    }

    private Expr awaitPrimary() {
        if (at("await")) {
            Token t = next();
            return new Await(primary()).at(t.line, t.col);
        }
        return primary();
    }

    private Expr primary() {
        Expr e = atom();
        while (true) {
            Token t = peek();
            if (accept(".")) {
                e = new Attribute(e, name()).at(t.line, t.col);
            } else if (accept("(")) {
                Call c = new Call(e).at(t.line, t.col);
                callArgs(c);
                expect(")");
                e = c;
            } else if (accept("[")) {
                e = new Subscript(e, slices()).at(t.line, t.col);
                expect("]");
            } else {
                return e;
            }
        }
    }

    private Expr slices() {
        Token t = peek();
        Expr first = sliceItem();
        if (!at(",")) {
            return first;
        }
        List<Expr> elts = new ArrayList<Expr>();
        elts.add(first);
        while (accept(",")) {
            if (at("]")) {
                break;
            }
            elts.add(sliceItem());
        }
        return new TupleExpr(elts).at(t.line, t.col);
    }

    private Expr sliceItem() {
        Token t = peek();
        Expr lower = null;
        if (!at(":")) {
            lower = starExpr();
            if (!at(":")) {
                return lower;
            }
        }
        expect(":");
        Expr upper = at(":") || at("]") || at(",") ? null : test();
        Expr step = null;
        if (accept(":")) {
            step = at("]") || at(",") ? null : test();
        }
        return new Slice(lower, upper, step).at(t.line, t.col);
    }

    private void callArgs(Call c) {
        while (!at(")")) {
            Token t = peek();
            if (accept("**")) {
                c.keywords.add(new Keyword(null, test()).at(t.line, t.col));
            } else if (accept("*")) {
                c.args.add(new Starred(test()).at(t.line, t.col));
            } else if (atName() && peek(1).is("=")) {
                String n = name();
                p++;
                c.keywords.add(new Keyword(n, test()).at(t.line, t.col));
            } else {
                Expr e = namedExprTest();
                if (at("for") || at("async")) {
                    Comp g = new Comp(Comp.Kind.GENERATOR, e, null).at(t.line, t.col);
                    compFor(g);
                    e = g;
                }
                if (!c.keywords.isEmpty() && c.keywords.get(c.keywords.size() - 1).arg != null) {
                    throw err(t, "positional argument follows keyword argument");
                }
                c.args.add(e);
            }
            if (!accept(",")) {
                break;
            }
        }
    }

    private void compFor(Comp comp) {
        while (at("for") || at("async")) {
            boolean isAsync = accept("async");
            Token t = expect("for");
            Expr target = targetList();
            expect("in");
            Expr iter = orTest();
            Comprehension g = new Comprehension(target, iter, isAsync).at(t.line, t.col);
            while (at("if")) {
                p++;
                g.ifs.add(testNoCond());
            }
            comp.generators.add(g);
        }
    }

    private Expr yieldExpr() {
        Token t = expect("yield");
        if (accept("from")) {
            return new Yield(test(), true).at(t.line, t.col);
        }
        if (atStmtEnd() || at(")") || at("]") || at("}")) {
            return new Yield(null, false).at(t.line, t.col);
        }
        return new Yield(starExpressions(), false).at(t.line, t.col);
    }

    private Expr atom() {
        Token t = peek();
        switch (t.kind) {
            case NUMBER: {
                p++;
                String s = t.text;
                String l = s.toLowerCase(java.util.Locale.ROOT);
                // An imaginary literal stays FLOAT with its `j`; the code
                // generators check the suffix.
                boolean isFloat = !l.startsWith("0x") && (l.indexOf('.') >= 0 || l.indexOf('e') >= 0
                        || l.endsWith("j"));
                return new Constant(isFloat ? Constant.Kind.FLOAT : Constant.Kind.INT, s).at(t.line, t.col);
            }
            case STRING:
                return strings();
            case NAME: {
                switch (t.text) {
                    case "None":
                        p++;
                        return new Constant(Constant.Kind.NONE, null).at(t.line, t.col);
                    case "True":
                        p++;
                        return new Constant(Constant.Kind.TRUE, null).at(t.line, t.col);
                    case "False":
                        p++;
                        return new Constant(Constant.Kind.FALSE, null).at(t.line, t.col);
                    default:
                        return new Name(name()).at(t.line, t.col);
                }
            }
            case OP:
                break;
            default:
                throw err(t, "expected an expression");
        }
        if (accept("...")) {
            return new Constant(Constant.Kind.ELLIPSIS, null).at(t.line, t.col);
        }
        if (accept("(")) {
            if (accept(")")) {
                return new TupleExpr(new ArrayList<Expr>()).at(t.line, t.col);
            }
            if (at("yield")) {
                Expr y = yieldExpr();
                expect(")");
                return y;
            }
            Expr first = starExpr();
            if (at("for") || at("async")) {
                Comp g = new Comp(Comp.Kind.GENERATOR, first, null).at(t.line, t.col);
                compFor(g);
                expect(")");
                return g;
            }
            if (!at(",")) {
                expect(")");
                return first;
            }
            List<Expr> elts = new ArrayList<Expr>();
            elts.add(first);
            while (accept(",")) {
                if (at(")")) {
                    break;
                }
                elts.add(starExpr());
            }
            expect(")");
            return new TupleExpr(elts).at(t.line, t.col);
        }
        if (accept("[")) {
            List<Expr> elts = new ArrayList<Expr>();
            if (accept("]")) {
                return new ListExpr(elts).at(t.line, t.col);
            }
            Expr first = starExpr();
            if (at("for") || at("async")) {
                Comp g = new Comp(Comp.Kind.LIST, first, null).at(t.line, t.col);
                compFor(g);
                expect("]");
                return g;
            }
            elts.add(first);
            while (accept(",")) {
                if (at("]")) {
                    break;
                }
                elts.add(starExpr());
            }
            expect("]");
            return new ListExpr(elts).at(t.line, t.col);
        }
        if (accept("{")) {
            if (accept("}")) {
                return new DictExpr().at(t.line, t.col);
            }
            if (accept("**")) {
                DictExpr d = new DictExpr().at(t.line, t.col);
                d.keys.add(null);
                d.values.add(bitOr());
                return dictRest(d);
            }
            Expr first = starExpr();
            if (accept(":")) {
                Expr v = test();
                if (at("for") || at("async")) {
                    Comp g = new Comp(Comp.Kind.DICT, first, v).at(t.line, t.col);
                    compFor(g);
                    expect("}");
                    return g;
                }
                DictExpr d = new DictExpr().at(t.line, t.col);
                d.keys.add(first);
                d.values.add(v);
                return dictRest(d);
            }
            if (at("for") || at("async")) {
                Comp g = new Comp(Comp.Kind.SET, first, null).at(t.line, t.col);
                compFor(g);
                expect("}");
                return g;
            }
            List<Expr> elts = new ArrayList<Expr>();
            elts.add(first);
            while (accept(",")) {
                if (at("}")) {
                    break;
                }
                elts.add(starExpr());
            }
            expect("}");
            return new SetExpr(elts).at(t.line, t.col);
        }
        throw err(t, "expected an expression");
    }

    private Expr dictRest(DictExpr d) {
        while (accept(",")) {
            if (at("}")) {
                break;
            }
            if (accept("**")) {
                d.keys.add(null);
                d.values.add(bitOr());
            } else {
                d.keys.add(test());
                expect(":");
                d.values.add(test());
            }
        }
        expect("}");
        return d;
    }

    /// Adjacent string literals concatenate; any f-string makes the whole a
    /// JoinedStr. Mixing bytes and str is an error, as in CPython.
    private Expr strings() {
        Token first = peek();
        boolean bytes = first.prefix.indexOf('b') >= 0;
        JoinedStr joined = new JoinedStr().at(first.line, first.col);
        boolean anyF = false;
        StringBuilder plain = new StringBuilder();
        while (atKind(Token.Kind.STRING)) {
            Token t = next();
            if ((t.prefix.indexOf('b') >= 0) != bytes) {
                throw err(t, "cannot mix bytes and nonbytes literals");
            }
            if (t.prefix.indexOf('f') >= 0) {
                if (plain.length() > 0) {
                    joined.parts.add(new Constant(Constant.Kind.STR, plain.toString()).at(t.line, t.col));
                    plain.setLength(0);
                }
                anyF = true;
                fstring(t, joined);
            } else {
                plain.append(t.value);
            }
        }
        if (!anyF) {
            return new Constant(bytes ? Constant.Kind.BYTES : Constant.Kind.STR, plain.toString())
                    .at(first.line, first.col);
        }
        if (plain.length() > 0) {
            joined.parts.add(new Constant(Constant.Kind.STR, plain.toString()).at(first.line, first.col));
        }
        return joined;
    }

    private void fstring(Token t, JoinedStr out) {
        boolean raw = t.prefix.indexOf('r') >= 0;
        fstringBody(t.value, raw, t, out);
    }

    private void fstringBody(String body, boolean raw, Token t, JoinedStr out) {
        StringBuilder lit = new StringBuilder();
        int n = body.length();
        int i = 0;
        while (i < n) {
            char c = body.charAt(i);
            if (c == '{' && i + 1 < n && body.charAt(i + 1) == '{') {
                lit.append('{');
                i += 2;
                continue;
            }
            if (c == '}' && i + 1 < n && body.charAt(i + 1) == '}') {
                lit.append('}');
                i += 2;
                continue;
            }
            if (c == '}') {
                throw new PythonSyntaxError(file, t.line, t.col, "f-string: single '}' is not allowed");
            }
            if (c != '{') {
                if (c == '\\' && !raw && i + 1 < n) {
                    int j = i + 2;
                    char e = body.charAt(i + 1);
                    if (e == 'x') {
                        j = i + 4;
                    } else if (e == 'u') {
                        j = i + 6;
                    } else if (e == 'U') {
                        j = i + 10;
                    } else if (e >= '0' && e <= '7') {
                        j = i + 2;
                        while (j < n && j < i + 4 && body.charAt(j) >= '0' && body.charAt(j) <= '7') {
                            j++;
                        }
                    }
                    lit.append(Lexer.decode(body.substring(i, Math.min(j, n)), false, t.line, t.col));
                    i = Math.min(j, n);
                    continue;
                }
                lit.append(c);
                i++;
                continue;
            }
            if (lit.length() > 0) {
                out.parts.add(new Constant(Constant.Kind.STR, lit.toString()).at(t.line, t.col));
                lit.setLength(0);
            }
            // Find the end of the replacement field and its top-level '!' / ':'.
            int depth = 0;
            int j = i + 1;
            int exprEnd = -1;
            int convAt = -1;
            int specAt = -1;
            while (j < n) {
                char d = body.charAt(j);
                if (d == '\'' || d == '"') {
                    j = skipString(body, j);
                    continue;
                }
                if (d == '#' && specAt < 0) {
                    while (j < n && body.charAt(j) != '\n') {
                        j++;
                    }
                    continue;
                }
                if (d == '(' || d == '[' || d == '{') {
                    depth++;
                } else if (d == ')' || d == ']') {
                    depth--;
                } else if (d == '}') {
                    if (depth == 0) {
                        break;
                    }
                    depth--;
                } else if (depth == 0 && specAt < 0 && d == '!' && j + 1 < n && body.charAt(j + 1) != '=') {
                    if (exprEnd < 0) {
                        exprEnd = j;
                    }
                    convAt = j;
                } else if (depth == 0 && d == ':' && specAt < 0) {
                    if (exprEnd < 0) {
                        exprEnd = j;
                    }
                    specAt = j;
                }
                j++;
            }
            if (j >= n) {
                throw new PythonSyntaxError(file, t.line, t.col, "f-string: expecting '}'");
            }
            if (exprEnd < 0) {
                exprEnd = j;
            }
            String exprText = body.substring(i + 1, exprEnd);
            String trimmed = exprText.trim();
            boolean debug = false;
            if (trimmed.endsWith("=") && !trimmed.endsWith("==") && !trimmed.endsWith("!=")
                    && !trimmed.endsWith("<=") && !trimmed.endsWith(">=")) {
                debug = true;
                out.parts.add(new Constant(Constant.Kind.STR, exprText).at(t.line, t.col));
                exprText = exprText.substring(0, exprText.lastIndexOf('='));
            }
            Expr value = subExpression(exprText, t);
            char conv = 0;
            if (convAt >= 0) {
                conv = body.charAt(convAt + 1);
                if (conv != 'r' && conv != 's' && conv != 'a') {
                    throw new PythonSyntaxError(file, t.line, t.col, "f-string: invalid conversion character");
                }
            } else if (debug && specAt < 0) {
                conv = 'r';
            }
            JoinedStr spec = null;
            if (specAt >= 0) {
                spec = new JoinedStr().at(t.line, t.col);
                fstringBody(body.substring(specAt + 1, j), true, t, spec);
            }
            out.parts.add(new FormattedValue(value, conv, spec).at(t.line, t.col));
            i = j + 1;
        }
        if (lit.length() > 0) {
            out.parts.add(new Constant(Constant.Kind.STR, lit.toString()).at(t.line, t.col));
        }
    }

    private static int skipString(String s, int i) {
        char q = s.charAt(i);
        boolean triple = s.startsWith("" + q + q + q, i);
        int j = i + (triple ? 3 : 1);
        while (j < s.length()) {
            char c = s.charAt(j);
            if (c == '\\') {
                j += 2;
                continue;
            }
            if (c == q && (!triple || s.startsWith("" + q + q + q, j))) {
                return j + (triple ? 3 : 1);
            }
            j++;
        }
        return j;
    }

    private Expr subExpression(String text, Token t) {
        List<Token> sub = new Lexer("(" + text + ")", file).tokenize();
        List<Token> fixed = new ArrayList<Token>();
        for (Token k : sub) {
            fixed.add(new Token(k.kind, k.text, t.line + k.line - 1, k.line == 1 ? t.col : k.col, k.value, k.prefix));
        }
        Parser ps = new Parser(fixed, file);
        Expr e = ps.starExpressions();
        ps.expectKind(Token.Kind.NEWLINE);
        return e;
    }

    /// Unused positional helper for future grammar extensions.
    static Node unused() {
        return null;
    }
}

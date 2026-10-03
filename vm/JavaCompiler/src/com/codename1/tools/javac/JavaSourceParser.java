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
package com.codename1.tools.javac;

import com.codename1.tools.javac.Token.Kind;
import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for Java 17 syntax, plus pattern {@code switch} and
 * record patterns. Ambiguities (cast vs parenthesised expression vs lambda,
 * declaration vs expression, generic method references) are settled by
 * speculative parsing: remember the position, try one reading, rewind on failure.
 */
final class JavaSourceParser {
    static final int ACC_PUBLIC = 0x0001;
    static final int ACC_PRIVATE = 0x0002;
    static final int ACC_PROTECTED = 0x0004;
    static final int ACC_STATIC = 0x0008;
    static final int ACC_FINAL = 0x0010;
    static final int ACC_SYNCHRONIZED = 0x0020;
    static final int ACC_VOLATILE = 0x0040;
    static final int ACC_TRANSIENT = 0x0080;
    static final int ACC_NATIVE = 0x0100;
    static final int ACC_ABSTRACT = 0x0400;
    static final int ACC_STRICT = 0x0800;
    /** Pseudo flags, above the class-file range. */
    static final int DEFAULT = 1 << 20;
    static final int SEALED = 1 << 21;
    static final int NON_SEALED = 1 << 22;

    private final Source source;
    private final Log log;
    /** Non-null in script mode: top-level statements and methods are wrapped into this class. */
    ScriptSpec script;
    private final List<Token> tokens;
    private int p;

    /** Set while speculating: errors become silent failures. */
    private int speculating;

    private static final class Failure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Failure() {
            super("speculative parse failed");
        }
    }

    private static final Failure FAILURE = new Failure();

    JavaSourceParser(Source source, Log log) {
        this.source = source;
        this.log = log;
        this.tokens = new Lexer(source, log).tokenize();
    }

    // ------------------------------------------------------------------ token helpers

    private Token tok() {
        return tokens.get(p);
    }

    private Token peek(int ahead) {
        int at = p + ahead;
        return at < tokens.size() ? tokens.get(at) : tokens.get(tokens.size() - 1);
    }

    private Kind kind() {
        return tokens.get(p).kind;
    }

    private boolean is(Kind k) {
        return tokens.get(p).kind == k;
    }

    private boolean isIdent(String name) {
        Token t = tokens.get(p);
        return t.kind == Kind.IDENT && t.name.equals(name);
    }

    private Token advance() {
        Token t = tokens.get(p);
        if (t.kind != Kind.EOF) {
            p++;
        }
        return t;
    }

    private boolean accept(Kind k) {
        if (is(k)) {
            p++;
            return true;
        }
        return false;
    }

    private RuntimeException error(int pos, String message) {
        if (speculating > 0) {
            return FAILURE;
        }
        log.error(source, pos, message);
        return new CompileError(message);
    }

    private Token expect(Kind k) {
        if (is(k)) {
            return advance();
        }
        // '>' may be the first half of '>>', '>>>', '>=', '>>=' or '>>>='.
        if (k == Kind.GT && splitGreater()) {
            return advance();
        }
        throw error(tok().pos, "'" + describe(k) + "' expected" + found());
    }

    private String found() {
        Token t = tok();
        return t.kind == Kind.EOF ? " but reached end of file" : ", found '" + t.name + "'";
    }

    private static String describe(Kind k) {
        switch (k) {
            case IDENT: return "<identifier>";
            case SEMI: return ";";
            case LPAREN: return "(";
            case RPAREN: return ")";
            case LBRACE: return "{";
            case RBRACE: return "}";
            case LBRACKET: return "[";
            case RBRACKET: return "]";
            case GT: return ">";
            case LT: return "<";
            case COLON: return ":";
            case COMMA: return ",";
            case DOT: return ".";
            case EQ: return "=";
            case ARROW: return "->";
            default: return asciiLower(k.toString());
        }
    }

    /** Token names are ASCII; toLowerCase would fold them by the default locale (a dotless i in Turkish). */
    private static String asciiLower(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return b.toString();
    }

    private boolean splitGreater() {
        Token t = tok();
        String rest;
        switch (t.kind) {
            case GTGT: rest = ">"; break;
            case GTGTGT: rest = ">>"; break;
            case GTEQ: rest = "="; break;
            case GTGTEQ: rest = ">="; break;
            case GTGTGTEQ: rest = ">>="; break;
            default: return false;
        }
        Kind restKind = rest.equals(">") ? Kind.GT : rest.equals(">>") ? Kind.GTGT : rest.equals("=") ? Kind.EQ
                : rest.equals(">=") ? Kind.GTEQ : Kind.GTGTEQ;
        tokens.set(p, new Token(Kind.GT, t.pos, t.pos + 1, ">", null));
        tokens.add(p + 1, new Token(restKind, t.pos + 1, t.end, rest, null));
        return true;
    }

    private String ident() {
        Token t = tok();
        if (t.kind == Kind.IDENT) {
            p++;
            return t.name;
        }
        throw error(t.pos, "<identifier> expected" + found());
    }

    private <T extends Tree> T at(T tree, int pos) {
        tree.pos = pos;
        return tree;
    }

    /** Runs a parse speculatively; returns null (and rewinds) if it fails. */
    private interface Attempt<T> {
        T run();
    }

    /** Runs {@code body} speculatively; on failure or a null result, rewinds and answers null. */
    private <T> T attempt(Attempt<T> body) {
        int save = p;
        speculating++;
        try {
            T result = body.run();
            if (result == null) {
                p = save;
            }
            return result;
        } catch (Failure f) {
            p = save;
            return null;
        } catch (CompileError e) {
            p = save;
            return null;
        } finally {
            speculating--;
        }
    }

    // ------------------------------------------------------------------ compilation unit

    Tree.CompilationUnit parseCompilationUnit() {
        Tree.CompilationUnit unit = at(new Tree.CompilationUnit(), 0);
        unit.source = source;
        try {
            skipAnnotationsBeforePackage();
            if (is(Kind.PACKAGE)) {
                advance();
                unit.packageName = qualifiedName();
                expect(Kind.SEMI);
            }
            while (is(Kind.IMPORT)) {
                unit.imports.add(importDeclaration());
            }
        } catch (CompileError e) {
            skipTo(Kind.SEMI);
        }
        if (script != null) {
            parseScriptBody(unit);
            return unit;
        }
        while (!is(Kind.EOF)) {
            if (accept(Kind.SEMI)) {
                continue;
            }
            try {
                Tree.Modifiers mods = modifiers();
                unit.types.add(classOrInterface(mods));
            } catch (CompileError e) {
                recoverToTopLevel();
            }
        }
        return unit;
    }

    // ------------------------------------------------------------------ script mode

    /**
     * Script mode: after the imports come type declarations, methods and statements in
     * any order. Type declarations stay top-level types; methods and statements become
     * members of a generated class:
     * <pre>
     * public class &lt;className&gt; implements &lt;interfaceName&gt; {
     *     &lt;methods&gt;
     *     public Object &lt;methodName&gt;(&lt;paramType&gt; &lt;paramName&gt;) throws Throwable {
     *         &lt;statements&gt;
     *         return new Object[] {&lt;valuesMarker&gt;, &lt;trailing expression or null&gt;, &lt;top-level locals&gt;...};
     *     }
     * }
     * </pre>
     * A final bare expression ({@code root} or {@code root;}) is the script's value. The
     * locals let a caller find what the script built when it ends without one.
     */
    private void parseScriptBody(Tree.CompilationUnit unit) {
        ScriptSpec spec = script;
        Tree.ClassDecl cls = at(new Tree.ClassDecl(), 0);
        cls.mods = at(new Tree.Modifiers(), 0);
        cls.mods.flags = ACC_PUBLIC;
        cls.name = spec.className;
        if (spec.interfaceName != null) {
            cls.implementing.add(qualifiedType(spec.interfaceName, 0));
        }
        List<Tree> stats = new ArrayList<Tree>();
        Tree value = null;
        int lastPos = 0;
        while (!is(Kind.EOF)) {
            if (accept(Kind.SEMI)) {
                continue;
            }
            int start = p;
            lastPos = tok().pos;
            try {
                if (value != null) {
                    throw error(value.pos, "not a statement");
                }
                if (is(Kind.IMPORT)) {
                    // A script may import anywhere at its top level; the import applies to all of it.
                    unit.imports.add(importDeclaration());
                } else if (scriptTypeDeclarationAhead()) {
                    unit.types.add(classOrInterface(modifiers()));
                } else if (scriptMethodAhead()) {
                    member(cls);
                } else {
                    value = scriptStatement(stats);
                }
            } catch (CompileError e) {
                recoverStatement(start);
            }
        }
        Tree.MethodDecl build = at(new Tree.MethodDecl(), 0);
        build.mods = at(new Tree.Modifiers(), 0);
        build.mods.flags = ACC_PUBLIC;
        build.name = spec.methodName;
        build.returnType = qualifiedType("java.lang.Object", 0);
        Tree.VarDef param = at(new Tree.VarDef(), 0);
        param.mods = at(new Tree.Modifiers(), 0);
        param.vartype = qualifiedType(spec.paramType, 0);
        param.name = spec.paramName;
        build.params.add(param);
        build.thrown.add(qualifiedType("java.lang.Throwable", 0));
        Tree.Block body = at(new Tree.Block(), 0);
        body.stats.addAll(stats);
        // return new Object[] {marker, value, locals...} -- synthetic (negative position):
        // Flow does not call it unreachable after a script that returns on its own.
        int synth = -1 - Math.max(0, lastPos);
        Tree.NewArray values = at(new Tree.NewArray(), synth);
        values.elemType = qualifiedType("java.lang.Object", synth);
        values.elems = new ArrayList<Tree>();
        Tree.Literal marker = at(new Tree.Literal(), synth);
        marker.kind = Kind.STRING_LITERAL;
        marker.value = spec.valuesMarker;
        values.elems.add(marker);
        if (value != null) {
            values.elems.add(value);
        } else {
            Tree.Literal nul = at(new Tree.Literal(), synth);
            nul.kind = Kind.NULL;
            values.elems.add(nul);
        }
        for (Tree s : stats) {
            if (s instanceof Tree.VarDef && ((Tree.VarDef) s).init != null) {
                Tree.Ident id = at(new Tree.Ident(), synth);
                id.name = ((Tree.VarDef) s).name;
                values.elems.add(id);
            }
        }
        Tree.Return ret = at(new Tree.Return(), synth);
        ret.expr = values;
        body.stats.add(ret);
        build.body = body;
        cls.members.add(build);
        unit.types.add(0, cls);
    }

    /** {@code a.b.C} as a type tree. */
    private Tree qualifiedType(String name, int pos) {
        Tree t = null;
        int start = 0;
        for (int i = 0; i <= name.length(); i++) {
            if (i == name.length() || name.charAt(i) == '.') {
                String part = name.substring(start, i);
                start = i + 1;
                if (t == null) {
                    Tree.Ident id = at(new Tree.Ident(), pos);
                    id.name = part;
                    t = id;
                } else {
                    Tree.Select sel = at(new Tree.Select(), pos);
                    sel.selected = t;
                    sel.name = part;
                    t = sel;
                }
            }
        }
        return t;
    }

    /** Modifiers/annotations then class, interface, enum, record or @interface. */
    private boolean scriptTypeDeclarationAhead() {
        int k = 0;
        while (true) {
            Token t = peek(k);
            switch (t.kind) {
                case CLASS:
                case INTERFACE:
                case ENUM:
                    return true;
                case AT:
                    if (peek(k + 1).kind == Kind.INTERFACE) {
                        return true;
                    }
                    // Skip an annotation: @Name or @a.b.Name, optionally (...).
                    k += 2;
                    while (peek(k).kind == Kind.DOT) {
                        k += 2;
                    }
                    if (peek(k).kind == Kind.LPAREN) {
                        int close = matchingParen(p + k);
                        if (close < 0) {
                            return false;
                        }
                        k = close - p + 1;
                    }
                    continue;
                case PUBLIC: case PRIVATE: case PROTECTED: case STATIC: case FINAL: case ABSTRACT: case STRICTFP:
                    k++;
                    continue;
                case IDENT:
                    if ("record".equals(t.name) && peek(k + 1).kind == Kind.IDENT
                            && (peek(k + 2).kind == Kind.LPAREN || peek(k + 2).kind == Kind.LT)) {
                        return true;
                    }
                    if ("sealed".equals(t.name) || "non".equals(t.name) && peek(k + 1).kind == Kind.SUB) {
                        k += "non".equals(t.name) ? 3 : 1;
                        continue;
                    }
                    return false;
                default:
                    return false;
            }
        }
    }

    /** [modifiers] [type parameters] (void | Type) name ( ... ) followed by { or throws. */
    private boolean scriptMethodAhead() {
        final int start = p;
        Boolean yes = attempt(new Attempt<Boolean>() {
            @Override
            public Boolean run() {
                modifiers();
                if (is(Kind.LT)) {
                    typeParameters(new ArrayList<Tree.TypeParameter>());
                }
                if (is(Kind.VOID)) {
                    advance();
                } else {
                    type();
                }
                if (!is(Kind.IDENT) || peek(1).kind != Kind.LPAREN) {
                    return Boolean.FALSE;
                }
                int close = matchingParen(p + 1);
                if (close < 0) {
                    return Boolean.FALSE;
                }
                Kind after = tokens.get(close + 1).kind;
                return after == Kind.LBRACE || after == Kind.THROWS || after == Kind.LBRACKET ? Boolean.TRUE : Boolean.FALSE;
            }
        });
        p = start;
        return Boolean.TRUE.equals(yes);
    }

    /**
     * One top-level script statement into {@code stats}. Returns the expression when it
     * is a bare value ({@code root} / {@code root;} at the end), else null.
     */
    private Tree scriptStatement(List<Tree> stats) {
        Kind k = kind();
        boolean expressionStart = k == Kind.IDENT && !looksLikeLocalVariable() && peek(1).kind != Kind.COLON
                && !(isIdent("yield") && yieldStatementAhead())
                || k == Kind.THIS || k == Kind.SUPER || k == Kind.NEW || k == Kind.LPAREN || k == Kind.STRING_LITERAL
                || k == Kind.INT_LITERAL || k == Kind.LONG_LITERAL || k == Kind.FLOAT_LITERAL || k == Kind.DOUBLE_LITERAL
                || k == Kind.CHAR_LITERAL || k == Kind.TRUE || k == Kind.FALSE || k == Kind.NULL
                || k == Kind.PLUSPLUS || k == Kind.SUBSUB || k == Kind.BANG || k == Kind.SUB || k == Kind.TILDE;
        if (!expressionStart) {
            blockStatement(stats);
            return null;
        }
        int pos = tok().pos;
        Tree e = expression();
        boolean statementExpr = e instanceof Tree.Assign || e instanceof Tree.CompoundAssign || e instanceof Tree.MethodCall
                || e instanceof Tree.NewClass || e instanceof Tree.Unary
                && (((Tree.Unary) e).op == Kind.PLUSPLUS || ((Tree.Unary) e).op == Kind.SUBSUB);
        if (is(Kind.EOF) || is(Kind.SEMI) && remainingIsEmpty(p + 1)) {
            accept(Kind.SEMI);
            // The last line's value: a bare expression, or a final `new X(...);`
            // (a method call may be void, so it stays a statement).
            if (!statementExpr || e instanceof Tree.NewClass) {
                return e;
            }
        } else {
            expect(Kind.SEMI);
            if (!statementExpr) {
                throw error(e.pos, "not a statement");
            }
        }
        Tree.ExpressionStatement s = at(new Tree.ExpressionStatement(), pos);
        s.expr = e;
        stats.add(s);
        return null;
    }

    /** Only semicolons from index k to the end. */
    private boolean remainingIsEmpty(int k) {
        for (int i = k; i < tokens.size(); i++) {
            Kind kk = tokens.get(i).kind;
            if (kk == Kind.EOF) {
                return true;
            }
            if (kk != Kind.SEMI) {
                return false;
            }
        }
        return true;
    }

    private Tree.Import importDeclaration() {
        Tree.Import imp = at(new Tree.Import(), expect(Kind.IMPORT).pos);
        imp.isStatic = accept(Kind.STATIC);
        StringBuilder name = new StringBuilder(ident());
        while (accept(Kind.DOT)) {
            if (accept(Kind.STAR)) {
                imp.onDemand = true;
                break;
            }
            name.append('.').append(ident());
        }
        imp.name = name.toString();
        expect(Kind.SEMI);
        return imp;
    }

    private void skipAnnotationsBeforePackage() {
        int save = p;
        while (is(Kind.AT) && !(peek(1).kind == Kind.INTERFACE)) {
            annotation();
        }
        if (!is(Kind.PACKAGE)) {
            p = save;
        }
    }

    private void recoverToTopLevel() {
        int depth = 0;
        while (!is(Kind.EOF)) {
            if (is(Kind.LBRACE)) {
                depth++;
            } else if (is(Kind.RBRACE)) {
                depth--;
                if (depth <= 0) {
                    advance();
                    return;
                }
            }
            advance();
        }
    }

    private void skipTo(Kind k) {
        while (!is(Kind.EOF) && !is(k)) {
            advance();
        }
        accept(k);
    }

    String qualifiedName() {
        StringBuilder b = new StringBuilder(ident());
        while (is(Kind.DOT) && peek(1).kind == Kind.IDENT) {
            advance();
            b.append('.').append(ident());
        }
        return b.toString();
    }

    // ------------------------------------------------------------------ modifiers and annotations

    private Tree.Modifiers modifiers() {
        Tree.Modifiers mods = at(new Tree.Modifiers(), tok().pos);
        while (true) {
            int flag = 0;
            switch (kind()) {
                case PUBLIC: flag = ACC_PUBLIC; break;
                case PRIVATE: flag = ACC_PRIVATE; break;
                case PROTECTED: flag = ACC_PROTECTED; break;
                case STATIC: flag = ACC_STATIC; break;
                case FINAL: flag = ACC_FINAL; break;
                case ABSTRACT: flag = ACC_ABSTRACT; break;
                case NATIVE: flag = ACC_NATIVE; break;
                case SYNCHRONIZED: flag = ACC_SYNCHRONIZED; break;
                case TRANSIENT: flag = ACC_TRANSIENT; break;
                case VOLATILE: flag = ACC_VOLATILE; break;
                case STRICTFP: flag = ACC_STRICT; break;
                case DEFAULT:
                    // `default` is a modifier only on an interface method, never `default:`/`default ->`.
                    if (peek(1).kind == Kind.COLON || peek(1).kind == Kind.ARROW) {
                        return mods;
                    }
                    flag = DEFAULT;
                    break;
                case AT:
                    if (peek(1).kind == Kind.INTERFACE) {
                        return mods;
                    }
                    mods.annotations.add(annotation());
                    continue;
                case IDENT:
                    if (isIdent("sealed") && startsDeclarationAfterModifier(1)) {
                        flag = SEALED;
                    } else if (isIdent("non") && peek(1).kind == Kind.SUB && peek(2).kind == Kind.IDENT
                            && "sealed".equals(peek(2).name)) {
                        p += 2;
                        flag = NON_SEALED;
                    } else {
                        return mods;
                    }
                    break;
                default:
                    return mods;
            }
            if ((mods.flags & flag) != 0) {
                throw error(tok().pos, "repeated modifier");
            }
            mods.flags |= flag;
            advance();
        }
    }

    /** True when the token {@code ahead} positions on could begin a declaration. */
    private boolean startsDeclarationAfterModifier(int ahead) {
        Kind k = peek(ahead).kind;
        return k == Kind.CLASS || k == Kind.INTERFACE || k == Kind.ABSTRACT || k == Kind.PUBLIC || k == Kind.PRIVATE
                || k == Kind.PROTECTED || k == Kind.STATIC || k == Kind.FINAL || k == Kind.STRICTFP || k == Kind.AT
                || k == Kind.IDENT && ("record".equals(peek(ahead).name) || "non".equals(peek(ahead).name));
    }

    private Tree.Annotation annotation() {
        Tree.Annotation a = at(new Tree.Annotation(), expect(Kind.AT).pos);
        Tree name = at(new Tree.Ident(), tok().pos);
        ((Tree.Ident) name).name = ident();
        while (is(Kind.DOT) && peek(1).kind == Kind.IDENT) {
            advance();
            Tree.Select sel = at(new Tree.Select(), tok().pos);
            sel.selected = name;
            sel.name = ident();
            name = sel;
        }
        if (accept(Kind.LPAREN)) {
            if (!is(Kind.RPAREN)) {
                do {
                    if (is(Kind.IDENT) && peek(1).kind == Kind.EQ) {
                        advance();
                        advance();
                    }
                    a.args.add(elementValue());
                } while (accept(Kind.COMMA));
            }
            expect(Kind.RPAREN);
        }
        return a;
    }

    private Tree elementValue() {
        if (is(Kind.AT)) {
            return annotation();
        }
        if (is(Kind.LBRACE)) {
            Tree.NewArray array = at(new Tree.NewArray(), advance().pos);
            array.elems = new ArrayList<Tree>();
            while (!is(Kind.RBRACE)) {
                array.elems.add(elementValue());
                if (!accept(Kind.COMMA)) {
                    break;
                }
            }
            expect(Kind.RBRACE);
            return array;
        }
        return conditionalExpression();
    }

    // ------------------------------------------------------------------ classes

    private boolean isRecordStart() {
        return isIdent("record") && peek(1).kind == Kind.IDENT
                && (peek(2).kind == Kind.LPAREN || peek(2).kind == Kind.LT);
    }

    private Tree.ClassDecl classOrInterface(Tree.Modifiers mods) {
        Tree.ClassDecl decl = at(new Tree.ClassDecl(), tok().pos);
        decl.mods = mods;
        if (accept(Kind.CLASS)) {
            decl.kind = Tree.ClassKind.CLASS;
        } else if (accept(Kind.INTERFACE)) {
            decl.kind = Tree.ClassKind.INTERFACE;
        } else if (accept(Kind.ENUM)) {
            decl.kind = Tree.ClassKind.ENUM;
        } else if (is(Kind.AT) && peek(1).kind == Kind.INTERFACE) {
            advance();
            advance();
            decl.kind = Tree.ClassKind.ANNOTATION;
        } else if (isRecordStart()) {
            advance();
            decl.kind = Tree.ClassKind.RECORD;
        } else {
            throw error(tok().pos, "class, interface, enum, or record expected" + found());
        }
        decl.pos = tok().pos;
        decl.name = ident();
        if (is(Kind.LT)) {
            typeParameters(decl.typeParams);
        }
        if (decl.kind == Tree.ClassKind.RECORD) {
            expect(Kind.LPAREN);
            if (!is(Kind.RPAREN)) {
                do {
                    Tree.VarDef component = at(new Tree.VarDef(), tok().pos);
                    component.mods = modifiers();
                    component.vartype = type();
                    if (accept(Kind.ELLIPSIS)) {
                        Tree.ArrayTypeTree arr = at(new Tree.ArrayTypeTree(), component.vartype.pos);
                        arr.elem = component.vartype;
                        component.vartype = arr;
                    }
                    component.pos = tok().pos;
                    component.name = ident();
                    decl.recordComponents.add(component);
                } while (accept(Kind.COMMA));
            }
            expect(Kind.RPAREN);
        }
        if (accept(Kind.EXTENDS)) {
            if (decl.kind == Tree.ClassKind.INTERFACE) {
                do {
                    decl.implementing.add(type());
                } while (accept(Kind.COMMA));
            } else {
                decl.extending = type();
            }
        }
        if (accept(Kind.IMPLEMENTS)) {
            do {
                decl.implementing.add(type());
            } while (accept(Kind.COMMA));
        }
        if (isIdent("permits")) {
            advance();
            do {
                decl.permitting.add(type());
            } while (accept(Kind.COMMA));
        }
        classBody(decl);
        return decl;
    }

    private void typeParameters(List<Tree.TypeParameter> into) {
        expect(Kind.LT);
        do {
            while (is(Kind.AT)) {
                annotation();
            }
            Tree.TypeParameter tp = at(new Tree.TypeParameter(), tok().pos);
            tp.name = ident();
            if (accept(Kind.EXTENDS)) {
                do {
                    tp.bounds.add(type());
                } while (accept(Kind.AMP));
            }
            into.add(tp);
        } while (accept(Kind.COMMA));
        expect(Kind.GT);
    }

    private void classBody(Tree.ClassDecl decl) {
        expect(Kind.LBRACE);
        if (decl.kind == Tree.ClassKind.ENUM) {
            enumConstants(decl);
        }
        while (!is(Kind.RBRACE) && !is(Kind.EOF)) {
            int start = p;
            try {
                member(decl);
            } catch (CompileError e) {
                recoverMember(start);
            }
        }
        expect(Kind.RBRACE);
    }

    private void recoverMember(int start) {
        if (p == start) {
            advance();
        }
        int depth = 0;
        while (!is(Kind.EOF)) {
            if (is(Kind.LBRACE)) {
                depth++;
            } else if (is(Kind.RBRACE)) {
                if (depth == 0) {
                    return;
                }
                depth--;
                if (depth == 0) {
                    advance();
                    return;
                }
            } else if (is(Kind.SEMI) && depth == 0) {
                advance();
                return;
            }
            advance();
        }
    }

    private void enumConstants(Tree.ClassDecl decl) {
        while (!is(Kind.SEMI) && !is(Kind.RBRACE)) {
            Tree.Modifiers mods = at(new Tree.Modifiers(), tok().pos);
            while (is(Kind.AT)) {
                mods.annotations.add(annotation());
            }
            Tree.VarDef constant = at(new Tree.VarDef(), tok().pos);
            constant.mods = mods;
            mods.flags = ACC_PUBLIC | ACC_STATIC | ACC_FINAL | 0x4000;
            constant.name = ident();
            Tree.NewClass init = at(new Tree.NewClass(), constant.pos);
            Tree.Ident enumType = at(new Tree.Ident(), constant.pos);
            enumType.name = decl.name;
            init.clazz = enumType;
            if (is(Kind.LPAREN)) {
                arguments(init.args);
            }
            if (is(Kind.LBRACE)) {
                Tree.ClassDecl body = at(new Tree.ClassDecl(), tok().pos);
                body.mods = at(new Tree.Modifiers(), tok().pos);
                body.name = "";
                body.anonymous = true;
                classBody(body);
                init.body = body;
            }
            constant.init = init;
            decl.members.add(constant);
            if (!accept(Kind.COMMA)) {
                break;
            }
        }
        accept(Kind.SEMI);
    }

    private void member(Tree.ClassDecl decl) {
        if (accept(Kind.SEMI)) {
            return;
        }
        if (is(Kind.LBRACE) || is(Kind.STATIC) && peek(1).kind == Kind.LBRACE) {
            boolean isStatic = accept(Kind.STATIC);
            Tree.Block block = block();
            block.isStatic = isStatic;
            decl.members.add(block);
            return;
        }
        Tree.Modifiers mods = modifiers();
        if (is(Kind.CLASS) || is(Kind.INTERFACE) || is(Kind.ENUM) || is(Kind.AT) && peek(1).kind == Kind.INTERFACE
                || isRecordStart()) {
            decl.members.add(classOrInterface(mods));
            return;
        }
        List<Tree.TypeParameter> typeParams = new ArrayList<Tree.TypeParameter>();
        if (is(Kind.LT)) {
            typeParameters(typeParams);
        }
        int pos = tok().pos;
        // Constructor: Name '(' ; compact record constructor: Name '{'.
        if (is(Kind.IDENT) && tok().name.equals(decl.name)
                && (peek(1).kind == Kind.LPAREN || decl.kind == Tree.ClassKind.RECORD && peek(1).kind == Kind.LBRACE)) {
            Tree.MethodDecl ctor = at(new Tree.MethodDecl(), pos);
            ctor.mods = mods;
            ctor.typeParams.addAll(typeParams);
            ctor.name = "<init>";
            advance();
            if (is(Kind.LBRACE)) {
                ctor.compactConstructor = true;
            } else {
                formalParameters(ctor);
            }
            if (accept(Kind.THROWS)) {
                do {
                    ctor.thrown.add(type());
                } while (accept(Kind.COMMA));
            }
            ctor.body = block();
            decl.members.add(ctor);
            return;
        }
        Tree type;
        if (is(Kind.VOID)) {
            Tree.PrimitiveTypeTree v = at(new Tree.PrimitiveTypeTree(), advance().pos);
            v.tag = Kind.VOID;
            type = v;
        } else {
            type = type();
        }
        int namePos = tok().pos;
        String name = ident();
        if (is(Kind.LPAREN)) {
            Tree.MethodDecl m = at(new Tree.MethodDecl(), namePos);
            m.mods = mods;
            m.typeParams.addAll(typeParams);
            m.returnType = type;
            m.name = name;
            formalParameters(m);
            m.returnType = bracketsOpt(m.returnType);
            if (accept(Kind.THROWS)) {
                do {
                    m.thrown.add(type());
                } while (accept(Kind.COMMA));
            }
            if (accept(Kind.DEFAULT)) {
                // An annotation element's default: parsed and dropped, as annotations
                // themselves are -- nothing the compiler emits reads them.
                elementValue();
            }
            if (is(Kind.LBRACE)) {
                m.body = block();
            } else {
                expect(Kind.SEMI);
            }
            decl.members.add(m);
            return;
        }
        // Fields: Type a = 1, b[], c;
        while (true) {
            Tree.VarDef field = at(new Tree.VarDef(), namePos);
            field.mods = mods;
            field.vartype = bracketsOpt(type);
            field.name = name;
            if (accept(Kind.EQ)) {
                field.init = variableInitializer();
            }
            decl.members.add(field);
            if (!accept(Kind.COMMA)) {
                break;
            }
            namePos = tok().pos;
            name = ident();
        }
        expect(Kind.SEMI);
    }

    private void formalParameters(Tree.MethodDecl m) {
        expect(Kind.LPAREN);
        if (!is(Kind.RPAREN)) {
            do {
                Tree.VarDef param = at(new Tree.VarDef(), tok().pos);
                param.mods = modifiers();
                param.vartype = type();
                // Receiver parameter: `Type this`.
                if (is(Kind.THIS)) {
                    advance();
                    continue;
                }
                if (accept(Kind.ELLIPSIS)) {
                    m.varargs = true;
                    Tree.ArrayTypeTree arr = at(new Tree.ArrayTypeTree(), param.vartype.pos);
                    arr.elem = param.vartype;
                    param.vartype = arr;
                }
                param.pos = tok().pos;
                param.name = ident();
                param.vartype = bracketsOpt(param.vartype);
                m.params.add(param);
            } while (accept(Kind.COMMA));
        }
        expect(Kind.RPAREN);
    }

    private Tree bracketsOpt(Tree type) {
        while (is(Kind.LBRACKET) && peek(1).kind == Kind.RBRACKET) {
            Tree.ArrayTypeTree arr = at(new Tree.ArrayTypeTree(), tok().pos);
            advance();
            advance();
            arr.elem = type;
            type = arr;
        }
        return type;
    }

    private Tree variableInitializer() {
        if (is(Kind.LBRACE)) {
            return arrayInitializer(null);
        }
        return expression();
    }

    private Tree.NewArray arrayInitializer(Tree elemType) {
        Tree.NewArray array = at(new Tree.NewArray(), expect(Kind.LBRACE).pos);
        array.elemType = elemType;
        array.elems = new ArrayList<Tree>();
        while (!is(Kind.RBRACE)) {
            array.elems.add(variableInitializer());
            if (!accept(Kind.COMMA)) {
                break;
            }
        }
        expect(Kind.RBRACE);
        return array;
    }

    // ------------------------------------------------------------------ types

    private boolean isPrimitive(Kind k) {
        return k == Kind.BOOLEAN || k == Kind.BYTE || k == Kind.CHAR || k == Kind.SHORT || k == Kind.INT
                || k == Kind.LONG || k == Kind.FLOAT || k == Kind.DOUBLE;
    }

    Tree type() {
        while (is(Kind.AT)) {
            annotation();
        }
        Tree t;
        if (isPrimitive(kind())) {
            Tree.PrimitiveTypeTree prim = at(new Tree.PrimitiveTypeTree(), tok().pos);
            prim.tag = advance().kind;
            t = prim;
        } else if (is(Kind.QUES)) {
            throw error(tok().pos, "unexpected wildcard");
        } else {
            t = classType();
        }
        return bracketsOpt(t);
    }

    private Tree classType() {
        Tree.Ident id = at(new Tree.Ident(), tok().pos);
        id.name = ident();
        Tree t = id;
        if (is(Kind.LT)) {
            t = typeArguments(t);
        }
        while (is(Kind.DOT) && (peek(1).kind == Kind.IDENT || peek(1).kind == Kind.AT)) {
            advance();
            while (is(Kind.AT)) {
                annotation();
            }
            Tree.Select sel = at(new Tree.Select(), tok().pos);
            sel.selected = t;
            sel.name = ident();
            t = sel;
            if (is(Kind.LT)) {
                t = typeArguments(t);
            }
        }
        return t;
    }

    private Tree typeArguments(Tree base) {
        Tree.TypeApply apply = at(new Tree.TypeApply(), tok().pos);
        apply.clazz = base;
        expect(Kind.LT);
        if (is(Kind.GT)) {
            // Diamond.
            advance();
            return apply;
        }
        do {
            apply.args.add(typeArgument());
        } while (accept(Kind.COMMA));
        expect(Kind.GT);
        return apply;
    }

    private Tree typeArgument() {
        while (is(Kind.AT)) {
            annotation();
        }
        if (is(Kind.QUES)) {
            Tree.Wildcard w = at(new Tree.Wildcard(), advance().pos);
            if (accept(Kind.EXTENDS)) {
                w.boundKind = Kind.EXTENDS;
                w.bound = type();
            } else if (accept(Kind.SUPER)) {
                w.boundKind = Kind.SUPER;
                w.bound = type();
            }
            return w;
        }
        return type();
    }

    private List<Tree> typeArgumentList() {
        List<Tree> args = new ArrayList<Tree>();
        expect(Kind.LT);
        if (!is(Kind.GT)) {
            do {
                args.add(typeArgument());
            } while (accept(Kind.COMMA));
        }
        expect(Kind.GT);
        return args;
    }

    // ------------------------------------------------------------------ statements

    Tree.Block block() {
        Tree.Block block = at(new Tree.Block(), expect(Kind.LBRACE).pos);
        while (!is(Kind.RBRACE) && !is(Kind.EOF)) {
            int start = p;
            try {
                blockStatement(block.stats);
            } catch (CompileError e) {
                recoverStatement(start);
            }
        }
        block.endPos = tok().pos;
        expect(Kind.RBRACE);
        return block;
    }

    private void recoverStatement(int start) {
        if (p == start) {
            advance();
        }
        int depth = 0;
        while (!is(Kind.EOF)) {
            if (is(Kind.LBRACE)) {
                depth++;
            } else if (is(Kind.RBRACE)) {
                if (depth == 0) {
                    return;
                }
                depth--;
            } else if (is(Kind.SEMI) && depth == 0) {
                advance();
                return;
            }
            advance();
        }
    }

    private void blockStatement(List<Tree> into) {
        // Local class / record / interface / enum.
        if (is(Kind.CLASS) || is(Kind.INTERFACE) || is(Kind.ENUM) || isRecordStart()
                || (is(Kind.ABSTRACT) || is(Kind.FINAL) || is(Kind.STATIC) || isIdent("sealed") || isIdent("non"))
                && localTypeAhead()) {
            Tree.Modifiers mods = modifiers();
            Tree.ClassDecl local = classOrInterface(mods);
            local.local = true;
            into.add(local);
            return;
        }
        if (is(Kind.FINAL) || is(Kind.AT)) {
            Tree.Modifiers mods = modifiers();
            if (is(Kind.CLASS) || is(Kind.INTERFACE) || is(Kind.ENUM) || isRecordStart()) {
                Tree.ClassDecl local = classOrInterface(mods);
                local.local = true;
                into.add(local);
                return;
            }
            localVariables(mods, into);
            expect(Kind.SEMI);
            return;
        }
        if (looksLikeLocalVariable()) {
            localVariables(at(new Tree.Modifiers(), tok().pos), into);
            expect(Kind.SEMI);
            return;
        }
        into.add(statement());
    }

    private boolean localTypeAhead() {
        int k = 0;
        while (true) {
            Token t = peek(k);
            if (t.kind == Kind.CLASS || t.kind == Kind.INTERFACE || t.kind == Kind.ENUM) {
                return true;
            }
            if (t.kind == Kind.IDENT && "record".equals(t.name) && peek(k + 1).kind == Kind.IDENT) {
                return true;
            }
            if (t.kind == Kind.ABSTRACT || t.kind == Kind.FINAL || t.kind == Kind.STATIC || t.kind == Kind.STRICTFP
                    || t.kind == Kind.IDENT && ("sealed".equals(t.name) || "non".equals(t.name) || "-".equals(t.name))
                    || t.kind == Kind.SUB) {
                k++;
                continue;
            }
            return false;
        }
    }

    /** `Type name` where `name` is followed by one of = ; , [ : -- a declaration. */
    private boolean looksLikeLocalVariable() {
        Kind k = kind();
        if (isPrimitive(k)) {
            return peek(1).kind != Kind.DOT && peek(1).kind != Kind.COLONCOLON
                    && !(peek(1).kind == Kind.LBRACKET && peek(2).kind == Kind.RBRACKET && peek(3).kind == Kind.DOT);
        }
        if (k != Kind.IDENT) {
            return false;
        }
        if (isIdent("yield") && yieldStatementAhead()) {
            // `yield` is a restricted identifier: never a type, so `yield x;` is a yield statement.
            return false;
        }
        if (isIdent("var") && peek(1).kind == Kind.IDENT) {
            return true;
        }
        // Fast rejects: `a = ..`, `a(...)`, `a.b(...)` are expressions.
        Kind next = peek(1).kind;
        if (next == Kind.IDENT) {
            return true;
        }
        if (next != Kind.LT && next != Kind.DOT && next != Kind.LBRACKET) {
            return false;
        }
        final int start = p;
        Boolean ok = attempt(new Attempt<Boolean>() {
            @Override
            public Boolean run() {
                type();
                if (!is(Kind.IDENT)) {
                    return Boolean.FALSE;
                }
                Kind after = peek(1).kind;
                return after == Kind.EQ || after == Kind.SEMI || after == Kind.COMMA || after == Kind.LBRACKET
                        || after == Kind.COLON ? Boolean.TRUE : Boolean.FALSE;
            }
        });
        p = start;
        return Boolean.TRUE.equals(ok);
    }

    private void localVariables(Tree.Modifiers mods, List<Tree> into) {
        Tree type;
        if (isIdent("var") && peek(1).kind == Kind.IDENT) {
            advance();
            type = null;
        } else {
            type = type();
        }
        while (true) {
            Tree.VarDef v = at(new Tree.VarDef(), tok().pos);
            v.mods = mods;
            v.name = ident();
            v.vartype = type == null ? null : bracketsOpt(type);
            if (accept(Kind.EQ)) {
                v.init = variableInitializer();
                if (v.init instanceof Tree.NewArray && ((Tree.NewArray) v.init).elemType == null && v.vartype != null) {
                    // `int[] a = {1, 2}`: the initializer takes its element type from the declaration.
                    Tree vt = v.vartype;
                    if (vt instanceof Tree.ArrayTypeTree) {
                        ((Tree.NewArray) v.init).elemType = ((Tree.ArrayTypeTree) vt).elem;
                    }
                }
            }
            into.add(v);
            if (!accept(Kind.COMMA)) {
                return;
            }
        }
    }

    private Tree statement() {
        Token t = tok();
        switch (t.kind) {
            case LBRACE:
                return block();
            case SEMI:
                return at(new Tree.Empty(), advance().pos);
            case IF: {
                Tree.If s = at(new Tree.If(), advance().pos);
                s.cond = parExpression();
                s.thenPart = statement();
                if (accept(Kind.ELSE)) {
                    s.elsePart = statement();
                }
                return s;
            }
            case WHILE: {
                Tree.WhileLoop s = at(new Tree.WhileLoop(), advance().pos);
                s.cond = parExpression();
                s.body = statement();
                return s;
            }
            case DO: {
                Tree.DoLoop s = at(new Tree.DoLoop(), advance().pos);
                s.body = statement();
                expect(Kind.WHILE);
                s.cond = parExpression();
                expect(Kind.SEMI);
                return s;
            }
            case FOR:
                return forStatement();
            case TRY:
                return tryStatement();
            case SWITCH: {
                Tree.Switch s = switchConstruct();
                return s;
            }
            case SYNCHRONIZED: {
                Tree.Synchronized s = at(new Tree.Synchronized(), advance().pos);
                s.lock = parExpression();
                s.body = block();
                return s;
            }
            case RETURN: {
                Tree.Return s = at(new Tree.Return(), advance().pos);
                if (!is(Kind.SEMI)) {
                    s.expr = expression();
                }
                expect(Kind.SEMI);
                return s;
            }
            case THROW: {
                Tree.Throw s = at(new Tree.Throw(), advance().pos);
                s.expr = expression();
                expect(Kind.SEMI);
                return s;
            }
            case BREAK: {
                Tree.Break s = at(new Tree.Break(), advance().pos);
                if (is(Kind.IDENT)) {
                    s.label = ident();
                }
                expect(Kind.SEMI);
                return s;
            }
            case CONTINUE: {
                Tree.Continue s = at(new Tree.Continue(), advance().pos);
                if (is(Kind.IDENT)) {
                    s.label = ident();
                }
                expect(Kind.SEMI);
                return s;
            }
            case ASSERT: {
                Tree.Assert s = at(new Tree.Assert(), advance().pos);
                s.cond = expression();
                if (accept(Kind.COLON)) {
                    s.detail = expression();
                }
                expect(Kind.SEMI);
                return s;
            }
            case IDENT:
                if (peek(1).kind == Kind.COLON) {
                    Tree.Labeled s = at(new Tree.Labeled(), t.pos);
                    s.label = ident();
                    advance();
                    s.body = statement();
                    return s;
                }
                if (isIdent("yield") && yieldStatementAhead()) {
                    Tree.Yield s = at(new Tree.Yield(), advance().pos);
                    s.value = expression();
                    expect(Kind.SEMI);
                    return s;
                }
                break;
            case ELSE:
                throw error(t.pos, "'else' without 'if'");
            case CATCH:
            case FINALLY:
                throw error(t.pos, "'" + t.name + "' without 'try'");
            case CASE:
            case DEFAULT:
                throw error(t.pos, "orphaned " + t.name);
            default:
                break;
        }
        Tree.ExpressionStatement s = at(new Tree.ExpressionStatement(), t.pos);
        s.expr = expression();
        checkExpressionStatement(s.expr);
        expect(Kind.SEMI);
        return s;
    }

    private boolean yieldStatementAhead() {
        Kind next = peek(1).kind;
        return next != Kind.EQ && next != Kind.DOT && next != Kind.LBRACKET && next != Kind.PLUSPLUS
                && next != Kind.SUBSUB && next != Kind.SEMI && next != Kind.PLUSEQ && next != Kind.SUBEQ;
    }

    private void checkExpressionStatement(Tree e) {
        if (e instanceof Tree.Assign || e instanceof Tree.CompoundAssign || e instanceof Tree.MethodCall
                || e instanceof Tree.NewClass || e instanceof Tree.Unary
                && (((Tree.Unary) e).op == Kind.PLUSPLUS || ((Tree.Unary) e).op == Kind.SUBSUB)) {
            return;
        }
        throw error(e.pos, "not a statement");
    }

    private Tree parExpression() {
        expect(Kind.LPAREN);
        Tree e = expression();
        expect(Kind.RPAREN);
        return e;
    }

    private Tree forStatement() {
        int pos = advance().pos;
        expect(Kind.LPAREN);
        // for (Type x : expr)
        int save = p;
        Tree.ForEach each = attempt(new Attempt<Tree.ForEach>() {
            @Override
            public Tree.ForEach run() {
                Tree.VarDef var = at(new Tree.VarDef(), tok().pos);
                var.mods = modifiers();
                if (isIdent("var") && peek(1).kind == Kind.IDENT) {
                    advance();
                } else {
                    var.vartype = type();
                }
                var.pos = tok().pos;
                var.name = ident();
                var.vartype = var.vartype == null ? null : bracketsOpt(var.vartype);
                if (!is(Kind.COLON)) {
                    throw FAILURE;
                }
                advance();
                Tree.ForEach f = new Tree.ForEach();
                f.var = var;
                return f;
            }
        });
        if (each != null) {
            each.pos = pos;
            each.expr = expression();
            expect(Kind.RPAREN);
            each.body = statement();
            return each;
        }
        p = save;
        Tree.ForLoop loop = at(new Tree.ForLoop(), pos);
        if (!is(Kind.SEMI)) {
            if (is(Kind.FINAL) || is(Kind.AT) || looksLikeLocalVariable()) {
                localVariables(modifiers(), loop.init);
            } else {
                do {
                    Tree.ExpressionStatement s = at(new Tree.ExpressionStatement(), tok().pos);
                    s.expr = expression();
                    loop.init.add(s);
                } while (accept(Kind.COMMA));
            }
        }
        expect(Kind.SEMI);
        if (!is(Kind.SEMI)) {
            loop.cond = expression();
        }
        expect(Kind.SEMI);
        if (!is(Kind.RPAREN)) {
            do {
                Tree.ExpressionStatement s = at(new Tree.ExpressionStatement(), tok().pos);
                s.expr = expression();
                loop.step.add(s);
            } while (accept(Kind.COMMA));
        }
        expect(Kind.RPAREN);
        loop.body = statement();
        return loop;
    }

    private Tree tryStatement() {
        Tree.Try t = at(new Tree.Try(), advance().pos);
        if (accept(Kind.LPAREN)) {
            while (!is(Kind.RPAREN)) {
                if (is(Kind.FINAL) || is(Kind.AT) || looksLikeLocalVariable()) {
                    Tree.Modifiers mods = modifiers();
                    Tree.VarDef v = at(new Tree.VarDef(), tok().pos);
                    v.mods = mods;
                    if (isIdent("var") && peek(1).kind == Kind.IDENT) {
                        advance();
                    } else {
                        v.vartype = type();
                    }
                    v.pos = tok().pos;
                    v.name = ident();
                    expect(Kind.EQ);
                    v.init = expression();
                    t.resources.add(v);
                } else {
                    t.resources.add(expression());
                }
                if (!accept(Kind.SEMI)) {
                    break;
                }
            }
            expect(Kind.RPAREN);
        }
        t.body = block();
        while (is(Kind.CATCH)) {
            Tree.Catch c = at(new Tree.Catch(), advance().pos);
            expect(Kind.LPAREN);
            Tree.VarDef param = at(new Tree.VarDef(), tok().pos);
            param.mods = modifiers();
            Tree first = type();
            if (is(Kind.BAR)) {
                param.unionTypes = new ArrayList<Tree>();
                param.unionTypes.add(first);
                while (accept(Kind.BAR)) {
                    param.unionTypes.add(type());
                }
            }
            param.vartype = first;
            param.pos = tok().pos;
            param.name = ident();
            expect(Kind.RPAREN);
            c.param = param;
            c.body = block();
            t.catches.add(c);
        }
        if (accept(Kind.FINALLY)) {
            t.finalizer = block();
        }
        if (t.catches.isEmpty() && t.finalizer == null && t.resources.isEmpty()) {
            throw error(t.pos, "'try' without 'catch', 'finally' or resource declarations");
        }
        return t;
    }

    /** A switch statement or expression (the caller decides which it is). */
    private Tree.Switch switchConstruct() {
        Tree.Switch sw = at(new Tree.Switch(), expect(Kind.SWITCH).pos);
        sw.selector = parExpression();
        expect(Kind.LBRACE);
        boolean sawArrow = false;
        boolean sawColon = false;
        while (!is(Kind.RBRACE) && !is(Kind.EOF)) {
            Tree.Case c = at(new Tree.Case(), tok().pos);
            if (accept(Kind.DEFAULT)) {
                c.isDefault = true;
            } else {
                expect(Kind.CASE);
                caseLabels(c);
            }
            if (accept(Kind.ARROW)) {
                sawArrow = true;
                if (is(Kind.LBRACE)) {
                    c.stats.add(block());
                } else if (is(Kind.THROW)) {
                    c.stats.add(statement());
                } else {
                    Tree.ExpressionStatement s = at(new Tree.ExpressionStatement(), tok().pos);
                    s.expr = expression();
                    expect(Kind.SEMI);
                    c.arrowExpr = s.expr;
                    c.stats.add(s);
                }
            } else {
                expect(Kind.COLON);
                sawColon = true;
                while (!is(Kind.CASE) && !is(Kind.DEFAULT) && !is(Kind.RBRACE) && !is(Kind.EOF)) {
                    int start = p;
                    try {
                        blockStatement(c.stats);
                    } catch (CompileError e) {
                        recoverStatement(start);
                    }
                }
            }
            sw.cases.add(c);
        }
        expect(Kind.RBRACE);
        if (sawArrow && sawColon) {
            throw error(sw.pos, "different case kinds used in the switch");
        }
        sw.arrows = sawArrow;
        return sw;
    }

    private void caseLabels(Tree.Case c) {
        do {
            if (is(Kind.NULL) && (peek(1).kind == Kind.COMMA || peek(1).kind == Kind.ARROW || peek(1).kind == Kind.COLON)) {
                advance();
                c.hasNull = true;
                if (accept(Kind.COMMA)) {
                    if (accept(Kind.DEFAULT)) {
                        c.isDefault = true;
                        return;
                    }
                    p--;
                }
                continue;
            }
            if (is(Kind.DEFAULT)) {
                advance();
                c.isDefault = true;
                continue;
            }
            Tree pattern = patternAhead() ? pattern() : null;
            c.labels.add(pattern != null ? pattern : ternaryForCase());
        } while (accept(Kind.COMMA));
        if (isIdent("when")) {
            advance();
            c.guard = expression();
        }
    }

    /** A case label is a pattern when it starts `Type ident`, `final Type ident` or `Type(`. */
    private boolean patternAhead() {
        if (is(Kind.FINAL)) {
            return true;
        }
        if (isPrimitive(kind()) && peek(1).kind == Kind.IDENT) {
            return true;
        }
        final int start = p;
        Boolean yes = attempt(new Attempt<Boolean>() {
            @Override
            public Boolean run() {
                type();
                return is(Kind.IDENT) || is(Kind.LPAREN) ? Boolean.TRUE : Boolean.FALSE;
            }
        });
        p = start;
        return Boolean.TRUE.equals(yes);
    }

    /** A case constant, which must not consume a following `->` as a lambda. */
    private Tree ternaryForCase() {
        return conditionalExpression();
    }

    private Tree pattern() {
        int pos = tok().pos;
        Tree.Modifiers mods = modifiers();
        Tree type;
        if (isIdent("var") && peek(1).kind == Kind.IDENT) {
            advance();
            type = null;
        } else {
            type = type();
        }
        if (type != null && is(Kind.LPAREN)) {
            Tree.RecordPattern rp = at(new Tree.RecordPattern(), pos);
            rp.deconstructor = type;
            advance();
            if (!is(Kind.RPAREN)) {
                do {
                    rp.nested.add(pattern());
                } while (accept(Kind.COMMA));
            }
            expect(Kind.RPAREN);
            return rp;
        }
        Tree.BindingPattern bp = at(new Tree.BindingPattern(), pos);
        Tree.VarDef var = at(new Tree.VarDef(), tok().pos);
        var.mods = mods;
        var.vartype = type;
        var.name = ident();
        bp.var = var;
        return bp;
    }

    // ------------------------------------------------------------------ expressions

    Tree expression() {
        Tree lhs = conditionalOrLambda();
        Kind k = kind();
        if (k == Kind.EQ) {
            Tree.Assign a = at(new Tree.Assign(), advance().pos);
            a.lhs = lhs;
            a.rhs = expression();
            checkAssignable(lhs);
            return a;
        }
        Kind op = compoundOperator(k);
        if (op != null) {
            Tree.CompoundAssign a = at(new Tree.CompoundAssign(), advance().pos);
            a.op = op;
            a.lhs = lhs;
            a.rhs = expression();
            checkAssignable(lhs);
            return a;
        }
        return lhs;
    }

    private void checkAssignable(Tree lhs) {
        Tree t = lhs;
        while (t instanceof Tree.Parens) {
            t = ((Tree.Parens) t).expr;
        }
        if (!(t instanceof Tree.Ident || t instanceof Tree.Select || t instanceof Tree.ArrayAccess)) {
            throw error(lhs.pos, "unexpected type: required variable, found value");
        }
    }

    private static Kind compoundOperator(Kind k) {
        switch (k) {
            case PLUSEQ: return Kind.PLUS;
            case SUBEQ: return Kind.SUB;
            case STAREQ: return Kind.STAR;
            case SLASHEQ: return Kind.SLASH;
            case PERCENTEQ: return Kind.PERCENT;
            case AMPEQ: return Kind.AMP;
            case BAREQ: return Kind.BAR;
            case CARETEQ: return Kind.CARET;
            case LTLTEQ: return Kind.LTLT;
            case GTGTEQ: return Kind.GTGT;
            case GTGTGTEQ: return Kind.GTGTGT;
            default: return null;
        }
    }

    private Tree conditionalOrLambda() {
        Tree lambda = lambdaAhead();
        if (lambda != null) {
            return lambda;
        }
        return conditionalExpression();
    }

    /** Parses a lambda if one starts here, else returns null without consuming anything. */
    private Tree lambdaAhead() {
        if (is(Kind.IDENT) && peek(1).kind == Kind.ARROW) {
            Tree.Lambda l = at(new Tree.Lambda(), tok().pos);
            Tree.VarDef param = at(new Tree.VarDef(), tok().pos);
            param.mods = at(new Tree.Modifiers(), tok().pos);
            param.name = ident();
            l.params.add(param);
            advance();
            l.body = lambdaBody();
            return l;
        }
        if (!is(Kind.LPAREN)) {
            return null;
        }
        int close = matchingParen(p);
        if (close < 0 || tokens.get(close + 1).kind != Kind.ARROW) {
            return null;
        }
        Tree.Lambda l = at(new Tree.Lambda(), tok().pos);
        advance();
        if (!is(Kind.RPAREN)) {
            boolean implicit = is(Kind.IDENT) && (peek(1).kind == Kind.COMMA || peek(1).kind == Kind.RPAREN);
            l.explicitParams = !implicit;
            do {
                Tree.VarDef param = at(new Tree.VarDef(), tok().pos);
                param.mods = at(new Tree.Modifiers(), tok().pos);
                if (implicit) {
                    param.name = ident();
                } else {
                    param.mods = modifiers();
                    if (isIdent("var") && peek(1).kind == Kind.IDENT) {
                        advance();
                        l.explicitParams = false;
                    } else {
                        param.vartype = type();
                        if (accept(Kind.ELLIPSIS)) {
                            Tree.ArrayTypeTree arr = at(new Tree.ArrayTypeTree(), param.vartype.pos);
                            arr.elem = param.vartype;
                            param.vartype = arr;
                        }
                    }
                    param.pos = tok().pos;
                    param.name = ident();
                    if (param.vartype != null) {
                        param.vartype = bracketsOpt(param.vartype);
                    }
                }
                l.params.add(param);
            } while (accept(Kind.COMMA));
        } else {
            l.explicitParams = true;
        }
        expect(Kind.RPAREN);
        expect(Kind.ARROW);
        l.body = lambdaBody();
        return l;
    }

    private Tree lambdaBody() {
        if (is(Kind.LBRACE)) {
            return block();
        }
        return expression();
    }

    /** Index of the ')' matching the '(' at {@code open}, or -1. */
    private int matchingParen(int open) {
        int depth = 0;
        for (int k = open; k < tokens.size(); k++) {
            Kind kk = tokens.get(k).kind;
            if (kk == Kind.LPAREN) {
                depth++;
            } else if (kk == Kind.RPAREN) {
                depth--;
                if (depth == 0) {
                    return k;
                }
            } else if (kk == Kind.EOF) {
                return -1;
            }
        }
        return -1;
    }

    Tree conditionalExpression() {
        Tree cond = binary(0);
        if (is(Kind.QUES)) {
            Tree.Conditional c = at(new Tree.Conditional(), advance().pos);
            c.cond = cond;
            // The middle operand is a full Expression (assignment allowed); the last is not.
            c.truePart = expression();
            expect(Kind.COLON);
            c.falsePart = conditionalOrLambdaNoAssign();
            return c;
        }
        return cond;
    }

    private Tree conditionalOrLambdaNoAssign() {
        Tree lambda = lambdaAhead();
        return lambda != null ? lambda : conditionalExpression();
    }

    /** Binary operator precedence, higher binds tighter; -1 when not a binary operator. */
    private static int precedence(Kind k) {
        switch (k) {
            case BARBAR: return 1;
            case AMPAMP: return 2;
            case BAR: return 3;
            case CARET: return 4;
            case AMP: return 5;
            case EQEQ: case BANGEQ: return 6;
            case LT: case GT: case LTEQ: case GTEQ: case INSTANCEOF: return 7;
            case LTLT: case GTGT: case GTGTGT: return 8;
            case PLUS: case SUB: return 9;
            case STAR: case SLASH: case PERCENT: return 10;
            default: return -1;
        }
    }

    private Tree binary(int minPrec) {
        Tree lhs = unary();
        while (true) {
            Kind k = kind();
            int prec = precedence(k);
            if (prec < 0 || prec <= minPrec) {
                return lhs;
            }
            Token opTok = advance();
            if (k == Kind.INSTANCEOF) {
                Tree.InstanceOf io = at(new Tree.InstanceOf(), opTok.pos);
                io.expr = lhs;
                if (is(Kind.FINAL) || patternAheadAfterInstanceof()) {
                    io.pattern = pattern();
                } else {
                    io.pattern = type();
                }
                lhs = io;
                continue;
            }
            Tree rhs = binary(prec);
            Tree.Binary b = at(new Tree.Binary(), opTok.pos);
            b.op = k;
            b.lhs = lhs;
            b.rhs = rhs;
            lhs = b;
        }
    }

    private boolean patternAheadAfterInstanceof() {
        final int start = p;
        Boolean yes = attempt(new Attempt<Boolean>() {
            @Override
            public Boolean run() {
                type();
                return is(Kind.IDENT) && !isIdent("instanceof") || is(Kind.LPAREN) ? Boolean.TRUE : Boolean.FALSE;
            }
        });
        p = start;
        return Boolean.TRUE.equals(yes);
    }

    private Tree unary() {
        Token t = tok();
        switch (t.kind) {
            case PLUSPLUS:
            case SUBSUB: {
                advance();
                Tree.Unary u = at(new Tree.Unary(), t.pos);
                u.op = t.kind;
                u.arg = unary();
                checkAssignable(u.arg);
                return u;
            }
            case PLUS:
            case SUB: {
                advance();
                // -2147483648 and -9223372036854775808L are legal only directly under minus.
                if (t.kind == Kind.SUB && (is(Kind.INT_LITERAL) || is(Kind.LONG_LITERAL))) {
                    Token lit = tok();
                    if (lit.kind == Kind.INT_LITERAL && ((Integer) lit.value).intValue() == Integer.MIN_VALUE
                            || lit.kind == Kind.LONG_LITERAL && ((Long) lit.value).longValue() == Long.MIN_VALUE) {
                        advance();
                        Tree.Literal l = at(new Tree.Literal(), t.pos);
                        l.kind = lit.kind;
                        l.value = lit.value;
                        return postfix(l);
                    }
                }
                Tree.Unary u = at(new Tree.Unary(), t.pos);
                u.op = t.kind;
                u.arg = unary();
                return u;
            }
            case BANG:
            case TILDE: {
                advance();
                Tree.Unary u = at(new Tree.Unary(), t.pos);
                u.op = t.kind;
                u.arg = unary();
                return u;
            }
            case LPAREN: {
                Tree cast = castAhead();
                if (cast != null) {
                    return cast;
                }
                break;
            }
            default:
                break;
        }
        return postfix(primaryWithSelectors());
    }

    private Tree postfix(Tree e) {
        while (is(Kind.PLUSPLUS) || is(Kind.SUBSUB)) {
            Token t = advance();
            Tree.Unary u = at(new Tree.Unary(), t.pos);
            u.op = t.kind;
            u.postfix = true;
            u.arg = e;
            checkAssignable(e);
            e = u;
        }
        return e;
    }

    /** `(Type) expr` -- a primitive cast takes any unary operand, a reference cast one that cannot be binary. */
    private Tree castAhead() {
        final int start = p;
        Tree.Cast cast = attempt(new Attempt<Tree.Cast>() {
            @Override
            public Tree.Cast run() {
                advance();
                Tree type = type();
                boolean primitive = type instanceof Tree.PrimitiveTypeTree;
                if (is(Kind.AMP)) {
                    Tree.IntersectionTypeTree it = at(new Tree.IntersectionTypeTree(), type.pos);
                    it.bounds.add(type);
                    while (accept(Kind.AMP)) {
                        it.bounds.add(type());
                    }
                    type = it;
                }
                if (!is(Kind.RPAREN)) {
                    return null;
                }
                advance();
                Kind next = kind();
                boolean operand;
                if (primitive) {
                    operand = next != Kind.EOF && next != Kind.RPAREN && next != Kind.SEMI && next != Kind.COMMA
                            && next != Kind.DOT && precedence(next) < 0 && next != Kind.QUES && next != Kind.COLON
                            && next != Kind.RBRACKET && next != Kind.RBRACE && compoundOperator(next) == null
                            && next != Kind.EQ || next == Kind.PLUS || next == Kind.SUB;
                } else {
                    operand = next == Kind.IDENT || next == Kind.LPAREN || next == Kind.THIS || next == Kind.SUPER
                            || next == Kind.NEW || next == Kind.BANG || next == Kind.TILDE || next == Kind.SWITCH
                            || isLiteral(next) || isPrimitive(next) || next == Kind.VOID;
                }
                if (!operand) {
                    return null;
                }
                Tree.Cast c = at(new Tree.Cast(), tokens.get(start).pos);
                c.clazz = type;
                Tree lambda = lambdaAhead();
                c.expr = lambda != null ? lambda : unary();
                return c;
            }
        });
        if (cast == null) {
            p = start;
        }
        return cast;
    }

    private static boolean isLiteral(Kind k) {
        return k == Kind.INT_LITERAL || k == Kind.LONG_LITERAL || k == Kind.FLOAT_LITERAL || k == Kind.DOUBLE_LITERAL
                || k == Kind.CHAR_LITERAL || k == Kind.STRING_LITERAL || k == Kind.TRUE || k == Kind.FALSE || k == Kind.NULL;
    }

    private Tree primaryWithSelectors() {
        Tree e = primary();
        while (true) {
            Token t = tok();
            if (t.kind == Kind.DOT) {
                advance();
                if (is(Kind.NEW)) {
                    Tree.NewClass nc = (Tree.NewClass) creator();
                    nc.outer = e;
                    e = nc;
                    continue;
                }
                if (is(Kind.LT)) {
                    List<Tree> typeArgs = typeArgumentList();
                    Tree.MethodCall call = at(new Tree.MethodCall(), tok().pos);
                    call.receiver = e;
                    call.typeArgs.addAll(typeArgs);
                    if (accept(Kind.SUPER)) {
                        call.name = "<init>";
                        call.superCall = true;
                    } else {
                        call.name = ident();
                    }
                    arguments(call.args);
                    e = call;
                    continue;
                }
                if (is(Kind.THIS)) {
                    Tree.Select sel = at(new Tree.Select(), advance().pos);
                    sel.selected = e;
                    sel.name = "this";
                    e = sel;
                    continue;
                }
                if (is(Kind.SUPER)) {
                    if (peek(1).kind == Kind.LPAREN) {
                        // outer.super(...): a superclass constructor call with an explicit outer instance.
                        Tree.MethodCall call = at(new Tree.MethodCall(), advance().pos);
                        call.receiver = e;
                        call.name = "<init>";
                        call.superCall = true;
                        arguments(call.args);
                        e = call;
                        continue;
                    }
                    Tree.Select sel = at(new Tree.Select(), advance().pos);
                    sel.selected = e;
                    sel.name = "super";
                    e = sel;
                    continue;
                }
                if (is(Kind.CLASS)) {
                    Tree.ClassLiteral cl = at(new Tree.ClassLiteral(), advance().pos);
                    cl.clazz = e;
                    e = cl;
                    continue;
                }
                int namePos = tok().pos;
                String name = ident();
                if (is(Kind.LPAREN)) {
                    Tree.MethodCall call = at(new Tree.MethodCall(), namePos);
                    call.receiver = e;
                    call.name = name;
                    call.dotPos = t.pos;
                    arguments(call.args);
                    e = call;
                } else {
                    Tree.Select sel = at(new Tree.Select(), namePos);
                    sel.selected = e;
                    sel.name = name;
                    sel.dotPos = t.pos;
                    e = sel;
                }
                continue;
            }
            if (t.kind == Kind.LBRACKET) {
                // Type[] in `String[].class` or `int[]::new`.
                if (peek(1).kind == Kind.RBRACKET) {
                    Tree arrType = bracketsOpt(e);
                    if (is(Kind.DOT) && peek(1).kind == Kind.CLASS) {
                        advance();
                        Tree.ClassLiteral cl = at(new Tree.ClassLiteral(), advance().pos);
                        cl.clazz = arrType;
                        e = cl;
                        continue;
                    }
                    if (is(Kind.COLONCOLON)) {
                        e = methodReference(arrType, true);
                        continue;
                    }
                    throw error(tok().pos, "'.class' expected");
                }
                Tree.ArrayAccess aa = at(new Tree.ArrayAccess(), advance().pos);
                aa.array = e;
                aa.index = expression();
                expect(Kind.RBRACKET);
                e = aa;
                continue;
            }
            if (t.kind == Kind.COLONCOLON) {
                e = methodReference(e, false);
                continue;
            }
            return e;
        }
    }

    private Tree methodReference(Tree qualifier, boolean isType) {
        Tree.MethodRef ref = at(new Tree.MethodRef(), expect(Kind.COLONCOLON).pos);
        ref.qualifier = qualifier;
        ref.qualifierIsType = isType;
        if (is(Kind.LT)) {
            ref.typeArgs.addAll(typeArgumentList());
        }
        if (accept(Kind.NEW)) {
            ref.name = "<init>";
        } else {
            ref.name = ident();
        }
        return ref;
    }

    private void arguments(List<Tree> into) {
        expect(Kind.LPAREN);
        if (!is(Kind.RPAREN)) {
            do {
                into.add(expression());
            } while (accept(Kind.COMMA));
        }
        expect(Kind.RPAREN);
    }

    private Tree primary() {
        Token t = tok();
        switch (t.kind) {
            case INT_LITERAL:
                if (((Integer) t.value).intValue() == Integer.MIN_VALUE && !t.name.startsWith("0")) {
                    throw error(t.pos, "integer number too large");
                }
                return literal();
            case LONG_LITERAL:
                if (((Long) t.value).longValue() == Long.MIN_VALUE && !t.name.startsWith("0")) {
                    throw error(t.pos, "integer number too large");
                }
                return literal();
            case FLOAT_LITERAL:
            case DOUBLE_LITERAL:
            case CHAR_LITERAL:
            case STRING_LITERAL:
            case TRUE:
            case FALSE:
            case NULL:
                return literal();
            case LPAREN: {
                Tree.Parens par = at(new Tree.Parens(), advance().pos);
                par.expr = expression();
                expect(Kind.RPAREN);
                return par;
            }
            case THIS: {
                Tree.Ident id = at(new Tree.Ident(), advance().pos);
                id.name = "this";
                if (is(Kind.LPAREN)) {
                    Tree.MethodCall call = at(new Tree.MethodCall(), t.pos);
                    call.name = "<init>";
                    arguments(call.args);
                    return call;
                }
                return id;
            }
            case SUPER: {
                advance();
                if (is(Kind.LPAREN)) {
                    Tree.MethodCall call = at(new Tree.MethodCall(), t.pos);
                    call.name = "<init>";
                    call.superCall = true;
                    arguments(call.args);
                    return call;
                }
                if (is(Kind.COLONCOLON)) {
                    Tree.Ident sup = at(new Tree.Ident(), t.pos);
                    sup.name = "super";
                    return sup;
                }
                expect(Kind.DOT);
                List<Tree> typeArgs = is(Kind.LT) ? typeArgumentList() : new ArrayList<Tree>();
                int namePos = tok().pos;
                String name = ident();
                if (is(Kind.LPAREN)) {
                    Tree.MethodCall call = at(new Tree.MethodCall(), namePos);
                    call.name = name;
                    call.superCall = true;
                    call.typeArgs.addAll(typeArgs);
                    arguments(call.args);
                    return call;
                }
                Tree.Select sel = at(new Tree.Select(), namePos);
                Tree.Ident sup = at(new Tree.Ident(), t.pos);
                sup.name = "super";
                sel.selected = sup;
                sel.name = name;
                return sel;
            }
            case NEW:
                return creator();
            case SWITCH: {
                Tree.Switch sw = switchConstruct();
                sw.isExpression = true;
                return sw;
            }
            case VOID: {
                Tree.PrimitiveTypeTree v = at(new Tree.PrimitiveTypeTree(), advance().pos);
                v.tag = Kind.VOID;
                if (is(Kind.DOT) && peek(1).kind == Kind.CLASS) {
                    advance();
                    Tree.ClassLiteral cl = at(new Tree.ClassLiteral(), advance().pos);
                    cl.clazz = v;
                    return cl;
                }
                throw error(t.pos, "'.class' expected");
            }
            case LT: {
                // Generic method call with explicit type arguments and no receiver: <T>foo() is not legal Java.
                throw error(t.pos, "illegal start of expression");
            }
            case IDENT: {
                // Generic type for a method reference: List<String>::new, Map.Entry<K,V>::getKey.
                if (peek(1).kind == Kind.LT || peek(1).kind == Kind.DOT) {
                    final int start = p;
                    Tree ref = attempt(new Attempt<Tree>() {
                        @Override
                        public Tree run() {
                            Tree type = type();
                            if (!is(Kind.COLONCOLON) || !(containsTypeApply(type))) {
                                return null;
                            }
                            return methodReference(type, true);
                        }
                    });
                    if (ref != null) {
                        return ref;
                    }
                    p = start;
                }
                Tree.Ident id = at(new Tree.Ident(), t.pos);
                id.name = ident();
                if (is(Kind.LPAREN)) {
                    Tree.MethodCall call = at(new Tree.MethodCall(), t.pos);
                    call.name = id.name;
                    arguments(call.args);
                    return call;
                }
                return id;
            }
            default:
                if (isPrimitive(t.kind)) {
                    Tree.PrimitiveTypeTree prim = at(new Tree.PrimitiveTypeTree(), advance().pos);
                    prim.tag = t.kind;
                    Tree type = bracketsOpt(prim);
                    if (is(Kind.DOT) && peek(1).kind == Kind.CLASS) {
                        advance();
                        Tree.ClassLiteral cl = at(new Tree.ClassLiteral(), advance().pos);
                        cl.clazz = type;
                        return cl;
                    }
                    if (is(Kind.COLONCOLON) && type instanceof Tree.ArrayTypeTree) {
                        return methodReference(type, true);
                    }
                    throw error(t.pos, "'.class' expected");
                }
                throw error(t.pos, "illegal start of expression" + found());
        }
    }

    private static boolean containsTypeApply(Tree t) {
        while (true) {
            if (t instanceof Tree.TypeApply) {
                return true;
            }
            if (t instanceof Tree.ArrayTypeTree) {
                t = ((Tree.ArrayTypeTree) t).elem;
            } else if (t instanceof Tree.Select) {
                t = ((Tree.Select) t).selected;
            } else {
                return false;
            }
        }
    }

    private Tree literal() {
        Token t = advance();
        Tree.Literal l = at(new Tree.Literal(), t.pos);
        l.kind = t.kind;
        switch (t.kind) {
            case TRUE: l.value = Boolean.TRUE; break;
            case FALSE: l.value = Boolean.FALSE; break;
            case NULL: l.value = null; break;
            default: l.value = t.value;
        }
        return l;
    }

    private Tree creator() {
        int pos = expect(Kind.NEW).pos;
        List<Tree> typeArgs = is(Kind.LT) ? typeArgumentList() : new ArrayList<Tree>();
        while (is(Kind.AT)) {
            annotation();
        }
        Tree type;
        if (isPrimitive(kind())) {
            Tree.PrimitiveTypeTree prim = at(new Tree.PrimitiveTypeTree(), tok().pos);
            prim.tag = advance().kind;
            type = prim;
        } else {
            type = classType();
        }
        if (is(Kind.LBRACKET)) {
            Tree.NewArray na = at(new Tree.NewArray(), pos);
            while (is(Kind.LBRACKET)) {
                advance();
                if (is(Kind.RBRACKET)) {
                    advance();
                    na.extraDims++;
                } else {
                    if (na.extraDims > 0) {
                        throw error(tok().pos, "']' expected");
                    }
                    na.dims.add(expression());
                    expect(Kind.RBRACKET);
                }
            }
            if (na.dims.isEmpty()) {
                // new int[][] {...}: the initializer's element type is one dimension down.
                Tree elem = type;
                for (int k = 1; k < na.extraDims; k++) {
                    Tree.ArrayTypeTree arr = at(new Tree.ArrayTypeTree(), type.pos);
                    arr.elem = elem;
                    elem = arr;
                }
                if (!is(Kind.LBRACE)) {
                    throw error(tok().pos, "array dimension missing");
                }
                Tree.NewArray init = arrayInitializer(elem);
                init.pos = pos;
                return init;
            }
            na.elemType = type;
            return na;
        }
        if (type instanceof Tree.PrimitiveTypeTree) {
            throw error(tok().pos, "'[' expected");
        }
        Tree.NewClass nc = at(new Tree.NewClass(), pos);
        nc.typeArgs.addAll(typeArgs);
        nc.clazz = type;
        arguments(nc.args);
        if (is(Kind.LBRACE)) {
            Tree.ClassDecl body = at(new Tree.ClassDecl(), tok().pos);
            body.mods = at(new Tree.Modifiers(), tok().pos);
            body.name = "";
            body.anonymous = true;
            classBody(body);
            nc.body = body;
        }
        return nc;
    }
}

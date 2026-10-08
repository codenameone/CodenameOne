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

/** One lexical token. {@code kind} is a keyword, operator or one of the literal/identifier kinds. */
final class Token {
    enum Kind {
        EOF, IDENT, INT_LITERAL, LONG_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL, CHAR_LITERAL, STRING_LITERAL,
        // keywords
        ABSTRACT, ASSERT, BOOLEAN, BREAK, BYTE, CASE, CATCH, CHAR, CLASS, CONST, CONTINUE, DEFAULT, DO, DOUBLE,
        ELSE, ENUM, EXTENDS, FINAL, FINALLY, FLOAT, FOR, GOTO, IF, IMPLEMENTS, IMPORT, INSTANCEOF, INT, INTERFACE,
        LONG, NATIVE, NEW, PACKAGE, PRIVATE, PROTECTED, PUBLIC, RETURN, SHORT, STATIC, STRICTFP, SUPER, SWITCH,
        SYNCHRONIZED, THIS, THROW, THROWS, TRANSIENT, TRY, VOID, VOLATILE, WHILE, TRUE, FALSE, NULL,
        // separators and operators
        LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, SEMI, COMMA, DOT, ELLIPSIS, AT, COLONCOLON,
        EQ, GT, LT, BANG, TILDE, QUES, COLON, ARROW, EQEQ, LTEQ, GTEQ, BANGEQ, AMPAMP, BARBAR, PLUSPLUS, SUBSUB,
        PLUS, SUB, STAR, SLASH, AMP, BAR, CARET, PERCENT, LTLT, GTGT, GTGTGT,
        PLUSEQ, SUBEQ, STAREQ, SLASHEQ, AMPEQ, BAREQ, CARETEQ, PERCENTEQ, LTLTEQ, GTGTEQ, GTGTGTEQ
    }

    final Kind kind;
    final int pos;
    final int end;
    /** Identifier name, or the source spelling of an operator. */
    final String name;
    /** Literal value: Integer, Long, Float, Double, Character or String. */
    final Object value;

    Token(Kind kind, int pos, int end, String name, Object value) {
        this.kind = kind;
        this.pos = pos;
        this.end = end;
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return kind == Kind.IDENT ? name : kind.toString();
    }
}

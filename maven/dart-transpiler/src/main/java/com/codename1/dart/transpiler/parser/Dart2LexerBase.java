/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.dart.transpiler.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.WritableToken;

/**
 * Base class required by the vendored Dart2Lexer grammar; hosts the single
 * semantic predicate the lexer uses for string-interpolation lexing.
 */
public abstract class Dart2LexerBase extends Lexer {
    protected Dart2LexerBase(CharStream input) {
        super(input);
    }

    /**
     * Dart identifiers that name a final or protected member of
     * {@code java.lang.Object} with an arity Dart code can plausibly declare
     * ({@code finalize()}, {@code getClass()}, {@code notify()},
     * {@code notifyAll()}). A Dart member of that name cannot be emitted as a
     * Java method (javac: "cannot override finalize() in Object" / "overridden
     * method is final"), so the identifier is renamed once, here in the lexer,
     * and every declaration, call site, named argument and interpolation sees
     * the same Java name. {@code wait} is left alone: the runtime's
     * {@code Future.wait} is a Dart API name and Object's waits take arguments
     * Dart callers do not pass. {@code hashCode}/{@code equals}/{@code clone}
     * are overridable and handled by the emitter.
     */
    public static String mangleObjectMember(String name) {
        if ("finalize".equals(name) || "getClass".equals(name)
                || "notify".equals(name) || "notifyAll".equals(name)) {
            return name + "$";
        }
        return name;
    }

    @Override
    public Token emit() {
        Token t = super.emit();
        if (t.getType() == Dart2Lexer.IDENTIFIER && t instanceof WritableToken) {
            String txt = t.getText();
            String m = mangleObjectMember(txt);
            if (!m.equals(txt)) {
                ((WritableToken) t).setText(m);
            }
        }
        return t;
    }

    protected boolean CheckNotOpenBrace() {
        return _input.LA(1) != '{';
    }
}

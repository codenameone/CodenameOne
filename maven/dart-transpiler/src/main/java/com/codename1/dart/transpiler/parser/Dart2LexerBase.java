package com.codename1.dart.transpiler.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

/**
 * Base class required by the vendored Dart2Lexer grammar; hosts the single
 * semantic predicate the lexer uses for string-interpolation lexing.
 */
public abstract class Dart2LexerBase extends Lexer {
    protected Dart2LexerBase(CharStream input) {
        super(input);
    }

    protected boolean CheckNotOpenBrace() {
        return _input.LA(1) != '{';
    }
}

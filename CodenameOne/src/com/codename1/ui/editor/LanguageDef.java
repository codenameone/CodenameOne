/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

import java.util.HashSet;
import java.util.Set;

/// Describes the lexical rules of a programming language for the pure code editor's tokenizer: its
/// keyword set, comment styles, string delimiters and number syntax. The keyword lists match the ones
/// the previous `BrowserComponent` highlighter used.
public final class LanguageDef {
    private final Set<String> keywords;
    private final boolean lineCommentSlash;
    private final boolean lineCommentHash;
    private final boolean blockComment;
    private final boolean templateString;
    private final boolean plain;

    private LanguageDef(String[] keywords, boolean lineCommentSlash, boolean lineCommentHash,
                        boolean blockComment, boolean templateString, boolean plain) {
        this.keywords = new HashSet<String>();
        for (int i = 0; i < keywords.length; i++) {
            this.keywords.add(keywords[i]);
        }
        this.lineCommentSlash = lineCommentSlash;
        this.lineCommentHash = lineCommentHash;
        this.blockComment = blockComment;
        this.templateString = templateString;
        this.plain = plain;
    }

    /// True for plain text (no syntax highlighting at all).
    public boolean isPlain() {
        return plain;
    }

    /// True when `word` is a keyword in this language.
    public boolean isKeyword(String word) {
        return keywords.contains(word);
    }

    /// True when `//` starts a line comment.
    public boolean hasLineCommentSlash() {
        return lineCommentSlash;
    }

    /// True when `#` starts a line comment.
    public boolean hasLineCommentHash() {
        return lineCommentHash;
    }

    /// True when `/* */` block comments are supported.
    public boolean hasBlockComment() {
        return blockComment;
    }

    /// True when backtick template strings (which may span lines) are supported.
    public boolean hasTemplateString() {
        return templateString;
    }

    private static final String[] JAVA = {
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
        "volatile", "while", "true", "false", "null", "var", "record", "sealed"
    };

    private static final String[] KOTLIN = {
        "as", "break", "by", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
        "interface", "is", "null", "object", "package", "return", "super", "this", "throw", "true",
        "try", "typealias", "val", "var", "when", "while", "abstract", "final", "open", "override",
        "private", "protected", "public", "internal", "companion", "data", "sealed", "suspend"
    };

    private static final String[] JAVASCRIPT = {
        "await", "async", "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "export", "extends", "false", "finally", "for", "function", "if",
        "import", "in", "instanceof", "let", "new", "null", "return", "super", "switch", "this", "throw",
        "true", "try", "typeof", "var", "void", "while", "with", "yield", "of", "static", "get", "set"
    };

    private static final String[] PYTHON = {
        "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif",
        "else", "except", "False", "finally", "for", "from", "global", "if", "import", "in", "is",
        "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "return", "True", "try", "while",
        "with", "yield", "self"
    };

    private static final String[] CSS = {
        "important", "inherit", "initial", "unset", "auto", "none", "flex", "grid", "block", "inline",
        "absolute", "relative", "fixed", "static"
    };

    private static final String[] JSON = {"true", "false", "null"};

    private static final String[] XML = {};

    private static final String[] C = {
        "auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else", "enum",
        "extern", "float", "for", "goto", "if", "int", "long", "register", "return", "short", "signed",
        "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while"
    };

    private static final LanguageDef DEF_JAVA = new LanguageDef(JAVA, true, false, true, false, false);
    private static final LanguageDef DEF_KOTLIN = new LanguageDef(KOTLIN, true, false, true, false, false);
    private static final LanguageDef DEF_JAVASCRIPT = new LanguageDef(JAVASCRIPT, true, false, true, true, false);
    private static final LanguageDef DEF_PYTHON = new LanguageDef(PYTHON, false, true, false, false, false);
    private static final LanguageDef DEF_CSS = new LanguageDef(CSS, true, false, true, false, false);
    private static final LanguageDef DEF_JSON = new LanguageDef(JSON, false, false, false, false, false);
    private static final LanguageDef DEF_XML = new LanguageDef(XML, false, false, false, false, false);
    private static final LanguageDef DEF_C = new LanguageDef(C, true, false, true, false, false);
    private static final LanguageDef DEF_TEXT = new LanguageDef(XML, false, false, false, false, true);

    /// Returns the language definition for the given language id, defaulting to a plain text definition
    /// (no highlighting) for unknown ids.
    public static LanguageDef forName(String language) {
        if ("java".equals(language)) {
            return DEF_JAVA;
        }
        if ("kotlin".equals(language)) {
            return DEF_KOTLIN;
        }
        if ("javascript".equals(language) || "js".equals(language)) {
            return DEF_JAVASCRIPT;
        }
        if ("python".equals(language) || "py".equals(language)) {
            return DEF_PYTHON;
        }
        if ("css".equals(language)) {
            return DEF_CSS;
        }
        if ("json".equals(language)) {
            return DEF_JSON;
        }
        if ("xml".equals(language) || "html".equals(language)) {
            return DEF_XML;
        }
        if ("c".equals(language) || "cpp".equals(language) || "c++".equals(language)) {
            return DEF_C;
        }
        return DEF_TEXT;
    }
}

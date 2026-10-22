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
package com.codename1.desktopcompat.org.fife.ui.rsyntaxtextarea;

/// The name of each token type, in the order of [TokenTypes], as a
/// [Theme] file spells them.
final class TokenNames {

    static final String[] NAMES = {
        "NULL",
        "COMMENT_EOL",
        "COMMENT_MULTILINE",
        "COMMENT_DOCUMENTATION",
        "COMMENT_KEYWORD",
        "COMMENT_MARKUP",
        "RESERVED_WORD",
        "RESERVED_WORD_2",
        "FUNCTION",
        "LITERAL_BOOLEAN",
        "LITERAL_NUMBER_DECIMAL_INT",
        "LITERAL_NUMBER_FLOAT",
        "LITERAL_NUMBER_HEXADECIMAL",
        "LITERAL_STRING_DOUBLE_QUOTE",
        "LITERAL_CHAR",
        "LITERAL_BACKQUOTE",
        "DATA_TYPE",
        "VARIABLE",
        "REGEX",
        "ANNOTATION",
        "IDENTIFIER",
        "WHITESPACE",
        "SEPARATOR",
        "OPERATOR",
        "PREPROCESSOR",
        "MARKUP_TAG_DELIMITER",
        "MARKUP_TAG_NAME",
        "MARKUP_TAG_ATTRIBUTE",
        "MARKUP_TAG_ATTRIBUTE_VALUE",
        "MARKUP_COMMENT",
        "MARKUP_DTD",
        "MARKUP_PROCESSING_INSTRUCTION",
        "MARKUP_CDATA_DELIMITER",
        "MARKUP_CDATA",
        "MARKUP_ENTITY_REFERENCE",
        "ERROR_IDENTIFIER",
        "ERROR_NUMBER_FORMAT",
        "ERROR_STRING_DOUBLE",
        "ERROR_CHAR"
    };

    private TokenNames() {
    }

    /// The token type with this name, or -1.
    static int of(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }
}

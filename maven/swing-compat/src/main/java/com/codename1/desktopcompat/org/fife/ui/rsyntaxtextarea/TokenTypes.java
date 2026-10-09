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

/// The kinds of token a [RSyntaxTextArea] tells apart, by number. A
/// [Theme] names them when it gives each a color.
public interface TokenTypes {

    int NULL = 0;
    int COMMENT_EOL = 1;
    int COMMENT_MULTILINE = 2;
    int COMMENT_DOCUMENTATION = 3;
    int COMMENT_KEYWORD = 4;
    int COMMENT_MARKUP = 5;
    int RESERVED_WORD = 6;
    int RESERVED_WORD_2 = 7;
    int FUNCTION = 8;
    int LITERAL_BOOLEAN = 9;
    int LITERAL_NUMBER_DECIMAL_INT = 10;
    int LITERAL_NUMBER_FLOAT = 11;
    int LITERAL_NUMBER_HEXADECIMAL = 12;
    int LITERAL_STRING_DOUBLE_QUOTE = 13;
    int LITERAL_CHAR = 14;
    int LITERAL_BACKQUOTE = 15;
    int DATA_TYPE = 16;
    int VARIABLE = 17;
    int REGEX = 18;
    int ANNOTATION = 19;
    int IDENTIFIER = 20;
    int WHITESPACE = 21;
    int SEPARATOR = 22;
    int OPERATOR = 23;
    int PREPROCESSOR = 24;
    int MARKUP_TAG_DELIMITER = 25;
    int MARKUP_TAG_NAME = 26;
    int MARKUP_TAG_ATTRIBUTE = 27;
    int MARKUP_TAG_ATTRIBUTE_VALUE = 28;
    int MARKUP_COMMENT = 29;
    int MARKUP_DTD = 30;
    int MARKUP_PROCESSING_INSTRUCTION = 31;
    int MARKUP_CDATA_DELIMITER = 32;
    int MARKUP_CDATA = 33;
    int MARKUP_ENTITY_REFERENCE = 34;
    int ERROR_IDENTIFIER = 35;
    int ERROR_NUMBER_FORMAT = 36;
    int ERROR_STRING_DOUBLE = 37;
    int ERROR_CHAR = 38;
    int DEFAULT_NUM_TOKEN_TYPES = 39;
}

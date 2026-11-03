/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.xml;

/// Thrown by an [XMLParser] refusing malformed input (see
/// [XMLParser#setRejectMalformed(boolean)])
/// when the document is not well formed, nests deeper than
/// [XMLParser#setMaxDepth(int)], or references an entity the parser does not
/// define.
///
/// It extends `IllegalArgumentException`, which is what the parser has always
/// thrown when a [ParserCallback] asks it to stop, so code that catches that
/// keeps working.
@com.codename1.impl.SharedWithBackend
public class XMLParseException extends IllegalArgumentException {
    private final int line;
    private final int column;

    /// Creates an exception with no position.
    ///
    /// #### Parameters
    ///
    /// - `message`: what is wrong
    public XMLParseException(String message) {
        super(message);
        line = -1;
        column = -1;
    }

    /// Creates an exception at a position, which the message names as
    /// "line 3, column 7: ...".
    ///
    /// #### Parameters
    ///
    /// - `message`: what is wrong
    ///
    /// - `line`: the line of the offending character, from 1
    ///
    /// - `column`: its column, from 1 (0 before the first character of a line)
    public XMLParseException(String message, int line, int column) {
        super("line " + line + ", column " + column + ": " + message);
        this.line = line;
        this.column = column;
    }

    /// The line of the error, from 1, or -1 when it has no position.
    ///
    /// #### Returns
    ///
    /// the line
    public int getLine() {
        return line;
    }

    /// The column of the error, from 1, or -1 when it has no position.
    ///
    /// #### Returns
    ///
    /// the column
    public int getColumn() {
        return column;
    }
}

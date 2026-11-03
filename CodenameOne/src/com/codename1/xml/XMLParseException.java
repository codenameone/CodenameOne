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

/// Thrown by an [XMLParser] in strict mode (see [XMLParser#setStrict(boolean)])
/// when the document is not well formed, nests deeper than
/// [XMLParser#setMaxDepth(int)], or references an entity the parser does not
/// define.
///
/// It extends `IllegalArgumentException`, which is what the parser has always
/// thrown when a [ParserCallback] asks it to stop, so code that catches that
/// keeps working.
@com.codename1.impl.SharedWithBackend
public class XMLParseException extends IllegalArgumentException {
    /// Creates the exception with the description of what is wrong.
    ///
    /// #### Parameters
    ///
    /// - `message`: the description
    public XMLParseException(String message) {
        super(message);
    }
}

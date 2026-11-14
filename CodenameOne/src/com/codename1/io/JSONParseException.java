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
package com.codename1.io;

import java.io.IOException;

/// Thrown by [JSONParser#parseStrict(String)] and its overloads for a document
/// that is not JSON, carrying where in the text reading stopped.
@com.codename1.impl.SharedWithBackend
public class JSONParseException extends IOException {
    private final int offset;

    /// Creates the exception.
    ///
    /// #### Parameters
    ///
    /// - `message`: what was wrong
    ///
    /// - `offset`: the character offset at which it was found, or -1
    public JSONParseException(String message, int offset) {
        super(message);
        this.offset = offset;
    }

    /// The character offset in the document at which the error was found.
    ///
    /// #### Returns
    ///
    /// the offset, or -1 when no single position applies
    public int getOffset() {
        return offset;
    }
}

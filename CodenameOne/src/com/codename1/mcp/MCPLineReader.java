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
package com.codename1.mcp;

import java.io.IOException;
import java.io.Reader;

/// Reads a single newline delimited message from a {@link Reader}. Used instead of
/// {@code java.io.BufferedReader}, which is not available on every Codename One target
/// (notably the ParparVM Java runtime), so that the portable transports link on all
/// platforms. Carriage returns are stripped so both `\n` and `\r\n` framing work.
final class MCPLineReader {
    private MCPLineReader() {
    }

    static String readLine(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        int read;
        boolean any = false;
        while ((read = reader.read()) != -1) {
            any = true;
            char c = (char) read;
            if (c == '\n') {
                return stripCr(sb);
            }
            sb.append(c);
        }
        if (!any) {
            return null;
        }
        return stripCr(sb);
    }

    private static String stripCr(StringBuilder sb) {
        int length = sb.length();
        if (length > 0 && sb.charAt(length - 1) == '\r') {
            sb.setLength(length - 1);
        }
        return sb.toString();
    }
}

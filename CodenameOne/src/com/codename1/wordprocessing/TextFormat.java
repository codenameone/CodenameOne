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
package com.codename1.wordprocessing;

import java.util.ArrayList;
import java.util.List;

/// Plain text: one paragraph per line.
final class TextFormat {
    private TextFormat() {
    }

    static Document read(String text) {
        Ids ids = new Ids();
        List<Block> blocks = new ArrayList<Block>();
        int start = 0;
        int n = text.length();
        while (start <= n) {
            int end = start;
            while (end < n && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
                end++;
            }
            if (end == n && start == n && !blocks.isEmpty()) {
                break;
            }
            blocks.add(new Paragraph(ids.next('p'), RichText.of(text.substring(start, end))));
            if (end < n && text.charAt(end) == '\r' && end + 1 < n && text.charAt(end + 1) == '\n') {
                end++;
            }
            start = end + 1;
        }
        return new Document(blocks, null, null, ids.peek());
    }

    static String write(Document d) {
        return d.plainText() + "\n";
    }
}

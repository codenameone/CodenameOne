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
package com.codename1.cli;

import java.util.ArrayList;
import java.util.List;

/// Plays captured output onto a grid the way a terminal would -- carriage
/// return, line feed, cursor movement and erasing -- so a test can assert on
/// what the person sees rather than on escape codes.
final class VirtualScreen {
    private final List<StringBuilder> lines = new ArrayList<StringBuilder>();
    private int row;
    private int col;
    boolean cursorVisible = true;

    static VirtualScreen play(String output) {
        VirtualScreen s = new VirtualScreen();
        s.feed(output);
        return s;
    }

    int cursorRow() {
        return row;
    }

    int cursorColumn() {
        return col;
    }

    void feed(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == 27 && i + 1 < s.length() && s.charAt(i + 1) == '[') {
                int j = i + 2;
                while (j < s.length() && (s.charAt(j) < 0x40 || s.charAt(j) > 0x7e)) {
                    j++;
                }
                csi(s.substring(i + 2, j), s.charAt(j));
                i = j + 1;
                continue;
            }
            if (c == 27) {
                i += TextWidth.escapeLength(s, i);
                continue;
            }
            if (c == '\r') {
                col = 0;
            } else if (c == '\n') {
                row++;
                col = 0;
            } else {
                put(c);
            }
            i++;
        }
    }

    private void csi(String params, char op) {
        boolean priv = params.startsWith("?");
        String p = priv ? params.substring(1) : params;
        int n = 1;
        if (p.length() > 0 && p.indexOf(';') < 0) {
            n = Integer.parseInt(p);
        }
        switch (op) {
            case 'A':
                row = Math.max(0, row - n);
                break;
            case 'B':
                row += n;
                break;
            case 'C':
                col += n;
                break;
            case 'D':
                col = Math.max(0, col - n);
                break;
            case 'G':
                col = n - 1;
                break;
            case 'K':
                line(row).setLength(0);
                break;
            case 'J':
                StringBuilder l = line(row);
                if (l.length() > col) {
                    l.setLength(col);
                }
                while (lines.size() > row + 1) {
                    lines.remove(lines.size() - 1);
                }
                break;
            case 'h':
                if (priv && "25".equals(p)) {
                    cursorVisible = true;
                }
                break;
            case 'l':
                if (priv && "25".equals(p)) {
                    cursorVisible = false;
                }
                break;
            default:
                break;
        }
    }

    private StringBuilder line(int r) {
        while (lines.size() <= r) {
            lines.add(new StringBuilder());
        }
        return lines.get(r);
    }

    private void put(char c) {
        StringBuilder l = line(row);
        while (l.length() < col) {
            l.append(' ');
        }
        if (col < l.length()) {
            l.setCharAt(col, c);
        } else {
            l.append(c);
        }
        col++;
    }

    /// The visible lines, trailing spaces and empty trailing lines removed.
    List<String> lines() {
        List<String> out = new ArrayList<String>();
        for (StringBuilder l : lines) {
            String s = l.toString();
            int end = s.length();
            while (end > 0 && s.charAt(end - 1) == ' ') {
                end--;
            }
            out.add(s.substring(0, end));
        }
        while (!out.isEmpty() && out.get(out.size() - 1).length() == 0) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    String text() {
        StringBuilder sb = new StringBuilder();
        for (String l : lines()) {
            sb.append(l).append('\n');
        }
        return sb.toString();
    }
}

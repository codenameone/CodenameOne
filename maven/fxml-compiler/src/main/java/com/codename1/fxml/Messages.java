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
package com.codename1.fxml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// What the compilers found, each entry in the form an IDE and a build log
/// link back to the source: `file:line:column: message`.
///
/// An error fails the build once everything has been compiled -- every
/// document is still compiled, so that one run reports all of them. A
/// warning is printed and the build goes on: it is what a style sheet gets
/// for a property or a value this layer does not have, which a style sheet
/// written for the desktop is full of.
public final class Messages {

    private final List<String> errors = new ArrayList<String>();
    private final List<String> warnings = new ArrayList<String>();

    /// Creates an empty collection.
    public Messages() {
        // Nothing found yet.
    }

    /// The position of `offset` in `text` as `line:column`, both from 1.
    static String position(String text, int offset) {
        int line = 1;
        int column = 1;
        int end = Math.min(offset, text.length());
        for (int i = 0; i < end; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return line + ":" + column;
    }

    /// Records an error at a position.
    public void error(String file, int line, int column, String message) {
        errors.add(file + ":" + line + ":" + column + ": " + message);
    }

    /// Records an error at an offset in the text of a file.
    void error(String file, String text, int offset, String message) {
        errors.add(file + ":" + position(text, offset) + ": " + message);
    }

    /// Records a warning at an offset in the text of a file.
    void warning(String file, String text, int offset, String message) {
        warnings.add(file + ":" + position(text, offset) + ": " + message);
    }

    /// Records a warning about a file as a whole.
    void warning(String file, String message) {
        warnings.add(file + ": " + message);
    }

    /// The errors, in the order found.
    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    /// The warnings, in the order found.
    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    /// Whether anything was found that fails the build.
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

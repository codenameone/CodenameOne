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
package com.codename1.spreadsheet;

import com.codename1.ui.editor.SyntaxHighlightResult;
import com.codename1.ui.editor.SyntaxHighlighter;
import com.codename1.ui.editor.SyntaxToken;

import java.util.ArrayList;
import java.util.List;

/// Colors a formula in the `CodeEditor` a cell is edited in, as a spreadsheet does: each
/// reference in the color its outline has in the grid, text in quotes and errors set apart,
/// and the rest -- functions, numbers, operators -- in the color of the text, so that nothing
/// else can be mistaken for a reference. A value that does not start with `=` is left as it is.
final class FormulaHighlighter implements SyntaxHighlighter {
    /// The language name the editor knows formulas by.
    static final String LANGUAGE = "cn1-spreadsheet-formula";

    private static boolean registered;

    private FormulaHighlighter() {
    }

    /// Registers the highlighter with `CodeEditor`, once.
    static void register() {
        if (!registered) {
            registered = true;
            com.codename1.ui.CodeEditor.registerSyntaxHighlighter(LANGUAGE, new FormulaHighlighter());
        }
    }

    @Override
    public SyntaxHighlightResult tokenize(String line, int startState) {
        List<SyntaxToken> out = new ArrayList<SyntaxToken>();
        for (FormulaLexer.Token t : FormulaLexer.lex(line)) {
            int length = t.end - t.start;
            switch (t.kind) {
                case FormulaLexer.REFERENCE:
                    out.add(new SyntaxToken(t.start, length, SyntaxToken.PROPERTY,
                            FormulaLexer.referenceColor(t.color, false), FormulaLexer.referenceColor(t.color, true)));
                    break;
                case FormulaLexer.STRING:
                    out.add(new SyntaxToken(t.start, length, SyntaxToken.STRING, 0x8a5300, 0xe0b060));
                    break;
                case FormulaLexer.ERROR:
                    out.add(new SyntaxToken(t.start, length, SyntaxToken.COMMENT, 0xc4302b, 0xff7b72));
                    break;
                default:
                    break;
            }
        }
        return new SyntaxHighlightResult(out, 0);
    }
}

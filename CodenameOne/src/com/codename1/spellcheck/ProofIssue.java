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
package com.codename1.spellcheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A spelling or grammar problem in a run of text: its character range, a message and the
/// suggested replacements, best first.
public final class ProofIssue {
    /// The kind of a misspelled word.
    public static final String SPELLING = "spelling";

    /// The kind of a grammar or style problem.
    public static final String GRAMMAR = "grammar";

    /// The first character of the problem.
    public final int start;

    /// The character after the last one.
    public final int end;

    /// `SPELLING` or `GRAMMAR`.
    public final String kind;

    /// A message for the user.
    public final String message;

    /// Replacements for the range, best first; possibly empty.
    public final List<String> suggestions;

    /// Creates an issue.
    ///
    /// #### Parameters
    ///
    /// - `start`: the first character
    ///
    /// - `end`: the character after the last one
    ///
    /// - `kind`: `SPELLING` or `GRAMMAR`
    ///
    /// - `message`: the message
    ///
    /// - `suggestions`: the replacements, or null for none
    public ProofIssue(int start, int end, String kind, String message, List<String> suggestions) {
        this.start = start;
        this.end = end;
        this.kind = kind;
        this.message = message;
        this.suggestions = suggestions == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(suggestions));
    }

    /// The same issue with other suggestions.
    ///
    /// #### Parameters
    ///
    /// - `s`: the suggestions
    ///
    /// #### Returns
    ///
    /// the copy
    public ProofIssue withSuggestions(List<String> s) {
        return new ProofIssue(start, end, kind, message, s);
    }

    /// Is this a misspelling?
    ///
    /// #### Returns
    ///
    /// true for `SPELLING`
    public boolean isSpelling() {
        return SPELLING.equals(kind);
    }

    @Override
    public String toString() {
        return kind + "[" + start + "," + end + "] " + message;
    }
}

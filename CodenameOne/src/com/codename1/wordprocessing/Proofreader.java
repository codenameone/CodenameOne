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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Spelling and grammar checks together, as a `DocumentView` uses them to underline problems.
/// Results are remembered per text, so a paragraph is checked again only when it changes.
///
/// ```java
/// Proofreader p = new Proofreader(SpellChecker.create(dictionary), GrammarChecker.english());
/// view.setProofreader(p);
/// ```
public final class Proofreader {
    private static final int CACHE = 512;

    private final SpellChecker spelling;
    private final GrammarChecker grammar;
    private final Map<String, List<ProofIssue>> cache = new Cache();

    /// Creates a proofreader; either checker may be null.
    ///
    /// #### Parameters
    ///
    /// - `spelling`: the spell checker, or null
    ///
    /// - `grammar`: the grammar checker, or null
    public Proofreader(SpellChecker spelling, GrammarChecker grammar) {
        this.spelling = spelling;
        this.grammar = grammar;
    }

    private static final class Cache extends LinkedHashMap<String, List<ProofIssue>> {
        private static final long serialVersionUID = 1L;

        Cache() {
            super(64, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<ProofIssue>> e) {
            return size() > CACHE;
        }
    }

    private static final class ByPosition implements Comparator<ProofIssue> {
        @Override
        public int compare(ProofIssue a, ProofIssue b) {
            return a.start != b.start ? a.start - b.start : a.end - b.end;
        }
    }

    /// The spell checker.
    ///
    /// #### Returns
    ///
    /// the checker, or null
    public SpellChecker getSpellChecker() {
        return spelling;
    }

    /// The grammar checker.
    ///
    /// #### Returns
    ///
    /// the checker, or null
    public GrammarChecker getGrammarChecker() {
        return grammar;
    }

    /// The problems in a text, by position, without spelling suggestions.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text, typically a paragraph
    ///
    /// #### Returns
    ///
    /// the issues; do not modify the list
    public synchronized List<ProofIssue> check(String text) {
        List<ProofIssue> out = cache.get(text);
        if (out == null) {
            out = new ArrayList<ProofIssue>();
            if (spelling != null) {
                out.addAll(spelling.check(text));
            }
            if (grammar != null) {
                out.addAll(grammar.check(text));
            }
            Collections.sort(out, new ByPosition());
            out = Collections.unmodifiableList(out);
            cache.put(text, out);
        }
        return out;
    }

    /// The problem at a character of a text, with suggestions filled in for a misspelling.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text
    ///
    /// - `offset`: the character
    ///
    /// - `max`: the most suggestions
    ///
    /// #### Returns
    ///
    /// the issue, or null when there is none there
    public ProofIssue issueAt(String text, int offset, int max) {
        for (ProofIssue i : check(text)) {
            if (offset >= i.start && offset <= i.end) {
                if (i.isSpelling() && spelling != null) {
                    return i.withSuggestions(spelling.suggest(text.substring(i.start, i.end),
                            max));
                }
                return i;
            }
        }
        return null;
    }

    /// Forgets remembered results, after the user dictionary or the rules changed.
    public synchronized void invalidate() {
        cache.clear();
    }
}

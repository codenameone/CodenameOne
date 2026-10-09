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
package com.codename1.desktopcompat.org.fife.ui.rtextarea;

import com.codename1.desktopcompat.javax.swing.JTextArea;
import com.codename1.desktopcompat.org.fife.ui.rsyntaxtextarea.DocumentRange;
import com.codename1.io.Log;
import java.util.ArrayList;
import java.util.List;

/// Find, replace and mark in a text area, by a [SearchContext].
///
/// Plain text searches work as on the desktop: with or without case,
/// whole words only, forward or backward from the caret, and wrapping
/// around the end. A regular expression search finds nothing and says so
/// once in the log.
public final class SearchEngine {

    private static boolean regexLogged;

    private SearchEngine() {
    }

    /// Whether this search can run; a regular expression cannot.
    private static boolean possible(SearchContext context) {
        String what = context.getSearchFor();
        if (what == null || what.length() == 0) {
            return false;
        }
        if (context.isRegularExpression()) {
            if (!regexLogged) {
                regexLogged = true;
                Log.p("Regular expression search is not available in the RSyntaxTextArea of this layer;"
                        + " such a search finds nothing.");
            }
            return false;
        }
        return true;
    }

    private static boolean wordChar(char c) {
        return c == '_' || c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c > 127;
    }

    private static boolean isWord(String in, int at, int length) {
        if (at > 0 && wordChar(in.charAt(at - 1))) {
            return false;
        }
        int end = at + length;
        return end >= in.length() || !wordChar(in.charAt(end));
    }

    /// The first match at or after `from` going forward, or the last one
    /// that ends at or before `from` going backward; -1 for none.
    private static int next(String what, String in, int from, boolean forward, boolean matchCase,
            boolean wholeWord) {
        int n = what.length();
        if (forward) {
            for (int i = Math.max(0, from); i + n <= in.length(); i++) {
                if (in.regionMatches(!matchCase, i, what, 0, n) && (!wholeWord || isWord(in, i, n))) {
                    return i;
                }
            }
        } else {
            for (int i = Math.min(from, in.length()) - n; i >= 0; i--) {
                if (in.regionMatches(!matchCase, i, what, 0, n) && (!wholeWord || isWord(in, i, n))) {
                    return i;
                }
            }
        }
        return -1;
    }

    /// The position of the next match of `searchFor` in `searchIn`: the
    /// first one going forward, the last one going backward, -1 for none.
    public static int getNextMatchPos(String searchFor, String searchIn, boolean forward,
            boolean matchCase, boolean wholeWord) {
        if (searchFor == null || searchIn == null || searchFor.length() == 0) {
            return -1;
        }
        return next(searchFor, searchIn, forward ? 0 : searchIn.length(), forward, matchCase, wholeWord);
    }

    private static String text(JTextArea area) {
        String s = area.getText();
        return s == null ? "" : s;
    }

    private static int markAll(JTextArea area, SearchContext context, String in) {
        if (!(area instanceof RTextArea)) {
            return 0;
        }
        RTextArea rta = (RTextArea) area;
        if (!context.getMarkAll() || !possible(context)) {
            rta.clearMarkAllHighlights();
            return 0;
        }
        String what = context.getSearchFor();
        List<DocumentRange> ranges = new ArrayList<DocumentRange>();
        int at = 0;
        while ((at = next(what, in, at, true, context.getMatchCase(), context.getWholeWord())) >= 0) {
            ranges.add(new DocumentRange(at, at + what.length()));
            at += what.length();
        }
        rta.markAll(ranges);
        return ranges.size();
    }

    /// Finds the next occurrence from the caret and selects it. With
    /// [SearchContext#getMarkAll] on, and a [RTextArea], every occurrence
    /// is marked as well.
    public static SearchResult find(JTextArea textArea, SearchContext context) {
        String in = text(textArea);
        int marked = markAll(textArea, context, in);
        if (!possible(context)) {
            return new SearchResult(null, 0, marked);
        }
        String what = context.getSearchFor();
        boolean forward = context.getSearchForward();
        int from = forward ? Math.max(textArea.getSelectionEnd(), textArea.getCaretPosition())
                : Math.min(textArea.getSelectionStart(), textArea.getCaretPosition());
        if (textArea.getSelectionStart() == textArea.getSelectionEnd()) {
            from = textArea.getCaretPosition();
        }
        int at = next(what, in, from, forward, context.getMatchCase(), context.getWholeWord());
        boolean wrapped = false;
        if (at < 0 && context.getSearchWrap()) {
            at = next(what, in, forward ? 0 : in.length(), forward, context.getMatchCase(),
                    context.getWholeWord());
            wrapped = at >= 0;
        }
        if (at < 0) {
            return new SearchResult(null, 0, marked);
        }
        textArea.setCaretPosition(at);
        textArea.moveCaretPosition(at + what.length());
        SearchResult result = new SearchResult(new DocumentRange(at, at + what.length()), 1, marked);
        result.setWrapped(wrapped);
        return result;
    }

    /// Marks every occurrence, or clears the marks when the context does
    /// not ask for marking or names nothing to look for.
    public static SearchResult markAll(RTextArea textArea, SearchContext context) {
        return new SearchResult(null, 0, markAll(textArea, context, text(textArea)));
    }

    /// Replaces the occurrence at the selection, when the selection is
    /// one, and selects the next occurrence.
    public static SearchResult replace(RTextArea textArea, SearchContext context) {
        if (!possible(context)) {
            return new SearchResult(null, 0, markAll(textArea, context, text(textArea)));
        }
        String what = context.getSearchFor();
        String with = context.getReplaceWith() == null ? "" : context.getReplaceWith();
        String in = text(textArea);
        int start = textArea.getSelectionStart();
        int end = textArea.getSelectionEnd();
        boolean forward = context.getSearchForward();
        int at = -1;
        if (end - start == what.length() && start >= 0 && end <= in.length()
                && in.regionMatches(!context.getMatchCase(), start, what, 0, what.length())
                && (!context.getWholeWord() || isWord(in, start, what.length()))) {
            at = start;
        } else {
            at = next(what, in, forward ? start : end, forward, context.getMatchCase(), context.getWholeWord());
            if (at < 0 && context.getSearchWrap()) {
                at = next(what, in, forward ? 0 : in.length(), forward, context.getMatchCase(),
                        context.getWholeWord());
            }
        }
        if (at < 0) {
            return new SearchResult(null, 0, markAll(textArea, context, in));
        }
        textArea.replaceRange(with, at, at + what.length());
        textArea.setCaretPosition(forward ? at + with.length() : at);
        SearchResult following = find(textArea, context);
        DocumentRange range = following.getMatchRange() != null ? following.getMatchRange()
                : new DocumentRange(at, at + with.length());
        SearchResult result = new SearchResult(range, 1, following.getMarkedCount());
        result.setWrapped(following.isWrapped());
        return result;
    }

    /// Replaces every occurrence in the text.
    public static SearchResult replaceAll(RTextArea textArea, SearchContext context) {
        if (!possible(context)) {
            return new SearchResult(null, 0, markAll(textArea, context, text(textArea)));
        }
        String what = context.getSearchFor();
        String with = context.getReplaceWith() == null ? "" : context.getReplaceWith();
        String in = text(textArea);
        StringBuilder out = new StringBuilder(in.length());
        int count = 0;
        int from = 0;
        int last = -1;
        int at;
        while ((at = next(what, in, from, true, context.getMatchCase(), context.getWholeWord())) >= 0) {
            out.append(in.substring(from, at)).append(with);
            last = out.length() - with.length();
            from = at + what.length();
            count++;
        }
        if (count == 0) {
            return new SearchResult(null, 0, markAll(textArea, context, in));
        }
        out.append(in.substring(from));
        textArea.setText(out.toString());
        textArea.setCaretPosition(Math.min(out.length(), last + with.length()));
        textArea.clearMarkAllHighlights();
        return new SearchResult(new DocumentRange(last, last + with.length()), count, 0);
    }
}

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

import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeSupport;

/// What a search looks for and how: the text, and whether case matters,
/// whole words only, forward or backward, wrapping, and marking every
/// occurrence. [SearchEngine] reads it.
///
/// A regular expression search is not available in this layer: with
/// [#setRegularExpression] on, a search finds nothing and says so once in
/// the log.
public class SearchContext {

    public static final String PROPERTY_SEARCH_FOR = "Search.searchFor";
    public static final String PROPERTY_REPLACE_WITH = "Search.replaceWith";
    public static final String PROPERTY_MATCH_CASE = "Search.MatchCase";
    public static final String PROPERTY_MATCH_WHOLE_WORD = "Search.MatchWholeWord";
    public static final String PROPERTY_SEARCH_FORWARD = "Search.Forward";
    public static final String PROPERTY_SEARCH_WRAP = "Search.Wrap";
    public static final String PROPERTY_SELECTION_ONLY = "Search.SelectionOnly";
    public static final String PROPERTY_USE_REGEX = "Search.UseRegex";
    public static final String PROPERTY_MARK_ALL = "Search.MarkAll";

    private String searchFor;
    private String replaceWith;
    private boolean matchCase;
    private boolean wholeWord;
    private boolean forward = true;
    private boolean wrap;
    private boolean selectionOnly;
    private boolean regex;
    private boolean markAll = true;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    public SearchContext() {
        this(null);
    }

    public SearchContext(String searchFor) {
        this(searchFor, false);
    }

    public SearchContext(String searchFor, boolean matchCase) {
        this.searchFor = searchFor;
        this.matchCase = matchCase;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        support.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        support.removePropertyChangeListener(l);
    }

    protected void firePropertyChange(String property, boolean oldValue, boolean newValue) {
        support.firePropertyChange(property, oldValue, newValue);
    }

    protected void firePropertyChange(String property, String oldValue, String newValue) {
        support.firePropertyChange(property, oldValue, newValue);
    }

    /// A copy with the same settings and no listeners.
    @Override
    public SearchContext clone() {
        SearchContext copy = new SearchContext(searchFor, matchCase);
        copy.replaceWith = replaceWith;
        copy.wholeWord = wholeWord;
        copy.forward = forward;
        copy.wrap = wrap;
        copy.selectionOnly = selectionOnly;
        copy.regex = regex;
        copy.markAll = markAll;
        return copy;
    }

    public boolean getMarkAll() {
        return markAll;
    }

    public boolean getMatchCase() {
        return matchCase;
    }

    public String getReplaceWith() {
        return replaceWith;
    }

    public String getSearchFor() {
        return searchFor;
    }

    public boolean getSearchForward() {
        return forward;
    }

    public boolean getSearchWrap() {
        return wrap;
    }

    public boolean getSearchSelectionOnly() {
        return selectionOnly;
    }

    public boolean getWholeWord() {
        return wholeWord;
    }

    public boolean isRegularExpression() {
        return regex;
    }

    public void setMarkAll(boolean markAll) {
        boolean old = this.markAll;
        this.markAll = markAll;
        firePropertyChange(PROPERTY_MARK_ALL, old, markAll);
    }

    public void setMatchCase(boolean matchCase) {
        boolean old = this.matchCase;
        this.matchCase = matchCase;
        firePropertyChange(PROPERTY_MATCH_CASE, old, matchCase);
    }

    /// Recorded, and then every search finds nothing: regular expressions
    /// are not available.
    public void setRegularExpression(boolean regex) {
        boolean old = this.regex;
        this.regex = regex;
        firePropertyChange(PROPERTY_USE_REGEX, old, regex);
    }

    public void setReplaceWith(String replaceWith) {
        String old = this.replaceWith;
        this.replaceWith = replaceWith;
        firePropertyChange(PROPERTY_REPLACE_WITH, old, replaceWith);
    }

    public void setSearchFor(String searchFor) {
        String old = this.searchFor;
        this.searchFor = searchFor;
        firePropertyChange(PROPERTY_SEARCH_FOR, old, searchFor);
    }

    public void setSearchForward(boolean forward) {
        boolean old = this.forward;
        this.forward = forward;
        firePropertyChange(PROPERTY_SEARCH_FORWARD, old, forward);
    }

    public void setSearchWrap(boolean wrap) {
        boolean old = this.wrap;
        this.wrap = wrap;
        firePropertyChange(PROPERTY_SEARCH_WRAP, old, wrap);
    }

    /// Recorded only: a search always covers the whole text.
    public void setSearchSelectionOnly(boolean selectionOnly) {
        boolean old = this.selectionOnly;
        this.selectionOnly = selectionOnly;
        firePropertyChange(PROPERTY_SELECTION_ONLY, old, selectionOnly);
    }

    public void setWholeWord(boolean wholeWord) {
        boolean old = this.wholeWord;
        this.wholeWord = wholeWord;
        firePropertyChange(PROPERTY_MATCH_WHOLE_WORD, old, wholeWord);
    }

    @Override
    public String toString() {
        return "[SearchContext: searchFor=" + searchFor + ", replaceWith=" + replaceWith
                + ", matchCase=" + matchCase + ", wholeWord=" + wholeWord + ", regex=" + regex
                + ", markAll=" + markAll + "]";
    }
}

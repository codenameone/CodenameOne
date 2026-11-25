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
package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

import java.util.ArrayList;
import java.util.List;

/// A text field that suggests people (or tags, or channels) as the user types
/// an `@` mention, and completes the name when one is tapped.
///
/// ```java
/// MentionField comment = new MentionField();
/// comment.setCandidates("Ada Lovelace", "Alan Turing", "Grace Hopper");
/// comment.getTextField().setHint("Write a comment");
/// form.add(comment);
/// // later
/// List<String> notify = comment.getMentions();
/// ```
///
/// The suggestions show under the field, in the layout, while the word at the
/// cursor starts with the trigger (`@` by default, see `#setTrigger(char)`)
/// and matches a candidate: one whose name, or any word of it, starts with
/// what was typed after the trigger, ignoring case. Picking one replaces that
/// word with the trigger, the name and a space. A trigger in the middle of a
/// word, as in an email address, does not start a mention.
///
/// For a directory too large to list up front, override `#suggest(String)`.
///
/// UIIDs: `MentionField` for the container, `MentionFieldSuggestions` for the
/// list and `MentionFieldSuggestion` for each row, falling back to
/// `TextField`, `Container` and a `Label` look.
public class MentionField extends Container {
    private final TextField field = new TextField();
    private final Container suggestions = new Container(BoxLayout.y());
    private final List<String> candidates = new ArrayList<String>();
    private final List<String> mentions = new ArrayList<String>();
    private final EventDispatcher listeners = new EventDispatcher();
    private char trigger = '@';
    private int maxSuggestions = 5;
    private int tokenStart = -1;
    private int tokenEnd = -1;
    private boolean picking;

    /// An empty mention field.
    public MentionField() {
        super(BoxLayout.y());
        setUIID("MentionField");
        suggestions.setUIID(themed("MentionFieldSuggestions", "Container"));
        suggestions.setHidden(true);
        field.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                updateSuggestions();
            }
        });
        add(field).add(suggestions);
    }

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
            unselectedStyle.setBgTransparency(0);
        }
    }

    private String themed(String uiid, String fallback) {
        return getUIManager().isThemeStyleDefined(uiid) ? uiid : fallback;
    }

    /// The text field the user types into.
    ///
    /// #### Returns
    ///
    /// the field
    public TextField getTextField() {
        return field;
    }

    /// The field's text.
    ///
    /// #### Returns
    ///
    /// the text
    public String getText() {
        return field.getText();
    }

    /// Sets the field's text and updates the suggestions for its last word.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text
    public void setText(String text) {
        field.setText(text);
        updateSuggestions();
    }

    /// Sets the names offered.
    ///
    /// #### Parameters
    ///
    /// - `names`: the candidates
    public void setCandidates(String... names) {
        candidates.clear();
        if (names != null) {
            for (String n : names) {
                candidates.add(n);
            }
        }
        updateSuggestions();
    }

    /// The character that starts a mention.
    ///
    /// #### Returns
    ///
    /// `@` by default
    public char getTrigger() {
        return trigger;
    }

    /// Sets the character that starts a mention, such as `#` for tags.
    ///
    /// #### Parameters
    ///
    /// - `trigger`: the trigger character
    public void setTrigger(char trigger) {
        this.trigger = trigger;
        updateSuggestions();
    }

    /// Sets how many suggestions show at most.
    ///
    /// #### Parameters
    ///
    /// - `max`: the limit, 5 by default
    public void setMaxSuggestions(int max) {
        maxSuggestions = Math.max(1, max);
        updateSuggestions();
    }

    /// The names the user picked from the suggestions that are still in the
    /// text, in the order picked.
    ///
    /// #### Returns
    ///
    /// the mentioned names
    public List<String> getMentions() {
        List<String> out = new ArrayList<String>();
        String text = field.getText() == null ? "" : field.getText();
        List<String> stale = new ArrayList<String>();
        for (String m : mentions) {
            if (!out.contains(m)) {
                if (containsMention(text, trigger + m)) {
                    out.add(m);
                } else {
                    stale.add(m);
                }
            }
        }
        mentions.removeAll(stale);
        return out;
    }

    /// True when `token` appears with a word boundary on both sides, so a
    /// picked "@Ann" is not found inside "@Anna" or "x@Ann".
    private static boolean containsMention(String text, String token) {
        int from = 0;
        while (true) {
            int i = text.indexOf(token, from);
            if (i < 0) {
                return false;
            }
            int end = i + token.length();
            boolean startOk = i == 0 || Character.isWhitespace(text.charAt(i - 1));
            boolean endOk = end == text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (startOk && endOk) {
                return true;
            }
            from = i + 1;
        }
    }

    /// Adds a listener called when the user picks a suggestion.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addActionListener(ActionListener l) {
        listeners.addListener(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeActionListener(ActionListener l) {
        listeners.removeListener(l);
    }

    /// True while the suggestion list shows.
    ///
    /// #### Returns
    ///
    /// whether suggestions are visible
    public boolean isShowingSuggestions() {
        return !suggestions.isHidden();
    }

    /// The suggestions for what was typed after the trigger. Override to query
    /// a server or a large directory; the default matches the candidates.
    ///
    /// #### Parameters
    ///
    /// - `query`: the text after the trigger, possibly empty
    ///
    /// #### Returns
    ///
    /// the names to offer, best first
    protected List<String> suggest(String query) {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < candidates.size() && out.size() < maxSuggestions; i++) {
            String c = candidates.get(i);
            if (matches(c, query)) {
                out.add(c);
            }
        }
        return out;
    }

    /// True when the name, or a word of it, starts with the query, ignoring case.
    static boolean matches(String name, String query) {
        if (query.length() == 0) {
            return true;
        }
        for (int i = 0; i < name.length(); i++) {
            boolean wordStart = i == 0 || name.charAt(i - 1) == ' ';
            if (wordStart && name.regionMatches(true, i, query, 0, query.length())) {
                return true;
            }
        }
        return false;
    }

    /// Picks a suggestion as a tap would.
    ///
    /// #### Parameters
    ///
    /// - `name`: the name to insert in place of the word being typed
    public void pick(String name) {
        String text = field.getText() == null ? "" : field.getText();
        if (tokenStart < 0 || tokenEnd > text.length()) {
            return;
        }
        String before = text.substring(0, tokenStart);
        String after = text.substring(tokenEnd);
        String inserted = trigger + name + " ";
        if (after.length() > 0 && after.charAt(0) == ' ') {
            inserted = trigger + name;
        }
        mentions.add(name);
        tokenStart = -1;
        tokenEnd = -1;
        // setText notifies the data listener before the cursor moves, and the
        // scan would find the completed name and offer it again.
        picking = true;
        try {
            field.setText(before + inserted + after);
            field.setCursorPosition(before.length() + inserted.length());
        } finally {
            picking = false;
        }
        hideSuggestions();
        listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Other));
    }

    private void hideSuggestions() {
        if (!suggestions.isHidden()) {
            suggestions.removeAll();
            suggestions.setHidden(true);
            revalidateLater();
        }
    }

    private void updateSuggestions() {
        if (picking) {
            return;
        }
        String text = field.getText() == null ? "" : field.getText();
        int cursor = cursor(field, text);
        int start = cursor;
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))
                && text.charAt(start - 1) != trigger) {
            start--;
        }
        boolean mention = start > 0 && text.charAt(start - 1) == trigger
                && (start == 1 || Character.isWhitespace(text.charAt(start - 2)));
        if (!mention) {
            tokenStart = -1;
            hideSuggestions();
            return;
        }
        tokenStart = start - 1;
        tokenEnd = cursor;
        List<String> found = suggest(text.substring(start, cursor));
        if (found.isEmpty()) {
            hideSuggestions();
            return;
        }
        suggestions.removeAll();
        String uiid = themed("MentionFieldSuggestion", "Label");
        for (int i = 0; i < found.size() && i < maxSuggestions; i++) {
            final String name = found.get(i);
            Button b = new Button(name);
            b.setUIID(uiid);
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    pick(name);
                }
            });
            suggestions.add(b);
        }
        suggestions.setHidden(false);
        revalidateLater();
    }

    private static int cursor(TextArea f, String text) {
        int c = f.getCursorPosition();
        if (c < 0 || c > text.length()) {
            return text.length();
        }
        // A field that is not being edited reports 0; read that as the end.
        // While editing, 0 is a real position at the start of the text.
        return c == 0 && !f.isEditing() ? text.length() : c;
    }
}

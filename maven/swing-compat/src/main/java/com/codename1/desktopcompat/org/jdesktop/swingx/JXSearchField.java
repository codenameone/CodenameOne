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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.javax.swing.Timer;
import com.codename1.desktopcompat.javax.swing.event.DocumentEvent;
import com.codename1.desktopcompat.javax.swing.event.DocumentListener;
import com.codename1.desktopcompat.javax.swing.text.Document;

/// A text field for search text. It tells its action listeners when to
/// search: in the regular mode when the edit is finished with Enter or
/// the keyboard's Done, in the instant mode a short while after every
/// change of the text.
///
/// The cancel action runs when Escape is pressed, where the platform
/// delivers that key to a text field; the one a field starts with clears
/// the text.
///
/// ## What differs from SwingX
///
/// The field is a plain Codename One text field with a "Search" prompt.
/// It has no find, cancel or popup buttons, so everything about them is
/// absent: the buttons, the find action, the layout style, the find popup
/// menu and the recent searches.
public class JXSearchField extends JXTextField {

    /// When the action listeners are told to search.
    public enum SearchMode {
        /// When the edit is finished.
        REGULAR,
        /// After every change of the text, once the instant search delay
        /// has passed without another one.
        INSTANT
    }

    private SearchMode searchMode = SearchMode.INSTANT;
    private int instantSearchDelay = 180;
    private ActionListener cancelAction;
    // No initializers: the superclass constructor sets the document, and
    // with it these, before this class's initializers would run.
    private DocumentListener changes;
    private Timer instantSearchTimer;

    public JXSearchField() {
        this("Search");
    }

    public JXSearchField(String prompt) {
        super(prompt);
        enableEvents(AWTEvent.KEY_EVENT_MASK);
    }

    @Override
    public void setDocument(Document doc) {
        if (changes == null) {
            changes = new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    cn1TextChanged();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    cn1TextChanged();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    cn1TextChanged();
                }
            };
        }
        Document old = getDocument();
        if (old != null) {
            old.removeDocumentListener(changes);
        }
        super.setDocument(doc);
        if (doc != null) {
            doc.addDocumentListener(changes);
        }
    }

    private void cn1TextChanged() {
        if (searchMode != SearchMode.INSTANT) {
            return;
        }
        Timer t = getInstantSearchTimer();
        if (instantSearchDelay > 0) {
            t.setInitialDelay(instantSearchDelay);
            t.restart();
        } else {
            postActionEvent();
        }
    }

    public SearchMode getSearchMode() {
        return searchMode;
    }

    public boolean isInstantSearchMode() {
        return searchMode == SearchMode.INSTANT;
    }

    public boolean isRegularSearchMode() {
        return searchMode == SearchMode.REGULAR;
    }

    public void setSearchMode(SearchMode searchMode) {
        if (searchMode == null) {
            throw new NullPointerException("searchMode must not be null");
        }
        SearchMode old = this.searchMode;
        this.searchMode = searchMode;
        if (searchMode != SearchMode.INSTANT && instantSearchTimer != null) {
            instantSearchTimer.stop();
        }
        firePropertyChange("searchMode", old, searchMode);
    }

    public int getInstantSearchDelay() {
        return instantSearchDelay;
    }

    /// Sets how many milliseconds the text must rest in the instant mode
    /// before the action listeners are told; 0 tells them at once.
    public void setInstantSearchDelay(int instantSearchDelay) {
        int old = this.instantSearchDelay;
        this.instantSearchDelay = instantSearchDelay;
        firePropertyChange("instantSearchDelay", old, instantSearchDelay);
    }

    /// The action Escape runs; the one a field starts with clears the
    /// text.
    public final ActionListener getCancelAction() {
        if (cancelAction == null) {
            cancelAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setText(null);
                    requestFocusInWindow();
                }
            };
        }
        return cancelAction;
    }

    /// Sets the action Escape runs; `null` goes back to clearing the
    /// text.
    public final void setCancelAction(ActionListener cancelAction) {
        ActionListener old = this.cancelAction;
        this.cancelAction = cancelAction;
        firePropertyChange("cancelAction", old, cancelAction);
    }

    /// The timer that waits out the instant search delay. It does not
    /// repeat and tells the action listeners when it fires.
    public Timer getInstantSearchTimer() {
        if (instantSearchTimer == null) {
            instantSearchTimer = new Timer(0, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    postActionEvent();
                }
            });
            instantSearchTimer.setRepeats(false);
        }
        return instantSearchTimer;
    }

    /// Tells the action listeners to search now, and stops a pending
    /// instant search.
    @Override
    public void postActionEvent() {
        if (instantSearchTimer != null) {
            instantSearchTimer.stop();
        }
        super.postActionEvent();
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (isEnabled() && e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE
                && !e.isConsumed()) {
            getCancelAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "cancel"));
            e.consume();
        }
        super.processKeyEvent(e);
    }
}

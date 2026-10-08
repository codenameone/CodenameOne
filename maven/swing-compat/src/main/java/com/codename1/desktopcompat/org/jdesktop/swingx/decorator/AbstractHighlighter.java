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
package com.codename1.desktopcompat.org.jdesktop.swingx.decorator;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import java.util.ArrayList;

/// The base of the highlighters: a predicate, the listener list, and the
/// rule that a cell is decorated when the predicate matches it and
/// [#canHighlight(Component, ComponentAdapter)] agrees.
///
/// The listener list is private here; SwingX exposes it as a protected
/// field of a weak list type this layer does not have.
public abstract class AbstractHighlighter implements Highlighter {

    private final ArrayList<ChangeListener> listeners = new ArrayList<ChangeListener>();
    private HighlightPredicate predicate;

    public AbstractHighlighter() {
        this(null);
    }

    /// Makes a highlighter for the cells `predicate` matches; `null` is
    /// [HighlightPredicate#ALWAYS].
    public AbstractHighlighter(HighlightPredicate predicate) {
        this.predicate = predicate == null ? HighlightPredicate.ALWAYS : predicate;
    }

    public void setHighlightPredicate(HighlightPredicate predicate) {
        HighlightPredicate p = predicate == null ? HighlightPredicate.ALWAYS : predicate;
        if (areEqual(p, this.predicate)) {
            return;
        }
        this.predicate = p;
        fireStateChanged();
    }

    public HighlightPredicate getHighlightPredicate() {
        return predicate;
    }

    @Override
    public Component highlight(Component component, ComponentAdapter adapter) {
        if (canHighlight(component, adapter) && predicate.isHighlighted(component, adapter)) {
            return doHighlight(component, adapter);
        }
        return component;
    }

    /// Whether this highlighter is able to decorate the component at all,
    /// asked before the predicate.
    protected boolean canHighlight(Component component, ComponentAdapter adapter) {
        return true;
    }

    /// Decorates the component of a cell the predicate matched.
    protected abstract Component doHighlight(Component component, ComponentAdapter adapter);

    protected boolean areEqual(Object oneItem, Object anotherItem) {
        return oneItem == null ? anotherItem == null : oneItem.equals(anotherItem);
    }

    @Override
    public final void addChangeListener(ChangeListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    @Override
    public final void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    @Override
    public final ChangeListener[] getChangeListeners() {
        return listeners.toArray(new ChangeListener[listeners.size()]);
    }

    protected final void fireStateChanged() {
        if (listeners.isEmpty()) {
            return;
        }
        ChangeEvent e = new ChangeEvent(this);
        ChangeListener[] all = getChangeListeners();
        for (int i = all.length - 1; i >= 0; i--) {
            all[i].stateChanged(e);
        }
    }
}

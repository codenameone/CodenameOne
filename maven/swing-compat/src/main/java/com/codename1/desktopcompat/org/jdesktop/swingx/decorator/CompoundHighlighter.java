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
import java.util.List;

/// Runs a list of highlighters in order, each on the component the one
/// before it answered, so a later highlighter wins where two set the same
/// property. A change of any of them is reported as a change of this one.
public class CompoundHighlighter extends AbstractHighlighter {

    public static final Highlighter[] EMPTY_HIGHLIGHTERS = new Highlighter[0];

    protected List<Highlighter> highlighters;

    private ChangeListener highlighterChangeListener;

    public CompoundHighlighter(Highlighter... inList) {
        this(null, inList);
    }

    public CompoundHighlighter(HighlightPredicate predicate, Highlighter... inList) {
        super(predicate);
        highlighters = new ArrayList<Highlighter>();
        cn1Set(inList);
    }

    /// Replaces the list.
    public void setHighlighters(Highlighter... inList) {
        if (inList == null) {
            throw new NullPointerException("highlighters must not be null");
        }
        if (highlighters.isEmpty() && inList.length == 0) {
            return;
        }
        cn1Set(inList);
        fireStateChanged();
    }

    private void cn1Set(Highlighter[] inList) {
        if (inList == null) {
            throw new NullPointerException("highlighters must not be null");
        }
        for (int i = 0; i < inList.length; i++) {
            if (inList[i] == null) {
                throw new NullPointerException("highlighter must not be null");
            }
        }
        for (int i = 0; i < highlighters.size(); i++) {
            highlighters.get(i).removeChangeListener(cn1Listener());
        }
        highlighters.clear();
        for (int i = 0; i < inList.length; i++) {
            highlighters.add(inList[i]);
            inList[i].addChangeListener(cn1Listener());
        }
    }

    /// Appends a highlighter.
    public void addHighlighter(Highlighter highlighter) {
        addHighlighter(highlighter, false);
    }

    /// Adds a highlighter at the start of the list or at its end.
    public void addHighlighter(Highlighter highlighter, boolean prepend) {
        if (highlighter == null) {
            throw new NullPointerException("highlighter must not be null");
        }
        if (prepend) {
            highlighters.add(0, highlighter);
        } else {
            highlighters.add(highlighter);
        }
        highlighter.addChangeListener(cn1Listener());
        fireStateChanged();
    }

    public void removeHighlighter(Highlighter hl) {
        if (highlighters.remove(hl)) {
            hl.removeChangeListener(cn1Listener());
            fireStateChanged();
        }
    }

    public Highlighter[] getHighlighters() {
        if (highlighters.isEmpty()) {
            return EMPTY_HIGHLIGHTERS;
        }
        return highlighters.toArray(new Highlighter[highlighters.size()]);
    }

    private ChangeListener cn1Listener() {
        if (highlighterChangeListener == null) {
            highlighterChangeListener = new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    fireStateChanged();
                }
            };
        }
        return highlighterChangeListener;
    }

    /// The listener this highlighter has on each of the ones it contains.
    protected ChangeListener getHighlighterChangeListener() {
        return cn1Listener();
    }

    @Override
    protected Component doHighlight(Component component, ComponentAdapter adapter) {
        Component c = component;
        for (int i = 0; i < highlighters.size(); i++) {
            c = highlighters.get(i).highlight(c, adapter);
        }
        return c;
    }
}

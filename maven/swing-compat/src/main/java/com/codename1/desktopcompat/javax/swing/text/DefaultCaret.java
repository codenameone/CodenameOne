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
package com.codename1.desktopcompat.javax.swing.text;

import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;

/// The caret every text component starts with: a dot and a mark.
///
/// It paints nothing. The insertion point the user sees is the one the
/// Codename One text widget draws while it is being edited, and a
/// selection made here is known to the component (`getSelectedText`,
/// `replaceSelection`, cut, copy and paste) but not highlighted.
public class DefaultCaret implements Caret {

    public static final int UPDATE_WHEN_ON_EDT = 0;

    public static final int NEVER_UPDATE = 1;

    public static final int ALWAYS_UPDATE = 2;

    protected EventListenerList listenerList = new EventListenerList();

    protected transient ChangeEvent changeEvent;

    private JTextComponent component;
    private int dot;
    private int mark;
    private boolean visible;
    private boolean selectionVisible;
    private int blinkRate;
    private int updatePolicy;
    private Point magic;

    public DefaultCaret() {
    }

    @Override
    public void install(JTextComponent c) {
        component = c;
    }

    @Override
    public void deinstall(JTextComponent c) {
        component = null;
    }

    protected final JTextComponent getComponent() {
        return component;
    }

    @Override
    public void paint(Graphics g) {
    }

    @Override
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        ChangeListener[] ls = getChangeListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            if (changeEvent == null) {
                changeEvent = new ChangeEvent(this);
            }
            ls[i].stateChanged(changeEvent);
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean v) {
        visible = v;
    }

    @Override
    public boolean isSelectionVisible() {
        return selectionVisible;
    }

    @Override
    public void setSelectionVisible(boolean v) {
        selectionVisible = v;
    }

    @Override
    public void setMagicCaretPosition(Point p) {
        magic = p == null ? null : new Point(p);
    }

    @Override
    public Point getMagicCaretPosition() {
        return magic == null ? null : new Point(magic);
    }

    @Override
    public void setBlinkRate(int rate) {
        blinkRate = rate;
    }

    @Override
    public int getBlinkRate() {
        return blinkRate;
    }

    public void setUpdatePolicy(int policy) {
        updatePolicy = policy;
    }

    public int getUpdatePolicy() {
        return updatePolicy;
    }

    @Override
    public int getDot() {
        return dot;
    }

    @Override
    public int getMark() {
        return mark;
    }

    private int clamp(int d) {
        int len = 0;
        if (component != null && component.getDocument() != null) {
            len = component.getDocument().getLength();
        }
        return d < 0 ? 0 : d > len ? len : d;
    }

    @Override
    public void setDot(int dot) {
        int d = clamp(dot);
        if (d != this.dot || d != mark) {
            this.dot = d;
            mark = d;
            fireStateChanged();
        }
    }

    @Override
    public void moveDot(int dot) {
        int d = clamp(dot);
        if (d != this.dot) {
            this.dot = d;
            fireStateChanged();
        }
    }

    @Override
    public String toString() {
        return "Dot=(" + dot + ", Forward) Mark=(" + mark + ", Forward)";
    }
}

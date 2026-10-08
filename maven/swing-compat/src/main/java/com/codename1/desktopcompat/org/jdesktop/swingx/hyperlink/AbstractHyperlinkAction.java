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
package com.codename1.desktopcompat.org.jdesktop.swingx.hyperlink;

import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.org.jdesktop.swingx.action.AbstractActionExt;

/// An action with a target -- the thing a link leads to -- and a visited
/// flag kept under [#VISITED_KEY], which a hyperlink follows to change
/// its color.
///
/// Setting a target names the action after it and clears the flag.
public abstract class AbstractHyperlinkAction<T> extends AbstractActionExt {

    public static final String VISITED_KEY = "visited";

    protected T target;

    public AbstractHyperlinkAction() {
        this(null);
    }

    public AbstractHyperlinkAction(T target) {
        setTarget(target);
    }

    public void setVisited(boolean visited) {
        putValue(VISITED_KEY, Boolean.valueOf(visited));
    }

    public boolean isVisited() {
        return Boolean.TRUE.equals(getValue(VISITED_KEY));
    }

    public T getTarget() {
        return target;
    }

    public void setTarget(T target) {
        T old = this.target;
        uninstallTarget();
        this.target = target;
        installTarget();
        firePropertyChange("target", old, target);
    }

    /// Called after the target changed: names the action after it and
    /// marks it as not visited.
    protected void installTarget() {
        setName(target != null ? target.toString() : "");
        setVisited(false);
    }

    /// Called before the target changes; does nothing.
    protected void uninstallTarget() {
    }

    /// Does nothing: a link is not a toggle.
    @Override
    public void itemStateChanged(ItemEvent e) {
    }

    /// Does nothing: a link is not a toggle.
    @Override
    public void setStateAction(boolean state) {
    }
}

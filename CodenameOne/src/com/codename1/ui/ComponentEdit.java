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
package com.codename1.ui;

import com.codename1.util.UndoableEdit;

/// An edit made through a component, which brings that component back into
/// view when it is undone or redone.
///
/// The history (`com.codename1.util.UndoHistory`) knows nothing about
/// components; this wrapper is what ties a step to one. When the step is
/// replayed and the component is on the form being shown, the component takes
/// the focus, if it can, and is scrolled into view, so the user sees what
/// changed. An edit made on another form is still replayed, without switching
/// forms.
///
/// Components that support undo wrap their own edits this way. An application
/// recording its own edits wraps them the same way:
///
/// ```java
/// UndoHistory.getGlobal().record(new ComponentEdit(field, edit));
/// ```
public class ComponentEdit extends UndoableEdit {
    private final Component component;
    private final UndoableEdit edit;

    /// Wraps an edit made through a component.
    ///
    /// #### Parameters
    ///
    /// - `component`: the component the edit was made in
    ///
    /// - `edit`: the edit itself
    public ComponentEdit(Component component, UndoableEdit edit) {
        if (component == null || edit == null) {
            throw new IllegalArgumentException("component and edit are required");
        }
        this.component = component;
        this.edit = edit;
    }

    /// The component the edit was made in.
    public Component getComponent() {
        return component;
    }

    /// The wrapped edit.
    public UndoableEdit getEdit() {
        return edit;
    }

    @Override
    public void undo() {
        edit.undo();
        reveal();
    }

    @Override
    public void redo() {
        edit.redo();
        reveal();
    }

    @Override
    public String getName() {
        return edit.getName();
    }

    @Override
    public boolean isAlive() {
        return edit.isAlive();
    }

    /// Brings the component into view when it is on the form being shown:
    /// gives it the focus if it can take it and scrolls it into view.
    protected void reveal() {
        Form f = component.getComponentForm();
        if (f == null || f != Display.getInstance().getCurrent()) { //NOPMD CompareObjectsWithEquals - the form shown, not an equal one
            return;
        }
        if (component.isFocusable()) {
            component.requestFocus();
        }
        f.scrollComponentToVisible(component);
    }
}

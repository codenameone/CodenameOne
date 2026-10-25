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
package com.codename1.desktopcompat.javax.swing.undo;

/// A change that can be taken back and applied again.
public interface UndoableEdit {

    /// Takes the change back.
    void undo() throws CannotUndoException;

    boolean canUndo();

    /// Applies the change again after it was taken back.
    void redo() throws CannotRedoException;

    boolean canRedo();

    /// Tells the edit it will never be used again.
    void die();

    /// Absorbs `anEdit` into this one; true when it was absorbed.
    boolean addEdit(UndoableEdit anEdit);

    /// Takes the place of `anEdit`; true when this edit now stands for both.
    boolean replaceEdit(UndoableEdit anEdit);

    /// False for an edit that should be undone together with its
    /// neighbours rather than on its own.
    boolean isSignificant();

    String getPresentationName();

    String getUndoPresentationName();

    String getRedoPresentationName();
}

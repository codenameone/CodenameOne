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
package com.codename1.desktopcompat.java.awt.dnd;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.rt.Dnd;

/// Makes a component a place where a drag can be dropped.
///
/// The drags it hears of are the ones that begin in this application --
/// see `javax.swing.TransferHandler` -- and, where the platform reports
/// them, files and text dragged in from another application. A file drag
/// offers `DataFlavor.javaFileListFlavor`, a text drag
/// `DataFlavor.stringFlavor`. On a platform that reports no drops from
/// outside, the target is there and such a drag never reaches it.
///
/// A target has one listener, given when it is made.
public class DropTarget implements DropTargetListener {

    private final DropTargetContext context = new DropTargetContext(this);
    private Component component;
    private int actions = DnDConstants.ACTION_COPY_OR_MOVE;
    private boolean active = true;
    private DropTargetListener listener;

    public DropTarget() {
        this(null, DnDConstants.ACTION_COPY_OR_MOVE, null, true);
    }

    public DropTarget(Component c, DropTargetListener dtl) {
        this(c, DnDConstants.ACTION_COPY_OR_MOVE, dtl, true);
    }

    public DropTarget(Component c, int ops, DropTargetListener dtl) {
        this(c, ops, dtl, true);
    }

    /// Makes `c` a place to drop, taking the actions in `ops` and telling
    /// `dtl`; `act` is whether the target starts out taking drops.
    public DropTarget(Component c, int ops, DropTargetListener dtl, boolean act) {
        listener = dtl;
        actions = ops & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_REFERENCE);
        active = act;
        if (c != null) {
            component = c;
            c.setDropTarget(this);
        }
        Dnd.want();
    }

    /// Moves the target to another component, or to none.
    public void setComponent(Component c) {
        if (component == c) {
            return;
        }
        Component old = component;
        component = c;
        if (old != null && old.getDropTarget() == this) {
            old.setDropTarget(null);
        }
        if (c != null && c.getDropTarget() != this) {
            c.setDropTarget(this);
        }
    }

    public Component getComponent() {
        return component;
    }

    public void setDefaultActions(int ops) {
        cn1SetTargetActions(ops & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_REFERENCE));
    }

    /// Sets the actions without leaving any out. Not Swing API.
    public void cn1SetTargetActions(int ops) {
        actions = ops;
    }

    public int getDefaultActions() {
        return actions;
    }

    public void setActive(boolean isActive) {
        active = isActive;
    }

    public boolean isActive() {
        return active;
    }

    public void removeDropTargetListener(DropTargetListener dtl) {
        if (dtl != null && dtl == listener) {
            listener = null;
        }
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        if (!active) {
            return;
        }
        if (listener != null) {
            listener.dragEnter(dtde);
        } else {
            dtde.getDropTargetContext().setTargetActions(DnDConstants.ACTION_NONE);
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if (active && listener != null) {
            listener.dragOver(dtde);
        }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        if (active && listener != null) {
            listener.dropActionChanged(dtde);
        }
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        if (active && listener != null) {
            listener.dragExit(dte);
        }
    }

    /// Hands the drop to the listener, and rejects it when the target is
    /// not active or has none.
    @Override
    public void drop(DropTargetDropEvent dtde) {
        if (listener != null && active) {
            listener.drop(dtde);
        } else {
            dtde.rejectDrop();
        }
    }

    public DropTargetContext getDropTargetContext() {
        return context;
    }
}

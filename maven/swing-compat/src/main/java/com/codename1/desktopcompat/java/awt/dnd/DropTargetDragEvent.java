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

import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.datatransfer.DataFlavor;
import com.codename1.desktopcompat.java.awt.datatransfer.Transferable;
import java.util.List;

/// A drag that entered a [DropTarget] or moved over it.
///
/// A listener answers with [#acceptDrag(int)] or [#rejectDrag()]; one that
/// does neither has accepted the action the event carries.
public class DropTargetDragEvent extends DropTargetEvent {

    private static final long serialVersionUID = 1L;

    private final Point location;
    private final int actions;
    private final int dropAction;

    public DropTargetDragEvent(DropTargetContext dtc, Point cursorLocn, int dropAction, int srcActions) {
        super(dtc);
        if (cursorLocn == null) {
            throw new NullPointerException("cursorLocn");
        }
        if (dropAction != DnDConstants.ACTION_NONE && dropAction != DnDConstants.ACTION_COPY
                && dropAction != DnDConstants.ACTION_MOVE && dropAction != DnDConstants.ACTION_LINK) {
            throw new IllegalArgumentException("dropAction" + dropAction);
        }
        if ((srcActions & ~(DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK)) != 0) {
            throw new IllegalArgumentException("srcActions");
        }
        location = cursorLocn;
        actions = srcActions;
        this.dropAction = dropAction;
    }

    /// Where the pointer is, in the coordinates of the target's component.
    public Point getLocation() {
        return location;
    }

    public DataFlavor[] getCurrentDataFlavors() {
        return getDropTargetContext().getCurrentDataFlavors();
    }

    public List<DataFlavor> getCurrentDataFlavorsAsList() {
        return getDropTargetContext().getCurrentDataFlavorsAsList();
    }

    public boolean isDataFlavorSupported(DataFlavor df) {
        return getDropTargetContext().isDataFlavorSupported(df);
    }

    /// The actions the source of the drag allows.
    public int getSourceActions() {
        return actions;
    }

    /// The action the user asked for.
    public int getDropAction() {
        return dropAction;
    }

    /// The data being dragged. What a drag from another application
    /// carries can be named while it hovers, and read once it is dropped.
    public Transferable getTransferable() {
        return getDropTargetContext().getTransferable();
    }

    public void acceptDrag(int dragOperation) {
        getDropTargetContext().acceptDrag(dragOperation);
    }

    public void rejectDrag() {
        getDropTargetContext().rejectDrag();
    }
}

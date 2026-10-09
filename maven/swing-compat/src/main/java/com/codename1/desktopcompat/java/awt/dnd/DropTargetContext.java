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
import com.codename1.desktopcompat.java.awt.datatransfer.DataFlavor;
import com.codename1.desktopcompat.java.awt.datatransfer.Transferable;
import com.codename1.desktopcompat.rt.Dnd;
import java.util.ArrayList;
import java.util.List;

/// What a [DropTarget] and its listener say to the drag that is over it.
///
/// There is one drag at a time, so every context speaks to the same one;
/// an answer given when the context's target is not the one the drag is
/// over is not heard.
public class DropTargetContext {

    private final DropTarget dropTarget;

    DropTargetContext(DropTarget dt) {
        dropTarget = dt;
    }

    public DropTarget getDropTarget() {
        return dropTarget;
    }

    public Component getComponent() {
        return dropTarget.getComponent();
    }

    /// Sets the actions the target takes, as [DropTarget#setDefaultActions].
    protected void setTargetActions(int actions) {
        dropTarget.cn1SetTargetActions(actions);
    }

    protected int getTargetActions() {
        return dropTarget.getDefaultActions();
    }

    /// Reports whether the dropped data was taken.
    ///
    /// #### Throws
    ///
    /// - `InvalidDnDOperationException`: never; the class is not part of the
    ///   layer and a report that comes when no drop is under way is ignored
    public void dropComplete(boolean success) {
        Dnd.complete(dropTarget, success);
    }

    protected void acceptDrag(int dragOperation) {
        Dnd.answer(dropTarget, dragOperation);
    }

    protected void rejectDrag() {
        Dnd.answer(dropTarget, DnDConstants.ACTION_NONE);
    }

    protected void acceptDrop(int dropOperation) {
        Dnd.answer(dropTarget, dropOperation);
    }

    protected void rejectDrop() {
        Dnd.answer(dropTarget, DnDConstants.ACTION_NONE);
    }

    protected DataFlavor[] getCurrentDataFlavors() {
        Transferable t = Dnd.transferable();
        return t == null ? new DataFlavor[0] : t.getTransferDataFlavors();
    }

    protected List<DataFlavor> getCurrentDataFlavorsAsList() {
        DataFlavor[] all = getCurrentDataFlavors();
        List<DataFlavor> out = new ArrayList<DataFlavor>(all.length);
        for (DataFlavor f : all) {
            out.add(f);
        }
        return out;
    }

    protected boolean isDataFlavorSupported(DataFlavor df) {
        Transferable t = Dnd.transferable();
        return t != null && t.isDataFlavorSupported(df);
    }

    /// The data of the drag.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: if no drag is under way. The JDK throws its
    ///   `InvalidDnDOperationException`, a subclass of this one
    protected Transferable getTransferable() {
        Transferable t = Dnd.transferable();
        if (t == null) {
            throw new IllegalStateException("No drag is under way");
        }
        return t;
    }
}

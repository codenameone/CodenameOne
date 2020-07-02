// Copyright (c) 2019 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.callback.CefDragData;
import org.cef.misc.EventFlags;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.List;

class CN1CefDropTargetListener implements DropTargetListener {
    private CN1CefBrowser browser_;
    private CefDragData dragData_ = null;
    private int dragOperations_ = CefDragData.DragOperations.DRAG_OPERATION_COPY;
    private int dragModifiers_ = EventFlags.EVENTFLAG_NONE;
    private int acceptOperations_ = DnDConstants.ACTION_COPY;

    CN1CefDropTargetListener(CN1CefBrowser browser) {
        browser_ = browser;
    }

    @Override
    public void dragEnter(DropTargetDragEvent event) {
        CreateDragData(event);
        browser_.dragTargetDragEnter(
                dragData_, event.getLocation(), dragModifiers_, dragOperations_);
    }

    @Override
    public void dragExit(DropTargetEvent event) {
        AssertDragData();
        browser_.dragTargetDragLeave();
        ClearDragData();
    }

    @Override
    public void dragOver(DropTargetDragEvent event) {
        AssertDragData();
        browser_.dragTargetDragOver(event.getLocation(), dragModifiers_, dragOperations_);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent event) {
        AssertDragData();
        acceptOperations_ = event.getDropAction();
        switch (acceptOperations_) {
            case DnDConstants.ACTION_LINK:
                dragOperations_ = CefDragData.DragOperations.DRAG_OPERATION_LINK;
                dragModifiers_ =
                        EventFlags.EVENTFLAG_CONTROL_DOWN | EventFlags.EVENTFLAG_SHIFT_DOWN;
                break;
            case DnDConstants.ACTION_COPY:
                dragOperations_ = CefDragData.DragOperations.DRAG_OPERATION_COPY;
                dragModifiers_ = EventFlags.EVENTFLAG_CONTROL_DOWN;
                break;
            case DnDConstants.ACTION_MOVE:
                dragOperations_ = CefDragData.DragOperations.DRAG_OPERATION_MOVE;
                dragModifiers_ = EventFlags.EVENTFLAG_SHIFT_DOWN;
                break;
            case DnDConstants.ACTION_NONE:
                // The user did not select an action, so use COPY as the default.
                dragOperations_ = CefDragData.DragOperations.DRAG_OPERATION_COPY;
                dragModifiers_ = EventFlags.EVENTFLAG_NONE;
                acceptOperations_ = DnDConstants.ACTION_COPY;
                break;
        }
    }

    @Override
    public void drop(DropTargetDropEvent event) {
        AssertDragData();
        browser_.dragTargetDrop(event.getLocation(), dragModifiers_);
        event.acceptDrop(acceptOperations_);
        event.dropComplete(true);
        ClearDragData();
    }

    private void CreateDragData(DropTargetDragEvent event) {
        assert dragData_ == null;
        dragData_ = createDragData(event);
        dropActionChanged(event);
    }

    private void AssertDragData() {
        assert dragData_ != null;
    }

    private void ClearDragData() {
        dragData_ = null;
    }

    private static CefDragData createDragData(DropTargetDragEvent event) {
        CefDragData dragData = CefDragData.create();

        Transferable transferable = event.getTransferable();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        for (DataFlavor flavor : flavors) {
            try {
                // TODO(JCEF): Add support for other flavor types.
                if (flavor.isFlavorJavaFileListType()) {
                    List<File> files = (List<File>) transferable.getTransferData(flavor);
                    for (File file : files) {
                        dragData.addFile(file.getPath(), file.getName());
                    }
                }
            } catch (Exception e) {
                // Data is no longer available or of unsupported flavor.
                e.printStackTrace();
            }
        }

        return dragData;
    }
}

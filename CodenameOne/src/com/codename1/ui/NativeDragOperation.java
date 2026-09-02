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

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;

/// Everything the operating system needs in order to drag something out of a Codename One
/// component: what is being dragged, what the receiver is allowed to do with it, and what the
/// user should see under the cursor while dragging.
///
/// The payload is a `ClipboardContent`, the same object a copy publishes, so a component that
/// can already be copied can be made draggable by handing the very same content to
/// `Component#setNativeDragOperation(com.codename1.ui.NativeDragOperation)`. Offering several
/// representations is what lets one drag land correctly in unrelated applications: a text editor
/// takes `ClipboardContent#MIME_HTML`, a plain text field takes `ClipboardContent#MIME_TEXT` and
/// the desktop or a file manager takes `ClipboardContent#MIME_FILE`.
///
/// Representations that are expensive to produce -- the file that only exists if the user
/// actually drops on the desktop -- should be registered with
/// `ClipboardContent#setDataProvider(java.lang.String, com.codename1.ui.ClipboardDataProvider)`
/// rather than built when the drag starts.
///
/// #### Moving rather than copying
///
/// `#ACTION_MOVE` means the receiver takes ownership and the source is expected to delete its
/// copy. The source only learns whether that happened once the operating system has finished
/// the transfer, which is why the outcome arrives asynchronously through
/// `#addCompletionListener(com.codename1.ui.events.ActionListener)` and not from the call that
/// started the drag.
public class NativeDragOperation {
    /// No transfer, which is what a rejected or cancelled drag reports.
    public static final int ACTION_NONE = 0;

    /// The receiver takes a copy and the source keeps its own.
    public static final int ACTION_COPY = 1;

    /// The receiver takes ownership; the source should delete its copy when the drag completes
    /// with this action.
    public static final int ACTION_MOVE = 2;

    /// The receiver stores a reference rather than the data, the way a shortcut or an alias does.
    public static final int ACTION_LINK = 4;

    private final ClipboardContent content;
    private int allowedActions = ACTION_COPY;
    private Image dragImage;
    private int dragImageOffsetX;
    private int dragImageOffsetY;
    private String label;
    private int performedAction = ACTION_NONE;
    /// True when the image below is the one the framework rendered from the source component,
    /// rather than one the application supplied.
    private boolean dragImageGenerated;
    private Component source;
    private EventDispatcher completionListeners;

    /// Creates a drag carrying the given representations.
    ///
    /// #### Parameters
    ///
    /// - `content`: the payload, which must not be null
    public NativeDragOperation(ClipboardContent content) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        this.content = content;
    }

    /// Creates a plain text drag, the shorthand for the common case.
    ///
    /// #### Parameters
    ///
    /// - `text`: the text being dragged
    public NativeDragOperation(String text) {
        this(new ClipboardContent().setData(ClipboardContent.MIME_TEXT, text == null ? "" : text));
    }

    /// Creates a drag carrying files, which is what a drop onto the desktop or a file manager
    /// consumes.
    ///
    /// #### Parameters
    ///
    /// - `paths`: the file paths or `file:` URIs being dragged
    ///
    /// #### Returns
    ///
    /// the new operation
    public static NativeDragOperation createFileDrag(String[] paths) {
        return new NativeDragOperation(new ClipboardContent().setFiles(paths));
    }

    /// Returns the payload.
    public ClipboardContent getContent() {
        return content;
    }

    /// Returns the bit set of actions the source is willing to allow, `#ACTION_COPY` by default.
    public int getAllowedActions() {
        return allowedActions;
    }

    /// Sets the bit set of actions the source is willing to allow. The receiver chooses one of
    /// them, usually influenced by the modifier keys the user is holding.
    ///
    /// #### Parameters
    ///
    /// - `allowedActions`: any combination of `#ACTION_COPY`, `#ACTION_MOVE` and `#ACTION_LINK`
    ///
    /// #### Returns
    ///
    /// this instance, for chaining
    public NativeDragOperation setAllowedActions(int allowedActions) {
        this.allowedActions = allowedActions;
        return this;
    }

    /// Returns the image drawn under the cursor during the drag, or null to let the port draw
    /// the component itself.
    public Image getDragImage() {
        return dragImage;
    }

    /// Sets the image drawn under the cursor during the drag. When this is left null the port
    /// renders the dragged component through `Component#getDragImage()`, so the user sees the
    /// thing they grabbed.
    ///
    /// #### Parameters
    ///
    /// - `dragImage`: the image, or null for the default
    ///
    /// #### Returns
    ///
    /// this instance, for chaining
    public NativeDragOperation setDragImage(Image dragImage) {
        this.dragImage = dragImage;
        this.dragImageGenerated = false;
        return this;
    }

    /// Installs the image the framework rendered from the source component, and the point of it
    /// the press landed on.
    ///
    /// Kept apart from `#setDragImage(com.codename1.ui.Image)` because the operation is reused
    /// for every drag of its component: writing a generated snapshot in as though the
    /// application had supplied it meant every later drag showed the first one's picture, taken
    /// before whatever the component has done since, grabbed at a point the user did not press.
    void setGeneratedDragImage(Image image, int offsetX, int offsetY) {
        this.dragImage = image;
        this.dragImageOffsetX = offsetX;
        this.dragImageOffsetY = offsetY;
        this.dragImageGenerated = true;
    }

    /// True when the current image was rendered by the framework, so a later gesture should
    /// render a fresh one rather than reuse it.
    boolean isDragImageGenerated() {
        return dragImageGenerated;
    }

    /// Returns the x offset of the cursor within the drag image.
    public int getDragImageOffsetX() {
        return dragImageOffsetX;
    }

    /// Returns the y offset of the cursor within the drag image.
    public int getDragImageOffsetY() {
        return dragImageOffsetY;
    }

    /// Places the cursor at a specific point of the drag image, so the image keeps the position
    /// it had relative to the finger or pointer when the drag began.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x offset within the image
    ///
    /// - `y`: the y offset within the image
    ///
    /// #### Returns
    ///
    /// this instance, for chaining
    public NativeDragOperation setDragImageOffset(int x, int y) {
        this.dragImageOffsetX = x;
        this.dragImageOffsetY = y;
        return this;
    }

    /// Returns the human readable label some platforms show beside the drag image.
    public String getLabel() {
        return label;
    }

    /// Sets the human readable label some platforms show beside the drag image, such as the
    /// file name of a dragged document. Platforms that have no such affordance ignore it.
    ///
    /// #### Parameters
    ///
    /// - `label`: the label
    ///
    /// #### Returns
    ///
    /// this instance, for chaining
    public NativeDragOperation setLabel(String label) {
        this.label = label;
        return this;
    }

    /// Returns the component the drag started from, or null when the drag was started through
    /// `NativeDragAndDrop#startDrag(com.codename1.ui.Component, com.codename1.ui.NativeDragOperation)`
    /// without one.
    public Component getSource() {
        return source;
    }

    void setSource(Component source) {
        this.source = source;
    }

    /// Returns the action the receiver actually performed, valid once the drag has completed.
    /// Before that, and for a drag that was cancelled or rejected, this is `#ACTION_NONE`.
    public int getPerformedAction() {
        return performedAction;
    }

    /// Adds a listener notified on the event dispatch thread once the operating system has
    /// finished with this drag, whether it was dropped or abandoned. Read
    /// `#getPerformedAction()` from the listener; a source offering `#ACTION_MOVE` deletes its
    /// copy here and nowhere else, because until this fires nothing is known about whether the
    /// receiver took it.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addCompletionListener(ActionListener l) {
        if (completionListeners == null) {
            completionListeners = new EventDispatcher();
        }
        completionListeners.addListener(l);
    }

    /// Removes a listener added by
    /// `#addCompletionListener(com.codename1.ui.events.ActionListener)`.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeCompletionListener(ActionListener l) {
        if (completionListeners != null) {
            completionListeners.removeListener(l);
        }
    }

    /// Clears the outcome of a previous drag, because this operation is being installed as the
    /// active one again.
    ///
    /// The same instance is offered for every drag of the component that owns it, so without
    /// this `#getPerformedAction()` would go on reporting the *previous* drag's result for the
    /// whole of the new one, which contradicts its contract that the value before completion is
    /// `#ACTION_NONE`.
    void resetPerformedAction() {
        performedAction = ACTION_NONE;
    }

    /// Records the outcome and notifies the completion listeners. Invoked by the port, on the
    /// event dispatch thread, when the native drag session ends.
    ///
    /// #### Parameters
    ///
    /// - `action`: the action the receiver performed, or `#ACTION_NONE`
    void fireCompleted(int action) {
        performedAction = action;
        if (completionListeners != null && completionListeners.hasListeners()) {
            completionListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.NativeDragCompleted));
        }
    }
}

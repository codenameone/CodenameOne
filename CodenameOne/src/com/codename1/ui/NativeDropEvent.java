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

/// One step of a native operating system drag passing over, or landing on, a component that was
/// marked as a native drop target with `Component#setNativeDropTarget(boolean)`.
///
/// The payload is a `ClipboardContent` -- the same shape a paste produces -- so a component that
/// already knows how to paste knows how to accept a drop. Inspect
/// `ClipboardContent#getMimeTypes()` and take the richest representation you understand, exactly
/// as you would for a paste.
///
/// The x and y inherited from `ActionEvent` are absolute screen coordinates within the surface
/// the drag is over, so `Component#getAbsoluteX()` and `Component#getAbsoluteY()` convert them
/// to component coordinates.
///
/// #### Accepting
///
/// A drag event tells the operating system whether the drop would be allowed and what would
/// happen. Call `#accept(int)` with one of the actions in `#getAllowedActions()` to show the
/// user the corresponding cursor, or `#reject()` to refuse. A target that never calls either
/// accepts `NativeDragOperation#ACTION_COPY` when the source allows it, which is what most
/// targets want.
public final class NativeDropEvent extends ActionEvent {
    private final ClipboardContent content;
    private final int allowedActions;
    private final boolean local;
    private final Component target;
    private int acceptedAction;

    /// Creates a drop event.
    ///
    /// #### Parameters
    ///
    /// - `target`: the component the drag is over
    ///
    /// - `type`: the event type
    ///
    /// - `content`: the dragged payload
    ///
    /// - `x`: the absolute x position of the pointer
    ///
    /// - `y`: the absolute y position of the pointer
    ///
    /// - `allowedActions`: the actions the drag source permits
    ///
    /// - `local`: true when the drag started inside this application
    NativeDropEvent(Component target, Type type, ClipboardContent content, int x, int y,
            int allowedActions, boolean local) {
        super(target, type, x, y);
        this.target = target;
        this.content = content;
        this.allowedActions = allowedActions;
        this.local = local;
        this.acceptedAction = defaultAction(allowedActions);
    }

    /// Picks the action a target that expresses no preference gets: a copy when the source
    /// allows one, otherwise whichever single action it does allow.
    private static int defaultAction(int allowedActions) {
        if ((allowedActions & NativeDragOperation.ACTION_COPY) != 0) {
            return NativeDragOperation.ACTION_COPY;
        }
        if ((allowedActions & NativeDragOperation.ACTION_MOVE) != 0) {
            return NativeDragOperation.ACTION_MOVE;
        }
        if ((allowedActions & NativeDragOperation.ACTION_LINK) != 0) {
            return NativeDragOperation.ACTION_LINK;
        }
        return NativeDragOperation.ACTION_NONE;
    }

    /// Returns the dragged payload.
    public ClipboardContent getContent() {
        return content;
    }

    /// Returns the component the drag is over.
    public Component getTarget() {
        return target;
    }

    /// Returns the bit set of actions the drag source permits.
    public int getAllowedActions() {
        return allowedActions;
    }

    /// Returns true when the drag started inside this application rather than in another
    /// application or on the desktop. A target that reorders its own items usually only wants
    /// to handle local drags, and a target that imports foreign data usually only wants the
    /// rest.
    public boolean isLocal() {
        return local;
    }

    /// Returns the file paths carried by the drag, or null when it carries none. This is the
    /// representation a drag out of a file manager or off the desktop arrives with.
    public String[] getFiles() {
        return content == null ? null : content.getFiles();
    }

    /// Returns the plain text carried by the drag, or null when it carries none.
    public String getText() {
        return content == null ? null : content.getText(ClipboardContent.MIME_TEXT);
    }

    /// Returns the action this target has accepted, or `NativeDragOperation#ACTION_NONE` when
    /// it has refused the drop.
    public int getAcceptedAction() {
        return acceptedAction;
    }

    /// Accepts the drag, telling the operating system what dropping here would do. An action
    /// the source does not allow is refused rather than silently substituted, because showing
    /// the user a move cursor for a drag that can only copy is worse than showing no cursor.
    ///
    /// #### Parameters
    ///
    /// - `action`: one of `NativeDragOperation#ACTION_COPY`, `NativeDragOperation#ACTION_MOVE`
    ///   or `NativeDragOperation#ACTION_LINK`
    public void accept(int action) {
        acceptedAction = (action & allowedActions) == action ? action : NativeDragOperation.ACTION_NONE;
    }

    /// Refuses the drag, so the user sees a "no drop" cursor over this component and no drop
    /// event is delivered.
    public void reject() {
        acceptedAction = NativeDragOperation.ACTION_NONE;
    }
}

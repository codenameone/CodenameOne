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

import com.codename1.io.Log;
import com.codename1.ui.events.ActionEvent;

/// Drag and drop through the operating system rather than inside the application.
///
/// Codename One has always had a lightweight drag and drop -- `Component#setDraggable(boolean)`
/// and `Component#setDropTarget(boolean)` -- which moves a rendered image around inside one
/// form. That never leaves the application, so it cannot drop a file on the desktop, cannot
/// carry text into another application's window, and cannot receive anything from one.
///
/// This class is the other half: it hands the drag to the operating system's own drag machinery,
/// using the same `ClipboardContent` a copy publishes as the payload. That is the whole idea --
/// a drag is a copy that the user aims with the pointer, so anything the application can already
/// put on the clipboard it can already drag out, and anything it can paste it can already accept
/// as a drop.
///
/// #### Dragging out
///
/// ```java
/// Label file = new Label("report.pdf");
/// file.setNativeDragOperation(NativeDragOperation.createFileDrag(
///         new String[] { FileSystemStorage.getInstance().getAppHomePath() + "report.pdf" }));
/// ```
///
/// Dropping that on the desktop, on a mail composer or into a file manager copies the file,
/// because the receiving application asked for `ClipboardContent#MIME_FILE` and the drag
/// offered it. Offer several representations and every receiver takes the best one it
/// understands.
///
/// #### Receiving a drop
///
/// ```java
/// Container inbox = new Container();
/// inbox.setNativeDropTarget(true);
/// inbox.addNativeDropListener(e -> {
///     NativeDropEvent drop = (NativeDropEvent)e;
///     String[] files = drop.getFiles();
///     ...
/// });
/// ```
///
/// #### Where it works
///
/// Native drag and drop needs the platform to have it. Check `#isSupported()` before offering
/// the affordance, and `#isDragOutsideApplicationSupported()` before promising the user that a
/// drag can leave the application: a desktop can drop onto any other window, a tablet can drop
/// into another application beside it, and a phone in full screen has nowhere for a drag to go
/// even though drags within the application still work. Where nothing is supported the calls
/// here are harmless no-ops and the lightweight drag and drop is unaffected.
///
/// #### Threading
///
/// The gesture half runs on the event dispatch thread; the receiving half is called from
/// whatever thread the platform hands the port. All of the shared state below is therefore
/// guarded by one lock, and no callback into component or port code is ever made while holding
/// it -- the framework's own event dispatch thread blocks on the platform's UI thread to paint
/// on some ports, so a lock held across a callback is a deadlock waiting for the first drag.
public final class NativeDragAndDrop {
    /// A press further than this from where it started is a drag rather than a click. Measured
    /// in millimetres so it is a finger on a phone and a pointer on a desktop.
    private static final float DRAG_THRESHOLD_MM = 1.5f;

    /// Guards every field below. Held only across field access, never across a call out.
    private static final Object LOCK = new Object();

    /// The operation prepared by the press that is currently down, waiting to see whether the
    /// user drags, and the component it came from.
    private static NativeDragOperation pending;
    private static Component pendingSource;

    /// Where that press landed, which is both the drag threshold's origin and the point the
    /// drag image is grabbed by.
    private static int pressX;
    private static int pressY;

    /// Set once this press has been offered to the port, so a platform that declined to start
    /// the session is not asked again on every drag event of the same gesture.
    private static boolean startOffered;

    /// The action mask the drag last advertised, so the drop event can report what the
    /// *source* permits rather than the one action the target settled on. The port hands
    /// drop() a single action, not a mask, and NativeDropEvent#getAllowedActions() is
    /// documented as the source's -- a listener could not otherwise tell a move-only source
    /// from one that offered both and chose a move.
    private static int advertisedActions;

    /// The session the operating system is currently running, or null.
    private static NativeDragOperation active;

    /// The drop target the drag is currently over, and the action it last agreed to. See the
    /// note on `#dragOver(int, int, int, com.codename1.ui.ClipboardContent, int)` about why the
    /// answer given to the operating system is the previous callback's.
    private static Component currentTarget;
    private static int currentAction = NativeDragOperation.ACTION_NONE;
    private static boolean overDispatchPending;

    /// Bumped whenever the target or the session changes.
    ///
    /// The callbacks below are queued onto the event dispatch thread, so one can still be
    /// waiting when its drag leaves and another arrives over the same component. Identity alone
    /// cannot tell those apart -- the component is the same one -- and the stale callback would
    /// then answer for the new drag, handing it the old one's action or its refusal.
    private static int targetGeneration;

    private NativeDragAndDrop() {
    }

    /// Returns true when this platform can drag and drop through the operating system at all.
    /// Where this is false every method here does nothing and reports failure, so no call site
    /// needs to be conditional -- but an application that shows a "drag me" affordance should
    /// hide it.
    public static boolean isSupported() {
        return Display.impl != null && Display.impl.isNativeDragAndDropSupported();
    }

    /// Returns true when a drag started here can be dropped outside the application: on the
    /// desktop, in a file manager or in another application's window.
    ///
    /// This is narrower than `#isSupported()`. A platform can route drags between components,
    /// and between this application's own windows, while still refusing to let one leave --
    /// which is the normal state of affairs on a phone.
    public static boolean isDragOutsideApplicationSupported() {
        return Display.impl != null && Display.impl.isNativeDragOutsideApplicationSupported();
    }

    /// Starts a native drag immediately, for an application that decides on its own that a drag
    /// has begun -- from a long press, or a menu item -- rather than letting a component do it
    /// through `Component#setNativeDragSource(boolean)`.
    ///
    /// Call this on the event dispatch thread while the pointer is still down; a drag the user
    /// is not currently holding cannot be aimed and platforms reject it.
    ///
    /// #### Parameters
    ///
    /// - `source`: the component the drag comes from, used for the default drag image and
    ///   reported by `NativeDragOperation#getSource()`. May be null.
    ///
    /// - `op`: what is being dragged
    ///
    /// #### Returns
    ///
    /// true when the operating system took the drag; false when the platform has no native drag
    /// and drop, refused to start a session, or is already running one
    public static boolean startDrag(Component source, NativeDragOperation op) {
        if (op == null || !isSupported()
                || op.getAllowedActions() == NativeDragOperation.ACTION_NONE) {
            // Allowing nothing to be done with a drag is having no drag, and a press stages one
            // on the same terms. Running it anyway would put a session in flight that no target
            // could ever accept.
            return false;
        }
        synchronized (LOCK) {
            if (active != null) {
                // One drag at a time, which is all any of these platforms runs. Installing the
                // second operation before the port has answered would strand the first: a
                // refusal clears the session entirely and a success attributes the first
                // session's completion to the second operation, so the original source never
                // learns what happened -- and a source waiting for ACTION_MOVE to delete its
                // data would wait forever.
                return false;
            }
            active = op;
            currentTarget = null;
            targetGeneration++;
            currentAction = NativeDragOperation.ACTION_NONE;
        }
        op.setSource(source);
        op.resetPerformedAction();
        if (needsGeneratedImage(op) && source != null) {
            try {
                // The same snapshot the gesture path renders, because this method documents the
                // source as providing the default preview and without it the ports fall back to
                // something worse: Android snapshots the whole Codename One surface, and JavaSE
                // drags with no image at all.
                //
                // Centred, because a drag begun in code has no grab point to offset from. The
                // gesture path uses where the finger actually went down; there is no such place
                // here, and the centre is what a preview with no anchor should hang from.
                op.setGeneratedDragImage(source.getDragImage(),
                        source.getWidth() / 2, source.getHeight() / 2);
            } catch (Throwable err) {
                Log.e(err);
            }
        }
        boolean started = false;
        try {
            started = Display.impl.startNativeDrag(op);
        } catch (Throwable err) {
            // A port that cannot start a session must not take the application down with it;
            // the gesture simply stays a lightweight one.
            Log.e(err);
        }
        synchronized (LOCK) {
            if (started) {
                // Whatever the press staged is spent: the application has started a drag of
                // its own and the gesture belongs to that. Leaving it staged had the release
                // that follows cancel the port's staging underneath a session already
                // running, and left a later press at the very same pixel looking like this
                // one to isStagedFor -- so it skipped asking the component and dragged the
                // stale payload.
                pending = null;
                pendingSource = null;
                startOffered = false;
            }
        }
        if (!started) {
            synchronized (LOCK) {
                if (active == op) { // NOPMD CompareObjectsWithEquals
                    active = null;
                }
            }
        }
        return started;
    }

    /// Reports that the platform started a drag session on its own, for the operation the press
    /// prepared. Ports whose operating system owns the drag gesture -- where a long press, not
    /// the framework's own threshold, is what begins a drag -- call this instead of returning
    /// true from `com.codename1.impl.CodenameOneImplementation#startNativeDrag(com.codename1.ui.NativeDragOperation)`.
    ///
    /// #### Returns
    ///
    /// the operation the session is carrying, or null when nothing was prepared -- in which case
    /// the port should refuse to start a session
    public static NativeDragOperation dragSessionStarted() {
        NativeDragOperation op;
        final Component source;
        synchronized (LOCK) {
            if (active != null) {
                // A session is already running; see startDrag for why a second one must not
                // displace it. The port refuses to start the drag on a null answer.
                return null;
            }
            op = pending;
            if (op == null || op.getAllowedActions() == NativeDragOperation.ACTION_NONE) {
                // As in startDrag. Refusing *before* the operation is made active also keeps a
                // session that can never complete from wedging every drag after it: a running
                // drag is what stops the next one from starting, and nothing would report this
                // one finished. A press does not stage such an operation in the first place, so
                // this is only reachable if the source changed its mind mid-gesture.
                return null;
            }
            source = pendingSource;
            pending = null;
            pendingSource = null;
            active = op;
            currentTarget = null;
            targetGeneration++;
            currentAction = NativeDragOperation.ACTION_NONE;
        }
        op.resetPerformedAction();
        if (source != null) {
            // On the event dispatch thread, because it repaints. A component that is draggable
            // as well as a native drag source would otherwise be left mid-drag with its image
            // stranded, since the platform stops delivering pointer drags once it takes over.
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    source.cancelLightweightDrag();
                }
            });
        }
        return op;
    }

    /// Returns the drag this application is currently running through the operating system, or
    /// null when it is not dragging. A drop target uses this to tell a drag it started itself
    /// from one that arrived from elsewhere, which `NativeDropEvent#isLocal()` reports.
    public static NativeDragOperation getActiveDrag() {
        synchronized (LOCK) {
            return active;
        }
    }

    // ------------------------------------------------------------------------------------
    // Gesture plumbing. Called by the top level containers as a press is dispatched, so that a
    // native drag source behaves exactly like a lightweight draggable one from the user's side.
    // ------------------------------------------------------------------------------------

    /// Prepares a drag for the press that just landed, so the port has the payload in hand
    /// before the operating system's own gesture recognizer asks for it. Every press either
    /// installs a new pending operation or clears the previous one, which is what keeps a drag
    /// source that was pressed and released from being dragged by a later gesture somewhere
    /// else.
    static void pressedOn(Component cmp, int x, int y) {
        if (isStagedFor(x, y)) {
            return;
        }
        // Everything that can call out -- into the component for its payload and its drag
        // image, and into the port -- happens outside the lock, and what this press staged is
        // then installed in one go. Installing it unconditionally, rather than clearing first
        // and filling in later, is also what keeps the two writes from reading as a botched
        // lazy initialization of a static field.
        NativeDragOperation op = null;
        Component source = cmp;
        if (cmp != null && isSupported()) {
            // A disabled component is not a drag source, and neither is a disabled ancestor.
            // A Form primes drag and drop before it applies its own isEnabled gate -- a Window
            // applies it first -- so without this a disabled control could be dragged out of
            // the application on the main surface and not in a window.
            while (source != null && !(source.isNativeDragSource() && source.isEnabled())) {
                source = source.getParent();
            }
            if (source != null) {
                try {
                    op = source.createNativeDragOperation(x, y);
                } catch (Throwable err) {
                    Log.e(err);
                }
            }
        }
        if (op != null && op.getAllowedActions() == NativeDragOperation.ACTION_NONE) {
            op = null;
        }
        if (op != null) {
            op.setSource(source);
            try {
                if (needsGeneratedImage(op) && Display.impl.isNativeDragImageNeededOnPrepare()) {
                    // The platform asks for the preview from inside its own gesture callback,
                    // which is not a moment at which a component can be rendered. Rendering here
                    // costs a snapshot per press on a drag source, which is what the lightweight
                    // drag has always cost when one starts.
                    op.setGeneratedDragImage(source.getDragImage(),
                            x - source.getAbsoluteX(), y - source.getAbsoluteY());
                }
            } catch (Throwable err) {
                Log.e(err);
            }
        }
        stage(op, source, x, y);
        if (op != null) {
            try {
                Display.impl.prepareNativeDrag(op);
            } catch (Throwable err) {
                Log.e(err);
            }
        }
    }

    /// True when this exact press has already staged an operation.
    ///
    /// A top level primes drag and drop on the component under the pointer and then again on
    /// its nearest draggable ancestor, and the ancestor walk in `#pressedOn(Component, int,
    /// int)` would not find a drag source that sits *between* the two -- so restaging would
    /// throw away what the first call correctly staged. Every release clears the pending
    /// operation, so a later press cannot land on a stale one even at the very same pixel.
    private static boolean isStagedFor(int x, int y) {
        synchronized (LOCK) {
            return pending != null && x == pressX && y == pressY;
        }
    }

    /// Installs what a press staged, or clears it when the press staged nothing. Unconditional
    /// rather than a clear followed by a fill, so that one press leaves one consistent state.
    private static void stage(NativeDragOperation op, Component source, int x, int y) {
        synchronized (LOCK) {
            pending = op;
            pendingSource = op == null ? null : source;
            pressX = x;
            pressY = y;
            startOffered = false;
        }
    }

    /// Hands the gesture to the operating system once the pointer has moved far enough to be a
    /// drag rather than a click.
    ///
    /// #### Parameters
    ///
    /// - `x`: the pointer position
    ///
    /// - `y`: the pointer position
    ///
    /// #### Returns
    ///
    /// true when the native drag has taken the gesture over and the framework should not also
    /// treat it as a scroll or a lightweight drag
    static boolean pointerDragged(int x, int y) {
        int threshold = dragThreshold();
        NativeDragOperation op;
        Component source;
        int grabX;
        int grabY;
        synchronized (LOCK) {
            if (pending == null) {
                // A session already running owns the gesture. Ports differ on whether they keep
                // delivering pointer drags during a native drag; swallowing them here means the
                // ones that do cannot scroll the surface out from under the drag.
                return active != null;
            }
            if (Math.abs(x - pressX) < threshold && Math.abs(y - pressY) < threshold) {
                return false;
            }
            if (startOffered) {
                // Already offered for this gesture and not taken, which is what a platform that
                // starts the session on its own recognizer looks like. Leave the gesture alone
                // until that recognizer fires; it announces itself through dragSessionStarted().
                return active != null;
            }
            startOffered = true;
            op = pending;
            source = pendingSource;
            grabX = pressX;
            grabY = pressY;
        }
        if (needsGeneratedImage(op) && source != null) {
            try {
                // Rendered afresh for this gesture, and recorded as generated rather than
                // written in as though the application had supplied it. An application's own
                // image is never touched -- it may have been positioned deliberately, and
                // overwriting that offset would tear the image away from the pointer.
                op.setGeneratedDragImage(source.getDragImage(),
                        grabX - source.getAbsoluteX(), grabY - source.getAbsoluteY());
            } catch (Throwable err) {
                Log.e(err);
            }
        }
        if (!startDrag(source, op)) {
            // The port did not take it. Keep the prepared operation: on a platform whose own
            // gesture recognizer owns dragging, the session begins later and this is what it
            // will carry. Where there is no native drag and drop at all nothing was prepared
            // in the first place, so there is nothing to keep.
            return false;
        }
        synchronized (LOCK) {
            if (pending == op) { // NOPMD CompareObjectsWithEquals
                pending = null;
                pendingSource = null;
            }
        }
        if (source != null) {
            // A component can be both draggable and a native drag source. The native session
            // owns the gesture from here, and the port stops delivering pointer drags, so the
            // lightweight drag would otherwise be left activated with its image stranded where
            // the drag began.
            source.cancelLightweightDrag();
        }
        return true;
    }

    /// Drops the operation prepared by a press that turned out to be a click. Called as the
    /// pointer is released.
    static void pointerReleased() {
        boolean hadPending;
        synchronized (LOCK) {
            startOffered = false;
            hadPending = pending != null;
            pending = null;
            pendingSource = null;
        }
        if (hadPending) {
            try {
                Display.impl.cancelNativeDrag();
            } catch (Throwable err) {
                Log.e(err);
            }
        }
    }

    /// True when this gesture should render its own preview: either the operation has no image
    /// at all, or the one it has was rendered for an earlier drag of the same reusable
    /// operation and is now out of date.
    private static boolean needsGeneratedImage(NativeDragOperation op) {
        return op.getDragImage() == null || op.isDragImageGenerated();
    }

    private static int dragThreshold() {
        try {
            return Math.max(4, Display.getInstance().convertToPixels(DRAG_THRESHOLD_MM));
        } catch (Throwable err) {
            return 8;
        }
    }

    // ------------------------------------------------------------------------------------
    // The receiving side. Ports call these from whatever thread the operating system hands
    // them, which is not the event dispatch thread.
    // ------------------------------------------------------------------------------------

    /// Reports that a native drag has entered one of the application's surfaces.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id of the window the drag is over, or zero for the main surface
    ///
    /// - `x`: the pointer position within that surface
    ///
    /// - `y`: the pointer position within that surface
    ///
    /// - `content`: the representations the drag is offering
    ///
    /// - `allowedActions`: the actions the source permits
    ///
    /// #### Returns
    ///
    /// the action a drop would perform right now, or `NativeDragOperation#ACTION_NONE` when
    /// nothing under the pointer will take it
    public static int dragEnter(int windowId, int x, int y, ClipboardContent content, int allowedActions) {
        boolean stillHovered;
        synchronized (LOCK) {
            stillHovered = currentTarget != null;
        }
        if (stillHovered) {
            // The platform says this is an entry, so it is one, and nothing can still be hovered
            // from before it. A session that ended without an exit -- a drop the target refused,
            // a drag cancelled while inside the surface -- used to leave the previous target in
            // place, and the next entry was then routed as a move over it: no enter callback
            // ever arrived and the component stayed at the ended session's answer, which for a
            // refusal is ACTION_NONE and is deliberately never recomputed. The ports clear this
            // on their own end-of-session paths as well; this is the one place that cannot be
            // reached by a platform forgetting to tell us.
            dragExit(windowId);
        }
        return dragOver(windowId, x, y, content, allowedActions);
    }

    /// Reports that a native drag has moved over one of the application's surfaces, and answers
    /// whether it would be accepted here.
    ///
    /// #### Threading
    ///
    /// The operating system needs the answer synchronously, while the framework's callbacks
    /// have to run on the event dispatch thread -- and blocking a native drag thread on the
    /// event dispatch thread deadlocks, because on some ports the event dispatch thread is
    /// itself waiting on that native thread to paint. So the target is resolved here, on the
    /// calling thread, from state that does not change under it, while
    /// `Component#nativeDragOver(com.codename1.ui.NativeDropEvent)` and the listeners are
    /// dispatched asynchronously; the value returned is the one *they* produced for the
    /// previous event on this same target. A target that changes its mind therefore shows the
    /// user the new cursor one drag event late, which is a frame, and never blocks.
    ///
    /// A target that refuses a drop outright should say so through
    /// `Component#canAcceptNativeDrop(com.codename1.ui.ClipboardContent)` or the accepted MIME
    /// list instead, both of which are consulted here and are therefore exact from the first
    /// event -- and from every event, including the drop itself. A `NativeDropEvent#reject()`
    /// in a callback is a change of mind rather than a refusal: it is honoured from the next
    /// event onward, and a drop landing before the callback has run reads what the target
    /// declared. `#drop(int, int, int, com.codename1.ui.ClipboardContent, int)` says why that
    /// cannot be closed without doing something worse.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id of the window the drag is over, or zero for the main surface
    ///
    /// - `x`: the pointer position within that surface
    ///
    /// - `y`: the pointer position within that surface
    ///
    /// - `content`: the representations the drag is offering
    ///
    /// - `allowedActions`: the actions the source permits
    ///
    /// #### Returns
    ///
    /// the action a drop would perform right now, or `NativeDragOperation#ACTION_NONE`
    public static int dragOver(int windowId, int x, int y, ClipboardContent content, int allowedActions) {
        Component target = findTarget(windowId, x, y, content, allowedActions);
        Component previous;
        boolean changed;
        boolean dispatchOver = false;
        int answer;
        synchronized (LOCK) {
            advertisedActions = allowedActions;
            previous = currentTarget;
            changed = previous != target; // NOPMD CompareObjectsWithEquals
            if (changed) {
                currentTarget = target;
                targetGeneration++;
                // A pending dispatch belongs to the target that just went away, and its finally
                // will no longer clear this -- so clearing it here is what keeps the new target
                // able to dispatch at all.
                overDispatchPending = false;
                currentAction = target == null ? NativeDragOperation.ACTION_NONE
                        : preferredAction(allowedActions & target.getAcceptedDropActions());
            } else if (target != null) {
                if (currentAction != NativeDragOperation.ACTION_NONE
                        && (currentAction & allowedActions) == 0) {
                    // The permitted set changed while the pointer stayed put, which is what the
                    // desktop modifier does. An action agreed under the old set is no longer on
                    // offer, so keeping it told the platform something it had just withdrawn.
                    //
                    // ACTION_NONE is excluded deliberately: it is a decision, not a stale value.
                    // Recomputing it here turned a target's refusal back into the default and
                    // handed it the drop -- the same defect the enter callback's own rejection
                    // suffered, arriving by a different route.
                    currentAction = preferredAction(allowedActions & target.getAcceptedDropActions());
                }
                if (!overDispatchPending) {
                    overDispatchPending = true;
                    dispatchOver = true;
                }
            }
            answer = target == null ? NativeDragOperation.ACTION_NONE : currentAction;
        }
        if (changed) {
            dispatch(previous, ActionEvent.Type.NativeDragExit, content, x, y, allowedActions);
            dispatch(target, ActionEvent.Type.NativeDragEnter, content, x, y, allowedActions);
        } else if (dispatchOver) {
            dispatch(target, ActionEvent.Type.NativeDragOver, content, x, y, allowedActions);
        }
        return answer;
    }

    /// Reports that a native drag has left the application's surfaces without dropping.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id of the window the drag left, or zero for the main surface
    public static void dragExit(int windowId) {
        Component previous;
        synchronized (LOCK) {
            previous = currentTarget;
            currentTarget = null;
            targetGeneration++;
            currentAction = NativeDragOperation.ACTION_NONE;
            advertisedActions = NativeDragOperation.ACTION_NONE;
        }
        dispatch(previous, ActionEvent.Type.NativeDragExit, null, 0, 0, NativeDragOperation.ACTION_NONE);
    }

    /// Delivers a native drop.
    ///
    /// The content must be fully materialized before this is called: on most platforms the
    /// native transfer object is only readable inside the drop callback, so a port that hands
    /// over a lazy view of it delivers empty data by the time the event dispatch thread reads
    /// it.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id of the window dropped on, or zero for the main surface
    ///
    /// - `x`: the pointer position within that surface
    ///
    /// - `y`: the pointer position within that surface
    ///
    /// - `content`: the dropped representations
    ///
    /// - `action`: the action the operating system settled on
    ///
    /// #### Returns
    ///
    /// the action actually accepted, or `NativeDragOperation#ACTION_NONE` when nothing under
    /// the pointer took the drop and the port should report the transfer as failed
    public static int drop(int windowId, int x, int y, ClipboardContent content, int action) {
        Component target = findTarget(windowId, x, y, content, action);
        int accepted;
        Component previous;
        int advertised;
        synchronized (LOCK) {
            // What the drag has been advertising all along. A drop arriving with no drag
            // event before it -- which no real port does -- has only the port's one action
            // to report.
            advertised = advertisedActions == NativeDragOperation.ACTION_NONE
                    ? action : advertisedActions;
            previous = currentTarget;
            if (target != null && target == currentTarget) { // NOPMD CompareObjectsWithEquals
                // The target's own latest word, not a recomputation from the action the port
                // supplied. That action is by construction one event behind -- it is what the
                // last drag event answered -- so a target that rejected, or changed its mind,
                // in a callback that has since run would have had that decision quietly
                // discarded here, and a refusal turned back into a delivered drop on every
                // port rather than only the one that was noticed.
                //
                // "Latest word" is as far as this can go, and deliberately so. A drop that
                // arrives before the queued nativeDragEnter has run reads what the target
                // *declared* rather than what that callback was about to say, and no
                // rearrangement of this method fixes that:
                //
                //  - Waiting for the callback deadlocks. This runs on the native drag thread,
                //    which the event dispatch thread blocks on to paint.
                //  - Refusing whenever a callback is outstanding refuses every ordinary drop
                //    that lands while the event dispatch thread is a frame behind.
                //  - Withholding delivery afterwards is worse than delivering. The platform has
                //    already been told the action; on ACTION_MOVE the source deletes its copy on
                //    that word, so a drop withheld after the fact destroys the data instead of
                //    misplacing it.
                //
                // What is exact is the declarative refusal: canAcceptNativeDrop and
                // getAcceptedDropActions are consulted by findTarget on this thread, here as
                // well as on every drag event, so a target refusing through either is never
                // selected and never receives the drop, whatever the event dispatch thread is
                // doing. That is what a target refusing outright must use -- reject() in a
                // callback is a late change of mind, honoured from the next event onward.
                accepted = currentAction;
            } else {
                // A different component from the one the callbacks were about: the pointer
                // moved between the last drag event and the drop, so there is no decision of
                // its own to honour and the declarative answer is the right one.
                accepted = target == null ? NativeDragOperation.ACTION_NONE
                        : preferredAction(action & target.getAcceptedDropActions());
            }
            currentTarget = null;
            targetGeneration++;
            overDispatchPending = false;
            currentAction = accepted;
        }
        if (previous != null && previous != target) { // NOPMD CompareObjectsWithEquals
            // A release that lands somewhere else -- a quick move and let go -- ends the drag
            // for the component it was over, and that component has to be told. Clearing the
            // target without it left the old hover highlight on for good: the drop goes to
            // somebody else, and the port's own end-of-session cleanup then finds the target
            // already cleared and has nothing left to deliver the exit to.
            dispatch(previous, ActionEvent.Type.NativeDragExit, content, x, y, action);
        }
        if (accepted == NativeDragOperation.ACTION_NONE) {
            return NativeDragOperation.ACTION_NONE;
        }
        // Queued after the exit above, so a component losing the drag hears about it before
        // the one taking it hears about the drop. The event carries the source's whole mask
        // and the action actually being performed -- different questions that used to get
        // the same answer, so the drop reported the chosen action as though it were all the
        // source had ever allowed.
        dispatch(target, ActionEvent.Type.NativeDrop, content, x, y, advertised, accepted);
        return accepted;
    }

    /// The action a drop at this position would perform, without dispatching anything or
    /// disturbing the drag in progress.
    ///
    /// A port whose platform commits to an action *before* it can read the transferred data --
    /// AWT does, because a drop has to be accepted before it becomes readable -- asks here
    /// first, so that what it commits to is what `#drop(int, int, int,
    /// com.codename1.ui.ClipboardContent, int)` will go on to report. Committing the platform's
    /// own stale action instead told the source a copy had happened while the target was handed
    /// a move.
    ///
    /// #### Parameters
    ///
    /// - `windowId`: the id of the window the drag is over, or zero for the main surface
    ///
    /// - `x`: the pointer position within that surface
    ///
    /// - `y`: the pointer position within that surface
    ///
    /// - `content`: the representations the drag is offering, which may still be a description
    ///   rather than the materialized payload
    ///
    /// - `action`: the action the platform is proposing
    ///
    /// #### Returns
    ///
    /// the action the drop would perform, or `NativeDragOperation#ACTION_NONE`
    public static int plannedDropAction(int windowId, int x, int y, ClipboardContent content,
            int action) {
        Component target = findTarget(windowId, x, y, content, action);
        synchronized (LOCK) {
            if (target != null && target == currentTarget) { // NOPMD CompareObjectsWithEquals
                return currentAction;
            }
            return target == null ? NativeDragOperation.ACTION_NONE
                    : preferredAction(action & target.getAcceptedDropActions());
        }
    }

    /// Reports that the session started by `#startDrag(com.codename1.ui.Component,
    /// com.codename1.ui.NativeDragOperation)` has finished, whatever the outcome, so that a
    /// source offering `NativeDragOperation#ACTION_MOVE` learns whether to delete its copy.
    ///
    /// #### Parameters
    ///
    /// - `performedAction`: the action the receiver performed, or
    ///   `NativeDragOperation#ACTION_NONE` when the drag was cancelled or refused
    public static void dragCompleted(final int performedAction) {
        final NativeDragOperation op;
        synchronized (LOCK) {
            op = active;
            active = null;
            currentTarget = null;
            targetGeneration++;
            currentAction = NativeDragOperation.ACTION_NONE;
            overDispatchPending = false;
            advertisedActions = NativeDragOperation.ACTION_NONE;
        }
        if (op == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                op.fireCompleted(performedAction);
            }
        });
    }

    // ------------------------------------------------------------------------------------

    /// Resolves the deepest component under the pointer that is willing to take this content.
    ///
    /// Runs on the native drag thread and reads the component tree without mutating it, which
    /// is the same thing the ports already do to route a native pointer press.
    /// #### Parameters
    ///
    /// - `actions`: the actions in play, so a target that can perform none of them is passed
    ///   over rather than selected and then found to have nothing to offer. A move-only target
    ///   nested in a copy-capable one used to swallow a copy-only drag: it was chosen on the
    ///   content alone, answered with nothing, and the ancestor that would have taken the drop
    ///   was never reached. Refusing on the action is the same kind of refusal as refusing on
    ///   the MIME type, and the walk treats it the same way.
    private static Component findTarget(int windowId, int x, int y, ClipboardContent content,
            int actions) {
        Container root = surfaceFor(windowId);
        if (root == null) {
            return null;
        }
        Component cmp;
        try {
            cmp = root.getComponentAt(x, y);
        } catch (Throwable err) {
            // The tree can be mutated on the event dispatch thread while this walks it. A drag
            // event that lands mid-layout is not worth a crash; the next one resolves.
            return null;
        }
        while (cmp != null) {
            if (cmp.isNativeDropTarget() && !cmp.isIgnorePointerEvents() && cmp.isEnabled()
                    && (actions & cmp.getAcceptedDropActions()) != 0) {
                try {
                    if (cmp.canAcceptNativeDrop(content)) {
                        return cmp;
                    }
                } catch (Throwable err) {
                    Log.e(err);
                }
            }
            cmp = cmp.getParent();
        }
        return null;
    }

    /// The container a window id names: the current form for the main surface, the window
    /// itself otherwise.
    private static Container surfaceFor(int windowId) {
        if (windowId == 0) {
            return Display.getInstance().getCurrent();
        }
        try {
            TopLevelContainer top = Desktop.getInstance().windowById(windowId);
            return top == null ? null : top.asContainer();
        } catch (Throwable err) {
            return null;
        }
    }

    /// Picks one action out of a bit set, preferring a copy because it is the one that cannot
    /// destroy the source's data.
    private static int preferredAction(int actions) {
        if ((actions & NativeDragOperation.ACTION_COPY) != 0) {
            return NativeDragOperation.ACTION_COPY;
        }
        if ((actions & NativeDragOperation.ACTION_MOVE) != 0) {
            return NativeDragOperation.ACTION_MOVE;
        }
        if ((actions & NativeDragOperation.ACTION_LINK) != 0) {
            return NativeDragOperation.ACTION_LINK;
        }
        return NativeDragOperation.ACTION_NONE;
    }

    /// Queues one callback onto the event dispatch thread, where component code is allowed to
    /// run, and folds whatever the target decided back into the answer the next drag event will
    /// give the operating system.
    private static void dispatch(final Component target, final ActionEvent.Type type,
            final ClipboardContent content, final int x, final int y, final int allowedActions) {
        dispatch(target, type, content, x, y, allowedActions, NativeDragOperation.ACTION_NONE);
    }

    private static void dispatch(final Component target, final ActionEvent.Type type,
            final ClipboardContent content, final int x, final int y, final int allowedActions,
            final int performedAction) {
        if (target == null) {
            if (type == ActionEvent.Type.NativeDragOver) {
                synchronized (LOCK) {
                    overDispatchPending = false;
                }
            }
            return;
        }
        final boolean local;
        final int generation;
        synchronized (LOCK) {
            local = active != null;
            generation = targetGeneration;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                try {
                    NativeDropEvent ev = new NativeDropEvent(target, type, content, x, y, allowedActions, local);
                    if (type == ActionEvent.Type.NativeDrop
                            && performedAction != NativeDragOperation.ACTION_NONE) {
                        // The action the drop is performing, which is not what the event
                        // would default to: a source allowing both defaults to a copy, so a
                        // target handed a move read getAcceptedAction() as a copy.
                        ev.accept(performedAction);
                    }
                    if (type == ActionEvent.Type.NativeDragOver || type == ActionEvent.Type.NativeDragEnter) {
                        // Read as this runs, not when it was queued. A drag event can arrive
                        // before the enter callback ahead of it in the queue has run, and
                        // capturing the answer at queueing time meant this event then restored
                        // the default over a decision that callback had since made -- so a
                        // target rejecting only in nativeDragEnter had its rejection undone by
                        // the very next no-op nativeDragOver, and was handed the drop.
                        int startingAction;
                        synchronized (LOCK) {
                            startingAction = currentAction;
                        }
                        // The target starts from what the framework already agreed to, so a
                        // target that does not care keeps the answer stable instead of
                        // resetting it to the default on every event.
                        ev.accept(startingAction);
                    }
                    target.dispatchNativeDropEvent(ev);
                    if (type == ActionEvent.Type.NativeDragOver || type == ActionEvent.Type.NativeDragEnter) {
                        synchronized (LOCK) {
                            // The generation as well as the component: the same component can be
                            // the target of the drag that just left and of the one that just
                            // arrived, and this decision belongs to whichever queued it.
                            if (generation == targetGeneration
                                    && currentTarget == target) { // NOPMD CompareObjectsWithEquals
                                currentAction = ev.getAcceptedAction();
                            }
                        }
                    }
                } catch (Throwable err) {
                    Log.e(err);
                } finally {
                    if (type == ActionEvent.Type.NativeDragOver) {
                        synchronized (LOCK) {
                            if (generation == targetGeneration) {
                                overDispatchPending = false;
                            }
                        }
                    }
                }
            }
        });
    }
}

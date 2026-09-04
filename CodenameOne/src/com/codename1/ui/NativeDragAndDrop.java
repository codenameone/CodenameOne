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

    /// The press the staged operation belongs to.
    ///
    /// A press is not its coordinates. Identifying it that way meant a gesture that ended
    /// without a release -- Android cancels a touch outright, and nothing delivers a release
    /// for it -- left an operation staged that a later press at the very same pixel then
    /// inherited, along with a source component that may not even be under the pointer any
    /// more. Every press mints one of these already, for the same reason.
    private static Object pressToken;

    /// The session the operating system is currently running, or null.
    private static NativeDragOperation active;

    /// The drop target the drag is currently over, and the action it last agreed to. See the
    /// note on `#dragOver(int, int, int, com.codename1.ui.ClipboardContent, int)` about why the
    /// answer given to the operating system is the previous callback's.
    private static Component currentTarget;
    private static int currentAction = NativeDragOperation.ACTION_NONE;
    private static boolean overDispatchPending;

    /// Where the last drag event was, on the surface it was on. What tells a release that did
    /// not move from one that landed somewhere else; see `#releasedWhereItHovered(int, int)`.
    private static int hoverX;
    private static int hoverY;

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
        return startDrag(source, op, true);
    }

    /// `renderPreview` is false for the gesture, which has already rendered one at the point the
    /// press actually landed. Rendering again here would take a second snapshot of the same
    /// component and replace that grab point with the component's centre, so the preview jumped
    /// out from under the pointer the moment the drag began. A generated image is re-rendered
    /// per gesture *by* the gesture; this entry point renders one only for a caller who has no
    /// gesture to have done it.
    private static boolean startDrag(Component source, NativeDragOperation op,
            boolean renderPreview) {
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
        // Whatever this operation still owes from its last session, paid before it is armed for
        // this one -- and before the source is replaced, so a listener asking which component
        // the drag it is being told about belonged to is answered with that one rather than
        // the component starting a drag of its own now. Already on the event dispatch thread
        // here, which is where a completion belongs.
        deliverCompletion(op);
        op.setSource(source);
        op.resetPerformedAction();
        // Before the port is asked to start, which is when it reads the payload.
        op.resetProvidedValues();
        if (renderPreview && needsGeneratedImage(op) && source != null) {
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
                pressToken = null;
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

    /// Delivers an owed completion, once, to whichever reaches it first: the callback
    /// `#dragCompleted(int)` queued, or the next start of the same operation.
    ///
    /// What is owed lives on the operation, so operations waiting at the same time do not
    /// displace one another -- see `NativeDragOperation#oweCompletion(int)`.
    ///
    /// #### Parameters
    ///
    /// - `op`: the operation whose completion is owed
    private static void deliverCompletion(NativeDragOperation op) {
        int action;
        synchronized (LOCK) {
            if (!op.owesCompletion()) {
                // Already delivered; the other of the two paths got here first.
                return;
            }
            action = op.takeOwedAction();
        }
        try {
            op.fireCompleted(action);
        } catch (Throwable err) {
            // A listener is application code and may throw anything. Letting it out of
            // here was the worst possible moment for it: this is called from startDrag
            // once the operation has been made active, so the exception left a drag that
            // had never begun looking like one still running, and every drag after it was
            // refused for the life of the process. The outcome has been taken by now, so
            // it is reported once either way.
            Log.e(err);
        }
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
        final NativeDragOperation op;
        final Component source;
        Component staged;
        synchronized (LOCK) {
            staged = pendingSource;
        }
        if (!stillWillingSource(staged)) {
            // The component disowned the drag after its press staged one. Nothing starts, and
            // what was staged goes: the platform is about to be told there is no session, and
            // leaving the operation behind would offer it to the next gesture instead.
            gestureCancelled();
            return null;
        }
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
        // The component the staging names, said again here. An operation instance shared by
        // several components can have been pointed at another one by a press that was then
        // refused the staging slot, and this is the last moment before the session owns it --
        // on a move, the operation's source is whose data is deleted when the drop lands.
        op.setSource(source);
        // Now, not queued: this runs on the platform's own thread and the port reads the
        // payload the moment this returns -- through nativeDragSessionStartedCallback on
        // iOS -- so anything the previous drag's providers produced has to be forgotten
        // before that read, not after it. Forgetting it afterwards would be worse than not
        // forgetting at all: a receiver reading a representation later in this same session
        // would run the provider a second time and get a second file for one drag.
        op.resetProvidedValues();
        // Queued rather than run here: this is the platform's own thread, and a completion
        // belongs to the event dispatch thread. Behind the callback dragCompleted queued for the
        // previous session, which is what puts that session's outcome before the reset arming
        // this one -- otherwise the reset happened first and the late completion wrote the old
        // drag's action onto the operation the new drag is using.
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                op.resetPerformedAction();
            }
        });
        if (source != null) {
            // On the event dispatch thread, because it repaints. A component that is draggable
            // as well as a native drag source would otherwise be left mid-drag with its image
            // stranded, since the platform stops delivering pointer drags once it takes over.
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    cancelLightweightDrag(source);
                }
            });
        }
        return op;
    }

    /// Produces one representation of a drag for the session that is reading it, rather than
    /// for whichever transfer armed the operation last.
    ///
    /// For a port whose platform keeps an older session readable while a newer one runs --
    /// iOS does, for as long as a receiver holds one of its item providers -- and which
    /// therefore keeps a memo of its own, one per session. Reading through the operation
    /// instead would hand that receiver the newer drag's value, or produce a second one for
    /// a drag that had already ended. Every other port reads through
    /// `ClipboardContent#getData(java.lang.String)`, whose memory is the running transfer's
    /// and is exactly right when only one session can be read at a time.
    ///
    /// #### Parameters
    ///
    /// - `op`: the operation the session is carrying, which may be null
    ///
    /// - `mimeType`: the representation being read
    ///
    /// #### Returns
    ///
    /// the value, or null when there is no such operation or representation
    public static Object produceDragValue(NativeDragOperation op, String mimeType) {
        return op == null ? null : op.produceData(mimeType);
    }

    /// The same, for a transfer that is not a drag: a clipboard a port keeps lazily, whose
    /// content the application may also be dragging.
    ///
    /// A copy and a drag can share one `ClipboardContent`, and arming a drag forgets what
    /// its providers produced. Reading the content's own memory then gave the clipboard
    /// whatever the drag had most recently produced -- for a provider that writes a file
    /// per transfer, a path belonging to that drag, which its cleanup may since have
    /// deleted. A port in that position produces its own value and remembers it itself.
    ///
    /// #### Parameters
    ///
    /// - `content`: the representations being transferred, which may be null
    ///
    /// - `mimeType`: the representation being read
    ///
    /// #### Returns
    ///
    /// the value, or null when there is no such representation
    /// Ends a content's memory of what its providers produced, because a new transfer of it is
    /// beginning.
    ///
    /// A representation registered through
    /// `ClipboardContent#setDataProvider(java.lang.String, com.codename1.ui.ClipboardDataProvider)`
    /// is resolved once per transfer and remembered, so a consumer that asks twice does not
    /// make the provider write its file twice. `Display#copyToClipboard(ClipboardContent)`
    /// calls this as the copy is asked for; a port whose publication really happens later --
    /// Android assembles the clip on its own UI thread -- calls it again there, because two
    /// copies asked for in quick succession would otherwise both reset first and the second
    /// would then publish what the first one's assembly had just cached.
    ///
    /// #### Parameters
    ///
    /// - `content`: the content about to be published, which may be null
    public static void beginTransfer(ClipboardContent content) {
        if (content != null) {
            content.resetProvidedValues();
        }
    }

    public static Object produceTransferValue(ClipboardContent content, String mimeType) {
        return content == null ? null : content.produceData(mimeType);
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
        Object token = pressTokenOf(cmp);
        if (isStagedFor(token, x, y)) {
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
            // As in startDrag: an operation its source reuses may still owe the outcome of the
            // drag before, and its listeners have to hear it while getSource() is still the
            // component that drag belonged to. Delivering what is owed is right whether or
            // not the claim below succeeds -- it is the previous session's outcome either
            // way, and it was taken from the operation in one step.
            deliverCompletion(op);
            if (!claimSource(op, source)) {
                // A drag is running on this very instance. Nothing is staged: it cannot be
                // started twice, and this press must not point it at a different component.
                op = null;
            }
        }
        if (op != null) {
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
        if (!stage(op, source, token, x, y)) {
            // Another gesture owns the staging slot and has already been handed to the
            // platform. Nothing to prepare: this press stages nothing at all.
            return;
        }
        if (op != null) {
            try {
                Display.impl.prepareNativeDrag(op);
            } catch (Throwable err) {
                Log.e(err);
            }
        }
    }

    /// Points an operation at the component whose press staged it, unless a drag is already
    /// running on that very instance.
    ///
    /// Both in one step, under the lock. A source may hand the same operation to every
    /// component it owns, and a second finger can press one of them while the first is
    /// dragging -- on iOS the session even begins on the platform's own thread, so the
    /// answer can change between asking and acting. Testing first and assigning afterwards
    /// left exactly that gap, and through it the running drag was pointed at the component
    /// that had merely been touched: on a move, the source deletes the wrong one's data.
    ///
    /// #### Parameters
    ///
    /// - `op`: the operation the press produced
    ///
    /// - `source`: the component it was produced for
    ///
    /// #### Returns
    ///
    /// true when the operation is now this component's to stage
    private static boolean claimSource(NativeDragOperation op, Component source) {
        synchronized (LOCK) {
            if (op == active) { // NOPMD CompareObjectsWithEquals
                return false;
            }
            op.setSource(source);
            return true;
        }
    }

    /// True when this exact press has already staged an operation.
    ///
    /// A top level primes drag and drop on the component under the pointer and then again on
    /// its nearest draggable ancestor, and the ancestor walk in `#pressedOn(Component, int,
    /// int)` would not find a drag source that sits *between* the two -- so restaging would
    /// throw away what the first call correctly staged. Every release clears the pending
    /// operation, so a later press cannot land on a stale one even at the very same pixel.
    private static boolean isStagedFor(Object token, int x, int y) {
        synchronized (LOCK) {
            if (pending == null) {
                return false;
            }
            if (token != null || pressToken != null) {
                return token == pressToken; // NOPMD CompareObjectsWithEquals
            }
            // No top level to mint a token: the position is all there is to go on.
            return x == pressX && y == pressY;
        }
    }

    /// The object the top level minted for the press in progress, or null when there is no
    /// top level to ask.
    private static Object pressTokenOf(Component cmp) {
        if (cmp == null) {
            return null;
        }
        Container root = TopLevelSupport.rootOf(cmp);
        return root == null ? null : root.getCurrentPointerPress();
    }

    /// Whether the component a gesture was staged for is still a drag source.
    ///
    /// A press stages before the component's own press handler runs, and that handler may
    /// clear the operation or the drag source flag -- both documented as stopping the
    /// component from being dragged. The setters only change the component's own fields, so
    /// without this the stale operation was started by the next movement, and on a platform
    /// whose recognizer owns the gesture it was handed to the session that began afterwards.
    ///
    /// Asked of both start paths rather than made the setters' business: a component can stop
    /// being draggable in more ways than there are setters -- being disabled is one -- and the
    /// question that matters is whether it is one *now*, at the moment something would begin.
    ///
    /// #### Parameters
    ///
    /// - `source`: the component the operation was staged for, or null for a drag begun in
    ///   code, which has no component to ask
    private static boolean stillWillingSource(Component source) {
        if (source == null) {
            return true;
        }
        return source.isNativeDragSource() && source.isEnabled();
    }

    /// Installs what a press staged, or clears it when the press staged nothing. In one go
    /// rather than a clear followed by a fill, so that one press leaves one consistent state.
    ///
    /// Refused for a *different* press once the gesture already staged has been offered to the
    /// platform. On a platform whose own recognizer owns dragging, that offer is the whole
    /// handover: the session begins later and takes whatever is staged, with no press of its
    /// own to identify it by. A second finger pressing another drag source in that window
    /// replaced both the operation and its source, so the drag the *first* finger had begun
    /// exported the second component's payload -- and reported a move against it, which is the
    /// word a source deletes its data on. The press that is not this gesture's leaves it alone;
    /// the release that ends either gesture clears it, so nothing is stranded.
    ///
    /// A press that lands with nothing offered still replaces what is staged, which is what a
    /// gesture the platform dropped on the floor needs -- see
    /// `#isStagedFor(java.lang.Object, int, int)` for the other half of that.
    ///
    /// #### Returns
    ///
    /// true when this press now owns the staging slot
    private static boolean stage(NativeDragOperation op, Component source, Object token,
            int x, int y) {
        synchronized (LOCK) {
            if (startOffered && pending != null) {
                // What the press claimed, given back. A source may hand one operation instance
                // to every component it owns, so the press being refused here may have pointed
                // that very instance at its own component a moment ago -- claiming the source
                // and claiming the slot cannot be one step, because rendering the preview
                // happens between them and must not hold the lock. Left retargeted, the drag
                // the first finger began completed against the second finger's component, and
                // on a move that is whose data gets deleted.
                if (pending == op) { // NOPMD CompareObjectsWithEquals
                    pending.setSource(pendingSource);
                }
                return false;
            }
            pending = op;
            pendingSource = op == null ? null : source;
            pressToken = op == null ? null : token;
            pressX = x;
            pressY = y;
            startOffered = false;
            return true;
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
        if (!stillWillingSource(source)) {
            // As in dragSessionStarted: the press staged this before the component's own press
            // handler ran, and that handler is allowed to change its mind -- clearing the
            // operation, or the drag source flag, is documented as stopping the component from
            // being dragged. Starting the gesture anyway dragged a component that had just
            // said it is not a drag source.
            gestureCancelled();
            return false;
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
        if (!startDrag(source, op, false)) {
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
                pressToken = null;
            }
        }
        cancelLightweightDrag(source);
        return true;
    }

    /// Abandons the lightweight drag this gesture started, whichever component it belongs to.
    ///
    /// A component can be both draggable and a native drag source, and from here the operating
    /// system owns the gesture: the port stops delivering pointer drags, no lightweight drop
    /// ever runs, and a drag left activated keeps its component hidden with its image stranded
    /// where the gesture began.
    ///
    /// Not necessarily the source's own drag. A drag source is found by walking *up* from the
    /// press, so a draggable child inside a native-drag-source ancestor stages the ancestor --
    /// and a motion too small to reach the native threshold starts the child's lightweight drag
    /// first, which hides the child and records it on the top level. Cancelling the ancestor,
    /// which never had a drag of its own, left that child invisible for good.
    ///
    /// #### Parameters
    ///
    /// - `source`: the component the native drag is running for, or null when there is none
    private static void cancelLightweightDrag(Component source) {
        if (source == null) {
            return;
        }
        Container root = TopLevelSupport.rootOf(source);
        Component dragged = root == null ? null : root.getDraggedComponent();
        if (dragged != null && dragged != source) { // NOPMD CompareObjectsWithEquals
            dragged.cancelLightweightDrag();
        }
        // The source as well, and whether or not it was the dragged one: a component can have
        // activated a drag that never became the top level's -- grabbing a scroll does exactly
        // that -- and those flags have to go, or the next gesture reads them as a drag already
        // under way. Cancelling twice is harmless; the second call finds nothing activated.
        source.cancelLightweightDrag();
    }

    /// Drops the operation prepared by a press that turned out to be a click. Called as the
    /// pointer is released.
    static void pointerReleased(Object token) {
        if (!ownsStaging(token)) {
            // A press that never owned the staging slot cannot end what does. The second
            // finger of a two finger gesture is exactly that: it is refused the slot by
            // stage() while the first finger's gesture is with the platform, and its release
            // then arrived here and cancelled the drag the first finger had already been
            // offered for -- so UIKit's session, when it finally began, found nothing staged
            // and started no drag at all.
            return;
        }
        gestureCancelled();
    }

    /// True when a release identified by this press token may end what is staged.
    ///
    /// Only the offered case is protected, which is the one where the framework has handed a
    /// gesture to the platform and is waiting to be told the session began. Everything else
    /// clears as it always did: a staged gesture the platform has not been offered belongs to
    /// whichever press is ending, and a release that arrives with no press in progress at all
    /// -- the token is null, which is what a second release after the first has already
    /// finished looks like -- is the backstop that keeps an offered slot from outliving every
    /// gesture on the surface.
    ///
    /// #### Parameters
    ///
    /// - `token`: the press the release belongs to, or null when there is no top level to ask
    ///   or no press in progress
    private static boolean ownsStaging(Object token) {
        synchronized (LOCK) {
            if (!startOffered || pending == null || token == null) {
                return true;
            }
            return token == pressToken; // NOPMD CompareObjectsWithEquals
        }
    }

    /// Abandons a staged gesture whose surface is going away.
    ///
    /// A press stages an operation on the surface it landed on, and that surface can be
    /// replaced before the gesture ends -- a press handler that shows another form does
    /// exactly that. What it staged has nowhere left to go: the component is off screen, the
    /// release will be dispatched to whatever replaced it, and a platform that starts the
    /// session from its own recognizer would begin a drag carrying a hidden form's payload.
    ///
    /// Only what belongs to this surface, so one form going away does not cancel a gesture
    /// staged in a window that is still on screen.
    ///
    /// #### Parameters
    ///
    /// - `root`: the top level being deinitialized
    static void topLevelDeinitialized(Container root) {
        Component source;
        synchronized (LOCK) {
            source = pendingSource;
        }
        if (source == null || root == null) {
            return;
        }
        Container owner = TopLevelSupport.rootOf(source);
        if (owner == null || owner == root) { // NOPMD CompareObjectsWithEquals
            // A source with no top level at all is stranded by construction: nothing will
            // deliver its release either.
            gestureCancelled();
        }
    }

    /// Abandons whatever a press staged, because the gesture it belonged to is over or has
    /// turned into something else.
    ///
    /// A release is the ordinary way that happens and the framework calls this itself. A port
    /// calls it for the ways that are not a release: a touch the platform cancels outright,
    /// which delivers no release at all, and anything else that ends a gesture without one.
    /// Leaving an operation staged past its gesture is what lets a later, unrelated movement
    /// start a drag nobody asked for.
    public static void gestureCancelled() {
        boolean hadPending;
        synchronized (LOCK) {
            startOffered = false;
            hadPending = pending != null;
            pending = null;
            pendingSource = null;
            pressToken = null;
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
            hoverX = x;
            hoverY = y;
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
                        && (currentAction & allowedActions
                                & target.getAcceptedDropActions()) == 0) {
                    // The permitted set changed while the pointer stayed put, which is what the
                    // desktop modifier does. An action agreed under the old set is no longer on
                    // offer, so keeping it told the platform something it had just withdrawn.
                    //
                    // Either side of it may have changed. A target narrowing its own accepted
                    // actions -- setAcceptedDropActions while the pointer rests on it -- is
                    // just as much a withdrawal as the source's modifier, and looking only at
                    // the source's mask kept advertising a move the target had stopped
                    // permitting: the release was then refused outright by the intersection
                    // at the drop, rather than settling for the copy it would still have
                    // taken.
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
        boolean local;
        int advertised;
        synchronized (LOCK) {
            local = active != null;
            advertised = advertisedActions;
        }
        return drop(windowId, x, y, content, action, advertised, local);
    }

    /// Delivers a native drop whose origin the port knows.
    ///
    /// A port that assembles a drop asynchronously calls this one, because by the time the
    /// assembly finishes the drag it belongs to may no longer be the one running: a drop that
    /// arrived from another application, still loading when the user began a drag of their
    /// own, would otherwise be reported to the target as local -- and a target that uses
    /// `NativeDropEvent#isLocal()` to tell reordering from importing would treat foreign
    /// content as an internal move.
    ///
    /// #### Parameters
    ///
    /// - `advertisedActions`: the mask *this* drag offered, or `NativeDragOperation#ACTION_NONE`
    ///   to use whatever the last drag event advertised. Carried for the same reason as the
    ///   locality beside it: a newer drag has since overwritten what the framework remembers,
    ///   and giving this drop that newer mask made its event report an action the source never
    ///   offered -- or, when the newer drag is narrower, report nothing accepted at all while
    ///   the platform had been told the drop succeeded.
    ///
    /// - `local`: true when the drag being dropped is one this application started
    ///
    /// #### Returns
    ///
    /// the action actually accepted, or `NativeDragOperation#ACTION_NONE`
    public static int drop(int windowId, int x, int y, ClipboardContent content, int action,
            int advertisedActions, boolean local) {
        return drop(windowId, x, y, content, action, advertisedActions, local, false);
    }

    /// Delivers a native drop whose action was already decided when the user released.
    ///
    /// For a port that assembles a drop asynchronously *and* keeps that session's decision
    /// with the session -- iOS does both. The ordinary entry point prefers what the component
    /// hovering said last, because a port's action is by construction one drag event behind;
    /// but a drop that has been loading is no longer the hovering session, and the framework
    /// keeps one hover state. Another drop hovering the same component in the meantime would
    /// otherwise lend this one its decision: its rejection would discard a drop the user had
    /// actually performed, and its acceptance would change the action this one reports.
    ///
    /// So the caller's action is taken as the answer here, narrowed only by what the target
    /// still permits. The hover state is cleared as it is for any drop -- a component holding
    /// a highlight for a drag that has moved on is sent an exit and re-entered by its next
    /// update, which is a frame that repairs itself, where a discarded drop is work the user
    /// did and lost.
    ///
    /// #### Parameters
    ///
    /// - `action`: the action this drop's own session settled on when it was released
    ///
    /// #### Returns
    ///
    /// the action actually accepted, or `NativeDragOperation#ACTION_NONE`
    public static int deferredDrop(int windowId, int x, int y, ClipboardContent content,
            int action, int advertisedActions, boolean local) {
        return drop(windowId, x, y, content, action, advertisedActions, local, true);
    }

    private static int drop(int windowId, int x, int y, ClipboardContent content, int action,
            int advertisedActions, boolean local, boolean actionAlreadyDecided) {
        // Nothing materialized. Every representation the platform offered failed to be read --
        // a transferable that threw, a one-shot stream already spent -- and an empty payload is
        // not a drop. A target that filters on a type refuses it anyway, but one that takes
        // anything would have been handed nothing and both it and the source told the transfer
        // had happened. The state below is still cleared, and the component that was hovering is
        // still told the drag left it.
        boolean carriesSomething = content != null && content.getMimeTypes().length > 0;
        Component target = carriesSomething
                ? findTarget(windowId, x, y, content, action) : null;
        int accepted;
        Component previous;
        int advertised;
        if (carriesSomething && target == null) {
            // Nothing is at the release point any more. On a port that assembles a drop
            // asynchronously the tree can be rebuilt while the item providers are still
            // loading -- a form shown, a list replaced -- and the component that accepted
            // this drag is then no longer where it was. It is still the component that
            // accepted it, so it is offered the drop rather than the payload being dropped
            // on the floor.
            //
            // Only where the release did not move away from the hover. A release somewhere
            // else is a release somewhere else, and position is what a drop means everywhere
            // else in here -- so the pointer having travelled on to something that is not a
            // target is not a tree that shifted, and restoring the old target there would
            // hand the payload to a component the user let go somewhere away from.
            //
            // The comparison is with the last drag event's position rather than with what is
            // under the release point now, which was the rule this said it applied and could
            // not: a live surface answers its hit test with *something* for every point it
            // is asked about -- the container itself where nothing else is there -- so
            // "resolves to nothing" was a case that never arose, and the guard it described
            // did not exist. Staying still and finding the target gone is exactly the tree
            // moving under a slow load; moving and finding no target is the ordinary miss.
            if (releasedWhereItHovered(x, y)) {
                target = stillWillingHoverTarget(windowId, content, action);
            }
        }
        synchronized (LOCK) {
            // What this drag advertised: the caller's answer where it has one, otherwise what
            // the last drag event said. A drop arriving with neither -- which no real port
            // does -- has only the port's one action to report.
            advertised = advertisedActions;
            if (advertised == NativeDragOperation.ACTION_NONE) {
                advertised = NativeDragAndDrop.advertisedActions;
            }
            if (advertised == NativeDragOperation.ACTION_NONE) {
                advertised = action;
            }
            previous = currentTarget;
            if (!actionAlreadyDecided && target != null
                    && target == currentTarget) { // NOPMD CompareObjectsWithEquals
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
                //
                // Intersected with what is true at the release, not taken on its own. The action
                // the platform proposes can change after the last drag event -- a modifier key
                // let go turns a move into a copy -- and a target can narrow its own mask in the
                // same window. Reporting the decision regardless told the platform a move had
                // been performed, which on a local drag is the word a source deletes its data on,
                // while the event delivered to the target refused that same move as no longer
                // permitted.
                //
                // Nothing performable leaves nothing, rather than falling back to the declarative
                // answer: a target that chose a move did not agree to a copy, and one whose
                // callback rejected the drag outright must not have that refusal promoted into a
                // drop.
                accepted = currentAction & action & target.getAcceptedDropActions();
            } else {
                // A different component from the one the callbacks were about: the pointer
                // moved between the last drag event and the drop, so there is no decision of
                // its own to honour and the declarative answer is the right one.
                //
                // Or a caller that brought its own decision -- see deferredDrop -- in which
                // case the hover state may belong to a different session entirely and this
                // is the only answer that is about *this* drop.
                accepted = target == null ? NativeDragOperation.ACTION_NONE
                        : preferredAction(action & target.getAcceptedDropActions());
            }
            currentTarget = null;
            targetGeneration++;
            overDispatchPending = false;
            currentAction = accepted;
        }
        if (previous != null
                && (previous != target // NOPMD CompareObjectsWithEquals
                        || accepted == NativeDragOperation.ACTION_NONE)) {
            // A release that lands somewhere else -- a quick move and let go -- ends the drag
            // for the component it was over, and that component has to be told. Clearing the
            // target without it left the old hover highlight on for good: the drop goes to
            // somebody else, and the port's own end-of-session cleanup then finds the target
            // already cleared and has nothing left to deliver the exit to.
            //
            // And when the drop is refused, even though the component is the one that was
            // hovering. It receives no drop, the hover state has just been cleared, and the
            // port's cleanup will find nothing to tell -- so a component that clears its
            // highlight from nativeDragExit or nativeDrop was left highlighted for good by
            // a drop it had itself refused.
            dispatch(previous, ActionEvent.Type.NativeDragExit, content, x, y, action,
                    NativeDragOperation.ACTION_NONE, Boolean.valueOf(local));
        }
        if (accepted == NativeDragOperation.ACTION_NONE) {
            return NativeDragOperation.ACTION_NONE;
        }
        // Queued after the exit above, so a component losing the drag hears about it before
        // the one taking it hears about the drop. The event carries the source's whole mask
        // and the action actually being performed -- different questions that used to get
        // the same answer, so the drop reported the chosen action as though it were all the
        // source had ever allowed.
        dispatch(target, ActionEvent.Type.NativeDrop, content, x, y, advertised, accepted,
                Boolean.valueOf(local));
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
                // Narrowed exactly as the drop narrows it. This exists so that what the platform
                // commits to is what the drop then reports, so the two have to answer the same
                // question -- an accepted action the release no longer proposes is not something
                // either of them may promise.
                return currentAction & action & target.getAcceptedDropActions();
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
            if (op != null) {
                // Owed here, not in a second lock afterwards. Releasing the drag is what
                // lets the next one start, and the event dispatch thread can start this
                // very operation again in between: it would find nothing owed, reset the
                // result, and the outcome recorded a moment later would then be reported
                // during the session that had already begun -- an old move telling a
                // source to delete what the new drag is carrying.
                op.oweCompletion(performedAction);
            }
        }
        if (op == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                deliverCompletion(op);
            }
        });
    }

    // ------------------------------------------------------------------------------------

    /// The component this drag was last over, when it is still part of a live surface and
    /// still willing to take what has arrived -- otherwise null.
    ///
    /// Asked only when the position no longer names anything. A component detached from its
    /// surface cannot be dropped on: it has no coordinates to speak of and nothing would
    /// repaint.
    /// True when this release is at the point the drag last reported hovering.
    ///
    /// Within the same slop a press uses to become a drag, because the platform's release
    /// point and its last hover update are rarely the same pixel and a rescue that needed
    /// them to be would never fire on any real port.
    private static boolean releasedWhereItHovered(int x, int y) {
        int slop = dragThreshold();
        synchronized (LOCK) {
            return Math.abs(x - hoverX) <= slop && Math.abs(y - hoverY) <= slop;
        }
    }

    private static Component stillWillingHoverTarget(int windowId, ClipboardContent content,
            int actions) {
        Component hovered;
        synchronized (LOCK) {
            hovered = currentTarget;
        }
        // On the surface this drop was released on, not merely on some live surface. A tree
        // rebuilt while the payload loaded can have moved the component to another window
        // entirely, and delivering a drop released on one surface to a component now living
        // on another is not a rescue -- it is a drop somewhere the user never released it.
        if (hovered != null && TopLevelSupport.rootOf(hovered) != surfaceFor(windowId)) { // NOPMD CompareObjectsWithEquals
            return null;
        }
        // Every test findTarget applies, including the one about pointer events: a
        // component that opted out of being pointed at between the hover and the drop is
        // not a target any more, and the walk has already skipped it -- so restoring it
        // here was the one way it could still be dropped on.
        if (hovered == null || TopLevelSupport.rootOf(hovered) == null
                || !visibleInHierarchy(hovered)
                || !hovered.isNativeDropTarget() || hovered.isIgnorePointerEvents()
                || !hovered.isEnabled()
                || (actions & hovered.getAcceptedDropActions()) == 0) {
            return null;
        }
        try {
            return hovered.canAcceptNativeDrop(content) ? hovered : null;
        } catch (Throwable err) {
            Log.e(err);
            return null;
        }
    }

    /// True when this component and every ancestor above it is visible, which is what hit
    /// testing means by visible: getComponentAt does not descend into a container that is
    /// not, so a component hidden by an ancestor is unreachable by position even though its
    /// own flag says otherwise. The fallback has to agree with the walk about that, or a
    /// drop the user can no longer see is delivered to something they cannot see it land on.
    private static boolean visibleInHierarchy(Component cmp) {
        for (Component above = cmp; above != null; above = above.getParent()) {
            if (!above.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /// Resolves the deepest component under the pointer that is willing to take this content.
    ///
    /// Runs on the native drag thread and reads the component tree without mutating it, which
    /// is the same thing the ports already do to route a native pointer press.
    ///
    /// Plain reads, and deliberately not published ones. The answer rests on the whole walk --
    /// the hit test, the bounds and parent links it follows, isEnabled, and the target's own
    /// canAcceptNativeDrop -- so marking the drop target fields volatile would not make this
    /// see a consistent tree. It would only make three names look guarded while everything the
    /// same answer depends on stayed exactly as it is, which is a worse account of what happens
    /// here than the honest one. Component state is event dispatch thread state in this
    /// framework, and this read cannot be marshalled onto that thread: the platform demands its
    /// answer inline, and the event dispatch thread blocks on the platform's thread to paint --
    /// the deadlock the class comment records.
    ///
    /// A target that has to change its mind while a drag is already in flight has the callback
    /// for it: reject() from the drag over event, which is delivered on the event dispatch
    /// thread and honoured from the next event onwards. The declarative fields read here are
    /// what a target settles before a drag begins.
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
            if (acceptsDrop(cmp, content, actions)) {
                return cmp;
            }
            cmp = cmp.getParent();
        }
        // Nothing on the path hit testing answered with -- and that answer is not always the
        // component deepest under the point. getComponentAt reports where a *pointer press*
        // would go, and it promotes a focusable container over a child that is not focusable;
        // being a native drop target has no say in it, since respondsToPointerEvents knows
        // nothing about that. A target inside a focusable container -- a scrollable one, among
        // plenty of others -- was therefore never examined at all: no enter, no drop, while the
        // same component took the drag when its parent was plain. So the descent is made here
        // instead, looking for what the pointer is actually over.
        //
        // After the walk rather than instead of it: where hit testing does answer with a
        // target, or with a child of one, that answer is the same component a press would
        // reach, and native drops and pointer events should not disagree about that.
        try {
            return descendToTarget(root, x, y, content, actions);
        } catch (Throwable err) {
            // Mid-layout, as above.
            return null;
        }
    }

    /// The topmost native drop target under a point, searched for directly.
    ///
    /// Last child first, because that is the one painted over the others and so the one the
    /// user is pointing at. A component that ignores pointer events is passed over but not its
    /// children: it is the component that opted out, not the subtree.
    private static Component descendToTarget(Container root, int x, int y,
            ClipboardContent content, int actions) {
        for (int iter = root.getComponentCount() - 1; iter >= 0; iter--) {
            Component child = root.getComponentAt(iter);
            if (child == null || !child.isVisible() || !child.contains(x, y)) {
                continue;
            }
            if (child instanceof Container) {
                Component found = descendToTarget((Container) child, x, y, content, actions);
                if (found != null) {
                    return found;
                }
            }
            if (acceptsDrop(child, content, actions)) {
                return child;
            }
        }
        return null;
    }

    /// Whether this component would take this drag, by everything it says declaratively.
    private static boolean acceptsDrop(Component cmp, ClipboardContent content, int actions) {
        if (!cmp.isNativeDropTarget() || cmp.isIgnorePointerEvents() || !cmp.isEnabled()
                || (actions & cmp.getAcceptedDropActions()) == 0) {
            return false;
        }
        try {
            return cmp.canAcceptNativeDrop(content);
        } catch (Throwable err) {
            Log.e(err);
            return false;
        }
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
        dispatch(target, type, content, x, y, allowedActions, NativeDragOperation.ACTION_NONE, null);
    }

    /// `knownLocal` is null when the caller has no better answer than the drag running now,
    /// which is right for every event that happens while it is running. Only a drop assembled
    /// after the fact knows better.
    private static void dispatch(final Component target, final ActionEvent.Type type,
            final ClipboardContent content, final int x, final int y, final int allowedActions,
            final int performedAction, final Boolean knownLocal) {
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
            local = knownLocal == null ? active != null : knownLocal.booleanValue();
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
                            //
                            // And the mask this event was built with. The platform's offer
                            // changes under the framework -- releasing the modifier turns a
                            // move and copy into a copy -- and neither the target nor the
                            // generation moves when it does. A callback queued under the older
                            // offer then started from a decision its own event cannot express,
                            // accept() refused it as outside what that event permits, and this
                            // wrote the refusal back: a target with no listener at all was
                            // recorded as rejecting the drag, on nothing but a modifier key.
                            if (generation == targetGeneration
                                    && allowedActions == advertisedActions
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

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


package com.codename1.impl.android;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.view.DragEvent;
import android.view.View;

import com.codename1.io.Log;
import com.codename1.ui.ClipboardContent;
import com.codename1.ui.ClipboardDataProvider;
import com.codename1.ui.NativeDragAndDrop;
import com.codename1.ui.NativeDragOperation;

/// Android's drag and drop, wired to the framework's.
///
/// Android has had in-application drag and drop since Ice Cream Sandwich, but a drag could not
/// leave the application until Nougat added `View#DRAG_FLAG_GLOBAL`. That is the split reported
/// by `#isSupported()` and `#isOutsideApplicationSupported()`: on a phone before Nougat a drag
/// can still move things around inside the application, and on a tablet or a Chromebook running
/// Nougat or later it can be dropped into another application beside it.
///
/// #### Files
///
/// A dragged file travels as a `content:` URI rather than a path, which is also how the
/// clipboard carries one, so the same `AndroidImplementation#contentFromClip(android.content.ClipData)`
/// reader serves both. A URI from another application is only readable while the drop's
/// permission grant is held, which is why the content is read inside the drop callback rather
/// than handed to the event dispatch thread to read later.
final class AndroidNativeDragAndDrop {
    /// The operation currently being dragged out of this application, so that the outcome
    /// reported when the drag ends can be attributed to it, and the action last agreed with the
    /// framework, reported back when the drop is accepted -- Android's drag events carry no
    /// copy/move/link distinction of their own.
    ///
    /// Both are written from the Codename One event dispatch thread and read from the Android
    /// UI thread, so the lock is what publishes one to the other.
    private static final Object LOCK = new Object();
    private static NativeDragOperation exporting;

    /// What the framework last answered about a drag over this surface, or `UNDECIDED` before it
    /// has answered anything.
    ///
    /// The distinction matters: `NativeDragOperation#ACTION_NONE` is a refusal, and Android
    /// hands ACTION_DROP to a subscribed view whatever it answered to the location events -- so
    /// treating a refusal as "no answer yet" and substituting a default turned a target's
    /// reject() back into a delivered drop.
    private static final int UNDECIDED = -1;
    private static int lastAction = UNDECIDED;

    /// What a drop of *our own* session onto one of our own components settled on, kept until
    /// the session ends so the source is told what really happened. Android's drag events carry
    /// no action, so this is the only place the true answer exists.
    private static int localDropAction = NativeDragOperation.ACTION_NONE;

    private static NativeDragOperation exporting() {
        synchronized (LOCK) {
            return exporting;
        }
    }

    private static void setExporting(NativeDragOperation op) {
        synchronized (LOCK) {
            exporting = op;
        }
    }

    private static int lastAction() {
        synchronized (LOCK) {
            return lastAction;
        }
    }

    private static void setLastAction(int action) {
        synchronized (LOCK) {
            lastAction = action;
        }
    }

    private static int localDropAction() {
        synchronized (LOCK) {
            return localDropAction;
        }
    }

    private static void setLocalDropAction(int action) {
        synchronized (LOCK) {
            localDropAction = action;
        }
    }

    private AndroidNativeDragAndDrop() {
    }

    /// Returns true when this device can drag and drop at all.
    static boolean isSupported() {
        return Build.VERSION.SDK_INT >= 11;
    }

    /// Returns true when a drag started here can be dropped into another application, which
    /// Android only allows from Nougat onwards.
    static boolean isOutsideApplicationSupported() {
        return Build.VERSION.SDK_INT >= 24;
    }

    /// Makes the Codename One surface a drop target. Called once, as the view is created.
    ///
    /// #### Parameters
    ///
    /// - `impl`: the implementation, used to read dropped content
    ///
    /// - `view`: the Android view Codename One renders into
    static void install(final AndroidImplementation impl, final View view) {
        if (!isSupported() || view == null) {
            return;
        }
        try {
            view.setOnDragListener(new View.OnDragListener() {
                @Override
                public boolean onDrag(View v, DragEvent event) {
                    return handle(impl, event);
                }
            });
        } catch (Throwable err) {
            Log.e(err);
        }
    }

    /// Starts an Android drag for the operation the framework decided on. Invoked on the
    /// Codename One event dispatch thread; the drag itself has to begin on the Android UI
    /// thread, which is what the post below is for.
    static boolean startDrag(final AndroidImplementation impl, final NativeDragOperation op) {
        if (op == null || !isSupported()) {
            return false;
        }
        final CodenameOneSurface surface = impl.myView;
        final View view = surface == null ? null : surface.getAndroidView();
        if (view == null) {
            return false;
        }
        final AndroidImplementation.AssembledClip assembled = toClipData(impl, op.getContent());
        final ClipData clip = assembled == null ? null : assembled.getData();
        if (clip == null) {
            return false;
        }
        // The drag holds this clip until it ends, and copies made while it runs must not age
        // its files out from under the receiver. The assembly's own id, not the latest one:
        // a clipboard copy on the Android UI thread can have begun since.
        AndroidImplementation.dragHolds(assembled.getClip());
        setExporting(op);
        setLastAction(UNDECIDED);
        setLocalDropAction(NativeDragOperation.ACTION_NONE);
        boolean posted = view.post(new Runnable() {
            @Override
            public void run() {
                boolean started = false;
                try {
                    View.DragShadowBuilder shadow = shadowFor(view, op);
                    if (Build.VERSION.SDK_INT >= 24) {
                        int flags = View.DRAG_FLAG_GLOBAL | View.DRAG_FLAG_GLOBAL_URI_READ;
                        started = view.startDragAndDrop(clip, shadow, null, flags);
                    } else {
                        started = view.startDrag(clip, shadow, null, 0);
                    }
                } catch (Throwable err) {
                    Log.e(err);
                }
                if (!started) {
                    AndroidImplementation.dragHolds(0);
                    setExporting(null);
                    NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
                }
            }
        });
        if (!posted) {
            // The view's message queue would not take it -- its looper is going away, which
            // is what an activity transition looks like from here -- so the runnable will
            // never run and nothing will ever report this drag finished. Everything staged
            // for it goes back, and the refusal is returned rather than a completion fired:
            // the framework has made the operation active by now and a false answer is what
            // it clears it on, exactly as when the port cannot start a session at all.
            // Reported success instead, this left a drag that had never begun looking like
            // one still running, and every drag after it was refused for the life of the
            // process.
            AndroidImplementation.dragHolds(0);
            setExporting(null);
            return false;
        }
        return true;
    }

    /// Forgets what a press staged, because the press turned out to be a click.
    ///
    /// What the press staged, never what a session is carrying -- the distinction the iOS side
    /// draws in CN1CancelNativeDrag. This port fills the export slot in startDrag and nowhere
    /// else: priming a press stages nothing here, so while a drag is running the slot holds
    /// that drag. A second press during one -- a second finger, or a press the platform
    /// delivers alongside the drag -- stages an operation of its own, and releasing it reaches
    /// this method with the drag untouched by any of it. Clearing the slot then
    /// took the running drag's operation away, and with it the exporting() != null guard that
    /// ACTION_DRAG_ENDED reports the outcome through -- so the framework's drag stayed active
    /// for the life of the process and every drag after it was refused.
    ///
    /// Asking the framework whether a drag is running is what tells the two apart. A slot with
    /// nothing running is a leftover and still goes.
    static void cancelDrag() {
        if (NativeDragAndDrop.getActiveDrag() == null) {
            setExporting(null);
        }
    }

    // ------------------------------------------------------------------------------------

    private static boolean handle(AndroidImplementation impl, DragEvent event) {
        try {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // Returning true is what subscribes this view to the rest of the drag;
                    // a view that answers false here never sees another event, so the answer
                    // is unconditional and the real filtering happens per position below.
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    setLastAction(NativeDragAndDrop.dragEnter(0, (int) event.getX(), (int) event.getY(),
                            describe(event.getClipDescription()), allowedActions()));
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    setLastAction(NativeDragAndDrop.dragOver(0, (int) event.getX(), (int) event.getY(),
                            describe(event.getClipDescription()), allowedActions()));
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    NativeDragAndDrop.dragExit(0);
                    setLastAction(UNDECIDED);
                    return true;
                case DragEvent.ACTION_DROP:
                    return drop(impl, event);
                case DragEvent.ACTION_DRAG_ENDED:
                    // Whatever happened, nothing is hovered any more. Android delivers this to
                    // every subscribed view, and it is the only event that arrives on the paths
                    // that otherwise clear nothing: a drop the target refused returns before
                    // NativeDragAndDrop.drop(), and a drag from another application never
                    // reaches the dragCompleted() below. A stale target left the component
                    // stuck at its refusal for the next drag, which was then routed as a move
                    // over it rather than an entry.
                    NativeDragAndDrop.dragExit(0);
                    if (exporting() != null) {
                        // Settled *before* the operation is forgotten. Reading the allowed
                        // actions afterwards is how this reported every move as a copy: with
                        // nothing exporting, allowedActions() answers with its copy fallback.
                        int completed = completedAction(event.getResult());
                        setExporting(null);
                        NativeDragAndDrop.dragCompleted(completed);
                    }
                    // The drag is over, so its clip goes back to ageing out with the rest.
                    // Not deleted here: a receiver that kept a URI may still read it, and the
                    // window of recent clips is what covers that.
                    AndroidImplementation.dragHolds(0);
                    setLastAction(UNDECIDED);
                    setLocalDropAction(NativeDragOperation.ACTION_NONE);
                    return true;
                default:
                    return false;
            }
        } catch (Throwable err) {
            Log.e(err);
            return false;
        }
    }

    /// What to tell the source a finished session actually did.
    ///
    /// A drop onto one of this application's own components knows exactly what was accepted,
    /// and that is the answer -- without it a move accepted locally was reported as a copy and
    /// a source relying on ACTION_MOVE to delete its data never did.
    ///
    /// A drop into *another* application cannot be answered so precisely: Android's drag
    /// protocol carries no notion of copy versus move, and ACTION_DRAG_ENDED reports only a
    /// boolean. Copy is the honest reading of "it succeeded and we do not know how", and it is
    /// also the safe one, because reporting a move the receiver may not have performed would
    /// have the source delete data nothing else holds. That holds even for an operation that
    /// allows only a move: what the source was willing to permit says nothing about what the
    /// receiver did, and an ordinary Android target simply copies the clip.
    private static int completedAction(boolean result) {
        if (!result) {
            return NativeDragOperation.ACTION_NONE;
        }
        int local = localDropAction();
        if (local != NativeDragOperation.ACTION_NONE) {
            return local;
        }
        // A copy, whatever the source was willing to allow. An external receiver has read the
        // clip and Android gives it no way to say more than that, so a copy is what actually
        // happened. Answering ACTION_MOVE because the source offered nothing else would infer
        // ownership from our own wishes: the receiver may simply have copied, and a source that
        // deletes on ACTION_MOVE would then destroy the only remaining copy.
        return NativeDragOperation.ACTION_COPY;
    }

    private static boolean drop(AndroidImplementation impl, DragEvent event) {
        int decided = lastAction();
        if (decided == NativeDragOperation.ACTION_NONE) {
            // Refused while it hovered. Android delivers ACTION_DROP to a subscribed view even
            // so, and substituting a default here is what turned a target's reject() back into
            // a delivered drop. Reporting failure also makes ACTION_DRAG_ENDED report no action.
            //
            // Asked before the clip is read rather than after. Materializing it opens an
            // external URI and reads the whole stream on Android's own UI thread, so a large
            // image dropped on a component that had already said no stalled the application,
            // or ran it out of memory, to produce a value the next line threw away. Nothing
            // below is needed either: a drop nobody accepts needs no permission to read what
            // it will not read.
            return false;
        }
        // A URI dropped by another application is only readable while this grant is held, and
        // the grant only exists from here on. Reading the content inside this method rather
        // than on the event dispatch thread is what keeps a dropped file readable.
        if (Build.VERSION.SDK_INT >= 24 && impl.getActivity() != null) {
            try {
                impl.getActivity().requestDragAndDropPermissions(event);
            } catch (Throwable err) {
                // A drag from within this application needs no grant and refuses one.
                Log.e(err);
            }
        }
        // With the description, so the types the drag advertised while it hovered survive into
        // the content the drop is filtered against -- otherwise a target that accepted the
        // hover on MIME_URI_LIST is refused the drop it was promised.
        ClipboardContent content = impl.contentFromClip(event.getClipData(), event.getClipDescription());
        int action = decided == UNDECIDED ? preferred(allowedActions()) : decided;
        int accepted = NativeDragAndDrop.drop(0, (int) event.getX(), (int) event.getY(), content, action);
        setLastAction(NativeDragOperation.ACTION_NONE);
        if (exporting() != null) {
            // Our own drag, dropped on our own surface: remember what the target took, because
            // ACTION_DRAG_ENDED is about to be asked and has no way of knowing.
            setLocalDropAction(accepted);
        }
        return accepted != NativeDragOperation.ACTION_NONE;
    }

    /// The actions in play. A drag this application started offers whatever its source allowed;
    /// one arriving from another application is a copy, because Android's cross-application
    /// drag has no way to express anything else.
    private static int allowedActions() {
        NativeDragOperation op = exporting();
        return op == null ? NativeDragOperation.ACTION_COPY : op.getAllowedActions();
    }

    private static int preferred(int actions) {
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

    /// Describes a drag in progress from its MIME types alone.
    ///
    /// Android does not hand over the data until the drop, so every representation here is a
    /// provider that answers null. That is enough: a drop target decides whether it wants the
    /// drag from the MIME types, and the real content arrives on the drop.
    private static ClipboardContent describe(ClipDescription description) {
        NativeDragOperation op = exporting();
        if (op != null && description == null) {
            // This application's own drag and nothing else to go on: the operation says what
            // it offers.
            return op.getContent();
        }
        // Otherwise the description, even for a drag of our own. The clip is what will
        // actually be delivered, and it can hold less than the content asked for: a
        // provider that answered null or threw leaves its type out of the clip while the
        // content still names it. Describing the hover from the content then promised a
        // type the drop could not produce, so a target filtered to it lit up for the whole
        // drag and was refused at the end -- the exact failure every rule below avoids.
        ClipboardContent content = new ClipboardContent();
        if (description == null) {
            return content;
        }
        // What the source of *our own* drag actually published, or null for a drag from
        // another application. The aliases below are inferences, and an inference is only
        // needed where the truth is unavailable: a clip this port assembled carries bytes on a
        // transport URI it minted, and contentFromClip deliberately refuses to call one of
        // those a file. Inferring one here anyway had a file-only target light up for the whole
        // drag of an image and then be refused the drop -- the hover promising what the drop
        // cannot keep, which is the one thing this method exists to avoid.
        NativeDragOperation exported = exporting();
        ClipboardContent published = exported == null ? null : exported.getContent();
        for (int iter = 0; iter < description.getMimeTypeCount(); iter++) {
            String mime = description.getMimeType(iter);
            if (mime == null) {
                continue;
            }
            // Locale independent: see AndroidImplementation.asciiLower for what a Turkish
            // default does to String.toLowerCase() and to a MIME type run through it.
            mime = AndroidImplementation.asciiLower(mime);
            if ("text/uri-list".equals(mime)) {
                // Android carries a dragged file as a URI, which is what the framework calls a
                // file list; advertise both so either kind of target matches.
                //
                // This is the one thing a hover cannot get right for a drag from elsewhere. A
                // file manager and a browser both describe themselves as text/uri-list, and the
                // clip that would tell them apart -- content: against https: -- is withheld
                // until the drop. Promising files is the useful way to be wrong: a link drag is
                // then refused at the drop rather than never accepted at all. For a drag of our
                // own there is nothing to guess, which is what declareAlias consults.
                declareAlias(content, published, ClipboardContent.MIME_FILE);
                declareAlias(content, published, ClipboardContent.MIME_URI_LIST);
                continue;
            }
            declare(content, mime);
            if (!mime.startsWith("text/")) {
                // A type that is not text is carried by a URI, because that is the only way
                // an Android clip carries anything else -- and a URI another application put
                // in a clip is a file reference as well as that type. Without this, an
                // ordinary content:// PDF from another app describes itself as application/pdf
                // alone, a target restricted to files refused every hover event, and the drop
                // never happened -- while contentFromClip went on to call the very same URI a
                // file.
                declareAlias(content, published, ClipboardContent.MIME_FILE);
                declareAlias(content, published, ClipboardContent.MIME_URI_LIST);
            }
        }
        return content;
    }

    /// Declares one of the file aliases a URI item implies, unless the source is known and did
    /// not publish it.
    ///
    /// #### Parameters
    ///
    /// - `published`: the content behind a drag this application started, or null when the drag
    ///   came from somewhere else and the alias has to be inferred
    private static void declareAlias(ClipboardContent content, ClipboardContent published,
            String mime) {
        if (published == null || published.hasMimeType(mime)) {
            declare(content, mime);
            return;
        }
        // A file the source published is a URI as well, and the drop says so: contentFromClip
        // builds the list back out of the URIs the clip carried, so a component filtered to
        // MIME_URI_LIST would have been refused the hover and then handed the very list it
        // asked for -- and takes that drag on the other two ports. Not the other way round: a
        // URI list may be nothing but links, and only the entries that name something local
        // come back as files.
        if (ClipboardContent.MIME_URI_LIST.equals(mime)
                && published.hasMimeType(ClipboardContent.MIME_FILE)) {
            declare(content, mime);
        }
    }

    private static void declare(ClipboardContent content, String mime) {
        if (content.hasMimeType(mime)) {
            return;
        }
        content.setDataProvider(mime, new ClipboardDataProvider() {
            @Override
            public Object getClipboardData(String requested) {
                // Android reveals nothing until the drop; the drop callback replaces this with
                // the real content.
                return null;
            }
        });
    }

    /// Builds the Android clip for an outgoing drag. The clipboard already knows how to turn a
    /// `ClipboardContent` into a clip -- including writing image bytes out through the
    /// application's file provider so another application can read them -- so this reuses that
    /// rather than growing a second conversion that would drift from it.
    ///
    /// #### Promised representations are produced here, not on demand
    ///
    /// `ClipboardContent#setDataProvider(java.lang.String, com.codename1.ui.ClipboardDataProvider)`
    /// is honoured lazily on the desktop and on iOS, where the platform asks for a
    /// representation when a receiver reads it. Android has no such moment: startDragAndDrop
    /// takes a complete `android.content.ClipData`, and a clip carries text or a URI to a file
    /// that already exists. So every provider runs when the drag begins, including for a drag
    /// the user goes on to abandon.
    ///
    /// A content provider resolving its bytes on demand would restore the guarantee, but it
    /// needs a second provider declared in the manifest -- which is generated by the builder,
    /// in another repository -- so it is not something this file can reach for.
    private static AndroidImplementation.AssembledClip toClipData(AndroidImplementation impl,
            ClipboardContent content) {
        try {
            return impl.clipDataFor(content);
        } catch (Throwable err) {
            Log.e(err);
            return null;
        }
    }

    /// The image under the finger during the drag: whatever the operation supplied, rendered at
    /// the offset the gesture grabbed it by.
    private static View.DragShadowBuilder shadowFor(View view, NativeDragOperation op) {
        com.codename1.ui.Image image = op.getDragImage();
        Object peer = image == null ? null : image.getImage();
        if (!(peer instanceof Bitmap)) {
            return new View.DragShadowBuilder(view);
        }
        final Bitmap bitmap = (Bitmap) peer;
        final int touchX = Math.max(0, Math.min(bitmap.getWidth(), op.getDragImageOffsetX()));
        final int touchY = Math.max(0, Math.min(bitmap.getHeight(), op.getDragImageOffsetY()));
        return new View.DragShadowBuilder(view) {
            @Override
            public void onProvideShadowMetrics(Point size, Point touch) {
                size.set(Math.max(1, bitmap.getWidth()), Math.max(1, bitmap.getHeight()));
                touch.set(touchX, touchY);
            }

            @Override
            public void onDrawShadow(Canvas canvas) {
                canvas.drawBitmap(bitmap, 0, 0, null);
            }
        };
    }
}

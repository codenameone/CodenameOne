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
    private static int lastAction = NativeDragOperation.ACTION_NONE;

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
        final ClipData clip = toClipData(impl, op.getContent());
        if (clip == null) {
            return false;
        }
        setExporting(op);
        setLastAction(NativeDragOperation.ACTION_NONE);
        view.post(new Runnable() {
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
                    setExporting(null);
                    NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
                }
            }
        });
        return true;
    }

    /// Forgets a prepared operation because the press turned out to be a click.
    static void cancelDrag() {
        setExporting(null);
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
                    setLastAction(NativeDragOperation.ACTION_NONE);
                    return true;
                case DragEvent.ACTION_DROP:
                    return drop(impl, event);
                case DragEvent.ACTION_DRAG_ENDED:
                    if (exporting() != null) {
                        int allowed = allowedActions();
                        setExporting(null);
                        NativeDragAndDrop.dragCompleted(event.getResult()
                                ? preferred(allowed) : NativeDragOperation.ACTION_NONE);
                    }
                    setLastAction(NativeDragOperation.ACTION_NONE);
                    return true;
                default:
                    return false;
            }
        } catch (Throwable err) {
            Log.e(err);
            return false;
        }
    }

    private static boolean drop(AndroidImplementation impl, DragEvent event) {
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
        ClipboardContent content = impl.contentFromClip(event.getClipData());
        int action = lastAction() == NativeDragOperation.ACTION_NONE
                ? preferred(allowedActions()) : lastAction();
        int accepted = NativeDragAndDrop.drop(0, (int) event.getX(), (int) event.getY(), content, action);
        setLastAction(NativeDragOperation.ACTION_NONE);
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
        ClipboardContent content = new ClipboardContent();
        if (description == null) {
            return content;
        }
        for (int iter = 0; iter < description.getMimeTypeCount(); iter++) {
            String mime = description.getMimeType(iter);
            if (mime == null) {
                continue;
            }
            mime = mime.toLowerCase();
            if ("text/uri-list".equals(mime)) {
                // Android carries a dragged file as a URI, which is what the framework calls a
                // file list; advertise both so either kind of target matches.
                declare(content, ClipboardContent.MIME_FILE);
                declare(content, ClipboardContent.MIME_URI_LIST);
                continue;
            }
            declare(content, mime);
        }
        return content;
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
    private static ClipData toClipData(AndroidImplementation impl, ClipboardContent content) {
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

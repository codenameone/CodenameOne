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


package com.codename1.impl.javase;

import com.codename1.io.Log;
import com.codename1.ui.ClipboardContent;
import com.codename1.ui.ClipboardDataProvider;
import com.codename1.ui.NativeDragAndDrop;
import com.codename1.ui.NativeDragOperation;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

/// Bridges Codename One's native drag and drop onto AWT's, which is what lets a desktop
/// application drag content out of itself -- onto the desktop, into a file manager, into another
/// application's window -- and accept drags coming the other way.
///
/// #### Which thread does what
///
/// AWT delivers drag notifications on its own event dispatch thread, which is not Codename
/// One's. Nothing here ever blocks one on the other: the Codename One event thread calls into
/// `javax.swing.TransferHandler` through `java.awt.EventQueue#invokeLater`, and the drop
/// callbacks answer AWT from `NativeDragAndDrop`, which resolves the target without needing the
/// Codename One event thread. That is not fastidiousness -- the Codename One event thread
/// blocks on AWT to blit every frame, so a synchronous call the other way deadlocks the
/// simulator on the first drag.
///
/// #### Why the drag reads nothing until the drop
///
/// While a drag is merely passing over the window the transferable's *data* is not reliably
/// readable -- on some platforms it does not exist yet -- but its list of flavors always is. So
/// a drag in progress is described by a `ClipboardContent` whose representations are all
/// `ClipboardDataProvider`s: enough for a drop target to say whether it wants a `text/html` or
/// a file list, without a byte being transferred for a drag that ends up going somewhere else.
/// On the drop the content is materialized eagerly instead, because the transferable stops
/// being readable the moment the drop callback returns.
final class JavaSENativeDragAndDrop {
    /// The operation the Codename One event thread has asked to export, read by the transfer
    /// handler on the AWT thread when the drag actually starts. One process drags one thing at
    /// a time, so a single slot is the whole of the state; the lock is what publishes it from
    /// one thread to the other.
    private static final Object LOCK = new Object();
    private static NativeDragOperation exporting;

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

    private JavaSENativeDragAndDrop() {
    }

    /// Makes one canvas both an AWT drop target and a drag source.
    ///
    /// #### Parameters
    ///
    /// - `canvas`: the canvas, which is the surface for the main form or for one window
    static void install(JavaSEPort.C canvas) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            canvas.setTransferHandler(new Cn1TransferHandler());
            new DropTarget(canvas, DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK,
                    new Cn1DropTargetListener(canvas), true);
        } catch (Throwable err) {
            // A desktop with no drag and drop service leaves the canvas as it was; the
            // lightweight drag and drop is unaffected.
            Log.e(err);
        }
    }

    /// Starts an AWT drag for the operation Codename One has decided on. Invoked on the
    /// Codename One event dispatch thread while the mouse button is still down.
    ///
    /// #### Returns
    ///
    /// true when the export was handed to AWT; the outcome arrives later through
    /// `NativeDragAndDrop#dragCompleted(int)`
    static boolean startDrag(final JavaSEPort port, final NativeDragOperation op) {
        if (op == null || GraphicsEnvironment.isHeadless()) {
            return false;
        }
        final JavaSEPort.C target = port.dndGestureCanvas != null ? port.dndGestureCanvas : port.canvas;
        if (target == null) {
            return false;
        }
        final InputEvent trigger = port.dndLastInputEvent();
        if (!(trigger instanceof MouseEvent)) {
            // AWT seeds a drag from the mouse event that provoked it and refuses without one.
            return false;
        }
        final java.awt.Image dragImage = toAwtDragImage(op, target);
        final Point offset = new Point(
                (int) (op.getDragImageOffsetX() / target.canvasScale()),
                (int) (op.getDragImageOffsetY() / target.canvasScale()));
        setExporting(op);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    TransferHandler handler = target.getTransferHandler();
                    if (handler == null) {
                        NativeDragAndDrop.dragCompleted(NativeDragOperation.ACTION_NONE);
                        return;
                    }
                    if (dragImage != null) {
                        handler.setDragImage(dragImage);
                        handler.setDragImageOffset(offset);
                    }
                    handler.exportAsDrag(target, trigger, toAwtAction(preferred(op.getAllowedActions())));
                } catch (Throwable err) {
                    Log.e(err);
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

    /// Renders the operation's drag image at the size AWT expects.
    ///
    /// Codename One images are in surface pixels while AWT places a drag image in points, so on
    /// a scaled display the image has to come down by the backing scale or the user drags a
    /// picture twice the size of the thing they grabbed.
    private static java.awt.Image toAwtDragImage(NativeDragOperation op, JavaSEPort.C canvas) {
        com.codename1.ui.Image image = op.getDragImage();
        if (image == null) {
            return null;
        }
        Object peer = image.getImage();
        if (!(peer instanceof java.awt.Image)) {
            return null;
        }
        java.awt.Image awt = (java.awt.Image) peer;
        double scale = canvas.canvasScale();
        if (scale <= 1.0) {
            return awt;
        }
        int w = Math.max(1, (int) (image.getWidth() / scale));
        int h = Math.max(1, (int) (image.getHeight() / scale));
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(awt, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    // ------------------------------------------------------------------------------------
    // Action mapping. AWT's constants are a different bit set from ours, and both sides use
    // masks in some places and a single action in others.
    // ------------------------------------------------------------------------------------

    static int toAwtActions(int actions) {
        int out = DnDConstants.ACTION_NONE;
        if ((actions & NativeDragOperation.ACTION_COPY) != 0) {
            out |= DnDConstants.ACTION_COPY;
        }
        if ((actions & NativeDragOperation.ACTION_MOVE) != 0) {
            out |= DnDConstants.ACTION_MOVE;
        }
        if ((actions & NativeDragOperation.ACTION_LINK) != 0) {
            out |= DnDConstants.ACTION_LINK;
        }
        return out;
    }

    static int fromAwtActions(int actions) {
        int out = NativeDragOperation.ACTION_NONE;
        if ((actions & DnDConstants.ACTION_COPY) != 0) {
            out |= NativeDragOperation.ACTION_COPY;
        }
        if ((actions & DnDConstants.ACTION_MOVE) != 0) {
            out |= NativeDragOperation.ACTION_MOVE;
        }
        if ((actions & DnDConstants.ACTION_LINK) != 0) {
            out |= NativeDragOperation.ACTION_LINK;
        }
        return out;
    }

    static int toAwtAction(int action) {
        return toAwtActions(action);
    }

    static int preferred(int actions) {
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

    // ------------------------------------------------------------------------------------
    // Reading an AWT transferable as a ClipboardContent.
    // ------------------------------------------------------------------------------------

    /// Maps an AWT flavor onto the MIME type Codename One names that representation by, or null
    /// when the flavor carries nothing the framework can express.
    private static String mimeFor(DataFlavor flavor) {
        if (flavor == null) {
            return null;
        }
        if (DataFlavor.javaFileListFlavor.equals(flavor)) {
            return ClipboardContent.MIME_FILE;
        }
        if (DataFlavor.imageFlavor.equals(flavor)) {
            return ClipboardContent.MIME_PNG;
        }
        if (flavor.getRepresentationClass() != null
                && java.awt.Image.class.isAssignableFrom(flavor.getRepresentationClass())) {
            // A decoded image, whatever the flavor calls itself. All this can produce from one
            // is a PNG, so PNG is what it advertises -- filing PNG bytes under image/jpeg or
            // image/webp because the flavor said so handed a target bytes it could not decode
            // by the type it had asked for. A source offering the real encoded bytes offers
            // them through a stream flavor as well, and that one keeps its own type.
            return ClipboardContent.MIME_PNG;
        }
        if (DataFlavor.stringFlavor.equals(flavor)) {
            return ClipboardContent.MIME_TEXT;
        }
        String primary = flavor.getPrimaryType();
        String sub = flavor.getSubType();
        if (primary == null || sub == null) {
            return null;
        }
        String mime = (primary + "/" + sub).toLowerCase();
        if ("application/rtf".equals(mime)) {
            return ClipboardContent.MIME_RTF;
        }
        if ("application/x-java-file-list".equals(mime)) {
            return ClipboardContent.MIME_FILE;
        }
        if (mime.startsWith("application/x-java")) {
            // AWT's own transport flavors -- serialized objects, local object references, the
            // text-encoding list. They describe how a payload travels between Java processes,
            // not what it is, and reading them can hand back arbitrary live objects.
            return null;
        }
        if (mime.startsWith("text/") || mime.startsWith("image/")) {
            return mime;
        }
        // Anything else the source offers in a shape this can actually read. RichTransferable
        // exports arbitrary binary types on the way out, so refusing them on the way in left a
        // component filtered to, say, application/pdf unable to receive one at all.
        Class<?> representation = flavor.getRepresentationClass();
        if (representation != null
                && (InputStream.class.isAssignableFrom(representation)
                        || byte[].class.equals(representation)
                        || String.class.equals(representation)
                        || java.io.Reader.class.isAssignableFrom(representation))) {
            return mime;
        }
        return null;
    }

    /// Describes a transferable as a `ClipboardContent`.
    ///
    /// #### Parameters
    ///
    /// - `transferable`: the AWT transferable
    ///
    /// - `flavors`: the flavors it is offering, in the order AWT reported them
    ///
    /// - `eager`: true to read every representation now, which is only correct inside a drop
    ///   callback; false to register providers that read on demand, which is what a drag in
    ///   progress needs
    static ClipboardContent contentFor(final Transferable transferable, DataFlavor[] flavors, boolean eager) {
        ClipboardContent content = new ClipboardContent();
        if (transferable == null || flavors == null) {
            return content;
        }
        for (int iter = 0; iter < flavors.length; iter++) {
            final DataFlavor flavor = flavors[iter];
            final String mime = mimeFor(flavor);
            if (mime == null || content.hasMimeType(mime)) {
                // The first flavor offering a MIME type wins: AWT lists them in the source's
                // preference order, and the richer representation is the earlier one.
                continue;
            }
            if (eager) {
                Object value = readValue(transferable, flavor, mime);
                if (value != null) {
                    content.setData(mime, value);
                }
            } else {
                content.setDataProvider(mime, new ClipboardDataProvider() {
                    @Override
                    public Object getClipboardData(String requested) {
                        return readValue(transferable, flavor, requested);
                    }
                });
            }
        }
        // A file list is also a URI list as far as most applications are concerned, and a drag
        // out of a Linux file manager offers only the latter. Presenting both means a drop
        // target that asks for files gets them either way. Declared the same way the rest of
        // the content is -- eagerly on a drop, on demand during a drag -- so describing a drag
        // still reads nothing.
        if (!content.hasMimeType(ClipboardContent.MIME_FILE) && content.hasMimeType(ClipboardContent.MIME_URI_LIST)) {
            if (eager) {
                content.setFiles(pathsFromUriList(content.getText(ClipboardContent.MIME_URI_LIST)));
            } else {
                final ClipboardContent describing = content;
                content.setDataProvider(ClipboardContent.MIME_FILE, new ClipboardDataProvider() {
                    @Override
                    public Object getClipboardData(String requested) {
                        String[] paths = pathsFromUriList(describing.getText(ClipboardContent.MIME_URI_LIST));
                        if (paths == null) {
                            return null;
                        }
                        return paths.length == 1 ? (Object) paths[0] : paths;
                    }
                });
            }
        }
        return content;
    }

    /// Reads one representation out of a transferable, converting it into the value type the
    /// MIME type implies. Returns null rather than throwing: a flavor that turns out to be
    /// unreadable is simply one the drop does not offer.
    private static Object readValue(Transferable transferable, DataFlavor flavor, String mime) {
        try {
            Object out = transferable.getTransferData(flavor);
            if (out == null) {
                return null;
            }
            if (ClipboardContent.MIME_FILE.equals(mime)) {
                return filePaths(out);
            }
            if (ClipboardContent.MIME_PNG.equals(mime) && out instanceof java.awt.Image) {
                return JavaSEPort.imageToPngBytes((java.awt.Image) out);
            }
            if (mime.startsWith("image/")) {
                if (out instanceof byte[]) {
                    return out;
                }
                if (out instanceof InputStream) {
                    return readBytes((InputStream) out);
                }
                if (out instanceof java.awt.Image) {
                    return JavaSEPort.imageToPngBytes((java.awt.Image) out);
                }
                return null;
            }
            if (mime.startsWith("text/") && !(out instanceof String) && flavor.isFlavorTextType()) {
                // A text flavor is free to hand over bytes -- text/html;class="[B" and
                // text/plain;class=java.io.InputStream are both ordinary on the desktop -- and
                // the encoding those bytes are in is a parameter of the flavor, not something
                // to assume. Storing them as a binary payload made getText() answer null for a
                // type the drop had just accepted, and decoding them as UTF-8 by hand would get
                // a charset=UTF-16 flavor wrong. DataFlavor's own reader is what knows.
                String text = textFromFlavor(transferable, flavor);
                if (text != null) {
                    return text;
                }
            }
            if (out instanceof byte[]) {
                return out;
            }
            if (!mime.startsWith("text/") && out instanceof InputStream) {
                // A non-text type read as a stream is bytes, not characters; decoding it as
                // UTF-8 the way the text path does would corrupt a PDF or an archive.
                return readBytes((InputStream) out);
            }
            return JavaSEPort.clipboardText(out);
        } catch (Throwable err) {
            return null;
        }
    }

    /// Reads a text flavor through the reader the flavor itself supplies, which applies the
    /// charset the flavor declares. Returns null when the flavor will not produce one, leaving
    /// the caller's own handling to run.
    private static String textFromFlavor(Transferable transferable, DataFlavor flavor) {
        try {
            java.io.Reader reader = flavor.getReaderForText(transferable);
            if (reader == null) {
                return null;
            }
            try {
                StringBuilder out = new StringBuilder();
                char[] buffer = new char[2048];
                int read;
                while ((read = reader.read(buffer)) >= 0) {
                    out.append(buffer, 0, read);
                }
                return out.toString();
            } finally {
                reader.close();
            }
        } catch (Throwable err) {
            return null;
        }
    }

    /// Turns whatever a file flavor produced -- a list of files, or a URI list as text -- into
    /// absolute paths.
    private static Object filePaths(Object value) throws Exception {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> paths = new ArrayList<String>();
            for (Object o : list) {
                if (o instanceof File) {
                    paths.add(((File) o).getAbsolutePath());
                } else if (o != null) {
                    paths.add(o.toString());
                }
            }
            if (paths.isEmpty()) {
                return null;
            }
            return paths.size() == 1 ? (Object) paths.get(0) : paths.toArray(new String[paths.size()]);
        }
        String[] fromUris = pathsFromUriList(JavaSEPort.clipboardText(value));
        if (fromUris == null) {
            return null;
        }
        return fromUris.length == 1 ? (Object) fromUris[0] : fromUris;
    }

    /// Parses the newline separated `text/uri-list` format, keeping only the `file:` entries,
    /// which is what a drop from a file manager or the desktop consists of.
    private static String[] pathsFromUriList(String uriList) {
        if (uriList == null || uriList.length() == 0) {
            return null;
        }
        List<String> paths = new ArrayList<String>();
        String[] lines = uriList.split("\r\n|\n|\r");
        for (int iter = 0; iter < lines.length; iter++) {
            String line = lines[iter].trim();
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            try {
                if (line.startsWith("file:")) {
                    paths.add(new File(new URI(line)).getAbsolutePath());
                }
            } catch (Throwable err) {
                // Not a URI this platform writes; skip it rather than fail the whole drop.
            }
        }
        return paths.isEmpty() ? null : paths.toArray(new String[paths.size()]);
    }

    private static byte[] readBytes(InputStream input) throws Exception {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            input.close();
        }
    }

    // ------------------------------------------------------------------------------------

    /// Exports whatever `#startDrag(JavaSEPort, com.codename1.ui.NativeDragOperation)` staged.
    /// The payload travels as the same `JavaSEPort.RichTransferable` a copy uses, which is why
    /// a drag out of the application lands correctly in a text editor, an image editor and a
    /// file manager alike.
    private static final class Cn1TransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            NativeDragOperation op = exporting();
            return op == null ? NONE : toAwtActions(op.getAllowedActions());
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            NativeDragOperation op = exporting();
            return op == null ? null : new JavaSEPort.RichTransferable(op.getContent());
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            setExporting(null);
            NativeDragAndDrop.dragCompleted(preferred(fromAwtActions(action)));
        }
    }

    /// Receives drags entering one canvas and routes them into the framework.
    private static final class Cn1DropTargetListener implements DropTargetListener {
        private final JavaSEPort.C canvas;

        Cn1DropTargetListener(JavaSEPort.C canvas) {
            this.canvas = canvas;
        }

        @Override
        public void dragEnter(DropTargetDragEvent e) {
            respond(e, true);
        }

        @Override
        public void dragOver(DropTargetDragEvent e) {
            respond(e, false);
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent e) {
            respond(e, false);
        }

        @Override
        public void dragExit(DropTargetEvent e) {
            try {
                NativeDragAndDrop.dragExit(canvas.windowId);
            } catch (Throwable err) {
                Log.e(err);
            }
        }

        @Override
        public void drop(DropTargetDropEvent e) {
            try {
                int allowed = fromAwtActions(e.getSourceActions());
                int action = preferred(fromAwtActions(e.getDropAction()));
                if (action == NativeDragOperation.ACTION_NONE) {
                    action = preferred(allowed);
                }
                Point at = e.getLocation();
                int x = canvas.scaleCoordinateX(at.x);
                int y = canvas.scaleCoordinateY(at.y);
                // What the framework will settle on, asked before anything is committed. AWT
                // has to be told the action when the drop is accepted, and that is before the
                // transferable can be read -- so accepting the action AWT proposed and only
                // then learning the target had chosen another reported a copy to the source
                // through exportDone while handing the target a move.
                action = NativeDragAndDrop.plannedDropAction(canvas.windowId, x, y,
                        contentFor(e.getTransferable(), e.getCurrentDataFlavors(), false), action);
                if (action == NativeDragOperation.ACTION_NONE) {
                    e.rejectDrop();
                    return;
                }
                // Before reading anything: on every platform the transferable only becomes
                // readable once the drop has been accepted, and it stops being readable when
                // this method returns -- which is why the content is materialized here rather
                // than handed to the event dispatch thread as a live view of the transfer.
                e.acceptDrop(toAwtAction(action));
                ClipboardContent content = contentFor(e.getTransferable(), e.getCurrentDataFlavors(), true);
                int accepted = NativeDragAndDrop.drop(canvas.windowId, x, y, content, action);
                e.dropComplete(accepted != NativeDragOperation.ACTION_NONE);
            } catch (Throwable err) {
                Log.e(err);
                try {
                    e.dropComplete(false);
                } catch (Throwable ignored) {
                    // The drop is already over; nothing left to report to.
                }
            }
        }

        /// What the source permits *at this instant*.
        ///
        /// getSourceActions is the whole mask the source offered, and answering with it alone
        /// made the framework prefer a copy every time -- so holding the platform's modifier to
        /// ask for a move changed nothing, because getDropAction, which is where AWT records
        /// that choice, was never read. The user's choice wins where the source allows it, and
        /// the full mask stands where it does not.
        private int allowedActionsFor(DropTargetDragEvent e) {
            int sourceActions = fromAwtActions(e.getSourceActions());
            int chosen = fromAwtActions(e.getDropAction()) & sourceActions;
            return chosen == NativeDragOperation.ACTION_NONE ? sourceActions : chosen;
        }

        private void respond(DropTargetDragEvent e, boolean entering) {
            try {
                Point at = e.getLocation();
                int x = canvas.scaleCoordinateX(at.x);
                int y = canvas.scaleCoordinateY(at.y);
                ClipboardContent content = contentFor(e.getTransferable(), e.getCurrentDataFlavors(), false);
                int allowed = allowedActionsFor(e);
                int action = entering
                        ? NativeDragAndDrop.dragEnter(canvas.windowId, x, y, content, allowed)
                        : NativeDragAndDrop.dragOver(canvas.windowId, x, y, content, allowed);
                if (action == NativeDragOperation.ACTION_NONE) {
                    e.rejectDrag();
                } else {
                    e.acceptDrag(toAwtAction(action));
                }
            } catch (Throwable err) {
                Log.e(err);
                try {
                    e.rejectDrag();
                } catch (Throwable ignored) {
                    // The drag has already moved on.
                }
            }
        }
    }
}

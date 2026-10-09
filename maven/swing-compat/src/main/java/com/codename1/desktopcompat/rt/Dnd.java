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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.datatransfer.DataFlavor;
import com.codename1.desktopcompat.java.awt.datatransfer.Transferable;
import com.codename1.desktopcompat.java.awt.datatransfer.UnsupportedFlavorException;
import com.codename1.desktopcompat.java.awt.dnd.DnDConstants;
import com.codename1.desktopcompat.java.awt.dnd.DropTarget;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetDragEvent;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetDropEvent;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetEvent;
import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.ui.ClipboardContent;
import com.codename1.ui.Display;
import com.codename1.ui.NativeDragOperation;
import com.codename1.ui.NativeDropEvent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// The one drag that can be under way, and the drop target it is over.
///
/// A drag comes from one of two places. One that begins in the layer,
/// with `TransferHandler.exportAsDrag`, is followed here from the pointer
/// events the [EventBridge] hands over: nothing leaves the application and
/// the data is the `Transferable` the handler made. One that the platform
/// reports -- files or text from another application -- arrives through
/// Codename One's native drop events on the peer of the window.
///
/// Either way the target is the deepest component under the pointer that
/// has an active `DropTarget`, as in AWT, and its listener is told of
/// enter, over, exit and drop in the component's own coordinates. A
/// listener that neither accepts nor rejects a drag has accepted the
/// action the user asked for, when the target takes that action.
public final class Dnd {

    /// What hears that a drag begun in the layer is over.
    public interface Source {

        /// The drag ended; `action` is what the target did with the
        /// data, `ACTION_NONE` when it was not dropped or not taken.
        void done(int action);
    }

    private static final String LISTENER = "cn1.desktopcompat.dnd";

    private static boolean wanted;

    private static Transferable data;
    private static int sourceActions;
    private static boolean localDrag;
    private static Source source;

    private static DropTarget over;
    private static int answer;
    private static boolean dropping;
    private static boolean completed;
    private static boolean success;

    private Dnd() {
    }

    // ------------------------------------------------------------ targets

    /// Something in the application takes drops: from here on the windows
    /// ask the platform for the drags it reports.
    public static void want() {
        if (wanted) {
            return;
        }
        wanted = true;
        Window[] all = WindowHosts.windows();
        for (Window w : all) {
            listen(w);
        }
    }

    /// Whether anything in the application takes drops.
    public static boolean wanted() {
        return wanted;
    }

    /// Asks the platform for the drags over `w`, once it has a peer and
    /// when something takes drops at all.
    public static void listen(Window w) {
        if (!wanted || w == null || !Display.isInitialized()) {
            return;
        }
        com.codename1.ui.Component peer = w.cn1PeerOrNull();
        if (peer == null || peer.getClientProperty(LISTENER) != null) {
            return;
        }
        Drops d = new Drops(w);
        peer.putClientProperty(LISTENER, d);
        peer.setNativeDropTarget(true);
        peer.addNativeDragOverListener(d);
        peer.addNativeDropListener(d);
    }

    /// The data of the drag under way, or `null`.
    public static Transferable transferable() {
        return data;
    }

    /// The answer of the listener of `t` to the drag over it.
    public static void answer(DropTarget t, int action) {
        if (t != over) {
            return;
        }
        answer = single(action);
    }

    /// The listener of `t` says whether it took the data dropped on it.
    public static void complete(DropTarget t, boolean ok) {
        if (t != over || !dropping) {
            return;
        }
        completed = true;
        success = ok;
    }

    /// The first of move, copy and link that is in `actions`.
    private static int single(int actions) {
        if ((actions & DnDConstants.ACTION_MOVE) != 0) {
            return DnDConstants.ACTION_MOVE;
        }
        if ((actions & DnDConstants.ACTION_COPY) != 0) {
            return DnDConstants.ACTION_COPY;
        }
        if ((actions & DnDConstants.ACTION_LINK) != 0) {
            return DnDConstants.ACTION_LINK;
        }
        return DnDConstants.ACTION_NONE;
    }

    private static Point local(Window w, Component target, int x, int y) {
        int lx = x;
        int ly = y;
        for (Component c = target; c != null && c != w; c = c.getParent()) {
            lx -= c.getX();
            ly -= c.getY();
        }
        return new Point(lx, ly);
    }

    private static DropTarget targetAt(Window w, int x, int y) {
        if (w == null) {
            return null;
        }
        for (Component c = w.findComponentAt(x, y); c != null; c = c.getParent()) {
            DropTarget t = c.getDropTarget();
            if (t != null && t.isActive() && t.getComponent() == c) {
                return t;
            }
        }
        return null;
    }

    private static void leave() {
        DropTarget old = over;
        if (old != null) {
            try {
                old.dragExit(new DropTargetEvent(old.getDropTargetContext()));
            } finally {
                over = null;
                answer = DnDConstants.ACTION_NONE;
            }
        }
    }

    /// A drag is at `x`, `y` of `w`, in logical pixels. `t` is its data,
    /// `allowed` the actions its source allows and `user` the one the
    /// user asked for. Answers the action the target under the pointer
    /// accepts, `ACTION_NONE` when there is none or it refused.
    public static int over(Window w, int x, int y, Transferable t, int allowed, int user, boolean local) {
        data = t;
        sourceActions = allowed;
        DropTarget target = targetAt(w, x, y);
        boolean entered = target != over;
        if (entered) {
            leave();
            over = target;
        }
        if (target == null) {
            return DnDConstants.ACTION_NONE;
        }
        answer = user;
        DropTargetDragEvent e = new DropTargetDragEvent(target.getDropTargetContext(),
                local(w, target.getComponent(), x, y), user, allowed);
        if (entered) {
            target.dragEnter(e);
        } else {
            target.dragOver(e);
        }
        if (over != target || (answer & target.getDefaultActions()) == 0) {
            return DnDConstants.ACTION_NONE;
        }
        return answer;
    }

    /// The drag left every window, or was given up.
    public static void exit() {
        try {
            leave();
        } finally {
            if (!localDrag) {
                data = null;
            }
        }
    }

    /// The drag was let go at `x`, `y` of `w`. Answers what the target
    /// did with the data: the action it accepted when it reported
    /// success, `ACTION_NONE` otherwise.
    public static int drop(Window w, int x, int y, Transferable t, int allowed, int user, boolean local) {
        int accepted = over(w, x, y, t, allowed, user, local);
        DropTarget target = over;
        if (target == null) {
            data = null;
            return DnDConstants.ACTION_NONE;
        }
        if (accepted == DnDConstants.ACTION_NONE) {
            // As a platform does: a drag let go where it was refused
            // leaves the target, it is not dropped on it.
            try {
                leave();
            } finally {
                data = null;
            }
            return DnDConstants.ACTION_NONE;
        }
        dropping = true;
        completed = false;
        success = false;
        answer = DnDConstants.ACTION_NONE;
        try {
            target.drop(new DropTargetDropEvent(target.getDropTargetContext(),
                    local(w, target.getComponent(), x, y), accepted, allowed, local));
            return completed && success ? answer : DnDConstants.ACTION_NONE;
        } finally {
            dropping = false;
            over = null;
            answer = DnDConstants.ACTION_NONE;
            data = null;
        }
    }

    // ------------------------------------------------------------ drags of the layer's own

    /// Begins a drag of `t` within the application. From now until the
    /// pointer is let go its events are the drag's: see [#dragging()].
    public static void begin(Transferable t, int actions, Source s) {
        if (localDrag) {
            cancel();
        }
        data = t;
        sourceActions = actions;
        source = s;
        localDrag = true;
    }

    /// Whether a drag begun with [#begin] is under way.
    public static boolean dragging() {
        return localDrag;
    }

    /// The action a drag asks for: the one the modifier keys name, as on
    /// a desktop, or the first of move, copy and link the source allows.
    public static int userAction(int modifiers, int allowed) {
        boolean ctrl = (modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.CTRL_MASK)) != 0;
        boolean shift = (modifiers & (InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK)) != 0;
        int asked;
        if (ctrl && shift) {
            asked = DnDConstants.ACTION_LINK;
        } else if (ctrl) {
            asked = DnDConstants.ACTION_COPY;
        } else if (shift) {
            asked = DnDConstants.ACTION_MOVE;
        } else {
            return single(allowed);
        }
        return asked & allowed;
    }

    /// The pointer moved during a drag of the layer's own.
    static void localMove(Window w, int x, int y, int modifiers) {
        over(w, x, y, data, sourceActions, userAction(modifiers, sourceActions), true);
    }

    /// The pointer was let go during a drag of the layer's own.
    static void localDrop(Window w, int x, int y, int modifiers) {
        int done = DnDConstants.ACTION_NONE;
        try {
            done = drop(w, x, y, data, sourceActions, userAction(modifiers, sourceActions), true);
        } finally {
            end(done);
        }
    }

    /// Gives up a drag of the layer's own.
    public static void cancel() {
        if (!localDrag) {
            return;
        }
        try {
            leave();
        } finally {
            end(DnDConstants.ACTION_NONE);
        }
    }

    private static void end(int action) {
        Source s = source;
        source = null;
        localDrag = false;
        data = null;
        if (s != null) {
            s.done(action);
        }
    }

    // ------------------------------------------------------------ drags the platform reports

    private static int fromNative(int actions) {
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

    private static int toNative(int action) {
        switch (action) {
            case DnDConstants.ACTION_COPY:
                return NativeDragOperation.ACTION_COPY;
            case DnDConstants.ACTION_MOVE:
                return NativeDragOperation.ACTION_MOVE;
            case DnDConstants.ACTION_LINK:
                return NativeDragOperation.ACTION_LINK;
            default:
                return NativeDragOperation.ACTION_NONE;
        }
    }

    /// What a drag from outside asks for when it allows several actions:
    /// a copy, which leaves the other application's data where it is.
    private static int preferred(int allowed) {
        if ((allowed & DnDConstants.ACTION_COPY) != 0) {
            return DnDConstants.ACTION_COPY;
        }
        return single(allowed);
    }

    /// The listener on the peer of one window.
    private static final class Drops implements ActionListener<ActionEvent> {

        private final Window window;
        private ClipboardContent seen;
        private Transferable made;

        Drops(Window w) {
            window = w;
        }

        private Transferable dataOf(ClipboardContent c) {
            if (c != seen || made == null) {
                seen = c;
                made = new Content(c);
            }
            return made;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (!(evt instanceof NativeDropEvent) || localDrag) {
                return;
            }
            NativeDropEvent ev = (NativeDropEvent) evt;
            ActionEvent.Type type = ev.getEventType();
            if (type == ActionEvent.Type.NativeDragExit) {
                exit();
                return;
            }
            com.codename1.ui.Component peer = window.cn1PeerOrNull();
            if (peer == null) {
                ev.reject();
                return;
            }
            int x = Units.toLogical(ev.getX() - peer.getAbsoluteX());
            int y = Units.toLogical(ev.getY() - peer.getAbsoluteY());
            int allowed = fromNative(ev.getAllowedActions());
            int user = preferred(allowed);
            Transferable t = dataOf(ev.getContent());
            int action;
            if (type == ActionEvent.Type.NativeDrop) {
                seen = null;
                made = null;
                action = drop(window, x, y, t, allowed, user, ev.isLocal());
            } else {
                action = over(window, x, y, t, allowed, user, ev.isLocal());
            }
            if (action == DnDConstants.ACTION_NONE) {
                ev.reject();
            } else {
                ev.accept(toNative(action));
            }
        }
    }

    /// What the platform is dragging, as a `Transferable`: files as
    /// `javaFileListFlavor`, text as `stringFlavor`.
    private static final class Content implements Transferable {

        private final ClipboardContent content;

        Content(ClipboardContent c) {
            content = c;
        }

        private boolean files() {
            return content != null && content.hasMimeType(ClipboardContent.MIME_FILE);
        }

        private boolean text() {
            return content != null && content.hasMimeType(ClipboardContent.MIME_TEXT);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            boolean f = files();
            boolean s = text();
            if (f && s) {
                return new DataFlavor[]{DataFlavor.javaFileListFlavor, DataFlavor.stringFlavor};
            }
            if (f) {
                return new DataFlavor[]{DataFlavor.javaFileListFlavor};
            }
            if (s) {
                return new DataFlavor[]{DataFlavor.stringFlavor};
            }
            return new DataFlavor[0];
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                return files();
            }
            return DataFlavor.stringFlavor.equals(flavor) && text();
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                String[] paths = content.getFiles();
                if (paths == null) {
                    // A platform names what a drag carries while it
                    // hovers and hands the values over with the drop.
                    throw new IOException("The files of a drag can be read once it is dropped");
                }
                List<java.io.File> out = new ArrayList<java.io.File>(paths.length);
                for (String p : paths) {
                    if (p != null) {
                        out.add(new java.io.File(p));
                    }
                }
                return out;
            }
            String s = content.getText(ClipboardContent.MIME_TEXT);
            if (s == null) {
                throw new IOException("The text of a drag can be read once it is dropped");
            }
            return s;
        }
    }

    /// Forgets the drag under way. For tests.
    public static void reset() {
        data = null;
        source = null;
        localDrag = false;
        over = null;
        answer = DnDConstants.ACTION_NONE;
        dropping = false;
    }
}

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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Image;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Toolkit;
import com.codename1.desktopcompat.java.awt.datatransfer.Clipboard;
import com.codename1.desktopcompat.java.awt.datatransfer.DataFlavor;
import com.codename1.desktopcompat.java.awt.datatransfer.Transferable;
import com.codename1.desktopcompat.java.awt.dnd.DnDConstants;
import com.codename1.desktopcompat.java.awt.dnd.DropTarget;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetDragEvent;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetDropEvent;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetEvent;
import com.codename1.desktopcompat.java.awt.dnd.DropTargetListener;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.plaf.UIResource;
import com.codename1.desktopcompat.rt.Dnd;

/// Moves data out of a component and into one: by drag and drop, and by
/// cut, copy and paste.
///
/// A subclass says what it exports ([#getSourceActions],
/// [#createTransferable], [#exportDone]) and what it takes
/// ([#canImport(TransferSupport)], [#importData(TransferSupport)]).
/// Installing one on a component with `JComponent.setTransferHandler`
/// makes the component a drop target; a `JTable` with `setDragEnabled`
/// begins a drag of its handler's data when a selected row is dragged.
///
/// A drag begun this way stays inside the application: it is followed by
/// the layer and never handed to the platform, so it cannot be dropped on
/// another application. Drags from another application do arrive, see
/// `java.awt.dnd.DropTarget`.
///
/// The constructor that names a bean property is not here: it reads and
/// writes the property by reflection, which a device does not have.
public class TransferHandler {

    public static final int NONE = 0;

    public static final int COPY = 1;

    public static final int MOVE = 2;

    public static final int COPY_OR_MOVE = 3;

    public static final int LINK = 1 << 30;

    private static final Action CUT = new TransferAction("cut");
    private static final Action COPY_ACTION = new TransferAction("copy");
    private static final Action PASTE = new TransferAction("paste");

    private Image dragImage;
    private Point dragImageOffset;

    protected TransferHandler() {
    }

    /// An action that cuts from the component that is the source of its
    /// event to the system clipboard.
    public static Action getCutAction() {
        return CUT;
    }

    public static Action getCopyAction() {
        return COPY_ACTION;
    }

    /// An action that hands the contents of the system clipboard to the
    /// handler of the component that is the source of its event.
    public static Action getPasteAction() {
        return PASTE;
    }

    /// Records an image for the drag. The layer draws none: where a drop
    /// would go is shown by the target.
    public void setDragImage(Image img) {
        dragImage = img;
    }

    public Image getDragImage() {
        return dragImage;
    }

    public void setDragImageOffset(Point p) {
        dragImageOffset = new Point(p);
    }

    public Point getDragImageOffset() {
        return dragImageOffset == null ? new Point(0, 0) : new Point(dragImageOffset);
    }

    /// Begins a drag of the data of `comp`. `e` is the event that began
    /// it, which has to be a mouse event, and `action` one of [#COPY],
    /// [#MOVE] and [#LINK] that [#getSourceActions] allows; otherwise no
    /// drag begins and [#exportDone] is told so.
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        final JComponent c = comp;
        int srcActions = getSourceActions(comp);
        int a = action;
        if (!(e instanceof MouseEvent) || !(a == COPY || a == MOVE || a == LINK) || (srcActions & a) == 0) {
            a = NONE;
        }
        if (a == NONE) {
            exportDone(comp, null, NONE);
            return;
        }
        final Transferable t = createTransferable(comp);
        if (t == null) {
            exportDone(comp, null, NONE);
            return;
        }
        Dnd.begin(t, srcActions, new Dnd.Source() {
            @Override
            public void done(int result) {
                exportDone(c, t, result);
            }
        });
    }

    /// Puts the data of `comp` on `clip`.
    ///
    /// #### Throws
    ///
    /// - `IllegalStateException`: if the clipboard cannot be written
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        if ((action == COPY || action == MOVE) && (getSourceActions(comp) & action) != 0) {
            Transferable t = createTransferable(comp);
            if (t != null) {
                try {
                    clip.setContents(t, null);
                } catch (IllegalStateException ise) {
                    exportDone(comp, t, NONE);
                    throw ise;
                }
                exportDone(comp, t, action);
                return;
            }
        }
        exportDone(comp, null, NONE);
    }

    /// Takes the data of a drop or a paste. As it comes it hands the
    /// component and the data to [#importData(JComponent, Transferable)].
    public boolean importData(TransferSupport support) {
        Component c = support.getComponent();
        return c instanceof JComponent && importData((JComponent) c, support.getTransferable());
    }

    /// Takes data into `comp`; as it comes, takes nothing.
    public boolean importData(JComponent comp, Transferable t) {
        return false;
    }

    /// Whether the data of a drop or a paste would be taken. Asked again
    /// and again while a drag is over the component.
    public boolean canImport(TransferSupport support) {
        Component c = support.getComponent();
        return c instanceof JComponent && canImport((JComponent) c, support.getDataFlavors());
    }

    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return false;
    }

    /// The actions a drag out of `c` can ask for; as it comes, none.
    public int getSourceActions(JComponent c) {
        return NONE;
    }

    public Icon getVisualRepresentation(Transferable t) {
        return null;
    }

    /// The data to export from `c`; `null`, as it comes, exports nothing.
    protected Transferable createTransferable(JComponent c) {
        return null;
    }

    /// The export is over. `action` is what was done with the data,
    /// [#NONE] when nothing was; a source that moved its data removes it
    /// here.
    protected void exportDone(JComponent source, Transferable data, int action) {
    }

    /// The drop target a component has while it has a handler and no
    /// target of the application's own. Not Swing API.
    static DropTarget cn1DropTarget(JComponent c) {
        return new SwingDropTarget(c);
    }

    /// Where on a component a drop would go.
    public static class DropLocation {

        private final Point dropPoint;

        protected DropLocation(Point dropPoint) {
            if (dropPoint == null) {
                throw new IllegalArgumentException("Point cannot be null");
            }
            this.dropPoint = new Point(dropPoint);
        }

        public final Point getDropPoint() {
            return new Point(dropPoint);
        }

        @Override
        public String toString() {
            return getClass().getName() + "[dropPoint=" + dropPoint + "]";
        }
    }

    /// What a handler is asked about: a drop, or a paste.
    public static final class TransferSupport {

        private final boolean isDrop;
        private Component component;
        private Transferable transferable;
        private Point point;
        private int userAction;
        private int sourceActions;
        private boolean showDropLocationIsSet;
        private boolean showDropLocation;
        private int dropAction = -1;
        private DropLocation dropLocation;

        /// A transfer that is not a drop: a paste of `transferable` into
        /// `component`.
        public TransferSupport(Component component, Transferable transferable) {
            if (component == null) {
                throw new NullPointerException("component is null");
            }
            if (transferable == null) {
                throw new NullPointerException("transferable is null");
            }
            isDrop = false;
            this.component = component;
            this.transferable = transferable;
        }

        TransferSupport() {
            isDrop = true;
        }

        void drag(Component c, DropTargetDragEvent e) {
            set(c, e.getLocation(), e.getDropAction(), e.getSourceActions(), e.getTransferable());
        }

        void drop(Component c, DropTargetDropEvent e) {
            set(c, e.getLocation(), e.getDropAction(), e.getSourceActions(), e.getTransferable());
        }

        private void set(Component c, Point p, int user, int source, Transferable t) {
            component = c;
            point = p;
            userAction = user;
            sourceActions = source;
            transferable = t;
            dropLocation = null;
            dropAction = -1;
            showDropLocationIsSet = false;
        }

        boolean showLocation(boolean otherwise) {
            return showDropLocationIsSet ? showDropLocation : otherwise;
        }

        public boolean isDrop() {
            return isDrop;
        }

        public Component getComponent() {
            return component;
        }

        private void assureIsDrop() {
            if (!isDrop) {
                throw new IllegalStateException("Not a drop");
            }
        }

        /// Where the drop would go, as the component sees it: a
        /// `JTable.DropLocation` for a table.
        ///
        /// #### Throws
        ///
        /// - `IllegalStateException`: if this is not a drop
        public DropLocation getDropLocation() {
            assureIsDrop();
            if (dropLocation == null) {
                if (component instanceof JComponent) {
                    dropLocation = ((JComponent) component).dropLocationForPoint(point);
                }
                if (dropLocation == null) {
                    dropLocation = new DropLocation(point);
                }
            }
            return dropLocation;
        }

        /// Whether the component shows where the drop would go, said from
        /// `canImport`; unsaid, it shows it when the drop would be taken.
        public void setShowDropLocation(boolean showDropLocation) {
            assureIsDrop();
            this.showDropLocation = showDropLocation;
            showDropLocationIsSet = true;
        }

        /// Chooses the action of the drop, one the source allows.
        ///
        /// #### Throws
        ///
        /// - `IllegalArgumentException`: if the source does not allow it
        public void setDropAction(int dropAction) {
            assureIsDrop();
            int action = dropAction & getSourceDropActions();
            if (!(action == COPY || action == MOVE || action == LINK)) {
                throw new IllegalArgumentException("unsupported drop action: " + dropAction);
            }
            this.dropAction = dropAction;
        }

        public int getDropAction() {
            return dropAction == -1 ? getUserDropAction() : dropAction;
        }

        public int getUserDropAction() {
            assureIsDrop();
            return userAction;
        }

        public int getSourceDropActions() {
            assureIsDrop();
            return sourceActions;
        }

        public DataFlavor[] getDataFlavors() {
            return transferable == null ? new DataFlavor[0] : transferable.getTransferDataFlavors();
        }

        public boolean isDataFlavorSupported(DataFlavor df) {
            return transferable != null && transferable.isDataFlavorSupported(df);
        }

        public Transferable getTransferable() {
            return transferable;
        }
    }

    /// The target that asks the component's handler. It is a
    /// `UIResource`, which is how setting another handler knows it may
    /// replace it and leaves a target of the application's alone.
    private static final class SwingDropTarget extends DropTarget implements UIResource {

        SwingDropTarget(JComponent c) {
            super(c, COPY_OR_MOVE | LINK, new DropHandler(c), true);
        }
    }

    private static final class DropHandler implements DropTargetListener {

        private final JComponent component;
        private final TransferSupport support = new TransferSupport();
        private Object state;

        DropHandler(JComponent c) {
            component = c;
        }

        private void location(boolean show, boolean forDrop) {
            state = component.setDropLocation(show ? support.getDropLocation() : null, state, forDrop);
        }

        private void handleDrag(DropTargetDragEvent e) {
            TransferHandler importer = component.getTransferHandler();
            if (importer == null) {
                e.rejectDrag();
                location(false, false);
                return;
            }
            support.drag(component, e);
            boolean canImport = importer.canImport(support);
            if (canImport) {
                e.acceptDrag(support.getDropAction());
            } else {
                e.rejectDrag();
            }
            location(support.showLocation(canImport), false);
        }

        @Override
        public void dragEnter(DropTargetDragEvent e) {
            state = null;
            handleDrag(e);
        }

        @Override
        public void dragOver(DropTargetDragEvent e) {
            handleDrag(e);
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent e) {
            handleDrag(e);
        }

        @Override
        public void dragExit(DropTargetEvent e) {
            cleanup(false);
        }

        @Override
        public void drop(DropTargetDropEvent e) {
            TransferHandler importer = component.getTransferHandler();
            if (importer == null) {
                e.rejectDrop();
                cleanup(false);
                return;
            }
            support.drop(component, e);
            if (!importer.canImport(support)) {
                e.rejectDrop();
                cleanup(false);
                return;
            }
            e.acceptDrop(support.getDropAction());
            location(support.showLocation(true), false);
            boolean success;
            try {
                success = importer.importData(support);
            } catch (RuntimeException re) {
                success = false;
            }
            e.dropComplete(success);
            cleanup(success);
        }

        private void cleanup(boolean forDrop) {
            location(false, forDrop);
            state = null;
        }
    }

    /// Cut, copy and paste, done by the handler of the event's source.
    private static final class TransferAction extends AbstractAction implements UIResource {

        TransferAction(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object src = e.getSource();
            if (!(src instanceof JComponent)) {
                return;
            }
            JComponent c = (JComponent) src;
            TransferHandler th = c.getTransferHandler();
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (th == null || clipboard == null) {
                return;
            }
            Object name = getValue(Action.NAME);
            if ("cut".equals(name)) {
                th.exportToClipboard(c, clipboard, MOVE);
            } else if ("copy".equals(name)) {
                th.exportToClipboard(c, clipboard, COPY);
            } else if ("paste".equals(name)) {
                Transferable trans = clipboard.getContents(null);
                if (trans != null) {
                    th.importData(new TransferSupport(c, trans));
                }
            }
        }
    }
}

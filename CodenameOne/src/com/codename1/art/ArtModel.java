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
package com.codename1.art;

import com.codename1.art.effects.Effect;

import java.util.ArrayList;
import java.util.List;

/// An editable, undoable `ArtDocument` with a selection.
///
/// Every change goes through this class so it can be undone: the operations below cover the
/// common edits, and `edit` wraps any other change to the nodes. Undo keeps whole snapshots
/// of the tree, so any change can be undone, however it was made. Nodes are found by id: every
/// node gets one when the model takes the document or a new node, and the selection is kept
/// as ids so it survives undo.
///
/// Snapshots share pixel data with the document, so never draw into a `RasterNode`'s raster
/// in place: give the node a copy (`setImage(getImage().copy())`) after the checkpoint and
/// draw into that.
///
/// ```java
/// ArtModel model = new ArtModel(PsdReader.read(in));
/// ArtNode layer = model.findById("layer-3");
/// model.setOpacity(layer, 0.5f);
/// model.undo();
/// ```
public class ArtModel {
    private ArtDocument document;
    private final List<ArtListener> listeners = new ArrayList<ArtListener>();
    private final List<GroupNode> undo = new ArrayList<GroupNode>();
    private final List<GroupNode> redo = new ArrayList<GroupNode>();
    private final List<String> selection = new ArrayList<String>();
    private int undoLimit = 50;
    private int groupDepth;
    private boolean groupSaved;
    private int nextId = 1;

    /// Creates a model over an empty 800 by 600 document.
    public ArtModel() {
        this(new ArtDocument(800, 600));
    }

    /// Creates a model over a document. The model edits that document in place.
    ///
    /// #### Parameters
    ///
    /// - `document`: the document
    public ArtModel(ArtDocument document) {
        this.document = document;
        assignIds(document.getRoot());
    }

    /// The document.
    public ArtDocument getDocument() {
        return document;
    }

    /// Replaces the document and clears the undo history and the selection.
    ///
    /// #### Parameters
    ///
    /// - `doc`: the new document
    public void setDocument(ArtDocument doc) {
        document = doc;
        undo.clear();
        redo.clear();
        selection.clear();
        assignIds(doc.getRoot());
        fire(true);
    }

    /// Adds a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addArtListener(ArtListener l) {
        listeners.add(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeArtListener(ArtListener l) {
        listeners.remove(l);
    }

    private void fire(boolean content) {
        ArtListener[] ls = listeners.toArray(new ArtListener[listeners.size()]);
        for (int i = 0; i < ls.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            ls[i].artChanged(this, content);
        }
    }

    private void assignIds(ArtNode n) {
        if (n.getId() == null || n.getId().length() == 0 || (n != document.getRoot() && idTaken(n))) { // NOPMD CompareObjectsWithEquals - node identity
            String id;
            do {
                id = "n" + nextId++;
            } while (document.findById(id) != null);
            n.setId(id);
        }
        if (n instanceof GroupNode) {
            List<ArtNode> ch = ((GroupNode) n).getChildren();
            for (int i = 0; i < ch.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
                assignIds(ch.get(i));
            }
        }
    }

    private boolean idTaken(ArtNode n) {
        ArtNode f = document.findById(n.getId());
        return f != null && f != n; // NOPMD CompareObjectsWithEquals - node identity
    }

    // ---- undo ----

    /// Saves the current tree so the next change can be undone. Operations call this
    /// themselves; call it before changing nodes directly, or use `edit`.
    public void checkpoint() {
        if (groupDepth > 0) {
            if (groupSaved) {
                return;
            }
            groupSaved = true;
        }
        undo.add((GroupNode) document.getRoot().snapshot());
        while (undo.size() > undoLimit) {
            undo.remove(0);
        }
        redo.clear();
    }

    /// Starts a group of changes that undo as one.
    public void beginGroup() {
        if (groupDepth == 0) {
            groupSaved = false;
        }
        groupDepth++;
    }

    /// Ends a group started by `beginGroup`.
    public void endGroup() {
        if (groupDepth > 0) {
            groupDepth--;
        }
    }

    /// Makes an arbitrary change undoable: saves the tree, runs the change and notifies the
    /// listeners.
    ///
    /// #### Parameters
    ///
    /// - `change`: changes nodes of the document
    public void edit(Runnable change) {
        checkpoint();
        change.run();
        assignIds(document.getRoot());
        fire(true);
    }

    /// Tells the listeners the nodes changed, after a change made between `checkpoint` and
    /// now, such as each step of a drag.
    public void changed() {
        assignIds(document.getRoot());
        fire(true);
    }

    /// True when there is something to undo.
    public boolean canUndo() {
        return !undo.isEmpty();
    }

    /// True when there is something to redo.
    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /// Undoes the last change; returns false when there is none.
    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        redo.add(document.getRoot());
        document.setRoot(undo.remove(undo.size() - 1));
        pruneSelection();
        fire(true);
        return true;
    }

    /// Redoes the last undone change; returns false when there is none.
    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        undo.add(document.getRoot());
        document.setRoot(redo.remove(redo.size() - 1));
        pruneSelection();
        fire(true);
        return true;
    }

    /// How many changes are kept for undo.
    ///
    /// #### Parameters
    ///
    /// - `limit`: at least 1
    public void setUndoLimit(int limit) {
        undoLimit = Math.max(1, limit);
    }

    // ---- selection ----

    private void pruneSelection() {
        for (int i = selection.size() - 1; i >= 0; i--) {
            if (document.findById(selection.get(i)) == null) {
                selection.remove(i);
            }
        }
    }

    /// The selected nodes, in the order they were selected.
    public List<ArtNode> getSelection() {
        List<ArtNode> out = new ArrayList<ArtNode>();
        for (int i = 0; i < selection.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            ArtNode n = document.findById(selection.get(i));
            if (n != null) {
                out.add(n);
            }
        }
        return out;
    }

    /// The first selected node, or null.
    public ArtNode getSelected() {
        List<ArtNode> s = getSelection();
        return s.isEmpty() ? null : s.get(0);
    }

    /// Selects a node.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node, or null to clear the selection
    /// - `add`: true to add it to the selection, false to replace the selection
    public void select(ArtNode n, boolean add) {
        if (!add) {
            selection.clear();
        }
        if (n != null) {
            assignIds(document.getRoot());
            if (!selection.contains(n.getId())) {
                selection.add(n.getId());
            }
        }
        fire(false);
    }

    /// Clears the selection.
    public void clearSelection() {
        selection.clear();
        fire(false);
    }

    /// True when the node is selected.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    public boolean isSelected(ArtNode n) {
        return n != null && n.getId() != null && selection.contains(n.getId());
    }

    // ---- lookup ----

    /// The node with an id, or null.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    public ArtNode findById(String id) {
        return document.findById(id);
    }

    /// The group holding a node, or null for the root.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    public GroupNode parentOf(ArtNode n) {
        return document.parentOf(n);
    }

    /// The transform from a node's own space to the document, its parents' transforms
    /// multiplied in.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    public Matrix toDocument(ArtNode n) {
        Matrix m = n.getTransform();
        GroupNode p = document.parentOf(n);
        while (p != null) {
            m = p.getTransform().multiply(m);
            p = document.parentOf(p);
        }
        return m;
    }

    // ---- operations ----

    /// Inserts a node.
    ///
    /// #### Parameters
    ///
    /// - `parent`: the group, or null for the root
    /// - `index`: the position among its children, or -1 for the top
    /// - `n`: the node
    public void insert(GroupNode parent, int index, ArtNode n) {
        checkpoint();
        GroupNode p = parent == null ? document.getRoot() : parent;
        if (index < 0 || index > p.getChildren().size()) {
            p.getChildren().add(n);
        } else {
            p.getChildren().add(index, n);
        }
        assignIds(n);
        fire(true);
    }

    /// Adds a node on top of the document, or of the selected node's group, and selects it.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    public void add(ArtNode n) {
        GroupNode parent = null;
        ArtNode sel = getSelected();
        if (sel instanceof GroupNode && sel.getMask() == null) {
            parent = (GroupNode) sel;
        } else if (sel != null) {
            parent = parentOf(sel);
        }
        int index = -1;
        if (parent != null && sel != null && sel != parent) { // NOPMD CompareObjectsWithEquals - node identity
            index = parent.getChildren().indexOf(sel) + 1;
        }
        insert(parent, index, n);
        select(n, false);
    }

    /// Removes a node.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    public void remove(ArtNode n) {
        GroupNode p = parentOf(n);
        if (p == null) {
            return;
        }
        checkpoint();
        p.getChildren().remove(n);
        pruneSelection();
        fire(true);
    }

    /// Removes the selected nodes.
    public void removeSelection() {
        List<ArtNode> s = getSelection();
        if (s.isEmpty()) {
            return;
        }
        checkpoint();
        for (int i = 0; i < s.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            GroupNode p = parentOf(s.get(i));
            if (p != null) {
                p.getChildren().remove(s.get(i));
            }
        }
        pruneSelection();
        fire(true);
    }

    /// Moves a node to another group or position.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `parent`: the new group, or null for the root
    /// - `index`: the position, or -1 for the top
    public void move(ArtNode n, GroupNode parent, int index) {
        GroupNode from = parentOf(n);
        GroupNode to = parent == null ? document.getRoot() : parent;
        if (from == null || n == to || n instanceof GroupNode && contains((GroupNode) n, to)) { // NOPMD CompareObjectsWithEquals - node identity
            return;
        }
        checkpoint();
        int old = from.getChildren().indexOf(n);
        from.getChildren().remove(n);
        int i = index;
        if (from == to && i > old) { // NOPMD CompareObjectsWithEquals - node identity
            i--;
        }
        if (i < 0 || i > to.getChildren().size()) {
            to.getChildren().add(n);
        } else {
            to.getChildren().add(i, n);
        }
        fire(true);
    }

    private static boolean contains(GroupNode g, ArtNode n) {
        for (int i = 0; i < g.getChildren().size(); i++) {
            ArtNode c = g.getChildren().get(i);
            if (c == n || c instanceof GroupNode && contains((GroupNode) c, n)) { // NOPMD CompareObjectsWithEquals - node identity
                return true;
            }
        }
        return false;
    }

    /// Moves a node one step up (toward the top) or down among its siblings.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `up`: true to bring it forward
    public void reorder(ArtNode n, boolean up) {
        GroupNode p = parentOf(n);
        if (p == null) {
            return;
        }
        int i = p.getChildren().indexOf(n);
        int j = up ? i + 1 : i - 1;
        if (j < 0 || j >= p.getChildren().size()) {
            return;
        }
        checkpoint();
        p.getChildren().remove(i);
        p.getChildren().add(j, n);
        fire(true);
    }

    /// Puts the nodes into a new group in place of the topmost of them, and selects it.
    ///
    /// #### Parameters
    ///
    /// - `nodes`: siblings or not; each keeps its place in the document visually
    public GroupNode group(List<ArtNode> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }
        checkpoint();
        ArtNode first = nodes.get(0);
        GroupNode parent = parentOf(first);
        if (parent == null) {
            return null;
        }
        Matrix parentToDoc = toDocument(parent);
        int at = parent.getChildren().indexOf(first);
        GroupNode g = new GroupNode();
        g.setName("Group");
        g.setPassThrough(true);
        for (ArtNode n : nodes) {
            GroupNode p = parentOf(n);
            if (p == null) {
                continue;
            }
            if (p == parent && p.getChildren().indexOf(n) < at) { // NOPMD CompareObjectsWithEquals - node identity
                at--;
            }
            Matrix inv = parentToDoc.invert();
            Matrix toDoc = toDocument(p);
            p.getChildren().remove(n);
            if (p != parent && inv != null) { // NOPMD CompareObjectsWithEquals - node identity
                n.setTransform(inv.multiply(toDoc).multiply(n.getTransform()));
            }
            g.add(n);
        }
        parent.getChildren().add(Math.max(0, Math.min(at, parent.getChildren().size())), g);
        assignIds(g);
        selection.clear();
        selection.add(g.getId());
        fire(true);
        return g;
    }

    /// Replaces a group by its children, which keep their place visually, and selects them.
    ///
    /// #### Parameters
    ///
    /// - `g`: the group
    public void ungroup(GroupNode g) {
        GroupNode p = parentOf(g);
        if (p == null) {
            return;
        }
        checkpoint();
        int at = p.getChildren().indexOf(g);
        p.getChildren().remove(at);
        selection.clear();
        List<ArtNode> ch = g.getChildren();
        for (int i = 0; i < ch.size(); i++) {
            ArtNode c = ch.get(i);
            c.setTransform(g.getTransform().multiply(c.getTransform()));
            if (g.getOpacity() < 1) {
                c.setOpacity(c.getOpacity() * g.getOpacity());
            }
            p.getChildren().add(at + i, c);
            selection.add(c.getId());
        }
        fire(true);
    }

    /// Sets a node's opacity.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `opacity`: 0 to 1
    public void setOpacity(ArtNode n, float opacity) {
        checkpoint();
        n.setOpacity(opacity);
        fire(true);
    }

    /// Shows or hides a node.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `visible`: true to show it
    public void setVisible(ArtNode n, boolean visible) {
        checkpoint();
        n.setVisible(visible);
        fire(true);
    }

    /// Locks or unlocks a node.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `locked`: true to lock it
    public void setLocked(ArtNode n, boolean locked) {
        checkpoint();
        n.setLocked(locked);
        fire(true);
    }

    /// Renames a node.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `name`: the name
    public void setName(ArtNode n, String name) {
        checkpoint();
        n.setName(name);
        fire(true);
    }

    /// Sets a node's blend mode.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `mode`: the mode
    public void setBlendMode(ArtNode n, BlendMode mode) {
        checkpoint();
        n.setBlendMode(mode);
        fire(true);
    }

    /// Sets a node's transform.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `t`: the transform, in its parent's space
    public void setTransform(ArtNode n, Matrix t) {
        checkpoint();
        n.setTransform(t);
        fire(true);
    }

    /// Moves the selected nodes by an offset in document units.
    ///
    /// #### Parameters
    ///
    /// - `dx`: x offset
    /// - `dy`: y offset
    public void translateSelection(double dx, double dy) {
        List<ArtNode> s = getSelection();
        if (s.isEmpty()) {
            return;
        }
        checkpoint();
        for (ArtNode n : s) {
            GroupNode p = parentOf(n);
            Matrix toDoc = p == null ? Matrix.IDENTITY : toDocument(p);
            Matrix inv = toDoc.invert();
            if (inv == null) {
                continue;
            }
            // An offset in the document is the same offset mapped into the parent's space.
            double ox = inv.mapX(dx, dy) - inv.mapX(0, 0);
            double oy = inv.mapY(dx, dy) - inv.mapY(0, 0);
            n.setTransform(Matrix.translate(ox, oy).multiply(n.getTransform()));
        }
        fire(true);
    }

    /// Replaces a shape's fills with one paint.
    ///
    /// #### Parameters
    ///
    /// - `s`: the shape
    /// - `paint`: the fill, or null for none
    public void setFill(ShapeNode s, ArtPaint paint) {
        checkpoint();
        s.getFills().clear();
        if (paint != null) {
            s.getFills().add(paint);
        }
        fire(true);
    }

    /// Replaces a shape's strokes with one stroke.
    ///
    /// #### Parameters
    ///
    /// - `s`: the shape
    /// - `stroke`: the stroke, or null for none
    public void setStroke(ShapeNode s, StrokeStyle stroke) {
        checkpoint();
        s.getStrokes().clear();
        if (stroke != null) {
            s.getStrokes().add(stroke);
        }
        fire(true);
    }

    /// Adds an effect to a node.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `e`: the effect
    public void addEffect(ArtNode n, Effect e) {
        checkpoint();
        n.getEffects().add(e);
        fire(true);
    }

    /// Removes an effect from a node.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `index`: the effect's position
    public void removeEffect(ArtNode n, int index) {
        if (index < 0 || index >= n.getEffects().size()) {
            return;
        }
        checkpoint();
        n.getEffects().remove(index);
        fire(true);
    }

    /// Sets a parameter of one of a node's effects.
    ///
    /// #### Parameters
    ///
    /// - `n`: the node
    /// - `index`: the effect's position
    /// - `param`: the parameter
    /// - `value`: its value
    public void setEffectParameter(ArtNode n, int index, String param, Object value) {
        if (index < 0 || index >= n.getEffects().size()) {
            return;
        }
        checkpoint();
        n.getEffects().get(index).set(param, value);
        fire(true);
    }

    /// Replaces a text node's characters, keeping the first run's style.
    ///
    /// #### Parameters
    ///
    /// - `t`: the text node
    /// - `text`: the characters
    public void setText(TextNode t, String text) {
        checkpoint();
        TextRun first = t.getRuns().isEmpty() ? new TextRun("", null, 24, new SolidPaint(0xff000000))
                : t.getRuns().get(0);
        first.setText(text);
        t.getRuns().clear();
        t.getRuns().add(first);
        fire(true);
    }
}

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
package com.codename1.ui.kanban;

import com.codename1.io.JSONParser;
import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanChangeListener;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanJson;
import com.codename1.kanban.KanbanLane;
import com.codename1.kanban.KanbanModel;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.ScrollSurface;
import com.codename1.ui.TextField;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;
import com.codename1.util.UndoHistory;
import com.codename1.util.UndoableEdit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A Kanban board: columns of cards, dragged from one stage to the next.
///
/// ```java
/// DefaultKanbanModel model = new DefaultKanbanModel();
/// model.addColumn(new KanbanColumn("todo", "To do"));
/// model.addColumn(new KanbanColumn("doing", "Doing").setWipLimit(3));
/// model.addColumn(new KanbanColumn("done", "Done"));
/// model.addCard("todo", new KanbanCard("c1", "Write the release notes").addTag("docs"));
///
/// KanbanBoard board = new KanbanBoard(model);
/// form.add(BorderLayout.CENTER, board);
/// ```
///
/// The whole board scrolls as one surface, both ways, under column titles
/// that stay in view. A card is dragged with a mouse, or after a long press
/// with a finger, which leaves a plain drag free to scroll; the other cards
/// make room where it would land. Column titles are dragged the same way to
/// reorder the columns, and the mark at the end of a title folds the column
/// to a strip. With `KanbanLane`s in the model the board has swimlanes, each
/// under a title that folds it.
///
/// A column shows how many cards it holds, against its limit when it has
/// one, and is marked when it is over. A model that enforces its limits
/// (`DefaultKanbanModel.setEnforceWipLimits`) shows the refusal while the
/// card is still held over the column.
///
/// On a narrow screen `setColumnSnap` brings scrolling to rest on a column.
/// A `KanbanCardFactory` puts a component in each card; it is drawn with the
/// card, and a tap on it reaches it.
///
/// The row under the cards of a column adds a card, typed in place, and a
/// double tap renames one. Every edit is offered to the `KanbanListener`s
/// and then to the model, and with `setUndoHistory` can be undone. With a
/// keyboard, Delete removes the selected card, and with Control or Command,
/// C, X, V and D copy, cut, paste and duplicate it, Z undoes and Shift-Z
/// redoes.
///
/// #### Styling
///
/// The UIIDs are `KanbanBoard`, `KanbanColumn`, `KanbanColumnHeader`,
/// `KanbanColumnOverLimit`, `KanbanCard`, `KanbanCardSelected`,
/// `KanbanCardTag`, `KanbanLane`, `KanbanAddCard` and `KanbanDropSlot`. A
/// theme that defines none of them gets defaults that follow its light or
/// dark mode.
public class KanbanBoard extends ScrollSurface {
    private KanbanModel model;
    private final List<KanbanListener> listeners = new ArrayList<KanbanListener>();
    private final KanbanChangeListener modelListener = new KanbanChangeListener() {
        @Override
        public void boardChanged(KanbanModel m) {
            modelChanged();
        }
    };
    private KanbanCardRenderer renderer = new DefaultKanbanCardRenderer();
    private KanbanCardFactory cardFactory;
    private final Map<String, Component> components = new HashMap<String, Component>();
    private UndoHistory undoHistory;
    private KanbanJson.UserDataCodec userDataCodec;
    private KanbanCard selected;
    private boolean editable = true;
    private boolean columnReorder = true;
    private boolean inlineAdd = true;
    private boolean inlineEditing = true;
    private boolean columnSnap;
    // scrolling coming to rest on a column
    private boolean snapPending;
    private int snapLastX;
    private Motion snapMotion;
    private float columnWidth = 42;
    private String addCardText = "+ Add card";
    private int idCounter;

    // the layout, in content coordinates; rebuilt when the model or the width changes
    private boolean dirty = true;
    private int laidWidth = -1;
    private List<KanbanColumn> cols = new ArrayList<KanbanColumn>();
    private List<KanbanLane> lanes = new ArrayList<KanbanLane>();
    private int[] colX = new int[0];
    private int[] colW = new int[0];
    private int[] laneTop = new int[0];
    private int[] laneBodyH = new int[0];
    private Cell[][] cells = new Cell[0][];
    private int contentW;
    private int contentH;
    private int headerH;
    private int bandH;
    private int pad;
    private int gap;
    private int addH;

    // a card being dragged
    private KanbanCard dragCard;
    private int[] dragFrom;
    private int grabX;
    private int grabY;
    private int dragW;
    private int dragH;
    private int dragX;
    private int dragY;
    private int dropCol = -1;
    private int dropLane = -1;
    private int dropIndex = -1;
    private boolean dropAllowed;
    private long slotSince;

    // a column being dragged
    private KanbanColumn dragColumn;
    private int dragColumnFrom;
    private int dragColumnTo;

    // the field a title is typed in
    private TextField editor;
    private Container editorHolder;
    private KanbanCard editCard;
    private KanbanColumn editColumn;
    private KanbanLane editLane;

    private final int[] rectBuf = new int[4];
    private final int[] cmpBuf = new int[4];

    /// The cards of one column in one lane, and where each is.
    private static final class Cell {
        List<KanbanCard> cards;
        // the top of each card under the top of the lane's body, and its height
        int[] y;
        int[] h;
        int addY;
        int height;
    }

    /// An empty board backed by a new `DefaultKanbanModel`.
    public KanbanBoard() {
        this(new DefaultKanbanModel());
    }

    /// A board showing `model`.
    public KanbanBoard(KanbanModel model) {
        setUIID("KanbanBoard");
        setFocusable(true);
        setModel(model);
    }

    // ------------------------------------------------------------------ model

    /// The model shown.
    public KanbanModel getModel() {
        return model;
    }

    /// Shows another model.
    public void setModel(KanbanModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model is null");
        }
        endEdit(false);
        if (this.model != null) {
            this.model.removeChangeListener(modelListener);
        }
        this.model = model;
        model.addChangeListener(modelListener);
        selected = null;
        components.clear();
        modelChanged();
    }

    /// The model changed: lay the board out again. Called by the model's
    /// listener, and by an application that changed a card behind the
    /// model's back, such as its tags.
    public void modelChanged() {
        dirty = true;
        if (selected != null && model.getCard(selected.getId()) != selected) { // NOPMD CompareObjectsWithEquals - the card of the model itself
            selected = null;
            fireSelection();
        }
        syncComponents();
        refreshLayout();
        repaint();
    }

    /// Paints the cards. Never null.
    public KanbanCardRenderer getCardRenderer() {
        return renderer;
    }

    /// Sets the renderer of the cards.
    public void setCardRenderer(KanbanCardRenderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer is null");
        }
        this.renderer = renderer;
        modelChanged();
    }

    /// Makes the components inside the cards, or null.
    public KanbanCardFactory getCardFactory() {
        return cardFactory;
    }

    /// Puts a real component inside each card; null, the default, for cards
    /// that are only painted.
    public void setCardFactory(KanbanCardFactory cardFactory) {
        components.clear();
        this.cardFactory = cardFactory;
        modelChanged();
    }

    /// The component the factory made for a card, or null.
    public Component getCardComponent(KanbanCard card) {
        return components.get(card.getId());
    }

    /// The height, in pixels, the component of a card takes at the bottom of
    /// it; 0 for a card without one.
    public int getCardComponentHeight(KanbanCard card) {
        Component c = components.get(card.getId());
        return c == null ? 0 : c.getPreferredH() + KanbanLook.mm(1.5f);
    }

    /// Adds a listener.
    public void addKanbanListener(KanbanListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /// Removes a listener.
    public void removeKanbanListener(KanbanListener l) {
        listeners.remove(l);
    }

    // ---------------------------------------------------------------- options

    /// True when the user can move, add, rename and remove cards.
    public boolean isEditingEnabled() {
        return editable;
    }

    /// Lets the user edit the board, the default, or only look at it. A
    /// board that is not editable still folds its columns and lanes.
    public void setEditingEnabled(boolean editable) {
        this.editable = editable;
        endEdit(false);
        modelChanged();
    }

    /// True when the columns can be dragged into another order.
    public boolean isColumnReorderEnabled() {
        return columnReorder;
    }

    /// Lets the user drag a column by its title to another place; on by
    /// default.
    public void setColumnReorderEnabled(boolean columnReorder) {
        this.columnReorder = columnReorder;
    }

    /// True when every column ends in a row that adds a card.
    public boolean isInlineAdd() {
        return inlineAdd;
    }

    /// Shows a row under the cards of each column that adds a card typed in
    /// place; on by default.
    public void setInlineAdd(boolean inlineAdd) {
        this.inlineAdd = inlineAdd;
        modelChanged();
    }

    /// True when a double tap renames a card in place.
    public boolean isInlineEditing() {
        return inlineEditing;
    }

    /// Makes a double tap on a card rename it in place, the default, or
    /// only tell the listeners the card was opened.
    public void setInlineEditing(boolean inlineEditing) {
        this.inlineEditing = inlineEditing;
    }

    /// True when scrolling comes to rest on a column.
    public boolean isColumnSnap() {
        return columnSnap;
    }

    /// Makes scrolling across the board come to rest with a column at its
    /// leading edge, so a narrow screen shows one whole column at a time
    /// and not the halves of two. Off by default.
    public void setColumnSnap(boolean columnSnap) {
        this.columnSnap = columnSnap;
        if (!columnSnap) {
            snapPending = false;
            snapMotion = null;
        }
    }

    /// Where scrolling across the board rests nearest to `x`: the start of a
    /// column, or the end of the board.
    private int snapTarget(int x) {
        int max = getMaxOffsetX();
        int best = Math.max(0, Math.min(x, max));
        int dist = Integer.MAX_VALUE;
        for (int c = 0; c <= colX.length; c++) {
            int at = c == colX.length ? max : Math.max(0, Math.min(colX[c] - gap, max));
            if (Math.abs(at - x) < dist) {
                dist = Math.abs(at - x);
                best = at;
            }
        }
        return best;
    }

    // Called every frame after a release: waits for the fling to stop, then
    // eases to the column.
    private boolean animateSnap() {
        if (!snapPending) {
            return false;
        }
        if (snapMotion == null) {
            int x = getOffsetX();
            if (x != snapLastX) {
                snapLastX = x;
                return true;
            }
            int to = snapTarget(x);
            if (to == x) {
                snapPending = false;
                return false;
            }
            snapMotion = Motion.createEaseInOutMotion(x, to, 180);
            snapMotion.start();
            return true;
        }
        scrollTo(snapMotion.getValue(), getOffsetY());
        if (snapMotion.isFinished()) {
            snapMotion = null;
            snapPending = false;
            return false;
        }
        return true;
    }

    /// The text of the row that adds a card.
    public String getAddCardText() {
        return addCardText;
    }

    /// Sets the text of the row that adds a card, for another language.
    public void setAddCardText(String addCardText) {
        this.addCardText = addCardText;
        repaint();
    }

    /// The width of a column, in millimetres.
    public float getColumnWidth() {
        return columnWidth;
    }

    /// Sets the width of a column, in millimetres; 42 by default. On a
    /// screen narrower than that a column takes the screen, less a strip
    /// that shows there is a next one.
    public void setColumnWidth(float millimetres) {
        columnWidth = Math.max(15, millimetres);
        modelChanged();
    }

    // -------------------------------------------------------------- selection

    /// The selected card, or null.
    public KanbanCard getSelectedCard() {
        return selected;
    }

    /// Selects a card, or with null none.
    public void select(KanbanCard card) {
        if (card == selected) { // NOPMD CompareObjectsWithEquals - the same card
            return;
        }
        selected = card;
        fireSelection();
    }

    private void fireSelection() {
        repaint();
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
            listeners.get(i).selectionChanged(this);
        }
    }

    /// Scrolls so a column is at the leading edge of the view.
    public void scrollToColumn(KanbanColumn column) {
        ensureLayout();
        int c = cols.indexOf(column);
        if (c >= 0) {
            scrollTo(colX[c] - gap, getOffsetY());
        }
    }

    /// Scrolls so a card is in view.
    public void scrollToCard(KanbanCard card) {
        ensureLayout();
        int[] at = locate(card);
        if (at == null || !cardRect(at[0], at[1], at[2], rectBuf)) {
            return;
        }
        int ox = getOffsetX();
        int oy = getOffsetY();
        int bw = getBodyWidth();
        int bh = getBodyHeight();
        if (rectBuf[0] < ox) {
            ox = rectBuf[0] - gap;
        } else if (rectBuf[0] + rectBuf[2] > ox + bw) {
            ox = rectBuf[0] + rectBuf[2] + gap - bw;
        }
        if (rectBuf[1] < oy) {
            oy = rectBuf[1] - gap;
        } else if (rectBuf[1] + rectBuf[3] > oy + bh) {
            oy = rectBuf[1] + rectBuf[3] + gap - bh;
        }
        scrollTo(ox, oy);
    }

    // ------------------------------------------------------------------ edits

    /// The history edits are recorded in, or null.
    public UndoHistory getUndoHistory() {
        return undoHistory;
    }

    /// Records every edit the user makes in a history, so it can be undone.
    /// One history may serve several components. Null, the default, records
    /// nothing. Changes the application makes to the model directly are not
    /// recorded.
    public void setUndoHistory(UndoHistory undoHistory) {
        this.undoHistory = undoHistory;
    }

    /// Takes back the last edit in the history.
    ///
    /// #### Returns
    ///
    /// false when there is no history or nothing to undo
    public boolean undo() {
        endEdit(true);
        return undoHistory != null && undoHistory.undo();
    }

    /// Makes the last undone edit again.
    ///
    /// #### Returns
    ///
    /// false when there is no history or nothing to redo
    public boolean redo() {
        endEdit(true);
        return undoHistory != null && undoHistory.redo();
    }

    private void record(UndoableEdit edit) {
        if (undoHistory != null) {
            undoHistory.record(edit);
        }
    }

    private KanbanEdits.Place placeOf(KanbanCard card) {
        ensureLayout();
        int[] at = locate(card);
        if (at == null) {
            return null;
        }
        return new KanbanEdits.Place(cols.get(at[1]), lanes.isEmpty() ? null : lanes.get(at[0]), at[2]);
    }

    /// Moves a card to an index of a cell, as a drop there does: offered to
    /// the listeners, then to the model, and recorded for undo. `lane` is
    /// null on a board without lanes. The index counts the cell without the
    /// card.
    ///
    /// #### Returns
    ///
    /// false when a listener or the model refused
    public boolean moveCard(KanbanCard card, KanbanColumn column, KanbanLane lane, int index) {
        KanbanEdits.Place from = placeOf(card);
        if (from == null || column == null) {
            return false;
        }
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
            if (!listeners.get(i).cardMoving(this, card, column, lane, index)) {
                return false;
            }
        }
        if (!model.moveCard(card, column, lane, index)) {
            return false;
        }
        record(new KanbanEdits.Move(model, card, from, placeOf(card)));
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
            listeners.get(i).cardMoved(this, card);
        }
        return true;
    }

    /// Moves a column to an index, counted without it, as dragging its title
    /// does.
    ///
    /// #### Returns
    ///
    /// false when a listener or the model refused
    public boolean moveColumn(KanbanColumn column, int index) {
        int from = model.getColumns().indexOf(column);
        if (from < 0 || from == index) {
            return false;
        }
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
            if (!listeners.get(i).columnMoving(this, column, index)) {
                return false;
            }
        }
        if (!model.moveColumn(column, index)) {
            return false;
        }
        record(new KanbanEdits.ColumnMove(model, column, from, index));
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
            listeners.get(i).columnMoved(this, column);
        }
        return true;
    }

    private String freeCardId() {
        String id;
        do {
            idCounter++;
            id = "card" + idCounter;
        } while (model.getCard(id) != null);
        return id;
    }

    private boolean insert(KanbanCard card, KanbanColumn column, KanbanLane lane, int index, String name) {
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
            if (!listeners.get(i).cardAdding(this, card, column, lane)) {
                return false;
            }
        }
        if (!model.insertCard(card, column, lane, index)) {
            return false;
        }
        KanbanEdits.Place at = placeOf(card);
        if (at != null) {
            record(new KanbanEdits.Presence(model, card, at, false, name));
        }
        select(card);
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
            listeners.get(i).cardAdded(this, card);
        }
        return true;
    }

    /// Adds a card with a title at the end of a cell, as typing it into the
    /// row under the column does: offered to the listeners, then to the
    /// model, recorded for undo and selected. `lane` is null on a board
    /// without lanes.
    ///
    /// #### Returns
    ///
    /// the card, or null when it was refused
    public KanbanCard addCard(KanbanColumn column, KanbanLane lane, String title) {
        KanbanCard card = new KanbanCard(freeCardId(), title);
        if (lane != null) {
            card.setLaneId(lane.getId());
        }
        return insert(card, column, lane, Integer.MAX_VALUE, "Add card") ? card : null;
    }

    /// Gives a card another title, as renaming it in place does.
    ///
    /// #### Returns
    ///
    /// false when a listener or the model refused
    public boolean renameCard(KanbanCard card, String title) {
        String old = card.getTitle();
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
            if (!listeners.get(i).cardRenaming(this, card, title)) {
                return false;
            }
        }
        if (!model.renameCard(card, title)) {
            return false;
        }
        record(new KanbanEdits.Rename(model, card, old, title));
        return true;
    }

    /// Removes a card, as Delete on it does.
    ///
    /// #### Returns
    ///
    /// false when a listener or the model refused
    public boolean removeCard(KanbanCard card) {
        KanbanEdits.Place at = placeOf(card);
        if (at == null) {
            return false;
        }
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
            if (!listeners.get(i).cardRemoving(this, card)) {
                return false;
            }
        }
        if (!model.removeCard(card)) {
            return false;
        }
        record(new KanbanEdits.Presence(model, card, at, true, "Remove card"));
        return true;
    }

    /// Adds a copy of the selected card under it.
    ///
    /// #### Returns
    ///
    /// false when no card is selected or the copy was refused
    public boolean duplicateSelection() {
        if (!editable || selected == null) {
            return false;
        }
        KanbanEdits.Place at = placeOf(selected);
        return at != null && insert(selected.copy(freeCardId()), at.column, at.lane, at.index + 1, "Duplicate card");
    }

    /// Writes the user data of a copied card to the clipboard and reads it
    /// back on paste; without one, a pasted card has no user data.
    public void setUserDataCodec(KanbanJson.UserDataCodec codec) {
        userDataCodec = codec;
    }

    /// Puts the selected card on the clipboard, as JSON.
    ///
    /// #### Returns
    ///
    /// false when no card is selected
    public boolean copySelection() {
        if (selected == null) {
            return false;
        }
        List<KanbanCard> one = new ArrayList<KanbanCard>();
        one.add(selected);
        Map<String, Object> m = KanbanJson.cardsToMap(one, userDataCodec);
        m.put("cn1kanban", Integer.valueOf(1));
        Display.getInstance().copyToClipboard(JSONParser.toJson(m));
        return true;
    }

    /// Copies the selected card and removes it.
    ///
    /// #### Returns
    ///
    /// false when no card is selected or it could not be removed
    public boolean cutSelection() {
        KanbanCard card = selected;
        return editable && copySelection() && removeCard(card);
    }

    /// Adds the card on the clipboard under the selected card, or at the
    /// end of the first column when none is selected.
    ///
    /// #### Returns
    ///
    /// false when the clipboard holds no card or it was refused
    public boolean paste() {
        if (!editable || model.getColumns().isEmpty()) {
            return false;
        }
        Object o = Display.getInstance().getPasteDataFromClipboard();
        if (!(o instanceof String) || ((String) o).indexOf("\"cn1kanban\"") < 0) {
            return false;
        }
        List<KanbanCard> read;
        try {
            read = KanbanJson.readCards(JSONParser.parseJSON((String) o), userDataCodec);
        } catch (IOException notCards) {
            return false;
        }
        KanbanEdits.Place at = selected == null ? null : placeOf(selected);
        KanbanColumn column = at == null ? model.getColumns().get(0) : at.column;
        KanbanLane lane = at == null ? model.getLanes().isEmpty() ? null : model.getLanes().get(0) : at.lane;
        int index = at == null ? Integer.MAX_VALUE : at.index + 1;
        boolean any = false;
        for (KanbanCard source : read) {
            KanbanCard c = source.copy(freeCardId());
            if (insert(c, column, lane, index, "Paste card")) {
                any = true;
                if (index != Integer.MAX_VALUE) {
                    index++;
                }
            }
        }
        return any;
    }

    private void toggleColumn(KanbanColumn column) {
        boolean to = !column.isCollapsed();
        endEdit(true);
        if (model.setCollapsed(column, to)) {
            record(new KanbanEdits.Fold(model, column, null, to));
            for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
                listeners.get(i).columnToggled(this, column);
            }
        }
    }

    private void toggleLane(KanbanLane lane) {
        boolean to = !lane.isCollapsed();
        endEdit(true);
        if (model.setLaneCollapsed(lane, to)) {
            record(new KanbanEdits.Fold(model, null, lane, to));
            for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
                listeners.get(i).laneToggled(this, lane);
            }
        }
    }

    // ---------------------------------------------------------- typing titles

    /// True while a title is being typed in place.
    @Override
    public boolean isEditing() {
        return editor != null;
    }

    /// Renames a card in place, in a text field over it; the title goes to
    /// `renameCard` when editing ends.
    public void startCardEdit(KanbanCard card) {
        endEdit(true);
        if (!editable || placeOf(card) == null) {
            return;
        }
        editCard = card;
        scrollToCard(card);
        showEditor(card.getTitle());
    }

    /// Opens the row that adds a card to a cell for typing; the card is
    /// added by `addCard` when editing ends with a title in it.
    public void startAddCard(KanbanColumn column, KanbanLane lane) {
        endEdit(true);
        if (!editable || column == null || column.isCollapsed()) {
            return;
        }
        editColumn = column;
        editLane = lane;
        showEditor("");
    }

    /// Ends the typing of a title and applies it.
    public void finishEditing() {
        endEdit(true);
    }

    // The field, for tests.
    TextField editor() {
        return editor;
    }

    private void showEditor(String text) {
        TopLevelContainer top = getTopLevelContainer();
        if (top == null) {
            // not on a form: there is nowhere to type
            editCard = null;
            editColumn = null;
            editLane = null;
            return;
        }
        ensureLayout();
        stopFling();
        final TextField tf = new TextField(text == null ? "" : text);
        editor = tf;
        ActionListener<ActionEvent> done = new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (editor == tf) { // NOPMD CompareObjectsWithEquals - the field itself
                    endEdit(true);
                }
            }
        };
        tf.addActionListener(done);
        tf.setDoneListener(done);
        tf.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(Component cmp) {
            }

            @Override
            public void focusLost(Component cmp) {
                if (editor == tf) { // NOPMD CompareObjectsWithEquals - the field itself
                    endEdit(true);
                }
            }
        });
        // over the board, in the form's layered pane: the board has no children
        Container pane = top.getFormLayeredPane(KanbanBoard.class, true);
        editorHolder = new EditorLayer(new Layout() {
            @Override
            public void layoutContainer(Container parent) {
                if (editorRect(rectBuf)) {
                    tf.setX(getAbsoluteX() + rectBuf[0] - parent.getAbsoluteX());
                    tf.setY(getAbsoluteY() + rectBuf[1] - parent.getAbsoluteY());
                    tf.setWidth(rectBuf[2]);
                    tf.setHeight(rectBuf[3]);
                } else {
                    tf.setWidth(0);
                    tf.setHeight(0);
                }
            }

            @Override
            public Dimension getPreferredSize(Container parent) {
                return new Dimension(0, 0);
            }
        }, tf);
        editorHolder.add(tf);
        pane.add(editorHolder);
        pane.revalidate();
        repaint();
        tf.startEditingAsync();
    }

    private void endEdit(boolean apply) {
        TextField tf = editor;
        if (tf == null) {
            return;
        }
        editor = null;
        KanbanCard card = editCard;
        KanbanColumn column = editColumn;
        KanbanLane lane = editLane;
        editCard = null;
        editColumn = null;
        editLane = null;
        String text = tf.getText();
        Container holder = editorHolder;
        editorHolder = null;
        if (tf.isEditing()) {
            tf.stopEditing(null);
        }
        if (holder != null) {
            Container pane = holder.getParent();
            holder.remove();
            if (pane != null) {
                pane.revalidate();
            }
        }
        text = text == null ? "" : text.trim();
        if (apply && text.length() > 0) {
            if (card != null) {
                if (!text.equals(card.getTitle()) && model.getCard(card.getId()) == card) { // NOPMD CompareObjectsWithEquals - the card of the model itself
                    renameCard(card, text);
                }
            } else if (column != null && model.getColumns().contains(column)) {
                addCard(column, lane, text);
            }
        }
        repaint();
    }

    // ----------------------------------------------------------------- layout

    private void ensureLayout() {
        // the width a column takes follows the board's: an answer laid out
        // for another width is not one
        if (dirty || getWidth() != laidWidth) {
            refreshLayout();
        }
    }

    private void relayout(int width) {
        dirty = false;
        laidWidth = width;
        cols = new ArrayList<KanbanColumn>(model.getColumns());
        lanes = new ArrayList<KanbanLane>(model.getLanes());
        pad = KanbanLook.mm(1.5f);
        gap = KanbanLook.mm(1.5f);
        headerH = KanbanLook.style("KanbanColumnHeader").getFont().getHeight() + KanbanLook.mm(3.5f);
        bandH = lanes.isEmpty() ? 0 : KanbanLook.style("KanbanLane").getFont().getHeight() + KanbanLook.mm(2);
        addH = inlineAdd && editable ? KanbanLook.style("KanbanAddCard").getFont().getHeight() + KanbanLook.mm(2.5f) : 0;
        int full = Math.max(KanbanLook.mm(15), Math.min(KanbanLook.mm(columnWidth), width - KanbanLook.mm(7)));
        int folded = KanbanLook.mm(7);
        int n = cols.size();
        colX = new int[n];
        colW = new int[n];
        int x = gap;
        for (int c = 0; c < n; c++) {
            colX[c] = x;
            colW[c] = cols.get(c).isCollapsed() ? folded : full;
            x += colW[c] + gap;
        }
        contentW = x;
        int cardW = full - pad * 2;
        int laneCount = Math.max(1, lanes.size());
        laneTop = new int[laneCount];
        laneBodyH = new int[laneCount];
        cells = new Cell[laneCount][n];
        int y = 0;
        for (int l = 0; l < laneCount; l++) {
            KanbanLane lane = lanes.isEmpty() ? null : lanes.get(l);
            boolean foldedLane = lane != null && lane.isCollapsed();
            laneTop[l] = y;
            y += bandH;
            int bodyH = 0;
            for (int c = 0; c < n; c++) {
                KanbanColumn col = cols.get(c);
                Cell cell = new Cell();
                cell.cards = new ArrayList<KanbanCard>(model.getCards(col, lane));
                int count = cell.cards.size();
                cell.y = new int[count];
                cell.h = new int[count];
                int cy = pad;
                if (!foldedLane && !col.isCollapsed()) {
                    for (int i = 0; i < count; i++) {
                        KanbanCard card = cell.cards.get(i);
                        cell.y[i] = cy;
                        cell.h[i] = renderer.getCardHeight(this, card, cardW) + getCardComponentHeight(card);
                        cy += cell.h[i] + gap;
                    }
                }
                cell.addY = cy;
                cell.height = cy + addH + pad;
                bodyH = Math.max(bodyH, cell.height);
                cells[l][c] = cell;
            }
            laneBodyH[l] = foldedLane ? 0 : Math.max(bodyH, KanbanLook.mm(10));
            y += laneBodyH[l];
        }
        contentH = y + gap;
        placeEditor();
    }

    private void syncComponents() {
        if (cardFactory == null) {
            return;
        }
        List<String> gone = new ArrayList<String>(components.keySet());
        List<KanbanColumn> columns = model.getColumns();
        List<KanbanLane> ls = model.getLanes();
        for (KanbanColumn col : columns) {
            for (int l = 0; l < Math.max(1, ls.size()); l++) {
                List<KanbanCard> cards = model.getCards(col, ls.isEmpty() ? null : ls.get(l));
                for (KanbanCard card : cards) {
                    gone.remove(card.getId());
                    if (!components.containsKey(card.getId())) {
                        Component cmp = cardFactory.createComponent(this, card);
                        components.put(card.getId(), cmp);
                    }
                }
            }
        }
        for (String goneId : gone) {
            components.remove(goneId);
        }
    }

    /// Where a card is in the layout: its lane, column and index, or null.
    private int[] locate(KanbanCard card) {
        for (int l = 0; l < cells.length; l++) {
            for (int c = 0; c < cells[l].length; c++) {
                int i = cells[l][c].cards.indexOf(card);
                if (i >= 0) {
                    return new int[]{l, c, i};
                }
            }
        }
        return null;
    }

    private boolean cellShown(int l, int c) {
        return !cols.get(c).isCollapsed() && (lanes.isEmpty() || !lanes.get(l).isCollapsed());
    }

    /// The rectangle of a card in the content; false when its column or its
    /// lane is folded.
    private boolean cardRect(int l, int c, int i, int[] out) {
        if (!cellShown(l, c)) {
            return false;
        }
        Cell cell = cells[l][c];
        out[0] = colX[c] + pad;
        out[1] = laneTop[l] + bandH + cell.y[i];
        out[2] = colW[c] - pad * 2;
        out[3] = cell.h[i];
        return true;
    }

    /// The rectangle of a card in the board's own coordinates, as it is laid
    /// out now, or null when it is not shown. For a popup placed by a card,
    /// and for tests.
    public int[] getCardBounds(KanbanCard card) {
        ensureLayout();
        int[] at = locate(card);
        int[] r = new int[4];
        if (at == null || !cardRect(at[0], at[1], at[2], r)) {
            return null;
        }
        r[0] -= getOffsetX();
        r[1] += headerH - getOffsetY();
        return r;
    }

    /// The rectangle of a column's title in the board's own coordinates, or
    /// null for a column that is not in the model.
    public int[] getColumnHeaderBounds(KanbanColumn column) {
        ensureLayout();
        int c = cols.indexOf(column);
        return c < 0 ? null : new int[]{colX[c] - getOffsetX(), 0, colW[c], headerH};
    }

    private int columnAt(int cx) {
        for (int c = 0; c < colX.length; c++) {
            if (cx >= colX[c] && cx < colX[c] + colW[c]) {
                return c;
            }
        }
        return -1;
    }

    private int nearestColumn(int cx) {
        int best = -1;
        int bestD = Integer.MAX_VALUE;
        for (int c = 0; c < colX.length; c++) {
            int d = Math.abs(colX[c] + colW[c] / 2 - cx);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    private int laneAt(int cy) {
        for (int l = laneTop.length - 1; l >= 0; l--) {
            if (cy >= laneTop[l]) {
                return l;
            }
        }
        return 0;
    }

    private KanbanCard cardAt(int cx, int cy) {
        int c = columnAt(cx);
        if (c < 0 || cy < 0 || cells.length == 0) {
            return null;
        }
        int l = laneAt(cy);
        if (!cellShown(l, c)) {
            return null;
        }
        Cell cell = cells[l][c];
        int ry = cy - laneTop[l] - bandH;
        if (cx < colX[c] + pad || cx >= colX[c] + colW[c] - pad) {
            return null;
        }
        for (int i = 0; i < cell.y.length; i++) {
            if (ry >= cell.y[i] && ry < cell.y[i] + cell.h[i]) {
                return cell.cards.get(i);
            }
        }
        return null;
    }

    // ----------------------------------------------------------------- gestures

    private boolean beginDrag(int x, int y, ScrollSurface.EditTrigger trigger) {
        if (trigger == ScrollSurface.EditTrigger.HANDLE_DRAG) {
            // a finger scrolls; it drags after a long press
            return false;
        }
        endEdit(true);
        int cx = x + getOffsetX();
        if (y < headerH) {
            int c = columnAt(cx);
            if (c < 0 || !columnReorder || !editable || cols.size() < 2) {
                return false;
            }
            dragColumn = cols.get(c);
            dragColumnFrom = c;
            dragColumnTo = c;
            grabX = cx - colX[c];
            dragX = x;
            return true;
        }
        if (!editable) {
            return false;
        }
        int cy = y - headerH + getOffsetY();
        KanbanCard card = cardAt(cx, cy);
        int[] at = card == null ? null : locate(card);
        if (at == null || !cardRect(at[0], at[1], at[2], rectBuf)) {
            return false;
        }
        dragCard = card;
        dragFrom = at;
        grabX = cx - rectBuf[0];
        grabY = cy - rectBuf[1];
        dragW = rectBuf[2];
        dragH = rectBuf[3];
        dropLane = at[0];
        dropCol = at[1];
        dropIndex = at[2];
        dropAllowed = true;
        // the gap is where the card was: nothing has to open
        slotSince = 0;
        select(card);
        placeEditor();
        return true;
    }

    private void dragTo(int x, int y) {
        dragX = x;
        dragY = y;
        int cx = x + getOffsetX();
        if (dragColumn != null) {
            int to = 0;
            for (int c = 0; c < cols.size(); c++) {
                if (c != dragColumnFrom && colX[c] + colW[c] / 2 < cx) {
                    to++;
                }
            }
            dragColumnTo = to;
            return;
        }
        if (dragCard == null) {
            return;
        }
        int cy = y - headerH + getOffsetY();
        int midX = cx - grabX + dragW / 2;
        int midY = cy - grabY + dragH / 2;
        int c = nearestColumn(midX);
        int l = laneAt(midY);
        Cell cell = cells[l][c];
        int index = 0;
        if (cols.get(c).isCollapsed()) {
            index = cell.cards.size() - (cell.cards.contains(dragCard) ? 1 : 0);
        } else {
            int ry = laneTop[l] + bandH + pad;
            for (int i = 0; i < cell.cards.size(); i++) {
                if (cell.cards.get(i) == dragCard) { // NOPMD CompareObjectsWithEquals - the dragged card itself
                    continue;
                }
                if (midY > ry + cell.h[i] / 2) {
                    index++;
                }
                ry += cell.h[i] + gap;
            }
        }
        if (c != dropCol || l != dropLane || index != dropIndex) {
            dropCol = c;
            dropLane = l;
            dropIndex = index;
            slotSince = currentTime();
            requestAnimation();
        }
        KanbanLane lane = lanes.isEmpty() ? null : lanes.get(l);
        dropAllowed = (lane == null || !lane.isCollapsed()) && model.canMoveCard(dragCard, cols.get(c), lane);
    }

    private void endDrag(boolean committed) {
        if (dragColumn != null) {
            KanbanColumn column = dragColumn;
            dragColumn = null;
            if (committed && dragColumnTo != dragColumnFrom) {
                moveColumn(column, dragColumnTo);
            }
            repaint();
            return;
        }
        KanbanCard card = dragCard;
        if (card == null) {
            return;
        }
        dragCard = null;
        int[] from = dragFrom;
        dragFrom = null;
        int gx = dragX + getOffsetX() - grabX;
        int gy = dragY - headerH + getOffsetY() - grabY;
        boolean elsewhere = dropCol != from[1] || dropLane != from[0] || dropIndex != from[2];
        if (committed && dropAllowed && elsewhere && dropCol >= 0) {
            moveCard(card, cols.get(dropCol), lanes.isEmpty() ? null : lanes.get(dropLane), dropIndex);
        }
        dropCol = -1;
        dropLane = -1;
        dropIndex = -1;
        // from where it was let go, the card glides to where it now belongs
        startSettle(card.getId(), gx, gy, dragW, dragH);
        dirty = true;
        refreshLayout();
        placeEditor();
        repaint();
    }

    private void tap(int x, int y, boolean twice) {
        int cx = x + getOffsetX();
        if (y < headerH) {
            int c = columnAt(cx);
            if (c >= 0 && (cols.get(c).isCollapsed() || cx >= colX[c] + colW[c] - KanbanLook.mm(7))) {
                toggleColumn(cols.get(c));
            }
            return;
        }
        int cy = y - headerH + getOffsetY();
        if (cy < 0 || cy >= contentH || cells.length == 0) {
            select(null);
            return;
        }
        int l = laneAt(cy);
        if (!lanes.isEmpty() && cy < laneTop[l] + bandH) {
            toggleLane(lanes.get(l));
            return;
        }
        int c = columnAt(cx);
        if (c >= 0 && cols.get(c).isCollapsed()) {
            toggleColumn(cols.get(c));
            return;
        }
        KanbanCard card = cardAt(cx, cy);
        if (card != null) {
            select(card);
            if (tapComponent(card, x, y)) {
                return;
            }
            if (twice) {
                if (inlineEditing && editable) {
                    startCardEdit(card);
                } else {
                    fireOpened(card);
                }
            }
            return;
        }
        if (c >= 0 && addH > 0 && cellShown(l, c)) {
            int ry = cy - laneTop[l] - bandH;
            Cell cell = cells[l][c];
            if (ry >= cell.addY && ry < cell.addY + addH) {
                startAddCard(cols.get(c), lanes.isEmpty() ? null : lanes.get(l));
                return;
            }
        }
        select(null);
    }

    private void fireOpened(KanbanCard card) {
        for (int i = 0; i < listeners.size(); i++) { // NOPMD ForLoopCanBeForeach - a listener may remove itself
            listeners.get(i).cardOpened(this, card);
        }
    }

    // A key reached the board; true when it was a command of it.
    boolean keyTyped(int keyCode) {
        if (editor != null) {
            return false;
        }
        if (keyCode == 127 || keyCode == 8) {
            return editable && selected != null && removeCard(selected);
        }
        if (keyCode == 10 || keyCode == 13) {
            if (selected == null) {
                return false;
            }
            fireOpened(selected);
            return true;
        }
        Display d = Display.getInstance();
        if (!d.isControlKeyDown() && !d.isMetaKeyDown()) {
            return false;
        }
        switch (keyCode) {
            case 'c':
            case 'C':
                return copySelection();
            case 'x':
            case 'X':
                return cutSelection();
            case 'v':
            case 'V':
                return paste();
            case 'd':
            case 'D':
                return duplicateSelection();
            case 'z':
            case 'Z':
                return d.isShiftKeyDown() ? redo() : undo();
            case 'y':
            case 'Y':
                return redo();
            default:
                return false;
        }
    }

    // --------------------------------------------------------------- painting

    private static int alpha(int base, int amount) {
        return base * amount / 255;
    }

    private void paintBoard(Graphics g, int x, int y) {
        Style bs = KanbanLook.style("KanbanBoard");
        Style cs = KanbanLook.style("KanbanColumn");
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipW = g.getClipWidth();
        int clipH = g.getClipHeight();
        int boardBg = KanbanLook.bg(bs, 0xf7f7fa);
        g.setColor(boardBg);
        g.fillRect(clipX, clipY, clipW, clipH);
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        int arc = KanbanLook.mm(2);
        int bottom = Math.max(y + contentH - gap, clipY + clipH - gap);
        int colBg = KanbanLook.bg(cs, KanbanLook.mix(boardBg, bs.getFgColor(), 16));
        int a = g.getAlpha();
        for (int c = 0; c < colX.length; c++) {
            int cx = x + colX[c];
            if (cx > clipX + clipW || cx + colW[c] < clipX) {
                continue;
            }
            boolean refused = dragCard != null && !dropAllowed && c == dropCol;
            g.setColor(refused ? KanbanLook.bg(KanbanLook.style("KanbanColumnOverLimit"), colBg) : colBg);
            g.setAlpha(cols.get(c) == dragColumn ? alpha(a, 110) : a); // NOPMD CompareObjectsWithEquals - the dragged column itself
            // the title above is the rounded top
            g.fillRoundRect(cx, y - arc, colW[c], bottom - y + arc, arc, arc);
            g.setAlpha(a);
            if (cols.get(c).isCollapsed()) {
                // a lane's title bar crosses the strip, so the title starts under the first one
                int titleTop = Math.max(y, clipY);
                if (!lanes.isEmpty() && laneTop.length > 0) {
                    titleTop = Math.max(titleTop, y + laneTop[0] + bandH);
                }
                paintFoldedTitle(g, cols.get(c), cx, titleTop, colW[c]);
            }
        }
        for (int l = 0; l < cells.length; l++) {
            KanbanLane lane = lanes.isEmpty() ? null : lanes.get(l);
            int top = y + laneTop[l];
            if (top > clipY + clipH || top + bandH + laneBodyH[l] < clipY) {
                continue;
            }
            for (int c = 0; c < colX.length; c++) {
                int cx = x + colX[c];
                if (cellShown(l, c) && cx <= clipX + clipW && cx + colW[c] >= clipX) {
                    paintCell(g, l, c, cx, top + bandH, clipY, clipY + clipH);
                }
            }
            if (lane != null) {
                paintBand(g, lane, l, clipX, top, clipW);
            }
        }
        g.setAntiAliased(aa);
    }

    private void paintFoldedTitle(Graphics g, KanbanColumn column, int x, int y, int w) {
        String title = column.getTitle();
        if (title == null || !g.isAffineSupported()) {
            return;
        }
        Style hs = KanbanLook.style("KanbanColumnHeader");
        Font f = hs.getFont();
        g.setFont(f);
        g.setColor(hs.getFgColor());
        // reads down the strip
        int px = x + w / 2;
        int py = y + pad;
        g.rotateRadians((float) (Math.PI / 2), px, py);
        g.drawString(title, px, py - f.getHeight() / 2);
        g.rotateRadians((float) (-Math.PI / 2), px, py);
    }

    private void paintBand(Graphics g, KanbanLane lane, int l, int x, int y, int w) {
        Style ls = KanbanLook.style("KanbanLane");
        Font f = ls.getFont();
        g.setColor(KanbanLook.bg(ls, 0xe1dce8));
        g.fillRect(x, y, w, bandH);
        g.setColor(ls.getFgColor());
        int mark = Math.max(3, f.getHeight() / 4);
        int mx = x + pad + mark;
        int my = y + bandH / 2;
        if (lane.isCollapsed()) {
            g.fillTriangle(mx - mark / 2, my - mark, mx - mark / 2, my + mark, mx + mark / 2 + 1, my);
        } else {
            g.fillTriangle(mx - mark, my - mark / 2, mx + mark, my - mark / 2, mx, my + mark / 2 + 1);
        }
        int count = 0;
        for (int c = 0; c < cells[l].length; c++) {
            count += cells[l][c].cards.size();
        }
        String title = lane.getTitle() == null ? "" : lane.getTitle();
        g.setFont(f);
        String text = KanbanLook.fit(title + "  " + count, f, w - mark * 3 - pad * 3);
        g.drawString(text, mx + mark + pad, y + (bandH - f.getHeight()) / 2);
    }

    private void paintCell(Graphics g, int l, int c, int x, int top, int clipTop, int clipBottom) {
        Cell cell = cells[l][c];
        int cx = x + pad;
        int w = colW[c] - pad * 2;
        boolean dropHere = dragCard != null && l == dropLane && c == dropCol;
        int slot = 0;
        if (dropHere) {
            long t = slotSince == 0 ? 120 : currentTime() - slotSince;
            float level = t >= 120 ? 1f : Math.max(0f, t / 120f);
            slot = Math.round((dragH + gap) * level);
        }
        int ry = top + pad;
        int index = 0;
        int count = cell.cards.size();
        for (int i = 0; i <= count; i++) {
            KanbanCard card = i < count ? cell.cards.get(i) : null;
            if (card != null && card == dragCard) { // NOPMD CompareObjectsWithEquals - the dragged card itself
                continue;
            }
            if (dropHere && index == dropIndex) {
                paintSlot(g, cx, ry, w, slot - gap);
                ry += slot;
            }
            if (card == null) {
                break;
            }
            int h = cell.h[i];
            rectBuf[0] = cx;
            rectBuf[1] = ry;
            rectBuf[2] = w;
            rectBuf[3] = h;
            // the settle works in the content's coordinates
            int ox = x - colX[c];
            int oy = top - bandH - laneTop[l];
            rectBuf[0] -= ox;
            rectBuf[1] -= oy;
            applySettle(card.getId(), rectBuf);
            rectBuf[0] += ox;
            rectBuf[1] += oy;
            if (rectBuf[1] < clipBottom && rectBuf[1] + rectBuf[3] > clipTop) {
                renderer.paintCard(g, this, card, rectBuf[0], rectBuf[1], rectBuf[2], rectBuf[3],
                        card == selected, 0f); // NOPMD CompareObjectsWithEquals - the selected card itself
                paintCardComponent(g, card, rectBuf[0], rectBuf[1], rectBuf[2], rectBuf[3]);
            }
            ry += h + gap;
            index++;
        }
        if (addH > 0 && !(editor != null && editCard == null && cols.get(c) == editColumn // NOPMD CompareObjectsWithEquals - the cell being typed in
                && (lanes.isEmpty() || lanes.get(l) == editLane))) { // NOPMD CompareObjectsWithEquals - the lane being typed in
            Style as = KanbanLook.style("KanbanAddCard");
            Font f = as.getFont();
            g.setFont(f);
            g.setColor(as.getFgColor());
            g.drawString(KanbanLook.fit(addCardText, f, w - pad), cx + pad / 2, ry + (addH - f.getHeight()) / 2);
        }
    }

    private void paintSlot(Graphics g, int x, int y, int w, int h) {
        if (h <= 2) {
            return;
        }
        Style s = KanbanLook.style(dropAllowed ? "KanbanDropSlot" : "KanbanColumnOverLimit");
        int arc = KanbanLook.mm(1.6f);
        g.setColor(KanbanLook.bg(s, 0xd3e3fd));
        g.fillRoundRect(x, y, w, h, arc, arc);
        g.setColor(s.getFgColor());
        g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
    }

    private void paintHeader(Graphics g, int x, int y) {
        Style bs = KanbanLook.style("KanbanBoard");
        Style cs = KanbanLook.style("KanbanColumn");
        Style hs = KanbanLook.style("KanbanColumnHeader");
        Style os = KanbanLook.style("KanbanColumnOverLimit");
        int boardBg = KanbanLook.bg(bs, 0xf7f7fa);
        g.setColor(boardBg);
        g.fillRect(g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight());
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        int arc = KanbanLook.mm(2);
        int colBg = KanbanLook.bg(cs, KanbanLook.mix(boardBg, bs.getFgColor(), 16));
        int a = g.getAlpha();
        for (int c = 0; c < colX.length; c++) {
            int cx = x + colX[c];
            if (cx > g.getClipX() + g.getClipWidth() || cx + colW[c] < g.getClipX()) {
                continue;
            }
            g.setAlpha(cols.get(c) == dragColumn ? alpha(a, 110) : a); // NOPMD CompareObjectsWithEquals - the dragged column itself
            paintColumnTitle(g, cols.get(c), cx, y, colW[c], colBg, arc, hs, os);
            g.setAlpha(a);
        }
        g.setAntiAliased(aa);
    }

    private void paintColumnTitle(Graphics g, KanbanColumn column, int x, int y, int w, int colBg, int arc,
            Style hs, Style os) {
        int top = y + gap / 2;
        g.setColor(colBg);
        g.fillRoundRect(x, top, w, headerH + arc, arc, arc);
        Font f = hs.getFont();
        int count = model.getCardCount(column);
        int limit = column.getWipLimit();
        boolean over = limit > 0 && count > limit;
        String counted = limit > 0 ? count + "/" + limit : String.valueOf(count);
        int ty = top + (headerH - gap / 2 - f.getHeight()) / 2;
        g.setFont(f);
        int cw = f.stringWidth(counted);
        if (column.isCollapsed()) {
            g.setColor(over ? os.getFgColor() : hs.getFgColor());
            g.drawString(String.valueOf(count), x + (w - f.stringWidth(String.valueOf(count))) / 2, ty);
            return;
        }
        // the mark that folds the column, then the count, from the trailing edge
        int mark = Math.max(3, f.getHeight() / 4);
        int mx = x + w - pad - mark;
        int my = top + (headerH - gap / 2) / 2;
        g.setColor(KanbanLook.mix(colBg, hs.getFgColor(), 150));
        g.fillTriangle(mx + mark / 2, my - mark, mx + mark / 2, my + mark, mx - mark / 2 - 1, my);
        int right = mx - mark - pad;
        if (over) {
            int pillPad = Math.max(2, pad / 2);
            g.setColor(KanbanLook.bg(os, 0xf9dedc));
            g.fillRoundRect(right - cw - pillPad * 2, ty, cw + pillPad * 2, f.getHeight(), f.getHeight(), f.getHeight());
            g.setColor(os.getFgColor());
            g.drawString(counted, right - cw - pillPad, ty);
            right -= cw + pillPad * 2 + pad;
        } else {
            g.setColor(KanbanLook.mix(colBg, hs.getFgColor(), 150));
            g.drawString(counted, right - cw, ty);
            right -= cw + pad;
        }
        g.setColor(hs.getFgColor());
        g.drawString(KanbanLook.fit(column.getTitle(), f, right - x - pad), x + pad, ty);
    }

    private void paintDrag(Graphics g, int x, int y) {
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        if (dragCard != null) {
            renderer.paintCard(g, this, dragCard, x + dragX - grabX, y + dragY - grabY, dragW, dragH, true,
                    getLiftLevel());
            paintCardComponent(g, dragCard, x + dragX - grabX, y + dragY - grabY, dragW, dragH);
        } else if (dragColumn != null) {
            Style cs = KanbanLook.style("KanbanColumn");
            Style hs = KanbanLook.style("KanbanColumnHeader");
            int arc = KanbanLook.mm(2);
            int w = colW[dragColumnFrom];
            int gx = x + dragX - grabX;
            KanbanLook.shadow(g, gx, y + gap / 2, w, headerH, arc, 1.5f);
            int cx = g.getClipX();
            int cy = g.getClipY();
            int cw = g.getClipWidth();
            int ch = g.getClipHeight();
            g.clipRect(gx - arc * 2, y, w + arc * 4, headerH + gap);
            paintColumnTitle(g, dragColumn, gx, y, w, KanbanLook.bg(cs, 0xece9f1), arc, hs,
                    KanbanLook.style("KanbanColumnOverLimit"));
            g.setClip(cx, cy, cw, ch);
            // where it would land
            int at = 0;
            int lineX = gap / 2;
            for (int c = 0; c < cols.size(); c++) {
                if (c == dragColumnFrom) {
                    continue;
                }
                if (at == dragColumnTo) {
                    break;
                }
                lineX = colX[c] + colW[c] + gap / 2;
                at++;
            }
            g.setColor(KanbanLook.style("KanbanCardSelected").getFgColor());
            int lw = Math.max(2, KanbanLook.mm(0.5f));
            g.fillRect(x + lineX - getOffsetX() - lw / 2, y, lw, getHeight());
        }
        g.setAntiAliased(aa);
    }

    /// Paints the whole board, every column and card of it, into a new
    /// image no larger than the given size, without the selection. For a
    /// thumbnail, a share sheet or a printout.
    public Image toImage(int maxWidth, int maxHeight) {
        endEdit(true);
        ensureLayout();
        int w = Math.max(1, contentW);
        int h = Math.max(1, headerH + contentH);
        Image img = Image.createImage(w, h, 0xffffffff);
        Graphics g = img.getGraphics();
        KanbanCard sel = selected;
        selected = null;
        g.setClip(0, 0, w, headerH);
        paintHeader(g, 0, 0);
        g.setClip(0, headerH, w, h - headerH);
        paintBoard(g, 0, headerH);
        selected = sel;
        if (w <= maxWidth && h <= maxHeight) {
            return img;
        }
        float ratio = Math.min(maxWidth / (float) w, maxHeight / (float) h);
        return img.scaled(Math.max(1, Math.round(w * ratio)), Math.max(1, Math.round(h * ratio)));
    }

    /// Where the field a title is typed in goes, in the board's own
    /// coordinates; false while its card or its cell is not shown.
    private boolean editorRect(int[] out) {
        if (editor == null) {
            return false;
        }
        int ph = editor.getPreferredH();
        int ox = getOffsetX();
        int oy = getOffsetY() - headerH;
        if (editCard != null) {
            int[] at = locate(editCard);
            if (at == null || !cardRect(at[0], at[1], at[2], out)) {
                return false;
            }
            out[0] -= ox;
            out[1] -= oy;
            out[3] = Math.max(ph, Math.min(out[3], ph * 2));
        } else {
            int c = cols.indexOf(editColumn);
            int l = editLane == null ? 0 : lanes.indexOf(editLane);
            if (c < 0 || l < 0 || l >= cells.length || !cellShown(l, c)) {
                return false;
            }
            out[0] = colX[c] + pad - ox;
            out[1] = laneTop[l] + bandH + cells[l][c].addY - oy;
            out[2] = colW[c] - pad * 2;
            out[3] = ph;
        }
        // the pinned titles cannot clip a field: under them it hides
        return out[1] >= headerH && out[1] < getHeight();
    }

    private void placeEditor() {
        if (editorHolder != null) {
            editorHolder.revalidate();
        }
    }

    /// Where the component of a card is drawn, given the card's rectangle:
    /// along its bottom edge.
    private boolean componentRect(Component cmp, int cardX, int cardY, int cardW, int cardH, int[] out) {
        int ph = cmp.getPreferredH();
        out[0] = cardX + pad;
        out[1] = cardY + cardH - ph - pad;
        out[2] = Math.max(0, cardW - pad * 2);
        out[3] = ph;
        return out[2] > 0 && ph > 0;
    }

    /// Draws the component of a card. It is never added to the form: like a
    /// renderer's, it is given its bounds and painted.
    private void paintCardComponent(Graphics g, KanbanCard card, int cardX, int cardY, int cardW, int cardH) {
        Component cmp = components.get(card.getId());
        if (cmp == null || !componentRect(cmp, cardX, cardY, cardW, cardH, cmpBuf)) {
            return;
        }
        place(cmp, cmpBuf[0], cmpBuf[1], cmpBuf[2], cmpBuf[3]);
        cmp.paintComponent(g);
    }

    private static void place(Component cmp, int x, int y, int w, int h) {
        cmp.setX(x);
        cmp.setY(y);
        cmp.setWidth(w);
        cmp.setHeight(h);
        if (cmp instanceof Container) {
            ((Container) cmp).layoutContainer();
        }
    }

    /// Hands a tap to the component of a card when it lands on it: the
    /// component is only drawn, so the board delivers the press and the
    /// release itself.
    private boolean tapComponent(KanbanCard card, int x, int y) {
        Component cmp = components.get(card.getId());
        int[] r = cmp == null ? null : getCardBounds(card);
        if (r == null || !componentRect(cmp, r[0], r[1], r[2], r[3], cmpBuf)) {
            return false;
        }
        if (x < cmpBuf[0] || x >= cmpBuf[0] + cmpBuf[2] || y < cmpBuf[1] || y >= cmpBuf[1] + cmpBuf[3]) {
            return false;
        }
        int ax = getAbsoluteX() + x;
        int ay = getAbsoluteY() + y;
        place(cmp, getAbsoluteX() + cmpBuf[0], getAbsoluteY() + cmpBuf[1], cmpBuf[2], cmpBuf[3]);
        Component target = cmp;
        if (cmp instanceof Container) {
            target = ((Container) cmp).getComponentAt(ax, ay);
        }
        if (target == null || !target.isEnabled()) {
            return false;
        }
        target.pointerPressed(ax, ay);
        target.pointerReleased(ax, ay);
        repaint();
        return true;
    }

    // The layer the title field is laid in. It covers the form, so only the
    // field itself takes the pointer: a tap anywhere else reaches what is
    // under it.
    private static final class EditorLayer extends Container {
        private final Component field;

        EditorLayer(Layout layout, Component field) {
            super(layout);
            this.field = field;
        }

        @Override
        public boolean contains(int x, int y) {
            return field.contains(x, y);
        }
    }

    // ---------------------------------------------------------- the surface

    @Override
    protected void layoutSurface(int width, int height) {
        if (dirty || width != laidWidth) {
            relayout(width);
        }
        setHeaderSize(0, headerH);
    }

    @Override
    protected int getContentWidth() {
        return contentW;
    }

    @Override
    protected int getContentHeight() {
        return contentH;
    }

    @Override
    protected void paintContent(Graphics g, int x, int y) {
        paintBoard(g, x, y);
    }

    @Override
    protected void paintTopHeader(Graphics g, int x, int y) {
        paintHeader(g, x, y);
    }

    @Override
    protected void paintOverlay(Graphics g, int x, int y) {
        if (dragCard != null || dragColumn != null) {
            paintDrag(g, x, y);
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(KanbanLook.mm(60), KanbanLook.mm(60));
    }

    @Override
    protected void scrollChanged() {
        placeEditor();
    }

    @Override
    public void pointerReleased(int x, int y) {
        boolean edit = isEditInProgress();
        super.pointerReleased(x, y);
        if (columnSnap && !edit && getMaxOffsetX() > 0) {
            snapPending = true;
            snapMotion = null;
            // one frame off, so the first frame only watches the fling
            snapLastX = getOffsetX() - 1;
            requestAnimation();
        }
    }

    @Override
    protected void pressStarted(int x, int y) {
        snapPending = false;
        snapMotion = null;
        if (!hasFocus() && getComponentForm() != null) {
            // keys: Delete, and copy, paste and undo with Ctrl or Cmd
            requestFocus();
        }
    }

    @Override
    protected boolean editStarted(int x, int y, EditTrigger trigger) {
        return beginDrag(x, y, trigger);
    }

    @Override
    protected void editMoved(int x, int y) {
        dragTo(x, y);
    }

    @Override
    protected void editEnded(boolean committed) {
        endDrag(committed);
    }

    @Override
    protected void tapped(int x, int y, boolean doubleTap) {
        tap(x, y, doubleTap);
    }

    @Override
    protected boolean animateSurface() {
        boolean snapping = animateSnap();
        // the gap a held card opens
        return snapping || dragCard != null && slotSince != 0 && currentTime() - slotSince < 120;
    }

    @Override
    protected void deinitialize() {
        endEdit(true);
        super.deinitialize();
    }

    @Override
    public void keyPressed(int keyCode) {
        if (!keyTyped(keyCode)) {
            super.keyPressed(keyCode);
        }
    }
}

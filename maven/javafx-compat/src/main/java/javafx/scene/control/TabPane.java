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
package javafx.scene.control;

import java.util.ArrayList;
import java.util.List;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.ui.Component;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/// Pages of which one shows at a time, chosen from a strip of tab
/// headers.
///
/// #### How it is built
///
/// There is no native component. Every tab has two regions among the
/// children of the tab pane: a header in the strip, holding the tab's
/// graphic, a `Label` with its text and, where the tab can be closed, a
/// `Label` to close it; and a content region holding the tab's content
/// node, visible only while the tab is selected. The content of every tab
/// is therefore in the scene graph all the time, as in JavaFX, and
/// `lookup` finds it whether it shows or not. The content region fills
/// the pane next to the strip and sizes its content to itself.
///
/// Pressing a header selects its tab. Pressing the close label fires
/// `Tab.TAB_CLOSE_REQUEST_EVENT` at the tab and, unless a handler
/// consumed it, removes the tab and fires `Tab.CLOSED_EVENT`.
///
/// #### Differences from JavaFX
///
/// - On the left and right sides the headers are stacked upright, not
///   rotated.
/// - Headers that do not fit the strip are cut off; there is no menu of
///   hidden tabs, and tabs cannot be dragged to reorder them.
/// - The tab size limits, `rotateGraphic` and the drag policy are not
///   part of this layer.
///
/// The pseudo-classes `top`, `right`, `bottom` and `left` follow the
/// side.
public class TabPane extends Control {

    /// When the user can close tabs.
    public enum TabClosingPolicy {
        /// Only the selected tab shows a way to close it.
        SELECTED_TAB,
        /// Every closable tab shows a way to close it.
        ALL_TABS,
        /// No tab can be closed by the user.
        UNAVAILABLE
    }

    /// The style class of a tab pane drawn without a frame.
    public static final String STYLE_CLASS_FLOATING = "floating";

    private static final int MODEL = Dirty.USER;
    private static final int SIDE = Dirty.USER << 1;
    private static final int HEADERS = Dirty.USER << 2;
    private static final double PAD = 6;
    private static final double GAP = 4;
    private static final Color STRIP = Color.gray(0.88);
    private static final Color TAB = Color.gray(0.8);
    private static final Color CHOSEN = Color.WHITE;
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass[] SIDES = {PseudoClass.getPseudoClass("top"),
        PseudoClass.getPseudoClass("right"), PseudoClass.getPseudoClass("bottom"),
        PseudoClass.getPseudoClass("left")};

    private final ObservableList<Tab> tabs = FXCollections.observableArrayList();
    private final ObjectProperty<SingleSelectionModel<Tab>> selectionModel =
            new FxObject<SingleSelectionModel<Tab>>(this, "selectionModel", null, MODEL);
    private final ObjectProperty<Side> side = new FxObject<Side>(this, "side", Side.TOP, SIDE | Dirty.LAYOUT);
    private final ObjectProperty<TabClosingPolicy> tabClosingPolicy = new FxObject<TabClosingPolicy>(this,
            "tabClosingPolicy", TabClosingPolicy.SELECTED_TAB, HEADERS);
    private final ArrayList<Page> pages = new ArrayList<Page>();
    private final ChangeListener<Tab> selectionListener = new ChangeListener<Tab>() {
        @Override
        public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
            selectionChanged();
        }
    };
    private SingleSelectionModel<Tab> watched;
    private double stripX;
    private double stripY;
    private double stripWidth;
    private double stripHeight;

    /// Creates a tab pane without tabs.
    public TabPane() {
        this((Tab[]) null);
    }

    /// Creates a tab pane with tabs.
    public TabPane(Tab... tabs) {
        getStyleClass().add("tab-pane");
        pseudoClassStateChanged(SIDES[0], true);
        this.tabs.addListener(new ListChangeListener<Tab>() {
            @Override
            public void onChanged(Change<? extends Tab> change) {
                tabsChanged(change);
            }
        });
        disabledProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                    Boolean newValue) {
                for (int i = 0; i < TabPane.this.tabs.size(); i++) {
                    TabPane.this.tabs.get(i).updateDisabled();
                }
            }
        });
        selectionModel.set(new TabSelectionModel(this.tabs));
        if (tabs != null) {
            this.tabs.addAll(tabs);
        }
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & MODEL) != 0) {
            if (watched != null) {
                watched.selectedItemProperty().removeListener(selectionListener);
            }
            watched = selectionModel.get();
            if (watched != null) {
                watched.selectedItemProperty().addListener(selectionListener);
            }
            selectionChanged();
        }
        if ((what & SIDE) != 0) {
            Side s = getSide();
            pseudoClassStateChanged(SIDES[0], s == Side.TOP);
            pseudoClassStateChanged(SIDES[1], s == Side.RIGHT);
            pseudoClassStateChanged(SIDES[2], s == Side.BOTTOM);
            pseudoClassStateChanged(SIDES[3], s == Side.LEFT);
        }
        if ((what & HEADERS) != 0) {
            for (int i = 0; i < pages.size(); i++) {
                pages.get(i).refresh();
            }
            requestLayout();
        }
        super.cn1Invalidated(what);
    }

    // ---------------------------------------------------------------- tabs

    private Page pageOf(Tab tab) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).tab == tab) {
                return pages.get(i);
            }
        }
        return null;
    }

    private void tabsChanged(ListChangeListener.Change<? extends Tab> change) {
        SingleSelectionModel<Tab> model = getSelectionModel();
        Tab chosen = model == null ? null : model.getSelectedItem();
        int chosenIndex = model == null ? -1 : model.getSelectedIndex();
        while (change.next()) {
            List<? extends Tab> removed = change.getRemoved();
            for (int i = 0; i < removed.size(); i++) {
                Tab t = removed.get(i);
                if (t != null && !tabs.contains(t) && t.getTabPane() == this) {
                    t.setSelected(false);
                    t.setTabPane(null);
                }
            }
            List<? extends Tab> added = change.getAddedSubList();
            for (int i = 0; i < added.size(); i++) {
                Tab t = added.get(i);
                if (t == null) {
                    throw new NullPointerException("A tab must not be null");
                }
                t.setTabPane(this);
            }
        }
        ArrayList<Page> fresh = new ArrayList<Page>();
        ArrayList<Node> children = new ArrayList<Node>();
        for (int i = 0; i < tabs.size(); i++) {
            Page page = pageOf(tabs.get(i));
            if (page == null) {
                page = new Page(tabs.get(i));
            }
            fresh.add(page);
            children.add(page.body);
        }
        for (int i = 0; i < fresh.size(); i++) {
            children.add(fresh.get(i).header);
        }
        pages.clear();
        pages.addAll(fresh);
        cn1Children().setAll(children);
        if (model != null) {
            if (tabs.isEmpty()) {
                model.clearSelection();
            } else if (chosen != null && tabs.contains(chosen)) {
                model.select(tabs.indexOf(chosen));
            } else {
                // The tab that takes the place of a removed selection, or
                // the first tab of a pane that had none.
                int next = Math.max(0, Math.min(chosenIndex, tabs.size() - 1));
                model.clearSelection();
                model.select(next);
            }
        }
        selectionChanged();
        requestLayout();
    }

    private void selectionChanged() {
        SingleSelectionModel<Tab> model = getSelectionModel();
        Tab chosen = model == null ? null : model.getSelectedItem();
        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            if (t != chosen) {
                t.setSelected(false);
            }
        }
        if (chosen != null && tabs.contains(chosen)) {
            chosen.setSelected(true);
        }
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).refresh();
        }
        requestLayout();
    }

    /// A property of a tab of this pane changed.
    final void tabChanged(Tab tab) {
        Page page = pageOf(tab);
        if (page != null) {
            page.refresh();
            requestLayout();
        }
    }

    private void requestClose(Tab tab) {
        Event request = new Event(tab, tab, Tab.TAB_CLOSE_REQUEST_EVENT);
        Event.fireEvent(tab, request);
        if (!request.isConsumed() && tabs.remove(tab)) {
            Event.fireEvent(tab, new Event(tab, tab, Tab.CLOSED_EVENT));
        }
    }

    // -------------------------------------------------------------- layout

    private boolean upright() {
        Side s = getSide();
        return s == Side.LEFT || s == Side.RIGHT;
    }

    private double stripThickness() {
        boolean upright = upright();
        double most = 0;
        for (int i = 0; i < pages.size(); i++) {
            Region header = pages.get(i).header;
            most = Math.max(most, upright ? header.prefWidth(-1) : header.prefHeight(-1));
        }
        return snapSizeY(most);
    }

    private double stripLength() {
        boolean upright = upright();
        double total = 0;
        for (int i = 0; i < pages.size(); i++) {
            Region header = pages.get(i).header;
            total += upright ? header.prefHeight(-1) : header.prefWidth(-1);
        }
        return total;
    }

    private double bodySize(boolean width, boolean min) {
        double most = 0;
        for (int i = 0; i < pages.size(); i++) {
            Region body = pages.get(i).body;
            double size;
            if (min) {
                size = width ? body.minWidth(-1) : body.minHeight(-1);
            } else {
                size = width ? body.prefWidth(-1) : body.prefHeight(-1);
            }
            most = Math.max(most, size);
        }
        return most;
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double x = in.getLeft();
        double y = in.getTop();
        double w = Math.max(0, getWidth() - in.getLeft() - in.getRight());
        double h = Math.max(0, getHeight() - in.getTop() - in.getBottom());
        Side s = getSide();
        boolean upright = upright();
        double thick = Math.min(stripThickness(), upright ? w : h);
        double bodyX = x;
        double bodyY = y;
        double bodyW = w;
        double bodyH = h;
        if (upright) {
            stripX = s == Side.LEFT ? x : x + w - thick;
            stripY = y;
            stripWidth = thick;
            stripHeight = h;
            bodyW = w - thick;
            bodyX = s == Side.LEFT ? x + thick : x;
        } else {
            stripX = x;
            stripY = s == Side.BOTTOM ? y + h - thick : y;
            stripWidth = w;
            stripHeight = thick;
            bodyH = h - thick;
            bodyY = s == Side.BOTTOM ? y : y + thick;
        }
        double at = 0;
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            if (upright) {
                double length = snapSizeY(page.header.prefHeight(-1));
                page.header.resizeRelocate(stripX, stripY + at, thick, length);
                at += length;
            } else {
                double length = snapSizeX(page.header.prefWidth(-1));
                page.header.resizeRelocate(stripX + at, stripY, length, thick);
                at += length;
            }
            page.body.resizeRelocate(bodyX, bodyY, bodyW, bodyH);
        }
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        super.cn1Paint(renderer);
        if (!pages.isEmpty()) {
            renderer.fillRect(stripX, stripY, stripWidth, stripHeight, STRIP);
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        double inner = upright() ? stripThickness() + bodySize(true, false)
                : Math.max(stripLength(), bodySize(true, false));
        return in.getLeft() + inner + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        double inner = upright() ? Math.max(stripLength(), bodySize(false, false))
                : stripThickness() + bodySize(false, false);
        return in.getTop() + inner + in.getBottom();
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        double inner = upright() ? stripThickness() + bodySize(true, true) : bodySize(true, true);
        return in.getLeft() + inner + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        double inner = upright() ? bodySize(false, true) : stripThickness() + bodySize(false, true);
        return in.getTop() + inner + in.getBottom();
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    // ------------------------------------------------------------- the API

    /// Returns the tabs, in the order of their headers.
    public final ObservableList<Tab> getTabs() {
        return tabs;
    }

    /// Sets the selection model.
    public final void setSelectionModel(SingleSelectionModel<Tab> value) {
        selectionModel.set(value);
    }

    /// Returns the selection model: which tab shows.
    public final SingleSelectionModel<Tab> getSelectionModel() {
        return selectionModel.get();
    }

    /// The selection model.
    public final ObjectProperty<SingleSelectionModel<Tab>> selectionModelProperty() {
        return selectionModel;
    }

    /// Sets the side the headers are on.
    public final void setSide(Side value) {
        side.set(value);
    }

    /// Returns the side the headers are on.
    public final Side getSide() {
        Side s = side.get();
        return s == null ? Side.TOP : s;
    }

    /// The side the headers are on.
    public final ObjectProperty<Side> sideProperty() {
        return side;
    }

    /// Sets when the user can close tabs.
    public final void setTabClosingPolicy(TabClosingPolicy value) {
        tabClosingPolicy.set(value);
    }

    /// Returns when the user can close tabs.
    public final TabClosingPolicy getTabClosingPolicy() {
        TabClosingPolicy p = tabClosingPolicy.get();
        return p == null ? TabClosingPolicy.SELECTED_TAB : p;
    }

    /// When the user can close tabs.
    public final ObjectProperty<TabClosingPolicy> tabClosingPolicyProperty() {
        return tabClosingPolicy;
    }

    // ------------------------------------------------------------- helpers

    /// The selection of a tab pane: one of its tabs.
    private static final class TabSelectionModel extends SingleSelectionModel<Tab> {

        private final ObservableList<Tab> tabs;

        TabSelectionModel(ObservableList<Tab> tabs) {
            this.tabs = tabs;
        }

        @Override
        protected Tab getModelItem(int index) {
            return index < 0 || index >= tabs.size() ? null : tabs.get(index);
        }

        @Override
        protected int getItemCount() {
            return tabs.size();
        }
    }

    /// The two regions of one tab: its header and its content region.
    private final class Page {

        final Tab tab;
        final Header header;
        final Body body;

        Page(Tab tab) {
            this.tab = tab;
            this.header = new Header();
            this.body = new Body();
            final Tab subject = tab;
            header.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    SingleSelectionModel<Tab> model = getSelectionModel();
                    if (model != null && !subject.isDisabled()) {
                        model.select(subject);
                    }
                }
            });
            header.close.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    event.consume();
                }
            });
            header.close.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    event.consume();
                    if (!subject.isDisabled()) {
                        requestClose(subject);
                    }
                }
            });
            refresh();
        }

        void refresh() {
            boolean chosen = tab.isSelected();
            TabClosingPolicy policy = getTabClosingPolicy();
            boolean closes = tab.isClosable() && policy != TabClosingPolicy.UNAVAILABLE
                    && (policy == TabClosingPolicy.ALL_TABS || chosen);
            header.show(tab.getGraphic(), tab.getText(), closes, chosen);
            header.setDisable(tab.isDisable());
            body.show(tab.getContent());
            body.setVisible(chosen);
            body.setDisable(tab.isDisable());
        }
    }

    /// The header of a tab in the strip.
    private static final class Header extends Region {

        private final Label title = new Label();
        private final Label close = new Label("x");

        Header() {
            getStyleClass().add("tab");
            close.getStyleClass().add("tab-close-button");
        }

        void show(Node graphic, String text, boolean closes, boolean chosen) {
            title.setText(text == null ? "" : text);
            ArrayList<Node> wanted = new ArrayList<Node>();
            if (graphic != null) {
                wanted.add(graphic);
            }
            wanted.add(title);
            if (closes) {
                wanted.add(close);
            }
            if (!wanted.equals(cn1Children())) {
                cn1Children().setAll(wanted);
            }
            pseudoClassStateChanged(SELECTED, chosen);
            setBackground(new Background(new BackgroundFill(chosen ? CHOSEN : TAB, CornerRadii.EMPTY,
                    new Insets(0, 1, 0, 0))));
        }

        private static double width(Node n) {
            return n.isResizable() ? n.prefWidth(-1) : n.getLayoutBounds().getWidth();
        }

        private static double height(Node n) {
            return n.isResizable() ? n.prefHeight(-1) : n.getLayoutBounds().getHeight();
        }

        @Override
        protected void layoutChildren() {
            List<Node> children = getChildrenUnmodifiable();
            double x = PAD;
            for (int i = 0; i < children.size(); i++) {
                Node n = children.get(i);
                double w = width(n);
                layoutInArea(n, x, 0, w, getHeight(), 0, HPos.LEFT, VPos.CENTER);
                x += w + GAP;
            }
        }

        @Override
        protected double computePrefWidth(double height) {
            List<Node> children = getChildrenUnmodifiable();
            double total = 2 * PAD;
            for (int i = 0; i < children.size(); i++) {
                total += width(children.get(i)) + (i > 0 ? GAP : 0);
            }
            return total;
        }

        @Override
        protected double computePrefHeight(double width) {
            List<Node> children = getChildrenUnmodifiable();
            double most = 0;
            for (int i = 0; i < children.size(); i++) {
                most = Math.max(most, height(children.get(i)));
            }
            return most + PAD;
        }
    }

    /// The region that holds the content of one tab.
    private static final class Body extends Region {

        void show(Node node) {
            if (node == null) {
                if (!cn1Children().isEmpty()) {
                    cn1Children().clear();
                }
            } else if (cn1Children().size() != 1 || cn1Children().get(0) != node) {
                cn1Children().setAll(node);
            }
        }

        @Override
        protected void layoutChildren() {
            List<Node> children = getChildrenUnmodifiable();
            for (int i = 0; i < children.size(); i++) {
                layoutInArea(children.get(i), 0, 0, getWidth(), getHeight(), 0, HPos.CENTER, VPos.CENTER);
            }
        }

        @Override
        protected double computeMinWidth(double height) {
            List<Node> children = getChildrenUnmodifiable();
            return children.isEmpty() ? 0 : children.get(0).minWidth(-1);
        }

        @Override
        protected double computeMinHeight(double width) {
            List<Node> children = getChildrenUnmodifiable();
            return children.isEmpty() ? 0 : children.get(0).minHeight(-1);
        }
    }
}

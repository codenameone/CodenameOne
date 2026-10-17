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
import java.util.HashMap;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;

/// One item of a tree: a value, an optional graphic, and the items under
/// it.
///
/// An item with no children is a leaf. An item with children is open or
/// closed by [#setExpanded(boolean)], and its children are shown by a
/// [TreeView] while it and every item above it is open.
///
/// Changes are told as [TreeModificationEvent]s to the handlers of the
/// item and of every item above it, which is how a tree view hears of
/// them: the value, the graphic, the children, and a branch opening or
/// closing. The count events of JavaFX
/// (`expandedItemCountChangeEvent`) are told as
/// [#treeNotificationEvent()].
public class TreeItem<T> implements EventTarget {

    private static final EventType<Event> TREE_NOTIFICATION = new EventType<Event>(Event.ANY,
            "TreeNotificationEvent");
    private static final EventType<Event> VALUE_CHANGED = new EventType<Event>(TREE_NOTIFICATION,
            "ValueChangedEvent");
    private static final EventType<Event> GRAPHIC_CHANGED = new EventType<Event>(TREE_NOTIFICATION,
            "GraphicChangedEvent");
    private static final EventType<Event> CHILDREN_MODIFICATION = new EventType<Event>(TREE_NOTIFICATION,
            "ChildrenModificationEvent");
    private static final EventType<Event> BRANCH_EXPANDED = new EventType<Event>(TREE_NOTIFICATION,
            "BranchExpandedEvent");
    private static final EventType<Event> BRANCH_COLLAPSED = new EventType<Event>(TREE_NOTIFICATION,
            "BranchCollapsedEvent");

    private final ObjectProperty<T> value = new SimpleObjectProperty<T>(this, "value");
    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<Node>(this, "graphic");
    private final BooleanProperty expanded = new SimpleBooleanProperty(this, "expanded", false);
    private final ReadOnlyBooleanWrapper leaf = new ReadOnlyBooleanWrapper(this, "leaf", true);
    private final ReadOnlyObjectWrapper<TreeItem<T>> parent = new ReadOnlyObjectWrapper<TreeItem<T>>(this,
            "parent");
    private final ObservableList<TreeItem<T>> children = FXCollections.observableArrayList();
    private HashMap<EventType<?>, ArrayList<EventHandler<?>>> handlers;

    /// Creates an item with no value.
    public TreeItem() {
        this(null);
    }

    /// Creates an item with a value.
    public TreeItem(T value) {
        this(value, null);
    }

    /// Creates an item with a value and a graphic.
    public TreeItem(T value, Node graphic) {
        this.value.set(value);
        this.graphic.set(graphic);
        children.addListener((ListChangeListener<TreeItem<T>>) change -> {
            while (change.next()) {
                List<? extends TreeItem<T>> gone = change.getRemoved();
                for (int i = 0; i < gone.size(); i++) {
                    TreeItem<T> item = gone.get(i);
                    if (item != null && item.getParent() == TreeItem.this && !children.contains(item)) {
                        item.parent.set(null);
                    }
                }
                List<? extends TreeItem<T>> come = change.getAddedSubList();
                for (int i = 0; i < come.size(); i++) {
                    TreeItem<T> item = come.get(i);
                    if (item != null) {
                        item.parent.set(TreeItem.this);
                    }
                }
            }
            leaf.set(children.isEmpty());
            tell(CHILDREN_MODIFICATION);
        });
        this.value.addListener((observable, was, now) -> tell(VALUE_CHANGED));
        this.graphic.addListener((observable, was, now) -> tell(GRAPHIC_CHANGED));
        this.expanded.addListener((observable, was, now) -> {
            if (!isLeaf()) {
                tell(now.booleanValue() ? BRANCH_EXPANDED : BRANCH_COLLAPSED);
            }
        });
    }

    /// The type every event of a tree item is of.
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> treeNotificationEvent() {
        return (EventType<TreeModificationEvent<T>>) (EventType<?>) TREE_NOTIFICATION;
    }

    /// The value of an item changed.
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> valueChangedEvent() {
        return (EventType<TreeModificationEvent<T>>) (EventType<?>) VALUE_CHANGED;
    }

    /// The graphic of an item changed.
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> graphicChangedEvent() {
        return (EventType<TreeModificationEvent<T>>) (EventType<?>) GRAPHIC_CHANGED;
    }

    /// Items were added to or removed from the children of an item.
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> childrenModificationEvent() {
        return (EventType<TreeModificationEvent<T>>) (EventType<?>) CHILDREN_MODIFICATION;
    }

    /// An item with children was opened.
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> branchExpandedEvent() {
        return (EventType<TreeModificationEvent<T>>) (EventType<?>) BRANCH_EXPANDED;
    }

    /// An item with children was closed.
    @SuppressWarnings("unchecked")
    public static <T> EventType<TreeModificationEvent<T>> branchCollapsedEvent() {
        return (EventType<TreeModificationEvent<T>>) (EventType<?>) BRANCH_COLLAPSED;
    }

    /// The number of items a tree shows changed; told here as
    /// [#treeNotificationEvent()].
    public static <T> EventType<TreeModificationEvent<T>> expandedItemCountChangeEvent() {
        return treeNotificationEvent();
    }

    /// Tells the handlers of this item and of every item above it.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tell(EventType<Event> type) {
        TreeModificationEvent<T> event = new TreeModificationEvent<T>((EventType) type, this);
        TreeItem<T> at = this;
        // A tree is not deep enough to need more; a cycle must not hang.
        for (int depth = 0; at != null && depth < 10000; depth++) {
            if (at.handlers != null) {
                EventType<?> t = type;
                while (t != null) {
                    ArrayList<EventHandler<?>> list = at.handlers.get(t);
                    if (list != null) {
                        Object[] all = list.toArray();
                        for (int i = 0; i < all.length; i++) {
                            ((EventHandler) all[i]).handle(event);
                        }
                    }
                    t = t.getSuperType();
                }
            }
            at = at.getParent();
        }
    }

    /// Adds a handler for events of a type from this item and the items
    /// under it.
    public <E extends Event> void addEventHandler(EventType<E> eventType, EventHandler<E> eventHandler) {
        if (eventType == null || eventHandler == null) {
            return;
        }
        if (handlers == null) {
            handlers = new HashMap<EventType<?>, ArrayList<EventHandler<?>>>();
        }
        ArrayList<EventHandler<?>> list = handlers.get(eventType);
        if (list == null) {
            list = new ArrayList<EventHandler<?>>();
            handlers.put(eventType, list);
        }
        list.add(eventHandler);
    }

    /// Removes a handler.
    public <E extends Event> void removeEventHandler(EventType<E> eventType, EventHandler<E> eventHandler) {
        ArrayList<EventHandler<?>> list = handlers == null ? null : handlers.get(eventType);
        if (list != null) {
            list.remove(eventHandler);
        }
    }

    /// The events of a tree item go to its own handlers, not along a
    /// chain of the scene.
    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail;
    }

    /// Returns the value.
    public final T getValue() {
        return value.get();
    }

    /// Sets the value.
    public final void setValue(T newValue) {
        value.set(newValue);
    }

    /// The value.
    public final ObjectProperty<T> valueProperty() {
        return value;
    }

    /// Returns the node shown before the value.
    public final Node getGraphic() {
        return graphic.get();
    }

    /// Sets the node shown before the value.
    public final void setGraphic(Node newValue) {
        graphic.set(newValue);
    }

    /// The node shown before the value.
    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    /// Returns whether the item is open.
    public final boolean isExpanded() {
        return expanded.get();
    }

    /// Opens or closes the item.
    public final void setExpanded(boolean newValue) {
        expanded.set(newValue);
    }

    /// Whether the item is open.
    public final BooleanProperty expandedProperty() {
        return expanded;
    }

    /// Returns whether the item has no children.
    public boolean isLeaf() {
        return leaf.get();
    }

    /// Whether the item has no children.
    public final ReadOnlyBooleanProperty leafProperty() {
        return leaf.getReadOnlyProperty();
    }

    /// Returns the item this one is a child of, or `null`.
    public final TreeItem<T> getParent() {
        return parent.get();
    }

    /// The item this one is a child of.
    public final ReadOnlyObjectProperty<TreeItem<T>> parentProperty() {
        return parent.getReadOnlyProperty();
    }

    /// Returns the items under this one.
    public ObservableList<TreeItem<T>> getChildren() {
        return children;
    }

    /// Returns the item before this one under the same parent, or `null`.
    public TreeItem<T> previousSibling() {
        return sibling(-1);
    }

    /// Returns the item after this one under the same parent, or `null`.
    public TreeItem<T> nextSibling() {
        return sibling(1);
    }

    private TreeItem<T> sibling(int step) {
        TreeItem<T> above = getParent();
        if (above == null) {
            return null;
        }
        int at = above.children.indexOf(this) + step;
        return at < 0 || at >= above.children.size() ? null : above.children.get(at);
    }

    @Override
    public String toString() {
        return "TreeItem [ value: " + getValue() + " ]";
    }

    /// What happened to an item of a tree.
    public static class TreeModificationEvent<T> extends Event {

        private static final long serialVersionUID = 1L;

        private final transient TreeItem<T> treeItem;

        /// Creates an event about an item.
        public TreeModificationEvent(EventType<? extends Event> eventType, TreeItem<T> treeItem) {
            super(eventType);
            this.treeItem = treeItem;
        }

        /// Returns the item the event is about.
        public TreeItem<T> getTreeItem() {
            return treeItem;
        }

        @Override
        public TreeItem<T> getSource() {
            return treeItem;
        }

        /// Returns the value of the item now.
        public T getNewValue() {
            return treeItem == null ? null : treeItem.getValue();
        }

        /// Returns whether the event is a branch opening.
        public boolean wasExpanded() {
            return getEventType() == BRANCH_EXPANDED;
        }

        /// Returns whether the event is a branch closing.
        public boolean wasCollapsed() {
            return getEventType() == BRANCH_COLLAPSED;
        }

        /// Returns whether the event is a change of children.
        public boolean wasAdded() {
            return getEventType() == CHILDREN_MODIFICATION;
        }
    }
}

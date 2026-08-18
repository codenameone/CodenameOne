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
package com.codename1.impl.html5;

import com.codename1.html5.js.dom.CSSStyleDeclaration;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.HTMLDocument;
import com.codename1.html5.js.dom.HTMLElement;
import com.codename1.html5.js.dom.HTMLInputElement;
import com.codename1.html5.js.dom.HTMLTextAreaElement;
import com.codename1.ui.Component;
import com.codename1.ui.TextArea;
import com.codename1.ui.accessibility.AccessibilityAction;
import com.codename1.ui.accessibility.AccessibilityCheckedState;
import com.codename1.ui.accessibility.AccessibilityCollectionInfo;
import com.codename1.ui.accessibility.AccessibilityCollectionItemInfo;
import com.codename1.ui.accessibility.AccessibilityLiveRegion;
import com.codename1.ui.accessibility.AccessibilityNodeSnapshot;
import com.codename1.ui.accessibility.AccessibilityRange;
import com.codename1.ui.accessibility.AccessibilityRole;
import com.codename1.ui.accessibility.AccessibilityTreeSnapshot;
import com.codename1.ui.geom.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maintains the DOM overlay that mirrors the Codename One component tree above the
 * rendering canvas.
 *
 * <p>The overlay is the port's projection of {@link AccessibilityTreeSnapshot}: one element
 * per semantic node, carrying the ARIA role/state, the node's screen rectangle, a tab index
 * and the activation listeners. Assistive technology, browser find-in-page and native focus
 * traversal all operate against this tree rather than against the canvas, which is marked
 * {@code role=presentation}.</p>
 *
 * <p>Updates are <em>incremental</em>. Elements are keyed by the stable node id and reused
 * across invalidations; only attributes, geometry and child ordering that actually changed
 * are written back to the DOM, and event listeners are registered exactly once per element.
 * This matters for more than throughput: {@code CHANGE_BOUNDS} is raised by every
 * {@code setX/setY/setWidth/setHeight}, so a rebuild-per-invalidation would discard DOM focus
 * and any in-progress text selection on every scroll step, and would re-marshal the whole tree
 * across the worker bridge each time.</p>
 *
 * <p>The class never reads back from the DOM. Structure, ordering and previously applied
 * attribute values are tracked worker-side so that every bridge call remains a fire-and-forget
 * write, preserving the port's no-barrier-reads invariant.</p>
 *
 * @author Codename One
 */
public final class JavaScriptSemanticOverlay {

    /**
     * Receives activation, focus and value-adjustment requests originating from the overlay.
     */
    public interface ActionDispatcher {
        /**
         * Dispatches a semantic action onto the Codename One event thread.
         *
         * @param nodeId the semantic node the action targets
         * @param actionId one of the {@link AccessibilityAction} identifiers
         * @param argument optional action argument, may be null
         */
        void performAction(long nodeId, String actionId, Object argument);
    }

    private static final String ATTRIBUTE_NODE_ID = "data-cn1-accessibility-id";

    /**
     * Marks a SET_TEXT control while it holds focus, so a snapshot arriving mid-edit does
     * not overwrite what is being typed.
     */
    private static final String ATTRIBUTE_EDITING = "data-cn1-editing";

    /**
     * Records whether a SET_TEXT control was built for an obscured field, so a field that
     * changes between masked and revealed is noticed on the next snapshot.
     */
    private static final String ATTRIBUTE_OBSCURED = "data-cn1-obscured";

    /**
     * Marks a SET_TEXT control built as a textarea, since an element cannot change tag: a field
     * that becomes multiline needs its control built again rather than reconfigured.
     */
    private static final String ATTRIBUTE_MULTILINE = "data-cn1-multiline";

    /**
     * Retained state for a single semantic node. Everything the diff needs to decide whether a
     * DOM write is required lives here, so no property is ever read back from the host.
     */
    private static final class Entry {
        private final long id;
        private final HTMLElement element;
        private final String tag;
        private final Map<String, String> attributes = new HashMap<String, String>();
        // Held for the same reason as in the text layer: getStyle() is a round trip that parks
        // the worker, and geometry is rewritten on every CHANGE_BOUNDS.
        private final CSSStyleDeclaration style;
        private final List<Long> childOrder = new ArrayList<Long>();
        private Map<String, HTMLElement> customActions;
        private Map<String, String> customActionLabels;
        // The document id the action controls were pointed at. An application can change a
        // node's identifier, and a control left pointing at the old one refers to nothing.
        private String actionOwnerId;
        private HTMLElement textNode;
        private String geometry;
        private String text;
        private long parentId = -1;
        private boolean listenersBound;
        private boolean activateEnabled;
        private boolean incrementEnabled;
        private boolean decrementEnabled;
        private int tabIndex = Integer.MIN_VALUE;

        Entry(long id, HTMLElement element, String tag) {
            this.id = id;
            this.element = element;
            this.tag = tag;
            this.style = element.getStyle();
        }
    }

    private final HTMLDocument document;
    private final HTMLElement container;
    private final ActionDispatcher dispatcher;
    private final Map<Long, Entry> entries = new HashMap<Long, Entry>();
    private final List<Long> rootOrder = new ArrayList<Long>();
    private HTMLElement actionsContainer;
    private boolean textContentEnabled = true;
    private long focusedNodeId = -1;

    private Entry pendingFocus;

    /**
     * Creates an overlay bound to a container element that is already attached to the document.
     *
     * @param document the host document used to create elements
     * @param container the overlay root, typically {@code #cn1-accessibility-tree}
     * @param dispatcher receives actions triggered from the overlay
     */
    public JavaScriptSemanticOverlay(HTMLDocument document, HTMLElement container,
            ActionDispatcher dispatcher) {
        this.document = document;
        this.container = container;
        this.dispatcher = dispatcher;
    }

    /**
     * Controls whether this overlay puts label text into the document.
     *
     * <p>It should not when something else already renders that text visibly: the overlay's
     * copy is nearly transparent but still findable, so browser find-in-page would report two
     * matches and could navigate to the invisible one. Assistive technology is unaffected --
     * the label reaches it through {@code aria-label} either way.</p>
     *
     * @param value true to mirror labels as text nodes
     */
    public void setTextContentEnabled(boolean value) {
        textContentEnabled = value;
    }

    /**
     * Reconciles the overlay against a freshly captured semantic tree.
     *
     * <p>Nodes are matched by id, so elements survive across calls together with their DOM
     * focus and any text selection they carry. Only differences are written.</p>
     *
     * @param tree the current semantic tree
     * @param devicePixelRatio the ratio used to convert Codename One device pixels to CSS pixels
     */
    public void update(AccessibilityTreeSnapshot tree, double devicePixelRatio) {
        if (tree == null) {
            return;
        }
        double ratio = devicePixelRatio <= 0 ? 1 : devicePixelRatio;
        Map<Long, AccessibilityNodeSnapshot> nodes = tree.getNodes();
        Set<Long> live = new HashSet<Long>();

        // Walk from the roots so that ordering is derived from getChildIds() rather than from
        // map iteration order, and so a child is never visited before its parent exists.
        List<Long> roots = tree.getRootIds();
        for (int i = 0; i < roots.size(); i++) {
            visit(nodes, roots.get(i), -1, live, ratio);
        }

        pruneRemovedNodes(live);
        reconcileChildOrder(container, rootOrder, roots, live);
        if (actionsContainer != null) {
            // Moved behind the tree every time it is rebuilt. The region is created the first
            // time a node needs a control, which happens while the tree is still being walked --
            // before the roots are attached -- so left where it was made it would sit ahead of
            // the whole form, and a keyboard or screen-reader user would meet "Set text" before
            // reaching the field it writes to. appendChild moves it rather than copying it.
            container.appendChild(actionsContainer);
        }

        if (pendingFocus != null) {
            // Recorded before the call, and that record is what tells the focus event apart
            // afterwards. A guard held only for the duration of the call would be useless: a
            // void call across the bridge is queued for the main thread, so it returns long
            // before the browser dispatches the event it causes.
            focusedNodeId = pendingFocus.id;
            HTMLElement target = pendingFocus.element;
            pendingFocus = null;
            target.focus();
        }
    }

    /**
     * Removes every element from the overlay and drops the retained state.
     */
    public void clear() {
        // Emptying the container detaches the whole subtree in one write. Walking the entries
        // and removing each from its parent would not be equivalent here, because a parent may
        // already have been dropped from the map by the time its children are visited.
        container.setInnerHTML("");
        actionsContainer = null;
        entries.clear();
        rootOrder.clear();
        focusedNodeId = -1;
        pendingFocus = null;
    }

    private void visit(Map<Long, AccessibilityNodeSnapshot> nodes, Long id, long parentId,
            Set<Long> live, double ratio) {
        AccessibilityNodeSnapshot node = nodes.get(id);
        if (node == null || !live.add(id)) {
            return;
        }
        AccessibilityNodeSnapshot parent = parentId == -1
                ? null : nodes.get(Long.valueOf(parentId));
        Entry entry = obtain(node);
        applyParent(entry, parentId);
        applyAttributes(entry, node, nodes, parent);
        applyGeometry(entry, node, parent, ratio);
        applyText(entry, node);
        applyListeners(entry, node);
        applyCustomActions(entry, node);
        applyFocus(entry, node);

        List<Long> children = node.getChildIds();
        for (int i = 0; i < children.size(); i++) {
            visit(nodes, children.get(i), node.getId(), live, ratio);
        }
        reconcileChildOrder(entry.element, entry.childOrder, children, live);
    }

    private Entry obtain(AccessibilityNodeSnapshot node) {
        Long key = Long.valueOf(node.getId());
        String tag = tagFor(node);
        Entry entry = entries.get(key);
        if (entry != null && entry.tag.equals(tag)) {
            return entry;
        }
        if (entry != null) {
            // The role changed in a way that needs a different element type, so the old
            // element cannot be reused. Unlink it so the ordering model no longer claims the
            // slot is filled -- otherwise the reconcile pass would see the id already in place
            // and never attach the replacement element.
            detach(entry);
            releaseCustomActions(entry);
            unlinkFromParentOrder(entry);
            entries.remove(key);
        }
        HTMLElement element = document.createElement(tag);
        element.setAttribute(ATTRIBUTE_NODE_ID, String.valueOf(node.getId()));
        Entry created = new Entry(node.getId(), element, tag);
        entries.put(key, created);
        return created;
    }

    private String tagFor(AccessibilityNodeSnapshot node) {
        // A real anchor gives the browser affordances an ARIA role cannot: status-bar URL
        // preview, middle-click and modifier-click to open in a new tab, and the native
        // context menu.
        if (node.getRole() == AccessibilityRole.LINK) {
            return "a";
        }
        return "div";
    }

    private void applyParent(Entry entry, long parentId) {
        if (entry.parentId == parentId) {
            return;
        }
        // Vacate the slot under the old parent. The element itself does not need an explicit
        // removal: appendChild/insertBefore under the new parent moves it.
        unlinkFromParentOrder(entry);
        entry.parentId = parentId;
    }

    private void applyAttributes(Entry entry, AccessibilityNodeSnapshot node,
            Map<Long, AccessibilityNodeSnapshot> nodes, AccessibilityNodeSnapshot parent) {
        Map<String, String> desired = describe(node, nodes, parent);
        for (Iterator<Map.Entry<String, String>> it = desired.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> attribute = it.next();
            String name = attribute.getKey();
            String value = attribute.getValue();
            if (!value.equals(entry.attributes.get(name))) {
                entry.element.setAttribute(name, value);
                entry.attributes.put(name, value);
            }
        }
        for (Iterator<Map.Entry<String, String>> it = entry.attributes.entrySet().iterator(); it.hasNext();) {
            String name = it.next().getKey();
            if (!desired.containsKey(name)) {
                entry.element.removeAttribute(name);
                it.remove();
            }
        }
        int tabIndex = node.isFocusable() ? 0 : -1;
        if (entry.tabIndex != tabIndex) {
            entry.element.setTabIndex(tabIndex);
            entry.tabIndex = tabIndex;
        }
    }

    private void applyGeometry(Entry entry, AccessibilityNodeSnapshot node,
            AccessibilityNodeSnapshot parent, double ratio) {
        Rectangle bounds = node.getBounds();
        // The snapshot reports absolute screen coordinates, but an entry is positioned inside
        // its parent entry, which is itself absolutely positioned -- so the parent's offset
        // would be counted twice, pushing descendants away from their component and, with the
        // parent clipping its content, often out of sight entirely.
        int originX = parent == null ? 0 : parent.getBounds().getX();
        int originY = parent == null ? 0 : parent.getBounds().getY();
        StringBuilder css = new StringBuilder(
                "position:absolute;opacity:0.001;pointer-events:none;overflow:hidden;");
        css.append("left:").append((bounds.getX() - originX) / ratio).append("px;");
        css.append("top:").append((bounds.getY() - originY) / ratio).append("px;");
        css.append("width:").append(Math.max(1, bounds.getWidth()) / ratio).append("px;");
        css.append("height:").append(Math.max(1, bounds.getHeight()) / ratio).append("px;");
        String geometry = css.toString();
        if (!geometry.equals(entry.geometry)) {
            entry.style.setCssText(geometry);
            entry.geometry = geometry;
        }
    }

    private void applyText(Entry entry, AccessibilityNodeSnapshot node) {
        String text = null;
        boolean obscured = node.getObscured() != null && node.getObscured().booleanValue();
        // Labels are mirrored whether or not the text layer is rendering them.
        //
        // Review has pulled both ways here. Mirroring means a browser find can match a label
        // twice, once in the visible layer and once in this near-transparent copy. Not
        // mirroring means a screen reader loses ordinary labels entirely: the visible layer is
        // aria-hidden, STATIC_TEXT has no ARIA role of its own, and an aria-label on a role-less
        // div is not reliably announced -- nor does it work as live-region content. A duplicate
        // find match is an annoyance; silent labels are a broken screen reader, so the mirror
        // stays.
        if (textContentEnabled && !obscured && isLabelVisibleAsText(node.getRole())) {
            text = node.getLabel() == null ? "" : node.getLabel();
        }
        if (text == null && !obscured && isTextEntryRole(node.getRole())) {
            // A textbox's value is its content: ARIA has no attribute that carries it --
            // aria-valuetext is defined for range roles and is not read as a textbox's value --
            // so a field holding only a label announces its name over an empty value however
            // much the user has typed into it. This is the field's value rather than a second
            // copy of a label, so unlike the mirror above it does not depend on that switch.
            text = textEntryValue(node);
        }
        if (text == null) {
            if (entry.textNode != null) {
                entry.textNode.setTextContent("");
                entry.text = null;
            }
            return;
        }
        if (text.equals(entry.text)) {
            return;
        }
        // The text goes into a child of its own rather than through setTextContent on the node.
        // setTextContent replaces every child, which would silently detach this node's semantic
        // children and its custom-action buttons while the retained ordering still claimed they
        // were attached -- so the reconcile pass would skip re-adding them and those controls
        // would vanish until an unrelated structural change rebuilt them.
        if (entry.textNode == null) {
            entry.textNode = document.createElement("span");
            HTMLElement first = firstChildElement(entry);
            if (first == null) {
                entry.element.appendChild(entry.textNode);
            } else {
                entry.element.insertBefore(entry.textNode, first);
            }
        }
        entry.textNode.setTextContent(text);
        entry.text = text;
    }

    /**
     * True for roles a user types into, whose value the document has to carry as content.
     */
    private boolean isTextEntryRole(AccessibilityRole role) {
        return role == AccessibilityRole.TEXT_FIELD || role == AccessibilityRole.SEARCH_FIELD;
    }

    /**
     * The text a typing field currently holds.
     *
     * @param node the field's semantic node
     * @return its contents, never null
     */
    private String textEntryValue(AccessibilityNodeSnapshot node) {
        if (node.getValue() != null) {
            return node.getValue();
        }
        // A field with no separately associated label has its label inferred from its own text,
        // and in that case the value is left unset -- so the component is the only place the
        // contents can be read from.
        Component owner = node.getComponent();
        if (owner instanceof TextArea) {
            String value = ((TextArea) owner).getText();
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    /**
     * True for roles whose label is text the user reads on screen.
     */
    private boolean isLabelVisibleAsText(AccessibilityRole role) {
        switch (role) {
            case STATIC_TEXT:
            case HEADING:
            case BUTTON:
            case TOGGLE_BUTTON:
            case CHECKBOX:
            case RADIO_BUTTON:
            case SWITCH:
            case LINK:
            case TAB:
            case MENU_ITEM:
            case LIST_ITEM:
            case TREE_ITEM:
            case CELL:
            case COLUMN_HEADER:
            case ROW_HEADER:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns the element currently occupying the first child slot, so a newly created text
     * node can be placed ahead of the node's children and read before them.
     */
    private HTMLElement firstChildElement(Entry entry) {
        if (!entry.childOrder.isEmpty()) {
            Entry child = entries.get(entry.childOrder.get(0));
            if (child != null) {
                return child.element;
            }
        }
        return null;
    }

    private void applyListeners(Entry entry, AccessibilityNodeSnapshot node) {
        // A node reported as disabled takes no action from here, whatever its actions say about
        // themselves: a slider builds its increment and decrement without asking the component
        // whether it is enabled, and an arrow key would otherwise move something the application
        // has switched off -- and which is announced as switched off.
        boolean usable = node.getEnabled() == null || node.getEnabled().booleanValue();
        AccessibilityAction activate = node.getAction(AccessibilityAction.ACTIVATE);
        entry.activateEnabled = usable && activate != null && activate.isEnabled();
        AccessibilityAction increment = node.getAction(AccessibilityAction.INCREMENT);
        entry.incrementEnabled = usable && increment != null && increment.isEnabled();
        AccessibilityAction decrement = node.getAction(AccessibilityAction.DECREMENT);
        entry.decrementEnabled = usable && decrement != null && decrement.isEnabled();
        if (entry.listenersBound) {
            return;
        }
        entry.listenersBound = true;
        final long nodeId = entry.id;
        final Entry bound = entry;
        entry.element.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event event) {
                if (!bound.activateEnabled) {
                    return;
                }
                event.preventDefault();
                event.stopPropagation();
                dispatcher.performAction(nodeId, AccessibilityAction.ACTIVATE, null);
            }
        });
        entry.element.addEventListener("focus", new EventListener() {
            @Override
            public void handleEvent(Event event) {
                if (focusedNodeId == nodeId) {
                    // The framework already has focus here -- either it reported this node as
                    // focused and this overlay mirrored it, or the user focused a node the
                    // framework was already on. Either way, telling the framework about it
                    // would be asking it to act on its own state.
                    return;
                }
                focusedNodeId = nodeId;
                dispatcher.performAction(nodeId, AccessibilityAction.FOCUS, null);
            }
        });
        entry.element.addEventListener("keydown", new EventListener() {
            @Override
            public void handleEvent(Event event) {
                JSOImplementations.KeyEvent key = (JSOImplementations.KeyEvent) event;
                int code = key.getKeyCode();
                String action = null;
                if ((code == 13 || code == 32) && bound.activateEnabled) {
                    action = AccessibilityAction.ACTIVATE;
                } else if ((code == 38 || code == 39) && bound.incrementEnabled) {
                    action = AccessibilityAction.INCREMENT;
                } else if ((code == 37 || code == 40) && bound.decrementEnabled) {
                    action = AccessibilityAction.DECREMENT;
                }
                // Arrows are only taken when the node actually offers the action. Consuming them
                // on an ordinary focusable node would stop them reaching the window-level key
                // handler that performs directional focus traversal, trapping the keyboard.
                if (action == null) {
                    return;
                }
                // The event would otherwise bubble to the window-level key handler and drive
                // the same component a second time: Enter would fire a button's action through
                // the semantic action and again through its pressed/released path, and an arrow
                // key would step a slider twice.
                event.preventDefault();
                event.stopPropagation();
                dispatcher.performAction(nodeId, action, null);
            }
        });
    }

    /**
     * Moves DOM focus to follow the framework's.
     *
     * <p>Elements are retained across invalidations, so the browser's focus stays where it was
     * unless it is moved deliberately. When the application moves focus itself -- requestFocus(),
     * keyboard traversal -- a screen reader would otherwise keep announcing the previous element,
     * and Enter or Space would activate a component that is no longer focused.</p>
     *
     * <p>Tracked by id rather than by asking the document what is focused: reading the active
     * element would be a round trip to the main thread.</p>
     */
    private void applyFocus(Entry entry, AccessibilityNodeSnapshot node) {
        if (!node.isFocused()) {
            if (focusedNodeId == entry.id) {
                // The framework has taken focus off this node -- an application calling
                // setFocused(null), or an editor closing. Letting the browser keep focus here
                // would leave assistive technology announcing a focus the application does not
                // have, and Enter, Space or an arrow key would still reach this element's
                // listeners and act on a component the framework considers unfocused.
                focusedNodeId = -1;
                entry.element.blur();
            }
            return;
        }
        if (focusedNodeId == entry.id) {
            return;
        }
        // Recorded, not applied: a node appearing for the first time -- the first snapshot, or
        // the first after a form change -- has not been attached yet, and a browser ignores
        // focus on an element that is not in the document. Applied once the tree is in place.
        pendingFocus = entry;
    }

    private void applyCustomActions(Entry entry, AccessibilityNodeSnapshot node) {
        Set<String> desired = null;
        // A node the framework reports as disabled offers nothing to activate. An action does not
        // have to know its component is disabled -- most are built without asking -- so a control
        // for one would let a screen reader work a node it announces as unavailable.
        boolean usable = node.getEnabled() == null || node.getEnabled().booleanValue();
        String owner = ownerId(entry);
        if (entry.customActions != null && !owner.equals(entry.actionOwnerId)) {
            // The node's identifier changed, so every control still pointing at the old one
            // has lost its association with what it acts on -- and its description with it.
            for (Iterator<HTMLElement> it = entry.customActions.values().iterator(); it.hasNext();) {
                HTMLElement button = it.next();
                button.setAttribute("aria-controls", owner);
                button.setAttribute("aria-describedby", owner);
            }
        }
        entry.actionOwnerId = owner;
        List<AccessibilityAction> actions = node.getActions();
        for (int i = 0; i < actions.size(); i++) {
            AccessibilityAction action = actions.get(i);
            if (!usable || !action.isEnabled() || isStandardWebAction(action.getId())) {
                continue;
            }
            if (desired == null) {
                desired = new HashSet<String>();
            }
            desired.add(action.getId());
            if (entry.customActions == null) {
                entry.customActions = new HashMap<String, HTMLElement>();
            }
            String label = controlLabel(action, node);
            HTMLElement existing = entry.customActions.get(action.getId());
            if (existing != null && staleControlShape(existing, action.getId(), node)) {
                // An input cannot become a textarea, so a field that turned multiline is given a
                // control that can hold the line breaks its value now has.
                actionsContainer().removeChild(existing);
                entry.customActions.remove(action.getId());
                if (entry.customActionLabels != null) {
                    entry.customActionLabels.remove(action.getId());
                }
                existing = null;
            }
            if (existing != null) {
                // An application can replace an action with the same id and a new label -- an
                // Expand that becomes a Collapse. Without this the retained button keeps
                // announcing the old wording until the action is removed entirely.
                if (!label.equals(entry.customActionLabels.get(action.getId()))) {
                    existing.setAttribute("aria-label", label);
                    if (!AccessibilityAction.SET_TEXT.equals(action.getId())) {
                        // A control that carries a value is named by aria-label alone: its text
                        // content IS its value for a textarea, and writing the label there would
                        // replace what the field holds with the name of the control.
                        existing.setTextContent(label);
                    }
                    entry.customActionLabels.put(action.getId(), label);
                }
                syncSetTextControl(existing, action.getId(), node);
                continue;
            }
            if (entry.customActionLabels == null) {
                entry.customActionLabels = new HashMap<String, String>();
            }
            entry.customActions.put(action.getId(), createCustomAction(entry, node, action, label));
            entry.customActionLabels.put(action.getId(), label);
        }
        if (entry.customActions == null) {
            return;
        }
        for (Iterator<Map.Entry<String, HTMLElement>> it = entry.customActions.entrySet().iterator();
                it.hasNext();) {
            Map.Entry<String, HTMLElement> existing = it.next();
            if (desired == null || !desired.contains(existing.getKey())) {
                actionsContainer().removeChild(existing.getValue());
                if (entry.customActionLabels != null) {
                    entry.customActionLabels.remove(existing.getKey());
                }
                it.remove();
            }
        }
    }

    private HTMLElement createCustomAction(Entry entry, AccessibilityNodeSnapshot node,
            final AccessibilityAction action, String label) {
        if (AccessibilityAction.SET_TEXT.equals(action.getId())) {
            return createSetTextControl(entry, node, action, label);
        }
        final long nodeId = entry.id;
        HTMLElement button = document.createElement("button");
        button.setAttribute("type", "button");
        button.setAttribute("aria-label", label);
        // Names the node this acts on as the button's description, since the button no longer
        // sits inside it: "Delete" on its own says nothing about what would be deleted.
        String owner = ownerId(entry);
        button.setAttribute("aria-controls", owner);
        button.setAttribute("aria-describedby", owner);
        button.setTextContent(label);
        button.getStyle().setCssText(
                "position:absolute;opacity:0.001;pointer-events:none;width:1px;height:1px;");
        button.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event event) {
                event.preventDefault();
                // The overlay root carries handlers of its own, and this is not one of the
                // node's own actions -- it must not read as a click on anything above it.
                event.stopPropagation();
                dispatcher.performAction(nodeId, action.getId(), null);
            }
        });
        // Held apart from the semantic tree rather than inside the node. Accessibility APIs
        // treat everything inside a widget role -- button, checkbox, switch -- as presentational,
        // so a button nested there may never be exposed at all; and a role=list accepts only
        // list items as children, which the scroll actions were breaking.
        actionsContainer().appendChild(button);
        return button;
    }

    /**
     * What a custom-action control announces itself as.
     *
     * <p>An action the application named is announced by that name. One the framework added has
     * only an id -- "setText" tells a screen-reader user nothing about which field it writes to,
     * so it is named after the field instead, and after what it does when the field has no name
     * of its own. A name inferred from the field's own contents is not used: that is its value,
     * and it would have the control announce the text it is meant to replace.</p>
     *
     * @param action the action being exposed
     * @param node the node it acts on
     * @return the control's label, never null
     */
    private String controlLabel(AccessibilityAction action, AccessibilityNodeSnapshot node) {
        if (action.getLabel() != null) {
            return action.getLabel();
        }
        if (!AccessibilityAction.SET_TEXT.equals(action.getId())) {
            return action.getId();
        }
        boolean obscured = node.getObscured() != null && node.getObscured().booleanValue();
        String name = node.getLabel();
        if (obscured || isDerivedFromContent(node, name)) {
            name = node.getHint();
        }
        return name == null || name.length() == 0 ? "Set text" : name;
    }

    /**
     * The control for SET_TEXT: an input, because the action takes the text to set.
     *
     * <p>A button cannot carry a value, so a field only ever reached through one could be
     * cleared, never written. This is a real input, so a screen reader in forms mode types into
     * it and the typed value is what reaches the framework -- the same handler a native editor
     * would have called.</p>
     *
     * @param entry the node the control acts on
     * @param node that node's snapshot, read for the value the field already holds
     * @param action the SET_TEXT action being exposed
     * @param label the action's label, which names the control
     * @return the control, already attached to the actions region
     */
    private HTMLElement createSetTextControl(Entry entry, AccessibilityNodeSnapshot node,
            final AccessibilityAction action, String label) {
        final long nodeId = entry.id;
        boolean obscured = node.getObscured() != null && node.getObscured().booleanValue();
        // A multiline field's value has line breaks in it, and an input cannot hold one: what a
        // screen-reader user typed would arrive with its lines run together. An obscured field
        // is single-line by nature and keeps the masking input.
        boolean multiline = !obscured && node.getMultiline() != null
                && node.getMultiline().booleanValue();
        final HTMLElement control = document.createElement(multiline ? "textarea" : "input");
        if (multiline) {
            control.setAttribute(ATTRIBUTE_MULTILINE, "1");
        } else {
            control.setAttribute("type", obscured ? "password" : "text");
            if (obscured) {
                control.setAttribute(ATTRIBUTE_OBSCURED, "1");
            }
        }
        control.setAttribute("aria-label", label);
        String owner = ownerId(entry);
        control.setAttribute("aria-controls", owner);
        control.setAttribute("aria-describedby", owner);
        applyMaxLength(control, node);
        if (!obscured) {
            setControlValue(control, textEntryValue(node));
        }
        control.getStyle().setCssText(
                "position:absolute;opacity:0.001;pointer-events:none;width:1px;height:1px;");
        EventListener commit = new EventListener() {
            @Override
            public void handleEvent(Event event) {
                // The overlay root carries handlers of its own, and this is not one of the
                // node's own actions -- it must not read as input on anything above it.
                event.stopPropagation();
                dispatcher.performAction(nodeId, action.getId(), controlValue(control));
            }
        };
        // change fires when the user leaves the field, which is when a screen reader in forms
        // mode has finished; input covers assistive technology that sets the value outright and
        // never sends a change.
        control.addEventListener("change", commit);
        control.addEventListener("input", commit);
        // Marked on the element rather than compared against document.activeElement, which this
        // port's document binding does not expose.
        control.addEventListener("focus", new EventListener() {
            @Override
            public void handleEvent(Event event) {
                control.setAttribute(ATTRIBUTE_EDITING, "1");
            }
        });
        final boolean masked = obscured;
        control.addEventListener("blur", new EventListener() {
            @Override
            public void handleEvent(Event event) {
                control.removeAttribute(ATTRIBUTE_EDITING);
                if (masked || control.getAttribute(ATTRIBUTE_OBSCURED) != null) {
                    // Every keystroke has already reached the framework, so the control has
                    // nothing left to hold -- and what it holds is a secret. type="password"
                    // only stops it being read off the screen; the value is still in the
                    // document for a script or an inspector, so it does not stay there past
                    // the edit. The sync pass will not do it: it leaves a masked field alone
                    // precisely so it never writes the secret back.
                    setControlValue(control, "");
                }
            }
        });
        actionsContainer().appendChild(control);
        return control;
    }

    /**
     * Holds a SET_TEXT control to the same length limit as the field it writes to.
     *
     * <p>Without it the control is the long way round an application's own limit, and not just
     * for the one edit: TextArea.setText() raises maxSize to fit whatever it is given, so a value
     * typed past the limit here moves the limit permanently.</p>
     *
     * @param element the control
     * @param node the field's snapshot, whose component carries the limit
     */
    private void applyMaxLength(HTMLElement element, AccessibilityNodeSnapshot node) {
        Component owner = node.getComponent();
        int max = owner instanceof TextArea ? ((TextArea) owner).getMaxSize() : 0;
        if (max > 0) {
            element.setAttribute("maxlength", String.valueOf(max));
        } else {
            // No limit to keep -- and an attribute left over from a field that used to have one
            // would be a limit the application no longer asks for.
            element.removeAttribute("maxlength");
        }
    }

    /**
     * True when a retained SET_TEXT control no longer has the shape its field needs.
     *
     * <p>Only the tag matters here: masking is an attribute an input can be given, but an input
     * cannot become a textarea, so that change means building the control again.</p>
     *
     * @param element the retained control
     * @param actionId the action it performs
     * @param node the field's current snapshot
     * @return true when the control has to be replaced
     */
    private boolean staleControlShape(HTMLElement element, String actionId,
            AccessibilityNodeSnapshot node) {
        if (!AccessibilityAction.SET_TEXT.equals(actionId)) {
            return false;
        }
        boolean obscured = node.getObscured() != null && node.getObscured().booleanValue();
        boolean multiline = !obscured && node.getMultiline() != null
                && node.getMultiline().booleanValue();
        return multiline != (element.getAttribute(ATTRIBUTE_MULTILINE) != null);
    }

    /**
     * Reads a SET_TEXT control's value, whichever element it was built as.
     *
     * @param element the control
     * @return what it holds, never null
     */
    private static String controlValue(HTMLElement element) {
        // Which element this is was recorded when it was built, rather than asked of the object:
        // the interop types are interfaces over the same host object, so a type test says
        // nothing about which tag was created.
        String value = element.getAttribute(ATTRIBUTE_MULTILINE) != null
                ? ((HTMLTextAreaElement) element).getValue()
                : ((HTMLInputElement) element).getValue();
        return value == null ? "" : value;
    }

    /**
     * Writes a SET_TEXT control's value, whichever element it was built as.
     *
     * @param element the control
     * @param value the value to show
     */
    private static void setControlValue(HTMLElement element, String value) {
        if (element.getAttribute(ATTRIBUTE_MULTILINE) != null) {
            ((HTMLTextAreaElement) element).setValue(value);
        } else {
            ((HTMLInputElement) element).setValue(value);
        }
    }

    /**
     * Brings a SET_TEXT control back in step with the field it writes to.
     *
     * <p>Skipped while the control has focus: overwriting what someone is in the middle of
     * typing is worse than showing a value one keystroke behind.</p>
     *
     * @param element the control, which is only an input for SET_TEXT
     * @param actionId the action the control performs
     * @param node the field's current snapshot
     */
    private void syncSetTextControl(HTMLElement element, String actionId,
            AccessibilityNodeSnapshot node) {
        if (!AccessibilityAction.SET_TEXT.equals(actionId)) {
            return;
        }
        boolean obscured = node.getObscured() != null && node.getObscured().booleanValue();
        if (obscured != (element.getAttribute(ATTRIBUTE_OBSCURED) != null)
                && element.getAttribute(ATTRIBUTE_MULTILINE) == null) {
            // A field can be masked and revealed while it stays in the tree -- the eye button on
            // a password field. A control left as it was would either keep typing in the clear
            // into a field that is now a secret, or go on masking one that no longer is.
            element.setAttribute("type", obscured ? "password" : "text");
            if (obscured) {
                element.setAttribute(ATTRIBUTE_OBSCURED, "1");
                // Cleared even mid-edit: what it holds became a secret, and a secret does not
                // stay in the document waiting for focus to leave.
                setControlValue(element, "");
            } else {
                element.removeAttribute(ATTRIBUTE_OBSCURED);
            }
        }
        if (obscured) {
            // A masked field's contents are never written into the document; the control exists
            // to set a new value, not to carry the old one.
            return;
        }
        applyMaxLength(element, node);
        if (element.getAttribute(ATTRIBUTE_EDITING) != null) {
            return;
        }
        String value = textEntryValue(node);
        if (!value.equals(controlValue(element))) {
            setControlValue(element, value);
        }
    }

    /**
     * The region holding custom-action controls, created on first use.
     *
     * @return the container element
     */
    private HTMLElement actionsContainer() {
        if (actionsContainer == null) {
            actionsContainer = document.createElement("div");
            actionsContainer.setAttribute("id", "cn1-accessibility-actions");
            actionsContainer.getStyle().setCssText(
                    "position:absolute;left:0;top:0;width:0;height:0;overflow:hidden;");
            container.appendChild(actionsContainer);
        }
        return actionsContainer;
    }

    /**
     * The document id a node's element currently carries.
     *
     * @param entry the node
     * @return the id an action control points at
     */
    private String ownerId(Entry entry) {
        String owner = entry.attributes.get("id");
        return owner == null ? elementId(entry.id) : owner;
    }

    /**
     * The document id given to a node's element when the application supplied none.
     *
     * @param nodeId the accessibility node id
     * @return a stable element id
     */
    private String elementId(long nodeId) {
        return "cn1-a11y-" + nodeId;
    }

    private void pruneRemovedNodes(Set<Long> live) {
        for (Iterator<Map.Entry<Long, Entry>> it = entries.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Long, Entry> existing = it.next();
            if (live.contains(existing.getKey())) {
                continue;
            }
            Entry entry = existing.getValue();
            detach(entry);
            releaseCustomActions(entry);
            unlinkFromParentOrder(entry);
            it.remove();
        }
    }

    /**
     * Drops a node's custom-action controls, which live outside it and would otherwise stay in
     * the document after the node they act on has gone.
     *
     * @param entry the node being removed
     */
    private void releaseCustomActions(Entry entry) {
        if (entry.customActions == null) {
            return;
        }
        for (Iterator<HTMLElement> it = entry.customActions.values().iterator(); it.hasNext();) {
            actionsContainer().removeChild(it.next());
        }
        entry.customActions = null;
        entry.customActionLabels = null;
    }

    private void unlinkFromParentOrder(Entry entry) {
        if (focusedNodeId == entry.id) {
            // Detaching the element moves browser focus to the document, and accessibility ids
            // are stable per component, so without this the node would be considered still
            // focused when it comes back and focus would never be restored to it.
            focusedNodeId = -1;
        }
        if (pendingFocus == entry) {
            pendingFocus = null;
        }
        Long key = Long.valueOf(entry.id);
        if (entry.parentId == -1) {
            rootOrder.remove(key);
            return;
        }
        Entry parent = entries.get(Long.valueOf(entry.parentId));
        if (parent != null) {
            parent.childOrder.remove(key);
        }
    }

    private void detach(Entry entry) {
        HTMLElement parent = entry.parentId == -1 ? container : parentElement(entry.parentId);
        if (parent != null) {
            parent.removeChild(entry.element);
        }
    }

    private HTMLElement parentElement(long parentId) {
        Entry parent = entries.get(Long.valueOf(parentId));
        return parent == null ? null : parent.element;
    }

    /**
     * Brings the DOM child order of {@code parent} in line with {@code desired}, mutating
     * {@code current} in lockstep so the retained model never drifts from the document.
     *
     * <p>Nodes already in the right slot are left untouched, which is what keeps a focused or
     * selected element from being moved -- moving an element in the DOM drops both.</p>
     */
    private void reconcileChildOrder(HTMLElement parent, List<Long> current, List<Long> desired,
            Set<Long> live) {
        int slot = 0;
        for (int i = 0; i < desired.size(); i++) {
            Long id = desired.get(i);
            if (!live.contains(id)) {
                continue;
            }
            Entry entry = entries.get(id);
            if (entry == null) {
                continue;
            }
            Long occupant = slot < current.size() ? current.get(slot) : null;
            if (id.equals(occupant)) {
                slot++;
                continue;
            }
            Entry occupantEntry = occupant == null ? null : entries.get(occupant);
            if (occupantEntry == null) {
                parent.appendChild(entry.element);
            } else {
                parent.insertBefore(entry.element, occupantEntry.element);
            }
            current.remove(id);
            current.add(slot, id);
            slot++;
        }
    }

    /**
     * Actions the browser already provides a way to perform, so the overlay does not add a
     * control for them.
     *
     * <p>The scroll actions are NOT included: the projection is a div, not a native scroll
     * container, so nothing else would perform them, and a list exposes only the items it is
     * currently showing -- without a control for them there is no way to reach the rest. They
     * take no argument, so a custom control dispatches them correctly.</p>
     *
     * <p>SET_TEXT is not included either, and the reason it once was is worth recording. A
     * button can only dispatch with a null argument, which for SET_TEXT means replacing the
     * field's contents with nothing, so it was left to the native input the port puts over a
     * field being edited. That input is created on demand and only where
     * useNativeOverlaysForTextFields() holds, which the default configuration does not, so a
     * screen reader was left with a field it could focus and never change. SET_TEXT now gets a
     * control that can carry a value -- a real input, not a button -- which is what an action
     * taking an argument needed all along.</p>
     */
    private boolean isStandardWebAction(String id) {
        return AccessibilityAction.ACTIVATE.equals(id) || AccessibilityAction.FOCUS.equals(id)
                || AccessibilityAction.INCREMENT.equals(id) || AccessibilityAction.DECREMENT.equals(id);
    }

    /**
     * Builds the full ARIA attribute set for a node. The result is diffed against the last
     * applied set, so this method describes rather than writes.
     */
    private Map<String, String> describe(AccessibilityNodeSnapshot node,
            Map<Long, AccessibilityNodeSnapshot> nodes, AccessibilityNodeSnapshot parent) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        out.put(ATTRIBUTE_NODE_ID, String.valueOf(node.getId()));
        String role = ariaRole(node.getRole());
        // A list whose items carry a selection is a listbox, and its items are options: a
        // listitem has no selected state in ARIA, and a list has no multi-selectable state, so a
        // framework list projected as a plain list reaches a screen reader as structure with no
        // indication of what is chosen.
        if (node.getRole() == AccessibilityRole.LIST && isSelectableCollection(node, nodes)) {
            role = "listbox";
        } else if (node.getRole() == AccessibilityRole.LIST_ITEM && parent != null
                && parent.getRole() == AccessibilityRole.LIST
                && isSelectableCollection(parent, nodes)) {
            // Asked of the list rather than of the item: an option belongs to a listbox, so a
            // selected item whose list stayed a plain list would be an option with no listbox
            // over it, and an unselected item beside a selected one has to be an option too --
            // a listbox's children are all options, and one that is not breaks the group a
            // screen reader reads the selection from.
            role = "option";
        }
        if (role != null) {
            out.put("role", role);
        }
        // An obscured field with no separately associated label has its label derived from the
        // text it contains, so writing that through would put the secret into the page DOM and
        // have a screen reader read it out as the field's name. Name it from the hint instead,
        // and never from its content.
        boolean obscured = node.getObscured() != null && node.getObscured().booleanValue();
        String accessibleName = node.getLabel();
        // Only a label that IS the secret is withheld. A field with no separately associated
        // label has one derived from its own text, which must never be published; an explicit
        // name set through setAccessibilityText(), setLabelForComponent() or the semantics
        // object is not secret and is the only thing naming the field for a screen reader.
        // The same applies to a plain field: a label inferred from its contents is its value,
        // not its name, and now that the value is published as the element's content, naming
        // the field with it too would have a screen reader read the typed text twice.
        if ((obscured || isTextEntryRole(node.getRole())) && isDerivedFromContent(node, accessibleName)) {
            accessibleName = node.getHint();
        }
        if (accessibleName == null) {
            // A dialog is normally named by its pane title -- inferred from the form title --
            // rather than by a label, and without this the element would have no accessible
            // name at all and be announced as an unnamed dialog.
            accessibleName = node.getPaneTitle();
        }
        if (accessibleName != null) {
            out.put("aria-label", accessibleName);
        }
        String description = node.getDescription();
        if (node.getHint() != null) {
            description = description == null ? node.getHint() : description + ". " + node.getHint();
        }
        if (node.getValidationError() != null) {
            description = description == null ? node.getValidationError()
                    : description + ". " + node.getValidationError();
        }
        if (description != null) {
            out.put("aria-description", description);
        }
        // Always an id, generated when the application did not give one: a custom action is
        // rendered outside the node it acts on, and aria-controls is how the two are tied back
        // together -- which needs the node to be referable.
        out.put("id", node.getIdentifier() != null ? node.getIdentifier() : elementId(node.getId()));
        if (node.getRoleDescription() != null) {
            out.put("aria-roledescription", node.getRoleDescription());
        }
        if (node.getValue() != null && !obscured && !isTextEntryRole(node.getRole())) {
            out.put("aria-valuetext", node.getValue());
        }
        if (node.getSelected() != null) {
            if (node.getRole() == AccessibilityRole.TOGGLE_BUTTON && node.getPressed() == null) {
                // A toggle button's state is aria-pressed. The framework infers "selected" for
                // one, and aria-selected on a button says nothing a screen reader will read out,
                // so it would be announced as an ordinary button with no state at all.
                out.put("aria-pressed", String.valueOf(node.getSelected()));
            } else {
                out.put("aria-selected", String.valueOf(node.getSelected()));
            }
        }
        if (node.getExpanded() != null) {
            out.put("aria-expanded", String.valueOf(node.getExpanded()));
        }
        if (node.getEnabled() != null && !node.getEnabled().booleanValue()) {
            out.put("aria-disabled", "true");
        }
        if (node.getInvalid() != null) {
            out.put("aria-invalid", String.valueOf(node.getInvalid()));
        }
        if (node.getBusy() != null) {
            out.put("aria-busy", String.valueOf(node.getBusy()));
        }
        if (node.getReadOnly() != null) {
            out.put("aria-readonly", String.valueOf(node.getReadOnly()));
        }
        if (node.getRequired() != null) {
            out.put("aria-required", String.valueOf(node.getRequired()));
        }
        if (node.getMultiline() != null) {
            out.put("aria-multiline", String.valueOf(node.getMultiline()));
        }
        if (node.getCurrent() != null && node.getCurrent().booleanValue()) {
            out.put("aria-current", "true");
        }
        if (node.isModal()) {
            out.put("aria-modal", "true");
        }
        if (node.getHeadingLevel() > 0) {
            out.put("aria-level", String.valueOf(node.getHeadingLevel()));
        }
        if (node.getChecked() != AccessibilityCheckedState.UNSPECIFIED) {
            out.put("aria-checked", node.getChecked() == AccessibilityCheckedState.MIXED
                    ? "mixed" : String.valueOf(node.getChecked() == AccessibilityCheckedState.CHECKED));
        }
        if (node.getPressed() != null) {
            out.put("aria-pressed", String.valueOf(node.getPressed()));
        }
        if (node.getLiveRegion() != AccessibilityLiveRegion.OFF) {
            out.put("aria-live", node.getLiveRegion() == AccessibilityLiveRegion.ASSERTIVE
                    ? "assertive" : "polite");
            out.put("aria-atomic", "true");
        }
        AccessibilityRange range = node.getRange();
        if (range != null && !obscured) {
            out.put("aria-valuemin", String.valueOf(range.getMinimum()));
            out.put("aria-valuemax", String.valueOf(range.getMaximum()));
            out.put("aria-valuenow", String.valueOf(range.getCurrent()));
            if (range.getText() != null) {
                out.put("aria-valuetext", range.getText());
            }
        }
        AccessibilityCollectionInfo collection = node.getCollectionInfo();
        if (collection != null) {
            if (collection.getRowCount() >= 0) {
                out.put("aria-rowcount", String.valueOf(collection.getRowCount()));
            }
            if (collection.getColumnCount() >= 0) {
                out.put("aria-colcount", String.valueOf(collection.getColumnCount()));
            }
            if (collection.getSelectionMode() == AccessibilityCollectionInfo.SELECTION_MULTIPLE) {
                out.put("aria-multiselectable", "true");
            }
        }
        AccessibilityCollectionItemInfo item = node.getCollectionItemInfo();
        if (item != null) {
            if (item.getPositionInSet() > 0) {
                out.put("aria-posinset", String.valueOf(item.getPositionInSet()));
            }
            if (item.getSetSize() != 0) {
                out.put("aria-setsize", String.valueOf(item.getSetSize()));
            }
            if (item.getLevel() > 0) {
                out.put("aria-level", String.valueOf(item.getLevel()));
            }
            if (item.getRowIndex() >= 0) {
                out.put("aria-rowindex", String.valueOf(item.getRowIndex() + 1));
            }
            if (item.getColumnIndex() >= 0) {
                out.put("aria-colindex", String.valueOf(item.getColumnIndex() + 1));
            }
            if (item.getRowSpan() > 1) {
                out.put("aria-rowspan", String.valueOf(item.getRowSpan()));
            }
            if (item.getColumnSpan() > 1) {
                out.put("aria-colspan", String.valueOf(item.getColumnSpan()));
            }
        }
        return out;
    }

    /**
     * True when a node's label is the text the component holds, rather than a name given to it.
     *
     * <p>A field with no separately associated label has its label inferred from its own
     * contents. For an obscured field that content is the secret, so the inferred label must
     * never be published -- but a name set explicitly must survive. The value is not a reliable
     * comparison on its own: an inferred label is taken from the component's text while the
     * node's value is left unset.</p>
     */
    private boolean isDerivedFromContent(AccessibilityNodeSnapshot node, String label) {
        if (label == null) {
            return false;
        }
        if (label.equals(node.getValue())) {
            return true;
        }
        Component owner = node.getComponent();
        return owner instanceof TextArea && label.equals(((TextArea) owner).getText());
    }

    /**
     * True when a collection reports a selection mode, or holds an item that knows whether it is
     * selected -- either way the user picks from it rather than merely reading it.
     *
     * @param node the collection node
     * @return true when the collection is one the user selects within
     */
    private boolean isSelectableCollection(AccessibilityNodeSnapshot node,
            Map<Long, AccessibilityNodeSnapshot> nodes) {
        AccessibilityCollectionInfo collection = node.getCollectionInfo();
        if (collection != null
                && collection.getSelectionMode() != AccessibilityCollectionInfo.SELECTION_NONE) {
            return true;
        }
        if (node.getSelected() != null) {
            return true;
        }
        // A list built by hand can carry the selection on its items and describe neither a
        // collection nor a selected state of its own. Its items still have to be options, and an
        // option under a plain list is a hierarchy assistive technology may refuse to read the
        // selection from, so the items are what decides.
        List<Long> children = node.getChildIds();
        for (int i = 0; i < children.size(); i++) {
            AccessibilityNodeSnapshot child = nodes.get(children.get(i));
            if (child != null && child.getRole() == AccessibilityRole.LIST_ITEM
                    && child.getSelected() != null) {
                return true;
            }
        }
        return false;
    }

    private String ariaRole(AccessibilityRole role) {
        switch (role) {
            case BUTTON:
            case TOGGLE_BUTTON: return "button";
            case CHECKBOX: return "checkbox";
            case RADIO_BUTTON: return "radio";
            case SWITCH: return "switch";
            case HEADING: return "heading";
            case LINK: return "link";
            case IMAGE: return "img";
            case TEXT_FIELD: return "textbox";
            case SEARCH_FIELD: return "searchbox";
            case SLIDER: return "slider";
            case PROGRESS_BAR: return "progressbar";
            case LIST: return "list";
            case LIST_ITEM: return "listitem";
            case GRID: return "grid";
            case ROW: return "row";
            case CELL: return "gridcell";
            case COLUMN_HEADER: return "columnheader";
            case ROW_HEADER: return "rowheader";
            case TAB_LIST: return "tablist";
            case TAB: return "tab";
            case TAB_PANEL: return "tabpanel";
            case DIALOG: return "dialog";
            case ALERT: return "alert";
            case MENU: return "menu";
            case MENU_ITEM: return "menuitem";
            case TOOLBAR: return "toolbar";
            case SCROLL_BAR: return "scrollbar";
            case SPIN_BUTTON: return "spinbutton";
            case COMBO_BOX: return "combobox";
            case TREE: return "tree";
            case TREE_ITEM: return "treeitem";
            case SEPARATOR: return "separator";
            case GENERIC: return "group";
            default: return null;
        }
    }
}

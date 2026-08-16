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
        private HTMLElement textNode;
        private String geometry;
        private String text;
        private long parentId = -1;
        private boolean listenersBound;
        private boolean activateEnabled;
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
    private boolean textContentEnabled = true;
    private long focusedNodeId = -1;

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
    }

    /**
     * Removes every element from the overlay and drops the retained state.
     */
    public void clear() {
        // Emptying the container detaches the whole subtree in one write. Walking the entries
        // and removing each from its parent would not be equivalent here, because a parent may
        // already have been dropped from the map by the time its children are visited.
        container.setInnerHTML("");
        entries.clear();
        rootOrder.clear();
        focusedNodeId = -1;
    }

    private void visit(Map<Long, AccessibilityNodeSnapshot> nodes, Long id, long parentId,
            Set<Long> live, double ratio) {
        AccessibilityNodeSnapshot node = nodes.get(id);
        if (node == null || !live.add(id)) {
            return;
        }
        Entry entry = obtain(node);
        applyParent(entry, parentId);
        applyAttributes(entry, node);
        applyGeometry(entry, node, ratio);
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

    private void applyAttributes(Entry entry, AccessibilityNodeSnapshot node) {
        Map<String, String> desired = describe(node);
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

    private void applyGeometry(Entry entry, AccessibilityNodeSnapshot node, double ratio) {
        Rectangle bounds = node.getBounds();
        StringBuilder css = new StringBuilder(
                "position:absolute;opacity:0.001;pointer-events:none;overflow:hidden;");
        css.append("left:").append(bounds.getX() / ratio).append("px;");
        css.append("top:").append(bounds.getY() / ratio).append("px;");
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
        if (textContentEnabled && !obscured
                && (node.getRole() == AccessibilityRole.STATIC_TEXT
                || node.getRole() == AccessibilityRole.HEADING)) {
            text = node.getLabel() == null ? "" : node.getLabel();
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
        AccessibilityAction activate = node.getAction(AccessibilityAction.ACTIVATE);
        entry.activateEnabled = activate != null && activate.isEnabled();
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
                } else if (code == 38 || code == 39) {
                    action = AccessibilityAction.INCREMENT;
                } else if (code == 37 || code == 40) {
                    action = AccessibilityAction.DECREMENT;
                }
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
                focusedNodeId = -1;
            }
            return;
        }
        if (focusedNodeId == entry.id) {
            return;
        }
        focusedNodeId = entry.id;
        entry.element.focus();
    }

    private void applyCustomActions(Entry entry, AccessibilityNodeSnapshot node) {
        Set<String> desired = null;
        List<AccessibilityAction> actions = node.getActions();
        for (int i = 0; i < actions.size(); i++) {
            AccessibilityAction action = actions.get(i);
            if (!action.isEnabled() || isStandardWebAction(action.getId())) {
                continue;
            }
            if (desired == null) {
                desired = new HashSet<String>();
            }
            desired.add(action.getId());
            if (entry.customActions == null) {
                entry.customActions = new HashMap<String, HTMLElement>();
            }
            String label = action.getLabel() == null ? action.getId() : action.getLabel();
            HTMLElement existing = entry.customActions.get(action.getId());
            if (existing != null) {
                // An application can replace an action with the same id and a new label -- an
                // Expand that becomes a Collapse. Without this the retained button keeps
                // announcing the old wording until the action is removed entirely.
                if (!label.equals(entry.customActionLabels.get(action.getId()))) {
                    existing.setAttribute("aria-label", label);
                    existing.setTextContent(label);
                    entry.customActionLabels.put(action.getId(), label);
                }
                continue;
            }
            if (entry.customActionLabels == null) {
                entry.customActionLabels = new HashMap<String, String>();
            }
            entry.customActions.put(action.getId(), createCustomAction(entry, action, label));
            entry.customActionLabels.put(action.getId(), label);
        }
        if (entry.customActions == null) {
            return;
        }
        for (Iterator<Map.Entry<String, HTMLElement>> it = entry.customActions.entrySet().iterator();
                it.hasNext();) {
            Map.Entry<String, HTMLElement> existing = it.next();
            if (desired == null || !desired.contains(existing.getKey())) {
                entry.element.removeChild(existing.getValue());
                if (entry.customActionLabels != null) {
                    entry.customActionLabels.remove(existing.getKey());
                }
                it.remove();
            }
        }
    }

    private HTMLElement createCustomAction(Entry entry, final AccessibilityAction action,
            String label) {
        final long nodeId = entry.id;
        HTMLElement button = document.createElement("button");
        button.setAttribute("type", "button");
        button.setAttribute("aria-label", label);
        button.setTextContent(label);
        button.getStyle().setCssText(
                "position:absolute;opacity:0.001;pointer-events:none;width:1px;height:1px;");
        button.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event event) {
                event.preventDefault();
                dispatcher.performAction(nodeId, action.getId(), null);
            }
        });
        entry.element.appendChild(button);
        return button;
    }

    private void pruneRemovedNodes(Set<Long> live) {
        for (Iterator<Map.Entry<Long, Entry>> it = entries.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Long, Entry> existing = it.next();
            if (live.contains(existing.getKey())) {
                continue;
            }
            Entry entry = existing.getValue();
            detach(entry);
            unlinkFromParentOrder(entry);
            it.remove();
        }
    }

    private void unlinkFromParentOrder(Entry entry) {
        if (focusedNodeId == entry.id) {
            // Detaching the element moves browser focus to the document, and accessibility ids
            // are stable per component, so without this the node would be considered still
            // focused when it comes back and focus would never be restored to it.
            focusedNodeId = -1;
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

    private boolean isStandardWebAction(String id) {
        return AccessibilityAction.ACTIVATE.equals(id) || AccessibilityAction.FOCUS.equals(id)
                || AccessibilityAction.INCREMENT.equals(id) || AccessibilityAction.DECREMENT.equals(id)
                || AccessibilityAction.SET_TEXT.equals(id) || AccessibilityAction.SCROLL_FORWARD.equals(id)
                || AccessibilityAction.SCROLL_BACKWARD.equals(id);
    }

    /**
     * Builds the full ARIA attribute set for a node. The result is diffed against the last
     * applied set, so this method describes rather than writes.
     */
    private Map<String, String> describe(AccessibilityNodeSnapshot node) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        out.put(ATTRIBUTE_NODE_ID, String.valueOf(node.getId()));
        String role = ariaRole(node.getRole());
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
        if (obscured && accessibleName != null && accessibleName.equals(node.getValue())) {
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
        if (node.getIdentifier() != null) {
            out.put("id", node.getIdentifier());
        }
        if (node.getRoleDescription() != null) {
            out.put("aria-roledescription", node.getRoleDescription());
        }
        if (node.getValue() != null && !obscured) {
            out.put("aria-valuetext", node.getValue());
        }
        if (node.getSelected() != null) {
            out.put("aria-selected", String.valueOf(node.getSelected()));
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

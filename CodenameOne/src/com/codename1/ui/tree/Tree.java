/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.tree;

import com.codename1.compat.java.util.Objects;
import com.codename1.components.SpanButton;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Image;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/// The `Tree` component allows constructing simple tree component hierarchies that can be expanded
/// seamlessly with no limit. The tree is bound to a model that can provide data with free form depth such as file system
/// or similarly structured data.
///
/// To customize the look of the tree the component can be derived and component creation can be replaced.
///
/// ```java
/// class StringArrayTreeModel implements TreeModel {
///     String[][] arr = new String[][] {
///             {"Colors", "Letters", "Numbers"},
///             {"Red", "Green", "Blue"},
///             {"A", "B", "C"},
///             {"1", "2", "3"}
///         };
///
///     public Vector getChildren(Object parent) {
///         if(parent == null) {
///             Vector v = new Vector();
///             for(int iter = 0 ; iter  iter + 1 && arr[iter + 1] != null) {
///                     for(int i = 0 ; i
///
/// And heres a more "real world" example showing an XML hierarchy in a `Tree`:
///
/// ```java
/// class XMLTreeModel implements TreeModel {
///     private Element root;
///     public XMLTreeModel(Element e) {
///         root = e;
///     }
///
///     public Vector getChildren(Object parent) {
///         if(parent == null) {
///             Vector c = new Vector();
///             c.addElement(root);
///             return c;
///         }
///         Vector result = new Vector();
///         Element e = (Element)parent;
///         for(int iter = 0 ; iter
///
/// Another real world example showing the `com.codename1.io.FileSystemStorage` as a tree:
///
/// ```java
/// Form hi = new Form("FileSystemTree", new BorderLayout());
/// TreeModel tm = new TreeModel() {
/// @Override
///     public Vector getChildren(Object parent) {
///         String[] files;
///         if(parent == null) {
///             files = FileSystemStorage.getInstance().getRoots();
///             return new Vector(Arrays.asList(files));
///         } else {
///             try {
///                 files = FileSystemStorage.getInstance().listFiles((String)parent);
///             } catch(IOException err) {
///                 Log.e(err);
///                 files = new String[0];
///             }
///         }
///         String p = (String)parent;
///         Vector result = new Vector();
///         for(String s : files) {
///             result.add(p + s);
///         }
///         return result;
///     }
/// @Override
///     public boolean isLeaf(Object node) {
///         return !FileSystemStorage.getInstance().isDirectory((String)node);
///     }
/// };
/// Tree t = new Tree(tm) {
/// @Override
///     protected String childToDisplayLabel(Object child) {
///         String n = (String)child;
///         int pos = n.lastIndexOf("/");
///         if(pos
/// @author Shai Almog
public class Tree extends Container {
    private static final String KEY_OBJECT = "TREE_OBJECT";
    private static final String KEY_PARENT = "TREE_PARENT";
    private static final String KEY_EXPANDED = "TREE_NODE_EXPANDED";
    private static final String KEY_DEPTH = "TREE_DEPTH";
    private static final int depthIndent = 2;
    private static Image folder;
    private static Image openFolder;
    private static Image nodeImage;
    private final EventDispatcher leafListener = new EventDispatcher();
    private final ActionListener expansionListener = new Handler();
    private TreeModel model;
    private boolean multilineMode;

    /// Constructor for usage by GUI builder and automated tools, normally one
    /// should use the version that accepts the model
    public Tree() {
        this(new StringArrayTreeModel(new String[][]{
                {"Colors", "Letters", "Numbers"},
                {"Red", "Green", "Blue"},
                {"A", "B", "C"},
                {"1", "2", "3"}
        }));
    }

    /// Construct a tree with the given tree model
    ///
    /// #### Parameters
    ///
    /// - `model`: represents the contents of the tree
    public Tree(TreeModel model) {
        this.model = model;
        setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        initDefaultIcons();
        buildBranch(null, 0, this);
        setScrollableY(true);
        setUIIDFinal("Tree");
    }

    private static synchronized void initDefaultIcons() {
        if (folder == null) {
            folder = UIManager.getInstance().getThemeImageConstant("treeFolderImage");
            openFolder = UIManager.getInstance().getThemeImageConstant("treeFolderOpenImage");
            nodeImage = UIManager.getInstance().getThemeImageConstant("treeNodeImage");
        }
    }

    /// Sets the icon for a tree folder
    ///
    /// #### Parameters
    ///
    /// - `folderIcon`: the icon for a folder within the tree
    public static void setFolderIcon(Image folderIcon) {
        folder = folderIcon;
    }

    /// Sets the icon for a tree folder in its expanded state
    ///
    /// #### Parameters
    ///
    /// - `folderIcon`: the icon for a folder within the tree
    public static void setFolderOpenIcon(Image folderIcon) {
        openFolder = folderIcon;
    }

    /// Sets the icon for a tree node
    ///
    /// #### Parameters
    ///
    /// - `nodeIcon`: the icon for a node within the tree
    public static void setNodeIcon(Image nodeIcon) {
        nodeImage = nodeIcon;
    }

    /// Gets the state of the tree in a format that can be restored later
    /// by either the same tree or a different tree whose model includes the same
    /// nodes.
    ///
    /// #### Returns
    ///
    /// A TreeState object that can be passed to `#setTreeState(com.codename1.ui.tree.Tree.TreeState)`
    public TreeState getTreeState() {
        State out = new State();
        out.extractStateFrom(this);
        return out;
    }

    /// Sets the tree state.
    ///
    /// #### Parameters
    ///
    /// - `state`: The state, which was returned from the `#getTreeState()` method.
    public void setTreeState(TreeState state) {
        if (state instanceof State) {
            ((State) state).applyStateTo(this);
        }
    }

    /// Toggles a mode where rows in the tree can be broken since span buttons will
    /// be used instead of plain buttons.
    ///
    /// #### Returns
    ///
    /// the multilineMode
    public boolean isMultilineMode() {
        return multilineMode;
    }

    /// Toggles a mode where rows in the tree can be broken since span buttons will
    /// be used instead of plain buttons.
    ///
    /// #### Parameters
    ///
    /// - `multilineMode`: the multilineMode to set
    public void setMultilineMode(boolean multilineMode) {
        this.multilineMode = multilineMode;
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyNames() {
        return new String[]{"data"};
    }

    /// {@inheritDoc}
    @Override
    public Class[] getPropertyTypes() {
        return new Class[]{com.codename1.impl.CodenameOneImplementation.getStringArray2DClass()};
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyTypeNames() {
        return new String[]{"String[][]"};
    }

    /// {@inheritDoc}
    @Override
    public Object getPropertyValue(String name) {
        if ("data".equals(name)) {
            return ((StringArrayTreeModel) model).arr;
        }
        return null;
    }

    /// {@inheritDoc}
    @Override
    public String setPropertyValue(String name, Object value) {
        if ("data".equals(name)) {
            setModel(new StringArrayTreeModel((String[][]) value));
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /// Returns the tree model instance
    ///
    /// #### Returns
    ///
    /// the tree model
    public TreeModel getModel() {
        return model;
    }

    /// Sets the tree model to a new value
    ///
    /// #### Parameters
    ///
    /// - `model`: the model of the tree
    public void setModel(TreeModel model) {
        this.model = model;
        removeAll();
        buildBranch(null, 0, this);
    }

    private Container expandNode(boolean animate, Component c) {
        return expandNode(animate, c, true);
    }

    private Container expandNode(boolean animate, Component c, boolean revalidate) {
        return expandNodeImpl(animate, c, revalidate);
    }

    private Container expandNodeImpl(boolean animate, Component c) {
        return expandNodeImpl(animate, c, true);
    }

    private Container expandNodeImpl(boolean animate, Component c, boolean revalidate) {
        Container p = c.getParent().getLeadParent();
        if (p != null) {
            c = p;
        }
        c.putClientProperty(KEY_EXPANDED, "true");
        if (openFolder == null) {
            setNodeMaterialIcon(FontImage.MATERIAL_FOLDER, c, 3);
        } else {
            setNodeIcon(openFolder, c);
        }
        int depth = ((Integer) c.getClientProperty(KEY_DEPTH)).intValue();
        Container parent = c.getParent();
        Object o = c.getClientProperty(KEY_OBJECT);
        Container dest = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        parent.addComponent(BorderLayout.CENTER, dest);
        buildBranch(o, depth, dest);
        if (isInitialized() && animate) {
            dest.setHeight(0);
            dest.setVisible(true);
            animateLayoutAndWait(300);
            //parent.animateHierarchyAndWait(300);
            if (multilineMode) {
                revalidate();
            }
        } else {
            if (revalidate) {
                parent.revalidate();
            }
        }
        return dest;
    }

    /// This method returns true if the given node is expanded.
    ///
    /// #### Parameters
    ///
    /// - `node`: a Component that represents a tree node.
    ///
    /// #### Returns
    ///
    /// true if this tree node is expanded
    protected boolean isExpanded(Component node) {
        Object e = node.getClientProperty(KEY_EXPANDED);
        return e != null && "true".equals(e);
    }

    private Container expandPathNode(boolean animate, Container parent, Object node) {
        int cc = parent.getComponentCount();
        for (int iter = 0; iter < cc; iter++) {
            Component current = parent.getComponentAt(iter);
            Object o = current.getClientProperty(KEY_OBJECT);

            if (!model.isLeaf(o)) {
                //if(current instanceof Container) {
                BorderLayout bl = (BorderLayout) ((Container) current).getLayout();

                // the tree component is always at north expanded or otherwise
                current = bl.getNorth();
                if (Objects.equals(o, node)) {
                    if (isExpanded(current)) {
                        return (Container) bl.getCenter();
                    }
                    return expandNodeImpl(animate, current);
                }
            }
        }
        return null;
    }

    private void collapsePathNode(Container parent, Object node) {
        int cc = parent.getComponentCount();
        for (int iter = 0; iter < cc; iter++) {
            Component current = parent.getComponentAt(iter);
            if (isExpanded(current)) {
                BorderLayout bl = (BorderLayout) ((Container) current).getLayout();

                // the tree component is always at north expanded or otherwise
                current = bl.getNorth();
                Object o = current.getClientProperty(KEY_OBJECT);
                if (o != null && o.equals(node)) {
                    if (isExpanded(current)) {
                        collapseNode(current, null);
                    }
                    return;
                }
            }
        }
    }

    /// Finds the component for a model node.
    ///
    /// #### Parameters
    ///
    /// - `node`: The node from the model.
    ///
    /// #### Returns
    ///
    /// The corresponding component in the UI or null if not found.
    ///
    /// #### Since
    ///
    /// 7.0
    public Component findNodeComponent(Object node) {
        return findNodeComponent(node, this);
    }

    /// Finds the component for a model node.
    ///
    /// #### Parameters
    ///
    /// - `node`: Model node whose view we seek.
    ///
    /// - `root`: A root component - we check root and its descendents.
    ///
    /// #### Returns
    ///
    /// The corresponding UI component for node, or null if not found.
    ///
    /// #### Since
    ///
    /// 7.0
    public Component findNodeComponent(Object node, Component root) {
        if (root == null) {
            return findNodeComponent(node, this);
        }
        Object rootNode = root.getClientProperty(KEY_OBJECT);

        if (node.equals(rootNode)) {
            return root;
        }
        if (root instanceof Container) {
            int len = ((Container) root).getComponentCount();
            for (int i = 0; i < len; i++) {
                Component found = findNodeComponent(node, ((Container) root).getComponentAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /// Expands the tree path
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to expand
    public void expandPath(Object... path) {
        expandPath(isInitialized(), path);
    }

    /// Expands the tree path
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to expand
    ///
    /// - `animate`: whether to animate expansion
    public void expandPath(boolean animate, Object... path) {
        Container c = this;
        int plen = path.length;
        for (int iter = 0; iter < plen; iter++) {
            c = expandPathNode(animate, c, path[iter]);
            if (c == null) {
                return;
            }
        }
    }

    /// Collapses the last element in the path
    ///
    /// #### Parameters
    ///
    /// - `path`: the path to the element that should be collapsed
    public void collapsePath(Object... path) {
        Container c = this;
        int plen = path.length;
        for (int iter = 0; iter < plen - 1; iter++) {
            c = expandPathNode(isInitialized(), c, path[iter]);
        }
        collapsePathNode(c, path[plen - 1]);
    }

    private void collapseNode(Component c) {
        collapseNode(c, CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, false, 300));
    }

    private void collapseNode(Component c, Transition t) {
        Container lead = c.getParent().getLeadParent();
        if (lead != null) {
            c = lead;
        }
        c.putClientProperty(KEY_EXPANDED, null);
        if (folder == null) {
            setNodeMaterialIcon(FontImage.MATERIAL_FOLDER, c, 3);
        } else {
            setNodeIcon(folder, c);
        }
        Container p = c.getParent();
        for (int iter = 0; iter < p.getComponentCount(); iter++) {
            if (p.getComponentAt(iter) != c) { //NOPMD CompareObjectsWithEquals
                if (t == null) {
                    p.removeComponent(p.getComponentAt(iter));
                    break; // there should only be one container with all children
                } else {
                    Component dest = p.getComponentAt(iter);
                    dest.setHidden(true);

                    animateLayoutAndWait(300);
                    p.removeComponent(dest);

                }
            }
        }
    }

    /// Returns the currently selected item in the tree
    ///
    /// #### Returns
    ///
    /// the object selected within the tree
    public Object getSelectedItem() {
        Component c = getComponentForm().getFocused();
        if (c != null) {
            return c.getClientProperty(KEY_OBJECT);
        }
        return null;
    }

    /// Gets the parent model node for a component.
    ///
    /// #### Parameters
    ///
    /// - `nodeComponent`: The UI for a node.
    ///
    /// #### Returns
    ///
    /// The model node's parent, or null if not found.
    public Object getParentNode(Component nodeComponent) {
        if (nodeComponent == null) {
            return null;
        }
        return nodeComponent.getClientProperty(KEY_PARENT);

    }

    /// Gets the UI component corresponding to the parent model mode of the node
    /// corresponding with the given UI component.
    ///
    /// #### Parameters
    ///
    /// - `nodeComponent`: UI component, whose node we seek the parent.
    ///
    /// #### Returns
    ///
    /// UI component for the given node's parent.
    ///
    /// #### Since
    ///
    /// 7.0
    public Component getParentComponent(Component nodeComponent) {
        if (nodeComponent == null) {
            return null;
        }
        return findNodeComponent(getParentNode(nodeComponent));
    }

    /// Refreshes a node of the tree.
    ///
    /// #### Parameters
    ///
    /// - `nodeComponent`: The node component.
    ///
    /// #### Since
    ///
    /// 7.0
    public void refreshNode(Component nodeComponent) {
        if (nodeComponent == null) {
            throw new IllegalArgumentException("refreshNode expects a non-null argument");
        }
        Object node = nodeComponent.getClientProperty(KEY_OBJECT);
        if (node == null) {
            return;
        }
        Object nodeParent = nodeComponent.getClientProperty(KEY_PARENT);
        int depth = ((Integer) nodeComponent.getClientProperty(KEY_DEPTH)).intValue();
        Container parentCnt = nodeComponent.getParent();
        if (parentCnt == null) {
            return;
        }
        boolean expanded = isExpanded(nodeComponent);
        Component newCmp = createNode(node, depth);
        Object current = node;
        newCmp.putClientProperty(KEY_OBJECT, current);
        newCmp.putClientProperty(KEY_PARENT, nodeParent);
        newCmp.putClientProperty(KEY_DEPTH, depth);
        newCmp.getAllStyles().setMarginLeft(nodeComponent.getStyle().getMarginLeft(nodeComponent.isRTL()));
        if (model.isLeaf(current)) {
            parentCnt.replace(nodeComponent, newCmp, null);
            bindNodeListener(new Handler(current), newCmp);
        } else {
            Container componentArea = new Container(new BorderLayout());
            componentArea.addComponent(BorderLayout.NORTH, newCmp);
            parentCnt.getParent().replace(parentCnt, componentArea, null);
            bindNodeListener(expansionListener, newCmp);
            if (expanded) {
                expandNode(false, newCmp, false);
            }
        }


        revalidateLater();
    }

    /// Adds the child components of a tree branch to the given container.
    private void buildBranch(Object parent, int depth, Container destination) {
        Vector children = model.getChildren(parent);
        int size = children.size();
        Integer depthVal = Integer.valueOf(depth + 1);
        for (int iter = 0; iter < size; iter++) {
            final Object current = children.elementAt(iter);
            Component nodeComponent = createNode(current, depth);
            if (model.isLeaf(current)) {
                destination.addComponent(nodeComponent);
                bindNodeListener(new Handler(current), nodeComponent);
            } else {
                Container componentArea = new Container(new BorderLayout());
                componentArea.addComponent(BorderLayout.NORTH, nodeComponent);
                destination.addComponent(componentArea);
                bindNodeListener(expansionListener, nodeComponent);
            }
            nodeComponent.putClientProperty(KEY_OBJECT, current);
            nodeComponent.putClientProperty(KEY_PARENT, parent);
            nodeComponent.putClientProperty(KEY_DEPTH, depthVal);
        }
    }

    /// Creates a node within the tree, this method is protected allowing tree to be
    /// subclassed to replace the rendering logic of individual tree buttons.
    ///
    /// #### Parameters
    ///
    /// - `node`: the node object from the model to display on the button
    ///
    /// - `depth`: the depth within the tree (normally represented by indenting the entry)
    ///
    /// #### Returns
    ///
    /// a button representing the node within the tree
    ///
    /// #### Deprecated
    ///
    /// replaced with createNode, bindNodeListener and setNodeIcon
    protected Button createNodeComponent(Object node, int depth) {
        Button cmp = new Button(childToDisplayLabel(node));
        cmp.setUIID("TreeNode");
        if (model.isLeaf(node)) {
            if (nodeImage == null) {
                FontImage.setMaterialIcon(cmp, FontImage.MATERIAL_DESCRIPTION, 3);
            } else {
                cmp.setIcon(nodeImage);
            }
        } else {
            if (folder == null) {
                FontImage.setMaterialIcon(cmp, FontImage.MATERIAL_FOLDER, 3);
            } else {
                cmp.setIcon(folder);
            }
        }
        updateNodeComponentStyle(cmp.getAllStyles(), depth);
        return cmp;
    }

    /// Since a node may be any component type developers should override this method to
    /// add support for binding the click listener to the given component.
    ///
    /// #### Parameters
    ///
    /// - `l`: listener interface
    ///
    /// - `node`: node component returned by createNode
    protected void bindNodeListener(ActionListener l, Component node) {
        if (node instanceof Button) {
            ((Button) node).addActionListener(l);
            return;
        }
        ((SpanButton) node).addActionListener(l);
    }

    /// Sets the icon for the given node similar in scope to bindNodeListener
    ///
    /// #### Parameters
    ///
    /// - `icon`: the icon for the node
    ///
    /// - `node`: the node instance
    protected void setNodeIcon(Image icon, Component node) {
        if (node instanceof Button) {
            ((Button) node).setIcon(icon);
            return;
        }
        ((SpanButton) node).setIcon(icon);
    }

    /// Sets material icon for the node.
    ///
    /// #### Parameters
    ///
    /// - `c`: Material icon code.  See `FontImage`
    ///
    /// - `node`: The node to set the icon for.
    ///
    /// - `size`: The size in millimetres for the icon.
    ///
    /// #### Since
    ///
    /// 7.0
    protected void setNodeMaterialIcon(char c, Component node, float size) {
        FontImage.setMaterialIcon(node, FontImage.MATERIAL_FOLDER, 3);
    }

    /// Creates a node within the tree, this method is protected allowing tree to be
    /// subclassed to replace the rendering logic of individual tree buttons.
    ///
    /// #### Parameters
    ///
    /// - `node`: the node object from the model to display on the button
    ///
    /// - `depth`: the depth within the tree (normally represented by indenting the entry)
    ///
    /// #### Returns
    ///
    /// a button representing the node within the tree
    protected Component createNode(Object node, int depth) {
        if (multilineMode) {
            SpanButton cmp = new SpanButton(childToDisplayLabel(node));
            cmp.setUIID("TreeNode");
            cmp.setTextUIID("TreeNode");
            if (model.isLeaf(node)) {
                cmp.setIcon(nodeImage);
            } else {
                cmp.setIcon(folder);
            }
            updateNodeComponentStyle(cmp.getAllStyles(), depth);
            return cmp;
        }
        return createNodeComponent(node, depth);
    }

    private void updateNodeComponentStyle(Style s, int depth) {
        s.setMarginUnit(Style.UNIT_TYPE_DIPS);
        s.setMarginLeft(depth * depthIndent);
    }

    /// Converts a tree child to a label, this method can be overriden for
    /// simple rendering effects
    ///
    /// #### Returns
    ///
    /// a string representing the given tree node
    protected String childToDisplayLabel(Object child) {
        return child.toString();
    }

    /// A listener that fires when a leaf is clicked
    ///
    /// #### Parameters
    ///
    /// - `l`: listener to fire when the leaf is clicked
    public void addLeafListener(ActionListener l) {
        leafListener.addListener(l);
    }

    /// Removes the listener that fires when a leaf is clicked
    ///
    /// #### Parameters
    ///
    /// - `l`: listener to remove
    public void removeLeafListener(ActionListener l) {
        leafListener.removeListener(l);
    }

    /// Gets the model for a component in the tree.
    ///
    /// #### Parameters
    ///
    /// - `node`: The component whose model we want to obtain.
    ///
    /// #### Returns
    ///
    /// The model.
    ///
    /// #### Since
    ///
    /// 7.0
    protected Object getModel(Component node) {
        return node.getClientProperty(KEY_OBJECT);
    }

    /// {@inheritDoc}
    @Override
    protected Dimension calcPreferredSize() {
        Dimension d = super.calcPreferredSize();

        // if the tree is entirely collapsed try to reserve at least 6 rows for the content
        int count = getComponentCount();
        for (int iter = 0; iter < count; iter++) {
            if (getComponentAt(iter) instanceof Container) {
                return d;
            }
        }
        int size = Math.max(1, model.getChildren(null).size());
        if (size < 6) {
            return new Dimension(Math.max(d.getWidth(), Display.getInstance().getDisplayWidth() / 4 * 3),
                    d.getHeight() / size * 6);
        }
        return d;
    }

    /// A marker interface used for Tree state returned from `#getTreeState()` and
    /// passed to `#setTreeState(com.codename1.ui.tree.Tree.TreeState)` for retaining
    /// state in a Tree when the model is changed.
    public interface TreeState {

    }

    private static class State implements TreeState {
        Set<Object> expandedSet = new HashSet<Object>();

        private void extractStateFrom(final Tree tree) {
            expandedSet.clear();
            ComponentSelector.select("*", tree).each(new ComponentSelector.ComponentClosure() {

                @Override
                public void call(Component c) {
                    if (tree.isExpanded(c)) {
                        Object o = c.getClientProperty(KEY_OBJECT);
                        expandedSet.add(o);
                    }
                }
            });


        }


        private void applyStateTo(Tree tree) {
            applyStateTo(tree, tree);
        }

        private void applyStateTo(final Tree tree, Container parent) {
            ComponentSelector.select("*", parent).each(new ComponentSelector.ComponentClosure() {

                @Override
                public void call(Component cmp) {
                    Object o = cmp.getClientProperty(KEY_OBJECT);

                    if (o != null) {
                        if (expandedSet.contains(o)) {
                            if (!tree.isExpanded(cmp)) {
                                Container dest = tree.expandNode(false, cmp, false);
                                applyStateTo(tree, dest);

                            }

                        } else {
                            if (tree.isExpanded(cmp)) {
                                tree.collapseNode(cmp, null);
                            }
                        }

                    }
                }

            });

        }
    }

    static class StringArrayTreeModel implements TreeModel {
        String[][] arr;

        StringArrayTreeModel(String[][] arr) {
            this.arr = arr;
        }

        @Override
        public Vector getChildren(Object parent) {
            if (parent == null) {
                Vector v = new Vector();
                int a0len = arr[0].length;
                for (int iter = 0; iter < a0len; iter++) {
                    v.addElement(arr[0][iter]);
                }
                return v;
            }
            int alen = arr.length;
            int aolen = arr[0].length;
            Vector v = new Vector();
            for (int iter = 0; iter < aolen; iter++) {
                if (parent.equals(arr[0][iter])) {
                    if (alen > iter + 1 && arr[iter + 1] != null) {
                        int ailen = arr[iter + 1].length;
                        for (int i = 0; i < ailen; i++) {
                            v.addElement(arr[iter + 1][i]);
                        }
                    }
                }
            }
            return v;
        }

        @Override
        public boolean isLeaf(Object node) {
            Vector v = getChildren(node);
            return v.isEmpty();
        }
    }

    /// This class unifies two action listeners into a single class to reduce the size overhead
    private class Handler implements ActionListener {
        private Object current;

        public Handler() {
        }

        public Handler(Object current) {
            this.current = current;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (current != null) {
                leafListener.fireActionEvent(new ActionEvent(current, ActionEvent.Type.Other));
                return;
            }
            Component c = (Component) evt.getSource();
            Container lead = c.getParent().getLeadParent();
            if (lead != null) {
                c = lead;
            }
            Object e = c.getClientProperty(KEY_EXPANDED);
            if (e != null && "true".equals(e)) {
                collapseNode(c);
            } else {
                expandNode(isInitialized(), c);
            }
        }
    }
}

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

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;
import java.util.Vector;

/**
 * The tree component allows constructing simple tree component hierechies that can be expaneded seamingly
 * with no limit. The tree is bound to a model that can provide data with free form depth such as file system
 * or similarly structured data.
 * To customize the look of the tree the component can be derived and component creation can be replaced.
 *
 * @author Shai Almog
 */
public class Tree extends Container {
    private static final String KEY_OBJECT = "TREE_OBJECT";
    private static final String KEY_PARENT = "TREE_PARENT";
    private static final String KEY_EXPANDED = "TREE_NODE_EXPANDED";
    private static final String KEY_DEPTH = "TREE_DEPTH";
    private EventDispatcher leafListener = new EventDispatcher();

    private ActionListener expansionListener = new Handler();
    private TreeModel model;
    private static Image folder;
    private static Image openFolder;
    private static Image nodeImage;
    private int depthIndent = 15;
    /**
     * Constructor for usage by GUI builder and automated tools, normally one
     * should use the version that accepts the model
     */
    public Tree() {
        this(new StringArrayTreeModel(new String[][] {
            {"Colors", "Letters", "Numbers"},
            {"Red", "Green", "Blue"},
            {"A", "B", "C"},
            {"1", "2", "3"}
        }));
    }

    static class StringArrayTreeModel implements TreeModel {
        String[][] arr;
        StringArrayTreeModel(String[][] arr) {
            this.arr = arr;
        }

        public Vector getChildren(Object parent) {
            if(parent == null) {
                Vector v = new Vector();
                for(int iter = 0 ; iter < arr[0].length ; iter++) {
                    v.addElement(arr[0][iter]);
                }
                return v;
            }
            Vector v = new Vector();
            for(int iter = 0 ; iter < arr[0].length ; iter++) {
                if(parent == arr[0][iter]) {
                    if(arr.length > iter + 1 && arr[iter + 1] != null) {
                        for(int i = 0 ; i < arr[iter + 1].length ; i++) {
                            v.addElement(arr[iter + 1][i]);
                        }
                    }
                }
            }
            return v;
        }

        public boolean isLeaf(Object node) {
            Vector v = getChildren(node);
            return v == null || v.size() == 0;
        }
    }

    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[] {"data"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
       return new Class[] {com.codename1.impl.CodenameOneImplementation.getStringArray2DClass()};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if(name.equals("data")) {
            return ((StringArrayTreeModel)model).arr;
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if(name.equals("data")) {
            setModel(new StringArrayTreeModel((String[][])value));
            return null;
        }
        return super.setPropertyValue(name, value);
    }

    /**
     * Construct a tree with the given tree model
     *
     * @param model represents the contents of the tree
     */
    public Tree(TreeModel model) {
        this.model = model;
        setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        buildBranch(null, 0, this);
        setScrollableY(true);
        setUIID("Tree");
    }

    /**
     * Returns the tree model instance
     *
     * @return the tree model
     */
    public TreeModel getModel() {
        return model;
    }

    /**
     * Sets the tree model to a new value
     *
     * @param model the model of the tree
     */
    public void setModel(TreeModel model) {
        this.model = model;
        removeAll();
        buildBranch(null, 0, this);
    }

    /**
     * Sets the icon for a tree folder 
     * 
     * @param folderIcon the icon for a folder within the tree
     */
    public static void setFolderIcon(Image folderIcon) {
        folder = folderIcon;
    }


    /**
     * Sets the icon for a tree folder in its expanded state
     *
     * @param folderIcon the icon for a folder within the tree
     */
    public static void setFolderOpenIcon(Image folderIcon) {
        openFolder = folderIcon;
    }


    /**
     * Sets the icon for a tree node
     *
     * @param nodeIcon the icon for a node within the tree
     */
    public static void setNodeIcon(Image nodeIcon) {
        nodeImage = nodeIcon;
    }

    private void expandNode(Component c) {
        c.putClientProperty(KEY_EXPANDED, "true");
        ((Button)c).setIcon(openFolder);
        int depth = ((Integer)c.getClientProperty(KEY_DEPTH)).intValue();
        Container parent = c.getParent();
        Object o = c.getClientProperty(KEY_OBJECT);
        Container dest = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        Label dummy = new Label();
        parent.addComponent(BorderLayout.CENTER, dummy);
        buildBranch(o, depth, dest);
        parent.replace(dummy, dest, CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, true, 300));
    }

    private void collapseNode(Component c) {
        c.putClientProperty(KEY_EXPANDED, null);
        ((Button)c).setIcon(folder);
        Container p = c.getParent();
        for(int iter = 0 ; iter < p.getComponentCount() ; iter++) {
            if(p.getComponentAt(iter) != c) {
                Label dummy = new Label();
                p.replaceAndWait(p.getComponentAt(iter), dummy, CommonTransitions.createSlide(CommonTransitions.SLIDE_VERTICAL, false, 300), true);
                p.removeComponent(dummy);
            }
        }
    }

    /**
     * Returns the currently selected item in the tree
     *
     * @return the object selected within the tree
     */
    public Object getSelectedItem() {
        Component c = getComponentForm().getFocused();
        if(c != null) {
            return c.getClientProperty(KEY_OBJECT);
        }
        return null;
    }

    /**
     * Adds the child components of a tree branch to the given container.
     */
    private void buildBranch(Object parent, int depth, Container destination) {
        Vector children = model.getChildren(parent);
        int size = children.size();
        Integer depthVal = new Integer(depth + 1);
        for(int iter = 0 ; iter < size ; iter++) {
            final Object current = children.elementAt(iter);
            Button nodeComponent = createNodeComponent(current, depth);
            if(model.isLeaf(current)) {
                destination.addComponent(nodeComponent);
                nodeComponent.addActionListener(new Handler(current));
            } else {
                Container componentArea = new Container(new BorderLayout());
                componentArea.addComponent(BorderLayout.NORTH, nodeComponent);
                destination.addComponent(componentArea);
                nodeComponent.addActionListener(expansionListener);
            }
            nodeComponent.putClientProperty(KEY_OBJECT, current);
            nodeComponent.putClientProperty(KEY_PARENT, parent);
            nodeComponent.putClientProperty(KEY_DEPTH, depthVal);
        }
    }

    /**
     * Creates a node within the tree, this method is protected allowing tree to be
     * subclassed to replace the rendering logic of individual tree buttons.
     *
     * @param node the node object from the model to display on the button
     * @param depth the depth within the tree (normally represented by indenting the entry)
     * @return a button representing the node within the tree
     */
    protected Button createNodeComponent(Object node, int depth) {
        Button cmp = new Button(childToDisplayLabel(node));
        cmp.setUIID("TreeNode");
        if(model.isLeaf(node)) {
            cmp.setIcon(nodeImage);
        } else {
            cmp.setIcon(folder);
        }
        updateNodeComponentStyle(cmp.getSelectedStyle(), depth);
        updateNodeComponentStyle(cmp.getUnselectedStyle(), depth);
        updateNodeComponentStyle(cmp.getPressedStyle(), depth);
        return cmp;
    }

    private void updateNodeComponentStyle(Style s, int depth) {
        s.setMargin(LEFT, depth * depthIndent);
    }

    /**
     * Converts a tree child to a label, this method can be overriden for
     * simple rendering effects
     *
     * @return a string representing the given tree node
     */
    protected String childToDisplayLabel(Object child) {
        return child.toString();
    }

    /**
     * A listener that fires when a leaf is clicked
     *
     * @param l listener to fire when the leaf is clicked
     */
    public void addLeafListener(ActionListener l) {
        leafListener.addListener(l);
    }

    /**
     * Removes the listener that fires when a leaf is clicked
     *
     * @param l listener to remove
     */
    public void removeLeafListener(ActionListener l) {
        leafListener.removeListener(l);
    }

    /**
     * @inheritDoc
     */
    protected Dimension calcPreferredSize() {
        Dimension d = super.calcPreferredSize();

        // if the tree is entirely collapsed try to reserve at least 6 rows for the content
        int count = getComponentCount();
        for(int iter = 0 ; iter < count ; iter++) {
            if(getComponentAt(iter) instanceof Container) {
                return d;
            }
        }
        int size = model.getChildren(null).size();
        if(size < 6) {
            return new Dimension(Math.max(d.getWidth(), Display.getInstance().getDisplayWidth() / 4 * 3),
                    d.getHeight() / size * 6);
        }
        return d;
    }

    /**
     * This class unifies two action listeners into a single class to reduce the size overhead
     */
    private class Handler implements ActionListener {
        private Object current;
        public Handler() {
        }

        public Handler(Object current) {
            this.current = current;
        }

        public void actionPerformed(ActionEvent evt) {
            if(current != null) {
                leafListener.fireActionEvent(new ActionEvent(current));
                return;
            }
            Component c = (Component)evt.getSource();
            Object e = c.getClientProperty(KEY_EXPANDED);
            if(e != null && e.equals("true")) {
                collapseNode(c);
            } else {
                expandNode(c);
            }
        }
    }
}

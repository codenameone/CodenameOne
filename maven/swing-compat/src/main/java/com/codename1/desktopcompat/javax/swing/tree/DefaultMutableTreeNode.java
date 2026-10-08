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
package com.codename1.desktopcompat.javax.swing.tree;

import com.codename1.desktopcompat.rt.ListEnumeration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

/// The general tree node: a user object, a parent and a list of children,
/// with the JDK's ways of walking the tree around it.
///
/// [#clone()] answers a plain `DefaultMutableTreeNode` with the same user
/// object and no parent or children, also for a subclass: a device cannot
/// copy an object of an unknown class.
public class DefaultMutableTreeNode implements Cloneable, MutableTreeNode {

    /// An enumeration with nothing in it.
    public static final Enumeration<TreeNode> EMPTY_ENUMERATION = new Enumeration<TreeNode>() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public TreeNode nextElement() {
            throw new NoSuchElementException("No more elements");
        }
    };

    protected MutableTreeNode parent;
    protected Vector children;
    protected transient Object userObject;
    protected boolean allowsChildren;

    public DefaultMutableTreeNode() {
        this(null);
    }

    public DefaultMutableTreeNode(Object userObject) {
        this(userObject, true);
    }

    public DefaultMutableTreeNode(Object userObject, boolean allowsChildren) {
        parent = null;
        this.allowsChildren = allowsChildren;
        this.userObject = userObject;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void insert(MutableTreeNode newChild, int childIndex) {
        if (!allowsChildren) {
            throw new IllegalStateException("node does not allow children");
        } else if (newChild == null) {
            throw new IllegalArgumentException("new child is null");
        } else if (isNodeAncestor(newChild)) {
            throw new IllegalArgumentException("new child is an ancestor");
        }
        TreeNode oldParent = newChild.getParent();
        if (oldParent instanceof MutableTreeNode) {
            ((MutableTreeNode) oldParent).remove(newChild);
        }
        newChild.setParent(this);
        if (children == null) {
            children = new Vector();
        }
        children.insertElementAt(newChild, childIndex);
    }

    @Override
    public void remove(int childIndex) {
        TreeNode child = getChildAt(childIndex);
        children.removeElementAt(childIndex);
        if (child instanceof MutableTreeNode) {
            ((MutableTreeNode) child).setParent(null);
        }
    }

    @Override
    public void setParent(MutableTreeNode newParent) {
        parent = newParent;
    }

    @Override
    public TreeNode getParent() {
        return parent;
    }

    @Override
    public TreeNode getChildAt(int index) {
        if (children == null) {
            throw new ArrayIndexOutOfBoundsException("node has no children");
        }
        Object c = children.elementAt(index);
        return c instanceof TreeNode ? (TreeNode) c : null;
    }

    @Override
    public int getChildCount() {
        return children == null ? 0 : children.size();
    }

    @Override
    public int getIndex(TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        if (!isNodeChild(aChild)) {
            return -1;
        }
        return children.indexOf(aChild);
    }

    @Override
    public Enumeration children() {
        return children == null ? EMPTY_ENUMERATION : children.elements();
    }

    public void setAllowsChildren(boolean allows) {
        if (allows != allowsChildren) {
            allowsChildren = allows;
            if (!allowsChildren) {
                removeAllChildren();
            }
        }
    }

    @Override
    public boolean getAllowsChildren() {
        return allowsChildren;
    }

    @Override
    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    public Object getUserObject() {
        return userObject;
    }

    @Override
    public void removeFromParent() {
        MutableTreeNode p = parent;
        if (p != null) {
            p.remove(this);
        }
    }

    @Override
    public void remove(MutableTreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        if (!isNodeChild(aChild)) {
            throw new IllegalArgumentException("argument is not a child");
        }
        remove(getIndex(aChild));
    }

    public void removeAllChildren() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            remove(i);
        }
    }

    public void add(MutableTreeNode newChild) {
        if (newChild != null && newChild.getParent() == this) {
            insert(newChild, getChildCount() - 1);
        } else {
            insert(newChild, getChildCount());
        }
    }

    // ------------------------------------------------------------ relations

    public boolean isNodeAncestor(TreeNode anotherNode) {
        if (anotherNode == null) {
            return false;
        }
        TreeNode ancestor = this;
        do {
            if (ancestor == anotherNode) {
                return true;
            }
            ancestor = ancestor.getParent();
        } while (ancestor != null);
        return false;
    }

    public boolean isNodeDescendant(DefaultMutableTreeNode anotherNode) {
        return anotherNode != null && anotherNode.isNodeAncestor(this);
    }

    public TreeNode getSharedAncestor(DefaultMutableTreeNode aNode) {
        if (aNode == this) {
            return this;
        } else if (aNode == null) {
            return null;
        }
        TreeNode a = this;
        TreeNode b = aNode;
        int la = getLevel();
        int lb = aNode.getLevel();
        while (la > lb && a != null) {
            a = a.getParent();
            la--;
        }
        while (lb > la && b != null) {
            b = b.getParent();
            lb--;
        }
        while (a != null && b != null) {
            if (a == b) {
                return a;
            }
            a = a.getParent();
            b = b.getParent();
        }
        return null;
    }

    public boolean isNodeRelated(DefaultMutableTreeNode aNode) {
        return aNode != null && getRoot() == aNode.getRoot();
    }

    /// The length of the longest way down from this node to a leaf.
    public int getDepth() {
        int deepest = 0;
        ArrayList<TreeNode> level = new ArrayList<TreeNode>();
        level.add(this);
        int depth = -1;
        while (!level.isEmpty()) {
            depth++;
            ArrayList<TreeNode> next = new ArrayList<TreeNode>();
            for (int i = 0; i < level.size(); i++) {
                TreeNode n = level.get(i);
                for (int j = 0; j < n.getChildCount(); j++) {
                    next.add(n.getChildAt(j));
                }
            }
            level = next;
        }
        deepest = Math.max(deepest, depth);
        return deepest;
    }

    /// The number of nodes above this one.
    public int getLevel() {
        int levels = 0;
        TreeNode ancestor = this;
        while ((ancestor = ancestor.getParent()) != null) {
            levels++;
        }
        return levels;
    }

    public TreeNode[] getPath() {
        return getPathToRoot(this, 0);
    }

    protected TreeNode[] getPathToRoot(TreeNode aNode, int depth) {
        int n = depth;
        for (TreeNode t = aNode; t != null; t = t.getParent()) {
            n++;
        }
        if (n == 0) {
            return null;
        }
        TreeNode[] out = new TreeNode[n];
        int at = n - depth;
        for (TreeNode t = aNode; t != null; t = t.getParent()) {
            out[--at] = t;
        }
        return out;
    }

    public Object[] getUserObjectPath() {
        TreeNode[] real = getPath();
        Object[] out = new Object[real.length];
        for (int i = 0; i < real.length; i++) {
            out[i] = real[i] instanceof DefaultMutableTreeNode ? ((DefaultMutableTreeNode) real[i]).getUserObject()
                    : null;
        }
        return out;
    }

    public TreeNode getRoot() {
        TreeNode ancestor = this;
        TreeNode previous;
        do {
            previous = ancestor;
            ancestor = ancestor.getParent();
        } while (ancestor != null);
        return previous;
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    private static DefaultMutableTreeNode own(Object n) {
        return n instanceof DefaultMutableTreeNode ? (DefaultMutableTreeNode) n : null;
    }

    /// The node after this one in a preorder walk of the whole tree.
    public DefaultMutableTreeNode getNextNode() {
        if (getChildCount() == 0) {
            DefaultMutableTreeNode next = getNextSibling();
            if (next == null) {
                DefaultMutableTreeNode p = own(getParent());
                while (p != null) {
                    next = p.getNextSibling();
                    if (next != null) {
                        return next;
                    }
                    p = own(p.getParent());
                }
                return null;
            }
            return next;
        }
        return own(getChildAt(0));
    }

    /// The node before this one in a preorder walk of the whole tree.
    public DefaultMutableTreeNode getPreviousNode() {
        DefaultMutableTreeNode p = own(getParent());
        if (p == null) {
            return null;
        }
        DefaultMutableTreeNode previous = getPreviousSibling();
        if (previous == null) {
            return p;
        }
        return previous.getChildCount() == 0 ? previous : previous.getLastLeaf();
    }

    // ------------------------------------------------------------ walks

    /// This node, then each child's whole subtree in turn.
    public Enumeration preorderEnumeration() {
        return new Preorder(this);
    }

    /// Each child's whole subtree in turn, then this node.
    public Enumeration postorderEnumeration() {
        return new Postorder(this);
    }

    /// This node, then its children, then theirs: level by level.
    public Enumeration breadthFirstEnumeration() {
        return new BreadthFirst(this);
    }

    /// The same walk as [#postorderEnumeration()].
    public Enumeration depthFirstEnumeration() {
        return postorderEnumeration();
    }

    /// The nodes from `ancestor` down to this node.
    public Enumeration pathFromAncestorEnumeration(TreeNode ancestor) {
        if (ancestor == null || !isNodeAncestor(ancestor)) {
            throw new IllegalArgumentException("node " + ancestor + " is not an ancestor of " + this);
        }
        ArrayList<TreeNode> way = new ArrayList<TreeNode>();
        for (TreeNode t = this; t != null; t = t.getParent()) {
            way.add(t);
            if (t == ancestor) {
                break;
            }
        }
        return new ListEnumeration<TreeNode>(way, true);
    }

    private static final class Preorder implements Enumeration<TreeNode> {

        private final ArrayList<TreeNode> stack = new ArrayList<TreeNode>();

        Preorder(TreeNode root) {
            stack.add(root);
        }

        @Override
        public boolean hasMoreElements() {
            return !stack.isEmpty();
        }

        @Override
        public TreeNode nextElement() {
            if (stack.isEmpty()) {
                throw new NoSuchElementException("No more elements");
            }
            TreeNode n = stack.remove(stack.size() - 1);
            for (int i = n.getChildCount() - 1; i >= 0; i--) {
                stack.add(n.getChildAt(i));
            }
            return n;
        }
    }

    private static final class Postorder implements Enumeration<TreeNode> {

        private final ArrayList<TreeNode> nodes = new ArrayList<TreeNode>();
        private final ArrayList<int[]> next = new ArrayList<int[]>();

        Postorder(TreeNode root) {
            nodes.add(root);
            next.add(new int[]{0});
        }

        @Override
        public boolean hasMoreElements() {
            return !nodes.isEmpty();
        }

        @Override
        public TreeNode nextElement() {
            if (nodes.isEmpty()) {
                throw new NoSuchElementException("No more elements");
            }
            while (true) {
                int top = nodes.size() - 1;
                TreeNode n = nodes.get(top);
                int[] at = next.get(top);
                if (at[0] < n.getChildCount()) {
                    nodes.add(n.getChildAt(at[0]++));
                    next.add(new int[]{0});
                } else {
                    nodes.remove(top);
                    next.remove(top);
                    return n;
                }
            }
        }
    }

    private static final class BreadthFirst implements Enumeration<TreeNode> {

        private final ArrayList<TreeNode> queue = new ArrayList<TreeNode>();
        private int head;

        BreadthFirst(TreeNode root) {
            queue.add(root);
        }

        @Override
        public boolean hasMoreElements() {
            return head < queue.size();
        }

        @Override
        public TreeNode nextElement() {
            if (head >= queue.size()) {
                throw new NoSuchElementException("No more elements");
            }
            TreeNode n = queue.get(head);
            queue.set(head++, null);
            for (int i = 0; i < n.getChildCount(); i++) {
                queue.add(n.getChildAt(i));
            }
            return n;
        }
    }

    // ------------------------------------------------------------ children

    public boolean isNodeChild(TreeNode aNode) {
        return aNode != null && getChildCount() != 0 && aNode.getParent() == this;
    }

    public TreeNode getFirstChild() {
        if (getChildCount() == 0) {
            throw new NoSuchElementException("node has no children");
        }
        return getChildAt(0);
    }

    public TreeNode getLastChild() {
        if (getChildCount() == 0) {
            throw new NoSuchElementException("node has no children");
        }
        return getChildAt(getChildCount() - 1);
    }

    public TreeNode getChildAfter(TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        int index = getIndex(aChild);
        if (index == -1) {
            throw new IllegalArgumentException("node is not a child");
        }
        return index < getChildCount() - 1 ? getChildAt(index + 1) : null;
    }

    public TreeNode getChildBefore(TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }
        int index = getIndex(aChild);
        if (index == -1) {
            throw new IllegalArgumentException("node is not a child");
        }
        return index > 0 ? getChildAt(index - 1) : null;
    }

    // ------------------------------------------------------------ siblings

    public boolean isNodeSibling(TreeNode anotherNode) {
        if (anotherNode == null) {
            return false;
        }
        if (anotherNode == this) {
            return true;
        }
        TreeNode p = getParent();
        return p != null && p == anotherNode.getParent();
    }

    public int getSiblingCount() {
        TreeNode p = getParent();
        return p == null ? 1 : p.getChildCount();
    }

    public DefaultMutableTreeNode getNextSibling() {
        DefaultMutableTreeNode p = own(getParent());
        return p == null ? null : own(p.getChildAfter(this));
    }

    public DefaultMutableTreeNode getPreviousSibling() {
        DefaultMutableTreeNode p = own(getParent());
        return p == null ? null : own(p.getChildBefore(this));
    }

    // ------------------------------------------------------------ leaves

    @Override
    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    public DefaultMutableTreeNode getFirstLeaf() {
        DefaultMutableTreeNode node = this;
        while (!node.isLeaf()) {
            DefaultMutableTreeNode first = own(node.getFirstChild());
            if (first == null) {
                break;
            }
            node = first;
        }
        return node;
    }

    public DefaultMutableTreeNode getLastLeaf() {
        DefaultMutableTreeNode node = this;
        while (!node.isLeaf()) {
            DefaultMutableTreeNode last = own(node.getLastChild());
            if (last == null) {
                break;
            }
            node = last;
        }
        return node;
    }

    public DefaultMutableTreeNode getNextLeaf() {
        DefaultMutableTreeNode p = own(getParent());
        if (p == null) {
            return null;
        }
        DefaultMutableTreeNode next = getNextSibling();
        if (next != null) {
            return next.getFirstLeaf();
        }
        return p.getNextLeaf();
    }

    public DefaultMutableTreeNode getPreviousLeaf() {
        DefaultMutableTreeNode p = own(getParent());
        if (p == null) {
            return null;
        }
        DefaultMutableTreeNode previous = getPreviousSibling();
        if (previous != null) {
            return previous.getLastLeaf();
        }
        return p.getPreviousLeaf();
    }

    public int getLeafCount() {
        int count = 0;
        Enumeration e = breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            Object n = e.nextElement();
            if (n instanceof TreeNode && ((TreeNode) n).isLeaf()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return userObject == null ? "" : userObject.toString();
    }

    /// A new plain node with this node's user object, and neither parent
    /// nor children.
    @Override
    public Object clone() {
        return new DefaultMutableTreeNode(userObject, allowsChildren);
    }
}

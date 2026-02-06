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

import java.util.Vector;

/// Arranges tree node objects, a node can essentially be anything and it will be displayed in a hierarchy
/// using the `com.codename1.ui.tree.Tree`
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
/// private Element root;
/// public XMLTreeModel(Element e) {
/// root = e;
/// }
///
/// public Vector getChildren(Object parent) {
/// if(parent == null) {
/// Vector c = new Vector();
/// c.addElement(root);
/// return c;
/// }
/// Vector result = new Vector();
/// Element e = (Element)parent;
/// for(int iter = 0 ; iter
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
public interface TreeModel {
    /// Returns the child objects representing the given parent, null should return
    /// the root objects
    ///
    /// #### Parameters
    ///
    /// - `parent`: @param parent the parent object whose children should be returned, null would return the
    ///               tree roots
    ///
    /// #### Returns
    ///
    /// the children of the given node within the tree
    Vector getChildren(Object parent);

    /// Is the node a leaf or a folder
    ///
    /// #### Parameters
    ///
    /// - `node`: a node within the tree
    ///
    /// #### Returns
    ///
    /// true if the node is a leaf that can't be expanded
    boolean isLeaf(Object node);
}

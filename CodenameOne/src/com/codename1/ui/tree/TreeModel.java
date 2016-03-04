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

/**
 * <p>Arranges tree node objects, a node can essentially be anything and it will be displayed in a hierarchy
 * using the {@link com.codename1.ui.tree.Tree}</p>
 *
 * <script src="https://gist.github.com/codenameone/870d4412694bca3092c4.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/tree.png" alt="Tree sample code" />
 * 
 * <p>
 * And heres a more "real world" example showing an XML hierarchy in a {@code Tree}:
 * </p>
 * <script src="https://gist.github.com/codenameone/5361ad7339c1ae26e0b8.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/components-tree-xml.png" alt="Tree with XML data" />
 *
 * <p>
 * Another real world example showing the {@link com.codename1.io.FileSystemStorage} as a tree:
 * </p>
 * <script src="https://gist.github.com/codenameone/2877412809a8cff646af.js"></script>            
 * <img src="https://www.codenameone.com/img/developer-guide/filesystem-tree.png" alt="Simple sample of a tree for the FileSystemStorage API">
 * 
 * @author Shai Almog
 */
public interface TreeModel {
    /**
     * Returns the child objects representing the given parent, null should return
     * the root objects
     *
     * @param parent the parent object whose children should be returned, null would return the
     * tree roots
     * @return the children of the given node within the tree
     */
    public Vector getChildren(Object parent);

    /**
     * Is the node a leaf or a folder
     *
     * @param node a node within the tree
     * @return true if the node is a leaf that can't be expanded
     */
    public boolean isLeaf(Object node);
}

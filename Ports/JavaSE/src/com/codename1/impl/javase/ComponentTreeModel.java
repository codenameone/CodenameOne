/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codename1.impl.javase;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.List;
import com.codename1.ui.list.ContainerList;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

/**
 *
 * @author Shai Almog
 */
class ComponentTreeModel implements javax.swing.tree.TreeModel {
    private Form root;

    ComponentTreeModel(Form root) {
        this.root = root;
    }

    public Object getRoot() {
        return root;
    }

    public Object getChild(Object parent, int index) {
        Component cmp = (Component) parent;
        if (cmp instanceof List) {
            Object val = ((List) cmp).getRenderingPrototype();
            if (val == null) {
                val = ((List) cmp).getModel().getItemAt(0);
            }
            if (index == 0) {
                return ((List) cmp).getRenderer().getListCellRendererComponent((List) cmp, val, 0, false);
            } else {
                return ((List) cmp).getRenderer().getListCellRendererComponent((List) cmp, val, 0, true);
            }
        }
        if (cmp instanceof ContainerList) {
            Object val = ((ContainerList) cmp).getModel().getItemAt(0);
            if (index == 0) {
                return ((ContainerList) cmp).getRenderer().getCellRendererComponent(cmp, ((ContainerList) cmp).getModel(), val, 0, false);
            } else {
                return ((ContainerList) cmp).getRenderer().getCellRendererComponent(cmp, ((ContainerList) cmp).getModel(), val, 0, true);
            }
        }
        return ((Container) cmp).getComponentAt(index);
    }

    public int getChildCount(Object parent) {
        Component cmp = (Component) parent;
        if (cmp instanceof List) {
            Object val = ((List) cmp).getRenderingPrototype();
            if (val != null) {
                // same component instance returned for both!
                if (getChild(parent, 0) == getChild(parent, 1)) {
                    return 1;
                }
                return 2;
            }
            return 0;
        }
        if (cmp instanceof ContainerList) {
            if (((ContainerList) cmp).getModel().getSize() > 0) {
                // same component instance returned for both!
                if (getChild(parent, 0) == getChild(parent, 1)) {
                    return 1;
                }
                return 2;
            }
            return 0;
        }
        if (cmp instanceof Container) {
            return ((Container) cmp).getComponentCount();
        }
        return 0;
    }

    public boolean isLeaf(Object node) {
        return !(node instanceof Container || node instanceof List);
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    public int getIndexOfChild(Object parent, Object child) {
        Component cmp = (Component) parent;
        Component chd = (Component) child;
        if (cmp instanceof List || cmp instanceof ContainerList) {
            if (chd.hasFocus()) {
                return 1;
            }
            return 0;
        }
        if (cmp instanceof Container) {
            return ((Container) cmp).getComponentIndex(chd);
        }
        return -1;
    }

    public void addTreeModelListener(TreeModelListener l) {
    }

    public void removeTreeModelListener(TreeModelListener l) {
    }

}

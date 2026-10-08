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

import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import com.codename1.desktopcompat.javax.swing.event.SwingPropertyChangeSupport;
import com.codename1.desktopcompat.javax.swing.event.TreeSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeSelectionListener;
import java.util.ArrayList;
import java.util.EventListener;

/// The selection of a tree: a set of paths, kept to one path or to one
/// run of rows when the mode asks for it.
///
/// A run of rows can only be checked with a [RowMapper], which a tree
/// gives its selection model. In the contiguous mode a selection that
/// would not be one run is replaced: setting such paths selects the first
/// of them, adding paths that do not join the run selects just the added
/// ones, and removing from the middle of the run clears it.
public class DefaultTreeSelectionModel implements Cloneable, TreeSelectionModel {

    public static final String SELECTION_MODE_PROPERTY = "selectionMode";

    protected SwingPropertyChangeSupport changeSupport;
    protected TreePath[] selection;
    protected EventListenerList listenerList = new EventListenerList();
    protected transient RowMapper rowMapper;
    protected int selectionMode;
    protected TreePath leadPath;
    protected int leadIndex;
    protected int leadRow;

    public DefaultTreeSelectionModel() {
        selectionMode = DISCONTIGUOUS_TREE_SELECTION;
        leadIndex = -1;
        leadRow = -1;
    }

    @Override
    public void setRowMapper(RowMapper newMapper) {
        rowMapper = newMapper;
        resetRowSelection();
    }

    @Override
    public RowMapper getRowMapper() {
        return rowMapper;
    }

    @Override
    public void setSelectionMode(int mode) {
        int old = selectionMode;
        selectionMode = mode;
        if (selectionMode != SINGLE_TREE_SELECTION && selectionMode != CONTIGUOUS_TREE_SELECTION
                && selectionMode != DISCONTIGUOUS_TREE_SELECTION) {
            selectionMode = DISCONTIGUOUS_TREE_SELECTION;
        }
        if (old != selectionMode && changeSupport != null) {
            changeSupport.firePropertyChange(SELECTION_MODE_PROPERTY, Integer.valueOf(old),
                    Integer.valueOf(selectionMode));
        }
    }

    @Override
    public int getSelectionMode() {
        return selectionMode;
    }

    @Override
    public void setSelectionPath(TreePath path) {
        if (path == null) {
            setSelectionPaths(null);
        } else {
            setSelectionPaths(new TreePath[]{path});
        }
    }

    private static boolean has(ArrayList<TreePath> list, TreePath p) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(p)) {
                return true;
            }
        }
        return false;
    }

    /// The paths without nulls and repeats.
    private static ArrayList<TreePath> clean(TreePath[] paths) {
        ArrayList<TreePath> out = new ArrayList<TreePath>();
        if (paths != null) {
            for (int i = 0; i < paths.length; i++) {
                if (paths[i] != null && !has(out, paths[i])) {
                    out.add(paths[i]);
                }
            }
        }
        return out;
    }

    private ArrayList<TreePath> current() {
        return clean(selection);
    }

    /// Makes `wanted` the selection and tells the listeners what joined
    /// and what left.
    private void become(ArrayList<TreePath> wanted, TreePath lead) {
        ArrayList<TreePath> old = current();
        ArrayList<TreePath> changed = new ArrayList<TreePath>();
        ArrayList<Boolean> added = new ArrayList<Boolean>();
        for (int i = 0; i < old.size(); i++) {
            if (!has(wanted, old.get(i))) {
                changed.add(old.get(i));
                added.add(Boolean.FALSE);
            }
        }
        for (int i = 0; i < wanted.size(); i++) {
            if (!has(old, wanted.get(i))) {
                changed.add(wanted.get(i));
                added.add(Boolean.TRUE);
            }
        }
        TreePath oldLead = leadPath;
        selection = wanted.isEmpty() ? null : wanted.toArray(new TreePath[wanted.size()]);
        leadPath = wanted.isEmpty() ? null : lead != null && has(wanted, lead) ? lead : wanted.get(wanted.size() - 1);
        updateLeadIndex();
        resetRowSelection();
        if (!changed.isEmpty()) {
            TreePath[] paths = changed.toArray(new TreePath[changed.size()]);
            boolean[] areNew = new boolean[paths.length];
            for (int i = 0; i < areNew.length; i++) {
                areNew[i] = added.get(i).booleanValue();
            }
            fireValueChanged(new TreeSelectionEvent(this, paths, areNew, oldLead, leadPath));
        }
    }

    @Override
    public void setSelectionPaths(TreePath[] pPaths) {
        ArrayList<TreePath> wanted = clean(pPaths);
        if (wanted.size() > 1) {
            boolean one = selectionMode == SINGLE_TREE_SELECTION
                    || (selectionMode == CONTIGUOUS_TREE_SELECTION && !contiguous(wanted));
            if (one) {
                TreePath first = wanted.get(0);
                wanted = new ArrayList<TreePath>();
                wanted.add(first);
            }
        }
        become(wanted, null);
    }

    @Override
    public void addSelectionPath(TreePath path) {
        if (path != null) {
            addSelectionPaths(new TreePath[]{path});
        }
    }

    @Override
    public void addSelectionPaths(TreePath[] paths) {
        ArrayList<TreePath> more = clean(paths);
        if (more.isEmpty()) {
            return;
        }
        if (selectionMode == SINGLE_TREE_SELECTION) {
            setSelectionPaths(paths);
            return;
        }
        ArrayList<TreePath> all = current();
        for (int i = 0; i < more.size(); i++) {
            if (!has(all, more.get(i))) {
                all.add(more.get(i));
            }
        }
        if (selectionMode == CONTIGUOUS_TREE_SELECTION && !contiguous(all)) {
            setSelectionPaths(paths);
            return;
        }
        become(all, more.get(more.size() - 1));
    }

    @Override
    public void removeSelectionPath(TreePath path) {
        if (path != null) {
            removeSelectionPaths(new TreePath[]{path});
        }
    }

    @Override
    public void removeSelectionPaths(TreePath[] paths) {
        ArrayList<TreePath> gone = clean(paths);
        if (gone.isEmpty() || selection == null) {
            return;
        }
        ArrayList<TreePath> old = current();
        ArrayList<TreePath> left = new ArrayList<TreePath>();
        for (int i = 0; i < old.size(); i++) {
            if (!has(gone, old.get(i))) {
                left.add(old.get(i));
            }
        }
        if (left.size() == old.size()) {
            return;
        }
        if (selectionMode == CONTIGUOUS_TREE_SELECTION && !contiguous(left)) {
            left = new ArrayList<TreePath>();
        }
        become(left, leadPath);
    }

    @Override
    public TreePath getSelectionPath() {
        return selection != null && selection.length > 0 ? selection[0] : null;
    }

    @Override
    public TreePath[] getSelectionPaths() {
        if (selection == null) {
            return null;
        }
        TreePath[] out = new TreePath[selection.length];
        System.arraycopy(selection, 0, out, 0, selection.length);
        return out;
    }

    @Override
    public int getSelectionCount() {
        return selection == null ? 0 : selection.length;
    }

    @Override
    public boolean isPathSelected(TreePath path) {
        if (path == null || selection == null) {
            return false;
        }
        for (int i = 0; i < selection.length; i++) {
            if (selection[i].equals(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSelectionEmpty() {
        return selection == null || selection.length == 0;
    }

    @Override
    public void clearSelection() {
        if (selection != null && selection.length > 0) {
            become(new ArrayList<TreePath>(), null);
        }
    }

    @Override
    public void addTreeSelectionListener(TreeSelectionListener x) {
        listenerList.add(TreeSelectionListener.class, x);
    }

    @Override
    public void removeTreeSelectionListener(TreeSelectionListener x) {
        listenerList.remove(TreeSelectionListener.class, x);
    }

    public TreeSelectionListener[] getTreeSelectionListeners() {
        return listenerList.getListeners(TreeSelectionListener.class);
    }

    protected void fireValueChanged(TreeSelectionEvent e) {
        TreeSelectionListener[] ls = getTreeSelectionListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            ls[i].valueChanged(e);
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /// The rows of the selected paths that are shown, in no set order.
    @Override
    public int[] getSelectionRows() {
        if (rowMapper == null || selection == null || selection.length == 0) {
            return new int[0];
        }
        int[] rows = rowMapper.getRowsForPaths(getSelectionPaths());
        if (rows == null) {
            return new int[0];
        }
        int n = 0;
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] >= 0) {
                n++;
            }
        }
        int[] out = new int[n];
        n = 0;
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] >= 0) {
                out[n++] = rows[i];
            }
        }
        return out;
    }

    @Override
    public int getMinSelectionRow() {
        int[] rows = getSelectionRows();
        int min = -1;
        for (int i = 0; i < rows.length; i++) {
            if (min < 0 || rows[i] < min) {
                min = rows[i];
            }
        }
        return min;
    }

    @Override
    public int getMaxSelectionRow() {
        int[] rows = getSelectionRows();
        int max = -1;
        for (int i = 0; i < rows.length; i++) {
            max = Math.max(max, rows[i]);
        }
        return max;
    }

    @Override
    public boolean isRowSelected(int row) {
        int[] rows = getSelectionRows();
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] == row) {
                return true;
            }
        }
        return false;
    }

    /// The rows of the paths changed: the lead row is looked up again.
    @Override
    public void resetRowSelection() {
        leadRow = -1;
        if (rowMapper != null && leadPath != null) {
            int[] rows = rowMapper.getRowsForPaths(new TreePath[]{leadPath});
            if (rows != null && rows.length > 0) {
                leadRow = rows[0];
            }
        }
    }

    @Override
    public int getLeadSelectionRow() {
        return leadRow;
    }

    @Override
    public TreePath getLeadSelectionPath() {
        return leadPath;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return changeSupport.getPropertyChangeListeners();
    }

    /// Whether the paths are shown on rows that follow one another. Paths
    /// are taken for one run when there is no row mapper to ask.
    private boolean contiguous(ArrayList<TreePath> paths) {
        if (rowMapper == null || paths.size() < 2) {
            return true;
        }
        int[] rows = rowMapper.getRowsForPaths(paths.toArray(new TreePath[paths.size()]));
        if (rows == null) {
            return true;
        }
        int min = Integer.MAX_VALUE;
        int max = -1;
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] < 0) {
                return false;
            }
            min = Math.min(min, rows[i]);
            max = Math.max(max, rows[i]);
        }
        return max - min + 1 == rows.length;
    }

    protected boolean arePathsContiguous(TreePath[] paths) {
        return contiguous(clean(paths));
    }

    protected void updateLeadIndex() {
        leadIndex = -1;
        if (leadPath != null && selection != null) {
            for (int i = selection.length - 1; i >= 0; i--) {
                if (selection[i].equals(leadPath)) {
                    leadIndex = i;
                    break;
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(' ').append(hashCode()).append(" [ ");
        if (selection != null) {
            for (int i = 0; i < selection.length; i++) {
                sb.append(selection[i]).append(' ');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /// A new model with the same mode and selected paths, and no
    /// listeners or row mapper.
    @Override
    public Object clone() {
        DefaultTreeSelectionModel copy = new DefaultTreeSelectionModel();
        copy.selectionMode = selectionMode;
        copy.selection = getSelectionPaths();
        copy.leadPath = leadPath;
        copy.leadIndex = leadIndex;
        copy.leadRow = leadRow;
        return copy;
    }
}

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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.javax.swing.event.ListSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.ListSelectionListener;
import java.util.ArrayList;

/// The selection of a list in its three modes.
///
/// Every operation reports one event covering the rows whose state changed
/// and, while [#isLeadAnchorNotificationEnabled()], the old and new places
/// of the anchor and the lead. While the value is adjusting the events say
/// so, and ending the adjustment reports the whole range touched since it
/// began.
public class DefaultListSelectionModel implements ListSelectionModel {

    private static final int NONE_MIN = Integer.MAX_VALUE;
    private static final int NONE_MAX = -1;

    private int selectionMode = MULTIPLE_INTERVAL_SELECTION;
    private int minIndex = NONE_MIN;
    private int maxIndex = NONE_MAX;
    private int anchorIndex = -1;
    private int leadIndex = -1;
    private int firstAdjustedIndex = NONE_MIN;
    private int lastAdjustedIndex = NONE_MAX;
    private boolean isAdjusting;
    private int firstChangedIndex = NONE_MIN;
    private int lastChangedIndex = NONE_MAX;
    private boolean[] value = new boolean[32];
    private final ArrayList<ListSelectionListener> listeners = new ArrayList<ListSelectionListener>();

    protected boolean leadAnchorNotificationEnabled = true;

    public DefaultListSelectionModel() {
    }

    @Override
    public int getMinSelectionIndex() {
        return isSelectionEmpty() ? -1 : minIndex;
    }

    @Override
    public int getMaxSelectionIndex() {
        return maxIndex;
    }

    @Override
    public boolean getValueIsAdjusting() {
        return isAdjusting;
    }

    @Override
    public int getSelectionMode() {
        return selectionMode;
    }

    @Override
    public void setSelectionMode(int selectionMode) {
        if (selectionMode != SINGLE_SELECTION && selectionMode != SINGLE_INTERVAL_SELECTION
                && selectionMode != MULTIPLE_INTERVAL_SELECTION) {
            throw new IllegalArgumentException("invalid selectionMode");
        }
        this.selectionMode = selectionMode;
    }

    @Override
    public boolean isSelectedIndex(int index) {
        return index >= minIndex && index <= maxIndex && get(index);
    }

    @Override
    public boolean isSelectionEmpty() {
        return minIndex > maxIndex;
    }

    @Override
    public void addListSelectionListener(ListSelectionListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    @Override
    public void removeListSelectionListener(ListSelectionListener l) {
        int i = listeners.lastIndexOf(l);
        if (i >= 0) {
            listeners.remove(i);
        }
    }

    public ListSelectionListener[] getListSelectionListeners() {
        return listeners.toArray(new ListSelectionListener[listeners.size()]);
    }

    protected void fireValueChanged(boolean isAdjusting) {
        if (lastChangedIndex == NONE_MAX) {
            return;
        }
        int oldFirst = firstChangedIndex;
        int oldLast = lastChangedIndex;
        firstChangedIndex = NONE_MIN;
        lastChangedIndex = NONE_MAX;
        fireValueChanged(oldFirst, oldLast, isAdjusting);
    }

    protected void fireValueChanged(int firstIndex, int lastIndex) {
        fireValueChanged(firstIndex, lastIndex, getValueIsAdjusting());
    }

    protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
        if (listeners.isEmpty()) {
            return;
        }
        ListSelectionEvent e = new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting);
        ListSelectionListener[] all = getListSelectionListeners();
        for (int i = all.length - 1; i >= 0; i--) {
            all[i].valueChanged(e);
        }
    }

    private void fireValueChanged() {
        if (lastAdjustedIndex == NONE_MAX) {
            return;
        }
        if (getValueIsAdjusting()) {
            firstChangedIndex = Math.min(firstChangedIndex, firstAdjustedIndex);
            lastChangedIndex = Math.max(lastChangedIndex, lastAdjustedIndex);
        }
        int oldFirst = firstAdjustedIndex;
        int oldLast = lastAdjustedIndex;
        firstAdjustedIndex = NONE_MIN;
        lastAdjustedIndex = NONE_MAX;
        fireValueChanged(oldFirst, oldLast);
    }

    private boolean get(int i) {
        return i >= 0 && i < value.length && value[i];
    }

    private void markAsDirty(int r) {
        if (r == -1) {
            return;
        }
        firstAdjustedIndex = Math.min(firstAdjustedIndex, r);
        lastAdjustedIndex = Math.max(lastAdjustedIndex, r);
    }

    private void set(int r) {
        if (get(r)) {
            return;
        }
        if (r >= value.length) {
            boolean[] grown = new boolean[Math.max(r + 1, value.length * 2)];
            System.arraycopy(value, 0, grown, 0, value.length);
            value = grown;
        }
        value[r] = true;
        markAsDirty(r);
        minIndex = Math.min(minIndex, r);
        maxIndex = Math.max(maxIndex, r);
    }

    private void clear(int r) {
        if (!get(r)) {
            return;
        }
        value[r] = false;
        markAsDirty(r);
        if (r == minIndex) {
            for (minIndex = minIndex + 1; minIndex <= maxIndex; minIndex++) {
                if (get(minIndex)) {
                    break;
                }
            }
        }
        if (r == maxIndex) {
            for (maxIndex = maxIndex - 1; minIndex <= maxIndex; maxIndex--) {
                if (get(maxIndex)) {
                    break;
                }
            }
        }
        if (isSelectionEmpty()) {
            minIndex = NONE_MIN;
            maxIndex = NONE_MAX;
        }
    }

    public void setLeadAnchorNotificationEnabled(boolean flag) {
        leadAnchorNotificationEnabled = flag;
    }

    public boolean isLeadAnchorNotificationEnabled() {
        return leadAnchorNotificationEnabled;
    }

    private void updateLeadAnchorIndices(int anchor, int lead) {
        if (leadAnchorNotificationEnabled) {
            if (anchorIndex != anchor) {
                markAsDirty(anchorIndex);
                markAsDirty(anchor);
            }
            if (leadIndex != lead) {
                markAsDirty(leadIndex);
                markAsDirty(lead);
            }
        }
        anchorIndex = anchor;
        leadIndex = lead;
    }

    private static boolean contains(int a, int b, int i) {
        return i >= a && i <= b;
    }

    private void changeSelection(int clearMin, int clearMax, int setMin, int setMax, boolean clearFirst) {
        for (int i = Math.min(setMin, clearMin); i <= Math.max(setMax, clearMax); i++) {
            boolean shouldClear = contains(clearMin, clearMax, i);
            boolean shouldSet = contains(setMin, setMax, i);
            if (shouldSet && shouldClear) {
                if (clearFirst) {
                    shouldClear = false;
                } else {
                    shouldSet = false;
                }
            }
            if (shouldSet) {
                set(i);
            }
            if (shouldClear) {
                clear(i);
            }
        }
        fireValueChanged();
    }

    @Override
    public void clearSelection() {
        removeSelectionIntervalImpl(minIndex, maxIndex, false);
    }

    @Override
    public void setSelectionInterval(int index0, int index1) {
        if (index0 == -1 || index1 == -1) {
            return;
        }
        if (getSelectionMode() == SINGLE_SELECTION) {
            index0 = index1;
        }
        updateLeadAnchorIndices(index0, index1);
        changeSelection(minIndex, maxIndex, Math.min(index0, index1), Math.max(index0, index1), true);
    }

    @Override
    public void addSelectionInterval(int index0, int index1) {
        if (index0 == -1 || index1 == -1) {
            return;
        }
        if (getSelectionMode() == SINGLE_SELECTION) {
            setSelectionInterval(index0, index1);
            return;
        }
        updateLeadAnchorIndices(index0, index1);
        int clearMin = NONE_MIN;
        int clearMax = NONE_MAX;
        int setMin = Math.min(index0, index1);
        int setMax = Math.max(index0, index1);
        // One interval only: a range that neither touches nor overlaps the
        // selection replaces it.
        if (getSelectionMode() == SINGLE_INTERVAL_SELECTION && (setMax < minIndex - 1 || setMin > maxIndex + 1)) {
            clearMin = minIndex;
            clearMax = maxIndex;
        }
        changeSelection(clearMin, clearMax, setMin, setMax, true);
    }

    @Override
    public void removeSelectionInterval(int index0, int index1) {
        removeSelectionIntervalImpl(index0, index1, true);
    }

    private void removeSelectionIntervalImpl(int index0, int index1, boolean changeLeadAnchor) {
        if (index0 == -1 || index1 == -1) {
            return;
        }
        if (changeLeadAnchor) {
            updateLeadAnchorIndices(index0, index1);
        }
        int clearMin = Math.min(index0, index1);
        int clearMax = Math.max(index0, index1);
        // Taking the middle out of a single interval would leave two.
        if (getSelectionMode() != MULTIPLE_INTERVAL_SELECTION && clearMin > minIndex && clearMax < maxIndex) {
            clearMax = maxIndex;
        }
        changeSelection(clearMin, clearMax, NONE_MIN, NONE_MAX, true);
    }

    private void setState(int index, boolean state) {
        if (state) {
            set(index);
        } else {
            clear(index);
        }
    }

    @Override
    public void insertIndexInterval(int index, int length, boolean before) {
        int insMinIndex = before ? index : index + 1;
        int insMaxIndex = insMinIndex + length - 1;
        for (int i = maxIndex; i >= insMinIndex; i--) {
            setState(i + length, get(i));
        }
        boolean setInsertedValues = getSelectionMode() != SINGLE_SELECTION && get(index);
        for (int i = insMinIndex; i <= insMaxIndex; i++) {
            setState(i, setInsertedValues);
        }
        int lead = leadIndex;
        if (lead > index || (before && lead == index)) {
            lead = leadIndex + length;
        }
        int anchor = anchorIndex;
        if (anchor > index || (before && anchor == index)) {
            anchor = anchorIndex + length;
        }
        if (lead != leadIndex || anchor != anchorIndex) {
            updateLeadAnchorIndices(anchor, lead);
        }
        fireValueChanged();
    }

    @Override
    public void removeIndexInterval(int index0, int index1) {
        int rmMinIndex = Math.min(index0, index1);
        int rmMaxIndex = Math.max(index0, index1);
        int gapLength = rmMaxIndex - rmMinIndex + 1;
        for (int i = rmMinIndex; i <= maxIndex; i++) {
            setState(i, get(i + gapLength));
        }
        int lead = leadIndex;
        if (lead == 0 && rmMinIndex == 0) {
            // Stays on the first row.
            lead = 0;
        } else if (lead > rmMaxIndex) {
            lead = leadIndex - gapLength;
        } else if (lead >= rmMinIndex) {
            lead = rmMinIndex - 1;
        }
        int anchor = anchorIndex;
        if (anchor == 0 && rmMinIndex == 0) {
            anchor = 0;
        } else if (anchor > rmMaxIndex) {
            anchor = anchorIndex - gapLength;
        } else if (anchor >= rmMinIndex) {
            anchor = rmMinIndex - 1;
        }
        if (lead != leadIndex || anchor != anchorIndex) {
            updateLeadAnchorIndices(anchor, lead);
        }
        fireValueChanged();
    }

    @Override
    public void setValueIsAdjusting(boolean isAdjusting) {
        if (isAdjusting != this.isAdjusting) {
            this.isAdjusting = isAdjusting;
            this.fireValueChanged(isAdjusting);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValueIsAdjusting() ? "~" : "=").append('{');
        boolean first = true;
        for (int i = minIndex; i <= maxIndex; i++) {
            if (get(i)) {
                if (!first) {
                    sb.append(' ');
                }
                sb.append(i);
                first = false;
            }
        }
        return sb.append('}').toString();
    }

    @Override
    public int getAnchorSelectionIndex() {
        return anchorIndex;
    }

    @Override
    public int getLeadSelectionIndex() {
        return leadIndex;
    }

    @Override
    public void setAnchorSelectionIndex(int anchorIndex) {
        updateLeadAnchorIndices(anchorIndex, this.leadIndex);
        fireValueChanged();
    }

    /// Moves the lead without changing the selection.
    public void moveLeadSelectionIndex(int leadIndex) {
        if (leadIndex == -1 && this.anchorIndex != -1) {
            return;
        }
        updateLeadAnchorIndices(this.anchorIndex, leadIndex);
        fireValueChanged();
    }

    /// Moves the lead and makes the rows between the anchor and it match
    /// the state of the anchor row, the way a shift click does.
    @Override
    public void setLeadSelectionIndex(int leadIndex) {
        int anchor = this.anchorIndex;
        if (leadIndex == -1) {
            if (anchor == -1) {
                updateLeadAnchorIndices(anchor, leadIndex);
                fireValueChanged();
            }
            return;
        } else if (anchor == -1) {
            return;
        }
        if (this.leadIndex == -1) {
            this.leadIndex = leadIndex;
        }
        boolean shouldSelect = get(this.anchorIndex);
        if (getSelectionMode() == SINGLE_SELECTION) {
            anchor = leadIndex;
            shouldSelect = true;
        }
        int oldMin = Math.min(this.anchorIndex, this.leadIndex);
        int oldMax = Math.max(this.anchorIndex, this.leadIndex);
        int newMin = Math.min(anchor, leadIndex);
        int newMax = Math.max(anchor, leadIndex);
        updateLeadAnchorIndices(anchor, leadIndex);
        if (shouldSelect) {
            changeSelection(oldMin, oldMax, newMin, newMax, true);
        } else {
            changeSelection(newMin, newMax, oldMin, oldMax, false);
        }
    }
}

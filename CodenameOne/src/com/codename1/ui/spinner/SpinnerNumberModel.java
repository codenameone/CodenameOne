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

package com.codename1.ui.spinner;

import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.util.EventDispatcher;

/**
 * Represents a numeric model for the spinner
 *
 * @author Shai Almog
 * @deprecated use Picker instead
 */
class SpinnerNumberModel implements ListModel {
    private final EventDispatcher dataListener = new EventDispatcher();
    private final EventDispatcher selectionListener = new EventDispatcher();
    private final double min;
    private final double max;
    private final double step;
    boolean realValues;
    private double currentValue;
    /**
     * The old DateSpinner relies on behavior that was broken in this commit:
     * https://github.com/codenameone/CodenameOne/commit/cfac9a6a1bb15027b48a9b822e2f21eb2835d38e#diff-d12531ab4b0dd8bf1233a09f3c5e2b2b5634bff3c3cd2f357ad0a001e5f19bbf
     * This is a workaround to preserve compatibility
     */
    private int maxOffset = 1;

    private boolean setSelectedIndexReentrantLock;

    /**
     * Indicates the range of the spinner
     *
     * @param min          lowest value allowed
     * @param max          maximum value allowed
     * @param currentValue the starting value for the mode
     * @param step         the value by which we increment the entries in the model
     */
    public SpinnerNumberModel(int min, int max, int currentValue, int step) {
        this.max = max;
        this.min = min;
        this.currentValue = currentValue;
        this.step = step;
    }

    SpinnerNumberModel(int min, int max, int currentValue, int step, int maxOffset) {
        this.max = max;
        this.min = min;
        this.currentValue = currentValue;
        this.step = step;
        this.maxOffset = maxOffset;
    }

    /**
     * Indicates the range of the spinner
     *
     * @param min          lowest value allowed
     * @param max          maximum value allowed
     * @param currentValue the starting value for the mode
     * @param step         the value by which we increment the entries in the model
     */
    public SpinnerNumberModel(double min, double max, double currentValue, double step) {
        this.max = max;
        this.min = min;
        this.currentValue = currentValue;
        this.step = step;
        realValues = true;
    }

    Object getValue() {
        if (realValues) {
            return Double.valueOf(currentValue);
        }
        return Integer.valueOf((int) currentValue);
    }

    void setValue(Object value) {
        int oldIndex = getSelectedIndex();
        if (value instanceof Integer) {
            currentValue = ((Integer) value).doubleValue();
        } else {
            currentValue = ((Double) value).doubleValue();
        }
        if (oldIndex != getSelectedIndex()) {
            selectionListener.fireSelectionEvent(oldIndex, getSelectedIndex());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getItemAt(int index) {
        if (realValues) {
            return Double.valueOf(min + step * index);
        }
        return Integer.valueOf((int) (min + step * index));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() {
        return (int) ((max - min) / step) + maxOffset;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getSelectedIndex() {
        return (int) ((currentValue - min) / step);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSelectedIndex(int index) {
        if (setSelectedIndexReentrantLock) {
            return;
        }
        setSelectedIndexReentrantLock = true;
        try {
            int oldIndex = getSelectedIndex();
            currentValue = min + index * step;
            int newIndex = getSelectedIndex();
            if (oldIndex != newIndex) {
                selectionListener.fireSelectionEvent(oldIndex, newIndex);
            }
        } finally {
            setSelectedIndexReentrantLock = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDataChangedListener(DataChangedListener l) {
        dataListener.addListener(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeDataChangedListener(DataChangedListener l) {
        dataListener.removeListener(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSelectionListener(SelectionListener l) {
        selectionListener.addListener(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSelectionListener(SelectionListener l) {
        selectionListener.removeListener(l);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addItem(Object item) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeItem(int index) {
    }

    /**
     * @return the min
     */
    public double getMin() {
        return min;
    }

    /**
     * @return the max
     */
    public double getMax() {
        return max;
    }
}

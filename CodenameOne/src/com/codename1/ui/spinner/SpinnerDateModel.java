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

import java.util.Calendar;
import java.util.Date;

/**
 * Represents a date model for the spinner
 *
 * @author Shai Almog
 * @deprecated use Picker instead
 */
class SpinnerDateModel implements ListModel {
    private static final long DAY = 24 * 60 * 60 * 1000;
    private EventDispatcher dataListener = new EventDispatcher();
    private EventDispatcher selectionListener = new EventDispatcher();
    private long min;
    private long max;
    private long currentValue;

    /**
     * Indicates the range of the spinner
     *
     * @param min          lowest value allowed
     * @param max          maximum value allowed
     * @param currentValue the starting value for the mode
     */
    public SpinnerDateModel(long min, long max, long currentValue) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(max));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.add(Calendar.MINUTE, 0);
        cal.add(Calendar.SECOND, 0);
        cal.add(Calendar.MILLISECOND, 0);
        //this.max = max - max % DAY;
        this.max = cal.getTime().getTime();

        cal.setTime(new Date(min));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.add(Calendar.MINUTE, 0);
        cal.add(Calendar.SECOND, 0);
        cal.add(Calendar.MILLISECOND, 0);
        //this.min = min - min % DAY;
        this.min = cal.getTime().getTime();

        cal.setTime(new Date(currentValue));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.add(Calendar.MINUTE, 0);
        cal.add(Calendar.SECOND, 0);
        cal.add(Calendar.MILLISECOND, 0);
        //this.currentValue = currentValue - currentValue % DAY + 12 * 60 * 60000;
        this.currentValue = cal.getTime().getTime();
    }

    Object getValue() {
        //return new Date(currentValue - currentValue % DAY + 12 * 60 * 60000);
        return new Date(currentValue);
    }

    void setValue(Date value) {
        int oldIndex = getSelectedIndex();
        Calendar cal = Calendar.getInstance();
        cal.setTime(value);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.add(Calendar.MINUTE, 0);
        cal.add(Calendar.SECOND, 0);
        cal.add(Calendar.MILLISECOND, 0);
        //currentValue = value.getTime() - value.getTime() % DAY + 12 * 60 * 60000;
        currentValue = cal.getTime().getTime();
        if (oldIndex != getSelectedIndex()) {
            selectionListener.fireSelectionEvent(oldIndex, getSelectedIndex());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getItemAt(int index) {
        //return new Date(min + DAY * index + 12 * 60 * 60000);
        return new Date(min + DAY * index);
    }


    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return (int) ((max - min) / DAY) + 1;
    }


    /**
     * {@inheritDoc}
     */
    public int getSelectedIndex() {
        int out = (int) ((currentValue - min) / DAY);
        return out;
    }


    /**
     * {@inheritDoc}
     */
    public void setSelectedIndex(int index) {
        int oldIndex = getSelectedIndex();
        currentValue = min + (index * DAY);
        int newIndex = getSelectedIndex();
        selectionListener.fireSelectionEvent(oldIndex, newIndex);
    }

    /**
     * {@inheritDoc}
     */
    public void addDataChangedListener(DataChangedListener l) {
        dataListener.addListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeDataChangedListener(DataChangedListener l) {
        dataListener.removeListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void addSelectionListener(SelectionListener l) {
        selectionListener.addListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeSelectionListener(SelectionListener l) {
        selectionListener.removeListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void addItem(Object item) {
    }

    /**
     * {@inheritDoc}
     */
    public void removeItem(int index) {
    }
}

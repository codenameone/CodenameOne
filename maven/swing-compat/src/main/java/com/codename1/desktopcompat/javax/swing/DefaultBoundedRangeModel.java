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

import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.javax.swing.event.EventListenerList;
import java.util.EventListener;

/// The bounded range model every slider, progress bar and scroll bar
/// starts with. A value outside the range is moved into it; a change of
/// any property notifies the change listeners once.
public class DefaultBoundedRangeModel implements BoundedRangeModel {

    protected transient ChangeEvent changeEvent;

    protected EventListenerList listenerList = new EventListenerList();

    private int value;
    private int extent;
    private int min;
    private int max = 100;
    private boolean isAdjusting;

    public DefaultBoundedRangeModel() {
    }

    public DefaultBoundedRangeModel(int value, int extent, int min, int max) {
        if (max >= min && value >= min && value + extent >= value && value + extent <= max) {
            this.value = value;
            this.extent = extent;
            this.min = min;
            this.max = max;
        } else {
            throw new IllegalArgumentException("invalid range properties");
        }
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public int getExtent() {
        return extent;
    }

    @Override
    public int getMinimum() {
        return min;
    }

    @Override
    public int getMaximum() {
        return max;
    }

    @Override
    public void setValue(int n) {
        n = Math.min(n, Integer.MAX_VALUE - extent);
        int newValue = Math.max(n, min);
        if (newValue + extent > max) {
            newValue = max - extent;
        }
        setRangeProperties(newValue, extent, min, max, isAdjusting);
    }

    @Override
    public void setExtent(int n) {
        int newExtent = Math.max(0, n);
        if (value + newExtent > max) {
            newExtent = max - value;
        }
        setRangeProperties(value, newExtent, min, max, isAdjusting);
    }

    @Override
    public void setMinimum(int n) {
        int newMax = Math.max(n, max);
        int newValue = Math.max(n, value);
        int newExtent = Math.min(newMax - newValue, extent);
        setRangeProperties(newValue, newExtent, n, newMax, isAdjusting);
    }

    @Override
    public void setMaximum(int n) {
        int newMin = Math.min(n, min);
        int newExtent = Math.min(n - newMin, extent);
        int newValue = Math.min(n - newExtent, value);
        setRangeProperties(newValue, newExtent, newMin, n, isAdjusting);
    }

    @Override
    public void setValueIsAdjusting(boolean b) {
        setRangeProperties(value, extent, min, max, b);
    }

    @Override
    public boolean getValueIsAdjusting() {
        return isAdjusting;
    }

    @Override
    public void setRangeProperties(int newValue, int newExtent, int newMin, int newMax, boolean adjusting) {
        if (newMin > newMax) {
            newMin = newMax;
        }
        if (newValue > newMax) {
            newMax = newValue;
        }
        if (newValue < newMin) {
            newMin = newValue;
        }
        if ((long) newExtent + (long) newValue > newMax) {
            newExtent = newMax - newValue;
        }
        if (newExtent < 0) {
            newExtent = 0;
        }
        boolean changed = newValue != value || newExtent != extent || newMin != min || newMax != max
                || adjusting != isAdjusting;
        if (changed) {
            value = newValue;
            extent = newExtent;
            min = newMin;
            max = newMax;
            isAdjusting = adjusting;
            fireStateChanged();
        }
    }

    @Override
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        ChangeListener[] ls = getChangeListeners();
        for (int i = ls.length - 1; i >= 0; i--) {
            if (changeEvent == null) {
                changeEvent = new ChangeEvent(this);
            }
            ls[i].stateChanged(changeEvent);
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[value=" + value + ", extent=" + extent + ", min=" + min + ", max=" + max
                + ", adj=" + isAdjusting + "]";
    }
}

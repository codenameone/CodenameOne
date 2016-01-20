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
package com.codename1.ui.list;

import com.codename1.ui.List;
import com.codename1.ui.TextField;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class allows filtering/sorting a list model dynamically using a text field
 *
 * @author Shai Almog
 */
public class FilterProxyListModel<T> implements ListModel<T>, DataChangedListener {
    private ListModel<T> underlying;
    private ArrayList<Integer> filter;
    private ArrayList<DataChangedListener> listeners = new ArrayList<DataChangedListener>();
    private boolean startsWithMode;
    
    /**
     * The proxy is applied to the actual model and effectively hides it
     * @param underlying the "real" model for the list
     */
    public FilterProxyListModel(ListModel<T> underlying) {
        this.underlying = underlying;
        underlying.addDataChangedListener(this);
    }
    
    private int getFilterOffset(int index) {
        if(filter == null) {
            return index;
        }
        if(filter.size() > index) {
            return filter.get(index).intValue();
        }
        return -1;
    }

    /**
     * This method performs a sort of the list, to determine the sort order this class should be derived
     * and the compare() method should be overriden
     * @param ascending sort in ascending order
     */
    public void sort(boolean ascending) {
        if(filter == null) {
            filterImpl("");
        }
        Integer[] filterArray = new Integer[filter.size()];
        Integer[] tempArray = new Integer[filter.size()];
        for(int iter = 0 ; iter < filter.size() ; iter++) {
            filterArray[iter] = filter.get(iter);
        }
        System.arraycopy(filterArray, 0, tempArray, 0, filterArray.length);
        mergeSort(filterArray, tempArray, 0, filterArray.length, 0, ascending);

        for(int iter = 0 ; iter < filter.size() ; iter++) {
            filter.set(iter, filterArray[iter]);
        }
        dataChanged(DataChangedListener.CHANGED, -1);
    }

    private int compareObj(Object a, Object b, boolean ascending) {
        return compare(underlying.getItemAt(((Integer)a).intValue()),
                underlying.getItemAt(((Integer)b).intValue()), ascending);
    }
    
    /**
     * This method can be overriden by subclasses to allow sorting arbitrary objects within
     * the list, it follows the traditional contract of the compare method in Java
     * @param a first object
     * @param b second object
     * @param ascending direction of sort
     * @return 1, 0 or -1 to indicate the larger/smaller object
     */
    protected int compare(Object a, Object b, boolean ascending) {

        String s1;
        String s2;
        if (a instanceof String) {
            s1 = (String) a;
            s2 = (String) b;
        } else {
            s1 = (String) ((Map) a).get("name");
            s2 = (String) ((Map) b).get("name");
        }
        s1 = s1.toUpperCase();
        s2 = s2.toUpperCase();

        if (ascending) {
            return s1.compareTo(s2);
        } else {
            return -s1.compareTo(s2);
        }
    }
    
    private void swap(Object[] dest, int offset1, int offset2) {
        Object val1 = dest[offset1];
        Object val2 = dest[offset2];
        dest[offset2] = val1;
        dest[offset1] = val2;
    }
    
    private void mergeSort(Object[] src,
				  Object[] dest,
				  int low,
				  int high,
				  int off, boolean ascending) {
	int length = high - low;

	// Insertion sort on smallest arrays
        if (length < 7) {
            for (int i=low; i<high; i++)
                for (int j=i; j>low &&
			 compareObj(dest[j-1], dest[j], ascending)>0; j--)
                    swap(dest, j, j-1);
            return;
        }

        // Recursively sort halves of dest into src
        int destLow  = low;
        int destHigh = high;
        low  += off;
        high += off;
        int mid = (low + high) / 2;
        mergeSort(dest, src, low, mid, -off, ascending);
        mergeSort(dest, src, mid, high, -off, ascending);

        // If list is already sorted, just copy from src to dest.  This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (compareObj(src[mid-1], src[mid], ascending) <= 0) {
            System.arraycopy(src, low, dest, destLow, length);
            return;
        }

        // Merge sorted halves (now in src) into dest
        for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && compareObj(src[p], src[q], ascending)<=0)
                dest[i] = src[p++];
            else
                dest[i] = src[q++];
        }
    }

    private int getUnderlyingOffset(int index) {
        if(filter == null) {
            return index;
        }
        return filter.indexOf(new Integer(index));
    }

    /**
     * Returns the underlying model which is needed to perform mutations on the list. 
     * @return the underlying model
     */
    public ListModel getUnderlying() {
        return underlying;
    }

    private void filterImpl(String str) {
        filter = new ArrayList<Integer>();
        str = str.toUpperCase();
        for(int iter = 0 ; iter < underlying.getSize() ; iter++) {
            Object o = underlying.getItemAt(iter);
            if(o != null) {
                if(check(o, str)) {
                    filter.add(new Integer(iter));                    
                }
            }
        }
    }

    /**
     * Checks whether the filter condition is matched, receives an uppercase version of the 
     * filter string to match against
     * @param o the object being compared
     * @param str the string
     * @return true if match is checked
     */
    protected boolean check(Object o, String str) {
        if(o instanceof Map) {
            Map h = (Map)o;
            if(comp(h.get("name"), str)) {
                return true;
            }
        } else {
            String element = o.toString();
            if(startsWithMode) {
                if(element.toUpperCase().startsWith(str)) {
                    return true;
                }
            } else {
                if(element.toUpperCase().indexOf(str) > -1) {
                    return true;
                }
            }
        }
        return false;
    } 

    private boolean comp(Object val, String str) {
        if(startsWithMode) {
            return val != null && ((String)val).toUpperCase().startsWith(str);
        }
        return val != null && ((String)val).toUpperCase().indexOf(str) > -1;
    }

    /**
     * Filters the list based on the given string
     * @param str the string to filter the list by
     */
    public void filter(String str) {
        filterImpl(str);
        dataChanged(DataChangedListener.CHANGED, -1);
    }
    
    /**
     * {@inheritDoc}
     */
    public T getItemAt(int index) {
        return underlying.getItemAt(getFilterOffset(index));
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        if(filter == null) {
            return underlying.getSize();
        }
        return filter.size();
    }

    /**
     * {@inheritDoc}
     */
    public int getSelectedIndex() {
        return Math.max(0, getUnderlyingOffset(underlying.getSelectedIndex()));
    }

    /**
     * {@inheritDoc}
     */
    public void setSelectedIndex(int index) {
        if(index < 0) {
            underlying.setSelectedIndex(index);
        } else {
            underlying.setSelectedIndex(getFilterOffset(index));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addDataChangedListener(DataChangedListener l) {
        listeners.add(l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeDataChangedListener(DataChangedListener l) {
        listeners.remove(l);
    }

    /**
     * {@inheritDoc}
     */
    public void addSelectionListener(SelectionListener l) {
        underlying.addSelectionListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeSelectionListener(SelectionListener l) {
        underlying.removeSelectionListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void addItem(T item) {
        underlying.addItem(item);
    }

    /**
     * {@inheritDoc}
     */
    public void removeItem(int index) {
        underlying.removeItem(getFilterOffset(index));
    }

    /**
     * {@inheritDoc}
     */
    public void dataChanged(int type, int index) {
        if(index > -1) {
            index = getUnderlyingOffset(index);
            if(index < 0) {
                return;
            }
        }
        for(int iter = 0 ; iter < listeners.size() ; iter++) {
            listeners.get(iter).dataChanged(type, index);
        }
    }


    /**
     * Installs a search field on a list making sure the filter method is invoked properly
     */
    public static void install(final TextField search, final List l) {
        search.addDataChangeListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                FilterProxyListModel f;
                if(l.getModel() instanceof FilterProxyListModel) {
                    f = (FilterProxyListModel)l.getModel();
                } else {
                    if(search.getText().length() == 0) {
                        return;
                    }
                    f = new FilterProxyListModel(l.getModel());
                    l.setModel(f);
                }
                if(search.getText().length() == 0) {
                    l.setModel(f.getUnderlying());
                } else {
                    f.filter(search.getText());
                }
            }
        });        
    }

    /**
     * Installs a search field on a list making sure the filter method is invoked properly
     */
    public static void install(final TextField search, final ContainerList l) {
        search.addDataChangeListener(new DataChangedListener() {
            public void dataChanged(int type, int index) {
                FilterProxyListModel f;
                if(l.getModel() instanceof FilterProxyListModel) {
                    f = (FilterProxyListModel)l.getModel();
                } else {
                    if(search.getText().length() == 0) {
                        return;
                    }
                    f = new FilterProxyListModel(l.getModel());
                    l.setModel(f);
                }
                if(search.getText().length() == 0) {
                    l.setModel(f.getUnderlying());
                } else {
                    f.filter(search.getText());
                }
            }
        });
    }

    /**
     * When enabled this makes the filter check that the string starts with rather than within the index
     * @return the startsWithMode
     */
    public boolean isStartsWithMode() {
        return startsWithMode;
    }

    /**
     * When enabled this makes the filter check that the string starts with rather than within the index
     * @param startsWithMode the startsWithMode to set
     */
    public void setStartsWithMode(boolean startsWithMode) {
        this.startsWithMode = startsWithMode;
    }
}

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
package com.codename1.cloud;

import com.codename1.io.CacheMap;
import com.codename1.io.Log;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.util.EventDispatcher;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Seamlessly creates a cloud based searchable list model
 *
 * @author Shai Almog
 */
public class CloudListModel implements ListModel {
    
    private String key;
    private String value;
    private int visibilityScope;
    private int batchSize = 20;
    private String sortProperty;
    private boolean ascending;
    private int size = -1;
    private EventDispatcher modelListener = new EventDispatcher();
    private EventDispatcher selectionListener = new EventDispatcher();
    private int selectedIndex = 0;
    private Hashtable loadingPlaceholder;
    private CacheMap cache;
    
    /**
     * Creates a list model that shows all the cloud elements that have the given key matching
     * the given value at the visibility scope listed bellow. This model can be further narrowed 
     * down by using the filter functionality bellow.<br>
     * This is effectively equivalent to issuing a queryEquals method, however it polls in batches
     * and caches data as needed.
     * 
     * @param key the key within the object
     * @param value the value to narrow the objects shown on the list
     * @param visibilityScope the scope of the list (CloudObject.ACCESS_* values)
     * @param sortProperty the property by which we sort the entries
     * @param ascending whether the sort is ascending or descending 
     */
    public CloudListModel(String key, String value, int visibilityScope, String sortProperty, boolean ascending) {
        this.key = key;
        this.value = value;
        this.visibilityScope = visibilityScope;
        this.sortProperty = sortProperty;
        this.ascending = ascending;
        cache = new CacheMap(key + "_" + value + visibilityScope);
        cache.setCacheSize(30);
        cache.setStorageCacheSize(100);
        cache.setAlwaysStore(true);
        loadingPlaceholder = new Hashtable();
        loadingPlaceholder.put("Line1", "Loading...");
    }
    
    /**
     * Refreshes the list from the server, this method blocks the EDT until
     * completion.
     */
    public void refresh() {
        Vector vec = cache.getKeysInCache();
        int s = vec.size();
        Vector cld = new Vector();
        for(int iter = 0 ; iter < s ; iter++) {
            Object key = vec.elementAt(iter);
            Object val = cache.get(key);
            if(val != null && val instanceof CloudObject) {
                cld.addElement((CloudObject)val);
            }
        }
        if(cld.size() > 0) {
            CloudObject[] obj = new CloudObject[cld.size()];
            cld.toArray(obj);
            int response = CloudStorage.getInstance().refresh(obj);
            if(response != CloudStorage.RETURN_CODE_SUCCESS) {
                onError(new CloudException(response));
            }
        }
    }

    /**
     * Flushes the cache which might be essential when adding new elements
     */
    public void clearCache() {
        cache.clearAllCache();
    }
    
    /**
     * Sets the size of the local cache
     * @param elements elements to store in cache
     */
    public void setCacheSize(int elements) {
        cache.setCacheSize(elements);
    }
    
    /**
     * Returns the elements cached within the implementation
     * @return the cache size
     */
    public int getCacheSize() {
        return cache.getCacheSize();
    }

    /**
     * Sets the size of the cache in persistent storage
     * @param elements elements to store in cache
     */
    public void setStorageCacheSize(int elements) {
        cache.setStorageCacheSize(elements);
    }
    
    /**
     * Returns the elements cached within the persistent storage
     * @return the cache size
     */
    public int getStorageCacheSize() {
        return cache.getCacheSize();
    }
        
    
    /**
     * When loading the element from the cloud a placehold is shown to indicate to the user that the content isn't fully
     * here
     * @param loadingPlaceholder shows blank content to the user
     */
    public void setLoadingPlaceholder(Hashtable loadingPlaceholder) {
        this.loadingPlaceholder = loadingPlaceholder;
    }
    
    /**
     * When loading the element from the cloud a placehold is shown to indicate to the user that the content isn't fully
     * here
     * 
     * @return the element shown when content isn't available for a specific entry
     */
    public Hashtable getLoadingPlaceholder() {
        return loadingPlaceholder;
    }
    
    /**
     * Returns the batch size fetched from the server
     * @return the size
     */
    public int getBatchSize() {
        return batchSize;
    }
    
    /**
     * Updates the number of elements fetched from the server in a single batch
     * @param batchSize the batch size
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    /**
     * @inheritDoc
     */
    public Object getItemAt(int index) {
        if(index < size && index > -1){
            Object key = new Integer(index);
            Object value = cache.get(key);
            if(value == null) {
                value = getLoadingPlaceholder();
                fillUpList(index);
            }
            return value;
        }
        
        return null;
    }

    private void fillUpList(final int startIndex) {
        for(int iter = startIndex ; iter < size ; iter++) {
            Integer key = new Integer(iter);
            if(cache.get(key) == null) {
                cache.put(key, value);
            }
        }
        CloudStorage.getInstance().queryEquals(key, value, startIndex, batchSize, visibilityScope, sortProperty, ascending, new CloudResponse<CloudObject[]>() {
            public void onSuccess(CloudObject[] returnValue) {
                for(int iter = startIndex ; iter < returnValue.length ; iter++) {
                    Integer key = new Integer(iter);
                    cache.put(key, returnValue[iter]);
                }
                modelListener.fireDataChangeEvent(startIndex, returnValue.length);
            }

            public void onError(CloudException err) {
                CloudListModel.this.onError(err);
            }
        });
    }
    
    /**
     * @inheritDoc
     */
    public int getSize() {
        if(size < 0) {
            Integer lstSize = (Integer)cache.get("ListSize");
            if(lstSize != null) {
                size = lstSize.intValue();
                return size;
            }
            size = 0;
            CloudStorage.getInstance().queryEqualsCount(key, value, visibilityScope, new CloudResponse<Integer>() {
                public void onSuccess(Integer returnValue) {
                    size = returnValue.intValue();
                    modelListener.fireDataChangeEvent(-1, DataChangedListener.ADDED);
                }

                public void onError(CloudException err) {
                    CloudListModel.this.onError(err);
                }
            });
        }
        return size;
    }

    /**
     * Invoked when a cloud error occurs
     * @param err the exception representing the error in cloud communications
     */
    protected void onError(CloudException err) {
        Log.e(err);
    }
    
    /**
     * @inheritDoc
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * @inheritDoc
     */
    public void setSelectedIndex(int index) {
        int oldIndex = selectedIndex;
        this.selectedIndex = index;
        selectionListener.fireSelectionEvent(oldIndex, selectedIndex);
    }

    /**
     * @inheritDoc
     */
    public void addDataChangedListener(DataChangedListener l) {
        modelListener.addListener(l);
    }

    /**
     * @inheritDoc
     */
    public void removeDataChangedListener(DataChangedListener l) {
        modelListener.removeListener(l);
    }

    /**
     * @inheritDoc
     */
    public void addSelectionListener(SelectionListener l) {
        selectionListener.addListener(l);
    }

    /**
     * @inheritDoc
     */
    public void removeSelectionListener(SelectionListener l) {
        selectionListener.removeListener(l);
    }

    /**
     * <b>Notice</b> this method does NOT commit the data, after committing the data
     * the cache MUST be cleared!
     * @inheritDoc
     */
    public void addItem(Object item) {
        CloudObject cld = (CloudObject)item;
        if(cld.getObject(key) == null) {
            cld.setString(key, value);
        }
        CloudStorage.getInstance().save(cld);
    }

    /**
     * <b>Notice</b> this method does NOT commit the data, after committing the data
     * the cache MUST be cleared!
     * @inheritDoc
     */
    public void removeItem(int index) {
        Object o = getItemAt(index);
        if(o instanceof CloudObject) {
            CloudObject cld = (CloudObject)o;
            CloudStorage.getInstance().delete(cld);
        }
    }
    
}

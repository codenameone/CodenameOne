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
import com.codename1.io.Util;
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
    
    private String type;
    private int visibilityScope;
    private int batchSize = 20;
    private int keyBatchSize = 1000;
    private int sortProperty;
    private boolean ascending;
    private Object[] keys;
    private EventDispatcher modelListener = new EventDispatcher();
    private EventDispatcher selectionListener = new EventDispatcher();
    private int selectedIndex = 0;
    private Hashtable loadingPlaceholder;
    private CacheMap cache;
    private int index;
    private String queryValue;
    
    /**
     * Refreshes items in the current view every 30 seconds when repainted
     */
    private int refreshRateMillis = 30000;
    
    private long[] lastRefresh;
    
    /**
     * Creates a list model that shows all the cloud elements that have the given key matching
     * the given value at the visibility scope listed bellow. This model can be further narrowed 
     * down by using the filter functionality bellow.<br>
     * This is effectively equivalent to issuing a queryEquals method, however it polls in batches
     * and caches data as needed.
     * 
     * @param type the type of object shown on the list
     * @param visibilityScope the scope of the list (CloudObject.ACCESS_* values)
     * @param sortProperty the index by which we sort the entries, 0 for unsorted
     * @param ascending whether the sort is ascending or descending 
     */
    public CloudListModel(String type, int visibilityScope, int sortProperty, boolean ascending) {
        this.type = type;
        this.visibilityScope = visibilityScope;
        this.sortProperty = sortProperty;
        this.ascending = ascending;
        init();
    }
    
    private void init() {
        cache = new CacheMap(type + visibilityScope + sortProperty);
        cache.setCacheSize(30);
        cache.setStorageCacheSize(100);
        cache.setAlwaysStore(true);
        loadingPlaceholder = new Hashtable();
        loadingPlaceholder.put("Line1", "Loading...");
        loadingPlaceholder.put("Placeholder", Boolean.TRUE);
        
        // remove loading placeholders that might have gotten stuck in cache
        Vector v = cache.getKeysInCache();
        int cacheSize = v.size();
        for(int iter = 0 ; iter < cacheSize ; iter++) {
            Object k = v.elementAt(iter);
            Object e = cache.get(k);
            if(e instanceof Hashtable) {
                Hashtable h = (Hashtable)e;
                if(h.containsKey("Placeholder")) {
                    cache.delete(k);
                }
            }
        }        
    }
        
    /**
     * Creates a list model that shows all the cloud elements that have the given key matching
     * the given value at the visibility scope listed bellow. This model can be further narrowed 
     * down by using the filter functionality bellow.<br>
     * This is effectively equivalent to issuing a queryEquals method, however it polls in batches
     * and caches data as needed.
     * 
     * @param type the type of object shown on the list
     * @param visibilityScope the scope of the list (CloudObject.ACCESS_* values)
     * @param index the index by which we limit the entries
     * @param value the queryValue for the given index
     * @param ascending whether the sort is ascending or descending 
     */
    public CloudListModel(String type, int visibilityScope, int index, String queryValue, boolean ascending) {
        this.type = type;
        this.visibilityScope = visibilityScope;
        this.index = index;
        this.queryValue = queryValue;
        this.ascending = ascending;
        init();
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
            } else {
                // persist the object to the cache and fire the list model change
                for(int iter = 0 ; iter < obj.length ; iter++) {
                    cache.put(obj[iter].getCloudId(), obj[iter]);
                }
                modelListener.fireDataChangeEvent(0, getSize());
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
        this.loadingPlaceholder.put("Placeholder", Boolean.TRUE);
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
     * {@inheritDoc}
     */
    public Object getItemAt(int index) {
        if(keys != null && index < keys.length && index > -1){
            Object value = cache.get(keys[index]);
            if(value == null) {
                value = getLoadingPlaceholder();
                fillUpList(index);
            } else {
                if(value instanceof CloudObject) {
                    long time = System.currentTimeMillis();
                    if(lastRefresh[index] + refreshRateMillis < time) {
                        CloudObject cld = (CloudObject)value;
                        if(cld.getLastModified() > lastRefresh[index]) {
                            lastRefresh[index] = cld.getLastModified();
                        } else {
                            if(cld.getStatus() == CloudObject.STATUS_COMMITTED) {
                                CloudStorage.getInstance().refreshAsync(cld);
                            }
                        }
                    }
                }
            }
            return value;
        }
        
        return null;
    }

    private void fillUpList(final int startIndex) {
        final int len = Math.min(batchSize, keys.length - startIndex);
        Vector<String> request = new Vector<String>();
        for(int iter = startIndex ; iter < startIndex + len ; iter++) {
            if(cache.get(keys[iter]) == null) {
                cache.put(keys[iter], loadingPlaceholder);
                request.addElement((String)keys[iter]);
            }
        }
        CloudResponse<CloudObject[]> resp = new CloudResponse<CloudObject[]>() {
            public void onSuccess(CloudObject[] returnValue) {
                for(int iter = 0 ; iter < returnValue.length ; iter++) {
                    cache.put(returnValue[iter].getCloudId(), returnValue[iter]);
                }
                modelListener.fireDataChangeEvent(startIndex, len);
            }

            public void onError(CloudException err) {
                CloudListModel.this.onError(err);
            }
        };
        String[] arr = new String[request.size()];
        request.toArray(arr);
        CloudStorage.getInstance().fetch(arr, resp);
    }
    
    private void newRefreshRate() {
        lastRefresh = new long[keys.length];
        long t = System.currentTimeMillis();
        for(int iter = 0 ; iter < lastRefresh.length ; iter++) {
            lastRefresh[iter] = t;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public int getSize() {
        if(keys == null) {
            keys = (Object[])cache.get("keyIndex");
            if(keys == null) {
                keys = new Object[0];
            } else {
                newRefreshRate();
            }
            
            // refresh the key list even if we have them in cache since this might have changed
            CloudResponse<String[]> resp = new CloudResponse<String[]>() {
                private int responseOffset;
                public void onSuccess(String[] returnValue) {
                    if(responseOffset == 0) {
                        keys = returnValue;
                    } else {
                        String[] k = new String[keys.length + returnValue.length];
                        Util.mergeArrays(keys, returnValue, k);
                        keys = k;
                    }
                    newRefreshRate();
                    cache.put("keyIndex", keys);
                    modelListener.fireDataChangeEvent(-1, DataChangedListener.ADDED);
                    
                    // we might have more data, send another request
                    if(returnValue.length == keyBatchSize) {
                        responseOffset = keys.length;
                        if(index > 0) {
                            CloudStorage.getInstance().queryEqualsKeys(type, index, queryValue, keys.length, keyBatchSize, visibilityScope, this);
                        } else {
                            CloudStorage.getInstance().querySortedKeys(type, sortProperty, ascending, keys.length, keyBatchSize, visibilityScope, this);
                        }
                    }
                }

                public void onError(CloudException err) {
                    CloudListModel.this.onError(err);
                }
            };
            if(index > 0) {
                CloudStorage.getInstance().queryEqualsKeys(type, index, queryValue, 0, keyBatchSize, visibilityScope, resp);
            } else {
                CloudStorage.getInstance().querySortedKeys(type, sortProperty, ascending, 0, keyBatchSize, visibilityScope, resp);
            }
        }
        return keys.length;
    }

    /**
     * Invoked when a cloud error occurs
     * @param err the exception representing the error in cloud communications
     */
    protected void onError(CloudException err) {
        Log.e(err);
    }
    
    /**
     * {@inheritDoc}
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * {@inheritDoc}
     */
    public void setSelectedIndex(int index) {
        int oldIndex = selectedIndex;
        this.selectedIndex = index;
        selectionListener.fireSelectionEvent(oldIndex, selectedIndex);
    }

    /**
     * {@inheritDoc}
     */
    public void addDataChangedListener(DataChangedListener l) {
        modelListener.addListener(l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeDataChangedListener(DataChangedListener l) {
        modelListener.removeListener(l);
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
     * <b>Notice</b> this method does NOT commit the data, after committing the data
     * the cache MUST be cleared!
     * {@inheritDoc}
     */
    public void addItem(Object item) {
        CloudObject cld = (CloudObject)item;
        if(cld.getType() == null) {
            cld.setType(type);
        }
        CloudStorage.getInstance().save(cld);
    }

    /**
     * <b>Notice</b> this method does NOT commit the data, after committing the data
     * the cache MUST be cleared!
     * {@inheritDoc}
     */
    public void removeItem(int index) {
        Object o = getItemAt(index);
        if(o instanceof CloudObject) {
            CloudObject cld = (CloudObject)o;
            CloudStorage.getInstance().delete(cld);
        }
    }

    /**
     * Indicates the rate in milliseconds in which to poll the server for the current data
     * of elements that are visible at the moment.
     * @return the refreshRateMillis
     */
    public int getRefreshRateMillis() {
        return refreshRateMillis;
    }

    /**
     * Indicates the rate in milliseconds in which to poll the server for the current data
     * of elements that are visible at the moment.
     * @param refreshRateMillis the refreshRateMillis to set
     */
    public void setRefreshRateMillis(int refreshRateMillis) {
        this.refreshRateMillis = refreshRateMillis;
    }
    
}

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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Log;
import com.codename1.io.MultipartRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.ui.Display;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The cloud storage class allows developers to use the Codename One cloud
 * based storage API as a shared database for your application. The database
 * will be visible only to a specific application (based on author/package
 * definitions).<br>
 * The calls to this class only become effective as a single batch when commit
 * is sent. The commit can be synchronous or asynchronous.<br>
 * <b>Important</b> due to the nature of the underlying object data store queries
 * can only be performed against an indexed field of which there are 10 hardcoded
 * indexes! Basic data is case sensitive and queries/sort will be performed in a case
 * sensitive way! In order to work around this create a property with an identical 
 * name that contains the field as lower or upper case in order to query/sort 
 * against.
 *
 * @author Shai Almog
 */
public class CloudStorage {
    static final String SERVER_URL = "https://codename-one.appspot.com"; 
    //static final String SERVER_URL = "http://127.0.0.1:8888"; 
            
    /**
     * Return code for methods in this class indicating a successful operation
     */
    public static final int RETURN_CODE_SUCCESS = 0;

    /**
     * Return code for methods in this class indicating a failure due to server side
     * modification of an object (e.g. different user changed the object)
     */
    public static final int RETURN_CODE_FAIL_OBJECT_MODIFIED = 1;

    /**
     * Return code for methods in this class indicating a failure due to generic server side
     * error
     */
    public static final int RETURN_CODE_FAIL_SERVER_ERROR = 2;

    /**
     * Return code for methods in this class indicating a failure due to exceeding
     * server storage quotas
     */
    public static final int RETURN_CODE_FAIL_QUOTA_EXCEEDED = 3;

    /**
     * Return code for methods in this class indicating an empty queue
     */
    public static final int RETURN_CODE_EMPTY_QUEUE = 4;

    /**
     * Return code for methods in this class indicating a failure due to exceeding
     * server storage quotas
     */
    public static final int RETURN_CODE_FAIL_PERMISSION_VIOLATION = 5;

    /**
     * Indicates the type of the field for queries and filtering
     */
    static final String TYPE_FIELD = "CN1Type";
    
    /**
     * Indicates the index field prefix
     */
    static final String INDEX_FIELD = "CN1Index";

    private static CloudStorage INSTANCE;
    
    private Vector storageQueue;
    
    static {
        Util.register("CloudObject", CloudObject.class);
    }
    
    private CloudStorage() {
        storageQueue = (Vector)Storage.getInstance().readObject("CN1StorageQueue");
        if(storageQueue == null) {
            storageQueue = new Vector();
        }
    }
    
    /**
     * Creates an instance of the cloud storage object, only one instance should be used per application.
     * This method is important since it may block to complete/cleanup a previous transaction that wasn't
     * fully completed before exiting the application. 
     * @return the instance of the class
     */
    public static CloudStorage getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new CloudStorage();
        }
        return INSTANCE;
    }
    
    
    /**
     * Adds the given object to the save queue, the operation will only take place once committed
     * @param object the object to save into the cloud, new objects are inserted. existing
     * objects are updated
     */
    public synchronized void save(CloudObject object) {
        if(storageQueue.contains(object)) {
            storageQueue.remove(object);
        }
        storageQueue.addElement(object);
        Storage.getInstance().writeObject("CN1StorageQueue", storageQueue);
        object.setStatus(CloudObject.STATUS_COMMIT_IN_PROGRESS);
    }
    
    /**
     * Deletes the following object from the cloud storage upon commit
     * @param cl the cloud object to delete
     */
    public synchronized void delete(CloudObject cl) {
        storageQueue.addElement(cl.getCloudId());
        Storage.getInstance().writeObject("CN1StorageQueue", storageQueue);
        cl.setStatus(CloudObject.STATUS_DELETE_IN_PROGRESS);
    }
    
    /**
     * Refresh the given objects with data from the server if they were modified on the server (this is the asynchronous
     * version of the method).
     * This operation executes immeditely without waiting for commit.
     * 
     * @param objects objects to refresh
     * @param callback object for the response
     * @return status code matching the situation, one of: RETURN_CODE_SUCCESS, 
     * RETURN_CODE_FAIL_SERVER_ERROR
     */
    public void refresh(CloudObject[] objects, CloudResponse<Integer> response) {
        refreshImpl(objects, response);
    }
    
    private Vector<CloudObject> pendingRefreshes;
    
    /**
     * Adds the given object to a set of refresh operations in which we don't
     * really care if the operation is successful
     * @param obj the object to refresh
     */
    public void refreshAsync(CloudObject obj) {
        if(pendingRefreshes == null) {
            pendingRefreshes = new Vector<CloudObject>();
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    CloudObject[] arr = new CloudObject[pendingRefreshes.size()];
                    pendingRefreshes.toArray(arr);
                    pendingRefreshes = null;
                    refresh(arr);
                }
            });
        }
        pendingRefreshes.addElement(obj);
        obj.setStatus(CloudObject.STATUS_REFRESH_IN_PROGRESS);
    }
    
    class RefreshConnection extends ConnectionRequest {
        int returnValue;
        CloudObject[] objects;
        CloudResponse<Integer> response;
        
        protected void postResponse() {
            if(response != null) {
                if(returnValue != RETURN_CODE_SUCCESS) {
                    response.onError(new CloudException(returnValue));
                } else {
                    response.onSuccess(new Integer(returnValue));
                }
            }
        }
    
        protected void handleErrorResponseCode(int code, String message) {
            returnValue = RETURN_CODE_FAIL_SERVER_ERROR;
        }
        
        protected void readResponse(InputStream input) throws IOException  {
            DataInputStream di = new DataInputStream(input);

            for(int iter = 0 ; iter  < objects.length ; iter++) {
                try {
                    if(di.readBoolean()) {
                        objects[iter].setLastModified(di.readLong());
                        objects[iter].setValues((Hashtable)Util.readObject(di));
                    }
                    objects[iter].setStatus(CloudObject.STATUS_COMMITTED);
                } catch (IOException ex) {
                    Log.e(ex);
                }
            }

            Util.cleanup(di);

            returnValue = RETURN_CODE_SUCCESS;
        }
    }
    
    private int refreshImpl(CloudObject[] objects, CloudResponse<Integer> response) {
        RefreshConnection refreshRequest = new RefreshConnection();
        refreshRequest.objects = objects;
        refreshRequest.response = response;
        refreshRequest.setPost(true);
        refreshRequest.setUrl(SERVER_URL + "/objStoreRefresh");
        for(int iter = 0 ; iter  < objects.length ; iter++) {
            objects[iter].setStatus(CloudObject.STATUS_REFRESH_IN_PROGRESS);
            refreshRequest.addArgument("i" + iter, objects[iter].getCloudId());
            refreshRequest.addArgument("m" + iter, "" + objects[iter].getLastModified());
        }
        refreshRequest.addArgument("t", CloudPersona.getCurrentPersona().getToken());
        refreshRequest.addArgument("pk", Display.getInstance().getProperty("package_name", null));
        refreshRequest.addArgument("bb", Display.getInstance().getProperty("built_by_user", null));
        
        // async code
        if(response != null) {
            NetworkManager.getInstance().addToQueue(refreshRequest);
            return -1;
        } 
        
        NetworkManager.getInstance().addToQueueAndWait(refreshRequest);
        
        return refreshRequest.returnValue;
    }
    
    /**
     * Refresh the given objects with data from the server if they were modified on the server.
     * This operation executes immeditely without waiting for commit.
     * 
     * @param objects objects to refresh
     * @return status code matching the situation, one of: RETURN_CODE_SUCCESS, 
     * RETURN_CODE_FAIL_SERVER_ERROR
     */
    public int refresh(CloudObject[] objects) {
        return refreshImpl(objects, null);
    }
    
    /**
     * Fetches the objects from the server.
     * This operation executes immeditely without waiting for commit.
     * 
     * @param cloudIds the object id's to fetch
     * @return the cloud objects or null if a server error occurred
     * @throws CloudException thrown for a server side/connection error
     */
    public CloudObject[] fetch(String[] cloudIds) throws CloudException {
        CloudObject[] objs = new CloudObject[cloudIds.length];
        for(int iter = 0 ; iter < objs.length ; iter++) {
            objs[iter] = new CloudObject();
            objs[iter].setCloudId(cloudIds[iter]);
        }
        int err = refresh(objs);
        if(err == RETURN_CODE_SUCCESS) {
            return objs;
        }
        
        throw new CloudException(err);
    }

    /**
     * Fetches the objects from the server asynchronously.
     * This operation executes immeditely without waiting for commit.
     * 
     * @param cloudIds the object id's to fetch
     * @param response returns the response from the server
     * @return the cloud objects or null if a server error occurred
     */
    public void fetch(String[] cloudIds, final CloudResponse<CloudObject[]> response) {
        final CloudObject[] objs = new CloudObject[cloudIds.length];
        for(int iter = 0 ; iter < objs.length ; iter++) {
            objs[iter] = new CloudObject();
            objs[iter].setCloudId(cloudIds[iter]);
        }
        refresh(objs, new CloudResponse<Integer>() {
            public void onSuccess(Integer returnValue) {
                response.onSuccess(objs);
            }

            public void onError(CloudException err) {
                response.onError(err);
            }
        });
    }
    
    /**
     * Performs a query to the server finding the objects where the key
     * value is equal to the given value. 
     * This operation executes immeditely without waiting for commit.
     * 
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return array of objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    public CloudObject[] queryEquals(String type, int index, String value, int page, int limit, int visibilityScope) throws CloudException {
        return (CloudObject[])queryImpl(type, value, index, page, limit, visibilityScope, 1, 0, false, false, false, null);
    }
    
    /**
     * Performs a query to the server finding the objects where the sort is equal to the given value. 
     * This operation executes immeditely without waiting for commit.
     * 
     * @param type the object type
     * @param index the index on which the sore is based
     * @param ascending indicates if the sort order is ascending or descending 
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return array of objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    public CloudObject[] querySorted(String type, int index, boolean ascending, int page, int limit, int visibilityScope) throws CloudException {
        return (CloudObject[])queryImpl(type, null, 0, page, limit, visibilityScope, 1, index, ascending, false, false, null);
    }

    /**
     * Performs a query to the server finding the objects where the sort is equal to the given value and returning
     * the cloud key of these objects. 
     * This operation executes immeditely without waiting for commit.
     * 
     * @param type the object type
     * @param index the index on which the sore is based
     * @param ascending indicates if the sort order is ascending or descending 
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return the keys for the cloud objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    public String[] querySortedKeys(String type, int index, boolean ascending, int page, int limit, int visibilityScope) throws CloudException {
        return (String[])queryImpl(type, null, 0, page, limit, visibilityScope, 1, index, ascending, false, true, null);
    }
    
    /**
     * Equivalent to the standard query but just returns the keys matching the given query
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return the keys for the cloud objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    public String[] queryEqualsKeys(String type, int index, String value, int page, int limit, int visibilityScope) throws CloudException {
        return (String[])queryImpl(type, value, index, page, limit, visibilityScope, 1, 0, false, false, true, null);
    }

    /**
     * Equivalent to the standard query but just returns the total count of entries that will be returned
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return the number of elements
     * @throws CloudException thrown for a server side/connection error
     */
    public int queryEqualsCount(String type, int index, String value, int visibilityScope) throws CloudException {
        return ((Integer)queryImpl(type, value, index, 0, 0, visibilityScope, 1, 0, false, true, false, null)).intValue();
    }

    /**
     * Equivalent to the standard query but just returns the total count of entries that will be returned
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return the number of elements
     * @throws CloudException thrown for a server side/connection error
     */
    public int queryGreaterThanCount(String type, int index, String value, int visibilityScope) throws CloudException {
        return ((Integer)queryImpl(type, value, index, 0, 0, visibilityScope, 2, 0, false, true, false, null)).intValue();
    }

    /**
     * Equivalent to the standard query but just returns the total count of entries that will be returned
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return the number of elements
     * @throws CloudException thrown for a server side/connection error
     */
    public int queryLessThanCount(String type, int index, String value, int visibilityScope) throws CloudException {
        return ((Integer)queryImpl(type, value, index, 0, 0, visibilityScope, 3, 0, false, true, false, null)).intValue();
    }

    /**
     * Performs a query to the server finding the objects where the key
     * value is greater than the given value. 
     * This operation executes immeditely without waiting for commit.
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return array of objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    public CloudObject[] queryGreaterThan(String type, int index, String value, int page, int limit, int visibilityScope) throws CloudException {
        return (CloudObject[])queryImpl(type, value, index, page, limit, visibilityScope, 2, 0, false, false, false, null);
    }

    /**
     * Performs a query to the server finding the objects where the key
     * value is smaller than the given value. 
     * This operation executes immeditely without waiting for commit.
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return array of objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    public CloudObject[] queryLessThan(String type, int index, String value, int page, int limit, int visibilityScope) throws CloudException {
        return (CloudObject[])queryImpl(type, value, index, page, limit, visibilityScope, 3, 0, false, false, false, null);
    }
    
    class QueryRequest extends ConnectionRequest {
        int returnValue = RETURN_CODE_FAIL_SERVER_ERROR;
        CloudResponse response;
        boolean countQuery;
        boolean keyQuery;
        Object returnObject;
        
        protected void postResponse() {
            if(response != null) {
                if(returnValue != RETURN_CODE_SUCCESS) {
                    response.onError(new CloudException(returnValue));
                } else {
                    response.onSuccess(returnObject);
                }
            }
        }

        protected void handleErrorResponseCode(int code, String message) {
            returnValue = RETURN_CODE_FAIL_SERVER_ERROR;
        }
        
        protected void readResponse(InputStream input) throws IOException  {        
            DataInputStream di = new DataInputStream(input);
            CloudObject[] objects = null;
            try {
                int count = di.readInt();
                if(countQuery) {
                    di.close();
                    returnObject = new Integer(count);
                    returnValue = RETURN_CODE_SUCCESS;
                    return;
                }
                if(keyQuery) {
                    String[] result = new String[count];
                    for(int iter = 0 ; iter  < result.length ; iter++) {
                        result[iter] = di.readUTF();
                    }
                    returnValue = RETURN_CODE_SUCCESS;
                    returnObject = result;
                    return;
                }

                objects = new CloudObject[count];
                for(int iter = 0 ; iter  < objects.length ; iter++) {
                    objects[iter] = new CloudObject(di.readInt());
                    objects[iter].setCloudId(di.readUTF());
                    objects[iter].setLastModified(di.readLong());
                    objects[iter].setValues((Hashtable)Util.readObject(di));
                    objects[iter].setStatus(CloudObject.STATUS_COMMITTED);
                }
                returnValue = RETURN_CODE_SUCCESS;
                returnObject = objects;
            } catch (IOException ex) {
                Log.e(ex);
                returnValue = RETURN_CODE_FAIL_SERVER_ERROR;
            } finally {
                Util.cleanup(di);
            }
        }
    }
    
    private Object queryImpl(String type, String value, int index, int page, int limit, int visibilityScope, int operator, int sort, boolean asc, boolean countQuery, boolean keyQuery, CloudResponse response) throws CloudException {
        if(CloudPersona.getCurrentPersona().getToken() == null) {
            CloudPersona.createAnonymous();
        }
        QueryRequest queryRequest = new QueryRequest();
        queryRequest.response = response;
        queryRequest.countQuery = countQuery;
        queryRequest.keyQuery = keyQuery;
        queryRequest.setPost(true);
        queryRequest.setUrl(SERVER_URL + "/objStoreQuery");
        
        queryRequest.addArgument("t", CloudPersona.getCurrentPersona().getToken());
        queryRequest.addArgument("pk", Display.getInstance().getProperty("package_name", null));
        queryRequest.addArgument("bb", Display.getInstance().getProperty("built_by_user", null));        
        queryRequest.addArgument("ty", type);
        if(value != null && index > 0) {
            queryRequest.addArgument("k", INDEX_FIELD + index);
            queryRequest.addArgument("v", value);
        }
        queryRequest.addArgument("p", "" + page);
        queryRequest.addArgument("l", "" + limit);
        queryRequest.addArgument("sc", "" + visibilityScope);
        if(sort != 0) {
            queryRequest.addArgument("s", INDEX_FIELD + sort);
            if(asc) {
                queryRequest.addArgument("sd", "0");
            } else {
                queryRequest.addArgument("sd", "1");
            }
        }
        if(countQuery) {
            queryRequest.addArgument("c", "1");
        } else {
            if(keyQuery) {
                queryRequest.addArgument("c", "2");
            }
        }
        
        queryRequest.addArgument("o", "" + operator);
        
        if(response != null) {
            NetworkManager.getInstance().addToQueue(queryRequest);
            return null;
            
        } 
        NetworkManager.getInstance().addToQueueAndWait(queryRequest);
        if(queryRequest.returnValue != RETURN_CODE_SUCCESS) {
            throw new CloudException(queryRequest.returnValue);
        }
        
        return queryRequest.returnObject;
    }
    
    /**
     * Performs a query to the server finding the objects where the sort is equal to the given value. 
     * This operation executes immeditely without waiting for commit.
     * 
     * @param type the object type
     * @param index the index on which the sore is based
     * @param ascending indicates if the sort order is ascending or descending 
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     */
    public void querySorted(String type, int index, boolean ascending, int page, int limit, int visibilityScope, CloudResponse<CloudObject[]> response) {
        try {
            queryImpl(type, null, 0, page, limit, visibilityScope, 1, index, ascending, false, false, response);
        } catch(CloudException e) {
            // won't happen
            response.onError(e);
        }
    }

    /**
     * Performs a query to the server finding the objects where the key
     * value is equal to the given value. 
     * This operation executes immeditely without waiting for commit.
     * 
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     */
    public void queryEquals(String type, int index, String value, int page, int limit, int visibilityScope, CloudResponse<CloudObject[]> response) {
        try {
            queryImpl(type, value, index, page, limit, visibilityScope, 1, 0, false, false, false, response);
        } catch(CloudException e) {
            // won't happen
            response.onError(e);
        }
    }
    
    /**
     * Equivalent to the standard query but just returns the total count of entries that will be returned
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     */
    public void queryEqualsCount(String type, int index, String value, int visibilityScope, CloudResponse<Integer> response) {
        try {
            queryImpl(type, value, index, 0, 0, visibilityScope, 1, 0, false, true, false, response);
        } catch(CloudException e) {
            // won't happen
            response.onError(e);
        }
    }

    /**
     * Equivalent to the standard query but just returns the total count of entries that will be returned
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return the number of elements
     */
    public void queryGreaterThanCount(String type, int index, String value, int visibilityScope, CloudResponse<Integer> response) {
        try {
            queryImpl(type, value, index, 0, 0, visibilityScope, 2, 0, false, true, false, response);
        } catch(CloudException e) {
            // won't happen
            response.onError(e);
        }
    }
    
    /**
     * Performs a query to the server finding the objects where the sort is equal to the given value and returning
     * the cloud key of these objects. 
     * This operation executes immeditely without waiting for commit.
     * 
     * @param type the object type
     * @param index the index on which the sore is based
     * @param ascending indicates if the sort order is ascending or descending 
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     */
    public void querySortedKeys(String type, int index, boolean ascending, int page, int limit, int visibilityScope, CloudResponse<String[]> response) {
        try {
            queryImpl(type, null, 0, page, limit, visibilityScope, 1, index, ascending, false, true, response);
        } catch(CloudException e) {
            // won't happen
            response.onError(e);
        }
    }
    
    /**
     * Equivalent to the standard query but just returns the keys matching the given query
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     */
    public void queryEqualsKeys(String type, int index, String value, int page, int limit, int visibilityScope, CloudResponse<String[]> response) {
        try {
            queryImpl(type, value, index, page, limit, visibilityScope, 1, 0, false, false, true, response);
        } catch(CloudException e) {
            // won't happen
            response.onError(e);
        }
    }

    /**
     * Equivalent to the standard query but just returns the total count of entries that will be returned
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     */
    public void queryLessThanCount(String type, int index, String value, int visibilityScope, CloudResponse<Integer> response) {
        try {
            queryImpl(type, value, index, 0, 0, visibilityScope, 3, 0, false, true, false, response);
        } catch(CloudException e) {
            // won't happen
            response.onError(e);
        }
    }

    /**
     * Performs a query to the server finding the objects where the key
     * value is greater than the given value. 
     * This operation executes immeditely without waiting for commit.
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     */
    public void queryGreaterThan(String type, int index, String value, int page, int limit, int visibilityScope, CloudResponse<CloudObject[]> response) {
        try {
            queryImpl(type, value, index, page, limit, visibilityScope, 2, 0, false, false, false, response);
        } catch(CloudException e) {
            // won't happen
            response.onError(e);
        }
    }

    /**
     * Performs a query to the server finding the objects where the key
     * value is smaller than the given value. 
     * This operation executes immeditely without waiting for commit.
     * @param type the object type
     * @param index the index to query for the given value
     * @param value the value of said index to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return array of objects matching the query
     */
    public void queryLessThan(String type, int index, String value, int page, int limit, int visibilityScope, CloudResponse<CloudObject[]> response) {
        try {
            queryImpl(type, value, index, page, limit, visibilityScope, 3, 0, false, false, false, response);
        } catch(CloudException e) {
            // won't happen
            response.onError(e);
        }
    }    
    
    /**
     * Allows uploading of images etc. to the cloud which can later on be referenced as URL's.
     * 
     * @param mimeType the mimetype of the uploaded file
     * @param file the URL of the local file
     * @return an ID for the given file that can be used to delete the file or construct a URL of the file
     * @throws CloudException in case of a server side error 
     * @throws IOException when a problem occurs with the file
     */
    public String uploadCloudFile(String mimeType, String file) throws CloudException, IOException {
        return uploadCloudFileImpl(mimeType, file, null, -1);
    }
    
    /**
     * Allows uploading of images etc. to the cloud which can later on be referenced as URL's.
     * 
     * @param mimeType the mimetype of the uploaded file
     * @param filename a short name for the file uploaded (not a full path)
     * @param data input stream from which to read the file
     * @param dataSize the size in bytes of the input stream (this is essential for file upload to work on all devices!)
     * @return an ID for the given file that can be used to delete the file or construct a URL of the file
     * @throws CloudException in case of a server side error 
     * @throws IOException when a problem occurs with the file
     */
    public String uploadCloudFile(String mimeType, String filename, InputStream data, int dataSize) throws CloudException, IOException {
        return uploadCloudFileImpl(mimeType, filename, data, dataSize);
    }
    
    private String uploadCloudFileImpl(String mimeType, String file, InputStream data, int dataSize) throws CloudException, IOException {
        if(CloudPersona.getCurrentPersona().getToken() == null) {
            CloudPersona.createAnonymous();
        }
        ConnectionRequest req = new ConnectionRequest();
        req.setPost(false);
        req.setUrl(SERVER_URL + "/fileStoreURLRequest");
        req.addArgument("bb", Display.getInstance().getProperty("built_by_user", null));

        NetworkManager.getInstance().addToQueueAndWait(req);
        int rc = req.getResposeCode();
        if(rc != 200) {
            if(rc == 420) {
                throw new CloudException(RETURN_CODE_FAIL_QUOTA_EXCEEDED);
            }
            throw new CloudException(RETURN_CODE_FAIL_SERVER_ERROR);
        }

        String d = new String(req.getResponseData());
        MultipartRequest uploadReq = new MultipartRequest();
        uploadReq.setUrl(d);
        uploadReq.setManualRedirect(false);
        uploadReq.addArgument("bb", Display.getInstance().getProperty("built_by_user", null));
        uploadReq.addArgument("t", CloudPersona.getCurrentPersona().getToken());
        uploadReq.addArgument("pk", Display.getInstance().getProperty("package_name", null));
        if(data == null) {
            int pos = file.lastIndexOf('/');
            String shortName = file;
            if(pos > -1) {
                shortName = file.substring(pos);
            }
            uploadReq.addData(shortName, file, mimeType);
        } else {
            uploadReq.addData(file, data, dataSize, mimeType);
        }
        NetworkManager.getInstance().addToQueueAndWait(uploadReq);
        if(uploadReq.getResposeCode() != 200) {
            throw new CloudException(RETURN_CODE_FAIL_SERVER_ERROR);
        }
        String r = new String(uploadReq.getResponseData());
        if("ERROR".equals(r)) {
            throw new CloudException(RETURN_CODE_FAIL_SERVER_ERROR);
        }
        return r;
    }
    
    /**
     * Deletes a file from the cloud storage
     * 
     * @param fileId the file id to delete
     * @return true if the operation was successful
     */
    public boolean deleteCloudFile(String fileId) {
        if(CloudPersona.getCurrentPersona().getToken() == null) {
            CloudPersona.createAnonymous();
        }
        ConnectionRequest req = new ConnectionRequest();
        req.setPost(false);
        req.setUrl(SERVER_URL + "/CloudFileStorageDelete");
        req.addArgument("i", fileId);
        req.addArgument("t", CloudPersona.getCurrentPersona().getToken());
        NetworkManager.getInstance().addToQueueAndWait(req);
        if(req.getResposeCode() == 200) {
            return new String(req.getResponseData()).equals("OK");
        }
        return false;
    }

    /**
     * Converts a file id to a URL with which the file can be downloaded, notice that the file URL is world 
     * readable!
     * 
     * @param fileId the file ID
     * @return a URL that allows downloading the file
     */
    public String getUrlForCloudFileId(String fileId) {
        return SERVER_URL + "/fileStoreDownload?i=" + fileId;
    }
    
    /**
     * Commit works synchronously and returns one of the return codes above to indicate 
     * the status. 
     * @return status code from the constants in this class
     */
    public synchronized int commit() {
        if(storageQueue.size() > 0) { 
            if(CloudPersona.getCurrentPersona().getToken() == null) {
                CloudPersona.createAnonymous();
            }
            StorageRequest req = new StorageRequest();
            req.setContentType("multipart/form-data");
            req.setUrl(SERVER_URL + "/objStoreCommit");
            req.setPost(true);
            NetworkManager.getInstance().addToQueueAndWait(req);

            int i = req.getReturnCode();
            if(i == RETURN_CODE_SUCCESS) {
                storageQueue.clear();
                Storage.getInstance().deleteStorageFile("CN1StorageQueue");
            }
            return i;
        }
        return RETURN_CODE_EMPTY_QUEUE;
    }

    /**
     * A commit version that works asynchronously and returns one of the return codes above to indicate 
     * the status. 
     * @param response response code with status code from the constants in this class
     */
    public void commit(CloudResponse<Integer> response) {
        if(storageQueue.size() > 0) { 
            if(CloudPersona.getCurrentPersona().getToken() == null) {
                CloudPersona.createAnonymous();
            }
            StorageRequest req = new StorageRequest();
            req.response = response;
            req.setContentType("multipart/form-data");
            req.setUrl(SERVER_URL + "/objStoreCommit");
            req.setPost(true);
            NetworkManager.getInstance().addToQueue(req);
        }
    }
    
    /**
     * Cancels current pending changes
     */
    public synchronized void rollback() {        
        storageQueue.clear();
        Storage.getInstance().deleteStorageFile("CN1StorageQueue");
    }
    

    class StorageRequest extends ConnectionRequest {
        CloudResponse<Integer> response;
        private int returnCode = RETURN_CODE_FAIL_SERVER_ERROR;
        protected void buildRequestBody(OutputStream os) throws IOException {
            DataOutputStream d = new DataOutputStream(os);
            d.writeInt(storageQueue.size());
            d.writeUTF(CloudPersona.getCurrentPersona().getToken());
            d.writeUTF(Display.getInstance().getProperty("package_name", null));
            d.writeUTF(Display.getInstance().getProperty("built_by_user", null));
            for(int iter = 0 ; iter < storageQueue.size() ; iter++) {
                Object e = storageQueue.elementAt(iter);
                if(e instanceof String) {
                    // delete operation
                    d.writeByte(1);
                    d.writeUTF((String)e);
                } else {
                    CloudObject cl = (CloudObject)e;
                    if(cl.getCloudId() == null) {
                        // insert operation
                        d.writeByte(2);
                        d.writeInt(cl.getAccessPermissions());
                        Util.writeObject(cl.getValues(), d);
                    } else {
                        // update operation
                        d.writeByte(3);
                        d.writeUTF(cl.getCloudId());
                        d.writeLong(cl.getLastModified());
                        Util.writeObject(cl.getValues(), d);
                    }
                }
            }
            d.writeInt(1);
        }
        
        protected void readResponse(InputStream input) throws IOException  {
            DataInputStream di = new DataInputStream(input);
            returnCode = di.readInt();
            if(returnCode == RETURN_CODE_SUCCESS) {
                long timeStamp = di.readLong();
                for(int iter = 0 ; iter < storageQueue.size() ; iter++) {
                    Object o = storageQueue.elementAt(iter);
                    if(o instanceof CloudObject) {
                        ((CloudObject)o).setLastModified(timeStamp);
                        ((CloudObject)o).setStatus(CloudObject.STATUS_COMMITTED);
                    }
                }
            }
        }

        protected void postResponse() {
            if(response != null) {
                if(returnCode == RETURN_CODE_SUCCESS) {
                    storageQueue.clear();
                    Storage.getInstance().deleteStorageFile("CN1StorageQueue");
                }
                response.onSuccess(new Integer(returnCode));                
            }
        }
       
        public int getReturnCode() {
            return returnCode;
        }
        
    }
}

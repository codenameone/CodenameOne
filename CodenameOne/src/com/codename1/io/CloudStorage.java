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
package com.codename1.io;

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
 * is sent. The commit can be synchronous or asynchronous.
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

    private static CloudStorage INSTANCE;
    
    private Vector storageQueue = new Vector();
    
    static {
        Util.register("CloudObject", CloudObject.class);
    }
    
    private CloudStorage() {}
    
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
        Storage.getInstance().writeObject("CN1StorageQueue", cl.getCloudId());
        cl.setStatus(CloudObject.STATUS_DELETE_IN_PROGRESS);
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
        ConnectionRequest refreshRequest = new ConnectionRequest();
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
        NetworkManager.getInstance().addToQueueAndWait(refreshRequest);
        if(refreshRequest.getResposeCode() != 200) {
            return RETURN_CODE_FAIL_SERVER_ERROR;
        }
        
        ByteArrayInputStream bi = new ByteArrayInputStream(refreshRequest.getResponseData());
        DataInputStream di = new DataInputStream(bi);
        
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
        
        return RETURN_CODE_SUCCESS;
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
            objs[iter].setCloudId(cloudIds[iter]);
        }
        int err = refresh(objs);
        if(err == RETURN_CODE_SUCCESS) {
            return objs;
        }
        
        throw new CloudException(err);
    }
    
    /**
     * Performs a query to the server finding the objects where the key
     * value is equal to the given value. 
     * This operation executes immeditely without waiting for commit.
     * @param key the key within the object store
     * @param value the value of said key to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @param ignoreCase set to true to ignore case sensitive state
     * @return array of objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    public CloudObject[] queryEquals(String key, String value, int page, int limit, int visibilityScope, boolean ignoreCase) throws CloudException {
        return queryImpl(key, value, page, limit, visibilityScope, 1, ignoreCase);
    }

    /**
     * Performs a query to the server finding the objects where the key
     * value is greater than the given value. 
     * This operation executes immeditely without waiting for commit.
     * @param key the key within the object store
     * @param value the value of said key to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @param ignoreCase set to true to ignore case sensitive state
     * @return array of objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    private CloudObject[] queryGreaterThan(String key, String value, int page, int limit, int visibilityScope, boolean ignoreCase) throws CloudException {
        return queryImpl(key, value, page, limit, visibilityScope, 2, ignoreCase);
    }

    /**
     * Performs a query to the server finding the objects where the key
     * value is smaller than the given value. 
     * This operation executes immeditely without waiting for commit.
     * @param key the key within the object store
     * @param value the value of said key to include in the response object
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @param ignoreCase set to true to ignore case sensitive state
     * @return array of objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    private CloudObject[] querySmallerThan(String key, String value, int page, int limit, int visibilityScope, boolean mine, boolean ignoreCase) throws CloudException {
        return queryImpl(key, value, page, limit, visibilityScope, 3, ignoreCase);
    }
    
    private CloudObject[] queryImpl(String key, String value, int page, int limit, int visibilityScope, int operator, boolean ignoreCase) throws CloudException {
        ConnectionRequest queryRequest = new ConnectionRequest();
        queryRequest.setPost(true);
        queryRequest.setUrl(SERVER_URL + "/objStoreQuery");
        
        queryRequest.addArgument("t", CloudPersona.getCurrentPersona().getToken());
        queryRequest.addArgument("pk", Display.getInstance().getProperty("package_name", null));
        queryRequest.addArgument("bb", Display.getInstance().getProperty("built_by_user", null));
        queryRequest.addArgument("k", key);
        queryRequest.addArgument("v", value);
        queryRequest.addArgument("p", "" + page);
        queryRequest.addArgument("l", "" + limit);
        queryRequest.addArgument("sc", "" + visibilityScope);
        /*if(mine) {
            queryRequest.addArgument("m", "1");
        } else {
            queryRequest.addArgument("m", "0");
        }*/
        
        if(ignoreCase) {
            queryRequest.addArgument("ig", "1");
        } else {
            queryRequest.addArgument("ig", "0");
        }
        queryRequest.addArgument("o", "" + operator);
        NetworkManager.getInstance().addToQueueAndWait(queryRequest);
        if(queryRequest.getResposeCode() != 200) {
            throw new CloudException(RETURN_CODE_FAIL_SERVER_ERROR);
        }
        
        ByteArrayInputStream bi = new ByteArrayInputStream(queryRequest.getResponseData());
        DataInputStream di = new DataInputStream(bi);
        CloudObject[] objects = null;
        try {
            int count = di.readInt();

            objects = new CloudObject[count];
            for(int iter = 0 ; iter  < objects.length ; iter++) {
                objects[iter] = new CloudObject(di.readInt());
                objects[iter].setCloudId(di.readUTF());
                objects[iter].setLastModified(di.readLong());
                objects[iter].setValues((Hashtable)Util.readObject(di));
                objects[iter].setStatus(CloudObject.STATUS_COMMITTED);
            }
        } catch (IOException ex) {
            Log.e(ex);
            throw new CloudException(RETURN_CODE_FAIL_SERVER_ERROR);
        } finally {
            Util.cleanup(di);
        }
        
        return objects;
    }
    
    /**
     * Commit works synchronously and returns one of the return codes above to indicate 
     * the status. 
     * @return status code from the constants in this class
     */
    public synchronized int commit() {
        if(storageQueue.size() > 0) { 
            if(CloudPersona.getCurrentPersona().getToken() == null) {
                CloudPersona.getCurrentPersona().createAnonymous();
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
     * Cancels current pending changes
     */
    public synchronized void rollback() {        
        storageQueue.clear();
        Storage.getInstance().deleteStorageFile("CN1StorageQueue");
    }
    

    class StorageRequest extends ConnectionRequest {
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
        
        public int getReturnCode() {
            return returnCode;
        }
        
    }
}

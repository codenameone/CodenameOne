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
 * @deprecated this API is targeted for removal due to changes in Google App Engine API support
 *
 * @author Shai Almog
 */
public class CloudStorage {
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
    

    private CloudStorage() {
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
    }
    
    /**
     * Deletes the following object from the cloud storage upon commit
     * @param cl the cloud object to delete
     */
    public synchronized void delete(CloudObject cl) {
    }
    
    /**
     * Refresh the given objects with data from the server if they were modified on the server (this is the asynchronous
     * version of the method).
     * This operation executes immeditely without waiting for commit.
     * 
     * @param objects objects to refresh
     * @param response object for the response
     */
    public void refresh(CloudObject[] objects, CloudResponse<Integer> response) {
    }
    
    private Vector<CloudObject> pendingRefreshes;
    
    /**
     * Adds the given object to a set of refresh operations in which we don't
     * really care if the operation is successful
     * @param obj the object to refresh
     */
    public void refreshAsync(CloudObject obj) {
    }
    
    /**
     * Refresh the given objects with data from the server if they were modified on the server.
     * This operation executes immeditely without waiting for commit.
     * 
     * @param objects objects to refresh
     * @return status code matching the situation, one of: RETURN_CODE_SUCCESS, 
     * RETURN_CODE_FAIL_SERVER_ERROR
     * @deprecated this feature is no longer supported
     */
    public int refresh(CloudObject[] objects) {
        return -1;
    }
    
    /**
     * Fetches the objects from the server.
     * This operation executes immediately without waiting for commit.
     * 
     * @param cloudIds the object id's to fetch
     * @return the cloud objects or null if a server error occurred
     * @throws CloudException thrown for a server side/connection error
     */
    public CloudObject[] fetch(String[] cloudIds) throws CloudException {
        throw new CloudException(-1);
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
        return null;
    }
    
    /**
     * Performs a query to the server finding the objects where the sort is equal to the given value. 
     * This operation executes immeditely without waiting for commit.
     * 
     * @param type the object type
     * @param index the index on which the sort is based
     * @param ascending indicates if the sort order is ascending or descending 
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return array of objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    public CloudObject[] querySorted(String type, int index, boolean ascending, int page, int limit, int visibilityScope) throws CloudException {
        return null;
    }

    /**
     * Performs a query to the server finding the objects where the sort is equal to the given value and returning
     * the cloud key of these objects. 
     * This operation executes immeditely without waiting for commit.
     * 
     * @param type the object type
     * @param index the index on which the sort is based
     * @param ascending indicates if the sort order is ascending or descending 
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     * @return the keys for the cloud objects matching the query
     * @throws CloudException thrown for a server side/connection error
     */
    public String[] querySortedKeys(String type, int index, boolean ascending, int page, int limit, int visibilityScope) throws CloudException {
        return null;
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
        return null;
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
        return -1;
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
        return -1;
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
        return -1;
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
        return null;
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
        return null;
    }
    

    /**
     * Performs a query to the server finding the objects where the sort is equal to the given value. 
     * This operation executes immeditely without waiting for commit.
     * 
     * @param type the object type
     * @param index the index on which the sort is based
     * @param ascending indicates if the sort order is ascending or descending 
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     */
    public void querySorted(String type, int index, boolean ascending, int page, int limit, int visibilityScope, CloudResponse<CloudObject[]> response) {
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

    }
    
    /**
     * Performs a query to the server finding the objects where the sort is equal to the given value and returning
     * the cloud key of these objects. 
     * This operation executes immeditely without waiting for commit.
     * 
     * @param type the object type
     * @param index the index on which the sort is based
     * @param ascending indicates if the sort order is ascending or descending 
     * @param page the page of responses (allows for paging)
     * @param limit number of responses to fetch
     * @param visibilityScope indicates the scope in which to look as one of the 
     * CloudObject constants ACCESS_*
     */
    public void querySortedKeys(String type, int index, boolean ascending, int page, int limit, int visibilityScope, CloudResponse<String[]> response) {

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
     * @param response array of objects matching the query
     */
    public void queryLessThan(String type, int index, String value, int page, int limit, int visibilityScope, CloudResponse<CloudObject[]> response) {
    }    
    
    /**
     * Allows uploading of images etc. to the cloud which can later on be referenced as URL's.
     * 
     * @param mimeType the mimetype of the uploaded file
     * @param file the URL of the local file
     * @return an ID for the given file that can be used to delete the file or construct a URL of the file
     * @throws CloudException in case of a server side error 
     * @throws IOException when a problem occurs with the file
     * @deprecated this API is currently deprecated due to Googles cloud storage deprection
     */
    public String uploadCloudFile(String mimeType, String file) throws CloudException, IOException {
        return null;
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
     * @deprecated this API is currently deprecated due to Googles cloud storage deprection
     */
    public String uploadCloudFile(String mimeType, String filename, InputStream data, int dataSize) throws CloudException, IOException {
        return null;
    }
    

    /**
     * Deletes a file from the cloud storage
     * 
     * @param fileId the file id to delete
     * @return true if the operation was successful
     * @deprecated this API is currently deprecated due to Googles cloud storage deprection
     */
    public boolean deleteCloudFile(String fileId) {
        return false;
    }

    /**
     * Deletes all the cloud files under this user, notice that this method
     * is asynchronous and a background server process performs the actual deletion
     * @deprecated this API is currently deprecated due to Googles cloud storage deprection
     */
    public void deleteAllCloudFilesForUser() {
    }
    
    /**
     * Deletes all the cloud files before the given time stamp for the given
     * development account. Notice that this method is meant for internal use 
     * and not for distributable apps since it includes your developer account.
     * This method works in a background server process and returns immediately.
     * @param timestamp the timestamp since epoch (as in System.currentTimemillis).
     * @param developerAccount your developer email
     * @param developerPassword your developer password
     * @deprecated this API is currently deprecated due to Googles cloud storage deprection
     */
    public void deleteAllCloudFilesBefore(long timestamp, String developerAccount, String developerPassword) {
    }

    /**
     * Converts a file id to a URL with which the file can be downloaded, notice that the file URL is world 
     * readable!
     * 
     * @param fileId the file ID
     * @return a URL that allows downloading the file
     * @deprecated this API is currently deprecated due to Googles cloud storage deprection
     */
    public String getUrlForCloudFileId(String fileId) {
        return null;
    }
    
    /**
     * Commit works synchronously and returns one of the return codes above to indicate 
     * the status. 
     * @return status code from the constants in this class
     */
    public synchronized int commit() {
        return RETURN_CODE_EMPTY_QUEUE;
    }

    /**
     * A commit version that works asynchronously and returns one of the return codes above to indicate 
     * the status. 
     * @param response response code with status code from the constants in this class
     */
    public void commit(CloudResponse<Integer> response) {
    }
    
    /**
     * Cancels current pending changes
     */
    public synchronized void rollback() {        
    }
}

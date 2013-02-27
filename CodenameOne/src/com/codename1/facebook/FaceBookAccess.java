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
package com.codename1.facebook;

import com.codename1.components.SliderBridge;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.io.Oauth2;
import com.codename1.io.Oauth2;
import com.codename1.io.Storage;
import com.codename1.io.services.ImageDownloadService;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.Slider;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.list.DefaultListModel;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This is the main access API to the facebook graph API
 * http://developers.facebook.com/docs/reference/api/
 * This class encapsulates the Network access and provide simple methods to acess the Facebook servers.
 * 
 * @author Chen Fishbein
 */
public class FaceBookAccess {

    private static String clientId = "132970916828080";
    private static String redirectURI = "http://www.codenameone.com/";
    private static String clientSecret = "6aaf4c8ea791f08ea15735eb647becfe";
    private static String[] permissions = new String[]{"user_photos",
        "friends_photos", "publish_stream", "read_stream", "user_relationships",
        "user_birthday", "friends_birthday", "friends_relationships",
        "read_mailbox", "user_events", "friends_events", "user_about_me"};
    private static FaceBookAccess instance = new FaceBookAccess();
    private Slider slider;
    private ConnectionRequest current;
    private Vector responseCodeListeners = new Vector();
    private static String token;
    private static final String TEMP_STORAGE = "FaceBookAccesstmp";

    private FaceBookAccess() {
    }

    /**
     * gets the class instance
     * @return a FaceBookAccess object
     */
    public static FaceBookAccess getInstance() {
        return instance;
    }

    private Oauth2 createOAuth() {
        String scope = "";
        if (permissions != null && permissions.length > 0) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                scope += permission + ",";
            }
            scope = scope.substring(0, scope.length() - 1);
        }
        Hashtable additionalParams = new Hashtable();
        String p = Display.getInstance().getPlatformName();
        //on simulator BB and J2ME use the popup display (no need for javascript)
        if(Display.getInstance().getProperty("OS", "SE").equals("SE") || 
                p.equals("rim") || p.equals("me")){
            additionalParams.put("display", "popup");
        }else{
            additionalParams.put("display", "touch");        
        }
        return new Oauth2("https://www.facebook.com/dialog/oauth", clientId, redirectURI, scope, "https://graph.facebook.com/oauth/access_token", clientSecret, additionalParams);
    }

    /**
     * This method creates a component which can authenticate. You will receive either the
     * authentication key or an Exception object within the ActionListener callback method.
     * 
     * @param al a listener that will receive at its source either a token for the service or an exception in case of a failure
     * @return a component that should be displayed to the user in order to perform the authentication
     */
    public Component createAuthComponent(final ActionListener al) {
        return createOAuth().createAuthComponent(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                if(evt.getSource() instanceof String){
                    token = (String) evt.getSource();
                }
                al.actionPerformed(evt);
            }
        });
    }

    /**
     * This method shows an authentication for login form
     * 
     * @param al a listener that will receive at its source either a token for the service or an exception in case of a failure
     * @return a component that should be displayed to the user in order to perform the authentication
     */
    public void showAuthentication(final ActionListener al) {
        createOAuth().showAuthentication(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                if(evt.getSource() instanceof String){
                    token = (String) evt.getSource();
                }
                al.actionPerformed(evt);
            }
        });
    }

    /**
     * This method returns true if the user is authenticated to the facebook service.
     * @return true if authenticated
     */
    public boolean isAuthenticated() {
        return token != null;
    }

    private void checkAuthentication() throws IOException {
        if (!isAuthenticated()) {
            throw new IOException("service is not authenticated, call public void authenticate(String clientId, String redirectURI, String [] permissions) first");
        }
    }

    /**
     * Sets the progress indicator to get network updates on the queries
     * @param slider 
     */
    public void setProgress(Slider slider) {
        this.slider = slider;
    }

    /**
     * This method returns immediately and will call the callback when it returns with
     * the FaceBook Object data.
     *
     * @param faceBookId the object id that we would like to query
     * @param callback the callback that should be updated when the data arrives
     */
    public void getFaceBookObject(String faceBookId, final ActionListener callback) throws IOException {
        checkAuthentication();
        final FacebookRESTService con = new FacebookRESTService(token, faceBookId, "", false);
        con.addResponseListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                if (!con.isAlive()) {
                    return;
                }
                if (callback != null) {
                    callback.actionPerformed(evt);
                }
            }
        });
        if (slider != null) {
            SliderBridge.bindProgress(con, slider);
        }
        for (int i = 0; i < responseCodeListeners.size(); i++) {
            con.addResponseCodeListener((ActionListener) responseCodeListeners.elementAt(i));
        }
        current = con;
        NetworkManager.getInstance().addToQueue(con);
    }

    class Listener implements ActionListener {
        private FacebookRESTService con;
        private ActionListener callback;
        Listener(FacebookRESTService con, ActionListener callback) {
            this.con = con;
            this.callback = callback;
        }
        public void actionPerformed(ActionEvent evt) {
            if (!con.isAlive()) {
                return;
            }
            if (callback != null) {
                callback.actionPerformed(evt);
            }
        }
    }
    
    /**
     * Get a list of FaceBook objects for a given id
     * 
     * @param faceBookId the id to preform the query upon
     * @param itemsConnection the type of the query
     * @param feed
     * @param params
     * @param callback the callback that should be updated when the data arrives
     */
    public void getFaceBookObjectItems(String faceBookId, String itemsConnection,
            final DefaultListModel feed, Hashtable params, final ActionListener callback) throws IOException {
        checkAuthentication();

        final FacebookRESTService con = new FacebookRESTService(token, faceBookId, itemsConnection, false);
        con.setResponseDestination(feed);
        con.addResponseListener(new Listener(con, callback));
        if (params != null) {
            Enumeration keys = params.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                con.addArgument(key, (String) params.get(key));
            }
        }
        if (slider != null) {
            SliderBridge.bindProgress(con, slider);
        }
        for (int i = 0; i < responseCodeListeners.size(); i++) {
            con.addResponseCodeListener((ActionListener) responseCodeListeners.elementAt(i));
        }
        current = con;
        NetworkManager.getInstance().addToQueue(con);
    }

    /**
     * Gets a user from a user id
     *
     * @param userId the user id or null to get detaild on the authenticated user
     * @param user an object to fill with the user details
     * @param callback the callback that should be updated when the data arrives
     */
    public void getUser(String userId, final User user, final ActionListener callback) throws IOException {
        String id = userId;
        if (id == null) {
            id = "me";
        }
        getFaceBookObject(id, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                Vector v = (Vector) ((NetworkEvent) evt).getMetaData();
                Hashtable t = (Hashtable) v.elementAt(0);
                if (user != null) {
                    user.copy(t);
                }
                if (callback != null) {
                    callback.actionPerformed(evt);
                }
            }
        });
    }

    /**
     * Gest a post from a post Id
     *
     * @param postId the postId
     * @param post an Object to fill with the data
     * @param callback the callback that should be updated when the data arrives
     */
    public void getPost(String postId, final Post post, final ActionListener callback) throws IOException {
        getFaceBookObject(postId, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                Vector v = (Vector) ((NetworkEvent) evt).getMetaData();
                Hashtable t = (Hashtable) v.elementAt(0);
                if (post != null) {
                    post.copy(t);
                }
                if (callback != null) {
                    callback.actionPerformed(evt);
                }
            }
        });
    }

    /**
     * Gest a photo from a photo Id
     *
     * @param photoId the photoId
     * @param photo an Object to fill with the data
     * @param callback the callback that should be updated when the data arrives
     */
    public void getPhoto(String photoId, final Photo photo, final ActionListener callback) throws IOException {
        getFaceBookObject(photoId, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                Vector v = (Vector) ((NetworkEvent) evt).getMetaData();
                Hashtable t = (Hashtable) v.elementAt(0);
                if (photo != null) {
                    photo.copy(t);
                }
                if (callback != null) {
                    callback.actionPerformed(evt);
                }
            }
        });
    }

    /**
     * Gest an album from an albumId
     *
     * @param albumId the albumId
     * @param album an Object to fill with the data
     * @param callback the callback that should be updated when the data arrives
     */
    public void getAlbum(String albumId, final Album album, final ActionListener callback) throws IOException {
        getFaceBookObject(albumId, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                Vector v = (Vector) ((NetworkEvent) evt).getMetaData();
                Hashtable t = (Hashtable) v.elementAt(0);
                if (album != null) {
                    album.copy(t);
                }
                if (callback != null) {
                    callback.actionPerformed(evt);
                }
            }
        });
    }

    /**
     * Gets the user news feed, the data is being stored in the given DefaultListModel.
     * By default this method will return last 13 news entries.
     * 
     * @param userId the userid we would like to query
     * @param feed the response to fill
     * @param callback the callback that should be updated when the data arrives
     */
    public void getNewsFeed(String userId, final DefaultListModel feed, final ActionListener callback) throws IOException {
        getNewsFeed(userId, feed, 13, callback);
    }

    /**
     * Gets the user news feed, the data is being stored in the given DefaultListModel.
     * 
     * @param userId the userid we would like to query
     * @param feed the response to fill
     * @param limit the number of items to return
     * @param callback the callback that should be updated when the data arrives
     */
    public void getNewsFeed(String userId, final DefaultListModel feed, int limit, final ActionListener callback) throws IOException {
        Hashtable params = new Hashtable();
        params.put("limit", "" + limit);
        getFaceBookObjectItems(userId, FacebookRESTService.HOME, feed, params, callback);
    }
    
    /**
     * Gets the user wall feed, the data is being stored in the given DefaultListModel.
     * By default this method will return last 13 news entries.
     *
     * @param userId the userid we would like to query
     * @param feed the response fo fill
     * @param callback the callback that should be updated when the data arrives
     */
    public void getWallFeed(String userId, DefaultListModel feed, final ActionListener callback) throws IOException {
        getWallFeed(userId, feed, 13, callback);
    }

    /**
     * Gets the user wall feed, the data is being stored in the given DefaultListModel.
     *
     * @param userId the userid we would like to query
     * @param feed the response to fill
     * @param limit the number of items to return
     * @param callback the callback that should be updated when the data arrives
     */
    public void getWallFeed(String userId, DefaultListModel feed, int limit, final ActionListener callback) throws IOException {
        Hashtable params = new Hashtable();
        params.put("limit", "" + limit);
        getFaceBookObjectItems(userId, FacebookRESTService.FEED, feed, params, callback);
    }
    
    /**
     * Gets the picture of the given facebook object id
     *
     * @param id the object id to query
     * @param label place the image on the given label as an icon
     * @param toScale scale the image to the given dimension
     * @param tempStorage if true place the image in a temp storage
     */
    public void getPicture(String id, final Label label, Dimension toScale, boolean tempStorage) throws IOException {
        checkAuthentication();

        FacebookRESTService fb = new FacebookRESTService(token, id, FacebookRESTService.PICTURE, false);
        fb.addArgument("type", "small");
        String cacheKey = id;
        //check if this image is a temporarey resource and it is not saved
        //already has a permanent image
        if (tempStorage && !Storage.getInstance().exists(id)) {
            cacheKey = TEMP_STORAGE + id;
        }
        ImageDownloadService.createImageToStorage(fb.requestURL(), label, cacheKey, toScale);
    }

    /**
     * Gets the picture of the given facebook object id
     *
     * @param id the object id to query
     * @param callback the callback that should be updated when the data arrives
     * @param tempStorage if true place the image in a temp storage
     */
    public void getPicture(String id, final ActionListener callback, boolean tempStorage) throws IOException {
        checkAuthentication();

        FacebookRESTService fb = new FacebookRESTService(token, id, FacebookRESTService.PICTURE, false);
        fb.addArgument("type", "small");
        String cacheKey = id;
        //check if this image is a temporarey resource and it is not saved
        //already has a permanent image
        if (tempStorage && !Storage.getInstance().exists(id)) {
            cacheKey = TEMP_STORAGE + id;
        }
        ImageDownloadService.createImageToStorage(fb.requestURL(), callback, cacheKey);
    }

    /**
     * Gets the picture of the given facebook object id and stores it in the given List in the offset index
     * 
     * This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     * 
     * @param id the object id to query
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     * @param cacheId a unique identifier to be used to store the image into storage
     * @param toScale the scale of the image to put in the List or null
     * @param tempStorage if true place the image in a temp storage
     */
    public void getPicture(String id, final List targetList, final int targetOffset, final String targetKey,
            Dimension toScale, boolean tempStorage) throws IOException {
        checkAuthentication();

        FacebookRESTService fb = new FacebookRESTService(token, id, FacebookRESTService.PICTURE, false);
        fb.addArgument("type", "small");
        String cacheKey = id;
        //check if this image is a temporarey resource and it is not saved
        //already has a permanent image
        if (tempStorage && !Storage.getInstance().exists(id)) {
            cacheKey = TEMP_STORAGE + id;
        }

        ImageDownloadService.createImageToStorage(fb.requestURL(), targetList, targetOffset, targetKey, cacheKey, toScale);
    }

    /**
     * Gets the photo thumbnail of a Photo Object
     *
     * @param photoId the photo id
     * @param callback the callback that should be updated when the data arrives
     * @param tempStorage if true place the image in a temp storage
     */
    public void getPhotoThumbnail(String photoId, final ActionListener callback, boolean tempStorage) throws IOException {
        checkAuthentication();

        FacebookRESTService fb = new FacebookRESTService(token, photoId, FacebookRESTService.PICTURE, false);
        fb.addArgument("type", "small");
        String cacheKey = photoId;
        //check if this image is a temporarey resource and it is not saved
        //already has a permanent image
        if (tempStorage && !Storage.getInstance().exists(photoId)) {
            cacheKey = TEMP_STORAGE + photoId;
        }
        ImageDownloadService.createImageToStorage(fb.requestURL(), callback, cacheKey);
    }

    /**
     * Gets the photo thumbnail of a Photo Object
     * @param photoId the photo id
     * @param label place the image on the given label as an icon
     * @param toScale scale the image to the given dimension
     * @param tempStorage if true place the image in a temp storage
     */
    public void getPhotoThumbnail(String photoId, final Label label, Dimension toScale, boolean tempStorage) throws IOException {
        checkAuthentication();

        FacebookRESTService fb = new FacebookRESTService(token, photoId, FacebookRESTService.PICTURE, false);
        fb.addArgument("type", "thumbnail");
        String cacheKey = photoId;
        //check if this image is a temporarey resource and it is not saved
        //already has a permanent image
        if (tempStorage && !Storage.getInstance().exists(photoId)) {
            cacheKey = TEMP_STORAGE + photoId;
        }
        ImageDownloadService.createImageToStorage(fb.requestURL(), label, cacheKey, toScale);
    }

    /**
     * Gets the user friends
     *
     * @param userId the id
     * @param friends store friends results into the given model,
     * each entry is an Hashtable Object contaning the Object data
     * @param callback the callback that should be updated when the data arrives
     */
    public void getUserFriends(String userId, DefaultListModel friends, final ActionListener callback) throws IOException {
        getFaceBookObjectItems(userId, FacebookRESTService.FRIENDS, friends, null, callback);
    }

    /**
     * Gets the user albums
     *
     * @param userId the id
     * @param albums store albums results into the given model,
     * each entry is an Hashtable Object contaning the Object data
     * @param callback the callback that should be updated when the data arrives
     */
    public void getUserAlbums(String userId, DefaultListModel albums, final ActionListener callback) throws IOException {
        getFaceBookObjectItems(userId, FacebookRESTService.ALBUMS, albums, null, callback);
    }

    /**
     * Gets the albums photos
     * 
     * @param albumId the id
     * @param photos store photos results into the given model,
     * each entry is an Hashtable Object contaning the Object data
     * @param offset the offset of the photo in the album
     * @param limit amount of photos to bring
     * @param callback the callback that should be updated when the data arrives
     */
    public void getAlbumPhotos(String albumId, DefaultListModel photos, int offset, int limit, final ActionListener callback) throws IOException {
        Hashtable params = new Hashtable();
        params.put("limit", "" + limit);
        params.put("offset", "" + offset);

        getFaceBookObjectItems(albumId, FacebookRESTService.PHOTOS, photos, params, callback);
    }

    /**
     *  Gets the post comments
     *
     * @param postId the id
     * @param comments store comments results into the given model,
     * each entry is an Hashtable Object contaning the Object data
     * @param callback the callback that should be updated when the data arrives
     */
    public void getPostComments(String postId, DefaultListModel comments, final ActionListener callback) throws IOException {
        getFaceBookObjectItems(postId, FacebookRESTService.COMMENTS, comments, null, callback);
    }

    /**
     * Gets the user inbox Threads
     *
     * @param userId the id
     * @param threads store threads results into the given model,
     * each entry is an Hashtable Object contaning the Object data
     * @param limit the amount of thread to return
     * @param callback the callback that should be updated when the data arrives
     */
    public void getUserInboxThreads(String userId, DefaultListModel threads, int limit, final ActionListener callback) throws IOException {
        Hashtable params = new Hashtable();
        params.put("limit", "" + limit);
        getFaceBookObjectItems(userId, FacebookRESTService.INBOX, threads, params, callback);
    }

    /**
     * Post a message on the users wall
     *
     * @param userId the userId
     * @param message the message to post
     */
    public void postOnWall(String userId, String message) throws IOException {
        postOnWall(userId, message, null);
    }

    /**
     * Post like on a given post
     *
     * @param postId the post Id
     */
    public void postLike(String postId) throws IOException {
        postLike(postId, null);
    }

    /**
     * Post a comment on a given post
     *
     * @param postId the post id
     * @param message the message to post
     */
    public void postComment(String postId, String message) throws IOException {
        postComment(postId, message, null);
    }
    
    /**
     * Post a note onto the users wall
     *
     * @param userId the userId
     * @param message the message to post
     */
    public void createNote(String userId, String subject, String message) throws IOException {
        createNote(userId, subject, message, null);
    }    
    
    
    /**
     * Post a message on the users wall
     *
     * @param userId the userId
     * @param message the message to post
     */
    public void postOnWall(String userId, String message, ActionListener callback) throws IOException {
        checkAuthentication();

        FacebookRESTService con = new FacebookRESTService(token, userId, FacebookRESTService.FEED, true);
        con.addArgument("message", "" + message);
        con.addResponseListener(new Listener(con, callback));
        if (slider != null) {
            SliderBridge.bindProgress(con, slider);
        }
        for (int i = 0; i < responseCodeListeners.size(); i++) {
            con.addResponseCodeListener((ActionListener) responseCodeListeners.elementAt(i));
        }
        current = con;
        NetworkManager.getInstance().addToQueueAndWait(con);
    }

    /**
     * Post like on a given post
     *
     * @param postId the post Id
     */
    public void postLike(String postId, ActionListener callback) throws IOException {
        checkAuthentication();
        FacebookRESTService con = new FacebookRESTService(token, postId, FacebookRESTService.LIKES, true);
        con.addResponseListener(new Listener(con, callback));
        if (slider != null) {
            SliderBridge.bindProgress(con, slider);
        }
        for (int i = 0; i < responseCodeListeners.size(); i++) {
            con.addResponseCodeListener((ActionListener) responseCodeListeners.elementAt(i));
        }
        current = con;
        NetworkManager.getInstance().addToQueueAndWait(con);
    }

    /**
     * Post a comment on a given post
     *
     * @param postId the post id
     * @param message the message to post
     */
    public void postComment(String postId, String message, ActionListener callback) throws IOException {
        checkAuthentication();

        FacebookRESTService con = new FacebookRESTService(token, postId, FacebookRESTService.COMMENTS, true);
        con.addResponseListener(new Listener(con, callback));
        con.addArgument("message", "" + message);
        if (slider != null) {
            SliderBridge.bindProgress(con, slider);
        }
        for (int i = 0; i < responseCodeListeners.size(); i++) {
            con.addResponseCodeListener((ActionListener) responseCodeListeners.elementAt(i));
        }
        current = con;
        NetworkManager.getInstance().addToQueueAndWait(con);
    }
    
    /**
     * Post a note onto the users wall
     *
     * @param userId the userId
     * @param message the message to post
     */
    public void createNote(String userId, String subject, String message, ActionListener callback) throws IOException {
        checkAuthentication();
        
        FacebookRESTService con = new FacebookRESTService(token, userId, FacebookRESTService.NOTES, true);
        con.addResponseListener(new Listener(con, callback));
        con.addArgument("subject","" + subject);
        con.addArgument("message", "" + message);
        if (slider != null) {
            SliderBridge.bindProgress(con, slider);
        }
        for (int i = 0; i < responseCodeListeners.size(); i++) {
            con.addResponseCodeListener((ActionListener) responseCodeListeners.elementAt(i));
        }
        current = con;        
        System.out.println(con.getUrl());
        NetworkManager.getInstance().addToQueueAndWait(con);
    }    

    /**
     * Gets the user notifications (this method uses the legacy rest api see http://developers.facebook.com/docs/reference/rest/)
     *
     * @param userId the user id
     * @param startTime Indicates the earliest time to return a notification.
     * This equates to the updated_time field in the notification FQL table. If not specified, this call returns all available notifications.
     * @param includeRead Indicates whether to include notifications that have already been read.
     * By default, notifications a user has read are not included.
     * @param notifications store notifications results into the given model,
     * each entry is an Hashtable Object contaning the Object data
     * @param callback the callback that should be updated when the data arrives
     */
    public void getUserNotifications(String userId, String startTime, boolean includeRead,
            DefaultListModel notifications, final ActionListener callback) throws IOException {
        checkAuthentication();

        final FacebookRESTService con = new FacebookRESTService(token, "https://api.facebook.com/method/notifications.getList", false);
        con.addArgument("start_time", startTime);
        con.addArgument("include_read", new Boolean(includeRead).toString());
        con.addArgument("format", "json");

        con.setResponseDestination(notifications);
        con.addResponseListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                if (!con.isAlive()) {
                    return;
                }
                if (callback != null) {
                    callback.actionPerformed(evt);
                }
            }
        });

        if (slider != null) {
            SliderBridge.bindProgress(con, slider);
        }
        for (int i = 0; i < responseCodeListeners.size(); i++) {
            con.addResponseCodeListener((ActionListener) responseCodeListeners.elementAt(i));
        }
        current = con;
        NetworkManager.getInstance().addToQueue(con);
    }

    /**
     * Gets users requested details ((this method uses the legacy rest api see http://developers.facebook.com/docs/reference/rest/))
     *
     * @param usersIds the users to query
     * @param fields which fields to query on the users see http://developers.facebook.com/docs/reference/rest/users.getInfo/
     * @param callback the result will call the callback with the result
     * to extrct the data preform the following:
     *  public void actionPerformed(ActionEvent evt) {
    Vector data = (Vector) ((NetworkEvent) evt).getMetaData();
    Vector users = (Vector) data.elementAt(0);
     * }
     */
    public void getUsersDetails(String[] usersIds, String[] fields, final ActionListener callback) throws IOException {
        checkAuthentication();

        final FacebookRESTService con = new FacebookRESTService(token, "https://api.facebook.com/method/users.getInfo", false);
        String ids = usersIds[0];
        for (int i = 1; i < usersIds.length; i++) {
            ids += "," + usersIds[i];

        }
        con.addArgumentNoEncoding("uids", ids);

        String fieldsStr = fields[0];
        for (int i = 1; i < fields.length; i++) {
            fieldsStr += "," + fields[i];

        }
        con.addArgumentNoEncoding("fields", fieldsStr);
        con.addArgument("format", "json");

        con.addResponseListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                if (!con.isAlive()) {
                    return;
                }
                if (callback != null) {
                    callback.actionPerformed(evt);
                }
            }
        });
        if (slider != null) {
            SliderBridge.bindProgress(con, slider);
        }
        for (int i = 0; i < responseCodeListeners.size(); i++) {
            con.addResponseCodeListener((ActionListener) responseCodeListeners.elementAt(i));
        }
        current = con;
        NetworkManager.getInstance().addToQueue(con);
    }

    /**
     * Gets the user events
     *
     * @param userId the user id
     * @param events store events results into the given model,
     * each entry is an Hashtable Object contaning the Object data
     * @param callback the callback that should be updated when the data arrives
     */
    public void getUserEvents(String userId, DefaultListModel events, final ActionListener callback) throws IOException {
        getFaceBookObjectItems(userId, FacebookRESTService.EVENTS, events, null, callback);
    }

    /**
     * Serach for facebook objects
     *
     * @param objectType one of each: post, user, page, event, group, place, checkin
     * @param query the query string to search for
     * @param results store results onto the given model,
     * each entry is an Hashtable Object contaning the Object data
     * @param callback the callback that should be updated when the data arrives
     */
    public void search(String objectType, String query, DefaultListModel results, ActionListener callback) throws IOException {
        Hashtable params = new Hashtable();
        params.put("q", query);
        params.put("type", objectType);

        getFaceBookObjectItems("search", "", results, params, callback);
    }

    /**
     * Kills the current request.
     */
    public void killCurrentRequest() {
        current.kill();
    }

    /**
     * Adds a response listener on the requests
     *
     * @param a response listener
     */
    public void addResponseCodeListener(ActionListener a) {
        responseCodeListeners.addElement(a);
    }

    /**
     * Removes a response listener
     */
    public void removeResponseCodeListener(ActionListener a) {
        responseCodeListeners.removeElement(a);
    }
    
    /**
     * This is a utility method that transforms a DefaultListModel that contains Hashtable entries
     * into a DefaultListModel that will contain FBObject objects that will be initialized with the Hashtable entries
     * 
     * @param hashtablesModel the model to transform, this model should hold it's data has Hashtable entries
     * @param fbObjectClass this is the class of the entries to be created, this class should be a FBObject type
     * @return a DefaultListModel with fbObjectClass objects entries
     * @throws IllegalAccessException if the fbObjectClass.newInstance() fails
     * @throws InstantiationExceptionif the fbObjectClass.newInstance() fails
     */
    public static DefaultListModel createObjectsModel(DefaultListModel hashtablesModel, Class fbObjectClass) throws IllegalAccessException, InstantiationException {
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < hashtablesModel.getSize(); i++) {
            Hashtable table = (Hashtable) hashtablesModel.getItemAt(i);
            FBObject obj = (FBObject) fbObjectClass.newInstance();
            obj.copy(table);
            model.addItem(obj);
        }
        return model;
    }

    /**
     * Deletes all temp storage.
     */
    public void cleanTempStorage() {
        String[] entries = Storage.getInstance().listEntries();
        for (int i = 0; i < entries.length; i++) {
            String key = entries[i];
            if (key.startsWith(TEMP_STORAGE)) {
                Storage.getInstance().deleteStorageFile(key);
            }
        }
    }

    /**
     * The client id (appid) which asks to connect (this is generated when an 
     * app is created see: https://developers.facebook.com/apps)
     * @param clientId 
     */
    public static void setClientId(String clientId) {
        FaceBookAccess.clientId = clientId;
    }

    /**
     * The client secret is been generated by facebook 
     * see: https://developers.facebook.com/apps
     * @param clientSecret 
     */
    public static void setClientSecret(String clientSecret) {
        FaceBookAccess.clientSecret = clientSecret;
    }

    /**
     * The requested permissions of the app 
     * http://developers.facebook.com/docs/reference/api/permissions/
     * @param permissions 
     */
    public static void setPermissions(String[] permissions) {
        FaceBookAccess.permissions = permissions;
    }

    /**
     * This is generated when an app is created see: 
     * https://developers.facebook.com/apps
     * 
     * @param redirectURI 
     */
    public static void setRedirectURI(String redirectURI) {
        FaceBookAccess.redirectURI = redirectURI;
    }
    
    /**
     * Sets the token to the FaceBookAccess class, this is useful if the token 
     * has not yet expired, get the expiration of the token with 
     * Oauth2.getExpires()
     * 
     * @param tok the token of the 
     */
    public static void setToken(String tok){
        token = tok;
    }
    
    /**
     * log out the current user
     */
    public static void logOut(){
        ConnectionRequest req = new ConnectionRequest();
        req.setPost(false);
        req.setUrl("https://www.facebook.com/logout.php?access_token="+token+"&confirm=1&next="+redirectURI);
        NetworkManager.getInstance().addToQueueAndWait(req);
        token = null;
    }
}

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
package com.codename1.push;

import com.codename1.cloud.CloudException;
import com.codename1.cloud.CloudObject;
import com.codename1.cloud.CloudResponse;
import com.codename1.cloud.CloudStorage;
import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.ui.Display;
import java.util.ArrayList;
import java.util.Date;

/**
 * The push inbox works as a simplified mailbox that allows sending and receiving
 * messages from the developer of the application. It only allows communications
 * from the developer to a specific user/all users or from the user to the developer.
 * Notice that this class uses the CloudObject storage and server push support
 * both of which are pro only features.
 *
 * @author Shai Almog
 */
class PushInbox {
    private java.util.List<CloudObject> cache;
    private static PushInbox instance;
    private boolean sync;
    private Runnable updateCallback;
    
    /**
     * Creates an empty push inbox
     */
    private PushInbox() {
    }
    
    public static PushInbox getInstance() {
        if(instance == null) {
            instance = new PushInbox();
        }
        return instance;
    }
    
    /**
     * Updates the inbox against the server, this normally isn't necessary since this will happen automatically when 
     * push is invoked
     */
    public void syncInbox() { 
        if(sync) {
            return;
        }
        sync = true;
        try {
            long t = System.currentTimeMillis();
            long gt = Preferences.get("PushSetupTime", t);
            java.util.List<CloudObject> vec = getCachedMessages();
            CloudObject[] i = CloudStorage.getInstance().queryGreaterThan("PushMessage", 1, "" + gt, 0, 1000, CloudObject.ACCESS_PRIVATE);
            CloudObject[] i2 = CloudStorage.getInstance().queryGreaterThan("PushMessage", 1, "" + gt, 0, 1000, CloudObject.ACCESS_APPLICATION_READ_ONLY);
            for(CloudObject o : i) {
                if(!vec.contains(o)) {
                    vec.add(o);
                }
            }
            for(CloudObject o : i2) {
                if(!vec.contains(o)) {
                    vec.add(o);
                }
            }
            storeCache();
            Preferences.set("PushSetupTime", t);
            if(updateCallback != null && (i.length > 0 || i2.length > 0)) {
                updateCallback.run();
            }
        } catch(CloudException err) {
            err.printStackTrace();
        }
        sync = false;
    }
    
    /**
     * Sets a callback for updating the UI if the data changed
     * @param updateCallback the callback, null to stop receiving updates
     */
    public void setUpdateCallback(Runnable updateCallback) {
        this.updateCallback = updateCallback;
    }
    
    /**
     * Returns true if we are currently in the process of syncing our inbox
     * @return true if our inbox is being synced
     */
    public boolean isSyncing() {
        return sync;
    }
    
    /**
     * Returns the number of messages within the inbox
     * @return the number of current messages
     */
    public int getMessageCount() {
        return getCachedMessages().size();
    }
    
    /**
     * Returns the unique id of the message at the given offset
     * @param offset the offset
     * @return the message id
     */
    public String getMessageId(int offset) {
        return getCachedMessages().get(offset).getCloudId();
    }
    
    
    /**
     * Gets the title of the message at the given offset
     * @param offset the offset of the message
     * @return the title
     */
    public String getTitle(int offset) {
        return getCachedMessages().get(offset).getString("title");
    }
    
    /**
     * Gets the from field of the message at the given offset
     * @param offset the offset of the message
     * @return the value
     */
    public String getFrom(int offset) {
        return getCachedMessages().get(offset).getString("from");
    }

    /**
     * Gets the time of the message at the given offset
     * @param offset the offset of the message
     * @return the value
     */
    public long getTime(int offset) {
        return getCachedMessages().get(offset).getLong("time");
    }

    /**
     * Gets the icon URL of the message at the given offset
     * @param offset the offset of the message
     * @return the value
     */
    public String getIconURL(int offset) {
        return getCachedMessages().get(offset).getString("icon");
    }

    /**
     * Gets the body of the message at the given offset
     * @param offset the offset of the message
     * @return the value
     */
    public String getBody(int offset) {
        return getCachedMessages().get(offset).getString("body");
    }
    
    /**
     * Indicates if the message body is an HTML message
     * @param offset the offset of the message
     * @return the value
     */
    public boolean isHTML(int offset) {
        return getCachedMessages().get(offset).getBoolean("isHTML");
    }

    /**
     * Indicates if the message was read
     * @param offset the offset of the message
     * @return the value
     */
    public boolean isRead(int offset) {
        return getCachedMessages().get(offset).getBoolean("read");
    }
    
    /**
     * Sets whether the message was read or not
     * 
     * @param offset the offset of the message
     * @param r whether the message was read or not
     */
    public void setRead(int offset, boolean r) {
        if(r != isRead(offset)) {
            CloudObject co = getCachedMessages().get(offset);
            co.setBoolean("read", r);
            CloudStorage.getInstance().rollback();
            CloudStorage.getInstance().save(co);
            CloudStorage.getInstance().commit(null);
        }
    }
    
    /**
     * A message can be placed in the push inbox manually, this is very useful for a sort of "welcome" 
     * message that can be inserted into the app. Notice that 
     * 
     * @param from the name of the sender to display
     * @param email allows the receiver to respond via email as well
     * @param title the title of the message
     * @param body the body of the message
     * @param iconUrl an optional URL to an icon that will appear next to the message, the icon should be square
     * @param isHTML true if this is an HTML message false if it is a plain text message
     * @param time the time of the message
     * @return the error code from the commit method
     */
    public int addMessage(String from, String email, String title, String body, String iconUrl, boolean isHTML, Date time) {
        addMessageImpl(from, email, title, body, iconUrl, isHTML, time);
        return CloudStorage.getInstance().commit();
    }

    private CloudObject addMessageImpl(String from, String email, String title, String body, String iconUrl, boolean isHTML, Date time) {
        CloudObject message = new CloudObject("PushMessage", CloudObject.ACCESS_PRIVATE);
        message.setString("from", from);
        message.setString("email", email);
        message.setString("title", title);
        message.setString("body", body);
        message.setString("pack", Display.getInstance().getProperty("package_name", ""));
        message.setString("pushKey", Push.getDeviceKey());
        message.setBoolean("read", false);
        message.setIndexLong(1, time.getTime());
        message.setIndexString(2, from.toUpperCase());
        message.setIndexString(3, title.toUpperCase());
        message.setIndexString(4, Display.getInstance().getProperty("package_name", ""));
        if(iconUrl != null && iconUrl.length() > 0) {
            message.setString("icon", iconUrl);
        }
        message.setBoolean("isHTML", isHTML);
        message.setLong("time", time.getTime());
        getCachedMessages().add(message);
        CloudStorage.getInstance().rollback();
        CloudStorage.getInstance().save(message);
        return message;
    }
    
    /**
     * A message can be placed in the push inbox manually, this is very useful for a sort of "welcome" 
     * message that can be inserted into the app. Notice that 
     * 
     * @param from the name of the sender to display
     * @param email allows the receiver to respond via email as well
     * @param title the title of the message
     * @param body the body of the message
     * @param iconUrl an optional URL to an icon that will appear next to the message, the icon should be square
     * @param isHTML true if this is an HTML message false if it is a plain text message
     * @param time the time of the message
     * @param response  the response code
     */
    public void addMessage(String from, String email, String title, String body, String iconUrl, boolean isHTML, Date time, CloudResponse<Integer> response) {
        addMessageImpl(from, email, title, body, iconUrl, isHTML, time);
        CloudStorage.getInstance().commit(response);
    }
    
    
    /*private ConnectionRequest sendMessageToDeveloperImpl(String from, String title, String body, String responseToMessageId) {
        CloudStorage.getInstance().rollback();
        CloudObject o = addMessageImpl(from, "", title, body, null, false, new Date());
        if(responseToMessageId != null) {
            o.setString("responseTo", responseToMessageId);
        }
        int val = CloudStorage.getInstance().commit();
        if(val != CloudStorage.RETURN_CODE_SUCCESS) {
            return null;
        }
        ConnectionRequest r = new ConnectionRequest();
        r.setPost(true);
        r.setUrl(Display.getInstance().getProperty("cloudServerURL", "https://codename-one.appspot.com/") + "sendPushInboxResponse");
        r.addArgument("from", from);
        r.addArgument("title", title);
        r.addArgument("body", body);
        r.addArgument("packageName", Display.getInstance().getProperty("package_name", ""));
        r.addArgument("email", Display.getInstance().getProperty("built_by_user", ""));
        r.addArgument("deviceKey", Push.getDeviceKey());
        r.addArgument("cid", o.getCloudId());
        if(responseToMessageId != null) {
            r.addArgument("respondingTo", responseToMessageId);
        }
        
        return r;
    }*/
    
    /**
     * Sends a message from the application user to the developer
     * 
     * @param from the name of the person sending the message
     * @param title the title of the message
     * @param body the body of the message
     * @param responseToMessageId the id of the message the user is responding to, null for a new message that isn't responding to
     * anything specific
     * @return true if the sending succeeded and false otherwise
     */
    /*private boolean sendMessageToDeveloper(String from, String title, String body, String responseToMessageId) {
        ConnectionRequest r = sendMessageToDeveloperImpl(from, title, body, responseToMessageId);
        if(r == null) {
            return false;
        }
        NetworkManager.getInstance().addToQueueAndWait(r);
        if(r.getResposeCode() == 200) {
            String objId = new String(r.getResponseData());
            try {
                getCachedMessages().add(CloudStorage.getInstance().fetch(new String[] {objId})[0]);
                storeCache();
                return true;
            } catch(CloudException err) {
                err.printStackTrace();
                return false;
            }
        }
        return false;
    }*/

    private java.util.List<CloudObject> getCachedMessages() {
        if(cache != null) {
            return cache;
        }
        cache = (java.util.List)Storage.getInstance().readObject("CN1$PushInbox");
        if(cache != null) {
            return cache;
        }
        cache = new ArrayList<CloudObject>();
        storeCache();
        
        // first time, we need to setup date
        Preferences.set("PushSetupTime", System.currentTimeMillis());
        
        return cache;
    }
    
    private void storeCache() {
        Storage.getInstance().writeObject("CN1$PushInbox", cache);
    }
}

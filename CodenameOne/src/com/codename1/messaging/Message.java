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
package com.codename1.messaging;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.ui.Display;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a message to be sent using underlying platform e-mail client.
 * 
 * @author Chen
 */
public class Message {

    private String content = "";
    
    private String fileUri = "";
    
    private String mimeType = MIME_TEXT;
    
    private String attachmentMimeType = MIME_IMAGE_JPG;

    private HashMap<String, String> attachments;
    
    private boolean cloudMessageFailSilently = false;
    
    public static final String MIME_TEXT = "text/plain";
    
    public static final String MIME_HTML = "text/html";
    
    public static final String MIME_IMAGE_JPG = "image/jpeg";
    
    public static final String MIME_IMAGE_PNG = "image/png";
    
    /**
     * Constructor with the message body content
     * @param content the message content
     */
    public Message(String content){
        this.content = content;
    }
    
    /**
     * Gets the message content
     * @return content
     */
    public String getContent(){
        return content;
    }
    
    /**
     * Sets the message attachment if exists
     * @param fileUri the file to attach to the message
     */
    public void setAttachment(String fileUri){
        this.fileUri = fileUri;
    }

    /**
     * Returns the attachment map which can be used to add multiple attachments
     * @return a map of file name to mime type that can be used to add attachments
     */
    public Map<String, String> getAttachments() {
        if(attachments == null) {
            attachments = new HashMap<String, String>();
        } 
        if(fileUri != null && attachmentMimeType != null && fileUri.length() > 0 && attachmentMimeType.length() > 0 && !attachments.containsKey(fileUri)) {
            attachments.put(fileUri, attachmentMimeType);
        }
        return attachments;
    }
    
    /**
     * Sets the message mime type.
     * 
     * @param mimeType 
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    /**
     * Gets the message mime type
     * 
     * @return the mime type
     */
    public String getMimeType() {
        return mimeType;
    }
    
    /**
     * Sets the attachment mime type.
     * 
     * @param mimeType 
     */
    public void setAttachmentMimeType(String mimeType) {
        this.attachmentMimeType = mimeType;
    }
    
    /**
     * Gets the attachment mime type
     * 
     * @return 
     */
    public String getAttachmentMimeType() {
        return attachmentMimeType;
    }
    
    /**
     * Gets the message attachment file path
     * @return the file path of the attachment
     */
    public String getAttachment() {
        return fileUri;
    }
    
    /**
     * Send an email using the platform mail client
     * @param recipients array of e-mail addresses  
     * @param subject e-mail subject
     * @param msg the Message to send
     */
    public static void sendMessage(String [] recipients, String subject, Message msg){
        Display.getInstance().sendMessage(recipients, subject, msg);
    }
    

    /**
     * Send an email message using the Codename One cloud to send the message, notice that this API
     * will only work for pro accounts
     * @param sender the name of the sender, notice all email will arrive from Codename One to avoid spam issues
     * @param recipient the email for the recipient
     * @param recipientName the display name for the recipient
     * @param subject e-mail subject
     * @param plainTextBody when sending an HTML message you should also attach a plain text fallback message,
     * this is redundant if the email is a plain text message to begin with
     */
    public void sendMessageViaCloud(String sender, String recipient, String recipientName, String subject, String plainTextBody) {
        NetworkManager.getInstance().addToQueue(createMessage(sender, recipient, recipientName, subject, plainTextBody));
    }

    /**
     * Send an email message using the Codename One cloud to send the message, notice that this API
     * will only work for pro accounts
     * @param sender the name of the sender, notice all email will arrive from Codename One to avoid spam issues
     * @param recipient the email for the recipient
     * @param recipientName the display name for the recipient
     * @param subject e-mail subject
     * @param plainTextBody when sending an HTML message you should also attach a plain text fallback message,
     * this is redundant if the email is a plain text message to begin with
     * @return true if sending succeeded
     */
    public boolean sendMessageViaCloudSync(String sender, String recipient, String recipientName, String subject, String plainTextBody) {
        ConnectionRequest r = createMessage(sender, recipient, recipientName, subject, plainTextBody);
        r.setFailSilently(true);
        NetworkManager.getInstance().addToQueueAndWait(r);
        return r.getResposeCode() == 200;
    }
    
    private ConnectionRequest createMessage(String sender, String recipient, String recipientName, String subject, String plainTextBody) {
        ConnectionRequest cr = new ConnectionRequest();
        cr.setUrl(Display.getInstance().getProperty("cloudServerURL", "https://codename-one.appspot.com/") + "sendEmailServlet");
        cr.setFailSilently(cloudMessageFailSilently);
        cr.setPost(true);
        cr.addArgument("d", Display.getInstance().getProperty("built_by_user", ""));
        cr.addArgument("from", sender);
        cr.addArgument("to", recipient);
        cr.addArgument("re", recipientName);
        cr.addArgument("subject", subject);
        if(mimeType.equals(MIME_TEXT)) {
            cr.addArgument("body", content);
        } else {
            cr.addArgument("body", plainTextBody);
            cr.addArgument("html", content);
        }
        
        return cr;
    }

    /**
     * Indicates whether the cloud message should produce an error dialog if sending failed
     * 
     * @return the cloudMessageFailSilently
     */
    public boolean isCloudMessageFailSilently() {
        return cloudMessageFailSilently;
    }

    /**
     * Indicates whether the cloud message should produce an error dialog if sending failed
     * 
     * @param cloudMessageFailSilently the cloudMessageFailSilently to set
     */
    public void setCloudMessageFailSilently(boolean cloudMessageFailSilently) {
        this.cloudMessageFailSilently = cloudMessageFailSilently;
    }
}

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
 * <p>Represents a message to be sent using underlying platform e-mail client or the cloud.<br>
 * The code below demonstrates sending a simple message with attachments using the devices
 * native email client:
 * </p>
 * <script src="https://gist.github.com/codenameone/3db47a2ff8b35cae6410.js"></script>
 * 
 * <p>
 * The following code demonstrates sending an email via the Codename One cloud, notice that this is a pro
 * specific feature:
 * </p>
 * <script src="https://gist.github.com/codenameone/8229c1d4627ab3a1f17e.js"></script>
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
     * Returns the attachment map which can be used to add multiple attachments.
     * The path needs to point at a full absolute file URI within {@link com.codename1.io.FileSystemStorage},
     * it will not work with {@link com.codename1.io.Storage} files!
     * @return a map of full file paths to mime type that can be used to add attachments
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
     * <p>Send an email message using the Codename One cloud to send the message, notice that this API
     * will only work for pro accounts.</p>
     * <script src="https://gist.github.com/codenameone/8229c1d4627ab3a1f17e.js"></script>
     * 
     * @param sender the name of the sender, notice all email will arrive from Codename One to avoid spam issues
     * @param recipient the email for the recipient
     * @param recipientName the display name for the recipient
     * @param subject e-mail subject
     * @param plainTextBody when sending an HTML message you should also attach a plain text fallback message,
     * this is redundant if the email is a plain text message to begin with
     * @deprecated this functionality is retired and no longer works. You can use the sendgrid cn1lib or similar libraries
     */
    public void sendMessageViaCloud(String sender, String recipient, String recipientName, String subject, String plainTextBody) {
    }

    /**
     * <p>Send an email message using the Codename One cloud to send the message, notice that this API
     * will only work for pro accounts.</p>
     * <script src="https://gist.github.com/codenameone/8229c1d4627ab3a1f17e.js"></script>
     * 
     * @param sender the name of the sender, notice all email will arrive from Codename One to avoid spam issues
     * @param recipient the email for the recipient
     * @param recipientName the display name for the recipient
     * @param subject e-mail subject
     * @param plainTextBody when sending an HTML message you should also attach a plain text fallback message,
     * this is redundant if the email is a plain text message to begin with
     * @return true if sending succeeded
     * @deprecated this functionality is retired and no longer works. You can use the sendgrid cn1lib or similar libraries
     */
    public boolean sendMessageViaCloudSync(String sender, String recipient, String recipientName, String subject, String plainTextBody) {
        return false;
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

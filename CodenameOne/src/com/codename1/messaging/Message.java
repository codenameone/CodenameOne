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

import com.codename1.ui.Display;

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
     * @return 
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
     * @param recieptents array of e-mail addresses
     * @param subject e-mail subject
     * @param msg the Message to send
     */
    public static void sendMessage(String [] recieptents, String subject, Message msg){
        Display.getInstance().sendMessage(recieptents, subject, msg);
    }
    
}

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * <p>A multipart post request allows a developer to submit large binary data 
 * files to the server in a multipart mime post request. This is a standard method
 * for large binary file uploads to webservers and data services. <br>
 * The sample code below includes both the client code using the upload capabilities as
 * well as a simple sample servlet that can accept multipart data:
 * </p>
 * <script src="https://gist.github.com/codenameone/9cad1248365512416101.js"></script>
 * 
 * <p>
 * The sample code below demonstrates uploading to the 
 * <a href="https://www.filestack.com/features-upload" target="_blank">filestack.com</a> API.</p>
 * 
 * <script src="https://gist.github.com/codenameone/7f6e07dadbdd169648ed.js"></script>
 *
 * @author Shai Almog
 */
public class MultipartRequest extends ConnectionRequest {
    private String boundary;
    private LinkedHashMap args = new LinkedHashMap();
    private Hashtable filenames = new Hashtable();
    private Hashtable filesizes = new Hashtable();
    private Hashtable mimeTypes = new Hashtable();
    private static final String CRLF = "\r\n"; 
    private long contentLength = -1L;
    private boolean manualRedirect = true;
    private static boolean canFlushStream = true;
    private Vector ignoreEncoding = new Vector();
    
    /**
     * Initialize variables
     */
    public MultipartRequest() {
        setPost(true);
        setWriteRequest(true);
        
        // Just generate some unique random value.
        boundary = Long.toString(System.currentTimeMillis(), 16); 
        
        // Line separator required by multipart/form-data.
        setContentType("multipart/form-data; boundary=" + boundary);
    }

    /**
     * Returns the boundary string which is normally generated based on system time
     * 
     * @return the multipart boundary string
     */
    public String getBoundary() {
        return boundary;
    }

    /**
     * Sets the boundary string, normally you don't need this method. Its useful to
     * workaround server issues only. Notice that this method must be invoked before adding
     * any elements.
     * @param boundary the boundary string
     */
    public void setBoundary(String boundary) {
        this.boundary = boundary;
        setContentType("multipart/form-data; boundary=" + boundary);
    }
    
    protected void initConnection(Object connection) {
    	contentLength = calculateContentLength();
       	addRequestHeader("Content-Length", Long.toString(contentLength));
        super.initConnection(connection);
    }

    
    /**
     * Adds a binary argument to the arguments
     * @param name the name of the data
     * @param data the data as bytes
     * @param mimeType the mime type for the content
     */
    public void addData(String name, byte[] data, String mimeType) {
        args.put(name, data);
        mimeTypes.put(name, mimeType);
        if(!filenames.containsKey(name)) {
            filenames.put(name, name);
        }
        filesizes.put(name, String.valueOf(data.length));
    }

    /**
     * Adds a binary argument to the arguments
     * 
     * @param name the name of the file data
     * @param filePath the path of the file to upload
     * @param mimeType the mime type for the content
     * @throws IOException if the file cannot be opened
     */
    public void addData(String name, String filePath, String mimeType) throws IOException {
        addData(name, FileSystemStorage.getInstance().openInputStream(filePath), 
                FileSystemStorage.getInstance().getLength(filePath), mimeType);
    }
    
    /**
     * Adds a binary argument to the arguments, notice the input stream will be 
     * read only during submission
     * 
     * @param name the name of the data
     * @param data the data stream
     * @param dataSize the byte size of the data stream, if the data stream is a file
     * the file size can be obtained using the 
     * FileSystemStorage.getInstance().getLength(file) method
     * @param mimeType the mime type for the content
     */
    public void addData(String name, InputStream data, long dataSize, String mimeType) {
        args.put(name, data);
        if(!filenames.containsKey(name)) {
            filenames.put(name, name);
        }
        filesizes.put(name, String.valueOf(dataSize));
        mimeTypes.put(name, mimeType);
    }
    
    /**
     * Sets the filename for the given argument
     * @param arg the argument name 
     * @param filename the file name
     */
    public void setFilename(String arg, String filename) {
        filenames.put(arg, filename);
    }
    
    /**
     * {@inheritDoc}
     */
    public void addArgumentNoEncoding(String key, String value) {
        args.put(key, value);
        if(!filenames.containsKey(key)) {
            filenames.put(key, key);
        }
        ignoreEncoding.addElement(key);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addArgumentNoEncoding(String key, String[] value) {
        addArgument(key, value);
        ignoreEncoding.add(key);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addArgumentNoEncodingArray(String key, String... value) {
        addArgumentNoEncoding(key, (String[])value);
    }
    
    /**
     * {@inheritDoc}
     */
    public void addArgument(String name, String[] value) {
        args.put(name, value);
    }
    
    /**
     * {@inheritDoc}
     */
    public void addArgument(String name, String value) {
        args.put(name, value);
        if(!filenames.containsKey(name)) {
            filenames.put(name, name);
        }
    }

    protected long calculateContentLength() {
        long length = 0L;
        Iterator e = args.keySet().iterator();
        
        long dLength = "Content-Disposition: form-data; name=\"\"; filename=\"\"".length() + 2; // 2 = CRLF
        long ctLength = "Content-Type: ".length() + 2; // 2 = CRLF
        long cteLength = "Content-Transfer-Encoding: binary".length() + 4; // 4 = 2 * CRLF
        long bLength = boundary.length() + 4; // -- + boundary + CRLF
        long baseBinaryLength = dLength + ctLength + cteLength + bLength + 2; // 2 = CRLF at end of part 
        dLength = "Content-Disposition: form-data; name=\"\"".length() + 2;  // 2 = CRLF
        ctLength = "Content-Type: text/plain; charset=UTF-8".length() + 4; // 4 = 2 * CRLF
        long baseTextLength = dLength + ctLength + bLength + 2;  // 2 = CRLF at end of part
        
        while(e.hasNext()) {
                String key = (String)e.next();
            Object value = args.get(key);
            if(value instanceof String) {
                length += baseTextLength;
                length += key.length();
                if(ignoreEncoding.contains(key)) {
                    try {
                        length += value.toString().getBytes("UTF-8").length;
                    } catch (UnsupportedEncodingException ex) {
                        length += value.toString().getBytes().length;
                    }
                } else {
                    length += Util.encodeBody((String)value).length();
                }
            } else {
                if(value instanceof String[]) {
                    for(String s : (String[])value) {
                        length += baseTextLength;
                        length += key.length();
                        if(ignoreEncoding.contains(key)) {
                            try {
                                length += s.toString().getBytes("UTF-8").length;
                            } catch (UnsupportedEncodingException ex) {
                                length += value.toString().getBytes().length;
                            }
                        } else {
                            length += Util.encodeBody(s).length();
                        }
                    }
                } else {
                    length += baseBinaryLength;
                    length += key.length();
                    length += ((String)filenames.get(key)).length();
                    length += ((String)mimeTypes.get(key)).length();
                    length += Long.parseLong((String)filesizes.get(key));
                }
            }
        }
        length += bLength + 2; // same as part boundaries, suffixed with: --
        return length;
    }
    
    /**
     * {@inheritDoc}
     */
    protected void buildRequestBody(OutputStream os) throws IOException {
        Writer writer = null;
        writer = new OutputStreamWriter(os, "UTF-8"); 
        Iterator e = args.keySet().iterator();
        while(e.hasNext()) {
            if (shouldStop()) {
                    break;
            }
            String key = (String)e.next();
            Object value = args.get(key);
            
            writer.write("--");
            writer.write(boundary);
            writer.write(CRLF);
            if(value instanceof String) {
                writer.write("Content-Disposition: form-data; name=\"");
                writer.write(key);
                writer.write("\"");
                writer.write(CRLF);
                writer.write("Content-Type: text/plain; charset=UTF-8");
                writer.write(CRLF);
                writer.write(CRLF);
                if(canFlushStream){
                    writer.flush();
                }                
                if(ignoreEncoding.contains(key)) {
                    writer.write((String)value);
                } else {
                    writer.write(Util.encodeBody((String)value));
                }
                //writer.write(CRLF);
                if(canFlushStream){
                    writer.flush();
                }
            } else { 
                if(value instanceof String[]) {
                    boolean first = true;
                    for(String s : (String[])value) {
                        if(!first) {
                            writer.write(CRLF);
                            writer.write("--");
                            writer.write(boundary);
                            writer.write(CRLF);                            
                        }
                        first = false;
                        writer.write("Content-Disposition: form-data; name=\"");
                        writer.write(key);
                        writer.write("\"");
                        writer.write(CRLF);
                        writer.write("Content-Type: text/plain; charset=UTF-8");
                        writer.write(CRLF);
                        writer.write(CRLF);
                        if(canFlushStream){
                            writer.flush();
                        }                
                        if(ignoreEncoding.contains(key)) {
                            writer.write(s);
                        } else {
                            writer.write(Util.encodeBody(s));
                        }
                        //writer.write(CRLF);
                        if(canFlushStream){
                            writer.flush();
                        }
                    }
                } else { 
                    writer.write("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + filenames.get(key) +"\"");
                    writer.write(CRLF);
                    writer.write("Content-Type: ");
                    writer.write((String)mimeTypes.get(key));
                    writer.write(CRLF);
                    writer.write("Content-Transfer-Encoding: binary");
                    writer.write(CRLF);
                    writer.write(CRLF);
                    if(canFlushStream){
                        writer.flush();
                    }
                    InputStream i;
                    if (value instanceof InputStream) {
                            i = (InputStream)value;
                    } else {
                            i = new ByteArrayInputStream((byte[])value);
                    }
                    byte[] buffer = new byte[8192];
                    int s = i.read(buffer);
                    while(s > -1) {
                            if (shouldStop()) {
                                    break;
                            }
                            os.write(buffer, 0, s);
                            if(canFlushStream){
                                writer.flush();
                            }
                            s = i.read(buffer);
                    }
                    // (when passed by stream, leave for caller to clean up).
                    if (!(value instanceof InputStream)) {
                            Util.cleanup(i);
                    }
                    //args.remove(key);
                    value = null;
                    if(canFlushStream){
                        writer.flush();
                    }
                }
            }
            writer.write(CRLF);
            if(canFlushStream){
                writer.flush();
            }
        }
        
        writer.write("--" + boundary + "--");
        writer.write(CRLF);
        writer.close();
    }

    /* (non-Javadoc)
     * @see com.codename1.io.ConnectionRequest#getContentLength()
     */
    public int getContentLength() {
            return (int)contentLength;
    }

    /* (non-Javadoc)
     * @see com.codename1.io.ConnectionRequest#onRedirect(java.lang.String)
     */
    public boolean onRedirect(String url) {
            return manualRedirect;
    }

    /**
     * By default redirect responses (302 etc.) are handled manually in multipart requests
     * @return the autoRedirect
     */
    public boolean isManualRedirect() {
        return manualRedirect;
    }

    /**
     * By default redirect responses (302 etc.) are handled manually in multipart requests, set this 
     * to false to handle the redirect. Notice that a redirect converts a post to a get.
     * @param autoRedirect the autoRedirect to set
     */
    public void setManualRedirect(boolean autoRedirect) {
        this.manualRedirect = autoRedirect;
    }
    
    /**
     * Sending large files requires flushing the writer once in a while to prevent
     * Out Of Memory Errors, Some J2ME implementation are not able to flush the 
     * streams causing the upload to fail.
     * This method can indicate to the upload to not use the flushing mechanism.
     */ 
    public static void setCanFlushStream(boolean flush){
        canFlushStream = flush;
    }
}
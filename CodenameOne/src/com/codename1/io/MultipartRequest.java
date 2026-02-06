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

import com.codename1.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

/// A multipart post request allows a developer to submit large binary data
/// files to the server in a multipart mime post request. This is a standard method
/// for large binary file uploads to webservers and data services.
///
/// The sample code below includes both the client code using the upload capabilities as
/// well as a simple sample servlet that can accept multipart data:
///
/// ```java
/// // File: MultipartClientSample.java
/// MultipartRequest request = new MultipartRequest();
/// request.setUrl(url);
/// request.addData("myFileName", fullPathToFile, "text/plain")
/// NetworkManager.getInstance().addToQueue(request);
/// ```
///
/// ```java
/// // File: UploadServlet.java
/// @WebServlet(name = "UploadServlet", urlPatterns = {"/upload"})
/// @MultipartConfig(fileSizeThreshold = 1024 * 1024 * 100, // 10 MB
///         maxFileSize = 1024 * 1024 * 150, // 50 MB
///         maxRequestSize = 1024 * 1024 * 200)      // 100 MB
/// public class UploadServlet extends HttpServlet {
/// @Override
///     public void doPost(HttpServletRequest req, HttpServletResponse res)
///             throws ServletException, IOException {
///         Collection parts = req.getParts();
///         Part data = parts.iterator().next();
///         try(InputStream is = data.getInputStream()) {}
///             // store or do something with the input stream
///         }
///     }
/// }
/// ```
///
/// The sample code below demonstrates uploading to the
/// [filestack.com](https://www.filestack.com/features-upload) API.
///
/// ```java
/// public void pictureUpload(final Callback resultURL) {
///     String picture = Capture.capturePhoto(1024, -1);
///     if(picture!=null){
///         String filestack = "https://www.filestackapi.com/api/store/S3?key=MY_KEY&filename=myPicture.jpg";
///         MultipartRequest request = new MultipartRequest() {
///            protected void readResponse(InputStream input) throws IOException  {
///               JSONParser jp = new JSONParser();
///               Map result = jp.parseJSON(new InputStreamReader(input, "UTF-8"));
///               String url = (String)result.get("url");
///               if(url == null) {
///                  resultURL.onError(null, null, 1, result.toString());
///                  return;
///               }
///               resultURL.onSucess(url);
///            }
///         };
///         request.setUrl(filestack);
///         try {
///             request.addData("fileUpload", picture, "image/jpeg");
///             request.setFilename("fileUpload", "myPicture.jpg");
///             NetworkManager.getInstance().addToQueue(request);
///         } catch(IOException err) {
///             err.printStackTrace();
///         }
///     }
/// }
/// ```
/// @author Shai Almog
public class MultipartRequest extends ConnectionRequest {

    private static final String CRLF = "\r\n";
    private static boolean canFlushStream = true;
    /// Special flag to keep input stream files open after they are read
    private static boolean leaveInputStreamsOpen;
    private String boundary;
    private LinkedHashMap args = new LinkedHashMap();
    private Hashtable filenames = new Hashtable();
    private Hashtable filesizes = new Hashtable();
    private Hashtable mimeTypes = new Hashtable();
    private long contentLength = -1L;
    private boolean manualRedirect = true;
    private Vector ignoreEncoding = new Vector();
    /// Set to true to encode binary data as base 64
    private boolean base64Binaries = true;

    /// Initialize variables
    public MultipartRequest() {
        setPost(true);
        setWriteRequest(true);

        // Just generate some unique random value.
        boundary = Long.toString(System.currentTimeMillis(), 16);

        // Line separator required by multipart/form-data.
        setContentType("multipart/form-data; boundary=" + boundary);
    }

    /// Special flag to keep input stream files open after they are read
    ///
    /// #### Returns
    ///
    /// the leaveInputStreamsOpen
    public static boolean isLeaveInputStreamsOpen() {
        return leaveInputStreamsOpen;
    }

    /// Special flag to keep input stream files open after they are read
    ///
    /// #### Parameters
    ///
    /// - `aLeaveInputStreamsOpen`: the leaveInputStreamsOpen to set
    public static void setLeaveInputStreamsOpen(boolean aLeaveInputStreamsOpen) {
        leaveInputStreamsOpen = aLeaveInputStreamsOpen;
    }

    /// Sending large files requires flushing the writer once in a while to prevent
    /// Out Of Memory Errors, Some J2ME implementation are not able to flush the
    /// streams causing the upload to fail.
    /// This method can indicate to the upload to not use the flushing mechanism.
    public static void setCanFlushStream(boolean flush) {
        canFlushStream = flush;
    }

    /// Returns the boundary string which is normally generated based on system time
    ///
    /// #### Returns
    ///
    /// the multipart boundary string
    public String getBoundary() {
        return boundary;
    }

    /// Sets the boundary string, normally you don't need this method. Its useful to
    /// workaround server issues only. Notice that this method must be invoked before adding
    /// any elements.
    ///
    /// #### Parameters
    ///
    /// - `boundary`: the boundary string
    public void setBoundary(String boundary) {
        this.boundary = boundary;
        setContentType("multipart/form-data; boundary=" + boundary);
    }

    @Override
    protected void initConnection(Object connection) {
        contentLength = calculateContentLength();
        addRequestHeader("Content-Length", Long.toString(contentLength));
        super.initConnection(connection);
    }

    /// Adds a binary argument to the arguments
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the data
    ///
    /// - `data`: the data as bytes
    ///
    /// - `mimeType`: the mime type for the content
    public void addData(String name, byte[] data, String mimeType) {
        args.put(name, data);
        mimeTypes.put(name, mimeType);
        if (!filenames.containsKey(name)) {
            filenames.put(name, name);
        }
        filesizes.put(name, String.valueOf(data.length));
    }

    /// Adds a binary argument to the arguments
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the file data
    ///
    /// - `filePath`: the path of the file to upload
    ///
    /// - `mimeType`: the mime type for the content
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the file cannot be opened
    public void addData(String name, String filePath, String mimeType) throws IOException {
        addData(name, FileSystemStorage.getInstance().openInputStream(filePath),
                FileSystemStorage.getInstance().getLength(filePath), mimeType);
    }

    /// Adds a binary argument to the arguments, notice the input stream will be
    /// read only during submission
    ///
    /// #### Parameters
    ///
    /// - `name`: the name of the data
    ///
    /// - `data`: the data stream
    ///
    /// - `dataSize`: @param dataSize the byte size of the data stream, if the data stream is a file
    ///                 the file size can be obtained using the
    ///                 FileSystemStorage.getInstance().getLength(file) method
    ///
    /// - `mimeType`: the mime type for the content
    public void addData(String name, InputStream data, long dataSize, String mimeType) {
        args.put(name, data);
        if (!filenames.containsKey(name)) {
            filenames.put(name, name);
        }
        filesizes.put(name, String.valueOf(dataSize));
        mimeTypes.put(name, mimeType);
    }

    /// Sets the filename for the given argument
    ///
    /// #### Parameters
    ///
    /// - `arg`: the argument name
    ///
    /// - `filename`: the file name
    public void setFilename(String arg, String filename) {
        filenames.put(arg, filename);
    }

    /// {@inheritDoc}
    @Override
    public void addArgumentNoEncoding(String key, String value) {
        args.put(key, value);
        if (!filenames.containsKey(key)) {
            filenames.put(key, key);
        }
        ignoreEncoding.addElement(key);
    }

    /// {@inheritDoc}
    @Override
    public void addArgumentNoEncoding(String key, String[] value) {
        addArgument(key, value);
        ignoreEncoding.add(key);
    }

    /// {@inheritDoc}
    @Override
    public void addArgumentNoEncodingArray(String key, String... value) {
        addArgumentNoEncoding(key, (String[]) value);
    }

    /// {@inheritDoc}
    @Override
    public void addArgument(String name, String[] value) {
        args.put(name, value);
    }

    /// {@inheritDoc}
    @Override
    public void addArgument(String name, String value) {
        args.put(name, value);
        if (!filenames.containsKey(name)) {
            filenames.put(name, name);
        }
    }

    protected long calculateContentLength() {
        long length = 0L;
        Iterator entries = args.entrySet().iterator();

        long dLength = "Content-Disposition: form-data; name=\"\"; filename=\"\"".length() + 2; // 2 = CRLF
        long ctLength = "Content-Type: ".length() + 2; // 2 = CRLF
        long cteLength = "Content-Transfer-Encoding: binary".length() + 4; // 4 = 2 * CRLF
        long bLength = boundary.length() + 4; // -- + boundary + CRLF
        long baseBinaryLength = dLength + ctLength + cteLength + bLength + 2; // 2 = CRLF at end of part
        dLength = "Content-Disposition: form-data; name=\"\"".length() + 2;  // 2 = CRLF
        ctLength = "Content-Type: text/plain; charset=UTF-8".length() + 4; // 4 = 2 * CRLF
        long baseTextLength = dLength + ctLength + bLength + 2;  // 2 = CRLF at end of part

        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                length += baseTextLength;
                length += key.length();
                if (ignoreEncoding.contains(key)) {
                    try {
                        length += ((String) value).getBytes("UTF-8").length;
                    } catch (UnsupportedEncodingException ex) {
                        length += StringUtil.getBytes((String) value).length;
                    }
                } else {
                    if (base64Binaries) {
                        length += Util.encodeBody((String) value).length();
                    } else {
                        length += ((String) value).length();
                    }
                }
            } else {
                if (value instanceof String[]) {
                    for (String s : (String[]) value) {
                        length += baseTextLength;
                        length += key.length();
                        if (ignoreEncoding.contains(key)) {
                            try {
                                length += s.getBytes("UTF-8").length;
                            } catch (UnsupportedEncodingException ex) {
                                length += StringUtil.getBytes(s).length;
                            }
                        } else {
                            if (base64Binaries) {
                                length += Util.encodeBody(s).length();
                            } else {
                                length += s.length();
                            }
                        }
                    }
                } else {
                    length += baseBinaryLength;
                    length += key.length();
                    try {
                        length += ((String) filenames.get(key)).getBytes("UTF-8").length;
                    } catch (UnsupportedEncodingException ex) {
                        length += StringUtil.getBytes((String) filenames.get(key)).length;
                    }
                    length += ((String) mimeTypes.get(key)).length();
                    length += Long.parseLong((String) filesizes.get(key));
                }
            }
        }
        length += bLength + 2; // same as part boundaries, suffixed with: --
        return length;
    }

    /// {@inheritDoc}
    @Override
    protected void buildRequestBody(OutputStream os) throws IOException {
        Writer writer = null; //NOPMD CloseResource
        try {
            writer = new OutputStreamWriter(os, "UTF-8");
            Iterator entries = args.entrySet().iterator();
            while (entries.hasNext()) {
                if (shouldStop()) {
                    break;
                }
                Map.Entry entry = (Map.Entry) entries.next();
                String key = (String) entry.getKey();
                Object value = entry.getValue();

                writer.write("--");
                writer.write(boundary);
                writer.write(CRLF);
                if (value instanceof String) {
                    writer.write("Content-Disposition: form-data; name=\"");
                    writer.write(key);
                    writer.write("\"");
                    writer.write(CRLF);
                    writer.write("Content-Type: text/plain; charset=UTF-8");
                    writer.write(CRLF);
                    writer.write(CRLF);
                    if (canFlushStream) {
                        writer.flush();
                    }
                    if (ignoreEncoding.contains(key)) {
                        writer.write((String) value);
                    } else {
                        if (base64Binaries) {
                            writer.write(Util.encodeBody((String) value));
                        } else {
                            writer.write((String) value);
                        }
                    }
                    //writer.write(CRLF);
                    if (canFlushStream) {
                        writer.flush();
                    }
                } else {
                    if (value instanceof String[]) {
                        boolean first = true;
                        for (String s : (String[]) value) {
                            if (!first) {
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
                            if (canFlushStream) {
                                writer.flush();
                            }
                            if (ignoreEncoding.contains(key)) {
                                writer.write(s);
                            } else {
                                if (base64Binaries) {
                                    writer.write(Util.encodeBody(s));
                                } else {
                                    writer.write(s);
                                }
                            }
                            //writer.write(CRLF);
                            if (canFlushStream) {
                                writer.flush();
                            }
                        }
                    } else {
                        writer.write("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + filenames.get(key) + "\"");
                        writer.write(CRLF);
                        writer.write("Content-Type: ");
                        writer.write((String) mimeTypes.get(key));
                        writer.write(CRLF);
                        writer.write("Content-Transfer-Encoding: binary");
                        writer.write(CRLF);
                        writer.write(CRLF);
                        if (canFlushStream) {
                            writer.flush();
                        }
                        InputStream i = null; //NOPMD CloseResource
                        try {
                            if (value instanceof InputStream) {
                                i = (InputStream) value;
                            } else {
                                i = new ByteArrayInputStream((byte[]) value);
                            }
                            byte[] buffer = new byte[8192];
                            int s = i.read(buffer);
                            while (s > -1) {
                                if (shouldStop()) {
                                    break;
                                }
                                os.write(buffer, 0, s);
                                if (canFlushStream) {
                                    writer.flush();
                                }
                                s = i.read(buffer);
                            }
                        } finally {
                            if (value instanceof InputStream) {
                                if (!leaveInputStreamsOpen) {
                                    Util.cleanup(i);
                                }
                            } else {
                                Util.cleanup(i);
                            }
                        }
                        //args.remove(key);
                        value = null;
                        if (canFlushStream) {
                            writer.flush();
                        }
                    }
                }
                writer.write(CRLF);
                if (canFlushStream) {
                    writer.flush();
                }
            }

            writer.write("--" + boundary + "--");
            writer.write(CRLF);
        } finally {
            Util.cleanup(writer);
        }
    }

    /* (non-Javadoc)
     * @see com.codename1.io.ConnectionRequest#getContentLength()
     */
    @Override
    public int getContentLength() {
        return (int) contentLength;
    }

    /* (non-Javadoc)
     * @see com.codename1.io.ConnectionRequest#onRedirect(java.lang.String)
     */
    @Override
    public boolean onRedirect(String url) {
        return manualRedirect;
    }

    /// By default redirect responses (302 etc.) are handled manually in multipart requests
    ///
    /// #### Returns
    ///
    /// the autoRedirect
    public boolean isManualRedirect() {
        return manualRedirect;
    }

    /// By default redirect responses (302 etc.) are handled manually in multipart requests, set this
    /// to false to handle the redirect. Notice that a redirect converts a post to a get.
    ///
    /// #### Parameters
    ///
    /// - `autoRedirect`: the autoRedirect to set
    public void setManualRedirect(boolean autoRedirect) {
        this.manualRedirect = autoRedirect;
    }

    /// Set to true to encode binary data as base 64
    ///
    /// #### Returns
    ///
    /// the base64Binaries
    public boolean isBase64Binaries() {
        return base64Binaries;
    }

    /// Set to true to encode binary data as base 64
    ///
    /// #### Parameters
    ///
    /// - `base64Binaries`: the base64Binaries to set
    public void setBase64Binaries(boolean base64Binaries) {
        this.base64Binaries = base64Binaries;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MultipartRequest)) {
            return false;
        }
        MultipartRequest that = (MultipartRequest) o;
        return super.equals(o) &&
                contentLength == that.contentLength &&
                manualRedirect == that.manualRedirect &&
                base64Binaries == that.base64Binaries &&
                (boundary == null ? that.boundary == null : boundary.equals(that.boundary)) &&
                (args == null ? that.args == null : args.equals(that.args)) &&
                (filenames == null ? that.filenames == null : filenames.equals(that.filenames)) &&
                (filesizes == null ? that.filesizes == null : filesizes.equals(that.filesizes)) &&
                (mimeTypes == null ? that.mimeTypes == null : mimeTypes.equals(that.mimeTypes)) &&
                (ignoreEncoding == null ? that.ignoreEncoding == null : ignoreEncoding.equals(that.ignoreEncoding));
    }

    /// {@inheritDoc}
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (boundary != null ? boundary.hashCode() : 0);
        result = 31 * result + (args != null ? args.hashCode() : 0);
        return result;
    }
}

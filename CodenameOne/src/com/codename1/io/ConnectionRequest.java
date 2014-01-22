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

package com.codename1.io;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class represents a connection object in the form of a request response
 * typically common for HTTP/HTTPS connections. Elements of this type are
 * placed in a priority queue based 
 *
 * @author Shai Almog
 */
public class ConnectionRequest implements IOProgressListener {
    /**
     * A critical priority request will "push" through the queue to the highest point
     * regardless of anything else and ignoring anything that is not in itself of
     * critical priority.
     * A critical priority will stop any none critical connection in progress
     */
    public static final byte PRIORITY_CRITICAL = (byte)100;

    /**
     * A high priority request is the second highest level, it will act exactly like
     * a critical priority with one difference. It doesn't block another incoming high priority
     * request. E.g. if a high priority request
     */
    public static final byte PRIORITY_HIGH = (byte)80;

    /**
     * Normal priority executes as usual on the queue
     */
    public static final byte PRIORITY_NORMAL = (byte)50;

    /**
     * Low priority requests are mostly background tasks that should still be accomplished though
     */
    public static final byte PRIORITY_LOW = (byte)30;

    /**
     * Redundant elements can be discarded from the queue when paused
     */
    public static final byte PRIORITY_REDUNDANT = (byte)0;

    /**
     * Workaround for https://bugs.php.net/bug.php?id=65633 allowing developers to
     * customize the name of the cookie header to Cookie
     * @return the cookieHeader
     */
    public static String getCookieHeader() {
        return cookieHeader;
    }

    /**
     * Workaround for https://bugs.php.net/bug.php?id=65633 allowing developers to
     * customize the name of the cookie header to Cookie
     * @param aCookieHeader the cookieHeader to set
     */
    public static void setCookieHeader(String aCookieHeader) {
        cookieHeader = aCookieHeader;
    }

    private EventDispatcher actionListeners;

    /**
     * @return the defaultFollowRedirects
     */
    public static boolean isDefaultFollowRedirects() {
        return defaultFollowRedirects;
    }

    /**
     * @param aDefaultFollowRedirects the defaultFollowRedirects to set
     */
    public static void setDefaultFollowRedirects(boolean aDefaultFollowRedirects) {
        defaultFollowRedirects = aDefaultFollowRedirects;
    }

    private byte priority = PRIORITY_NORMAL;
    private long timeSinceLastUpdate;
    private Hashtable requestArguments;

    private boolean post = true;
    private String contentType = "application/x-www-form-urlencoded; charset=UTF-8";
    private static String defaultUserAgent = null;
    private String userAgent = getDefaultUserAgent();
    private String url;
    private boolean writeRequest;
    private boolean readRequest = true;
    private boolean paused;
    private boolean killed = false;
    private static boolean defaultFollowRedirects = true;
    private boolean followRedirects = defaultFollowRedirects;
    private int timeout = -1;
    private InputStream input;
    private OutputStream output;
    private int progress = NetworkEvent.PROGRESS_TYPE_OUTPUT;
    private int contentLength = -1;
    private boolean duplicateSupported;
    private EventDispatcher responseCodeListeners;
    private Hashtable userHeaders;
    private Dialog showOnInit;
    private Dialog disposeOnCompletion;
    private byte[] data;
    private int responseCode;
    private String httpMethod;
    private int silentRetryCount = 0;
    private boolean failSilently;
    boolean retrying;
    private boolean readResponseForErrors;
    private String responseContentType;
    private boolean redirecting;

    /**
     * Workaround for https://bugs.php.net/bug.php?id=65633 allowing developers to
     * customize the name of the cookie header to Cookie
     */
    private static String cookieHeader = "cookie";
    
    public ConnectionRequest() {
        if(NetworkManager.getInstance().isAPSupported()) {
            silentRetryCount = 1;
        }
    }
    
    /**
     * This method will return a valid value for only some of the responses and only after the response was processed
     * @return null or the actual data returned
     */
    public byte[] getResponseData() {
        return data;
    }
    
    
    /**
     * Sets the http method for the request
     * @param httpMethod the http method string
     */
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    } 
    
    /**
     * Returns the http method 
     * @return the http method of the request
     */
    public String getHttpMethod() {
        return httpMethod;
    }
    
    /**
     * Adds the given header to the request that will be sent
     * 
     * @param key the header key
     * @param value the header value
     */
    public void addRequestHeader(String key, String value) {
        if(userHeaders == null) {
            userHeaders = new Hashtable();
        }
        if(key.equalsIgnoreCase("content-type")) {
            setContentType(value);
        } else {
            userHeaders.put(key, value);
        }
    }

    /**
     * Adds the given header to the request that will be sent unless the header
     * is already set to something else
     *
     * @param key the header key
     * @param value the header value
     */
    void addRequestHeaderDontRepleace(String key, String value) {
        if(userHeaders == null) {
            userHeaders = new Hashtable();
        }
        if(!userHeaders.containsKey(key)) {
            userHeaders.put(key, value);
        }
    }

    void prepare() {
        timeSinceLastUpdate = System.currentTimeMillis();
    }

    /**
     * Invoked to initialize HTTP headers, cookies etc. 
     * 
     * @param connection the connection object
     */
    protected void initConnection(Object connection) {
        timeSinceLastUpdate = System.currentTimeMillis();
        CodenameOneImplementation impl = Util.getImplementation();
        impl.setPostRequest(connection, isPost());

        if(getUserAgent() != null) {
            impl.setHeader(connection, "User-Agent", getUserAgent());
        }

        if(getContentType() != null) {
            impl.setHeader(connection, "Content-Type", getContentType());
        }

        if(userHeaders != null) {
            Enumeration e = userHeaders.keys();
            while(e.hasMoreElements()) {
                String k = (String)e.nextElement();
                String value = (String)userHeaders.get(k);
                impl.setHeader(connection, k, value);
            }
        }
    }

    /**
     * Performs the actual network request on behalf of the network manager
     */
    void performOperation() throws IOException {
        if(shouldStop()) {
            return;
        }
        CodenameOneImplementation impl = Util.getImplementation();
        Object connection = null;
        input = null;
        output = null;
        redirecting = false;
        try {
            String actualUrl = createRequestURL();
            if(timeout > 0) {
                connection = impl.connect(actualUrl, isReadRequest(), isPost() || isWriteRequest(), timeout);
            } else {
                connection = impl.connect(actualUrl, isReadRequest(), isPost() || isWriteRequest());
            }
            if(shouldStop()) {
                return;
            }
            initConnection(connection);
            if(httpMethod != null) {
                impl.setHttpMethod(connection, httpMethod);
            }
            Vector v = impl.getCookiesForURL(actualUrl);
            if(v != null) {
                int c = v.size();
                if(c > 0) {
                    StringBuilder cookieStr = new StringBuilder();
                    Cookie first = (Cookie)v.elementAt(0);
                    cookieStr.append(first.getName());
                    cookieStr.append("=");
                    cookieStr.append(first.getValue());
                    for(int iter = 1 ; iter < c ; iter++) {
                        Cookie current = (Cookie)v.elementAt(iter);
                        cookieStr.append(";");
                        cookieStr.append(current.getName());
                        cookieStr.append("=");
                        cookieStr.append(current.getValue());
                    }
                    impl.setHeader(connection, cookieHeader, initCookieHeader(cookieStr.toString()));
                } else {
                    String s = initCookieHeader(null);
                    if(s != null) {
                        impl.setHeader(connection, cookieHeader, s);
                    }
                }
            } else {
                String s = initCookieHeader(null);
                if(s != null) {
                    impl.setHeader(connection, cookieHeader, s);
                }
            }
            if(isWriteRequest()) {
            	progress = NetworkEvent.PROGRESS_TYPE_OUTPUT;
                output = impl.openOutputStream(connection);
                if(shouldStop()) {
                    return;
                }
                if(NetworkManager.getInstance().hasProgressListeners() && output instanceof BufferedOutputStream) {
                    ((BufferedOutputStream)output).setProgressListener(this);
                }
                buildRequestBody(output);
                if(shouldStop()) {
                    return;
                }
                if(output instanceof BufferedOutputStream) {
                    ((BufferedOutputStream)output).flushBuffer();
                    if(shouldStop()) {
                        return;
                    }
                }
            }
            timeSinceLastUpdate = System.currentTimeMillis();
            responseCode = impl.getResponseCode(connection);
            
            String[] cookies = impl.getHeaderFields("Set-Cookie", connection);
            if(cookies != null && cookies.length > 0){
                Vector cook = new Vector();
                for(int iter = 0 ; iter < cookies.length ; iter++) {
                    Cookie coo = parseCookieHeader(cookies[iter]);
                    if(coo != null) {
                        cook.addElement(coo);
                    }
                }
                Cookie [] arr = new Cookie[cook.size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = (Cookie) cook.elementAt(i);
                }
                impl.addCookie(arr);
            }
            
            if(responseCode - 200 < 0 || responseCode - 200 > 100) {
                readErrorCodeHeaders(connection);
                // redirect to new location
                if(followRedirects && (responseCode == 301 || responseCode == 302
                        || responseCode == 303)) {
                    String uri = impl.getHeaderField("location", connection);

                    if(!(uri.startsWith("http://") || uri.startsWith("https://"))) {
                        // relative URI's in the location header are illegal but some sites mistakenly use them
                        url = Util.relativeToAbsolute(url, uri);
                    } else {
                        url = uri;
                    }
                    if((responseCode == 302 || responseCode == 303)){
                        if(this.post && shouldConvertPostToGetOnRedirect()) {
                            this.post = false;
                            setWriteRequest(false);
                        }
                    }

                    impl.cleanup(output);
                    impl.cleanup(connection);
                    connection = null;
                    output = null;
                    if(!onRedirect(url)){
                        redirecting = true;
                        retry();
                    }
                    return;
                }

                handleErrorResponseCode(responseCode, impl.getResponseMessage(connection));
                if(!isReadResponseForErrors()) {
                    return;
                }
            }
            responseContentType = getHeader(connection, "Content-Type");
            readHeaders(connection);
            contentLength = impl.getContentLength(connection);
            timeSinceLastUpdate = System.currentTimeMillis();
            
            progress = NetworkEvent.PROGRESS_TYPE_INPUT;
            if(isReadRequest()) {
                input = impl.openInputStream(connection);
                if(shouldStop()) {
                    return;
                }
                if(input instanceof BufferedInputStream) {
                    if(NetworkManager.getInstance().hasProgressListeners()) {
                        ((BufferedInputStream)input).setProgressListener(this);
                    }
                    ((BufferedInputStream)input).setYield(getYield());
                }
                readResponse(input);
                if(shouldAutoCloseResponse()) {
                    input.close();
                }
                input = null;
            }
        } finally {
            // always cleanup connections/streams even in case of an exception
            impl.cleanup(output);
            impl.cleanup(input);
            impl.cleanup(connection);
            timeSinceLastUpdate = -1;
            input = null;
            output = null;
            connection = null;
        }
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                postResponse();
            }
        });
    }
    
    /**
     * Allows subclasses to inject cookies into the request
     * @param cookie the cookie that the implementation is about to send or null for no cookie
     * @return new cookie or the value of cookie
     */
    protected String initCookieHeader(String cookie) {
        return cookie;
    }

    /**
     * Returns the response code for this request, this is only relevant after the request completed and
     * might contain a temporary (e.g. redirect) code while the request is in progress
     * @return the response code
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Returns the response code for this request, this is only relevant after the request completed and
     * might contain a temporary (e.g. redirect) code while the request is in progress
     * @return the response code
     * @deprecated misspelled method name please use getResponseCode
     */
    public int getResposeCode() {
        return responseCode;
    }
    
    /**
     * This mimics the behavior of browsers that convert post operations to get operations when redirecting a
     * request.
     * @return defaults to true, this case be modified by subclasses
     */
    protected boolean shouldConvertPostToGetOnRedirect() {
        return true;
    }

    /**
     * Allows reading the headers from the connection by calling the getHeader() method. 
     * @param connection used when invoking getHeader
     * @throws java.io.IOException thrown on failure
     */
    protected void readHeaders(Object connection) throws IOException {
    }

    /**
     * Allows reading the headers from the connection by calling the getHeader() method when a response that isn't 200 OK is sent. 
     * @param connection used when invoking getHeader
     * @throws java.io.IOException thrown on failure
     */
    protected void readErrorCodeHeaders(Object connection) throws IOException {
    }

    /**
     * Returns the HTTP header field for the given connection, this method is only guaranteed to work
     * when invoked from the readHeaders method.
     *
     * @param connection the connection to the network
     * @param header the name of the header
     * @return the value of the header
     * @throws java.io.IOException thrown on failure
     */
    protected String getHeader(Object connection, String header) throws IOException {
        return Util.getImplementation().getHeaderField(header, connection);
    }

    /**
     * Returns the HTTP header field for the given connection, this method is only guaranteed to work
     * when invoked from the readHeaders method. Unlike the getHeader method this version works when
     * the same header name is declared multiple times.
     *
     * @param connection the connection to the network
     * @param header the name of the header
     * @return the value of the header
     * @throws java.io.IOException thrown on failure
     */
    protected String[] getHeaders(Object connection, String header) throws IOException {
        return Util.getImplementation().getHeaderFields(header, connection);
    }

    /**
     * Returns the HTTP header field names for the given connection, this method is only guaranteed to work
     * when invoked from the readHeaders method.
     *
     * @param connection the connection to the network
     * @return the names of the headers
     * @throws java.io.IOException thrown on failure
     */
    protected String[] getHeaderFieldNames(Object connection) throws IOException {
        return Util.getImplementation().getHeaderFieldNames(connection);
    }
    
    /**
     * Returns the amount of time to yield for other processes, this is an implicit 
     * method that automatically generates values for lower priority connections
     * @return yield duration or -1 for no yield
     */
    protected int getYield() {
        if(priority > PRIORITY_NORMAL) {
            return -1;
        }
        if(priority == PRIORITY_NORMAL) {
            return 20;
        }
        return 40;
    }

    /**
     * Indicates whether the response stream should be closed automatically by
     * the framework (defaults to true), this might cause an issue if the stream
     * needs to be passed to a separate thread for reading.
     * 
     * @return true to close the response stream automatically.
     */
    protected boolean shouldAutoCloseResponse() {
        return true;
    }

    /**
     * Parses a raw cookie header and returns a cookie object to send back at the server
     * 
     * @param h raw cookie header
     * @return the cookie object
     */
    private Cookie parseCookieHeader(String h) {
        String lowerH = h.toLowerCase();
        
        Cookie c = new Cookie();
        int edge = h.indexOf(';');
        int equals = h.indexOf('=');
        if(equals < 0) {
            return null;
        }
        c.setName(h.substring(0, equals));
        if(edge < 0) {
            c.setValue(h.substring(equals + 1));
            c.setDomain(Util.getImplementation().getURLDomain(url));
            return c;
        }else{
            c.setValue(h.substring(equals + 1, edge));
        }
        
        int index = lowerH.indexOf("domain=");
        if (index > -1) {
            String domain = h.substring(index + 7);
            index = domain.indexOf(';');
            if (index!=-1) {
                domain = domain.substring(0, index);
            }

            if (url.indexOf(domain) < 0) { //if (!hc.getHost().endsWith(domain)) {
                System.out.println("Warning: Cookie tried to set to another domain");
                c.setDomain(Util.getImplementation().getURLDomain(url));
            } else {
                c.setDomain(domain);
            }
        } else {
            c.setDomain(Util.getImplementation().getURLDomain(url));
        }
        
        index = lowerH.indexOf("path=");
        if (index > -1) {
            String path = h.substring(index + 5);
            index = path.indexOf(';');
            if (index > -1) {
                path = path.substring(0, index);
            }
            
            if (Util.getImplementation().getURLPath(url).indexOf(path) != 0) { //if (!hc.getHost().endsWith(domain)) {
                System.out.println("Warning: Cookie tried to set to another path");
                c.setPath(path);
            } else {
                // Don't set the path explicitly
            }
        } else {
            // Don't set the path explicitly
        }
        
        // Check for secure and httponly.
        // SJH NOTE:  It would be better to rewrite this whole method to 
        // split it up this way, rather than do the domain and path 
        // separately.. but this is a patch job to just get secure
        // path, and httponly working... don't want to break any existing
        // code for now.
        Vector parts = StringUtil.tokenizeString(lowerH, ';');
        for ( int i=0; i<parts.size(); i++){
            String part = (String) parts.elementAt(i);
            part = part.trim();
            if ( part.indexOf("secure") == 0 ){
                c.setSecure(true);
            } else if ( part.indexOf("httponly") == 0 ){
                c.setHttpOnly(true);
            }
        }
        
        

        return c;
    }

    /**
     * Handles IOException thrown when performing a network operation
     * 
     * @param err the exception thrown
     */
    protected void handleIOException(IOException err) {
        handleException(err);
    }

    /**
     * Handles an exception thrown when performing a network operation
     *
     * @param err the exception thrown
     */
    protected void handleRuntimeException(RuntimeException err) {
        handleException(err);
    }

    /**
     * Handles an exception thrown when performing a network operation, the default
     * implementation shows a retry dialog.
     *
     * @param err the exception thrown
     */
    protected void handleException(Exception err) {
        if(killed || failSilently) {
            return;
        }
        err.printStackTrace();
        if(silentRetryCount > 0) {
            silentRetryCount--;
            NetworkManager.getInstance().resetAPN();
            retry();
            return;
        }
        if(Display.isInitialized() &&
                Dialog.show("Exception", err.toString() + ": for URL " + url + "\n" + err.getMessage(), "Retry", "Cancel")) {
            retry();
        }
    }

    /**
     * Handles a server response code that is not 200 and not a redirect (unless redirect handling is disabled)
     *
     * @param code the response code from the server
     * @param message the response message from the server
     */
    protected void handleErrorResponseCode(int code, String message) {
        if(failSilently) {
            return;
        }
        if(responseCodeListeners != null) {
            NetworkEvent n = new NetworkEvent(this, code, message);
            responseCodeListeners.fireActionEvent(n);
            return;
        }
        if(Display.isInitialized() &&
                Dialog.show("Error", code + ": " + message, "Retry", "Cancel")) {
            retry();
        }
    }

    /**
     * Retry the current operation in case of an exception
     */
    public void retry() {
        retrying = true;
        NetworkManager.getInstance().addToQueue(this, true);
    }

    /**
     * This is a callback method that been called when there is a redirect.
     *
     * @param url the url to be redirected
     * @return true if the implementation would like to handle this by itself
     */
    public boolean onRedirect(String url){
        return false;
    }

    /**
     * Callback for the server response with the input stream from the server.
     * This method is invoked on the network thread
     * 
     * @param input the input stream containing the response
     * @throws IOException when a read input occurs
     */
    protected void readResponse(InputStream input) throws IOException  {
        data = Util.readInputStream(input);
        if(hasResponseListeners()) {
            fireResponseListener(new NetworkEvent(this, data));
        }
    }

    /**
     * A callback method that's invoked on the EDT after the readResponse() method has finished,
     * this is the place where developers should change their Codename One user interface to
     * avoid race conditions that might be triggered by modifications within readResponse.
     * Notice this method is only invoked on a successful response and will not be invoked in case
     * of a failure.
     */
    protected void postResponse() {
    }
    
    /**
     * Creates the request URL mostly for a get request
     * 
     * @return the string of a request
     */
    protected String createRequestURL() {
        if(!post && requestArguments != null) {
            StringBuilder b = new StringBuilder(url);
            Enumeration e = requestArguments.keys();
            if(e.hasMoreElements()) {
                b.append("?");
            }
            while(e.hasMoreElements()) {
                String key = (String)e.nextElement();
                Object requestVal = requestArguments.get(key);
                if(requestVal instanceof String[]) {
                    String[] val = (String[])requestVal;
                    for(int iter = 0 ; iter < val.length - 1; iter++) {
                        b.append(key);
                        b.append("=");
                        b.append(val[iter]);
                        b.append("&");
                    }
                    b.append(key);
                    b.append("=");
                    b.append(val[val.length - 1]);
                    if(e.hasMoreElements()) {
                        b.append("&");
                    }
                    continue;
                }
                String value = (String)requestVal;
                b.append(key);
                b.append("=");
                b.append(value);
                if(e.hasMoreElements()) {
                    b.append("&");
                }
            }
            return b.toString();
        }
        return url;
    }

    /**
     * Invoked when send body is true, by default sends the request arguments based
     * on "POST" conventions
     *
     * @param os output stream of the body
     */
    protected void buildRequestBody(OutputStream os) throws IOException {
        if(post && requestArguments != null) {
            StringBuilder val = new StringBuilder();
            Enumeration e = requestArguments.keys();
            while(e.hasMoreElements()) {
                String key = (String)e.nextElement();
                Object requestVal = requestArguments.get(key);
                if(requestVal instanceof String[]) {
                    String[] valArray = (String[])requestVal;
                    for(int iter = 0 ; iter < valArray.length - 1; iter++) {
                        val.append(key);
                        val.append("=");
                        val.append(valArray[iter]);
                        val.append("&");
                    }
                    val.append(key);
                    val.append("=");
                    val.append(valArray[valArray.length - 1]);
                    if(e.hasMoreElements()) {
                        val.append("&");
                    }
                    continue;
                }
                String value = (String)requestVal;
                val.append(key);
                val.append("=");
                val.append(value);
                if(e.hasMoreElements()) {
                    val.append("&");
                }
            }
            if(Util.getImplementation().shouldWriteUTFAsGetBytes()) {
                os.write(val.toString().getBytes("UTF-8"));
            } else {
                OutputStreamWriter w = new OutputStreamWriter(os, "UTF-8");
                w.write(val.toString());
            }
        }
    }
    
    /**
     * Kills this request if possible
     */
    public void kill() {
        killed = true;
        //if the connection is in the midle of a reading, stop it to release the 
        //resources
        if(input != null && input instanceof BufferedInputStream) {
            ((BufferedInputStream)input).stop();
        }
    }

    /**
     * Returns true if the request is paused or killed, developers should call this
     * method periodically to test whether they should quit the current IO operation immediately
     *
     * @return true if the request is paused or killed
     */
    protected boolean shouldStop() {
        return isPaused() || isKilled();
    }

    /**
     * Return true from this method if this connection can be paused and resumed later on.
     * A pausable network operation receives a "pause" invocation and is expected to stop
     * network operations as soon as possible. It will later on receive a resume() call and
     * optionally start downloading again.
     *
     * @return false by default.
     */
    protected boolean isPausable() {
        return false;
    }

    /**
     * Invoked to pause this opeation, this method will only be invoked if isPausable() returns true
     * (its false by default). After this method is invoked current network operations should
     * be stoped as soon as possible for this class.
     *
     * @return This method can return false to indicate that there is no need to resume this
     * method since the operation has already been completed or made redundant
     */
    public boolean pause() {
        paused = true;
        return true;
    }

    /**
     * Called when a previously paused operation now has the networking time to resume.
     * Assuming this method returns true, the network request will be resent to the server
     * and the operation can resume.
     *
     * @return This method can return false to indicate that there is no need to resume this
     * method since the operation has already been completed or made redundant
     */
    public boolean resume() {
        paused = false;
        return true;
    }

    /**
     * Returns true for a post operation and false for a get operation
     *
     * @return the post
     */
    public boolean isPost() {
        return post;
    }

    /**
     * Set to true for a post operation and false for a get operation, this will implicitly 
     * set the method to post/get respectively (which you can change back by setting the method).
     * The main importance of this method is how arguments are added to the request (within the 
     * body or in the URL) and so it is important to invoke this method before any argument was 
     * added.
     *
     * @throws IllegalStateException if invoked after an addArgument call
     */
    public void setPost(boolean post) {
        if(this.post != post && requestArguments != null && requestArguments.size() > 0) {
            throw new IllegalStateException("Request method (post/get) can't be modified one arguments have been assigned to the request");
        }
        this.post = post;
        if(this.post) {
            setWriteRequest(true);
        }
    }

    /**
     * Add an argument to the request response
     *
     * @param key the key of the argument
     * @param value the value for the argument
     */
    private void addArg(String key, Object value) {
        if(requestArguments == null) {
            requestArguments = new Hashtable();
        }
        if(value == null || key == null){
            return;
        }
        requestArguments.put(key, value);
    }

    /**
     * Add an argument to the request response
     *
     * @param key the key of the argument
     * @param value the value for the argument
     * @deprecated use the version that accepts a string instead
     */
    public void addArgument(String key, byte[] value) {
        key = key.intern();
        if(post) {
            addArg(Util.encodeBody(key), Util.encodeBody(value));
        } else {
            addArg(Util.encodeUrl(key), Util.encodeUrl(value));
        }
    }

    /**
     * Removes the given argument from the request 
     * 
     * @param key the key of the argument no longer used
     */
    public void removeArgument(String key) {
        if(requestArguments != null) {
            requestArguments.remove(key);
        }
    }

    /**
     * Removes all arguments
     */
    public void removeAllArguments() {
        requestArguments = null;
    }
    
    /**
     * Add an argument to the request response without encoding it, this is useful for
     * arguments which are already encoded
     *
     * @param key the key of the argument
     * @param value the value for the argument
     */
    public void addArgumentNoEncoding(String key, String value) {
        addArg(key, value);
    }

    /**
     * Add an argument to the request response
     *
     * @param key the key of the argument
     * @param value the value for the argument
     */
    public void addArgument(String key, String value) {
        if(post) {
            addArg(Util.encodeBody(key), Util.encodeBody(value));
        } else {
            addArg(Util.encodeUrl(key), Util.encodeUrl(value));
        }
    }

    /**
     * Add an argument to the request response as an array of elements, this will
     * trigger multiple request entries with the same key
     *
     * @param key the key of the argument
     * @param value the value for the argument
     */
    public void addArgument(String key, String[] value) {
        // copying the array to prevent mutation
        String[] v = new String[value.length];
        if(post) {
            for(int iter = 0 ; iter < value.length ; iter++) {
                v[iter] = Util.encodeBody(value[iter]);
            }
            addArg(Util.encodeBody(key), v);
        } else {
            for(int iter = 0 ; iter < value.length ; iter++) {
                v[iter] = Util.encodeUrl(value[iter]);
            }
            addArg(Util.encodeUrl(key), v);
        }
    }

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return the writeRequest
     */
    public boolean isWriteRequest() {
        return writeRequest;
    }

    /**
     * @param writeRequest the writeRequest to set
     */
    public void setWriteRequest(boolean writeRequest) {
        this.writeRequest = writeRequest;
    }

    /**
     * @return the readRequest
     */
    public boolean isReadRequest() {
        return readRequest;
    }

    /**
     * @param readRequest the readRequest to set
     */
    public void setReadRequest(boolean readRequest) {
        this.readRequest = readRequest;
    }

    /**
     * @return the paused
     */
    protected boolean isPaused() {
        return paused;
    }

    /**
     * @param paused the paused to set
     */
    protected void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * @return the killed
     */
    protected boolean isKilled() {
        return killed;
    }

    /**
     * @param killed the killed to set
     */
    protected void setKilled(boolean killed) {
        this.killed = killed;
    }

    /**
     * The priority of this connection based on the constants in this class
     *
     * @return the priority
     */
    public byte getPriority() {
        return priority;
    }

    /**
     * The priority of this connection based on the constants in this class
     * 
     * @param priority the priority to set
     */
    public void setPriority(byte priority) {
        this.priority = priority;
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * @param userAgent the userAgent to set
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * @return the defaultUserAgent
     */
    public static String getDefaultUserAgent() {
        return defaultUserAgent;
    }

    /**
     * @param aDefaultUserAgent the defaultUserAgent to set
     */
    public static void setDefaultUserAgent(String aDefaultUserAgent) {
        defaultUserAgent = aDefaultUserAgent;
    }

    /**
     * @return the followRedirects
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * @param followRedirects the followRedirects to set
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Indicates the timeout for this connection request 
     *
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Indicates the timeout for this connection request 
     * 
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * This method prevents a manual timeout from occuring when invoked at a frequency faster
     * than the timeout.
     */
    void updateActivity() {
        timeSinceLastUpdate = System.currentTimeMillis();
    }

    /**
     * Returns the time since the last activity update
     */
    int getTimeSinceLastActivity() {
        if(input != null && input instanceof BufferedInputStream) {
            long t = ((BufferedInputStream)input).getLastActivityTime();
            if(t > timeSinceLastUpdate) {
                timeSinceLastUpdate = t;
            }
        }
        if(output != null && output instanceof BufferedOutputStream) {
            long t = ((BufferedOutputStream)output).getLastActivityTime();
            if(t > timeSinceLastUpdate) {
                timeSinceLastUpdate = t;
            }
        }
        return (int)(System.currentTimeMillis() - timeSinceLastUpdate);
    }

    /**
     * Returns the content length header value
     *
     * @return the content length
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * @inheritDoc
     */
    public void ioStreamUpdate(Object source, int bytes) {
        NetworkManager.getInstance().fireProgressEvent(this, progress, getContentLength(), bytes);
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        if(url.indexOf(' ') > -1) {
            url = StringUtil.replaceAll(url, " ", "%20");
        }
        url = url.intern();
        this.url = url;
    }

    /**
     * Adds a listener that would be notified on the CodenameOne thread of a response from the server.
     * This event is specific to the connection request type and its firing will change based on
     * how the connection request is read/processed
     *
     * @param a listener
     */
    public void addResponseListener(ActionListener a) {
        if(actionListeners == null) {
            actionListeners = new EventDispatcher();
            actionListeners.setBlocking(false);
        }
        actionListeners.addListener(a);
    }

    /**
     * Removes the given listener
     *
     * @param a listener
     */
    public void removeResponseListener(ActionListener a) {
        if(actionListeners == null) {
            return;
        }
        actionListeners.removeListener(a);
        if(actionListeners.getListenerCollection()== null || actionListeners.getListenerCollection().size() == 0) {
            actionListeners = null;
        }
    }

    /**
     * Adds a listener that would be notified on the CodenameOne thread of a response code that
     * is not a 200 (OK) or 301/2 (redirect) response code.
     *
     * @param a listener
     */
    public void addResponseCodeListener(ActionListener a) {
        if(responseCodeListeners == null) {
            responseCodeListeners = new EventDispatcher();
            responseCodeListeners.setBlocking(false);
        }
        responseCodeListeners.addListener(a);
    }

    /**
     * Removes the given listener
     *
     * @param a listener
     */
    public void removeResponseCodeListener(ActionListener a) {
        if(responseCodeListeners == null) {
            return;
        }
        responseCodeListeners.removeListener(a);
        if(responseCodeListeners.getListenerCollection()== null || responseCodeListeners.getListenerCollection().size() == 0) {
            responseCodeListeners = null;
        }
    }

    /**
     * Returns true if someone is listening to action response events, this is useful
     * so we can decide whether to bother collecting data for an event in some cases
     * since building the event object might be memory/CPU intensive.
     * 
     * @return true or false
     */
    protected boolean hasResponseListeners() {
        return actionListeners != null;
    }

    /**
     * Fires the response event to the listeners on this connection
     *
     * @param ev the event to fire
     */
    protected void fireResponseListener(ActionEvent ev) {
        if(actionListeners != null) {
            actionListeners.fireActionEvent(ev);
        }
    }

    /**
     * Indicates whether this connection request supports duplicate entries in the request queue
     *
     * @return the duplicateSupported value
     */
    public boolean isDuplicateSupported() {
        return duplicateSupported;
    }

    /**
     * Indicates whether this connection request supports duplicate entries in the request queue
     * 
     * @param duplicateSupported the duplicateSupported to set
     */
    public void setDuplicateSupported(boolean duplicateSupported) {
        this.duplicateSupported = duplicateSupported;
    }

    /**
     * @inheritDoc
     */
    public int hashCode() {
        if(url != null) {
            int i = url.hashCode();
            if(requestArguments != null) {
                i = i ^ requestArguments.hashCode();
            }
            return i;
        }
        return 0;
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object o) {
        if(o != null && o.getClass() == getClass()) {
            ConnectionRequest r = (ConnectionRequest)o;

            // interned string comparison
            if(r.url == url) {
                if(requestArguments != null) {
                    if(r.requestArguments != null && requestArguments.size() == r.requestArguments.size()) {
                        Enumeration e = requestArguments.keys();
                        while(e.hasMoreElements()) {
                            Object key = e.nextElement();
                            Object value = requestArguments.get(key);
                            Object otherValue = r.requestArguments.get(key);
                            if(otherValue == null || !value.equals(otherValue)) {
                                return false;
                            }
                        }
                        return true;
                    }
                } else {
                    if(r.requestArguments == null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void validateImpl() {
        if(url == null) {
            throw new IllegalStateException("URL is null");
        }
        if(url.length() == 0) {
            throw new IllegalStateException("URL is empty");
        }
        validate();
    }

    /**
     * Validates that the request has the required information before being added to the queue
     * e.g. checks if the URL is null. This method should throw an IllegalStateException for
     * a case where one of the values required for this connection request is missing.
     * This method can be overriden by subclasses to add additional tests. It is usefull
     * to do tests here since the exception will be thrown immediately when invoking addToQueue
     * which is more intuitive to debug than the alternative.
     */
    protected void validate() {
        if(!url.toLowerCase().startsWith("http")) {
            throw new IllegalStateException("Only HTTP urls are supported!");
        }
    }

    /**
     * A dialog that will be seamlessly disposed once the given request has been completed
     *
     * @return the disposeOnCompletion
     */
    public Dialog getDisposeOnCompletion() {
        return disposeOnCompletion;
    }

    /**
     * A dialog that will be seamlessly disposed once the given request has been completed
     * 
     * @param disposeOnCompletion the disposeOnCompletion to set
     */
    public void setDisposeOnCompletion(Dialog disposeOnCompletion) {
        this.disposeOnCompletion = disposeOnCompletion;
    }

    /**
     * This dialog will be shown when this request enters the network queue
     *
     * @return the showOnInit
     */
    public Dialog getShowOnInit() {
        return showOnInit;
    }

    /**
     * This dialog will be shown when this request enters the network queue
     *
     * @param showOnInit the showOnInit to set
     */
    public void setShowOnInit(Dialog showOnInit) {
        this.showOnInit = showOnInit;
    }

    /**
     * Indicates the number of times to silently retry a connection that failed
     * before prompting
     *
     * @return the silentRetryCount
     */
    public int getSilentRetryCount() {
        return silentRetryCount;
    }

    /**
     * Indicates the number of times to silently retry a connection that failed
     * before prompting
     * @param silentRetryCount the silentRetryCount to set
     */
    public void setSilentRetryCount(int silentRetryCount) {
        this.silentRetryCount = silentRetryCount;
    }

    /**
     * Indicates that we are uninterested in error handling
     * @return the failSilently
     */
    public boolean isFailSilently() {
        return failSilently;
    }

    /**
     * Indicates that we are uninterested in error handling
     * @param failSilently the failSilently to set
     */
    public void setFailSilently(boolean failSilently) {
        this.failSilently = failSilently;
    }
    
    /**
     * Indicates whether the native Cookie stores should be used
     * @param b true to enable native cookie stores when applicable
     */
    public static void setUseNativeCookieStore(boolean b) {
        Util.getImplementation().setUseNativeCookieStore(b);
    }

    /**
     * When set to true the read response code will happen even for error codes such as 400 and 500
     * @return the readResponseForErrors
     */
    public boolean isReadResponseForErrors() {
        return readResponseForErrors;
    }

    /**
     * When set to true the read response code will happen even for error codes such as 400 and 500
     * @param readResponseForErrors the readResponseForErrors to set
     */
    public void setReadResponseForErrors(boolean readResponseForErrors) {
        this.readResponseForErrors = readResponseForErrors;
    }
    
    /**
     * Returns the content type from the response headers
     * @return the content type
     */
    public String getResponseContentType() {
        return responseContentType;
    }
    
    /**
     * Returns true if this request is been redirected to a different url
     * @return true if redirecting
     */ 
    public boolean isRedirecting(){
        return redirecting;
    }
}

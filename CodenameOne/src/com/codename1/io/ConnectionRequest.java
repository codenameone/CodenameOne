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
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.Base64;
import com.codename1.util.Callback;
import com.codename1.util.CallbackAdapter;
import com.codename1.util.CallbackDispatcher;
import com.codename1.util.FailureCallback;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * <p>This class represents a connection object in the form of a request response
 * typically common for HTTP/HTTPS connections. A connection request is added to
 * the {@link com.codename1.io.NetworkManager} for processing in a queue on one of the
 * network threads. You can read more about networking in Codename One {@link com.codename1.io here}</p>
 * 
 * <p>The sample
 * code below fetches a page of data from the nestoria housing listing API.<br>
 * You can see instructions on how to display the data in the {@link com.codename1.components.InfiniteScrollAdapter}
 * class. You can read more about networking in Codename One {@link com.codename1.io here}.</p>
 * <script src="https://gist.github.com/codenameone/22efe9e04e2b8986dfc3.js"></script>
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
     * The default value for the cacheMode property see {@link #getCacheMode()}
     * @return the defaultCacheMode
     */
    public static CachingMode getDefaultCacheMode() {
        return defaultCacheMode;
    }

    /**
     * The default value for the cacheMode property see {@link #getCacheMode()}
     * @param aDefaultCacheMode the defaultCacheMode to set
     */
    public static void setDefaultCacheMode(CachingMode aDefaultCacheMode) {
        defaultCacheMode = aDefaultCacheMode;
    }

    /**
     * There are 4 caching modes: OFF is the default  meaning no caching.
     * SMART means all get requests are cached intelligently and caching is "mostly" seamless
     * MANUAL means that the developer is responsible for the actual caching but the system
     * will not do a request on a resource that's already "fresh"
     * OFFLINE will fetch data from the cache and wont try to go to the server. It will generate
     * a 404 error if data isn't available
     * @return the cacheMode
     */
    public CachingMode getCacheMode() {
        return cacheMode;
    }

    /**
     * There are 4 caching modes: OFF is the default meaning no caching.
     * SMART means all get requests are cached intelligently and caching is "mostly" seamless
     * MANUAL means that the developer is responsible for the actual caching but the system
     * will not do a request on a resource that's already "fresh"
     * OFFLINE will fetch data from the cache and wont try to go to the server. It will generate
     * a 404 error if data isn't available
     * @param cacheMode the cacheMode to set
     */
    public void setCacheMode(CachingMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    /**
     * There are 4 caching modes: OFF is the default meaning no caching. 
     * SMART means all get requests are cached intelligently and caching is "mostly" seamless
     * MANUAL means that the developer is responsible for the actual caching but the system
     * will not do a request on a resource that's already "fresh"
     * OFFLINE will fetch data from the cache and wont try to go to the server. It will generate
     * a 404 error if data isn't available
     */
    public static enum CachingMode {
        OFF,
        MANUAL,
        SMART,
        OFFLINE
    }
    
    /**
     * The default value for the cacheMode property see {@link #getCacheMode()}
     */
    private static CachingMode defaultCacheMode = CachingMode.OFF;
    
    /**
     * There are 4 caching modes: OFF is the default meaning no caching. 
     * SMART means all get requests are cached intelligently and caching is "mostly" seamless
     * MANUAL means that the developer is responsible for the actual caching but the system
     * will not do a request on a resource that's already "fresh"
     * OFFLINE will fetch data from the cache and wont try to go to the server. It will generate
     * a 404 error if data isn't available
     */
    private CachingMode cacheMode = defaultCacheMode;
    
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

    /**
     * @return the cookiesEnabledDefault
     */
    public static boolean isCookiesEnabledDefault() {
        return cookiesEnabledDefault;
    }

    /**
     * @param aCookiesEnabledDefault the cookiesEnabledDefault to set
     */
    public static void setCookiesEnabledDefault(boolean aCookiesEnabledDefault) {
        if(!aCookiesEnabledDefault) {
            setUseNativeCookieStore(false);
        }
        cookiesEnabledDefault = aCookiesEnabledDefault;
    }

    private EventDispatcher actionListeners;

    /**
     * Enables/Disables automatic redirects globally and returns the 302 error code, <strong>IMPORTANT</strong>
     * this feature doesn't work on all platforms and currently doesn't work on iOS which always implicitly redirects
     * @return the defaultFollowRedirects
     */
    public static boolean isDefaultFollowRedirects() {
        return defaultFollowRedirects;
    }

    /**
     * Enables/Disables automatic redirects globally and returns the 302 error code, <strong>IMPORTANT</strong>
     * this feature doesn't work on all platforms and currently doesn't work on iOS which always implicitly redirects
     * @param aDefaultFollowRedirects the defaultFollowRedirects to set
     */
    public static void setDefaultFollowRedirects(boolean aDefaultFollowRedirects) {
        defaultFollowRedirects = aDefaultFollowRedirects;
    }

    private byte priority = PRIORITY_NORMAL;
    private long timeSinceLastUpdate;
    private LinkedHashMap requestArguments;

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
    private boolean duplicateSupported = true;
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
    private static boolean cookiesEnabledDefault = true;
    private boolean cookiesEnabled = cookiesEnabledDefault;
    private int chunkedStreamingLen = -1;
    private Exception failureException;
    private int failureErrorCode;
    private String destinationFile;
    private String destinationStorage;
    
    /**
     * The request body can be used instead of arguments to pass JSON data to a restful request,
     * it can't be used in a get request and will fail if you have arguments
     */
    private String requestBody;
    
    // Flag to indicate if the contentType was explicitly set for this 
    // request
    private boolean contentTypeSetExplicitly;
    
    /**
     * Workaround for https://bugs.php.net/bug.php?id=65633 allowing developers to
     * customize the name of the cookie header to Cookie
     */
    private static String cookieHeader = "cookie";
    
    /**
     * Default constructor
     */
    public ConnectionRequest() {
        if(NetworkManager.getInstance().isAPSupported()) {
            silentRetryCount = 1;
        }
    }

    /**
     * Construct a connection request to a url
     * 
     * @param url the url
     */
    public ConnectionRequest(String url) {
        this();
        setUrl(url);
    }
    

    /**
     * Construct a connection request to a url
     * 
     * @param url the url
     * @param post whether the request is a post url or a get URL
     */
    public ConnectionRequest(String url, boolean post) {
        this(url);
        setPost(post);
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

        if (getContentType() != null) {
            // UWP will automatically filter out the Content-Type header from GET requests
            // Historically, CN1 has always included this header even though it has no meaning
            // for GET requests.  it would be be better if CN1 did not include this header 
            // with GET requests, but for backward compatibility, I'll leave it on as
            // the default, and add a property to turn it off.
            //  -- SJH Sept. 15, 2016
            boolean shouldAddContentType = contentTypeSetExplicitly || 
                    Display.getInstance().getProperty("ConnectionRequest.excludeContentTypeFromGetRequests", "true").equals("false");

            if (isPost() || (getHttpMethod() != null && !"get".equals(getHttpMethod().toLowerCase()))) {
                shouldAddContentType = true;
            }

            if(shouldAddContentType) {
                impl.setHeader(connection, "Content-Type", getContentType());
            }
        }
        
        if(chunkedStreamingLen > -1){
            impl.setChunkedStreamingMode(connection, chunkedStreamingLen);
        }
        
        if(!post && (cacheMode == CachingMode.MANUAL || cacheMode == CachingMode.SMART)) {
            String msince = Preferences.get("cn1MSince" + createRequestURL(), null);
            if(msince != null) {
                impl.setHeader(connection, "If-Modified-Since", msince);
            } else {
                String etag = Preferences.get("cn1Etag" + createRequestURL(), null);
                if(etag != null) {
                    impl.setHeader(connection, "If-None-Match", etag);
                } 
            }
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
     * This method should be overriden in CacheMode.MANUAL to provide offline caching. The default
     * implementation will work as expected in the CacheMode.SMART mode.
     * @return the offline cached data or null/exception if unavailable
     */
    protected InputStream getCachedData() throws IOException{
        if(destinationFile != null) {
            if(FileSystemStorage.getInstance().exists(destinationFile)) {
                return FileSystemStorage.getInstance().openInputStream(destinationFile);
            }
            return null;
        } 
        
        if(destinationStorage != null) {
            if(Storage.getInstance().exists(destinationFile)) {
                return Storage.getInstance().createInputStream(destinationFile);
            }
            return null;
        } 
        
        String s = getCacheFileName();
        if(FileSystemStorage.getInstance().exists(s)) {
            return FileSystemStorage.getInstance().openInputStream(s);
        }
        return null;
    }
    
    /**
     * Deletes the cache file if it exists, notice that this will not work for download files 
     */
    public void purgeCache() {
        FileSystemStorage.getInstance().delete(getCacheFileName());
    }
    
    /**
     * This callback is invoked on a 304 server response indicating the data in the server matches the result
     * we currently have in the cache. This method can be overriden to detect this case 
     */
    protected void cacheUnmodified() throws IOException {
        if(destinationFile != null || destinationStorage != null) {
            if(hasResponseListeners() && !isKilled()) {
                if(destinationFile != null) {
                    data = Util.readInputStream(FileSystemStorage.getInstance().openInputStream(destinationFile));
                } else {
                    data = Util.readInputStream(Storage.getInstance().createInputStream(destinationStorage));
                }
                fireResponseListener(new NetworkEvent(this, data));
            }
            return;
        }
        InputStream is = FileSystemStorage.getInstance().openInputStream(getCacheFileName());
        readResponse(is);
        Util.cleanup(is);
        
    }
    
    /**
     * Purges all locally cached files
     */
    public static void purgeCacheDirectory() throws IOException {
        Set<String> s = Preferences.keySet();
        Iterator<String> i = s.iterator();
        ArrayList<String> remove = new ArrayList<String>();
        while(i.hasNext()) {
            String ss = i.next();
            if(ss.startsWith("cn1MSince") || ss.startsWith("cn1Etag")) {
                remove.add(ss);
            }
        }
        for(String ss : remove) {
            Preferences.set(ss, null);
        }
        String root;
        FileSystemStorage fs = FileSystemStorage.getInstance();
        if(fs.hasCachesDir()) {
            root = fs.getCachesDir() + "cn1ConCache/";
        } else {
            root = fs.getAppHomePath()+ "cn1ConCache/";
        }

        for(String ss : fs.listFiles(root)) {
            fs.delete(ss);
        }
    }
    
    private String getCacheFileName() {
        String root;
        if(FileSystemStorage.getInstance().hasCachesDir()) {
            root = FileSystemStorage.getInstance().getCachesDir() + "cn1ConCache/";
        } else {
            root = FileSystemStorage.getInstance().getAppHomePath()+ "cn1ConCache/";
        }
        FileSystemStorage.getInstance().mkdir(root);
        return root + Base64.encodeNoNewline(createRequestURL().getBytes()).replace('/', '-').replace('+', '_');
    }
    
    /**
     * Performs the actual network request on behalf of the network manager
     */
    void performOperation() throws IOException {
        if(shouldStop()) {
            return;
        }
        if(cacheMode == CachingMode.OFFLINE) {
            InputStream is = getCachedData();
            if(is != null) {
                readResponse(is);
                Util.cleanup(is);
            } else {
                responseCode = 404;
                throw new IOException("File unavilable in cache");
            }
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
                    cookieSent(first);
                    cookieStr.append(first.getName());
                    cookieStr.append("=");
                    cookieStr.append(first.getValue());
                    for(int iter = 1 ; iter < c ; iter++) {
                        Cookie current = (Cookie)v.elementAt(iter);
                        cookieStr.append(";");
                        cookieStr.append(current.getName());
                        cookieStr.append("=");
                        cookieStr.append(current.getValue());
                        cookieSent(current);
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
                if(requestBody != null) {
                    if(shouldWriteUTFAsGetBytes()) {
                        output.write(requestBody.getBytes("UTF-8"));
                    } else {
                        OutputStreamWriter w = new OutputStreamWriter(output, "UTF-8");
                        w.write(requestBody);
                    }
                } else {
                    buildRequestBody(output);
                }
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

            if(isCookiesEnabled()) {
                String[] cookies = impl.getHeaderFields("Set-Cookie", connection);
                if(cookies != null && cookies.length > 0){
                    Vector cook = new Vector();
                    int clen = cookies.length;
                    for(int iter = 0 ; iter < clen ; iter++) {
                        Cookie coo = parseCookieHeader(cookies[iter]);
                        if(coo != null) {
                            cook.addElement(coo);
                            cookieReceived(coo);
                        }
                    }
                    Cookie [] arr = new Cookie[cook.size()];
                    int arlen = arr.length;
                    for (int i = 0; i < arlen; i++) {
                        arr[i] = (Cookie) cook.elementAt(i);
                    }
                    impl.addCookie(arr);
                }
            }
            
            if(responseCode == 304 && cacheMode != CachingMode.OFF) {
                cacheUnmodified();
                return;
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
                    if(requestArguments != null && url.indexOf('?') > -1) {
                        requestArguments.clear();
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
            
            if(cacheMode == CachingMode.SMART || cacheMode == CachingMode.MANUAL) {
                String last = getHeader(connection, "Last-Modified");
                String etag = getHeader(connection, "ETag");
                Preferences.set("cn1MSince" + createRequestURL(), last);
                Preferences.set("cn1Etag" + createRequestURL(), etag);
            }
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
                if(!post && cacheMode == CachingMode.SMART && destinationFile == null && destinationStorage == null) {
                    byte[] d = Util.readInputStream(input);
                    OutputStream os = FileSystemStorage.getInstance().openOutputStream(getCacheFileName());
                    os.write(d);
                    os.close();
                    readResponse(new ByteArrayInputStream(d));
                } else {
                    readResponse(input);
                }
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
        if(!isKilled()) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    postResponse();
                }
            });
        }
    }
    
    /**
     * Callback invoked for every cookie received from the server
     * @param c the cookie
     */
    protected void cookieReceived(Cookie c) {
    }

    /**
     * Callback invoked for every cookie being sent to the server
     * @param c the cookie
     */
    protected void cookieSent(Cookie c) {
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
            failureException = err;
            return;
        }
        err.printStackTrace();
        if(silentRetryCount > 0) {
            silentRetryCount--;
            NetworkManager.getInstance().resetAPN();
            retry();
            return;
        }
        if(Display.isInitialized() && !Display.getInstance().isMinimized() &&
                Dialog.show("Exception", err.toString() + ": for URL " + url + "\n" + err.getMessage(), "Retry", "Cancel")) {
            retry();
        } else {
            retrying = false;
            killed = true;
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
            failureErrorCode = code;
            return;
        }
        if(responseCodeListeners != null) {
            if(!isKilled()) {
                NetworkEvent n = new NetworkEvent(this, code, message);
                responseCodeListeners.fireActionEvent(n);
            }
            return;
        }
        if(Display.isInitialized() && !Display.getInstance().isMinimized() &&
                Dialog.show("Error", code + ": " + message, "Retry", "Cancel")) {
            retry();
        } else {
            retrying = false;
            if(!isReadResponseForErrors()){
                killed = true;
            }
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
     * <strong>IMPORTANT</strong>
     * this feature doesn't work on all platforms and currently doesn't work on iOS which always implicitly redirects
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
        if(isKilled()) {
            return;
        }
        if(destinationFile != null) {
            OutputStream o = FileSystemStorage.getInstance().openOutputStream(destinationFile);
            Util.copy(input, o);
            Util.cleanup(o);
            
            // was the download killed while we downloaded
            if(isKilled()) {
                FileSystemStorage.getInstance().delete(destinationFile);
            }
        } else {
            if(destinationStorage != null) {
                OutputStream o = Storage.getInstance().createOutputStream(destinationStorage);
                Util.copy(input, o);
                Util.cleanup(o);
            
                // was the download killed while we downloaded
                if(isKilled()) {
                    Storage.getInstance().deleteStorageFile(destinationStorage);
                }
            } else {
                data = Util.readInputStream(input);
            }
        }
        if(hasResponseListeners() && !isKilled()) {
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
            Iterator e = requestArguments.keySet().iterator();
            if(e.hasNext()) {
                b.append("?");
            }
            while(e.hasNext()) {
                String key = (String)e.next();
                Object requestVal = requestArguments.get(key);
                if(requestVal instanceof String) {
                    String value = (String)requestVal;
                    b.append(key);
                    b.append("=");
                    b.append(value);
                    if(e.hasNext()) {
                        b.append("&");
                    }
                    continue;
                }
                String[] val = (String[])requestVal;
                int vlen = val.length;
                for(int iter = 0 ; iter < vlen - 1; iter++) {
                    b.append(key);
                    b.append("=");
                    b.append(val[iter]);
                    b.append("&");
                }
                b.append(key);
                b.append("=");
                b.append(val[vlen - 1]);
                if(e.hasNext()) {
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
            Iterator e = requestArguments.keySet().iterator();
            while(e.hasNext()) {
                String key = (String)e.next();
                Object requestVal = requestArguments.get(key);
                if(requestVal instanceof String) {
                    String value = (String)requestVal;
                    val.append(key);
                    val.append("=");
                    val.append(value);
                    if(e.hasNext()) {
                        val.append("&");
                    }
                    continue;
                }
                String[] valArray = (String[])requestVal;
                int vlen = valArray.length;
                for(int iter = 0 ; iter < vlen - 1; iter++) {
                    val.append(key);
                    val.append("=");
                    val.append(valArray[iter]);
                    val.append("&");
                }
                val.append(key);
                val.append("=");
                val.append(valArray[vlen - 1]);
                if(e.hasNext()) {
                    val.append("&");
                }
            }
            if(shouldWriteUTFAsGetBytes()) {
                os.write(val.toString().getBytes("UTF-8"));
            } else {
                OutputStreamWriter w = new OutputStreamWriter(os, "UTF-8");
                w.write(val.toString());
            }
        }
    }
    
    /**
     * Returns whether when writing a post body the platform expects something in the form of 
     * string.getBytes("UTF-8") or new OutputStreamWriter(os, "UTF-8"). 
     */
    protected boolean shouldWriteUTFAsGetBytes() {
        return Util.getImplementation().shouldWriteUTFAsGetBytes();
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
        NetworkManager.getInstance().kill9(this);
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
            throw new IllegalStateException("Request method (post/get) can't be modified once arguments have been assigned to the request");
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
        if(requestBody != null) {
            throw new IllegalStateException("Request body and arguments are mutually exclusive, you can't use both");
        }
        if(requestArguments == null) {
            requestArguments = new LinkedHashMap();
        }
        if(value == null || key == null){
            return;
        }
        if(post) {
            // this needs to be implicit for a post request with arguments
            setWriteRequest(true);
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
     * Add an argument to the request response as an array of elements, this will
     * trigger multiple request entries with the same key, notice that this doesn't implicitly
     * encode the value
     *
     * @param key the key of the argument
     * @param value the value for the argument
     */
    public void addArgumentNoEncoding(String key, String[] value) {
        if(value == null || value.length == 0) {
            return;
        }
        if(value.length == 1) {
            addArgumentNoEncoding(key, value[0]);
            return;
        }
        // copying the array to prevent mutation
        String[] v = new String[value.length];
        System.arraycopy(value, 0, v, 0, value.length);
        addArg(key, v);
    }
    
    /**
     * Add an argument to the request response as an array of elements, this will
     * trigger multiple request entries with the same key, notice that this doesn't implicitly
     * encode the value
     *
     * @param key the key of the argument
     * @param value the value for the argument
     */
    public void addArgumentNoEncodingArray(String key, String... value) {
        addArgumentNoEncoding(key, (String[])value);
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
    public void addArgumentArray(String key, String... value) {
        addArgument(key, value);
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
            int vlen = value.length;
            for(int iter = 0 ; iter < vlen ; iter++) {
                v[iter] = Util.encodeBody(value[iter]);
            }
            addArg(Util.encodeBody(key), v);
        } else {
            int vlen = value.length;
            for(int iter = 0 ; iter < vlen ; iter++) {
                v[iter] = Util.encodeUrl(value[iter]);
            }
            addArg(Util.encodeUrl(key), v);
        }
    }

    /**
     * Add an argument to the request response as an array of elements, this will
     * trigger multiple request entries with the same key
     *
     * @param key the key of the argument
     * @param value the value for the argument
     */
    public void addArguments(String key, String... value) {
        if(value.length == 1) {
            addArgument(key, value[0]);
        } else {
            addArgument(key, (String[])value);
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
        contentTypeSetExplicitly = true;
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
     * Enables/Disables automatic redirects globally and returns the 302 error code, <strong>IMPORTANT</strong>
     * this feature doesn't work on all platforms and currently doesn't work on iOS which always implicitly redirects
     * @return the followRedirects
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * Enables/Disables automatic redirects globally and returns the 302 error code, <strong>IMPORTANT</strong>
     * this feature doesn't work on all platforms and currently doesn't work on iOS which always implicitly redirects
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
     * This method prevents a manual timeout from occurring when invoked at a frequency faster
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
     * {@inheritDoc}
     */
    public void ioStreamUpdate(Object source, int bytes) {
        if(!isKilled()) {
            NetworkManager.getInstance().fireProgressEvent(this, progress, getContentLength(), bytes);
        }
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
    public void addResponseListener(ActionListener<NetworkEvent> a) {
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
    public void removeResponseListener(ActionListener<NetworkEvent> a) {
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
    public void addResponseCodeListener(ActionListener<NetworkEvent> a) {
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
    public void removeResponseCodeListener(ActionListener<NetworkEvent> a) {
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if(o != null && o.getClass() == getClass()) {
            ConnectionRequest r = (ConnectionRequest)o;

            // interned string comparison
            if(r.url == url) {
                if(requestArguments != null) {
                    if(r.requestArguments != null && requestArguments.size() == r.requestArguments.size()) {
                        Iterator e = requestArguments.keySet().iterator();
                        while(e.hasNext()) {
                            Object key = e.next();
                            Object value = requestArguments.get(key);
                            Object otherValue = r.requestArguments.get(key);
                            if(otherValue == null || !value.equals(otherValue)) {
                                return false;
                            }
                        }
                        return r.killed == killed;
                    }
                } else {
                    if(r.requestArguments == null) {
                        return r.killed == killed;
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

    /**
     * When set to a none null string saves the response to file system under
     * this file name
     * @return the destinationFile
     */
    public String getDestinationFile() {
        return destinationFile;
    }

    /**
     * When set to a none null string saves the response to file system under
     * this file name
     * @param destinationFile the destinationFile to set
     */
    public void setDestinationFile(String destinationFile) {
        this.destinationFile = destinationFile;
    }

    /**
     * When set to a none null string saves the response to storage under
     * this file name
     * @return the destinationStorage
     */
    public String getDestinationStorage() {
        return destinationStorage;
    }

    /**
     * When set to a none null string saves the response to storage under
     * this file name
     * @param destinationStorage the destinationStorage to set
     */
    public void setDestinationStorage(String destinationStorage) {
        this.destinationStorage = destinationStorage;
    }

    /**
     * @return the cookiesEnabled
     */
    public boolean isCookiesEnabled() {
        return cookiesEnabled;
    }

    /**
     * @param cookiesEnabled the cookiesEnabled to set
     */
    public void setCookiesEnabled(boolean cookiesEnabled) {
        this.cookiesEnabled = cookiesEnabled;
        if(!cookiesEnabled) {
            setUseNativeCookieStore(false);
        }
    }
    
     /**
     * This method is used to enable streaming of a HTTP request body without 
     * internal buffering, when the content length is not known in advance. 
     * In this mode, chunked transfer encoding is used to send the request body. 
     * Note, not all HTTP servers support this mode.
     * This mode is supported on Android and the Desktop ports.
     * 
     * @param chunklen The number of bytes to write in each chunk. If chunklen 
     * is zero a default value will be used.
     */ 
    public void setChunkedStreamingMode(int chunklen){    
        this.chunkedStreamingLen = chunklen;
    }
   

    /**
     * Utility method that returns a JSON structure or throws an IOException in case of a failure.
     * This method blocks the EDT legally and can be used synchronously. Notice that this method assumes
     * all JSON data is UTF-8
     * @param url the URL hosing the JSON
     * @return map data
     * @throws IOException in case of an error
     */
    public static Map<String, Object> fetchJSON(String url) throws IOException {
        ConnectionRequest cr = new ConnectionRequest();
        cr.setFailSilently(true);
        cr.setPost(false);
        cr.setUrl(url);
        NetworkManager.getInstance().addToQueueAndWait(cr);
        if(cr.getResponseData() == null) {
            if(cr.failureException != null) {
                throw new IOException(cr.failureException.toString());
            } else {
                throw new IOException("Server returned error code: " + cr.failureErrorCode);
            }
        }
        JSONParser jp = new JSONParser();
        Map<String, Object> result = jp.parseJSON(new InputStreamReader(new ByteArrayInputStream(cr.getResponseData()), "UTF-8"));
        return result;
    }
    
    /**
     * Downloads an image to a specified storage file asynchronously and calls the onSuccessCallback with the resulting image.  
     * If useCache is true, then this will first try to load the image from Storage if it exists.
     * @param storageFile The storage file where the file should be saved.
     * @param onSuccess Callback called if the image is successfully loaded.
     * @param onFail Callback called if we fail to load the image.
     * @param useCache If true, then this will first check the storage to see if the image is already downloaded.
     * @since 3.4
     */
    public void downloadImageToStorage(String storageFile, final SuccessCallback<Image> onSuccess, FailureCallback<Image> onFail, boolean useCache) {
        setDestinationStorage(storageFile);
        downloadImage(onSuccess, onFail, useCache);
    }
    
    /**
     * Downloads an image to a specified storage file asynchronously and calls the onSuccessCallback with the resulting image.  
     * If useCache is true, then this will first try to load the image from Storage if it exists.
     * 
     * @param storageFile The storage file where the file should be saved.
     * @param onSuccess Callback called if the image is successfully loaded.
     * @param useCache If true, then this will first check the storage to see if the image is already downloaded.
     * @since 3.4
     */
    public void downloadImageToStorage(String storageFile, SuccessCallback<Image> onSuccess, boolean useCache) {
        downloadImageToStorage(storageFile, onSuccess, new CallbackAdapter<Image>(), useCache);
    }
    
    /**
     * Downloads an image to a specified storage file asynchronously and calls the onSuccessCallback with the resulting image.  
     * This will first try to load the image from Storage if it exists.
     * 
     * @param storageFile The storage file where the file should be saved.
     * @param onSuccess Callback called if the image is successfully loaded.
     * @since 3.4
     */
    public void downloadImageToStorage(String storageFile, SuccessCallback<Image> onSuccess) {
        downloadImageToStorage(storageFile, onSuccess, new CallbackAdapter<Image>(), true);
    }
    
    /**
     * Downloads an image to a specified storage file asynchronously and calls the onSuccessCallback with the resulting image.  
     * This will first try to load the image from Storage if it exists.
     * 
     * @param storageFile The storage file where the file should be saved.
     * @param onSuccess Callback called if the image is successfully loaded.
     * @since 3.4
     */
    public void downloadImageToStorage(String storageFile, SuccessCallback<Image> onSuccess, FailureCallback<Image> onFail) {
        downloadImageToStorage(storageFile, onSuccess, onFail, true);
    }
    
    /**
     * Downloads an image to a the file system asynchronously and calls the onSuccessCallback with the resulting image.  
     * If useCache is true, then this will first try to load the image from Storage if it exists.
     * 
     * @param file The storage file where the file should be saved.
     * @param onSuccess Callback called if the image is successfully loaded.
     * @param onFail Callback called if we fail to load the image.
     * @param useCache If true, then this will first check the storage to see if the image is already downloaded.
     * @since 3.4
     */
    public void downloadImageToFileSystem(String file, final SuccessCallback<Image> onSuccess, FailureCallback<Image> onFail, boolean useCache) {
        setDestinationFile(file);
        downloadImage(onSuccess, onFail, useCache);
    }
    
    /**
     * Downloads an image to a the file system asynchronously and calls the onSuccessCallback with the resulting image.  
     * If useCache is true, then this will first try to load the image from Storage if it exists.
     * 
     * @param file The storage file where the file should be saved.
     * @param onSuccess Callback called if the image is successfully loaded.
     * @param useCache If true, then this will first check the storage to see if the image is already downloaded.
     * @since 3.4
     */
    public void downloadImageToFileSystem(String file, SuccessCallback<Image> onSuccess, boolean useCache) {
        downloadImageToFileSystem(file, onSuccess, new CallbackAdapter<Image>(), useCache);
    }
    
    /**
     * Downloads an image to a the file system asynchronously and calls the onSuccessCallback with the resulting image.  
     * This will first try to load the image from Storage if it exists.
     * 
     * @param file The storage file where the file should be saved.
     * @param onSuccess Callback called if the image is successfully loaded.
     * @since 3.4
     */
    public void downloadImageToFileSystem(String file, SuccessCallback<Image> onSuccess) {
        downloadImageToFileSystem(file, onSuccess, new CallbackAdapter<Image>(), true);
    }
    
    /**
     * Downloads an image to a the file system asynchronously and calls the onSuccessCallback with the resulting image.  
     * This will first try to load the image from Storage if it exists.
     * 
     * @param file The storage file where the file should be saved.
     * @param onSuccess Callback called if the image is successfully loaded.
     * @param onFail Callback called if the image fails to load.
     * @since 3.4
     */
    public void downloadImageToFileSystem(String file, SuccessCallback<Image> onSuccess, FailureCallback<Image> onFail) {
        downloadImageToFileSystem(file, onSuccess, onFail, true);
    }
    

    private void downloadImage(final SuccessCallback<Image> onSuccess, FailureCallback<Image> onFail) {
        downloadImage(onSuccess, onFail,  true);
    }
    
    private void downloadImage(final SuccessCallback<Image> onSuccess, final FailureCallback<Image> onFail, boolean useCache) {
        if (useCache) {
            Display.getInstance().scheduleBackgroundTask(new Runnable() {
                public void run() {
                    if (getDestinationFile() != null) {
                        String file = getDestinationFile();
                        FileSystemStorage fs = FileSystemStorage.getInstance();
                        if (fs.exists(file)) {
                            try {
                                EncodedImage img = EncodedImage.create(fs.openInputStream(file), (int)fs.getLength(file));
                                if (img == null) {
                                    throw new IOException("Failed to load image at "+file);
                                }
                                CallbackDispatcher.dispatchSuccess(onSuccess, img);
                            } catch (Exception ex) {
                                CallbackDispatcher.dispatchError(onFail, ex);
                            }
                        } else {
                            downloadImage(onSuccess, onFail, false);
                        }
                    } else if (getDestinationStorage() != null) {
                        String file = getDestinationStorage();
                        Storage fs = Storage.getInstance();
                        if (fs.exists(file)) {
                            try {
                                EncodedImage img = EncodedImage.create(fs.createInputStream(file), fs.entrySize(file));
                                if (img == null) {
                                    throw new IOException("Failed to load image at "+file);
                                }
                                CallbackDispatcher.dispatchSuccess(onSuccess, img);
                            } catch (Exception ex) {
                                CallbackDispatcher.dispatchError(onFail, ex);
                            }
                        } else {
                            downloadImage(onSuccess, onFail, false);
                        } 
                    }
                }
            });
                
        } else {
            final ActionListener onDownload = new ActionListener<NetworkEvent>() {

                public void actionPerformed(NetworkEvent nevt) {
                    if (nevt.getResponseCode() == 200) {
                        downloadImage(onSuccess, onFail, true);
                    } else {
                        if (nevt.getError() == null) {
                            nevt.setError(new IOException("Failed to get image:  Code was "+nevt.getResponseCode()));
                        }
                        CallbackDispatcher.dispatchError(onFail, nevt.getError());
                    }
                    removeResponseListener(this);
                }

               
            };
            addResponseListener(onDownload);
            NetworkManager.getInstance().addToQueue(this);
        }
        
    }

    /**
     * The request body can be used instead of arguments to pass JSON data to a restful request,
     * it can't be used in a get request and will fail if you have arguments
     * @return the requestBody
     */
    public String getRequestBody() {
        return requestBody;
    }

    /**
     * <p>The request body can be used instead of arguments to pass JSON data to a restful request,
     * it can't be used in a get request and will fail if you have arguments.</p>
     * <p>Notice that invoking this method blocks the {@link #buildRequestBody(java.io.OutputStream)} method
     * callback.</p>
     * @param requestBody a string to pass in the post body
     */
    public void setRequestBody(String requestBody) {
        if(requestArguments != null) {
            throw new IllegalStateException("Request body and arguments are mutually exclusive, you can't use both");
        }
        this.requestBody = requestBody;
    }
}

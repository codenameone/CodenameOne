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
package com.codename1.ui.html;

/**
 * DocumentInfo holds important information about a document that is loading.
 * This class is constructed internally by HTMLComponent and HTMLForm and is sent to the RequestHandler.
 * It is intended for the RequestHandler to use and update (For example update encoding according to the HTTP response, update URL in case of a redirect etc.)
 *
 * @author Ofir Leitner
 */
public class DocumentInfo {

    /**
     * ISO-8859-1 encoding, the default one
     */
    public final static String ENCODING_ISO = "ISO-8859-1";

    /**
     *  UTF8 encoding, very common
     */
    public final static String ENCODING_UTF8 = "UTF-8";

    /**
     * Indicates that the request is for a page
     */
    public static int TYPE_HTML = 0;

    /**
     * Indicates that the request is for an image
     */
    public static int TYPE_IMAGE = 1;

    /**
     * Indicates that the request is for a CSS file
     */
    public static int TYPE_CSS = 2;


    private String pageURL;
    private String baseURL;
    private String hostURL;
    private String protocol;

    private String params;
    private boolean postRequest;
    private String encoding=ENCODING_ISO;
    private int expectedContentType=TYPE_HTML;

    /**
     * Constructs the DocumentInfo with the given URL
     * 
     * @param url The URL of the document
     */
    DocumentInfo(String url) {
        setUrl(url);
    }

    /**
     * Constructs the DocumentInfo with the given URL
     *
     * @param url The URL of the document
     */
    DocumentInfo(String url,int type) {
        setUrl(url);
        expectedContentType=type;
    }

    /**
     * Constructs the DocumentInfo with the given URL
     * 
     * @param url The URL of the document
     * @param params The parameters
     * @param postRequest true if this is a POST request, false otherwise (i.e. GET)
     */
    DocumentInfo(String url, String params, boolean postRequest) {
        this.params = params;
        this.postRequest = postRequest;
        setUrl(url);
    }

    /**
     * Returns the absolute URL associated with this DocumentInfo object
     *
     * @return the absolute URL associated with this DocumentInfo object
     */
    public String getUrl() {
        return pageURL;
    }

    /**
     * Returns the full url string including parameters in GET request
     * 
     * @return the full url string including parameters in GET request
     */
    public String getFullUrl() {
        if ((postRequest) || (params==null) || (params.equals(""))) {
            return pageURL;
        } else {
            return pageURL+"?"+params;
        }
    }


    /**
     * Sets the URL to the specified URL
     *
     * @param url the URL to set as the URL of the document
     */
    public void setUrl(String url) {
        pageURL = convertURL(url);

        int index=pageURL.lastIndexOf('/');
        if (index==-1) {
            setBaseURL("");
            hostURL="";
            protocol="";
        } else {
            setBaseURL(pageURL.substring(0, index + 1));
            index=pageURL.indexOf("://");
            if (index!=-1) {
                protocol=pageURL.substring(0, index+1); //The protocol will be http: , ftp: (without the //)
            }
            index=pageURL.indexOf('/', index+3);
            if (index!=-1) {
                hostURL=pageURL.substring(0, index);
            } else {
                hostURL=pageURL;
                if (!protocol.startsWith("file")) { // for file the following is not relevant as "0file:///filename" means the base url is file:///
                    setBaseURL(pageURL + "/"); // The url was a domain without folder and missing a trailing / - i.e. http://www.sun.com
                }

            }
        }
    }

    /**
     * Returns the expected content type, one of TYPE_HTML, TYPE_IMAGE or TYPE_CSS
     *
     * @return the expected content type, one of TYPE_HTML, TYPE_IMAGE or TYPE_CSS
     */
    public int getExpectedContentType() {
        return expectedContentType;
    }

    /**
     * Sets this expected content type to be either TYPE_HTML, TYPE_IMAGE or TYPE_CSS
     * When the document itself is requested the type will be TYPE_HTML and when images in the document are requested the type will be TYPE_IMAGE
     * The differentiation is important to handle cases in which the HTMLComponent expects one type but the URL is has a resource of another type
     *
     * @param requestType the requestType to set, one of TYPE_HTML, TYPE_IMAGE or TYPE_CSS
     */
    public void setExpectedContentType(int requestType) {
        this.expectedContentType = requestType;
    }

    /**
     * Returns whether this document request is a POST request or not
     *
     * @return true if the document was requested via POST, false otherwise
     */
    public boolean isPostRequest() {
        return postRequest;
    }

    /**
     * Sets this DocumentInfo as using a POST request or not
     *
     * @param postRequest true if this is a POST request, false otherwise
     */
    public void setPostRequest(boolean postRequest) {
        this.postRequest = postRequest;
    }

    /**
     * Returns the request paramter as an percentage-encoded string
     *
     * @return the request paramter as an encoded string
     */
    public String getParams() {
        return params;
    }

    /**
     * Sets the request paramters of this request
     *
     * @param params The request paramters to set, should be as a percentage encoded string
     */
    public void setParams(String params) {
        this.params = params;
    }

    /**
     * Returns a string describing the document's encoding
     *
     * @return the document's encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Sets the document encoding (This can be determined via the charset attribute in the response)
     *
     * @param encoding the encoding to set. It is recommended to use the ENCODING_* constants when possible to avoid typos
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Check if the specified URL is an absolute URL
     * 
     * @param url the URL to check
     * @return true if the specified URL is an absolute URL, false otherwise
     */
    static boolean isAbsoluteURL(String url) {
        return (url.substring(0, Math.min(10,url.length())).indexOf("://")!=-1); //Absolute URL - check only the start of the string for a case where a    parameter is ....&url=http://..
    }

    /**
     * Converts the given URL to an absolute URL based on the current page's URL
     *
     * @param url The url to convert (Can be relative)
     * @return The absolute URL representing the given URL in relation to the current one.
     */
    String convertURL(String url) {
        if (url==null) {
            return pageURL; //Refresh
        }
        if (isAbsoluteURL(url)) {
            return url; //Absolute URL
        } else {
            if (url.startsWith("//")) { // Take just the protocol from the original url
                return protocol+url;
            } else if (url.startsWith("/")) { // Host name root
                return hostURL+url;
            } else if (url.startsWith(".")) { // Back or current folder
                int back=0;
                while ((url.length()>0) && (url.charAt(0)=='.')) {
                    if (url.startsWith("./")) { // Same folder
                        url=url.substring(2);
                    } else if (url.startsWith("../")) { // Go one folder up
                        url=url.substring(3);
                        back++;
                    } else if (url.equals("..")) {
                        url="";
                        back++;
                    } else { // either "." or something else not-understandable (resting the URL gets us out of the loop)
                        url="";
                    }
                }
                String folder=getBaseURL().substring(hostURL.length()+1); //+1 in order not to include the /
                while ((back>0) && (folder.length()>0)) {
                    back--;
                    folder=folder.substring(0, folder.length()-1);
                    int index=folder.lastIndexOf('/');
                    if (index==-1) {
                        folder="";
                    } else {
                        folder=folder.substring(0, index+1); // +1 to include the '/'
                    }
                }
                return hostURL+"/"+folder+url;
            } else { // Relative URL
                return getBaseURL()+url;
            }
        }
    }

    /**
     * Returns the base URL for this document
     *
     * @return the baseURL
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * Sets the base URL for this document. Usually this is deduced automatically from the page URL, but in some cases this is different, for example when an HREF attribute is provided in the BASE tag
     *
     * @param baseURL the baseURL to set
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

}

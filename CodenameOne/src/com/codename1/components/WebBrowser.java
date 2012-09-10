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
package com.codename1.components;

import com.codename1.io.ConnectionRequest;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.html.AsyncDocumentRequestHandler.IOCallback;
import com.codename1.ui.html.DocumentInfo;
import com.codename1.ui.html.HTMLComponent;
import com.codename1.ui.html.AsyncDocumentRequestHandlerImpl;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.html.DefaultHTMLCallback;
import com.codename1.ui.html.HTMLCallback;
import com.codename1.ui.layouts.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * A simple browser view that encapsulates a usable version of HTMLComponent or BrowserComponent and
 * automatically picks the right component for the platform preferring BrowserComponent whenever it
 * is supported.
 *
 * @author Shai Almog
 */
public class WebBrowser extends Container {

    private Component internal;
    private boolean isNative;
    private String page;

    /**
     * Default constructor
     */
    public WebBrowser() {
        super(new BorderLayout());
        if (BrowserComponent.isNativeBrowserSupported()) {
            isNative = true;
            BrowserComponent b = new BrowserComponent();
            b.addWebEventListener("onStart", new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    onStart((String) evt.getSource());
                }
            });

            b.addWebEventListener("onLoad", new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    onLoad((String) evt.getSource());
                }
            });
            b.addWebEventListener("onError", new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    onError((String) evt.getSource(), evt.getKeyEvent());
                }
            });
            internal = b;
        } else {
            isNative = false;
            HTMLComponent h = new HTMLComponent(new AsyncDocumentRequestHandlerImpl() {

                protected ConnectionRequest createConnectionRequest(final DocumentInfo docInfo,
                        final IOCallback callback, final Object[] response) {
                    return new ConnectionRequest() {

                        protected void buildRequestBody(OutputStream os) throws IOException {
                            if (isPost()) {
                                if (docInfo.getParams() != null) {
                                    OutputStreamWriter w = new OutputStreamWriter(os, docInfo.getEncoding());
                                    w.write(docInfo.getParams());
                                    w.flush();
                                }
                            }
                        }

                        protected void handleIOException(IOException err) {
                            if (callback == null) {
                                response[0] = err;
                            }
                            super.handleIOException(err);
                        }

                        protected boolean shouldAutoCloseResponse() {
                            return callback != null;
                        }

                        protected void readResponse(InputStream input) throws IOException {
                            if (callback != null) {
                                callback.streamReady(input, docInfo);
                            } else {
                                response[0] = input;
                                synchronized (LOCK) {
                                    LOCK.notify();
                                }
                            }
                        }

                        protected void handleErrorResponseCode(int code, String message) {
                            onError(message, code);
                        }

                        protected void handleException(Exception err) {
                            System.out.println("Error occured");
                            err.printStackTrace();
                        }

                        public boolean onRedirect(String url) {
                            onStart(url);
                            if(((HTMLComponent)internal).getPageStatus() == HTMLCallback.STATUS_CANCELLED){
                                return true;
                            }
                            return super.onRedirect(url);                            
                        }
                        
                        
                    };

                }
            });
            h.setIgnoreCSS(true);
            h.setHTMLCallback(new DefaultHTMLCallback() {

                public void pageStatusChanged(HTMLComponent htmlC, int status, String url) {
                    if (status == STATUS_REQUESTED && url != null) {
                        onStart(url);
                    } else if (status == STATUS_DISPLAYED && url != null) {
                        onLoad(url);
                    } else if (status == STATUS_ERROR) {
                        onError("error on page", -1);
                    }
                }
            });

            internal = h;
        }
        addComponent(BorderLayout.CENTER, internal);
    }

    /**
     * This is a callback method, this method is called before the url has been
     * loaded
     * @param url 
     */
    public void onStart(String url) {
    }

    /**
     * This is a callback method, this method is called after the url has been
     * loaded
     * @param url 
     */
    public void onLoad(String url) {
    }

    /**
     * This is a callback method to inform on an error.
     * @param message 
     * @param errorCode 
     */
    public void onError(String message, int errorCode) {
    }

    /**
     * Since the internal component can be either an HTMLComponent or a BrowserComponent one of them
     * will be returned. If you are targeting modern smartphones only you can rely on this method 
     * returning a BrowserComponent instance.
     * @return BrowserComponent or HTMLComponent
     */
    public Component getInternal() {
        return internal;
    }

    /**
     * Returns the title for the browser page
     * @return the title
     */
    public String getTitle() {
        if (isNative) {
            return ((BrowserComponent) internal).getTitle();
        } else {
            return ((HTMLComponent) internal).getTitle();
        }
    }

    /**
     * The page URL
     * @return the URL
     */
    public String getURL() {
        if (isNative) {
            return ((BrowserComponent) internal).getURL();
        } else {
            return ((HTMLComponent) internal).getPageURL();
        }
    }

    /**
     * Sets the page URL, jar: URL's must be supported by the implementation
     * @param url  the URL
     */
    public void setURL(String url) {
        if (isNative) {
            ((BrowserComponent) internal).setURL(url);
        } else {
            ((HTMLComponent) internal).setPage(url);
        }
    }

    /**
     * Reload the current page
     */
    public void reload() {
        if (isNative) {
            ((BrowserComponent) internal).reload();
        } else {
            ((HTMLComponent) internal).refreshDOM();
        }
    }

    /**
     * Stop loading the current page
     */
    public void stop() {
        if (isNative) {
            ((BrowserComponent) internal).stop();
        } else {
            ((HTMLComponent) internal).cancel();
        }
    }
    
    /**
     * Release WebBrowser native resources.
     */
    public void destroy() {
        if (isNative) {
            ((BrowserComponent) internal).destroy();
        }
        internal = null;
    }

    /**
     * Shows the given HTML in the native viewer
     *
     * @param html HTML web page
     * @param baseUrl base URL to associate with the HTML
     */
    public void setPage(String html, String baseUrl) {
        page = html;
        if (isNative) {
            ((BrowserComponent) internal).setPage(html, baseUrl);
        } else {
            ((HTMLComponent) internal).setHTML(html, "UTF-8", null, true);
        }
    }

    /**
     * Returns the page set by getPage for the GUI builder
     * 
     * @return the HTML page set manually
     */
    public String getPage() {
        return page;
    }

    /**
     * @inheritDoc
     */
    public String[] getPropertyNames() {
        return new String[]{"url", "html"};
    }

    /**
     * @inheritDoc
     */
    public Class[] getPropertyTypes() {
        return new Class[]{String.class, String.class};
    }

    /**
     * @inheritDoc
     */
    public Object getPropertyValue(String name) {
        if (name.equals("url")) {
            return getURL();
        }
        if (name.equals("html")) {
            return getPage();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String setPropertyValue(String name, Object value) {
        if (name.equals("url")) {
            setURL((String) value);
            return null;
        }
        if (name.equals("html")) {
            setPage((String) value, null);
            return null;
        }
        return super.setPropertyValue(name, value);
    }
}

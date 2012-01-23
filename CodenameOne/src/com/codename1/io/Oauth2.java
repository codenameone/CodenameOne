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

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.html.AsyncDocumentRequestHandler.IOCallback;
import com.codename1.ui.html.DocumentInfo;
import com.codename1.ui.html.HTMLComponent;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.html.AsyncDocumentRequestHandlerImpl;
import com.codename1.ui.layouts.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This is a utility class that allows Oauth2 authentication
 * This utility uses the Codename One XHTML Component to display the authentication
 * pages.
 * http://tools.ietf.org/pdf/draft-ietf-oauth-v2-12.pdf
 *
 * @author Chen Fishbein
 */
public class Oauth2 {

    public static final String TOKEN = "access_token";
    private String token;
    private String clientId;
    private String redirectURI;
    private String scope;
    private String OauthURL;

    private Hashtable additionalParams;

    private IOException error;

    /**
     * Simple constructor
     *
     * @param OauthURL the authentication url of the service
     * @param clientId the client id that would like to use the service
     * @param redirectURI the redirect uri
     * @param scope the authentication scope
     */
    public Oauth2(String OauthURL, String clientId, String redirectURI, String scope) {
        this(OauthURL, clientId, redirectURI, scope, null);
    }

    /**
     * Simple constructor
     *
     * @param OauthURL the authentication url of the service
     * @param clientId the client id that would like to use the service
     * @param redirectURI the redirect uri
     * @param scope the authentication scope
     * @param additionalParams hashtable of additional parameters to the
     * authentication request
     */
    public Oauth2(String OauthURL, String clientId, String redirectURI, String scope, Hashtable additionalParams) {
        this.OauthURL = OauthURL;
        this.redirectURI = redirectURI;
        this.clientId = clientId;
        this.scope = scope;
        this.additionalParams = additionalParams;
    }

    /**
     * This method preforms the actual authentication, this method is a blocking
     * method that will display the user the html authentication pages.
     *
     * @return the method if passes authentication will return the access token
     * or null if authentication failed.
     *
     * @throws IOException the method will throw an IOException if something went
     * wrong in the communication.
     */
    public String authenticate() throws IOException{

        if (token == null) {
            final Form current = Display.getInstance().getCurrent();
            final boolean[] loginFlag = new boolean[1];
            error = null;

            Form login = new Form();
            login.setLayout(new BorderLayout());
            login.setScrollable(false);
            Component html = createLoginComponent(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    loginFlag[0] = true;
                }
            });
            login.addComponent(BorderLayout.CENTER, html);
            login.show();
            Display.getInstance().invokeAndBlock(new Runnable() {

                public void run() {
                    while (!loginFlag[0]) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    if (current != null) {
                        current.show();
                    }
                }
            });
            if(error != null){
                throw error;
            }
        }

        return token;
    }

    private Component createLoginComponent(final ActionListener loginCallback) {

        String URL = OauthURL + "?client_id=" + clientId
                + "&redirect_uri=" + Util.encodeUrl(redirectURI) + "&scope=" + scope + "&response_type=token";

        if (additionalParams != null) {
            Enumeration e = additionalParams.keys();
            while(e.hasMoreElements()){
                String key = (String) e.nextElement();
                String val = additionalParams.get(key).toString();
                URL += "&" + key + "=" + val;
            }
        }

        HTMLComponent c = new HTMLComponent(new AsyncDocumentRequestHandlerImpl() {

            protected ConnectionRequest createConnectionRequest(final DocumentInfo docInfo, final IOCallback callback, final Object[] response) {
                return new ConnectionRequest() {

                    protected void buildRequestBody(OutputStream os) throws IOException {
                        if (isPost()) {
                            if (docInfo.getParams() != null) {
                                OutputStreamWriter w = new OutputStreamWriter(os, docInfo.getEncoding());
                                w.write(docInfo.getParams());
                            }
                        }
                    }

                    protected void handleIOException(IOException err) {
                        if (callback == null) {
                            response[0] = err;
                        }
                        error = err;
                        loginCallback.actionPerformed(null);
                    }

                    protected boolean shouldAutoCloseResponse() {
                        return callback != null;
                    }

                    protected void readResponse(InputStream input) throws IOException {
                        BufferedInputStream i;
                        if (input instanceof BufferedInputStream) {
                            i = (BufferedInputStream) input;
                        } else {
                            i = new BufferedInputStream(input);
                        }
                        i.setYield(-1);
                        if (callback != null) {
                            callback.streamReady(input, docInfo);
                        } else {
                            response[0] = input;
                            synchronized (LOCK) {
                                LOCK.notify();
                            }
                        }
                    }

                    public boolean onRedirect(String url) {
                        if ((url.startsWith(redirectURI))){
                            boolean success = url.indexOf("#") > -1;
                            if(success){
                                String accessToken = url.substring(url.indexOf("#") + 1);
                                token = accessToken.substring(accessToken.indexOf("=") + 1, accessToken.indexOf("&"));
                            }
                            loginCallback.actionPerformed(null);
                            return true;
                        }
                        return false;
                    }
                };
            }
        });

        c.setPage(URL);
        c.getDocumentInfo().setPostRequest(true);
        c.setIgnoreCSS(true);
        c.getDocumentInfo().setEncoding("UTF-8");
        return c;
    }
    
}

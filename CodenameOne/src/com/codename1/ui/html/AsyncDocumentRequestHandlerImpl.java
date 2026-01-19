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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;

/**
 * Implementation of the HTML components document request handler to allow simple
 * HTML support in CodenameOne.
 *
 * @author Shai Almog
 */
public class AsyncDocumentRequestHandlerImpl extends DefaultDocumentRequestHandler {
    protected static final Object LOCK = new Object();

    /**
     * {@inheritDoc}
     */
    @Override
    public void resourceRequestedAsync(final DocumentInfo docInfo, final IOCallback callback) {
        String url = docInfo.getUrl();
        if (url.startsWith("jar://") || url.startsWith("res://") || url.startsWith("local://")) {
            super.resourceRequestedAsync(docInfo, callback);
            return;
        }
        visitingURL(url);
        resourceRequested(docInfo, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream resourceRequested(DocumentInfo docInfo) {
        return null;
    }

    private InputStream resourceRequested(final DocumentInfo docInfo, final IOCallback callback) {
        try {
            if (docInfo.getUrl().startsWith("file://")) {
                String url = docInfo.getUrl();

                // trim anchors
                int hash = url.indexOf('#');
                if (hash != -1) {
                    url = url.substring(0, hash);
                }
                callback.streamReady(FileSystemStorage.getInstance().openInputStream(url), docInfo);
                return null;
            }
        } catch (IOException ex) {
            Log.e(ex);
        }
        final Object[] response = new Object[1];

        ConnectionRequest reqest = createConnectionRequest(docInfo, callback, response);
        reqest.setPost(docInfo.isPostRequest());
        if (docInfo.isPostRequest()) {
            reqest.setUrl(docInfo.getUrl());
            reqest.setWriteRequest(true);
        } else {
            reqest.setUrl(docInfo.getFullUrl());
        }

        NetworkManager.getInstance().addToQueue(reqest);

        if (callback == null) {
            synchronized (LOCK) {
                while (response[0] == null) {
                    try {
                        LOCK.wait(50);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                if (response[0] instanceof InputStream) {
                    return (InputStream) response[0];
                }

                // we need a better way to handle this...
                if (response[0] instanceof Throwable) {
                    ((Throwable) response[0]).printStackTrace();
                }
            }
        }

        return null;
    }

    protected ConnectionRequest createConnectionRequest(final DocumentInfo docInfo,
                                                        final IOCallback callback, final Object[] response) {
        return new AsyncDocumentConnectionRequest(docInfo, callback, response);
    }

    private static class AsyncDocumentConnectionRequest extends ConnectionRequest {
        private final DocumentInfo docInfo;
        private final IOCallback callback;
        private final Object[] response;

        public AsyncDocumentConnectionRequest(DocumentInfo docInfo, IOCallback callback, Object[] response) {
            this.docInfo = docInfo;
            this.callback = callback;
            this.response = response;
        }

        @Override
        protected void buildRequestBody(OutputStream os) throws IOException {
            if (isPost()) {
                if (docInfo.getParams() != null) {
                    OutputStreamWriter w = new OutputStreamWriter(os, docInfo.getEncoding());
                    w.write(docInfo.getParams());
                }
            }
        }

        @Override
        protected void handleIOException(IOException err) {
            if (callback == null) {
                response[0] = err;
            }
            super.handleIOException(err);
        }

        @Override
        protected boolean shouldAutoCloseResponse() {
            return callback != null;
        }

        @Override
        protected void readResponse(InputStream input) throws IOException {
            if (callback != null) {
                callback.streamReady(input, docInfo);
            } else {
                synchronized (LOCK) {
                    response[0] = input;
                    LOCK.notifyAll();
                }
            }
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof AsyncDocumentConnectionRequest)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            AsyncDocumentConnectionRequest that = (AsyncDocumentConnectionRequest) o;
            return (docInfo == null ? that.docInfo == null : docInfo.equals(that.docInfo)) && (callback == null ? that.callback == null : callback.equals(that.callback)) && Arrays.equals(response, that.response);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (docInfo != null ? docInfo.hashCode() : 0);
            result = 31 * result + (callback != null ? callback.hashCode() : 0);
            result = 31 * result + Arrays.hashCode(response);
            return result;
        }
    }
}

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

import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.util.Resources;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Vector;

/**
 * Default implementation of the HTML components document request handler to allow simple
 * HTML support in Codename One. This version includes only the basics supported by MIDP
 * e.g. resources and jar file URL's such as jar:// and res://
 *
 * @author Shai Almog
 */
public class DefaultDocumentRequestHandler implements AsyncDocumentRequestHandler {
    private static Resources resFile;
    private boolean trackVisitedURLs;
    private Vector visitedURLs;

    /**
     * @inheritDoc
     */
    public void resourceRequestedAsync(final DocumentInfo docInfo, final IOCallback callback) {
        resourceRequested(docInfo, callback);
    }

    /**
     * @inheritDoc
     */
    public InputStream resourceRequested(DocumentInfo docInfo) {
        return null;
    }

    /**
     * This method can be invoked to indicate a URL was visited fro tracking
     * 
     * @param url the url
     */
    protected void visitingURL(String url) {
        if(trackVisitedURLs) {
            if(visitedURLs == null) {
                visitedURLs = new Vector();
            }
            if(!visitedURLs.contains(url)) {
                visitedURLs.addElement(url);
            }
        }
    }

    /**
     * Returns true if the URL was visited, requires trackVisitedURLs to be true
     * 
     * @param url the url
     * @return true if it was visited
     */
    public boolean wasURLVisited(String url) {
        return visitedURLs != null && visitedURLs.contains(url);
    }

    private InputStream resourceRequested(final DocumentInfo docInfo, final IOCallback callback) {
        String url = docInfo.getUrl();

        visitingURL(url);

        // trim anchors
        int hash = url.indexOf('#');
        if (hash!=-1) {
           url = url.substring(0,hash);
        }

        if(url.startsWith("jar://")) {
            callback.streamReady(Display.getInstance().getResourceAsStream(getClass(), docInfo.getUrl().substring(6)), docInfo);
            return null;
        } else {
            try {
                if(url.startsWith("local://")) {
                    Image img = resFile.getImage(url.substring(8));
                    if(img instanceof EncodedImage) {
                        callback.streamReady(new ByteArrayInputStream(((EncodedImage)img).getImageData()),
                                docInfo);
                    }
                }
                if(url.startsWith("res://")) {
                    InputStream i = Display.getInstance().getResourceAsStream(getClass(), docInfo.getUrl().substring(6));
                    Resources r = Resources.open(i);
                    i.close();
                    i = r.getData(docInfo.getParams());
                    if(i != null) {
                        callback.streamReady(i, docInfo);
                    } else {
                        Image img = r.getImage(docInfo.getParams());
                        if(img instanceof EncodedImage) {
                            callback.streamReady(new ByteArrayInputStream(((EncodedImage)img).getImageData()),
                                    docInfo);
                        }
                    }
                    return null;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Allows URL's referring to a res:// local resource to default to this file
     *
     * @return the resFile
     */
    public static Resources getResFile() {
        return resFile;
    }

    /**
     * Allows URL's referring to a local:// local resource to default to this file
     * 
     * @param res the resource
     */
    public static void setResFile(Resources res) {
        resFile = res;
    }

    /**
     * Allows tracking whether a URL was visited or not
     * @return the trackVisitedURLs
     */
    public boolean isTrackVisitedURLs() {
        return trackVisitedURLs;
    }

    /**
     * Allows tracking whether a URL was visited or not
     * @param trackVisitedURLs the trackVisitedURLs to set
     */
    public void setTrackVisitedURLs(boolean trackVisitedURLs) {
        this.trackVisitedURLs = trackVisitedURLs;
        visitedURLs = null;
    }
}

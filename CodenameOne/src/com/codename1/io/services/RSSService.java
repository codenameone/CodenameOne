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

package com.codename1.io.services;

import com.codename1.io.CharArrayReader;
import com.codename1.ui.Dialog;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.ui.Image;
import com.codename1.xml.Element;
import com.codename1.xml.ParserCallback;
import com.codename1.xml.XMLParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Simple RSS read and parse request, to handle errors just subclass this and override
 * parsingError.
 *
 * @author Shai Almog
 */
public class RSSService extends ConnectionRequest implements ParserCallback {
    private Vector results;
    private int limit = -1;
    private int startOffset = -1;
    private boolean hasMore;
    private boolean createPlainTextDetails = true;
    private Image iconPlaceholder;

    /**
     * Simple constructor accepting the RSS url
     *
     * @param url rss link
     */
    public RSSService(String url) {
        setUrl(url);
        setPost(false);
        setDuplicateSupported(true);
    }

    /**
     * Simple constructor accepting the RSS url
     *
     * @param url rss link
     * @param limit the limit on the number of RSS entries supported
     */
    public RSSService(String url, int limit) {
        this(url);
        this.limit = limit;
        setDuplicateSupported(true);
    }

    /**
     * Simple constructor accepting the RSS url
     *
     * @param url rss link
     * @param limit the limit on the number of RSS entries supported
     * @param startOffset indicates the entry offset which we are interested
     * in, this is useful if previously the limit for RSS entries was reached.
     */
    public RSSService(String url, int limit, int startOffset) {
        this(url, limit);
        this.startOffset = startOffset;
        setDuplicateSupported(true);
    }

    /**
     * @inheritDoc
     */
    protected void readResponse(InputStream input) throws IOException {
        results = new Vector();
        class FinishParsing extends RuntimeException {
        }
        XMLParser p = new XMLParser() {
            private String lastTag;
            private Hashtable current;
            private String url;
            protected boolean startTag(String tag) {
                if("item".equalsIgnoreCase(tag) || "entry".equalsIgnoreCase(tag)) {
                    if(startOffset > 0) {
                        return true;
                    }
                    current = new Hashtable();
                    if(iconPlaceholder != null) {
                        current.put("icon", iconPlaceholder);
                    }
                }
                lastTag = tag;
                return true;
            }

            protected void attribute(String tag, String attributeName, String value) {
                if(current != null) {
                    if("media:thumbnail".equalsIgnoreCase(tag) && "url".equalsIgnoreCase(attributeName)) {
                        current.put("thumb", value);
                    } else {
                        if("media:player".equalsIgnoreCase(tag) && "url".equalsIgnoreCase(attributeName)) {
                            current.put("player", value);
                        }
                    }
                }
            }

            protected void textElement(String text) {
                if(lastTag != null && current != null) {
                    // make "ATOM" seem like RSS
                    if("summary".equals(lastTag)) {
                        current.put("details", text);
                    } else {
                        if("content".equals(lastTag)) {
                            current.put("description", text);
                        } else {
                            current.put(lastTag, text);
                        }
                    }
                }
            }

            protected void endTag(String tag) {                
                if("item".equalsIgnoreCase(tag) || "entry".equalsIgnoreCase(tag)) {
                    if(startOffset > 0) {
                        startOffset--;
                        return;
                    }
                    results.addElement(current);
                    current = null;
                    if(limit > -1 && results.size() >= limit) {
                        throw new FinishParsing();
                    }
                }
                if(tag.equals(lastTag)) {
                    lastTag = null;
                }
            }
        };
        p.setParserCallback(this);
        input.mark(10);

        // Skip the bom marking UTF-8 in some streams
        while(input.read() != '<') {
            //input.mark(4);
        }
        int question = input.read();
        String cType = "UTF-8";
        if(question == '?') {
            // we are in an XML header, check if the encoding section exists 
            StringBuilder cs = new StringBuilder();
            question = input.read();
            while(question != '>') {
                cs.append((char)question);
                question = input.read();
            }
            String str = cs.toString();
            int index = str.indexOf("encoding=\"") + 10;
            if(index > -1) {
                cType = str.substring(index, Math.max(str.indexOf("\"", index), str.indexOf("'", index)));
            }
        } else {
            // oops, continue as usual
            input.reset();
        }

        String resultType = getResponseContentType();
        if(resultType != null && resultType.indexOf("charset=") > -1) {
            cType = resultType.substring(resultType.indexOf("charset=") + 8);
        }
        try {
            int pos2 = cType.indexOf(';');
            if(pos2 > 0) {
                cType = cType.substring(0, pos2);
            }
            p.eventParser(new InputStreamReader(input, cType));
        } catch(FinishParsing ignor) {
            hasMore = true;
        }

        if(isCreatePlainTextDetails()) {
            int elementCount = results.size();
            for(int iter = 0 ; iter < elementCount ; iter++) {
                Hashtable h = (Hashtable)results.elementAt(iter);
                String s = (String)h.get("description");
                if(s != null && !h.containsKey("details")) {
                    XMLParser x = new XMLParser();
                    Element e = x.parse(new CharArrayReader(("<xml>" + s + "</xml>").toCharArray()));
                    Vector results = e.getTextDescendants(null, false);
                    StringBuilder endResult = new StringBuilder();
                    for(int i = 0 ; i < results.size() ; i++) {
                        endResult.append(((Element)results.elementAt(i)).getText());
                    }
                    h.put("details", endResult.toString());
                }
            }
        }
        
        fireResponseListener(new NetworkEvent(this, results));
    }


    /**
     * The results are presented as a vector of hashtables easily presentable in Codename One
     *
     * @return vector of hashtables
     */
    public Vector getResults() {
        return results;
    }

    /**
     * @inheritDoc
     */
    public boolean parsingError(int errorId, String tag, String attribute, String value, String description) {
        return Dialog.show("Parsing Error", description, "Continue", "Cancel");
    }

    /**
     * Indicates whether more entries might be available since the limt might have been reached
     *
     * @return the hasMore
     */
    public boolean hasMore() {
        return hasMore;
    }

    /**
     * Creates an additional "details" attribute in the resulting hashtables
     * which effectively contains a plain text version of the description tag.
     *
     * @return the createPlainTextDetails
     */
    public boolean isCreatePlainTextDetails() {
        return createPlainTextDetails;
    }

    /**
     * Creates an additional "details" attribute in the resulting hashtables
     * which effectively contains a plain text version of the description tag.
     * 
     * @param createPlainTextDetails the createPlainTextDetails to set
     */
    public void setCreatePlainTextDetails(boolean createPlainTextDetails) {
        this.createPlainTextDetails = createPlainTextDetails;
    }

    /**
     * @return the iconPlaceholder
     */
    public Image getIconPlaceholder() {
        return iconPlaceholder;
    }

    /**
     * @param iconPlaceholder the iconPlaceholder to set
     */
    public void setIconPlaceholder(Image iconPlaceholder) {
        this.iconPlaceholder = iconPlaceholder;
    }
}

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
package com.codename1.facebook;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This class represents a Facebook Photo Object
 *  http://developers.facebook.com/docs/reference/api/photo/
 *
 * @author Chen Fishbein
 */
public class Photo extends FBObject {

    private User from = new User();
    private String name;
    private String iconUrl;
    private String pictureUrl;
    private String sourceUrl;
    private int height;
    private int width;
    private String link;
    private String created_time;
    private String updated_time;
    private int position;
    private  Vector images;
    private Vector comments;

    /**
     * Empty Contructor
     */
    public Photo() {
    }

    /**
     * @inheritDoc
     */
    public Photo(Hashtable props) {
        super(props);
        init(props);
    }

    /**
     *  Get created_time
     * @return created_time
     */
    public String getCreated_time() {
        return created_time;
    }

    /**
     *  Get from
     * @return from
     */
    public User getFrom() {
        return from;
    }

    /**
     *  Get height
     * @return height
     */
    public int getHeight() {
        return height;
    }

    /**
     *  Get iconUrl
     * @return iconUrl
     */
    public String getIconUrl() {
        return iconUrl;
    }

    /**
     *  Get link
     * @return link
     */
    public String getLink() {
        return link;
    }

    /**
     *  Get name
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     *  Get pictureUrl
     * @return pictureUrl
     */
    public String getPictureUrl() {
        return pictureUrl;
    }

    /**
     *  Get position
     * @return position
     */
    public int getPosition() {
        return position;
    }

    /**
     *  Get sourceUrl
     * @return sourceUrl
     */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     *  Get updated_time
     * @return updated_time
     */
    public String getUpdated_time() {
        return updated_time;
    }

    /**
     *  Get width
     * @return width
     */
    public int getWidth() {
        return width;
    }

    /**
     *  Get images vector where each entry is a String of a url
     * @return images
     */
    public Vector getImages() {
        return images;
    }

    /**
     * Gets the comments on this Photos, where each
     * entry is a Post Object
     * @return a Vector of Post Objects
     */
    public Vector getComments() {
        return comments;
    }
    
    /**
     * copies the relevant values from the given hashtable
     * @param props an hashtable to copy from
     */
    public void copy(Hashtable props){
        super.copy(props);
        init(props);
    }

    private void init(Hashtable toCopy) {
        super.copy(toCopy);
        Hashtable f = (Hashtable) toCopy.get("from");
        if (f != null) {
            from.copy(f);
        }

        name = (String) toCopy.get("name");
        iconUrl = (String) toCopy.get("icon");
        pictureUrl = (String) toCopy.get("picture");
        sourceUrl = (String) toCopy.get("source");
        String heightStr = (String) toCopy.get("height");
        if(heightStr != null){
            height = Integer.parseInt(heightStr);
        }
        String widthStr = (String) toCopy.get("width");
        if(widthStr != null){
            width = Integer.parseInt(widthStr);
        }
        link = (String) toCopy.get("link");
        created_time = (String) toCopy.get("created_time");
        updated_time = (String) toCopy.get("updated_time");
        String positionStr = (String) toCopy.get("position");
         if(widthStr != null){
            position = Integer.parseInt(positionStr);
        }

        images = (Vector) toCopy.get("images");

       Hashtable data = (Hashtable) toCopy.get("comments");
        if (data != null) {
            comments = new Vector();
            Vector commentsArray = (Vector) data.get("data");
            for (int i = 0; i < commentsArray.size(); i++) {
                Hashtable comment = (Hashtable) commentsArray.elementAt(i);
                Post p = new Post();
                p.copy(comment);
                comments.addElement(p);
            }
        }

    }
}

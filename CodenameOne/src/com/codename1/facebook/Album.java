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

/**
 * This class represents a Facebook Photo Object
 *  http://developers.facebook.com/docs/reference/api/album
 *
 * @author Chen Fishbein
 */
public class Album extends FBObject {

    private User from = new User();
    private String name;
    private String description;
    private String location;
    private String link;
    private String cover_photo;
    private String privacy;
    private int count = -1;
    private String type;
    private String created_time;
    private String updated_time;


    /**
     * Empty Contructor
     */
    public Album() {
    }

    /**
     * @inheritDoc
     */
    public Album(Hashtable props) {
        super(props);
        init(props);
    }

    /**
     * Gets number of photos in this album
     * @return count
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets the cover photos url of this album
     * @return cover_photo
     */
    public String getCover_photo() {
        return cover_photo;
    }

    /**
     * Gets created_time
     * @return created_time
     */
    public String getCreated_time() {
        return created_time;
    }

    /**
     * Gets description
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the from User
     * @return from
     */
    public User getFrom() {
        return from;
    }

    /**
     * Gets the link
     * @return link
     */
    public String getLink() {
        return link;
    }

    /**
     * Gets the location
     * @return location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Gets the name
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the privacy
     * @return privacy
     */
    public String getPrivacy() {
        return privacy;
    }

    /**
     * Gets the type
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the updated_time
     * @return updated_time
     */
    public String getUpdated_time() {
        return updated_time;
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
        description = (String) toCopy.get("description");
        location = (String) toCopy.get("location");
        link = (String) toCopy.get("link");
        cover_photo = (String) toCopy.get("cover_photo");
        privacy = (String) toCopy.get("privacy");
        String countStr = (String) toCopy.get("count");
        if (countStr != null) {
            count = Integer.parseInt(countStr);
        }
        type = (String) toCopy.get("type");
        created_time = (String) toCopy.get("created_time");
        updated_time = (String) toCopy.get("updated_time");
    }
}

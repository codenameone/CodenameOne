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

/// This class represents a Facebook Photo Object
/// http://developers.facebook.com/docs/reference/api/album
///
/// @author Chen Fishbein
public class Album extends FBObject {

    private final User from = new User();
    private String description;
    private String location;
    private String link;
    private String cover_photo;
    private String privacy;
    private int count = -1;
    private String type;
    private String created_time;
    private String updated_time;


    /// Empty Contructor
    public Album() {
    }

    /// {@inheritDoc}
    public Album(Hashtable props) {
        super(props);
        init(props);
    }

    /// Gets number of photos in this album
    ///
    /// #### Returns
    ///
    /// count
    public int getCount() {
        return count;
    }

    /// Gets the cover photos url of this album
    ///
    /// #### Returns
    ///
    /// cover_photo
    ///
    /// #### Deprecated
    ///
    /// Use `#getCoverPhoto()`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public String getCover_photo() {
        return getCoverPhoto();
    }

    /// Gets the cover photo URL of this album.
    ///
    /// #### Returns
    ///
    /// cover photo URL
    public String getCoverPhoto() {
        return cover_photo;
    }

    /// Gets created_time
    ///
    /// #### Returns
    ///
    /// created_time
    ///
    /// #### Deprecated
    ///
    /// Use `#getCreatedTime()`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public String getCreated_time() {
        return getCreatedTime();
    }

    /// Gets created time.
    ///
    /// #### Returns
    ///
    /// created time
    public String getCreatedTime() {
        return created_time;
    }

    /// Gets description
    ///
    /// #### Returns
    ///
    /// description
    public String getDescription() {
        return description;
    }

    /// Gets the from User
    ///
    /// #### Returns
    ///
    /// from
    public User getFrom() {
        return from;
    }

    /// Gets the link
    ///
    /// #### Returns
    ///
    /// link
    public String getLink() {
        return link;
    }

    /// Gets the location
    ///
    /// #### Returns
    ///
    /// location
    public String getLocation() {
        return location;
    }

    /// Gets the privacy
    ///
    /// #### Returns
    ///
    /// privacy
    public String getPrivacy() {
        return privacy;
    }

    /// Gets the type
    ///
    /// #### Returns
    ///
    /// type
    public String getType() {
        return type;
    }

    /// Gets the updated_time
    ///
    /// #### Returns
    ///
    /// updated_time
    ///
    /// #### Deprecated
    ///
    /// Use `#getUpdatedTime()`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public String getUpdated_time() {
        return getUpdatedTime();
    }

    /// Gets updated time.
    ///
    /// #### Returns
    ///
    /// updated time
    public String getUpdatedTime() {
        return updated_time;
    }

    /// copies the relevant values from the given hashtable
    ///
    /// #### Parameters
    ///
    /// - `props`: an hashtable to copy from
    @Override
    public void copy(Hashtable props) {
        super.copy(props);
        init(props);
    }

    private void init(Hashtable toCopy) {
        super.copy(toCopy);
        Hashtable f = (Hashtable) toCopy.get("from");
        if (f != null) {
            from.copy(f);
        }
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

    /// {@inheritDoc}
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + from.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (link != null ? link.hashCode() : 0);
        result = 31 * result + (cover_photo != null ? cover_photo.hashCode() : 0);
        result = 31 * result + (privacy != null ? privacy.hashCode() : 0);
        result = 31 * result + count;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (created_time != null ? created_time.hashCode() : 0);
        result = 31 * result + (updated_time != null ? updated_time.hashCode() : 0);
        return result;
    }
}

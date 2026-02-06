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

/// This class represents a Facebook Post Object
/// http://developers.facebook.com/docs/reference/api/post/
///
/// @author Chen Fishbein
public class Post extends FBObject {

    private String type;
    private String attribution;
    private String message;
    private String linkUrl;
    private String linkDescription;
    private String commentsCount;
    private String likes;
    private User from = new User();
    private Vector to;
    private String created_time;
    private String picture;

    /// Empty Contructor
    public Post() {
    }

    /// {@inheritDoc}
    public Post(Hashtable props) {
        super(props);
        init(props);
    }

    /// Gets the from User
    ///
    /// #### Returns
    ///
    /// from User
    public User getFrom() {
        if (from == null) {
            from = new User();
        }
        return from;
    }

    /// Gets the to users
    ///
    /// #### Returns
    ///
    /// Vector of Users
    public Vector getTo() {
        return to;
    }

    /// Get the type
    ///
    /// #### Returns
    ///
    /// the type
    public String getType() {
        return type;
    }

    /// Gets the Attribution
    ///
    /// #### Returns
    ///
    /// the attribution
    public String getAttribution() {
        return attribution;
    }

    /// Gets the message
    ///
    /// #### Returns
    ///
    /// the message
    public String getMessage() {
        return message;
    }

    /// Gets the link Url
    ///
    /// #### Returns
    ///
    /// the linkUrl
    public String getLinkUrl() {
        return linkUrl;
    }

    /// Gets the comments count number
    ///
    /// #### Returns
    ///
    /// the comments count
    public String getCommentsCount() {
        return commentsCount;
    }

    /// Gets the Link Name
    ///
    /// #### Returns
    ///
    /// the linkName
    ///
    /// #### Deprecated
    ///
    /// use getName() instead
    public String getLinkName() {
        return getName();
    }

    /// Gets the linkDescription
    ///
    /// #### Returns
    ///
    /// the linkDescription
    public String getLinkDescription() {
        return linkDescription;
    }

    /// Gets the picture id
    ///
    /// #### Returns
    ///
    /// the picture id
    public String getPicture() {
        return picture;
    }

    // PMD Fix (UnusedPrivateField): Expose the created_time metadata via an accessor.
    public String getCreatedTime() {
        return created_time;
    }

    /// Gets the Link Count
    ///
    /// #### Returns
    ///
    /// the likes count
    public String getLikes() {
        return likes;
    }

    /// {@inheritDoc}
    @Override
    public String toString() {
        return "type = " + type + " post = " + message;
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
        type = (String) toCopy.get("type");
        attribution = (String) toCopy.get("attribution");
        message = (String) toCopy.get("message");
        linkUrl = (String) toCopy.get("link");
        linkDescription = (String) toCopy.get("description");

        Hashtable cmnts = (Hashtable) toCopy.get("comments");
        if (cmnts != null) {
            commentsCount = (String) cmnts.get("count");
        }

        Hashtable f = (Hashtable) toCopy.get("from");
        if (f != null) {
            from.copy(f);
        }
        Hashtable toUsers = (Hashtable) toCopy.get("to");
        if (toUsers != null) {
            Vector toUsersArray = (Vector) toUsers.get("data");
            if (toUsersArray != null) {
                to = new Vector();
                for (int i = 0; i < toUsersArray.size(); i++) {
                    Hashtable u = (Hashtable) toUsersArray.elementAt(i);
                    User toUser = new User();
                    toUser.copy(u);
                    to.addElement(toUser);
                }
            }
        }
        created_time = (String) toCopy.get("created_time");
        picture = (String) toCopy.get("picture");
        Object likesObj = toCopy.get("likes");
        if (likesObj != null) {
            if (likesObj instanceof Hashtable) {
                likes = (String) ((Hashtable) likesObj).get("count");
            } else {
                likes = likesObj.toString();
            }
        }
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
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (attribution != null ? attribution.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (linkUrl != null ? linkUrl.hashCode() : 0);
        result = 31 * result + (linkDescription != null ? linkDescription.hashCode() : 0);
        result = 31 * result + (commentsCount != null ? commentsCount.hashCode() : 0);
        result = 31 * result + (likes != null ? likes.hashCode() : 0);
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (created_time != null ? created_time.hashCode() : 0);
        result = 31 * result + (picture != null ? picture.hashCode() : 0);
        return result;
    }
}

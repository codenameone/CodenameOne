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
 * This class represents a Facebook Post Object
 *  http://developers.facebook.com/docs/reference/api/post/
 * 
 * @author Chen Fishbein
 */
public class Post extends FBObject {

    private String type;
    private String attribution;
    private String message;
    private String linkName;
    private String linkUrl;
    private String linkDescription;
    private String commentsCount;
    private String likes;
    private User from = new User();
    private Vector to;
    private String created_time;
    private String picture;

    /**
     * Empty Contructor
     */
    public Post() {
    }

    /**
     * @inheritDoc
     */
    public Post(Hashtable props) {
        super(props);
        init(props);
    }

    /**
     * Gets the from User
     *
     * @return from User
     */
    public User getFrom() {
        if (from == null) {
            from = new User();
        }
        return from;
    }

    /**
     * Gets the to users
     * 
     * @return Vector of Users
     */
    public Vector getTo() {
        return to;
    }

    /**
     * Get the type
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the Attribution
     *
     * @return the attribution
     */
    public String getAttribution() {
        return attribution;
    }

    /**
     * Gets the message
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    /**
     * Gets the link Url
     *
     * @return the linkUrl
     */
    public String getLinkUrl() {
        return linkUrl;
    }

    /**
     * Gets the comments count number
     *
     * @return the comments count
     */
    public String getCommentsCount() {
        return commentsCount;
    }

    /**
     * Gets the Link Name
     *
     * @return the linkName
     */
    public String getLinkName() {
        return linkName;
    }

    /**
     * Gets the linkDescription
     * @return the linkDescription
     */
    public String getLinkDescription() {
        return linkDescription;
    }

    /**
     *  Gets the picture id
     *
     * @return the picture id
     */
    public String getPicture() {
        return picture;
    }

    /**
     * Gets the Link Count
     *
     * @return the likes count
     */
    public String getLikes() {
        return likes;
    }

     /**
     * @inheritDoc
     */
    public String toString() {
        return "type = " + type + " post = " + message;
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
        type = (String) toCopy.get("type");
        attribution = (String) toCopy.get("attribution");
        message = (String) toCopy.get("message");
        linkName = (String) toCopy.get("name");
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
}

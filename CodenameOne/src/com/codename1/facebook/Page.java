/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
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
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.facebook;

import java.util.Hashtable;

/**
 * This class represents a Facebook Page object
 * https://developers.facebook.com/docs/reference/api/page/
 * @author Chen Fishbein
 */
public class Page extends FBObject {

    private String about;
    private String category;
    private String founded;
    private int likesCount;
    private String link;
    private String username;
    private String website;
    private String coverId;
    private String coverLink;
    
    /**
     * Empty Contructor
     */
    public Page() {
    }

    /**
     * @inheritDoc
     */
    public Page(Hashtable props) {
        super(props);
        init(props);
    }

    /**
     * Simple Getter
     */ 
    public String getAbout() {
        return about;
    }

    /**
     * Simple Getter
     */ 
    public String getCategory() {
        return category;
    }

    /**
     * Simple Getter
     */ 
    public String getCoverId() {
        return coverId;
    }

    /**
     * Simple Getter
     */ 
    public String getCoverLink() {
        return coverLink;
    }

    /**
     * Simple Getter
     */ 
    public String getFounded() {
        return founded;
    }

    /**
     * Simple Getter
     */ 
    public int getLikesCount() {
        return likesCount;
    }

    /**
     * Simple Getter
     */ 
    public String getLink() {
        return link;
    }

    /**
     * Simple Getter
     */ 
    public String getUsername() {
        return username;
    }

    /**
     * Simple Getter
     */ 
    public String getWebsite() {
        return website;
    }

    /**
     * copies the relevant values from the given hashtable
     *
     * @param props an hashtable to copy from
     */
    public void copy(Hashtable props) {
        super.copy(props);
        init(props);
    }

    private void init(Hashtable toCopy) {
        super.copy(toCopy);
        about = (String) toCopy.get("about");
        category = (String) toCopy.get("category");
        link = (String) toCopy.get("link");
        about = (String) toCopy.get("about");
        founded = (String) toCopy.get("founded");
        website = (String) toCopy.get("website");
        String l = (String) toCopy.get("likes");
        if(l != null) {
            likesCount = Integer.parseInt(l);
        } else {
            likesCount = -1;
        }
        username = (String) toCopy.get("username");

        Hashtable cover = (Hashtable) toCopy.get("cover");
        if (cover != null) {
            coverId = (String) cover.get("cover_id");
            coverLink = (String) cover.get("source");
        }
    }
}

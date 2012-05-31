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

import com.codename1.maps.Coord;
import java.util.Hashtable;

/**
 * This class represents a Facebook User objject
 *  http://developers.facebook.com/docs/reference/api/user/
 * @author Chen Fishbein
 */
public class User extends FBObject {

    private String first_name;
    private String last_name;
    private String link;
    private String about;
    private String birthday;
    private String email;
    private String website;
    private String bio;
    private String quotes;
    private String gender;
    private String relationship_status;
    private long timezone;
    private String last_updated;
    private String locale;
    private FBObject location;
    private FBObject hometown;

    /**
     * Empty Contructor
     */
    public User() {
    }

    /**
     * @inheritDoc
     */
    public User(Hashtable props) {
        super(props);
        init(props);
    }

    
    /**
     * @return the first_name
     */
    public String getFirst_name() {
        return first_name;
    }

    /**
     * @return the last_name
     */
    public String getLast_name() {
        return last_name;
    }

    /**
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @return the about
     */
    public String getAbout() {
        return about;
    }

    /**
     * @return the birthday
     */
    public String getBirthday() {
        return birthday;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the website
     */
    public String getWebsite() {
        return website;
    }

    /**
     * @return the bio
     */
    public String getBio() {
        return bio;
    }

    /**
     * @return the quotes
     */
    public String getQuotes() {
        return quotes;
    }

    /**
     * @return the gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * @return the relationship_status
     */
    public String getRelationship_status() {
        return relationship_status;
    }

    /**
     * @return the timezone
     */
    public long getTimezone() {
        return timezone;
    }
    /**
     * @return the last_updated
     */
    public String getLast_updated() {
        return last_updated;
    }

    /**
     * @return the locale
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Gets the user City if available
     * 
     * @return 
     */
    public FBObject getLocation(){
        return location;
    }
    
    /**
     * Gets the user Hometown if available
     * 
     * @return 
     */
    public FBObject getHometown(){
        return hometown;
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
        first_name = (String) toCopy.get("first_name");
        last_name = (String) toCopy.get("last_name");
        link = (String) toCopy.get("link");
        about = (String) toCopy.get("about");
        birthday = (String) toCopy.get("birthday");
        email = (String) toCopy.get("email");
        website = (String) toCopy.get("website");
        bio = (String) toCopy.get("bio");
        gender = (String) toCopy.get("gender");
        relationship_status = (String) toCopy.get("relationship_status");
        //timezone = Long.parseLong((String) toCopy.get("timezone"));
        last_updated = (String) toCopy.get("last_updated");
        locale = (String) toCopy.get("locale");
        Hashtable l = (Hashtable)toCopy.get("location");
        if(l != null){
            location = new FBObject(l);
        }
        Hashtable h = (Hashtable)toCopy.get("hometown");
        if(h != null){
            hometown = new FBObject(h);
        }
    }

}

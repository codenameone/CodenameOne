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

/// This class represents a Facebook User objject
/// http://developers.facebook.com/docs/reference/api/user/
///
/// @author Chen Fishbein
public class User extends FBObject {

    private String username;
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

    /// Empty Contructor
    public User() {
    }

    /// {@inheritDoc}
    public User(Hashtable props) {
        super(props);
        init(props);
    }


    /// #### Returns
    ///
    /// the first_name
    ///
    /// #### Deprecated
    ///
    /// Use `#getFirstName()`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public String getFirst_name() {
        return getFirstName();
    }

    /// #### Returns
    ///
    /// the first name
    public String getFirstName() {
        return first_name;
    }

    /// #### Returns
    ///
    /// the last_name
    ///
    /// #### Deprecated
    ///
    /// Use `#getLastName()`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public String getLast_name() {
        return getLastName();
    }

    /// #### Returns
    ///
    /// the last name
    public String getLastName() {
        return last_name;
    }

    /// #### Returns
    ///
    /// the link
    public String getLink() {
        return link;
    }

    /// #### Returns
    ///
    /// the about
    public String getAbout() {
        return about;
    }

    /// #### Returns
    ///
    /// the birthday
    public String getBirthday() {
        return birthday;
    }

    /// #### Returns
    ///
    /// the email
    public String getEmail() {
        return email;
    }

    /// #### Returns
    ///
    /// the website
    public String getWebsite() {
        return website;
    }

    /// #### Returns
    ///
    /// the bio
    public String getBio() {
        return bio;
    }

    /// #### Returns
    ///
    /// the quotes
    public String getQuotes() {
        return quotes;
    }

    /// #### Returns
    ///
    /// the gender
    public String getGender() {
        return gender;
    }

    /// #### Returns
    ///
    /// the relationship_status
    ///
    /// #### Deprecated
    ///
    /// Use `#getRelationshipStatus()`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public String getRelationship_status() {
        return getRelationshipStatus();
    }

    /// #### Returns
    ///
    /// the relationship status
    public String getRelationshipStatus() {
        return relationship_status;
    }

    /// #### Returns
    ///
    /// the timezone
    public long getTimezone() {
        return timezone;
    }

    /// #### Returns
    ///
    /// the last_updated
    ///
    /// #### Deprecated
    ///
    /// Use `#getLastUpdated()`.
    @Deprecated
    @SuppressWarnings("PMD.MethodNamingConventions")
    public String getLast_updated() {
        return getLastUpdated();
    }

    /// #### Returns
    ///
    /// the last updated
    public String getLastUpdated() {
        return last_updated;
    }

    /// #### Returns
    ///
    /// the locale
    public String getLocale() {
        return locale;
    }

    /// Returns the username
    public String getUsername() {
        return username;
    }

    /// Gets the user City if available
    public FBObject getLocation() {
        return location;
    }

    /// Gets the user Hometown if available
    public FBObject getHometown() {
        return hometown;
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
        username = (String) toCopy.get("username");
        first_name = (String) toCopy.get("first_name");
        last_name = (String) toCopy.get("last_name");
        link = (String) toCopy.get("link");
        about = (String) toCopy.get("about");
        birthday = (String) toCopy.get("birthday");
        email = (String) toCopy.get("email");
        website = (String) toCopy.get("website");
        bio = (String) toCopy.get("bio");
        quotes = (String) toCopy.get("quotes");
        gender = (String) toCopy.get("gender");
        relationship_status = (String) toCopy.get("relationship_status");
        Object tz = toCopy.get("timezone");
        if (tz instanceof Integer) {
            timezone = ((Integer) tz).longValue();
        } else if (tz instanceof Long) {
            timezone = ((Long) tz).longValue();
        } else if (tz instanceof Short) {
            timezone = ((Short) tz).shortValue();
        } else if (tz instanceof Byte) {
            timezone = ((Byte) tz).byteValue();
        } else if (tz instanceof Double) {
            timezone = (long) ((Double) tz).doubleValue();
        } else if (tz instanceof Float) {
            timezone = (long) ((Float) tz).floatValue();
        } else if (tz instanceof String) {
            try {
                timezone = Long.parseLong((String) tz);
            } catch (NumberFormatException ignore) {
                timezone = 0;
            }
        }
        last_updated = (String) toCopy.get("last_updated");
        locale = (String) toCopy.get("locale");
        Hashtable l = (Hashtable) toCopy.get("location");
        if (l != null) {
            location = new FBObject(l);
        }
        Hashtable h = (Hashtable) toCopy.get("hometown");
        if (h != null) {
            hometown = new FBObject(h);
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
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (first_name != null ? first_name.hashCode() : 0);
        result = 31 * result + (last_name != null ? last_name.hashCode() : 0);
        result = 31 * result + (link != null ? link.hashCode() : 0);
        result = 31 * result + (about != null ? about.hashCode() : 0);
        result = 31 * result + (birthday != null ? birthday.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (website != null ? website.hashCode() : 0);
        result = 31 * result + (bio != null ? bio.hashCode() : 0);
        result = 31 * result + (quotes != null ? quotes.hashCode() : 0);
        result = 31 * result + (gender != null ? gender.hashCode() : 0);
        result = 31 * result + (relationship_status != null ? relationship_status.hashCode() : 0);
        result = 31 * result + (int) (timezone ^ (timezone >>> 32));
        result = 31 * result + (last_updated != null ? last_updated.hashCode() : 0);
        result = 31 * result + (locale != null ? locale.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (hometown != null ? hometown.hashCode() : 0);
        return result;
    }
}

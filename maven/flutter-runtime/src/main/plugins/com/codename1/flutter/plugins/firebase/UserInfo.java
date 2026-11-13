/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.firebase;

/// Firebase's `UserInfo`: one identity provider linked to a [User], as
/// `User.providerData` lists them.
public class UserInfo {
    private final String providerId;
    private final String uid;
    private final String email;
    private final String displayName;
    private final String photoURL;
    private final String phoneNumber;

    UserInfo(String providerId, String uid, String email, String displayName, String photoURL,
            String phoneNumber) {
        this.providerId = providerId;
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoURL = photoURL;
        this.phoneNumber = phoneNumber;
    }

    public String providerId() {
        return providerId;
    }

    public String uid() {
        return uid;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public String photoURL() {
        return photoURL;
    }

    public String phoneNumber() {
        return phoneNumber;
    }
}

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
package com.codenameone.developerguide.backend.storage;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.storage.ObjectStorage;

import java.io.IOException;

// tag::backend-storage-presign[]
@Component
public class Avatars {
    private final ObjectStorage storage;

    public Avatars(ObjectStorage storage) {
        this.storage = storage;
    }

    /// A URL the app PUTs the image to, valid for ten minutes.
    public String uploadUrl(long userId) throws IOException {
        return storage.presignPut("avatars", userId + ".png", 600);
    }

    /// A URL any client can show the image from, valid for an hour.
    public String viewUrl(long userId) throws IOException {
        return storage.presignGet("avatars", userId + ".png", 3600);
    }
}
// end::backend-storage-presign[]

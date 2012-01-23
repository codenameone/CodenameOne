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

package com.codename1.io.services;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkEvent;
import com.codename1.io.JSONParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Calls to the Twitter REST API can be performed via this class although currently
 * support for authentication isn't implemented due to the transition to oAuth instead
 * of basic authentication.
 *
 * @author Shai Almog
 */
public class TwitterRESTService extends ConnectionRequest {
    /**
     * The constructor accepts the method to invoke
     *
     * @param method the api method to invoke e.g. "statuses/public_timeline"
     */
    public TwitterRESTService(String method) {
        this(method, "1", false);
    }

    /**
     * The constructor accepts the method to invoke
     *
     * @param method the api method to invoke e.g. "statuses/public_timeline"
     * @param post true for post requests and false for get request
     */
    public TwitterRESTService(String method, boolean post) {
        this(method, "1", post);
    }

    /**
     * The constructor accepts the method to invoke
     *
     * @param method the api method to invoke e.g. "statuses/public_timeline"
     * @param version the API version to send e.g. "1"
     * @param post true for post requests and false for get request
     */
    public TwitterRESTService(String method, String version, boolean post) {
        setPost(post);
        setUrl("http://api.twitter.com/" + version + "/" + method + ".json");
    }


    /**
     * @inheritDoc
     */
    protected void readResponse(InputStream input) throws IOException  {
        InputStreamReader i = new InputStreamReader(input, "UTF-8");
        fireResponseListener(new NetworkEvent(this, new JSONParser().parse(i)));
    }
}

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
package com.codename1.io.rest;

/**
 * This class is used to create the Http RequestBuilder(get/post/head/options/delete/put)
 *
 * @author Chen Fishbein
 */
public class Rest {

    /**
     * Creates a GET request builder
     *
     * @param url The request URL
     * @return RequestBuilder instance
     */
    public static RequestBuilder get(String url) {
        return new RequestBuilder("GET", url);
    }

    /**
     * Creates a HEAD request builder
     *
     * @param url The request URL
     * @return RequestBuilder instance
     */
    public static RequestBuilder head(String url) {
        return new RequestBuilder("HEAD", url);
    }

    /**
     * Creates a PATCH request builder
     *
     * @param url The request URL
     * @return RequestBuilder instance
     */
    public static RequestBuilder patch(String url) {
        return new RequestBuilder("PATCH", url);
    }

    /**
     * Creates a OPTIONS request builder
     *
     * @param url The request URL
     * @return RequestBuilder instance
     */
    public static RequestBuilder options(String url) {
        return new RequestBuilder("OPTIONS", url);
    }

    /**
     * Creates a POST request builder
     *
     * @param url The request URL
     * @return RequestBuilder instance
     */
    public static RequestBuilder post(String url) {
        return new RequestBuilder("POST", url);
    }

    /**
     * Creates a DELETE request builder
     *
     * @param url The request URL
     * @return RequestBuilder instance
     */
    public static RequestBuilder delete(String url) {
        return new RequestBuilder("DELETE", url);
    }

    /**
     * Creates a PUT request builder
     *
     * @param url The request URL
     * @return RequestBuilder instance
     */
    public static RequestBuilder put(String url) {
        return new RequestBuilder("PUT", url);
    }

}

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
package com.codename1.flutter;

/**
 * An {@link ImageProvider} that fetches an image over the network — Flutter's
 * {@code NetworkImage}.
 */
public class NetworkImage extends ImageProvider {

    private final String url;
    private double scale = 1.0;
    private Object headers;

    public NetworkImage(String url) {
        this.url = url;
    }

    /** Named parameter setter for the Dart {@code scale:} parameter. */
    public void scale(double v) {
        this.scale = v;
    }

    /** Named parameter setter for the Dart {@code headers:} parameter. */
    public void headers(Object v) {
        this.headers = v;
    }

    public String getUrl() {
        return url;
    }

    public double getScale() {
        return scale;
    }

    /// The Dart {@code headers:} map, or null. Sent with the request that fetches
    /// the image -- an authenticated image endpoint answers 401 without them.
    public Object getHeaders() {
        return headers;
    }

    @Override
    public String sourceKey() {
        return "url:" + url;
    }
}

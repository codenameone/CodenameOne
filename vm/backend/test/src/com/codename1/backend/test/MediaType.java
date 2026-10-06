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
package com.codename1.backend.test;

/// Media types, for `contentType(...)` and `accept(...)`. Strings, so
/// `contentType(MediaType.APPLICATION_JSON)` reads as it does in Spring.
public final class MediaType {
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_JSON_VALUE = APPLICATION_JSON;
    public static final String APPLICATION_XML = "application/xml";
    public static final String APPLICATION_XML_VALUE = APPLICATION_XML;
    public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String APPLICATION_FORM_URLENCODED_VALUE = APPLICATION_FORM_URLENCODED;
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String APPLICATION_OCTET_STREAM_VALUE = APPLICATION_OCTET_STREAM;
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String MULTIPART_FORM_DATA_VALUE = MULTIPART_FORM_DATA;
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_PLAIN_VALUE = TEXT_PLAIN;
    public static final String TEXT_HTML = "text/html";
    public static final String TEXT_HTML_VALUE = TEXT_HTML;
    public static final String ALL = "*/*";
    public static final String ALL_VALUE = ALL;

    private MediaType() {
    }
}

/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.calendar;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// Provider HTTP response body and selected concurrency/authentication headers.
public final class CalendarHttpResponse {
    private final int statusCode; private final String body; private final Map<String,String> headers;
    public CalendarHttpResponse(int statusCode,String body,Map<String,String>headers){this.statusCode=statusCode;this.body=body;this.headers=Collections.unmodifiableMap(headers==null?new HashMap<String,String>():new HashMap<String,String>(headers));}
    public int getStatusCode(){return statusCode;} public String getBody(){return body;} public String getHeader(String name){return headers.get(name);} public Map<String,String>getHeaders(){return headers;}
}

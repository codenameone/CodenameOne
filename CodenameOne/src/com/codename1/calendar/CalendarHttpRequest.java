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
import java.util.LinkedHashMap;
import java.util.Map;

/// Provider HTTP request. Public so applications and tests may inject a transport.
public final class CalendarHttpRequest {
    private final String method, url;
    private String body;
    private final Map<String,String> headers = new LinkedHashMap<String,String>();
    public CalendarHttpRequest(String method, String url) { this.method = method; this.url = url; }
    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public String getBody() { return body; }
    public CalendarHttpRequest setBody(String v) { body = v; return this; }
    public CalendarHttpRequest header(String k,String v) { headers.put(k,v); return this; }
    public Map<String,String> getHeaders() { return Collections.unmodifiableMap(headers); }
}

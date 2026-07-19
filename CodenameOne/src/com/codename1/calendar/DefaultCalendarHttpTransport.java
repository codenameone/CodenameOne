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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.util.AsyncResource;
import com.codename1.util.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Default asynchronous transport backed by Codename One networking. */
public class DefaultCalendarHttpTransport implements CalendarHttpTransport {
    public AsyncResource<CalendarHttpResponse> execute(final CalendarHttpRequest spec) {
        final AsyncResource<CalendarHttpResponse> out = new AsyncResource<CalendarHttpResponse>();
        ConnectionRequest request = new ConnectionRequest() {
            private final Map<String,String> responseHeaders = new HashMap<String,String>();
            protected void readHeaders(Object connection) throws IOException { capture(connection); }
            protected void readErrorCodeHeaders(Object connection) throws IOException { capture(connection); }
            private void capture(Object c) throws IOException {
                String[] names={"ETag","WWW-Authenticate","Location","Retry-After","Sync-Token"};
                for(String name:names){String value=getHeader(c,name);if(value!=null)responseHeaders.put(name,value);}
            }
            protected void readResponse(InputStream input) throws IOException {
                byte[] bytes=Util.readInputStream(input);String body=StringUtil.newString(bytes);
                out.complete(new CalendarHttpResponse(getResponseCode(),body,responseHeaders));
            }
            protected void handleErrorResponseCode(int code,String message) {
                byte[] bytes=getResponseData();String body=bytes==null?null:StringUtil.newString(bytes);
                out.complete(new CalendarHttpResponse(code,body,responseHeaders));
            }
            protected void handleException(Exception error) { out.error(new CalendarException(CalendarError.NETWORK,error.getMessage(),error)); }
        };
        request.setUrl(spec.getUrl()); request.setPost(!"GET".equals(spec.getMethod()) && !"DELETE".equals(spec.getMethod())); request.setHttpMethod(spec.getMethod());
        for(Map.Entry<String,String> h:spec.getHeaders().entrySet())request.addRequestHeader(h.getKey(),h.getValue());
        if(spec.getBody()!=null)request.setRequestBody(spec.getBody());
        NetworkManager.getInstance().addToQueue(request); return out;
    }
}

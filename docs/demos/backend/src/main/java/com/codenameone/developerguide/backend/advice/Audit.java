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
package com.codenameone.developerguide.backend.advice;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Order;
import com.codename1.backend.web.HandlerInterceptor;
import com.codename1.backend.web.HandlerMethod;

// tag::backend-web-interceptor[]
@Component
@Order(1)
public class Audit implements HandlerInterceptor {
    private final AuditTrail audit = new AuditTrail();

    public HttpServer.Response preHandle(HttpServer.Request request, HandlerMethod handler) {
        return null;                       // null proceeds; a Response answers instead
    }

    public void afterCompletion(HttpServer.Request request, HandlerMethod handler,
                                HttpServer.Response response, Throwable failure) {
        audit.record(handler.getPattern(), failure == null ? "ok" : failure.toString());
    }
}
// end::backend-web-interceptor[]

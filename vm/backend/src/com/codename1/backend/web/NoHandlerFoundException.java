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
package com.codename1.backend.web;

/// What a `@ControllerAdvice` `@ExceptionHandler` receives for a request no
/// route and no static file served, as Spring's `NoHandlerFoundException`.
/// Handle it to answer such paths yourself -- a JSON 404 for an API client, an
/// HTML page for a browser; with no handler for it the server answers its plain
/// 404 as before. The handler is given a null [HandlerMethod], since nothing
/// matched, and an advice restricted by `basePackages` is never asked.
public class NoHandlerFoundException extends RuntimeException {
    private final String httpMethod;
    private final String requestUrl;

    /// @param httpMethod the request's method
    /// @param requestUrl the request's path
    public NoHandlerFoundException(String httpMethod, String requestUrl) {
        super("No endpoint " + httpMethod + " " + requestUrl);
        this.httpMethod = httpMethod;
        this.requestUrl = requestUrl;
    }

    /// @return the request's method
    public String getHttpMethod() {
        return httpMethod;
    }

    /// @return the request's path
    public String getRequestURL() {
        return requestUrl;
    }
}

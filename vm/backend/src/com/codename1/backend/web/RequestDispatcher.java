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

/// The request attributes the server sets when it hands a request to the error
/// page, under the names a servlet container uses, so an error controller
/// ported from Spring Boot reads them unchanged:
///
/// ```java
/// @RequestMapping("/error")
/// public HttpServer.Response error(HttpServer.Request request) {
///     Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
///     Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
///     ...
/// }
/// ```
///
/// A request no route, file or `NoHandlerFoundException` handler answers is
/// dispatched to the application's route for the error path -- `/error`, or
/// `cn1.server.error.path`, empty to turn the dispatch off -- with
/// these set, as Spring Boot forwards it to its `ErrorController`. Its answer is
/// sent with status 404 unless it chose another one itself. With no route at
/// that path the server's own plain-text 404 stands.
public final class RequestDispatcher {
    /// The status the request would have been answered with, an `Integer`.
    public static final String ERROR_STATUS_CODE = "jakarta.servlet.error.status_code";
    /// The path of the request as it arrived, without its query, a `String`.
    public static final String ERROR_REQUEST_URI = "jakarta.servlet.error.request_uri";
    /// A short description of the failure, a `String`.
    public static final String ERROR_MESSAGE = "jakarta.servlet.error.message";
    /// The method of the request as it arrived, a `String`. Not a servlet name:
    /// a forward keeps the method, and an error page that answers differently
    /// for a HEAD can read it here.
    public static final String ERROR_METHOD = "cn1.error.method";

    private RequestDispatcher() {
    }
}

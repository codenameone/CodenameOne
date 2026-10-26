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

import com.codename1.backend.HttpServer;

/// Turns an exception a controller threw into a response.
///
/// The build writes one for each controller with `@ExceptionHandler` methods and
/// for each `@ControllerAdvice` bean; an application rarely implements it
/// directly, but may, and register it as a bean like an advice.
public interface ExceptionResolver {

    /// @param request the request that failed
    /// @param handler the route it matched
    /// @param failure what was thrown
    /// @return the response, or null when this resolver does not handle it
    /// @throws Exception when handling it failed; answered as a server error
    HttpServer.Response resolve(HttpServer.Request request, HandlerMethod handler,
                                Exception failure) throws Exception;
}

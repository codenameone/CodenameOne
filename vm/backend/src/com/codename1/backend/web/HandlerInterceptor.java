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

/// Runs around every controller method, for cross-cutting work that is not
/// security: timing, auditing, tenant selection, request-scoped cleanup.
///
/// Declare one as a bean -- a `@Component` class implementing this interface, or
/// a `@Bean` method returning one -- and the build hands it to every generated
/// router. Several run in `@Order` order, and the ones without an order last in
/// bean-name order.
///
/// #### Where it runs
///
/// 1. The security filter chain: authentication, CSRF, the URL rules. A request
///    it refuses never reaches an interceptor.
/// 2. Route selection. Only a request that matched a controller route is
///    intercepted; a 404 is not.
/// 3. [#preHandle], in order.
/// 4. Binding, validation and the controller method, with its method security.
/// 5. Exception handlers, when 3 or 4 threw.
/// 6. [#postHandle], in reverse order, only when the controller method
///    returned normally.
/// 7. [#afterCompletion], in reverse order, for every interceptor whose
///    [#preHandle] let the request through -- whatever happened after it.
///
/// Because the chain sits inside the security layer, an interceptor cannot
/// admit a request security refused, and the failures security owns --
/// `AccessDeniedException`, `AuthenticationException` and their subclasses,
/// CSRF included -- pass every interceptor and exception handler untouched on
/// their way back to it.
public interface HandlerInterceptor {

    /// Called before binding. Return null to let the request through, or a
    /// response to answer with instead -- the controller method is then not
    /// called, and [#afterCompletion] runs only for the interceptors before this
    /// one.
    ///
    /// @param request the request
    /// @param handler the route it matched
    /// @return null to proceed, or the response to answer with
    /// @throws Exception routed to the exception handlers like the controller's own
    default HttpServer.Response preHandle(HttpServer.Request request, HandlerMethod handler)
            throws Exception {
        return null;
    }

    /// Called after the controller method returned normally; not after a
    /// refusal, and not after an exception, handled or not.
    ///
    /// @param request the request
    /// @param handler the route it matched
    /// @param response the response about to be written
    /// @throws Exception fails the request
    default void postHandle(HttpServer.Request request, HandlerMethod handler,
                            HttpServer.Response response) throws Exception {
    }

    /// Called once the request is finished with, on success, on a refusal by a
    /// later interceptor, on a binding or validation failure and on a failure
    /// nothing handled. This is the place for cleanup. A throw from here is
    /// logged and does not change the response.
    ///
    /// @param request the request
    /// @param handler the route it matched
    /// @param response the response, or null when the request failed unhandled
    /// @param failure what failed and was not handled, or null
    default void afterCompletion(HttpServer.Request request, HandlerMethod handler,
                                 HttpServer.Response response, Throwable failure) {
    }
}

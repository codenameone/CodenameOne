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
import com.codename1.backend.security.AccessDeniedException;
import com.codename1.backend.security.AuthenticationException;
import com.codename1.backend.validation.ValidationException;

import java.util.ArrayList;
import java.util.List;

/// The interceptors and global exception resolvers every generated router runs a
/// matched request through.
///
/// The generated wiring builds one from the application's
/// [HandlerInterceptor] beans and `@ControllerAdvice` beans and hands it to each
/// router. A router whose controller declares no exception handler and whose
/// route validates nothing skips it entirely while it [#isEmpty], so an
/// application with none of these pays one field read per request.
///
/// #### Exception handling precedence
///
/// 1. A failure security owns -- [AccessDeniedException] or
///    [AuthenticationException], CSRF failures included -- and any `Error`
///    are never offered to a handler. They go straight back to the server and
///    the security chain, so a broad `@ExceptionHandler(Exception.class)` cannot
///    turn a refusal into a success.
/// 2. The controller's own `@ExceptionHandler` methods; among those that apply,
///    the one declaring the closest superclass of what was thrown.
/// 3. Each `@ControllerAdvice`, in `@Order` order (unordered ones last, by bean
///    name), each again choosing its closest match. The first that answers wins.
/// 4. A [ValidationException] nothing handled answers 400 with its field errors
///    as JSON.
/// 5. Anything else is rethrown, and the server answers it as before: the
///    status of a `ResponseStatusException`, otherwise a 500 that reveals
///    nothing about the failure.
public final class ControllerHooks implements HttpServer.Handler {
    private static final ControllerHooks NONE =
            new ControllerHooks(new HandlerInterceptor[0], new ExceptionResolver[0]);

    /// What a generated router implements: running route `route` of itself.
    public interface Invocation {
        /// @param route the route's index in the router
        /// @param request the request
        /// @param bound the path variables, or null for a static route
        /// @return the response
        /// @throws Exception whatever binding or the controller threw
        HttpServer.Response proceed(int route, HttpServer.Request request, String[] bound)
                throws Exception;
    }

    private final HandlerInterceptor[] interceptors;
    private final ExceptionResolver[] resolvers;

    /// Null entries are dropped: a conditional bean that was not created is null
    /// in the wiring, and leaving the hole in would fail every request.
    ///
    /// @param interceptors in the order they run
    /// @param resolvers the global resolvers, in the order they are asked
    public ControllerHooks(HandlerInterceptor[] interceptors, ExceptionResolver[] resolvers) {
        this.interceptors = compact(interceptors, new HandlerInterceptor[0]);
        this.resolvers = compact(resolvers, new ExceptionResolver[0]);
    }

    /// No interceptors and no global resolvers.
    ///
    /// @return the shared empty instance
    public static ControllerHooks none() {
        return NONE;
    }

    /// Whether there is nothing to run.
    ///
    /// @return true with no interceptors and no global resolvers
    public boolean isEmpty() {
        return interceptors.length == 0 && resolvers.length == 0;
    }

    private static <T> T[] compact(T[] in, T[] empty) {
        if (in == null) {
            return empty;
        }
        List<T> out = new ArrayList<T>(in.length);
        for (T t : in) {
            if (t != null) {
                out.add(t);
            }
        }
        return out.toArray(empty);
    }

    /// Never a router: always null. The wiring hands these hooks to the server
    /// among its handlers, and the server takes them out of the chain and asks
    /// [#notFound] only once every router and static file has passed.
    ///
    /// @param request the request
    /// @return null
    @Override
    public HttpServer.Response handle(HttpServer.Request request) {
        return null;
    }

    /// Whether any global resolver could answer an unmatched path.
    ///
    /// @return true when a `@ControllerAdvice` or resolver bean exists
    public boolean hasResolvers() {
        return resolvers.length > 0;
    }

    /// The answer for a request nothing served: the global resolvers are offered
    /// a [NoHandlerFoundException], with a null handler. Null when none answers,
    /// which leaves the server's own 404. Security failures and `Error`s are not
    /// asked here, as nothing threw them.
    ///
    /// @param request the request
    /// @return the response, or null
    /// @throws Exception what a resolver threw
    public HttpServer.Response notFound(HttpServer.Request request) throws Exception {
        if (resolvers.length == 0) {
            return null;
        }
        String target = request.getTarget();
        int query = target == null ? -1 : target.indexOf('?');
        NoHandlerFoundException missing = new NoHandlerFoundException(request.getMethod(),
                query < 0 ? target : target.substring(0, query));
        for (ExceptionResolver resolver : resolvers) {
            HttpServer.Response r = resolver.resolve(request, null, missing);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /// Runs one matched route through the interceptors and exception handlers.
    ///
    /// @param request the request
    /// @param handler the route it matched
    /// @param call the router
    /// @param route the route's index in the router
    /// @param bound the path variables, or null
    /// @param local the controller's own exception handlers, or null
    /// @return the response
    /// @throws Exception a failure nothing handled
    public HttpServer.Response invoke(HttpServer.Request request, HandlerMethod handler,
            Invocation call, int route, String[] bound, ExceptionResolver local)
            throws Exception {
        int admitted = 0;
        HttpServer.Response response = null;
        Throwable failure = null;
        try {
            try {
                HttpServer.Response refused = null;
                while (admitted < interceptors.length) {
                    refused = interceptors[admitted].preHandle(request, handler);
                    if (refused != null) {
                        break;
                    }
                    admitted++;
                }
                if (refused != null) {
                    response = refused;
                    return response;
                }
                response = call.proceed(route, request, bound);
            } catch (Exception err) {
                // A handled failure is answered, but it is not the controller's
                // answer: postHandle is for a method that returned normally.
                response = resolve(request, handler, local, err);
                return response;
            }
            for (int i = admitted - 1; i >= 0; i--) {
                interceptors[i].postHandle(request, handler, response);
            }
            return response;
        } catch (Exception err) {
            failure = err;
            response = null;
            throw err;
        } catch (Error err) {
            failure = err;
            response = null;
            throw err;
        } finally {
            for (int i = admitted - 1; i >= 0; i--) {
                try {
                    interceptors[i].afterCompletion(request, handler, response, failure);
                } catch (Exception cleanup) {
                    System.err.println("afterCompletion failed in "
                            + interceptors[i].getClass().getName() + ": " + cleanup);
                }
            }
        }
    }

    /// The precedence the class documentation describes.
    private HttpServer.Response resolve(HttpServer.Request request, HandlerMethod handler,
            ExceptionResolver local, Exception err) throws Exception {
        if (err instanceof AccessDeniedException || err instanceof AuthenticationException) {
            throw err;
        }
        if (local != null) {
            HttpServer.Response r = local.resolve(request, handler, err);
            if (r != null) {
                return r;
            }
        }
        for (ExceptionResolver resolver : resolvers) {
            HttpServer.Response r = resolver.resolve(request, handler, err);
            if (r != null) {
                return r;
            }
        }
        if (err instanceof ValidationException) {
            return ((ValidationException) err).toResponse(request);
        }
        throw err;
    }
}

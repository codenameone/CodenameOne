/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.io;

/** Dart's {@code HttpStatus} constants (the ones apps compare against). */
public final class HttpStatus {
    public static final long continue_ = 100;
    public static final long ok = 200;
    public static final long created = 201;
    public static final long accepted = 202;
    public static final long noContent = 204;
    public static final long movedPermanently = 301;
    public static final long found = 302;
    public static final long seeOther = 303;
    public static final long notModified = 304;
    public static final long temporaryRedirect = 307;
    public static final long permanentRedirect = 308;
    public static final long badRequest = 400;
    public static final long unauthorized = 401;
    public static final long paymentRequired = 402;
    public static final long forbidden = 403;
    public static final long notFound = 404;
    public static final long methodNotAllowed = 405;
    public static final long notAcceptable = 406;
    public static final long requestTimeout = 408;
    public static final long conflict = 409;
    public static final long gone = 410;
    public static final long unprocessableEntity = 422;
    public static final long tooManyRequests = 429;
    public static final long internalServerError = 500;
    public static final long notImplemented = 501;
    public static final long badGateway = 502;
    public static final long serviceUnavailable = 503;
    public static final long gatewayTimeout = 504;

    private HttpStatus() {
    }
}

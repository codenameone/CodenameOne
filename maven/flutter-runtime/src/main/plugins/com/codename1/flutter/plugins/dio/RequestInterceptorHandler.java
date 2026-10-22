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
package com.codename1.flutter.plugins.dio;

/// What an `onRequest` hook calls to continue: [#next] with the (possibly
/// changed) options, [#resolve] to answer without sending, or [#reject] to fail.
public class RequestInterceptorHandler {
    private final Dio.Call call;
    private final int index;
    private boolean used;

    RequestInterceptorHandler(Dio.Call call, int index) {
        this.call = call;
        this.index = index;
    }

    private boolean first() {
        if (used) {
            return false;
        }
        used = true;
        return true;
    }

    public void next(RequestOptions options) {
        if (first()) {
            call.request(index + 1, options);
        }
    }

    public void resolve(Response<?> response) {
        resolve(response, false);
    }

    @SuppressWarnings("unchecked")
    public void resolve(Response<?> response, Boolean callFollowingResponseInterceptor) {
        if (first()) {
            if (callFollowingResponseInterceptor != null && callFollowingResponseInterceptor.booleanValue()) {
                call.response(0, (Response<Object>) response);
            } else {
                call.succeed((Response<Object>) response);
            }
        }
    }

    public void reject(DioException error) {
        reject(error, false);
    }

    public void reject(DioException error, Boolean callFollowingErrorInterceptor) {
        if (first()) {
            if (callFollowingErrorInterceptor != null && callFollowingErrorInterceptor.booleanValue()) {
                call.error(0, error);
            } else {
                call.fail(error);
            }
        }
    }
}

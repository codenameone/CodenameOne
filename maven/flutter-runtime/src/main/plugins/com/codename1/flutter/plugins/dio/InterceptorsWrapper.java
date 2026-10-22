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

import dart.runtime.Funcs;

/// dio's `InterceptorsWrapper`: an Interceptor made of closures. A hook that was
/// not given passes its value straight on.
public class InterceptorsWrapper extends Interceptor {
    private Funcs.VoidFunc2<RequestOptions, RequestInterceptorHandler> onRequest;
    private Funcs.VoidFunc2<Response<Object>, ResponseInterceptorHandler> onResponse;
    private Funcs.VoidFunc2<DioException, ErrorInterceptorHandler> onError;

    public InterceptorsWrapper() {
    }

    public void onRequest(Funcs.VoidFunc2<RequestOptions, RequestInterceptorHandler> v) {
        onRequest = v;
    }

    public void onResponse(Funcs.VoidFunc2<Response<Object>, ResponseInterceptorHandler> v) {
        onResponse = v;
    }

    public void onError(Funcs.VoidFunc2<DioException, ErrorInterceptorHandler> v) {
        onError = v;
    }

    @Override
    public void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
        if (onRequest == null) {
            handler.next(options);
        } else {
            onRequest.call(options, handler);
        }
    }

    @Override
    public void onResponse(Response<Object> response, ResponseInterceptorHandler handler) {
        if (onResponse == null) {
            handler.next(response);
        } else {
            onResponse.call(response, handler);
        }
    }

    @Override
    public void onError(DioException err, ErrorInterceptorHandler handler) {
        if (onError == null) {
            handler.next(err);
        } else {
            onError.call(err, handler);
        }
    }
}

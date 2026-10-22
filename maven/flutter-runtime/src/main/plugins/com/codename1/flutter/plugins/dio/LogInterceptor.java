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

import dart.runtime.DartRuntime;
import dart.runtime.Funcs;

/// dio's `LogInterceptor`: prints each request, response and error through
/// `logPrint` (Codename One's log when none is given).
public class LogInterceptor extends Interceptor {
    private boolean request = true;
    private boolean requestHeader = true;
    private boolean requestBody;
    private boolean responseHeader = true;
    private boolean responseBody;
    private boolean error = true;
    private Funcs.VoidFunc1<Object> logPrint;

    public LogInterceptor() {
    }

    public void request(boolean v) {
        request = v;
    }

    public void requestHeader(boolean v) {
        requestHeader = v;
    }

    public void requestBody(boolean v) {
        requestBody = v;
    }

    public void responseHeader(boolean v) {
        responseHeader = v;
    }

    public void responseBody(boolean v) {
        responseBody = v;
    }

    public void error(boolean v) {
        error = v;
    }

    public void logPrint(Funcs.VoidFunc1<Object> v) {
        logPrint = v;
    }

    private void log(String s) {
        if (logPrint != null) {
            logPrint.call(s);
        } else {
            com.codename1.io.Log.p(s);
        }
    }

    @Override
    public void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
        if (request) {
            log("*** Request ***");
            log("uri: " + options.url());
            log("method: " + options.method());
            if (requestHeader) {
                log("headers: " + DartRuntime.str(options.headers()));
            }
            if (requestBody) {
                log("data: " + DartRuntime.str(options.data()));
            }
        }
        handler.next(options);
    }

    @Override
    public void onResponse(Response<Object> response, ResponseInterceptorHandler handler) {
        log("*** Response ***");
        log("statusCode: " + response.statusCode());
        if (responseHeader) {
            log("headers: " + response.headers());
        }
        if (responseBody) {
            log("data: " + DartRuntime.str(response.data()));
        }
        handler.next(response);
    }

    @Override
    public void onError(DioException err, ErrorInterceptorHandler handler) {
        if (error) {
            log("*** DioException ***: " + err);
        }
        handler.next(err);
    }
}

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

/// dio's `Response<T>`.
public class Response<T> {
    T data;
    Long statusCode;
    String statusMessage;
    Headers headers = new Headers();
    RequestOptions requestOptions;
    dart.core.DartMap<String, Object> extra;

    public Response() {
    }

    public T data() {
        return data;
    }

    public void data(T v) {
        data = v;
    }

    public Long statusCode() {
        return statusCode;
    }

    public void statusCode(Long v) {
        statusCode = v;
    }

    public String statusMessage() {
        return statusMessage;
    }

    public void statusMessage(String v) {
        statusMessage = v;
    }

    public Headers headers() {
        return headers;
    }

    public void headers(Headers v) {
        headers = v == null ? new Headers() : v;
    }

    public RequestOptions requestOptions() {
        return requestOptions;
    }

    public void requestOptions(RequestOptions v) {
        requestOptions = v;
    }

    public dart.core.DartMap<String, Object> extra() {
        if (extra == null) {
            extra = new dart.core.DartMap<String, Object>();
        }
        return extra;
    }

    public dart.core.DartUri realUri() {
        return requestOptions == null ? null : requestOptions.uri();
    }

    @Override
    public String toString() {
        return DartRuntime.str(data);
    }
}

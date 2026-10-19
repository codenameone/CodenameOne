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

import java.util.Map;

import dart.core.DartMap;
import dart.core.Duration;
import dart.runtime.Funcs;

/// The settings BaseOptions, Options and RequestOptions share. A value left null
/// falls through to the next level: request, then the Dio instance's options.
public abstract class OptionsBase {
    String method;
    DartMap<String, Object> headers;
    DartMap<String, Object> extra;
    String contentType;
    ResponseType responseType;
    Duration sendTimeout;
    Duration receiveTimeout;
    Funcs.Func1<Long, Boolean> validateStatus;
    Boolean followRedirects;

    public String method() {
        return method;
    }

    public void method(String v) {
        method = v;
    }

    public DartMap<String, Object> headers() {
        if (headers == null) {
            headers = new DartMap<String, Object>();
        }
        return headers;
    }

    public void headers(Map<String, Object> v) {
        headers = v == null ? null : copy(v);
    }

    public DartMap<String, Object> extra() {
        if (extra == null) {
            extra = new DartMap<String, Object>();
        }
        return extra;
    }

    public void extra(Map<String, Object> v) {
        extra = v == null ? null : copy(v);
    }

    public String contentType() {
        return contentType;
    }

    public void contentType(String v) {
        contentType = v;
    }

    public ResponseType responseType() {
        return responseType;
    }

    public void responseType(ResponseType v) {
        responseType = v;
    }

    public Duration sendTimeout() {
        return sendTimeout;
    }

    public void sendTimeout(Duration v) {
        sendTimeout = v;
    }

    public Duration receiveTimeout() {
        return receiveTimeout;
    }

    public void receiveTimeout(Duration v) {
        receiveTimeout = v;
    }

    public Funcs.Func1<Long, Boolean> validateStatus() {
        return validateStatus;
    }

    public void validateStatus(Funcs.Func1<Long, Boolean> v) {
        validateStatus = v;
    }

    public Boolean followRedirects() {
        return followRedirects;
    }

    public void followRedirects(Boolean v) {
        followRedirects = v;
    }

    static DartMap<String, Object> copy(Map<String, Object> m) {
        DartMap<String, Object> out = new DartMap<String, Object>();
        if (m != null) {
            out.putAll(m);
        }
        return out;
    }
}

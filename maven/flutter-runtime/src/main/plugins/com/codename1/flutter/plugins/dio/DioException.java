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

/// dio's `DioException` (and the deprecated `DioError`, which names the same class).
public class DioException extends RuntimeException {
    RequestOptions requestOptions;
    Response<Object> response;
    DioExceptionType type = DioExceptionType.unknown;
    Object error;
    String message;
    Object stackTrace;

    public DioException() {
        super();
    }

    public RequestOptions requestOptions() {
        return requestOptions;
    }

    public void requestOptions(RequestOptions v) {
        requestOptions = v;
    }

    public Response<Object> response() {
        return response;
    }

    @SuppressWarnings("unchecked")
    public void response(Response<?> v) {
        response = (Response<Object>) v;
    }

    public DioExceptionType type() {
        return type;
    }

    public void type(DioExceptionType v) {
        type = v == null ? DioExceptionType.unknown : v;
    }

    public Object error() {
        return error;
    }

    public void error(Object v) {
        error = v;
    }

    public String message() {
        return message;
    }

    public void message(String v) {
        message = v;
    }

    public Object stackTrace() {
        return stackTrace;
    }

    public void stackTrace(Object v) {
        stackTrace = v;
    }

    @Override
    public String getMessage() {
        return message != null ? message : (error != null ? String.valueOf(error) : type.name());
    }

    @Override
    public String toString() {
        return "DioException [" + type.name() + "]: " + getMessage();
    }

    static DioException of(RequestOptions ro, DioExceptionType type, String message, Object error,
            Response<?> response) {
        DioException e = new DioException();
        e.requestOptions = ro;
        e.type(type);
        e.message = message;
        e.error = error;
        e.response(response);
        return e;
    }
}

/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.io.rest;

/// Builds a [Response] for a test.
///
/// It lives in `com.codename1.io.rest` because [Response]'s constructor is package private and
/// there is no factory for one: the class exists to be handed to a REST callback, and only the
/// generated client ever makes one. A test that wants to assert how a status code is REPORTED --
/// which is what issue #5832 turned out to be about -- has no other way to produce the input.
public final class Responses {

    private Responses() {
    }

    public static <T> Response<T> of(int code, T data) {
        return new Response<T>(code, data, null);
    }

    public static <T> Response<T> failure(int code, String message) {
        return new Response<T>(code, null, message);
    }
}

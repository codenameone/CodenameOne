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

/// dio's `BaseOptions`: what every request of one Dio instance starts from.
public class BaseOptions extends OptionsBase {
    String baseUrl = "";
    Duration connectTimeout;
    DartMap<String, Object> queryParameters;

    public String baseUrl() {
        return baseUrl;
    }

    public void baseUrl(String v) {
        baseUrl = v == null ? "" : v;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public void connectTimeout(Duration v) {
        connectTimeout = v;
    }

    public DartMap<String, Object> queryParameters() {
        if (queryParameters == null) {
            queryParameters = new DartMap<String, Object>();
        }
        return queryParameters;
    }

    public void queryParameters(Map<String, Object> v) {
        queryParameters = v == null ? null : copy(v);
    }
}

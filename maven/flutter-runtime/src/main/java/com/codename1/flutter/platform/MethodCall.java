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
package com.codename1.flutter.platform;

/**
 * Mirrors Android's {@code io.flutter.plugin.common.MethodCall}: the method
 * name and argument a {@link MethodChannel.MethodCallHandler} receives.
 *
 * <p>This is the SAME object the Dart side constructs: the transpiled
 * {@code package:flutter/services.dart} stub names this exact class via
 * {@code @JavaName}, so there is no serialize/deserialize boundary to cross
 * in-process. One consequence of being shared with Dart: {@link #method()}
 * and {@link #arguments()} are no-arg accessor methods rather than Android's
 * public final fields, because a Dart property read ({@code call.method})
 * always lowers to a no-arg method call in the generated Java -- porting
 * code written against the real field needs {@code call.method()} instead.</p>
 */
public final class MethodCall {

    private final String method;
    private final Object arguments;

    public MethodCall(String method, Object arguments) {
        this.method = method;
        this.arguments = arguments;
    }

    public String method() {
        return method;
    }

    public Object arguments() {
        return arguments;
    }
}

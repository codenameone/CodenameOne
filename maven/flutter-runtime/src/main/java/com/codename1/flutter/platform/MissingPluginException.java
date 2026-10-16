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
 * Mirrors Android's {@code io.flutter.plugin.common.MissingPluginException}:
 * the error a Dart {@code invokeMethod} call completes with when no
 * {@link MethodChannel.MethodCallHandler} is registered for the channel (or
 * when one is registered but replies via
 * {@link MethodChannel.Result#notImplemented()}).
 *
 * <p>The real Dart constructor takes one optional POSITIONAL parameter
 * ({@code message}), so the stub maps it straight to a Java constructor
 * argument rather than a setter -- unlike {@link PlatformException}'s named
 * parameters.</p>
 */
public class MissingPluginException extends RuntimeException {

    public MissingPluginException() {
        super();
    }

    public MissingPluginException(String message) {
        super();
        this.message = message;
    }

    // RuntimeException's own message storage can only be set from a
    // constructor taking (message, cause, suppression, stack) -- not in
    // ParparVM's JavaAPI or CLDC11 -- so this keeps its own field instead.
    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    /** The Dart-visible accessor (a Dart property read lowers to a no-arg method call). */
    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return "MissingPluginException(" + message + ")";
    }
}

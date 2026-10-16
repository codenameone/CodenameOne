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
 * Mirrors Android's {@code io.flutter.plugin.common.PlatformException}: the
 * error a {@link MethodChannel.Result#error} reply completes a Dart
 * {@code invokeMethod} call with.
 *
 * <p>The real Dart {@code PlatformException} constructor is entirely named
 * parameters ({@code required code}, {@code message}, {@code details},
 * {@code stacktrace}), which the stub convention turns into an allocate-then-
 * setters call (a no-arg constructor plus void setter methods of the same
 * name) rather than a single constructor call -- so {@link #code}/
 * {@link #message}/{@link #details} are mutable fields behind getter/setter
 * pairs, not Android's public final ones. {@link #getMessage()} is
 * overridden to read the mutable field, since
 * {@code RuntimeException}'s own message storage can only be set once, from
 * a constructor.</p>
 */
public class PlatformException extends RuntimeException {

    private String code;
    private String message;
    private Object details;
    private String stacktrace;

    /** The no-arg constructor the Dart named-parameter stub constructs through. */
    public PlatformException() {
        super();
    }

    /** Convenience constructor for hand-written Java (platform/test) code. */
    public PlatformException(String code, String message, Object details) {
        super();
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public void code(String v) {
        this.code = v;
    }

    public String code() {
        return code;
    }

    public void message(String v) {
        this.message = v;
    }

    public String message() {
        return message;
    }

    public void details(Object v) {
        this.details = v;
    }

    public Object details() {
        return details;
    }

    public void stacktrace(String v) {
        this.stacktrace = v;
    }

    public String stacktrace() {
        return stacktrace;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "PlatformException(" + code + ", " + message + ", " + details + ")";
    }
}

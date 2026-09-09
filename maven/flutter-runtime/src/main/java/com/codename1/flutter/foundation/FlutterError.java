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
package com.codename1.flutter.foundation;

/**
 * The error type the Flutter framework (and app assertions) throw — Flutter's
 * {@code FlutterError}. Modelled as a {@link RuntimeException} so transpiled
 * {@code throw FlutterError(...)} statements compile and propagate like any Dart
 * throw. {@link #reportError(Object)} routes a caught error to the current
 * handler; this pass logs it.
 */
public class FlutterError extends RuntimeException {

    public FlutterError(String message) {
        super(message);
    }

    /** Dart's {@code FlutterError.reportError(details)}. */
    public static void reportError(Object details) {
        if (details != null) {
            System.err.println("FlutterError.reportError: " + details);
        }
    }

    @Override
    public String toString() {
        return "FlutterError: " + getMessage();
    }
}

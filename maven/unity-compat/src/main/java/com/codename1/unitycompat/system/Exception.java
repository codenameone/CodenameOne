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
package com.codename1.unitycompat.system;

/// `System.Exception`, and the root of everything translated C# can throw or
/// catch. It is a `RuntimeException` because C# has no checked exceptions.
///
/// Exceptions the VM raises on its own -- a null dereference, an array index
/// out of range -- are not of this type; [Interop#wrap] turns them into the
/// .NET exception a C# `catch` expects before any clause is tested.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Exception extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String message;
    private final Exception inner;

    public Exception() {
        this(null, null);
    }

    public Exception(String message) {
        this(message, null);
    }

    public Exception(String message, Exception inner) {
        super(message);
        this.message = message;
        this.inner = inner;
    }

    public String get_Message() {
        return message != null ? message : "Exception of type '" + getClass().getName() + "' was thrown.";
    }

    public Exception get_InnerException() {
        return inner;
    }

    @Override
    public String getMessage() {
        return get_Message();
    }
}

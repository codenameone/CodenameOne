/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.validation;

/// One constraint one value broke.
public final class FieldError implements java.io.Serializable {
    private final String field;
    private final String code;
    private final String message;

    /// @param field the path of the value, such as `address.zip` or `items[2].name`;
    /// for a request parameter, its name
    /// @param code the constraint, such as `NotBlank` or `Size`
    /// @param message what to tell the caller
    public FieldError(String field, String code, String message) {
        this.field = field == null ? "" : field;
        this.code = code == null ? "" : code;
        this.message = message == null ? "" : message;
    }

    /// The path of the value that broke the constraint. Stable: it is built from
    /// the JSON property names and parameter names, never from Java internals.
    public String getField() {
        return field;
    }

    /// The constraint's simple name, such as `NotBlank`.
    public String getCode() {
        return code;
    }

    /// The message for the caller.
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return field + ": " + message;
    }
}

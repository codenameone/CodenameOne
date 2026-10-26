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

import java.util.ArrayList;
import java.util.List;

/// Collects the [FieldError]s of one request. The generated validation code
/// calls it; an application has no reason to.
public final class Violations {
    private List<FieldError> errors;

    /// Records one failure.
    ///
    /// @param field the path
    /// @param code the constraint's simple name
    /// @param message the declared message, or empty for `fallback`
    /// @param fallback the default message
    public void add(String field, String code, String message, String fallback) {
        if (errors == null) {
            errors = new ArrayList<FieldError>();
        }
        errors.add(new FieldError(field, code,
                message == null || message.length() == 0 ? fallback : message));
    }

    /// What was recorded so far.
    ///
    /// @return the errors, possibly empty
    public List<FieldError> errors() {
        return errors == null ? new ArrayList<FieldError>() : errors;
    }

    /// Throws a [ValidationException] when anything was recorded.
    public void check() {
        if (errors != null) {
            throw new ValidationException(errors);
        }
    }

    /// `prefix.name`, or `name` at the root.
    ///
    /// @param prefix the parent path, empty at the root
    /// @param name the property
    /// @return the path
    public static String path(String prefix, String name) {
        return prefix.length() == 0 ? name : prefix + "." + name;
    }

    /// `prefix[index]`.
    ///
    /// @param prefix the collection's path
    /// @param index the element
    /// @return the path
    public static String index(String prefix, int index) {
        return prefix + "[" + index + "]";
    }

    /// Whether `s` is null or only whitespace.
    ///
    /// @param s the value
    /// @return true when blank
    public static boolean isBlank(CharSequence s) {
        if (s == null) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}

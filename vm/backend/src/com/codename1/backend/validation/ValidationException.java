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

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A request whose input broke its declared constraints.
///
/// Thrown by the generated router before the controller method runs, so nothing
/// the method does happens for invalid input. An `@ExceptionHandler` for it may
/// shape the answer; unhandled, the request is answered 400 with
///
/// ```json
/// {"status":400,"error":"Bad Request",
///  "errors":[{"field":"address.zip","code":"Size","message":"size must be between 5 and 5"}]}
/// ```
///
/// An application may throw it too, for a check no annotation expresses.
public class ValidationException extends ResponseStatusException {
    private final FieldError[] errors;

    /// @param errors what failed; at least one
    public ValidationException(List<FieldError> errors) {
        super(400, "validation failed");
        this.errors = errors.toArray(new FieldError[errors.size()]);
    }

    /// Every constraint that failed, in the order the checks ran.
    ///
    /// @return the errors, never empty
    public List<FieldError> getFieldErrors() {
        return Collections.unmodifiableList(Arrays.asList(errors));
    }

    /// The errors for one path.
    ///
    /// @param field the path
    /// @return its errors, possibly empty
    public List<FieldError> getFieldErrors(String field) {
        List<FieldError> out = new ArrayList<FieldError>();
        for (FieldError e : errors) {
            if (e.getField().equals(field)) {
                out.add(e);
            }
        }
        return out;
    }

    /// The default answer: 400 with the errors as JSON.
    ///
    /// @param request the request being answered
    /// @return the response
    public HttpServer.Response toResponse(HttpServer.Request request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("status", Integer.valueOf(400));
        body.put("error", "Bad Request");
        List<Object> list = new ArrayList<Object>();
        for (FieldError e : errors) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("field", e.getField());
            m.put("code", e.getCode());
            m.put("message", e.getMessage());
            list.add(m);
        }
        body.put("errors", list);
        return request.respondJson(400, body);
    }
}

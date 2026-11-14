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
package com.codename1.backend.mongo;

import java.io.IOException;
import java.util.Map;

/// A command MongoDB answered with `ok: 0`, or a write it reported failed.
///
/// [#getCode()] is MongoDB's numeric error code (11000 for a duplicate key) and
/// [#getCodeName()] its name, when the server sent one.
public class MongoException extends IOException {
    private static final long serialVersionUID = 1L;
    /// The code of a unique index violation.
    public static final int DUPLICATE_KEY = 11000;

    private final int code;
    private final String codeName;

    /// A failure with a server code, its name (or null) and the message.
    public MongoException(int code, String codeName, String message) {
        super(message);
        this.code = code;
        this.codeName = codeName;
    }

    static MongoException from(Map reply) {
        Object c = reply.get("code");
        Object name = reply.get("codeName");
        Object msg = reply.get("errmsg");
        return new MongoException(c instanceof Number ? ((Number) c).intValue() : 0,
                name == null ? null : String.valueOf(name),
                msg == null ? "MongoDB refused the command" : String.valueOf(msg));
    }

    /// MongoDB's error code, or 0 when it sent none.
    public int getCode() {
        return code;
    }

    /// The name of the code, such as `DuplicateKey`, or null.
    public String getCodeName() {
        return codeName;
    }

    /// Whether this failure is a unique index violation.
    public boolean isDuplicateKey() {
        return code == DUPLICATE_KEY;
    }
}

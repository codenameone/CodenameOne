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
package com.codename1.backend.dynamo;

import java.io.IOException;

/// A request DynamoDB refused.
///
/// [#getType()] is the part of the `__type` field after the `#`, for example
/// `ConditionalCheckFailedException` or `ResourceNotFoundException`, so a caller
/// can tell a failed condition from a missing table without parsing the message.
public class DynamoDbException extends IOException {
    private static final long serialVersionUID = 1L;
    private final String type;
    private final int status;

    /// A refusal with DynamoDB's error type, its message and the HTTP status.
    public DynamoDbException(String type, String message, int status) {
        super(type + ": " + message);
        this.type = type;
        this.status = status;
    }

    /// The error type, without the namespace prefix DynamoDB sends.
    public String getType() {
        return type;
    }

    /// The HTTP status of the response.
    public int getStatus() {
        return status;
    }

    /// Whether the request failed only because a condition expression was false.
    public boolean isConditionalCheckFailed() {
        return "ConditionalCheckFailedException".equals(type);
    }
}

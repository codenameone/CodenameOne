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
package com.codename1.backend.security.ldap;

import java.io.IOException;

/// An LDAP operation the directory refused, with its result code (RFC 4511
/// section 4.1.9) and the server's diagnostic message.
public class LdapException extends IOException {
    public static final int SUCCESS = 0;
    public static final int OPERATIONS_ERROR = 1;
    public static final int PROTOCOL_ERROR = 2;
    public static final int SIZE_LIMIT_EXCEEDED = 4;
    public static final int STRONGER_AUTH_REQUIRED = 8;
    public static final int NO_SUCH_OBJECT = 32;
    public static final int INVALID_DN_SYNTAX = 34;
    public static final int INAPPROPRIATE_AUTHENTICATION = 48;
    public static final int INVALID_CREDENTIALS = 49;
    public static final int INSUFFICIENT_ACCESS_RIGHTS = 50;
    public static final int BUSY = 51;
    public static final int UNAVAILABLE = 52;
    public static final int UNWILLING_TO_PERFORM = 53;

    private final int resultCode;
    private final String diagnosticMessage;

    public LdapException(int resultCode, String message, String diagnosticMessage) {
        super(message);
        this.resultCode = resultCode;
        this.diagnosticMessage = diagnosticMessage == null ? "" : diagnosticMessage;
    }

    public int getResultCode() {
        return resultCode;
    }

    /// What the server said about it; Active Directory puts its sub-error
    /// code here (`data 52e`, `data 775`).
    public String getDiagnosticMessage() {
        return diagnosticMessage;
    }
}

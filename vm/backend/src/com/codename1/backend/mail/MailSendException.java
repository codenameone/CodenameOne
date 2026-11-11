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
package com.codename1.backend.mail;

/// A mail the server refused, with the reply it gave.
public class MailSendException extends MailException {
    private final int code;
    private final boolean permanent;

    /// - `code`: the three-digit SMTP reply code, an HTTP status from a
    ///   provider's API, or 0 when there was none
    /// - `permanent`: whether the server will give the same answer next time.
    ///   The transport decides: an SMTP 4xx means "later" and an HTTP 4xx
    ///   mostly means "never", so the number alone does not say.
    public MailSendException(String message, int code, boolean permanent) {
        super(message);
        this.code = code;
        this.permanent = permanent;
    }

    /// The server's reply code, or 0.
    public int getCode() {
        return code;
    }

    @Override
    public boolean isPermanent() {
        return permanent;
    }
}

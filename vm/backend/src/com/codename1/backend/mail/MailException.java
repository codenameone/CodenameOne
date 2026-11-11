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

import java.io.IOException;

/// A mail that could not be sent: the server could not be reached, refused
/// the sender, or refused the message.
public class MailException extends IOException {
    /// A failure with a description of what was being done.
    public MailException(String message) {
        super(message);
    }

    /// A failure caused by another.
    public MailException(String message, Throwable cause) {
        super(message, cause);
    }

    /// Whether sending the same mail again can never succeed: the server
    /// answered with a permanent refusal. A failure that is not permanent --
    /// a timeout, a dropped connection, a "try again later" -- is retried.
    public boolean isPermanent() {
        return false;
    }
}

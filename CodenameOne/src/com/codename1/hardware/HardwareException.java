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
package com.codename1.hardware;

import java.io.IOException;

/// Thrown when a hardware bus or pin cannot be opened or an I/O operation on
/// it fails: the device node is absent, the process lacks permission for it,
/// the line is already claimed, or the peripheral did not acknowledge.
///
/// [#getErrno()] carries the operating system error number when the port has
/// one (Linux `errno`), and `0` otherwise, so code can tell "no such device"
/// from "permission denied" without parsing the message.
@com.codename1.impl.SharedWithBackend
public class HardwareException extends IOException {

    private final int errno;

    /// Creates an exception with a message and no operating system error.
    public HardwareException(String message) {
        this(message, 0);
    }

    /// Creates an exception with a message and the operating system error
    /// number that caused it.
    public HardwareException(String message, int errno) {
        super(message);
        this.errno = errno;
    }

    /// The operating system error number behind this failure, or `0` when the
    /// port did not report one.
    public int getErrno() {
        return errno;
    }
}

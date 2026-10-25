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
package javafx.fxml;

import java.io.IOException;

/// Thrown when an FXML document cannot be loaded.
public class LoadException extends IOException {

    /// Creates an exception without a message.
    public LoadException() {
        super();
    }

    /// Creates an exception with a message.
    public LoadException(String message) {
        super(message);
    }

    /// Creates an exception with a cause.
    public LoadException(Throwable cause) {
        super(cause == null ? null : cause.toString());
        if (cause != null) {
            initCause(cause);
        }
    }

    /// Creates an exception with a message and a cause.
    public LoadException(String message, Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
    }
}

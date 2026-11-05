/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.io;

/// Dart's {@code OSError}: the operating system's message and error code behind a
/// failed I/O call. {@link #noErrorCode} (-1) when there is no code.
public class OSError {
    public static final long noErrorCode = -1;

    private final String message;
    private final long errorCode;

    public OSError() {
        this("", noErrorCode);
    }

    public OSError(String message) {
        this(message, noErrorCode);
    }

    public OSError(String message, long errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }

    public String message() {
        return message;
    }

    public long errorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("OS Error");
        if (message.length() > 0) {
            sb.append(": ").append(message);
            if (errorCode != noErrorCode) {
                sb.append(", errno = ").append(errorCode);
            }
        } else if (errorCode != noErrorCode) {
            sb.append(": errno = ").append(errorCode);
        }
        return sb.toString();
    }
}

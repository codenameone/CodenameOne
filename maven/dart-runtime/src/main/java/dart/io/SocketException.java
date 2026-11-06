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

/** Dart's {@code SocketException}: the connection could not be made or was lost. */
public class SocketException extends IOException {
    private static final long serialVersionUID = 1L;

    private OSError osError;
    private InternetAddress address;
    private Long port;

    /// For a program class that `implements SocketException` and so extends it.
    protected SocketException() {
        this("");
    }

    public SocketException(String message) {
        this(message, null, null, null);
    }

    /// {@code SocketException(message, {osError, address, port})}: the named
    /// arguments in their declared order.
    public SocketException(String message, OSError osError, InternetAddress address, Long port) {
        super(message);
        this.osError = osError;
        this.address = address;
        this.port = port;
    }

    public static SocketException closed() {
        return new SocketException("Socket has been closed");
    }

    /// Named constructor arguments arrive as these setter calls on the new instance.
    public void osError(OSError v) {
        osError = v;
    }

    public void address(InternetAddress v) {
        address = v;
    }

    public void port(Long v) {
        port = v;
    }

    public OSError osError() {
        return osError;
    }

    public InternetAddress address() {
        return address;
    }

    public Long port() {
        return port;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SocketException");
        if (getMessage() != null && getMessage().length() > 0) {
            sb.append(": ").append(getMessage());
            if (osError != null) {
                sb.append(" (").append(osError).append(')');
            }
        } else if (osError != null) {
            sb.append(": ").append(osError);
        }
        if (address != null) {
            sb.append(", address = ").append(address.host());
        }
        if (port != null) {
            sb.append(", port = ").append(port);
        }
        return sb.toString();
    }
}

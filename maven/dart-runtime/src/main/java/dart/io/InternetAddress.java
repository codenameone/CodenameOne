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

/// Dart's {@code InternetAddress}, as far as a failure reports one: the numeric
/// address and the host it was looked up for. Nothing here resolves names; a
/// connection goes through the port's network layer, not a socket of ours.
public class InternetAddress {
    private final String address;
    private final String host;

    public InternetAddress(String address) {
        this(address, null);
    }

    public InternetAddress(String address, String host) {
        this.address = address;
        this.host = host != null ? host : address;
    }

    public String address() {
        return address;
    }

    public String host() {
        return host;
    }

    @Override
    public String toString() {
        return "InternetAddress('" + address + "')";
    }
}

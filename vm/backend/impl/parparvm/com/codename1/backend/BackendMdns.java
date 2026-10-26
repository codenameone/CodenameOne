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
package com.codename1.backend;

import com.codename1.impl.MdnsEngine;

/// The backend's multicast DNS socket: the C is `cn1_mdns_impl.h`, shared with
/// the Linux port and wrapped in `cn1_backend_mdns.c`. Built for a host with
/// no POSIX sockets the socket cannot open, and [Discovery] reports that to
/// the listener.
final class BackendMdns extends MdnsEngine {

    @Override
    protected int rawOpen() {
        return nOpen();
    }

    private static native int nOpen();

    @Override
    protected int rawSend(int handle, byte[] packet, int length) {
        return nSend(handle, packet, length);
    }

    private static native int nSend(int fd, byte[] packet, int length);

    @Override
    protected int rawReceive(int handle, byte[] buffer, int timeoutMs) {
        return nReceive(handle, buffer, timeoutMs);
    }

    private static native int nReceive(int fd, byte[] buffer, int timeoutMs);

    @Override
    protected String rawHostInfo() {
        return nHostInfo();
    }

    private static native String nHostInfo();

    @Override
    protected void rawClose(int handle) {
        nClose(handle);
    }

    private static native void nClose(int fd);
}

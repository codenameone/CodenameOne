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
package com.codename1.impl.linux;

import com.codename1.impl.MdnsEngine;

/// The Linux port's [MdnsEngine]: the socket natives are in
/// `cn1_linux_mdns.c`, shared with the backend through `cn1_mdns_impl.h`. It
/// needs no avahi, so a Buildroot image without one still advertises itself.
final class LinuxMdns extends MdnsEngine {

    @Override
    protected int rawOpen() {
        return LinuxNative.mdnsOpen();
    }

    @Override
    protected int rawSend(int handle, byte[] packet, int length) {
        return LinuxNative.mdnsSend(handle, packet, length);
    }

    @Override
    protected int rawReceive(int handle, byte[] buffer, int timeoutMs) {
        return LinuxNative.mdnsReceive(handle, buffer, timeoutMs);
    }

    @Override
    protected String rawHostInfo() {
        return LinuxNative.mdnsHostInfo();
    }

    @Override
    protected void rawClose(int handle) {
        LinuxNative.mdnsClose(handle);
    }
}

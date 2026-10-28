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

/*
 * The multicast DNS socket under com.codename1.impl.linux.LinuxMdns. The
 * protocol is com.codename1.impl.MdnsEngine; the socket is shared with the
 * backend in cn1_mdns_impl.h. libc only, so desktop and embedded alike.
 */

#include "cn1_mdns_impl.h"

JAVA_INT com_codename1_impl_linux_LinuxNative_mdnsOpen___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1MdnsImpl_open(threadStateData);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_mdnsSend___int_byte_1ARRAY_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT packet, JAVA_INT length) {
    return cn1MdnsImpl_send(threadStateData, fd, packet, length);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_mdnsReceive___int_byte_1ARRAY_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT buffer, JAVA_INT timeoutMs) {
    return cn1MdnsImpl_receive(threadStateData, fd, buffer, timeoutMs);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_mdnsHostInfo___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    return cn1MdnsImpl_hostInfo(threadStateData);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mdnsClose___int(CODENAME_ONE_THREAD_STATE, JAVA_INT fd) {
    cn1MdnsImpl_close(threadStateData, fd);
}

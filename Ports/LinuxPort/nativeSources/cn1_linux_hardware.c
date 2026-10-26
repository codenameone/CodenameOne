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
 * The Linux port's hardware natives (com.codename1.impl.linux.LinuxNative).
 * Each forwards to the shared body in cn1_hardware_impl.h, which the headless
 * backend uses too; see that header for what they do.
 */

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include "cn1_hardware_impl.h"

JAVA_INT com_codename1_impl_linux_LinuxNative_hwGpioOpen___java_lang_String_int_boolean_boolean_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT chip, JAVA_INT line, JAVA_BOOLEAN output, JAVA_BOOLEAN high, JAVA_INT bias, JAVA_INT edges) {
    return cn1HwImpl_hwGpioOpen(threadStateData, chip, line, output, high, bias, edges);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwGpioRead___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h) {
    return cn1HwImpl_hwGpioRead(threadStateData, h);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwGpioWrite___int_boolean_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_BOOLEAN high) {
    return cn1HwImpl_hwGpioWrite(threadStateData, h, high);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwGpioWaitEdge___int_int_long_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_INT timeoutMs, JAVA_OBJECT tsOut) {
    return cn1HwImpl_hwGpioWaitEdge(threadStateData, h, timeoutMs, tsOut);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwI2cOpen___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT bus, JAVA_INT address) {
    return cn1HwImpl_hwI2cOpen(threadStateData, bus, address);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwI2cTransfer___int_byte_1ARRAY_int_byte_1ARRAY_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT w, JAVA_INT wl, JAVA_OBJECT rd, JAVA_INT rl) {
    return cn1HwImpl_hwI2cTransfer(threadStateData, h, w, wl, rd, rl);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwSpiOpen___int_int_int_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT bus, JAVA_INT cs, JAVA_INT mode, JAVA_INT speed, JAVA_INT bits) {
    return cn1HwImpl_hwSpiOpen(threadStateData, bus, cs, mode, speed, bits);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwSpiTransfer___int_byte_1ARRAY_byte_1ARRAY_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT tx, JAVA_OBJECT rx, JAVA_INT len) {
    return cn1HwImpl_hwSpiTransfer(threadStateData, h, tx, rx, len);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwSerialOpen___java_lang_String_int_int_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT dev, JAVA_INT baud, JAVA_INT dataBits, JAVA_INT parity, JAVA_INT stopBits) {
    return cn1HwImpl_hwSerialOpen(threadStateData, dev, baud, dataBits, parity, stopBits);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwSerialRead___int_byte_1ARRAY_int_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT buf, JAVA_INT off, JAVA_INT len, JAVA_INT timeoutMs) {
    return cn1HwImpl_hwSerialRead(threadStateData, h, buf, off, len, timeoutMs);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwSerialWrite___int_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT buf, JAVA_INT off, JAVA_INT len) {
    return cn1HwImpl_hwSerialWrite(threadStateData, h, buf, off, len);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwPwmOpen___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT chip, JAVA_INT channel) {
    return cn1HwImpl_hwPwmOpen(threadStateData, chip, channel);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwPwmConfigure___int_long_long_boolean_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_LONG period, JAVA_LONG duty, JAVA_BOOLEAN enabled) {
    return cn1HwImpl_hwPwmConfigure(threadStateData, h, period, duty, enabled);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwCanOpen___java_lang_String_R_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT iface) {
    return cn1HwImpl_hwCanOpen(threadStateData, iface);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwCanSend___int_int_boolean_byte_1ARRAY_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_INT id, JAVA_BOOLEAN ext, JAVA_OBJECT data, JAVA_INT len) {
    return cn1HwImpl_hwCanSend(threadStateData, h, id, ext, data, len);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwCanReceive___int_int_1ARRAY_byte_1ARRAY_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT idOut, JAVA_OBJECT data, JAVA_INT timeoutMs) {
    return cn1HwImpl_hwCanReceive(threadStateData, h, idOut, data, timeoutMs);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwWatchdogOpen___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT timeoutSeconds) {
    return cn1HwImpl_hwWatchdogOpen(threadStateData, timeoutSeconds);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwWatchdogKick___int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT h) {
    return cn1HwImpl_hwWatchdogKick(threadStateData, h);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_hwClose___int_boolean(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_BOOLEAN disarm) {
    cn1HwImpl_hwClose(threadStateData, h, disarm);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_hwList___int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_INT kind) {
    return cn1HwImpl_hwList(threadStateData, kind);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_hwBacklight___int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_INT op, JAVA_INT value) {
    return cn1HwImpl_hwBacklight(threadStateData, op, value);
}

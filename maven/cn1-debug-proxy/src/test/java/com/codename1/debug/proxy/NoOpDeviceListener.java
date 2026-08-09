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
package com.codename1.debug.proxy;

/**
 * A {@link DeviceConnection.DeviceListener} that ignores everything, so a test
 * only has to override the one callback it is about.
 */
class NoOpDeviceListener implements DeviceConnection.DeviceListener {
    @Override public void onSymbols(SymbolTable symbols) {}
    @Override public void onHello(int version) {}
    @Override public void onBreakpointHit(long threadId, int methodId, int line) {}
    @Override public void onStepComplete(long threadId, int methodId, int line) {}
    @Override public void onThreads(long[] threadIds, boolean[] suspended, long[] threadObjects) {}
    @Override public void onStack(long threadId, int[] methodIds, int[] lines) {}
    @Override public void onLocals(int[] slots, byte[] typeCodes, long[] values) {}
    @Override public void onVmDeath() {}
    @Override public void onStringValue(String value) {}
    @Override public void onObjectClass(int classId, boolean isArray, int dimensions) {}
    @Override public void onObjectFields(byte[] typeCodes, long[] values) {}
    @Override public void onInvokeResult(byte type, long value) {}
    @Override public void onArrayLength(int length) {}
    @Override public void onArrayValues(byte tag, int count, byte[] rawBytes) {}
    @Override public void onReplyStatus() {}
    @Override public void onStdoutLine(String line) {}
    @Override public void onStderrLine(String line) {}
    @Override public void onUnknownEvent(int code, byte[] payload) {}
    @Override public void onDisconnected() {}
}

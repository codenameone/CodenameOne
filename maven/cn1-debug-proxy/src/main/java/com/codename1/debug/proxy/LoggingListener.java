/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.debug.proxy;

import java.util.Arrays;

/**
 * Diagnostic implementation of {@link DeviceConnection.DeviceListener} that
 * just prints every event. Useful when iterating on the wire protocol
 * before the JDWP front-end is wired in: lets you confirm the device side
 * is sending well-formed frames by hand-poking it from the proxy CLI.
 */
public final class LoggingListener implements DeviceConnection.DeviceListener {

    private final SymbolTable symbols;

    public LoggingListener(SymbolTable symbols) {
        this.symbols = symbols;
    }

    @Override public void onHello(int version) {
        System.out.println("[event] HELLO proto-version=" + version);
    }

    @Override public void onBreakpointHit(long threadId, int methodId, int line) {
        System.out.println("[event] BP_HIT tid=" + threadId + " " + describeLocation(methodId, line));
    }

    @Override public void onStepComplete(long threadId, int methodId, int line) {
        System.out.println("[event] STEP_COMPLETE tid=" + threadId + " " + describeLocation(methodId, line));
    }

    @Override public void onStack(long threadId, int[] methodIds, int[] lines) {
        System.out.println("[event] STACK tid=" + threadId + " depth=" + methodIds.length);
        for (int i = 0; i < methodIds.length; i++) {
            System.out.println("        #" + i + " " + describeLocation(methodIds[i], lines[i]));
        }
    }

    @Override public void onLocals(int[] slots, byte[] typeCodes, long[] values) {
        System.out.println("[event] LOCALS count=" + slots.length);
        for (int i = 0; i < slots.length; i++) {
            System.out.println("        slot=" + slots[i] + " type='" + (char) typeCodes[i]
                    + "' value=" + formatValue(typeCodes[i], values[i]));
        }
    }

    @Override public void onVmDeath() { System.out.println("[event] VM_DEATH"); }
    @Override public void onStringValue(String value) { System.out.println("[event] STRING_VALUE=" + value); }
    @Override public void onObjectClass(int classId) { System.out.println("[event] OBJECT_CLASS=" + classId); }
    @Override public void onReplyStatus() { System.out.println("[event] REPLY_STATUS"); }
    @Override public void onUnknownEvent(int code, byte[] payload) {
        System.out.println("[event] UNKNOWN code=0x" + Integer.toHexString(code) + " payload=" + Arrays.toString(payload));
    }
    @Override public void onDisconnected() { System.out.println("[event] DISCONNECTED"); }

    private String describeLocation(int methodId, int line) {
        SymbolTable.MethodInfo m = symbols.methodById(methodId);
        if (m == null) return "method=" + methodId + " line=" + line;
        SymbolTable.ClassInfo c = symbols.classById(m.classId);
        String cls = c != null ? c.name.replace('_', '.') : "?";
        return cls + "." + m.name + m.descriptor + ":" + line;
    }

    private String formatValue(byte typeCode, long value) {
        switch ((char) typeCode) {
            case 'I': case 'B': case 'S': case 'C': case 'Z': return Integer.toString((int) value);
            case 'J': return Long.toString(value);
            case 'F': return Float.toString(Float.intBitsToFloat((int) value));
            case 'D': return Double.toString(Double.longBitsToDouble(value));
            case 'L': case '[':
                return value == 0 ? "null" : "ref@0x" + Long.toHexString(value);
            default: return "0x" + Long.toHexString(value);
        }
    }
}

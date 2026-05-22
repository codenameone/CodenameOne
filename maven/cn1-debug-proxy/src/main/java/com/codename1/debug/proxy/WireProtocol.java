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

/**
 * Wire-protocol constants shared with the device-side runtime in
 * Ports/iOSPort/nativeSources/cn1_debugger.m. The two ends must stay in
 * lock-step.
 *
 * Frame format: 4-byte big-endian payload length, 1-byte command/event
 * code, then payload bytes. Codes 0x01-0x7F are commands from the proxy
 * to the device; codes 0x80-0xFF are events the device emits.
 */
public final class WireProtocol {
    private WireProtocol() {}

    public static final int PROTOCOL_VERSION = 1;

    public static final int CMD_SET_BREAKPOINT   = 0x02;
    public static final int CMD_CLEAR_BREAKPOINT = 0x03;
    public static final int CMD_RESUME           = 0x04;
    public static final int CMD_SUSPEND          = 0x05;
    public static final int CMD_GET_THREADS      = 0x06;
    public static final int CMD_GET_STACK        = 0x07;
    public static final int CMD_GET_LOCALS       = 0x08;
    public static final int CMD_STEP             = 0x09;
    public static final int CMD_DISPOSE          = 0x0A;
    public static final int CMD_GET_STRING       = 0x0B;
    public static final int CMD_GET_OBJECT_CLASS = 0x0C;
    public static final int CMD_GET_OBJECT_FIELDS = 0x0D;

    public static final int EVT_HELLO            = 0x80;
    public static final int EVT_THREAD_LIST      = 0x81;
    public static final int EVT_BP_HIT           = 0x82;
    public static final int EVT_STEP_COMPLETE    = 0x83;
    public static final int EVT_STACK            = 0x84;
    public static final int EVT_LOCALS           = 0x85;
    public static final int EVT_VM_DEATH         = 0x86;
    public static final int EVT_STRING_VALUE     = 0x87;
    public static final int EVT_REPLY_STATUS     = 0x88;
    public static final int EVT_OBJECT_CLASS     = 0x89;
    public static final int EVT_OBJECT_FIELDS    = 0x8A;
    public static final int EVT_STDOUT_LINE      = 0x8B;
    public static final int EVT_STDERR_LINE      = 0x8C;

    public static final int STEP_INTO = 0;
    public static final int STEP_OVER = 1;
    public static final int STEP_OUT  = 2;
}

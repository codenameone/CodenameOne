/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */

#ifndef CN1_DEBUGGER_H
#define CN1_DEBUGGER_H

#include "cn1_globals.h"

#ifdef CN1_ON_DEVICE_DEBUG

/**
 * Boots the on-device-debug listener thread. Reads the desktop proxy host
 * and port from Info.plist keys CN1ProxyHost and CN1ProxyPort, opens an
 * outbound TCP connection, sends a HELLO event, and services commands
 * (set/clear breakpoint, resume, step, get stack, get locals) in a loop.
 *
 * Called from CodenameOne_GLAppDelegate.m's application:didFinishLaunching
 * after signal handlers are in place but before the Java VM callback. If
 * Info.plist has CN1ProxyWaitForAttach=YES the function blocks until the
 * proxy connects and sends RESUME; otherwise it returns immediately and the
 * listener thread retries the connection in the background.
 */
extern void cn1_debugger_start(void);

#endif // CN1_ON_DEVICE_DEBUG
#endif // CN1_DEBUGGER_H

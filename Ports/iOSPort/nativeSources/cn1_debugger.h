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
 * Boots the on-device-debug listener thread (non-blocking). Reads the
 * desktop proxy host and port from Info.plist keys CN1ProxyHost /
 * CN1ProxyPort, spawns a background thread that opens an outbound TCP
 * connection, sends a HELLO event, and services commands (set/clear
 * breakpoint, resume, step, get stack, get locals) in a loop.
 *
 * Returns immediately. If Info.plist has CN1ProxyWaitForAttach=YES, the
 * function also installs a "Waiting for debugger" overlay UIWindow so the
 * user sees something other than the splash while the wait is in progress;
 * the overlay is dismissed automatically when {@link
 * cn1_debugger_run_when_ready} fires its block.
 */
extern void cn1_debugger_start(void);

#ifdef __BLOCKS__
/**
 * Defers the VM callback until the proxy reports the IDE has attached, so
 * the AppDelegate can keep `didFinishLaunchingWithOptions` returning
 * promptly and let UIKit draw the waiting overlay.
 *
 * If CN1ProxyWaitForAttach=NO (or the on-device-debug listener isn't
 * configured), the block is invoked synchronously on the calling thread.
 * Otherwise the block is stored and the proxy listener invokes it on the
 * main queue once it receives the first RESUME from the desktop proxy.
 */
extern void cn1_debugger_run_when_ready(void (^onReady)(void));
#endif

#endif // CN1_ON_DEVICE_DEBUG
#endif // CN1_DEBUGGER_H

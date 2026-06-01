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
 * Native bootstrap skeleton for the Codename One Windows (Win32) desktop port.
 *
 * The whole translation unit is gated on _WIN32, so it compiles to nothing on
 * iOS/macOS/Linux (where this file is never part of a build anyway) and only
 * carries weight in the "windows" clean-target executable build, where it is
 * compiled by clang-cl against the Windows SDK and linked alongside the
 * translated runtime and the Direct2D/DirectWrite layer.
 *
 * Phase 1 scaffolding: this provides only the early-boot logging helper. It does
 * NOT yet define WinMain or a message loop -- the ParparVM clean target still
 * emits the program entry from the app's Java main(), and defining a second
 * entry here would collide. The Win32 window, message pump and WinMain wiring
 * land in the windowing phase together with the WindowsNative bridge bodies.
 */

#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <stdio.h>

/*
 * Early-boot log sink used before the Codename One log/UI is available. Routes
 * to the debugger (OutputDebugString, visible in the VM's debugger / DebugView)
 * and to stderr so console runs and CI capture it too.
 */
void cn1WindowsLog(const char* message) {
    if (message == NULL) {
        return;
    }
    OutputDebugStringA(message);
    OutputDebugStringA("\n");
    fputs(message, stderr);
    fputc('\n', stderr);
}

#endif /* _WIN32 */

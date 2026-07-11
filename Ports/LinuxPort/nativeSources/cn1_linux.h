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
#ifndef CN1_LINUX_H
#define CN1_LINUX_H

/*
 * Common (GTK-free) internals shared across the native Codename One Linux port
 * translation units. The public surface the Java side
 * (com.codename1.impl.linux.LinuxNative) binds to is the set of ParparVM-mangled
 * bridge functions defined across the windowing / graphics / text / image / io /
 * net translation units; this header holds the cross-unit plumbing those share
 * that does NOT require the GTK/Cairo stack, so the pure-POSIX units (io, net,
 * socket) can include it without pulling in GTK. The render structs and GTK
 * includes live in cn1_linux_gfx.h, included only by the rendering units.
 */

#include "cn1_globals.h"
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ------------------------------------------------------------------ events */

/*
 * GTK runs its main loop on the process main thread, while the Codename One EDT
 * lives on a translated Java thread. Input is handed across a small
 * mutex-protected ring buffer: the GTK signal handlers push encoded events; the
 * EDT drains them through the pollEvent bridge. Keeping the bridge Java-driven
 * (Java pulls; C never calls arbitrary Java back) avoids threading the
 * GC/thread-state machinery through GTK callbacks. The type codes mirror the
 * EVENT_* constants in LinuxImplementation.
 */
typedef enum {
    CN1_EVENT_NONE = 0,
    CN1_EVENT_POINTER_PRESSED = 1,
    CN1_EVENT_POINTER_RELEASED = 2,
    CN1_EVENT_POINTER_DRAGGED = 3,
    CN1_EVENT_KEY_PRESSED = 4,
    CN1_EVENT_KEY_RELEASED = 5,
    CN1_EVENT_SIZE_CHANGED = 6,
    CN1_EVENT_CLOSE = 7,
    CN1_EVENT_MOUSE_WHEEL = 8,
    CN1_EVENT_MOUSE_HWHEEL = 9,
    /* Touchpad pinch / rotate: x/y are the gesture's widget coordinates and
     * keyCode is the incremental value in 1/10000 units -- an incremental scale
     * multiplier for PINCH (10000 == scale 1.0) and incremental radians for
     * ROTATE. drainInput decodes these into Display.fireMagnifyGesture /
     * fireRotationGesture, the same hooks the macOS trackpad drives. */
    CN1_EVENT_PINCH = 10,
    CN1_EVENT_ROTATE = 11,
    CN1_EVENT_ACCESSIBILITY_ACTION = 12
} CN1EventType;

/* Fixed-point scale for the gesture keyCode field (see CN1_EVENT_PINCH). */
#define CN1_GESTURE_FIXED 10000

/* For pointer (pressed/released/dragged) events the otherwise-unused keyCode
 * field carries the pointer metadata: the low bits are a button bitmask that
 * mirrors com.codename1.ui.events.PointerEvent.MASK_* (so a press/release carry
 * the button that changed and a drag carries the buttons held down), and the
 * high bits flag a touch digitizer so the Java side reports TYPE_TOUCH. A value
 * of 0 means "no detail" and defaults to a primary mouse press.
 * LinuxImplementation.drainInput decodes this. */
#define CN1_PE_MASK_PRIMARY   1
#define CN1_PE_MASK_SECONDARY 2
#define CN1_PE_MASK_MIDDLE    4
#define CN1_PE_MASK_BACK      8
#define CN1_PE_MASK_FORWARD   16
#define CN1_PE_TOUCH_FLAG     256

/* Pushes one event onto the ring buffer (called from the GTK thread). */
void cn1LinuxPushEvent(int type, int x, int y, int keyCode);

/*
 * Pops one event into out[0..3] = {type, x, y, keyCode}; returns 1 if one was
 * dequeued, 0 when the queue is empty. Drained by the EDT through pollEvent.
 */
int cn1LinuxPopEvent(int* out);

/* ------------------------------------------------------------- resources */

/*
 * Returns a pointer to the bytes of a classpath resource embedded into the ELF
 * (.incbin'd by the ParparVM linux target), writing the length into *lenOut, or
 * NULL with *lenOut = 0 when no such resource was embedded. Defined in the
 * generated cn1_resources_table.c.
 */
const unsigned char* cn1LinuxFindResource(const char* name, int* lenOut);

/* ------------------------------------------------------------- diagnostics */

/*
 * Logs (once per distinct tag) that a not-yet-implemented native bridge
 * capability was invoked on Linux. Used by the generated stub surface so a
 * missing capability degrades to an honest no-op + a single stderr line rather
 * than a silent wrong answer. Defined in cn1_linux_io.c.
 */
void cn1LinuxStubOnce(const char* tag);

/* Writes one line to stderr (and the journal) -- the nativeLog backing. */
void cn1LinuxLog(const char* message);

/* ---------------------------------------------------------------- helpers */

/*
 * Allocates a Java byte[] of n bytes and copies src into it (src may be NULL
 * when n == 0). Returns the array object, or JAVA_NULL on allocation failure.
 * Defined in cn1_linux_io.c; shared by the image/media/net units.
 */
JAVA_OBJECT cn1LinuxNewByteArray(CODENAME_ONE_THREAD_STATE, const void* src, int n);

#ifdef __cplusplus
}
#endif

#endif /* CN1_LINUX_H */

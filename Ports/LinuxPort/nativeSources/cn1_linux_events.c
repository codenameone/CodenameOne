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
 * The input ring buffer every Linux display backend feeds and the EDT drains
 * through the pollEvent bridge: the GTK window (cn1_linux_window.c) and the
 * embedded DRM/KMS, framebuffer and headless display (cn1_linux_embedded.c).
 * GTK-free on purpose, so both targets compile it unchanged.
 *
 * scripts/test_native_hover_queue.py and scripts/test_linux_event_pump.py
 * compile the ring standalone by slicing this file from CN1_EVENT_RING to the
 * end marker below; keep anything the slice needs inside it.
 */

#include "cn1_linux.h"
#include <pthread.h>

/* ----------------------------------------------------------- event ring */

#define CN1_EVENT_RING 1024
typedef struct {
    int type, x, y, key;
    /* Which window the event came from. Zero is the application's main window,
     * which is every event this port produced before desktop windows existed. */
    int windowId;
} CN1Event;
static CN1Event cn1EventRing[CN1_EVENT_RING];
static int cn1EventHead = 0;
static int cn1EventTail = 0;
static pthread_mutex_t cn1EventLock = PTHREAD_MUTEX_INITIALIZER;


void cn1LinuxPushEvent(int type, int x, int y, int keyCode) {
    cn1LinuxPushWindowEvent(0, type, x, y, keyCode);
}

/* Events the framework cannot reconstruct if they are lost, of which there are two
 * kinds.
 *
 * Lifecycle: a lost hide leaves a window the framework believes is on screen, painting
 * and animating until something else happens to it, and a lost close leaves it
 * registered with no native window behind it.
 *
 * Terminations: a release ends something a press started. Lose it and the component
 * the press went to stays in that state for good -- the key goes on repeating, the
 * button stays down, the drag never finishes -- and the focus change that would
 * otherwise cancel a held gesture is no use as a backstop if it is droppable too.
 *
 * Note the asymmetry with presses, which stay droppable: a release that arrives with
 * no press behind it finds no recorded target and is discarded harmlessly, so when
 * something has to go it must never be the release. */
/* Unlike hover motion, leave has no later motion outside the window to repair
 * a dropped notification. Protect only the terminal sentinel, not the motion stream. */
static int cn1LinuxIsProtectedEvent(int type, int x, int y) {
    return type == CN1_EVENT_WINDOW_SHOWN || type == CN1_EVENT_WINDOW_HIDDEN
            || type == CN1_EVENT_WINDOW_CLOSE
            || type == CN1_EVENT_KEY_RELEASED
            || type == CN1_EVENT_POINTER_RELEASED
            || type == CN1_EVENT_WINDOW_FOCUS
            || type == CN1_EVENT_SIZE_CHANGED
            || (type == CN1_EVENT_POINTER_HOVER && x == -1 && y == -1);
}

/* Visibility only. A close request is protected from eviction like any other
 * lifecycle event, but it is not a state that a later one supersedes: the delete
 * signal does not destroy the window, so a close that a subsequent minimize overwrote
 * would take the close listener and the close operation with it. */
static int cn1LinuxStateClass(int type) {
    if (type == CN1_EVENT_WINDOW_SHOWN || type == CN1_EVENT_WINDOW_HIDDEN) {
        return 1;
    }
    if (type == CN1_EVENT_SIZE_CHANGED) {
        return 2;
    }
    return 0;
}

/* Replaces a queued visibility event for the same window with this newer one. The
 * latest state is the one that matters -- a hide followed by a show leaves the window
 * shown -- so superseding costs nothing and needs no room. */
static int cn1LinuxCoalesceLifecycleLocked(int windowId, int type, int x, int y,
        int keyCode) {
    int idx = cn1EventHead;
    int newest = -1;
    int cls = cn1LinuxStateClass(type);
    if (cls == 0) {
        return 0;
    }
    /* The *newest* match, not the first one found. A window can already have more than
     * one transition queued -- a hide then a show -- and replacing the older of the two
     * leaves the newer one as the last word, so the framework would end up believing a
     * window that is natively hidden is on screen, and go on painting it. */
    while (idx != cn1EventTail) {
        if (cn1EventRing[idx].windowId == windowId
                && cn1LinuxStateClass(cn1EventRing[idx].type) == cls) {
            newest = idx;
        }
        idx = (idx + 1) % CN1_EVENT_RING;
    }
    if (newest < 0) {
        return 0;
    }
    cn1EventRing[newest].type = type;
    cn1EventRing[newest].x = x;
    cn1EventRing[newest].y = y;
    cn1EventRing[newest].key = keyCode;
    return 1;
}

/* Removes the oldest droppable event, closing the gap. Used to make room for a
 * protected one: advancing the head instead would evict whatever is oldest, and that
 * can be a protected event itself -- which is the very thing being kept. */
static void cn1LinuxRemoveAtLocked(int idx) {
    int cur = idx;
    int follow = (cur + 1) % CN1_EVENT_RING;
    while (follow != cn1EventTail) {
        cn1EventRing[cur] = cn1EventRing[follow];
        cur = follow;
        follow = (follow + 1) % CN1_EVENT_RING;
    }
    cn1EventTail = cur;
}

static int cn1LinuxEvictInputLocked(void) {
    int idx = cn1EventHead;
    while (idx != cn1EventTail) {
        if (!cn1LinuxIsProtectedEvent(cn1EventRing[idx].type,
                cn1EventRing[idx].x, cn1EventRing[idx].y)) {
            cn1LinuxRemoveAtLocked(idx);
            return 1;
        }
        idx = (idx + 1) % CN1_EVENT_RING;
    }
    return 0;
}

/* Last resort when the ring holds nothing but lifecycle events and so has no input to
 * give up. A window that toggled visibility several times before the framework drained
 * anything has more than one transition queued, and every one but its last is already
 * superseded, so dropping the oldest of them frees a slot without changing what any
 * window ends up as. Without this a close arriving for a *different* window has nowhere
 * to go and is dropped, which is the one outcome this whole path exists to prevent. */
static int cn1LinuxEvictSupersededVisibilityLocked(void) {
    int idx = cn1EventHead;
    while (idx != cn1EventTail) {
        int cls = cn1LinuxStateClass(cn1EventRing[idx].type);
        if (cls != 0) {
            int scan = (idx + 1) % CN1_EVENT_RING;
            while (scan != cn1EventTail) {
                if (cn1EventRing[scan].windowId == cn1EventRing[idx].windowId
                        && cn1LinuxStateClass(cn1EventRing[scan].type) == cls) {
                    cn1LinuxRemoveAtLocked(idx);
                    return 1;
                }
                scan = (scan + 1) % CN1_EVENT_RING;
            }
        }
        idx = (idx + 1) % CN1_EVENT_RING;
    }
    return 0;
}

/* Last resort before giving up an entry outright: drop the oldest *termination*.
 *
 * When the queue cannot grow, the question is only which loss costs least, and the
 * order is droppable input, then a state a later event already supersedes, then a
 * termination, then a lifecycle event. A lost release latches one component; a lost
 * close or hide loses a whole window -- the close operation never runs, or the
 * framework goes on painting a window that is not on screen. So a queued close or
 * visibility transition outranks any number of releases behind it. */
static int cn1LinuxEvictOldestTerminationLocked(void) {
    int idx = cn1EventHead;
    while (idx != cn1EventTail) {
        int t = cn1EventRing[idx].type;
        if (t == CN1_EVENT_KEY_RELEASED || t == CN1_EVENT_POINTER_RELEASED
                || t == CN1_EVENT_WINDOW_FOCUS
                || (t == CN1_EVENT_POINTER_HOVER && cn1EventRing[idx].x == -1
                        && cn1EventRing[idx].y == -1)) {
            cn1LinuxRemoveAtLocked(idx);
            return 1;
        }
        idx = (idx + 1) % CN1_EVENT_RING;
    }
    return 0;
}

void cn1LinuxPushWindowEvent(int windowId, int type, int x, int y, int keyCode) {
    pthread_mutex_lock(&cn1EventLock);
    int next = (cn1EventTail + 1) % CN1_EVENT_RING;
    if (next == cn1EventHead && cn1LinuxIsProtectedEvent(type, x, y)) {
        /* Full, and this one must not be the casualty. Supersede this window's own
         * queued transition if it has one, otherwise take the room from an input event,
         * and failing that from a transition that a later one already supersedes. Never
         * from a transition that is still some window's last word. */
        if (cn1LinuxCoalesceLifecycleLocked(windowId, type, x, y, keyCode)) {
            pthread_mutex_unlock(&cn1EventLock);
            return;
        }
        if (cn1LinuxEvictInputLocked() || cn1LinuxEvictSupersededVisibilityLocked()
                || cn1LinuxEvictOldestTerminationLocked()) {
            next = (cn1EventTail + 1) % CN1_EVENT_RING;
        } else {
            /* Nothing left but lifecycle events -- closes and visibility transitions
             * for more distinct windows than the ring can hold, which needs more windows
             * open than any application has. Giving up the oldest is all that remains,
             * and the newer event at least describes the more recent state. */
            cn1EventHead = (cn1EventHead + 1) % CN1_EVENT_RING;
            next = (cn1EventTail + 1) % CN1_EVENT_RING;
        }
    }
    if (next != cn1EventHead) {
        cn1EventRing[cn1EventTail].type = type;
        cn1EventRing[cn1EventTail].x = x;
        cn1EventRing[cn1EventTail].y = y;
        cn1EventRing[cn1EventTail].key = keyCode;
        cn1EventRing[cn1EventTail].windowId = windowId;
        cn1EventTail = next;
    }
    pthread_mutex_unlock(&cn1EventLock);
}

int cn1LinuxPopEvent(int* out) {
    int has = 0;
    pthread_mutex_lock(&cn1EventLock);
    if (cn1EventHead != cn1EventTail) {
        out[0] = cn1EventRing[cn1EventHead].type;
        out[1] = cn1EventRing[cn1EventHead].x;
        out[2] = cn1EventRing[cn1EventHead].y;
        out[3] = cn1EventRing[cn1EventHead].key;
        out[4] = cn1EventRing[cn1EventHead].windowId;
        cn1EventHead = (cn1EventHead + 1) % CN1_EVENT_RING;
        has = 1;
    }
    pthread_mutex_unlock(&cn1EventLock);
    return has;
}

/* ------------------------------------------------------------- globals */

/* Converts a stream of fractional scroll notches into whole ones.
 *
 * Smooth scroll deltas are fractions of a notch, so they cannot be forwarded one
 * for one: wheelUnits() on the Java side floors any sub-notch delta to a whole
 * notch, and a touchpad emits deltas continuously. Whole notches are returned and
 * the remainder is carried in *residue until it adds up. Truncation is toward
 * zero, so the residue always keeps the sign of the travel. */
int cn1LinuxTakeWholeNotches(double delta, double* residue) {
    double total = *residue + delta;
    int notches = (int) total;
    *residue = total - notches;
    return notches;
}

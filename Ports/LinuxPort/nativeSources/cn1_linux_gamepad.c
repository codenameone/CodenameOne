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
 * Game controllers over evdev (com.codename1.gaming.Gamepads).
 *
 * One reader thread polls every /dev/input/event* node that looks like a
 * gamepad, normalizes buttons and axes to the standard (Xbox) layout and pushes
 * them onto the shared input ring as CN1_EVENT_GAMEPAD_* events; the EDT drains
 * them with every other input and LinuxImplementation reports them to Gamepads.
 *
 * evdev rather than SDL or libudev, on purpose:
 *  - it needs no library, so it runs inside the Steam Linux Runtime container
 *    and in the embedded (no GTK) build alike;
 *  - under Steam, Steam Input presents every controller -- the Deck's own
 *    controls, a Steam Controller, a DualSense -- as one virtual Xbox pad, so
 *    the xpad layout is the only one that has to be right;
 *  - hotplug is inotify on /dev/input. IN_ATTRIB matters as much as IN_CREATE:
 *    udev creates the node first and grants the user access a moment later.
 *
 * Steam also leaves the physical controllers' own nodes visible and names them
 * in SDL_GAMECONTROLLER_IGNORE_DEVICES ("0xVID/0xPID,..."); those are skipped,
 * or every Steam Input pad would show up twice.
 */

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include "cn1_linux.h"
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/input.h>
#include <poll.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/inotify.h>
#include <sys/ioctl.h>
#include <unistd.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);

#define CN1_PAD_MAX 8
#define CN1_PAD_NAME 128
#define CN1_INPUT_DIR "/dev/input"

/* Mirrors com.codename1.gaming.Gamepad.BUTTON_* and AXIS_*. */
enum {
    PAD_A = 0, PAD_B, PAD_X, PAD_Y, PAD_LB, PAD_RB, PAD_BACK, PAD_START, PAD_GUIDE,
    PAD_LS, PAD_RS, PAD_UP, PAD_DOWN, PAD_LEFT, PAD_RIGHT
};
enum { AX_LX = 0, AX_LY, AX_RX, AX_RY, AX_LT, AX_RT };

#define BITS_PER_LONG (sizeof(long) * 8)
#define NLONGS(x) (((x) + BITS_PER_LONG - 1) / BITS_PER_LONG)
#define TEST_BIT(bit, arr) (((arr)[(bit) / BITS_PER_LONG] >> ((bit) % BITS_PER_LONG)) & 1UL)

typedef struct {
    int fd;                      /* -1 when the slot is free */
    char path[64];
    char name[CN1_PAD_NAME];
    struct input_absinfo abs[ABS_CNT];
    int hasAbs[ABS_CNT];
    int lastAxis[6];             /* last pushed value, fixed point */
    int hatX, hatY;
    int rumbleEffect;            /* uploaded FF effect id, or -1 */
} Cn1Pad;

static Cn1Pad cn1Pads[CN1_PAD_MAX];
static pthread_mutex_t cn1PadLock = PTHREAD_MUTEX_INITIALIZER;
static int cn1PadStarted = 0;

static void cn1PadPush(int type, int index, int a, int b) {
    cn1LinuxPushEvent(type, index, a, b);
}

/* "0x28de/0x1205,0x045e/0x028e" -> is vid/pid listed? */
static int cn1PadIgnored(unsigned vid, unsigned pid) {
    const char* list = getenv("SDL_GAMECONTROLLER_IGNORE_DEVICES");
    const char* p = list;
    if (list == NULL) {
        return 0;
    }
    while (*p) {
        unsigned v = 0, d = 0;
        if (sscanf(p, "%x/%x", &v, &d) == 2 && v == vid && d == pid) {
            return 1;
        }
        while (*p && *p != ',') {
            p++;
        }
        if (*p == ',') {
            p++;
        }
    }
    return 0;
}

static int cn1PadIsGamepad(int fd) {
    unsigned long evbits[NLONGS(EV_CNT)];
    unsigned long keybits[NLONGS(KEY_CNT)];
    unsigned long absbits[NLONGS(ABS_CNT)];
    memset(evbits, 0, sizeof(evbits));
    memset(keybits, 0, sizeof(keybits));
    memset(absbits, 0, sizeof(absbits));
    if (ioctl(fd, EVIOCGBIT(0, sizeof(evbits)), evbits) < 0
            || !TEST_BIT(EV_KEY, evbits)) {
        return 0;
    }
    ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(keybits)), keybits);
    if (TEST_BIT(BTN_GAMEPAD, keybits)) {
        return 1;
    }
    /* Older joystick-class pads report BTN_JOYSTICK plus sticks. */
    if (TEST_BIT(EV_ABS, evbits) && TEST_BIT(BTN_JOYSTICK, keybits)) {
        ioctl(fd, EVIOCGBIT(EV_ABS, sizeof(absbits)), absbits);
        return TEST_BIT(ABS_X, absbits) && TEST_BIT(ABS_Y, absbits);
    }
    return 0;
}

static int cn1PadFindPathLocked(const char* path) {
    int i;
    for (i = 0; i < CN1_PAD_MAX; i++) {
        if (cn1Pads[i].fd >= 0 && strcmp(cn1Pads[i].path, path) == 0) {
            return i;
        }
    }
    return -1;
}

static void cn1PadOpen(const char* path) {
    int fd, slot = -1, i;
    struct input_id id;
    pthread_mutex_lock(&cn1PadLock);
    if (cn1PadFindPathLocked(path) >= 0) {
        pthread_mutex_unlock(&cn1PadLock);
        return;
    }
    pthread_mutex_unlock(&cn1PadLock);

    /* Read-write for force feedback; read-only still gives input. */
    fd = open(path, O_RDWR | O_NONBLOCK | O_CLOEXEC);
    if (fd < 0) {
        fd = open(path, O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    }
    if (fd < 0) {
        return;
    }
    if (!cn1PadIsGamepad(fd)) {
        close(fd);
        return;
    }
    memset(&id, 0, sizeof(id));
    if (ioctl(fd, EVIOCGID, &id) == 0 && cn1PadIgnored(id.vendor, id.product)) {
        close(fd);
        return;
    }

    pthread_mutex_lock(&cn1PadLock);
    for (i = 0; i < CN1_PAD_MAX; i++) {
        if (cn1Pads[i].fd < 0) {
            slot = i;
            break;
        }
    }
    if (slot < 0) {
        pthread_mutex_unlock(&cn1PadLock);
        close(fd);
        return;
    }
    {
        Cn1Pad* pad = &cn1Pads[slot];
        int a;
        unsigned long absbits[NLONGS(ABS_CNT)];
        memset(pad, 0, sizeof(*pad));
        pad->fd = fd;
        pad->rumbleEffect = -1;
        /* -1 is "no direction"; 0 would be PAD_A and fake an A release. */
        pad->hatX = -1;
        pad->hatY = -1;
        snprintf(pad->path, sizeof(pad->path), "%s", path);
        if (ioctl(fd, EVIOCGNAME(sizeof(pad->name) - 1), pad->name) < 0) {
            pad->name[0] = 0;
        }
        memset(absbits, 0, sizeof(absbits));
        ioctl(fd, EVIOCGBIT(EV_ABS, sizeof(absbits)), absbits);
        for (a = 0; a < ABS_CNT; a++) {
            if (TEST_BIT(a, absbits) && ioctl(fd, EVIOCGABS(a), &pad->abs[a]) == 0
                    && pad->abs[a].maximum > pad->abs[a].minimum) {
                pad->hasAbs[a] = 1;
            }
        }
        for (a = 0; a < 6; a++) {
            pad->lastAxis[a] = 0x7fffffff;
        }
    }
    pthread_mutex_unlock(&cn1PadLock);
    cn1PadPush(CN1_EVENT_GAMEPAD_CONNECTED, slot, 0, 0);
}

static void cn1PadClose(int slot) {
    pthread_mutex_lock(&cn1PadLock);
    if (cn1Pads[slot].fd >= 0) {
        close(cn1Pads[slot].fd);
    }
    cn1Pads[slot].fd = -1;
    pthread_mutex_unlock(&cn1PadLock);
    cn1PadPush(CN1_EVENT_GAMEPAD_DISCONNECTED, slot, 0, 0);
}

static int cn1PadButtonOf(int code) {
    switch (code) {
        case BTN_SOUTH: return PAD_A;
        case BTN_EAST: return PAD_B;
        /* xpad, and Steam Input's virtual pad after it, report X as BTN_NORTH
         * (BTN_X) and Y as BTN_WEST (BTN_Y) -- the codes' names, not their
         * compass points. SDL's mapping for these devices agrees. */
        case BTN_NORTH: return PAD_X;
        case BTN_WEST: return PAD_Y;
        case BTN_TL: return PAD_LB;
        case BTN_TR: return PAD_RB;
        case BTN_SELECT: return PAD_BACK;
        case BTN_START: return PAD_START;
        case BTN_MODE: return PAD_GUIDE;
        case BTN_THUMBL: return PAD_LS;
        case BTN_THUMBR: return PAD_RS;
        case BTN_DPAD_UP: return PAD_UP;
        case BTN_DPAD_DOWN: return PAD_DOWN;
        case BTN_DPAD_LEFT: return PAD_LEFT;
        case BTN_DPAD_RIGHT: return PAD_RIGHT;
        default: return -1;
    }
}

static void cn1PadAxis(int slot, Cn1Pad* pad, int axis, double v) {
    int fixed = (int) (v * CN1_GAMEPAD_FIXED);
    int last = pad->lastAxis[axis];
    /* Sticks report hundreds of values a second; anything finer than 1/256 of
     * the travel is noise and only fills the ring. */
    /* The rest and end positions always go through, or a stick let go just
     * under the threshold would read as slightly pushed forever. */
    int edge;
    if (fixed == last) {
        return;
    }
    edge = fixed == 0 || abs(fixed) >= CN1_GAMEPAD_FIXED;
    if (last != 0x7fffffff && !edge && abs(fixed - last) < CN1_GAMEPAD_FIXED / 256) {
        return;
    }
    pad->lastAxis[axis] = fixed;
    cn1PadPush(CN1_EVENT_GAMEPAD_AXIS, slot, axis, fixed);
}

static double cn1PadNorm(const struct input_absinfo* ai, int value, int centered) {
    double t;
    /* The driver's own dead zone: a worn stick resting off-center reads 0. */
    if (centered && ai->flat > 0) {
        int center = ai->minimum + (ai->maximum - ai->minimum) / 2;
        if (abs(value - center) <= ai->flat) {
            return 0;
        }
    }
    t = (double) (value - ai->minimum) / (double) (ai->maximum - ai->minimum);
    if (t < 0) {
        t = 0;
    } else if (t > 1) {
        t = 1;
    }
    return centered ? t * 2.0 - 1.0 : t;
}

static void cn1PadHat(int slot, int* current, int value, int neg, int pos) {
    int next = value < 0 ? neg : (value > 0 ? pos : -1);
    if (next == *current) {
        return;
    }
    if (*current >= 0) {
        cn1PadPush(CN1_EVENT_GAMEPAD_BUTTON, slot, *current, 0);
    }
    if (next >= 0) {
        cn1PadPush(CN1_EVENT_GAMEPAD_BUTTON, slot, next, 1);
    }
    *current = next;
}

static void cn1PadHandle(int slot, Cn1Pad* pad, const struct input_event* ev) {
    if (ev->type == EV_KEY) {
        int b = cn1PadButtonOf(ev->code);
        if (ev->value == 2) {
            return; /* autorepeat */
        }
        if (b >= 0) {
            cn1PadPush(CN1_EVENT_GAMEPAD_BUTTON, slot, b, ev->value ? 1 : 0);
        } else if (ev->code == BTN_TL2 && !pad->hasAbs[ABS_Z] && !pad->hasAbs[ABS_BRAKE]) {
            cn1PadAxis(slot, pad, AX_LT, ev->value ? 1.0 : 0.0);
        } else if (ev->code == BTN_TR2 && !pad->hasAbs[ABS_RZ] && !pad->hasAbs[ABS_GAS]) {
            cn1PadAxis(slot, pad, AX_RT, ev->value ? 1.0 : 0.0);
        }
        return;
    }
    if (ev->type != EV_ABS || ev->code >= ABS_CNT || !pad->hasAbs[ev->code]) {
        return;
    }
    switch (ev->code) {
        case ABS_X: cn1PadAxis(slot, pad, AX_LX, cn1PadNorm(&pad->abs[ABS_X], ev->value, 1)); break;
        case ABS_Y: cn1PadAxis(slot, pad, AX_LY, cn1PadNorm(&pad->abs[ABS_Y], ev->value, 1)); break;
        case ABS_RX: cn1PadAxis(slot, pad, AX_RX, cn1PadNorm(&pad->abs[ABS_RX], ev->value, 1)); break;
        case ABS_RY: cn1PadAxis(slot, pad, AX_RY, cn1PadNorm(&pad->abs[ABS_RY], ev->value, 1)); break;
        case ABS_Z:
        case ABS_BRAKE:
            cn1PadAxis(slot, pad, AX_LT, cn1PadNorm(&pad->abs[ev->code], ev->value, 0));
            break;
        case ABS_RZ:
        case ABS_GAS:
            cn1PadAxis(slot, pad, AX_RT, cn1PadNorm(&pad->abs[ev->code], ev->value, 0));
            break;
        case ABS_HAT0X: cn1PadHat(slot, &pad->hatX, ev->value, PAD_LEFT, PAD_RIGHT); break;
        case ABS_HAT0Y: cn1PadHat(slot, &pad->hatY, ev->value, PAD_UP, PAD_DOWN); break;
        default: break;
    }
}

static void cn1PadScan(void) {
    DIR* d = opendir(CN1_INPUT_DIR);
    struct dirent* e;
    char path[64];
    if (d == NULL) {
        return;
    }
    while ((e = readdir(d)) != NULL) {
        if (strncmp(e->d_name, "event", 5) == 0) {
            snprintf(path, sizeof(path), CN1_INPUT_DIR "/%s", e->d_name);
            cn1PadOpen(path);
        }
    }
    closedir(d);
}

static void* cn1PadThread(void* arg) {
    int ino = inotify_init1(IN_NONBLOCK | IN_CLOEXEC);
    (void) arg;
    if (ino >= 0) {
        inotify_add_watch(ino, CN1_INPUT_DIR, IN_CREATE | IN_ATTRIB);
    }
    cn1PadScan();
    for (;;) {
        struct pollfd fds[CN1_PAD_MAX + 1];
        int slots[CN1_PAD_MAX + 1];
        int n = 0, i;
        if (ino >= 0) {
            fds[n].fd = ino;
            fds[n].events = POLLIN;
            slots[n] = -1;
            n++;
        }
        pthread_mutex_lock(&cn1PadLock);
        for (i = 0; i < CN1_PAD_MAX; i++) {
            if (cn1Pads[i].fd >= 0) {
                fds[n].fd = cn1Pads[i].fd;
                fds[n].events = POLLIN;
                slots[n] = i;
                n++;
            }
        }
        pthread_mutex_unlock(&cn1PadLock);
        if (poll(fds, (nfds_t) n, ino >= 0 ? -1 : 2000) < 0) {
            if (errno == EINTR) {
                continue;
            }
            usleep(100000);
            continue;
        }
        if (ino < 0) {
            /* No inotify (an unusual sandbox): rescan now and then instead. */
            cn1PadScan();
        }
        for (i = 0; i < n; i++) {
            if (fds[i].revents == 0) {
                continue;
            }
            if (slots[i] < 0) {
                char buf[4096];
                ssize_t len;
                int changed = 0;
                while ((len = read(ino, buf, sizeof(buf))) > 0) {
                    changed = 1;
                }
                if (changed) {
                    cn1PadScan();
                }
                continue;
            }
            if (fds[i].revents & (POLLERR | POLLHUP | POLLNVAL)) {
                cn1PadClose(slots[i]);
                continue;
            }
            {
                struct input_event evs[64];
                ssize_t len;
                int slot = slots[i];
                Cn1Pad* pad = &cn1Pads[slot];
                while ((len = read(fds[i].fd, evs, sizeof(evs))) > 0) {
                    int k, count = (int) (len / (ssize_t) sizeof(struct input_event));
                    for (k = 0; k < count; k++) {
                        cn1PadHandle(slot, pad, &evs[k]);
                    }
                }
                if (len < 0 && errno == ENODEV) {
                    cn1PadClose(slot);
                }
            }
        }
    }
    return NULL;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_gamepadStart__(CODENAME_ONE_THREAD_STATE) {
    pthread_t t;
    pthread_attr_t attr;
    int i;
    pthread_mutex_lock(&cn1PadLock);
    if (cn1PadStarted) {
        pthread_mutex_unlock(&cn1PadLock);
        return;
    }
    cn1PadStarted = 1;
    for (i = 0; i < CN1_PAD_MAX; i++) {
        cn1Pads[i].fd = -1;
    }
    pthread_mutex_unlock(&cn1PadLock);
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    pthread_create(&t, &attr, cn1PadThread, NULL);
    pthread_attr_destroy(&attr);
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_gamepadName___int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_INT index) {
    char name[CN1_PAD_NAME];
    name[0] = 0;
    if (index >= 0 && index < CN1_PAD_MAX) {
        pthread_mutex_lock(&cn1PadLock);
        if (cn1Pads[index].fd >= 0) {
            snprintf(name, sizeof(name), "%s", cn1Pads[index].name);
        }
        pthread_mutex_unlock(&cn1PadLock);
    }
    return newStringFromCString(threadStateData, name);
}

/* strong/weak are 0..65535, the units FF_RUMBLE takes. */
JAVA_VOID com_codename1_impl_linux_LinuxNative_gamepadRumble___int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_INT index, JAVA_INT strong, JAVA_INT weak, JAVA_INT millis) {
    struct ff_effect effect;
    struct input_event play;
    Cn1Pad* pad;
    if (index < 0 || index >= CN1_PAD_MAX) {
        return;
    }
    pthread_mutex_lock(&cn1PadLock);
    pad = &cn1Pads[index];
    if (pad->fd < 0) {
        pthread_mutex_unlock(&cn1PadLock);
        return;
    }
    memset(&effect, 0, sizeof(effect));
    effect.type = FF_RUMBLE;
    effect.id = (short) pad->rumbleEffect;
    effect.u.rumble.strong_magnitude = (unsigned short) (strong < 0 ? 0 : (strong > 0xffff ? 0xffff : strong));
    effect.u.rumble.weak_magnitude = (unsigned short) (weak < 0 ? 0 : (weak > 0xffff ? 0xffff : weak));
    effect.replay.length = (unsigned short) (millis < 0 ? 0 : (millis > 0xffff ? 0xffff : millis));
    /* Re-uploading with the same id updates the effect in place. */
    if (ioctl(pad->fd, EVIOCSFF, &effect) == 0) {
        pad->rumbleEffect = effect.id;
        memset(&play, 0, sizeof(play));
        play.type = EV_FF;
        play.code = (unsigned short) effect.id;
        play.value = 1;
        if (write(pad->fd, &play, sizeof(play)) < 0) {
            /* The device went away or is read-only; rumble is best effort. */
        }
    }
    pthread_mutex_unlock(&cn1PadLock);
}

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
 * The embedded Linux display: the application owns the screen with no window
 * system, no GTK and no X11 or Wayland. Compiled only when the generated
 * project is configured with CN1_LINUX_EMBEDDED (LinuxNativeBuilder sets it
 * for linux.embedded=true); the desktop target compiles this file empty and
 * uses cn1_linux_window.c instead. The two define the same bridge natives.
 *
 * Rendering is the same Cairo/Pango code the desktop target uses -- every
 * primitive in cn1_linux_graphics.c / cn1_linux_text.c / cn1_linux_image.c
 * draws into the back buffer here -- so an application looks identical on
 * both. Presenting a frame copies the damaged region of that buffer to the
 * screen through one of these backends, picked at start-up (CN1_DISPLAY, or
 * the first that works in this order):
 *
 *   kms       DRM/KMS dumb buffers, double buffered with a page flip on vblank.
 *             Works on every KMS driver (vc4/v3d on a Raspberry Pi, i.MX
 *             DCSS/LCDIF, Rockchip VOP, simpledrm, virtio-gpu) with no GPU
 *             driver at all, so it is the default. Needs DRM master, which a
 *             process started outside a desktop session has.
 *   fbdev     /dev/fb0, for kernels and boards with no KMS driver. 32 and 16 bpp.
 *   memory    an in-memory scanout of the same layout as kms. The frame is
 *             copied exactly as it would be to a screen and then discarded;
 *             for CI and benchmarks, where the copy must still be paid.
 *   headless  no output; captureWindowToPngBytes and screenshots still work.
 *
 * Input is read straight from evdev (/dev/input/event*) on one thread, with
 * hot-plug through inotify: multitouch (protocol B) and single-touch screens,
 * mice (with a software cursor), keyboards (US layout) and rotary encoders,
 * which arrive as up/down keys so focus-based HMI navigation works with no
 * touch screen at all. Devices are grabbed (EVIOCGRAB) so keystrokes do not
 * also reach the text console underneath.
 *
 * Environment (the builder bakes the board profile's defaults for the first
 * three in as CN1_DEFAULT_DISPLAY_NAME, CN1_DEFAULT_ROTATION and CN1_DEFAULT_DPI):
 *
 *   CN1_DISPLAY            kms | fbdev | memory | headless   (default: auto)
 *   CN1_DRM_DEVICE         /dev/dri/cardN                    (default: scan)
 *   CN1_FB_DEVICE          /dev/fbN                          (default: /dev/fb0)
 *   CN1_ROTATION           0 | 90 | 180 | 270, clockwise
 *   CN1_DPI                override the density the panel reports
 *   CN1_SIZE               WxH for memory / headless         (default: 800x480)
 *   CN1_CURSOR             1 | 0                             (default: with a mouse)
 *   CN1_TOUCH_SWAP_XY, CN1_TOUCH_INVERT_X, CN1_TOUCH_INVERT_Y   touch calibration
 *   CN1_INPUT_NO_GRAB      leave input devices shared with the console
 *   CN1_SNAPSHOT           write one frame (CN1_SNAPSHOT_FRAME, default 60) as a PNG
 */

#ifdef CN1_LINUX_EMBEDDED

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include "cn1_linux_gfx.h"
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <poll.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <sys/inotify.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <linux/fb.h>
#include <linux/input.h>
#include <linux/kd.h>
#include <linux/vt.h>
#ifdef CN1_EMBEDDED_HAS_DRM
#include <xf86drm.h>
#include <xf86drmMode.h>
#endif
#ifdef CN1_EMBEDDED_GPU
#include "cn1_linux_gpu.h"
#endif

/* Board-profile defaults (LinuxNativeBuilder, EmbeddedBoardProfile). The display
 * arrives as a bare token, CN1_DEFAULT_DISPLAY_NAME=kms, because a quoted string
 * does not survive the trip through a CMake list. */
#define CN1_EMB_STR2(x) #x
#define CN1_EMB_STR(x) CN1_EMB_STR2(x)
#ifdef CN1_DEFAULT_DISPLAY_NAME
#define CN1_DEFAULT_DISPLAY CN1_EMB_STR(CN1_DEFAULT_DISPLAY_NAME)
#else
#define CN1_DEFAULT_DISPLAY "auto"
#endif
#ifndef CN1_DEFAULT_ROTATION
#define CN1_DEFAULT_ROTATION 0
#endif
#ifndef CN1_DEFAULT_DPI
#define CN1_DEFAULT_DPI 0
#endif

enum { OUT_HEADLESS = 0, OUT_MEMORY, OUT_FBDEV, OUT_KMS };

typedef struct { int x, y, w, h; } CN1EmbRect;

/* ---------------------------------------------------------------- state */

static CN1Graphics cn1WindowG;        /* the logical back buffer every paint goes to */
static int cn1Output = OUT_HEADLESS;
static int cn1Rotation = 0;           /* 0, 90, 180, 270 clockwise */
static int cn1LogicalW = 800, cn1LogicalH = 480;   /* what the application sees */
static int cn1PhysW = 800, cn1PhysH = 480;         /* the scanout */
static int cn1DisplayUp = 0;                        /* initDisplay has sized the buffers */
static int cn1Dpi = 0;
static int cn1Initialized = 0;
static int cn1HeadlessForced = 0;     /* enableHeadlessScreenshot was called */
static volatile int cn1Running = 1;
static volatile int cn1CurrentModifiers = 0;
static int cn1HasTouch = 0, cn1HasPointer = 0;

/* The scanout: one or two buffers of cn1PhysW x cn1PhysH. Each remembers the
 * region it is stale in, because with page flipping the buffer being drawn
 * was last updated two frames ago. */
typedef struct {
    unsigned char* map;
    size_t size;
    int pitch;
    int bpp;            /* 32 or 16 */
    unsigned fbId;
    unsigned handle;
    CN1EmbRect stale;   /* region still to be copied into this buffer */
    CN1EmbRect cursor;  /* where the cursor was drawn into it, w == 0 if not */
} CN1Scanout;

static CN1Scanout cn1Buf[2];
static int cn1BufCount = 0;
static int cn1Front = 0;
static pthread_mutex_t cn1PresentLock = PTHREAD_MUTEX_INITIALIZER;
static int cn1FbRedOffset = 16, cn1FbBlueOffset = 0;

#ifdef CN1_EMBEDDED_HAS_DRM
static int cn1DrmFd = -1;
static unsigned cn1DrmCrtc, cn1DrmConnector;
static drmModeModeInfo cn1DrmMode;
static drmModeCrtcPtr cn1DrmSavedCrtc;
static int cn1FlipPending = 0;
#endif
static int cn1FbFd = -1;
static int cn1TtyFd = -1;
static int cn1TtySavedKbMode = -1;

/* Software cursor, in logical coordinates. */
static int cn1CursorEnabled = 0;
static volatile int cn1CursorX = 0, cn1CursorY = 0;
#define CN1_CURSOR_W 12
#define CN1_CURSOR_H 19

/* Wakes pumpMessages when the input thread queued something. */
static int cn1WakePipe[2] = { -1, -1 };

/* ---------------------------------------------------------- utilities */

static const char* cn1Env(const char* name, const char* def) {
    const char* v = getenv(name);
    return v != 0 && *v != 0 ? v : def;
}

static int cn1EnvInt(const char* name, int def) {
    const char* v = getenv(name);
    return v != 0 && *v != 0 ? atoi(v) : def;
}

static CN1EmbRect cn1RectUnion(CN1EmbRect a, CN1EmbRect b) {
    CN1EmbRect r;
    int x2, y2;
    if (a.w <= 0 || a.h <= 0) return b;
    if (b.w <= 0 || b.h <= 0) return a;
    r.x = a.x < b.x ? a.x : b.x;
    r.y = a.y < b.y ? a.y : b.y;
    x2 = a.x + a.w > b.x + b.w ? a.x + a.w : b.x + b.w;
    y2 = a.y + a.h > b.y + b.h ? a.y + a.h : b.y + b.h;
    r.w = x2 - r.x;
    r.h = y2 - r.y;
    return r;
}

static CN1EmbRect cn1RectClip(CN1EmbRect r, int w, int h) {
    if (r.x < 0) { r.w += r.x; r.x = 0; }
    if (r.y < 0) { r.h += r.y; r.y = 0; }
    if (r.x + r.w > w) r.w = w - r.x;
    if (r.y + r.h > h) r.h = h - r.y;
    if (r.w < 0) r.w = 0;
    if (r.h < 0) r.h = 0;
    return r;
}

static void cn1Wake(void) {
    if (cn1WakePipe[1] >= 0) {
        char c = 1;
        ssize_t n = write(cn1WakePipe[1], &c, 1);
        (void) n;
    }
}

/* Logical (application) -> physical (scanout) point for the current rotation. */
static void cn1ToPhysical(int lx, int ly, int* px, int* py) {
    switch (cn1Rotation) {
        case 90:  *px = cn1PhysW - 1 - ly; *py = lx; break;
        case 180: *px = cn1PhysW - 1 - lx; *py = cn1PhysH - 1 - ly; break;
        case 270: *px = ly; *py = cn1PhysH - 1 - lx; break;
        default:  *px = lx; *py = ly; break;
    }
}

/* Physical -> logical, the inverse, for input. */
static void cn1ToLogical(int px, int py, int* lx, int* ly) {
    switch (cn1Rotation) {
        case 90:  *lx = py; *ly = cn1PhysW - 1 - px; break;
        case 180: *lx = cn1PhysW - 1 - px; *ly = cn1PhysH - 1 - py; break;
        case 270: *lx = cn1PhysH - 1 - py; *ly = px; break;
        default:  *lx = px; *ly = py; break;
    }
}

/* ----------------------------------------------------------- back buffer */

static void cn1ResizeBackBuffer(int w, int h) {
    if (cn1WindowG.cr) {
        cairo_destroy(cn1WindowG.cr);
    }
    if (cn1WindowG.surface) {
        cairo_surface_destroy(cn1WindowG.surface);
    }
    cn1LinuxFreeClipShape(&cn1WindowG);
    memset(&cn1WindowG, 0, sizeof(cn1WindowG));
    cn1WindowG.surface = cairo_image_surface_create(CAIRO_FORMAT_ARGB32, w, h);
    cn1WindowG.cr = cairo_create(cn1WindowG.surface);
    cn1WindowG.width = w;
    cn1WindowG.height = h;
    cn1WindowG.alpha = 255;
    cn1WindowG.clipIsRect = 1;
    cn1WindowG.clipW = w;
    cn1WindowG.clipH = h;
    cn1WindowG.isWindowTarget = 1;
    cairo_matrix_init_identity(&cn1WindowG.transform);
    cairo_set_source_rgb(cn1WindowG.cr, 0, 0, 0);
    cairo_paint(cn1WindowG.cr);
}

CN1Graphics* cn1LinuxWindowGraphics(void) {
    return &cn1WindowG;
}

/* No main loop to marshal onto: every caller already runs where it may. */
void cn1LinuxRunOnMainAndWait(void (*fn)(void*), void* arg) {
    fn(arg);
}

/* ------------------------------------------------------- console / VT */

/* Stops the text console from drawing over the application or receiving its
 * keystrokes. Best effort: without a VT (a container, a serial console) there
 * is nothing to hide. Restored at exit. */
static void cn1ConsoleGraphics(void) {
    int mode;
    cn1TtyFd = open("/dev/tty0", O_RDWR | O_CLOEXEC);
    if (cn1TtyFd < 0) {
        cn1TtyFd = open("/dev/tty", O_RDWR | O_CLOEXEC);
    }
    if (cn1TtyFd < 0) {
        return;
    }
    if (ioctl(cn1TtyFd, KDSETMODE, KD_GRAPHICS) < 0) {
        close(cn1TtyFd);
        cn1TtyFd = -1;
        return;
    }
    if (ioctl(cn1TtyFd, KDGKBMODE, &mode) == 0) {
        cn1TtySavedKbMode = mode;
        ioctl(cn1TtyFd, KDSKBMODE, K_OFF);
    }
}

static void cn1ConsoleRestore(void) {
    if (cn1TtyFd >= 0) {
        if (cn1TtySavedKbMode >= 0) {
            ioctl(cn1TtyFd, KDSKBMODE, cn1TtySavedKbMode);
        }
        ioctl(cn1TtyFd, KDSETMODE, KD_TEXT);
        close(cn1TtyFd);
        cn1TtyFd = -1;
    }
}

/* ------------------------------------------------------------- outputs */

#ifdef CN1_EMBEDDED_HAS_DRM
static int cn1DrmCreateBuffer(CN1Scanout* b, int w, int h) {
    struct drm_mode_create_dumb create;
    struct drm_mode_map_dumb map;
    memset(&create, 0, sizeof(create));
    create.width = (unsigned) w;
    create.height = (unsigned) h;
    create.bpp = 32;
    if (drmIoctl(cn1DrmFd, DRM_IOCTL_MODE_CREATE_DUMB, &create) < 0) {
        return -1;
    }
    b->handle = create.handle;
    b->pitch = (int) create.pitch;
    b->size = (size_t) create.size;
    b->bpp = 32;
    b->fbId = 0;
    /* The blit writes pitch * h bytes from the map; a driver answering with a
     * smaller buffer is refused rather than written past. */
    if (create.pitch < (unsigned) w * 4u || b->size < (size_t) create.pitch * (size_t) h
            || drmModeAddFB(cn1DrmFd, (unsigned) w, (unsigned) h, 24, 32, create.pitch, create.handle, &b->fbId) != 0) {
        b->fbId = 0;
        goto fail;
    }
    memset(&map, 0, sizeof(map));
    map.handle = create.handle;
    if (drmIoctl(cn1DrmFd, DRM_IOCTL_MODE_MAP_DUMB, &map) < 0) {
        goto fail;
    }
    b->map = (unsigned char*) mmap(0, b->size, PROT_READ | PROT_WRITE, MAP_SHARED, cn1DrmFd, (off_t) map.offset);
    if (b->map == MAP_FAILED) {
        b->map = 0;
        goto fail;
    }
    memset(b->map, 0, b->size);
    return 0;
fail:
    /* Handed back, so a fallback or a retry does not pile up buffers. */
    if (b->fbId != 0) {
        drmModeRmFB(cn1DrmFd, b->fbId);
        b->fbId = 0;
    }
    {
        struct drm_mode_destroy_dumb destroy;
        memset(&destroy, 0, sizeof(destroy));
        destroy.handle = create.handle;
        drmIoctl(cn1DrmFd, DRM_IOCTL_MODE_DESTROY_DUMB, &destroy);
    }
    return -1;
}

/* Opens one card and picks its first connected connector, that connector's
 * preferred mode and a CRTC that can drive it. */
static int cn1DrmTryCard(const char* path) {
    drmModeResPtr res;
    drmModeConnectorPtr conn = 0;
    drmModeEncoderPtr enc;
    int i, j, fd;
    unsigned crtc = 0;
    fd = open(path, O_RDWR | O_CLOEXEC);
    if (fd < 0) {
        return -1;
    }
    res = drmModeGetResources(fd);
    if (res == 0) {
        close(fd);
        return -1;
    }
    for (i = 0; i < res->count_connectors && conn == 0; i++) {
        drmModeConnectorPtr c = drmModeGetConnector(fd, res->connectors[i]);
        if (c != 0 && c->connection == DRM_MODE_CONNECTED && c->count_modes > 0) {
            conn = c;
        } else if (c != 0) {
            drmModeFreeConnector(c);
        }
    }
    if (conn == 0) {
        drmModeFreeResources(res);
        close(fd);
        return -1;
    }
    cn1DrmMode = conn->modes[0];
    for (i = 0; i < conn->count_modes; i++) {
        if (conn->modes[i].type & DRM_MODE_TYPE_PREFERRED) {
            cn1DrmMode = conn->modes[i];
            break;
        }
    }
    if (conn->encoder_id != 0) {
        enc = drmModeGetEncoder(fd, conn->encoder_id);
        if (enc != 0) {
            crtc = enc->crtc_id;
            drmModeFreeEncoder(enc);
        }
    }
    for (i = 0; crtc == 0 && i < conn->count_encoders; i++) {
        enc = drmModeGetEncoder(fd, conn->encoders[i]);
        if (enc == 0) continue;
        for (j = 0; j < res->count_crtcs; j++) {
            if (enc->possible_crtcs & (1u << j)) {
                crtc = res->crtcs[j];
                break;
            }
        }
        drmModeFreeEncoder(enc);
    }
    if (crtc == 0) {
        drmModeFreeConnector(conn);
        drmModeFreeResources(res);
        close(fd);
        return -1;
    }
    cn1DrmFd = fd;
    cn1DrmCrtc = crtc;
    cn1DrmConnector = conn->connector_id;
    cn1PhysW = cn1DrmMode.hdisplay;
    cn1PhysH = cn1DrmMode.vdisplay;
    if (cn1Dpi == 0 && conn->mmWidth > 0) {
        cn1Dpi = (int) (cn1PhysW * 25.4 / conn->mmWidth + 0.5);
    }
    drmModeFreeConnector(conn);
    drmModeFreeResources(res);
    return 0;
}

static int cn1KmsDumbBuffers(void);

static int cn1InitKms(void) {
    char path[64];
    const char* dev = getenv("CN1_DRM_DEVICE");
    int i;
    if (dev != 0 && *dev) {
        if (cn1DrmTryCard(dev) != 0) return -1;
    } else {
        for (i = 0; i < 8; i++) {
            snprintf(path, sizeof(path), "/dev/dri/card%d", i);
            if (cn1DrmTryCard(path) == 0) break;
        }
        if (cn1DrmFd < 0) return -1;
    }
    cn1DrmSavedCrtc = drmModeGetCrtc(cn1DrmFd, cn1DrmCrtc);
#ifdef CN1_EMBEDDED_GPU
    /* The GPU renderer scans out its own buffers; the dumb buffers below are the
     * software renderer's, and its fallback when the device has no usable GPU. */
    if (cn1GpuInitKms(cn1DrmFd, cn1DrmCrtc, cn1DrmConnector, &cn1DrmMode, cn1PhysW, cn1PhysH, cn1Rotation) == 0) {
        return 0;
    }
    fprintf(stderr, "CN1: no usable GPU on %s, using the software renderer\n", dev != 0 && *dev ? dev : "the DRM device");
#endif
    return cn1KmsDumbBuffers();
}

/* Two dumb buffers the CPU writes and the display scans out, shown alternately. */
static int cn1KmsDumbBuffers(void) {
    int i;
    for (i = 0; i < 2; i++) {
        if (cn1DrmCreateBuffer(&cn1Buf[i], cn1PhysW, cn1PhysH) != 0) {
            close(cn1DrmFd);
            cn1DrmFd = -1;
            return -1;
        }
    }
    cn1BufCount = 2;
    if (drmModeSetCrtc(cn1DrmFd, cn1DrmCrtc, cn1Buf[0].fbId, 0, 0, &cn1DrmConnector, 1, &cn1DrmMode) != 0) {
        /* Not DRM master: a compositor owns the screen. */
        close(cn1DrmFd);
        cn1DrmFd = -1;
        cn1BufCount = 0;
        return -1;
    }
    cn1Front = 0;
    return 0;
}

static void cn1PageFlipDone(int fd, unsigned frame, unsigned sec, unsigned usec, void* data) {
    (void) fd; (void) frame; (void) sec; (void) usec; (void) data;
    cn1FlipPending = 0;
}

static void cn1KmsWaitFlip(void) {
    drmEventContext ev;
    struct pollfd p;
    memset(&ev, 0, sizeof(ev));
    ev.version = 2;
    ev.page_flip_handler = cn1PageFlipDone;
    while (cn1FlipPending) {
        p.fd = cn1DrmFd;
        p.events = POLLIN;
        p.revents = 0;
        if (poll(&p, 1, 100) <= 0) {
            /* A flip that never completes (display unplugged) must not hang the EDT. */
            cn1FlipPending = 0;
            break;
        }
        drmHandleEvent(cn1DrmFd, &ev);
    }
}
#endif /* CN1_EMBEDDED_HAS_DRM */

static int cn1InitFbdev(void) {
    struct fb_var_screeninfo var;
    struct fb_fix_screeninfo fix;
    const char* dev = cn1Env("CN1_FB_DEVICE", "/dev/fb0");
    cn1FbFd = open(dev, O_RDWR | O_CLOEXEC);
    if (cn1FbFd < 0) {
        return -1;
    }
    if (ioctl(cn1FbFd, FBIOGET_VSCREENINFO, &var) < 0 || ioctl(cn1FbFd, FBIOGET_FSCREENINFO, &fix) < 0
            || (var.bits_per_pixel != 32 && var.bits_per_pixel != 16)
            /* The blit writes yres rows of xres pixels from the map's start; a
             * driver whose memory or line length cannot hold that is refused. */
            || (size_t) var.xres * (var.bits_per_pixel / 8) > fix.line_length
            || (size_t) var.yres * fix.line_length > fix.smem_len) {
        close(cn1FbFd);
        cn1FbFd = -1;
        return -1;
    }
    cn1PhysW = (int) var.xres;
    cn1PhysH = (int) var.yres;
    cn1FbRedOffset = (int) var.red.offset;
    cn1FbBlueOffset = (int) var.blue.offset;
    cn1Buf[0].pitch = (int) fix.line_length;
    cn1Buf[0].bpp = (int) var.bits_per_pixel;
    cn1Buf[0].size = (size_t) fix.smem_len;
    cn1Buf[0].map = (unsigned char*) mmap(0, cn1Buf[0].size, PROT_READ | PROT_WRITE, MAP_SHARED, cn1FbFd, 0);
    if (cn1Buf[0].map == MAP_FAILED) {
        cn1Buf[0].map = 0;
        close(cn1FbFd);
        cn1FbFd = -1;
        return -1;
    }
    if (cn1Dpi == 0 && var.width > 0 && var.width < 10000) {
        cn1Dpi = (int) (cn1PhysW * 25.4 / var.width + 0.5);
    }
    cn1BufCount = 1;
    return 0;
}

static int cn1InitMemory(void) {
    cn1Buf[0].pitch = cn1PhysW * 4;
    cn1Buf[0].bpp = 32;
    cn1Buf[0].size = (size_t) cn1Buf[0].pitch * cn1PhysH;
    cn1Buf[0].map = (unsigned char*) calloc(1, cn1Buf[0].size);
    if (cn1Buf[0].map == 0) {
        return -1;
    }
    cn1BufCount = 1;
    return 0;
}

/* --------------------------------------------------------------- blit */

static inline uint32_t cn1ToTarget32(uint32_t p) {
    if (cn1FbRedOffset == 16) {
        return p | 0xff000000u;
    }
    /* BGR panel: swap red and blue. */
    return 0xff000000u | ((p & 0xffu) << 16) | (p & 0xff00u) | ((p >> 16) & 0xffu);
}

static inline uint16_t cn1ToRgb565(uint32_t p) {
    return (uint16_t) (((p >> 8) & 0xf800u) | ((p >> 5) & 0x07e0u) | ((p >> 3) & 0x001fu));
}

/* Copies a logical rectangle of the back buffer into one scanout buffer,
 * applying rotation and pixel format. */
static void cn1Blit(CN1Scanout* b, CN1EmbRect r) {
    const unsigned char* src;
    int srcStride, x, y;
    if (b->map == 0 || r.w <= 0 || r.h <= 0) {
        return;
    }
#ifdef CN1_EMBEDDED_GPU
    /* cn1Present has just read the damage back from the GPU; get_data would
     * read the whole window again and mark its texture stale. */
    src = cn1WindowG.surface->gpu ? (const unsigned char*) cn1WindowG.surface->data
            : cairo_image_surface_get_data(cn1WindowG.surface);
#else
    src = cairo_image_surface_get_data(cn1WindowG.surface);
#endif
    srcStride = cairo_image_surface_get_stride(cn1WindowG.surface);
    if (cn1Rotation == 0 && b->bpp == 32 && cn1FbRedOffset == 16) {
        for (y = 0; y < r.h; y++) {
            memcpy(b->map + (size_t) (r.y + y) * b->pitch + (size_t) r.x * 4,
                    src + (size_t) (r.y + y) * srcStride + (size_t) r.x * 4, (size_t) r.w * 4);
        }
        return;
    }
    for (y = r.y; y < r.y + r.h; y++) {
        const uint32_t* row = (const uint32_t*) (src + (size_t) y * srcStride);
        for (x = r.x; x < r.x + r.w; x++) {
            int px, py;
            cn1ToPhysical(x, y, &px, &py);
            if (b->bpp == 32) {
                ((uint32_t*) (b->map + (size_t) py * b->pitch))[px] = cn1ToTarget32(row[x]);
            } else {
                ((uint16_t*) (b->map + (size_t) py * b->pitch))[px] = cn1ToRgb565(row[x]);
            }
        }
    }
}

/* A 12x19 arrow: 1 = black outline, 2 = white fill. */
static const char* const cn1CursorShape[CN1_CURSOR_H] = {
    "1           ", "11          ", "121         ", "1221        ", "12221       ",
    "122221      ", "1222221     ", "12222221    ", "122222221   ", "1222222221  ",
    "12222222221 ", "122222111111", "1221221     ", "121 1221    ", "11  1221    ",
    "1    1221   ", "     1221   ", "      1221  ", "       11   " };

static void cn1DrawCursor(CN1Scanout* b, int cx, int cy) {
    int x, y;
    for (y = 0; y < CN1_CURSOR_H; y++) {
        for (x = 0; x < CN1_CURSOR_W; x++) {
            char c = cn1CursorShape[y][x];
            int lx = cx + x, ly = cy + y, px, py;
            uint32_t color;
            if (c == ' ' || lx < 0 || ly < 0 || lx >= cn1LogicalW || ly >= cn1LogicalH) continue;
            color = c == '1' ? 0xff000000u : 0xffffffffu;
            cn1ToPhysical(lx, ly, &px, &py);
            if (b->bpp == 32) {
                ((uint32_t*) (b->map + (size_t) py * b->pitch))[px] = color;
            } else {
                ((uint16_t*) (b->map + (size_t) py * b->pitch))[px] = cn1ToRgb565(color);
            }
        }
    }
    b->cursor.x = cx;
    b->cursor.y = cy;
    b->cursor.w = CN1_CURSOR_W;
    b->cursor.h = CN1_CURSOR_H;
}

/* Brings the screen up to date with `damage` (logical coordinates). Called on
 * the EDT after a frame, and from the input thread when only the cursor moved. */
static void cn1Present(CN1EmbRect damage) {
    CN1Scanout* b;
    CN1EmbRect copy;
    int i, target;
#ifdef CN1_EMBEDDED_GPU
    int gpu = cn1WindowG.surface != 0 && cn1WindowG.surface->gpu;
    if (gpu && cn1GpuScansOut()) {
        /* Only the EDT gets here: the cursor moves on its own plane. */
        cn1GpuPresent();
        return;
    }
#endif
    if (cn1BufCount == 0 || cn1WindowG.surface == 0) {
        return;
    }
    damage = cn1RectClip(damage, cn1LogicalW, cn1LogicalH);
    pthread_mutex_lock(&cn1PresentLock);
#ifdef CN1_EMBEDDED_GPU
    /* Offscreen GPU: only what is copied out below is read back. */
    if (!gpu)
#endif
    cairo_surface_flush(cn1WindowG.surface);
    for (i = 0; i < cn1BufCount; i++) {
        cn1Buf[i].stale = cn1RectUnion(cn1Buf[i].stale, damage);
    }
#ifdef CN1_EMBEDDED_HAS_DRM
    if (cn1Output == OUT_KMS) {
        cn1KmsWaitFlip();
    }
#endif
    target = cn1BufCount == 2 ? 1 - cn1Front : 0;
    b = &cn1Buf[target];
    copy = b->stale;
    if (b->cursor.w > 0) {
        copy = cn1RectUnion(copy, b->cursor);
        b->cursor.w = 0;
    }
    copy = cn1RectClip(copy, cn1LogicalW, cn1LogicalH);
#ifdef CN1_EMBEDDED_GPU
    if (gpu) {
        cn1GpuReadRect(cn1WindowG.surface, copy.x, copy.y, copy.w, copy.h);
    }
#endif
    cn1Blit(b, copy);
    b->stale.w = 0;
    b->stale.h = 0;
    if (cn1CursorEnabled) {
        cn1DrawCursor(b, cn1CursorX, cn1CursorY);
    }
#ifdef CN1_EMBEDDED_HAS_DRM
    if (cn1Output == OUT_KMS) {
        if (drmModePageFlip(cn1DrmFd, cn1DrmCrtc, b->fbId, DRM_MODE_PAGE_FLIP_EVENT, 0) == 0) {
            cn1FlipPending = 1;
            cn1Front = target;
        } else {
            /* Flip refused (no vblank support): show it directly. */
            drmModeSetCrtc(cn1DrmFd, cn1DrmCrtc, b->fbId, 0, 0, &cn1DrmConnector, 1, &cn1DrmMode);
            cn1Front = target;
        }
    }
#endif
    pthread_mutex_unlock(&cn1PresentLock);
}

static void cn1RestoreScreen(void) {
#ifdef CN1_EMBEDDED_GPU
    cn1GpuShutdown();
#endif
#ifdef CN1_EMBEDDED_HAS_DRM
    if (cn1DrmFd >= 0) {
        cn1KmsWaitFlip();
        if (cn1DrmSavedCrtc != 0) {
            drmModeSetCrtc(cn1DrmFd, cn1DrmSavedCrtc->crtc_id, cn1DrmSavedCrtc->buffer_id,
                    cn1DrmSavedCrtc->x, cn1DrmSavedCrtc->y, &cn1DrmConnector, 1, &cn1DrmSavedCrtc->mode);
        }
    }
#endif
    cn1ConsoleRestore();
}

/* ---------------------------------------------------------------- input */

#define CN1_MAX_INPUT 32

typedef struct {
    int fd;
    char name[64];
    int touch, pointer, keyboard, rotary;
    int absMinX, absMaxX, absMinY, absMaxY;
    int mt;                     /* multitouch protocol B */
    int slot;
    int slotId[10];
    int slotX[10], slotY[10];
    int stX, stY, stDown;       /* single-touch state */
    int down;                   /* primary contact currently pressed */
    int lastX, lastY;
    double pinchDist;
    int pinching;
} CN1InputDev;

static CN1InputDev cn1Inputs[CN1_MAX_INPUT];
static int cn1InputCount = 0;
static int cn1InotifyFd = -1;
static int cn1PointerButtons = 0;

#define CN1_BIT(arr, b) (((arr)[(b) / (8 * sizeof(long))] >> ((b) % (8 * sizeof(long)))) & 1)
#define CN1_LONGS(n) (((n) + 8 * sizeof(long) - 1) / (8 * sizeof(long)))

static void cn1OpenInput(const char* path) {
    unsigned long evBits[CN1_LONGS(EV_MAX + 1)];
    unsigned long keyBits[CN1_LONGS(KEY_MAX + 1)];
    unsigned long absBits[CN1_LONGS(ABS_MAX + 1)];
    unsigned long relBits[CN1_LONGS(REL_MAX + 1)];
    CN1InputDev* d;
    int fd, i;
    struct input_absinfo ax, ay;
    if (cn1InputCount >= CN1_MAX_INPUT) {
        return;
    }
    for (i = 0; i < cn1InputCount; i++) {
        /* Already open (inotify can report a node twice). */
        char link[64];
        snprintf(link, sizeof(link), "%s", path);
        if (strcmp(cn1Inputs[i].name, link) == 0) return;
    }
    fd = open(path, O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    if (fd < 0) {
        return;
    }
    memset(evBits, 0, sizeof(evBits));
    memset(keyBits, 0, sizeof(keyBits));
    memset(absBits, 0, sizeof(absBits));
    memset(relBits, 0, sizeof(relBits));
    ioctl(fd, EVIOCGBIT(0, sizeof(evBits)), evBits);
    ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(keyBits)), keyBits);
    ioctl(fd, EVIOCGBIT(EV_ABS, sizeof(absBits)), absBits);
    ioctl(fd, EVIOCGBIT(EV_REL, sizeof(relBits)), relBits);
    d = &cn1Inputs[cn1InputCount];
    memset(d, 0, sizeof(*d));
    d->fd = fd;
    snprintf(d->name, sizeof(d->name), "%s", path);
    for (i = 0; i < 10; i++) d->slotId[i] = -1;
    if (CN1_BIT(evBits, EV_ABS) && CN1_BIT(absBits, ABS_MT_POSITION_X)) {
        d->touch = 1;
        d->mt = 1;
        ioctl(fd, EVIOCGABS(ABS_MT_POSITION_X), &ax);
        ioctl(fd, EVIOCGABS(ABS_MT_POSITION_Y), &ay);
    } else if (CN1_BIT(evBits, EV_ABS) && CN1_BIT(absBits, ABS_X) && CN1_BIT(keyBits, BTN_TOUCH)) {
        d->touch = 1;
        ioctl(fd, EVIOCGABS(ABS_X), &ax);
        ioctl(fd, EVIOCGABS(ABS_Y), &ay);
    }
    if (d->touch) {
        d->absMinX = ax.minimum; d->absMaxX = ax.maximum > ax.minimum ? ax.maximum : ax.minimum + 1;
        d->absMinY = ay.minimum; d->absMaxY = ay.maximum > ay.minimum ? ay.maximum : ay.minimum + 1;
    }
    if (CN1_BIT(evBits, EV_REL) && CN1_BIT(relBits, REL_X) && CN1_BIT(keyBits, BTN_LEFT)) {
        d->pointer = 1;
    } else if (CN1_BIT(evBits, EV_REL) && (CN1_BIT(relBits, REL_DIAL) || CN1_BIT(relBits, REL_WHEEL) || CN1_BIT(relBits, REL_X))) {
        d->rotary = 1;
    }
    if (CN1_BIT(evBits, EV_KEY) && (CN1_BIT(keyBits, KEY_A) || CN1_BIT(keyBits, KEY_ENTER) || CN1_BIT(keyBits, KEY_UP))) {
        d->keyboard = 1;
    }
    if (!d->touch && !d->pointer && !d->keyboard && !d->rotary) {
        close(fd);
        return;
    }
    if (getenv("CN1_INPUT_NO_GRAB") == 0) {
        ioctl(fd, EVIOCGRAB, 1);
    }
    cn1HasTouch |= d->touch;
    cn1HasPointer |= d->pointer;
    cn1InputCount++;
}

static void cn1ScanInputs(void) {
    DIR* dir = opendir("/dev/input");
    struct dirent* e;
    char path[300];
    if (dir == 0) {
        return;
    }
    while ((e = readdir(dir)) != 0) {
        if (strncmp(e->d_name, "event", 5) == 0) {
            snprintf(path, sizeof(path), "/dev/input/%s", e->d_name);
            cn1OpenInput(path);
        }
    }
    closedir(dir);
}

static void cn1CloseInput(int i) {
    close(cn1Inputs[i].fd);
    cn1Inputs[i] = cn1Inputs[cn1InputCount - 1];
    cn1InputCount--;
}

/* Touch panel coordinates -> logical, through calibration and rotation. */
static void cn1TouchToLogical(CN1InputDev* d, int rx, int ry, int* lx, int* ly) {
    long px, py, t;
    if (getenv("CN1_TOUCH_SWAP_XY") != 0) {
        t = rx; rx = ry; ry = (int) t;
    }
    px = (long) (rx - d->absMinX) * (cn1PhysW - 1) / (d->absMaxX - d->absMinX);
    py = (long) (ry - d->absMinY) * (cn1PhysH - 1) / (d->absMaxY - d->absMinY);
    if (getenv("CN1_TOUCH_INVERT_X") != 0) px = cn1PhysW - 1 - px;
    if (getenv("CN1_TOUCH_INVERT_Y") != 0) py = cn1PhysH - 1 - py;
    cn1ToLogical((int) px, (int) py, lx, ly);
}

/* US layout. Printable keys push their character (shifted when Shift is held);
 * the rest push the X keysym the desktop port uses, which LinuxImplementation
 * already maps (arrows to game keys and so on). */
static int cn1KeyCode(int code, int shift) {
    static const char plain[] = "\0\0" "1234567890-=\0\0" "qwertyuiop[]\0\0" "asdfghjkl;'`\0\\" "zxcvbnm,./";
    static const char shifted[] = "\0\0" "!@#$%^&*()_+\0\0" "QWERTYUIOP{}\0\0" "ASDFGHJKL:\"~\0|" "ZXCVBNM<>?";
    switch (code) {
        case KEY_BACKSPACE: return 8;
        case KEY_TAB: return 9;
        case KEY_ENTER: case KEY_KPENTER: case KEY_OK: case KEY_SELECT: return 13;
        case KEY_ESC: case KEY_BACK: return 27;
        case KEY_SPACE: return ' ';
        case KEY_DELETE: return 127;
        case KEY_HOME: return 0xff50;
        case KEY_LEFT: return 0xff51;
        case KEY_UP: return 0xff52;
        case KEY_RIGHT: return 0xff53;
        case KEY_DOWN: return 0xff54;
        case KEY_PAGEUP: return 0xff55;
        case KEY_PAGEDOWN: return 0xff56;
        case KEY_END: return 0xff57;
        case KEY_INSERT: return 0xff63;
        case KEY_MENU: return 0xff67;
        default: break;
    }
    if (code >= KEY_F1 && code <= KEY_F10) return 0xffbe + (code - KEY_F1);
    if (code == KEY_F11) return 0xffc8;
    if (code == KEY_F12) return 0xffc9;
    if (code >= KEY_KP7 && code <= KEY_KPDOT) {
        static const char kp[] = "789-456+1230.";
        return kp[code - KEY_KP7];
    }
    if (code > 0 && code < (int) sizeof(plain) - 1) {
        char c = shift ? shifted[code] : plain[code];
        return c;
    }
    return 0;
}

static int cn1Shift = 0, cn1Ctrl = 0, cn1Alt = 0;

static void cn1HandleKey(int code, int value) {
    int k;
    switch (code) {
        case KEY_LEFTSHIFT: case KEY_RIGHTSHIFT: cn1Shift = value != 0; break;
        case KEY_LEFTCTRL: case KEY_RIGHTCTRL: cn1Ctrl = value != 0; break;
        case KEY_LEFTALT: case KEY_RIGHTALT: cn1Alt = value != 0; break;
        default: break;
    }
    cn1CurrentModifiers = (cn1Shift ? 1 : 0) | (cn1Ctrl ? 2 : 0) | (cn1Alt ? 4 : 0);
    k = cn1KeyCode(code, cn1Shift);
    if (k == 0) {
        return;
    }
    /* value 2 is autorepeat: another press, as a desktop keyboard delivers it. */
    cn1LinuxPushEvent(value ? CN1_EVENT_KEY_PRESSED : CN1_EVENT_KEY_RELEASED, 0, 0, k);
}

static void cn1MoveCursor(void) {
#ifdef CN1_EMBEDDED_GPU
    if (cn1CursorEnabled && cn1GpuScansOut()) {
        int px, py;
        cn1ToPhysical(cn1CursorX, cn1CursorY, &px, &py);
        if (!cn1GpuCursor(1, px, py)) {
            fprintf(stderr, "CN1: the display has no cursor plane; the pointer is not drawn\n");
            cn1CursorEnabled = 0;
        }
        return;
    }
#endif
    if (cn1CursorEnabled) {
        CN1EmbRect none = { 0, 0, 0, 0 };
        cn1Present(none);
    }
}

static int cn1PointerMask(int code) {
    switch (code) {
        case BTN_LEFT: return CN1_PE_MASK_PRIMARY;
        case BTN_RIGHT: return CN1_PE_MASK_SECONDARY;
        case BTN_MIDDLE: return CN1_PE_MASK_MIDDLE;
        case BTN_SIDE: return CN1_PE_MASK_BACK;
        case BTN_EXTRA: return CN1_PE_MASK_FORWARD;
        default: return 0;
    }
}

static void cn1TouchFrame(CN1InputDev* d) {
    int active = 0, first = -1, second = -1, i, lx, ly;
    const int flags = CN1_PE_MASK_PRIMARY | CN1_PE_TOUCH_FLAG;
    if (d->mt) {
        for (i = 0; i < 10; i++) {
            if (d->slotId[i] >= 0) {
                if (first < 0) first = i; else if (second < 0) second = i;
                active++;
            }
        }
    } else {
        active = d->stDown ? 1 : 0;
    }
    if (active >= 2 && d->mt) {
        int ax, ay, bx, by;
        double dx, dy, dist;
        cn1TouchToLogical(d, d->slotX[first], d->slotY[first], &ax, &ay);
        cn1TouchToLogical(d, d->slotX[second], d->slotY[second], &bx, &by);
        dx = bx - ax;
        dy = by - ay;
        dist = dx * dx + dy * dy;
        if (!d->pinching) {
            if (d->down) {
                cn1LinuxPushEvent(CN1_EVENT_POINTER_RELEASED, d->lastX, d->lastY, flags);
                d->down = 0;
            }
            d->pinching = 1;
            d->pinchDist = dist;
            cn1LinuxPushEvent(CN1_EVENT_PINCH_BEGIN, (ax + bx) / 2, (ay + by) / 2, 0);
        } else if (d->pinchDist > 0 && dist > 0) {
            /* Incremental scale, the convention CN1_EVENT_PINCH carries. */
            double ratio = 1.0;
            double r2 = dist / d->pinchDist;
            int it;
            for (it = 0; it < 20; it++) ratio = 0.5 * (ratio + r2 / ratio);  /* sqrt */
            cn1LinuxPushEvent(CN1_EVENT_PINCH, (ax + bx) / 2, (ay + by) / 2, (int) (ratio * CN1_GESTURE_FIXED));
            d->pinchDist = dist;
        }
        return;
    }
    if (d->pinching) {
        d->pinching = 0;
        cn1LinuxPushEvent(CN1_EVENT_PINCH_END, d->lastX, d->lastY, 0);
        if (active == 0) {
            return;
        }
    }
    if (active == 0) {
        if (d->down) {
            cn1LinuxPushEvent(CN1_EVENT_POINTER_RELEASED, d->lastX, d->lastY, flags);
            d->down = 0;
        }
        return;
    }
    if (d->mt) {
        cn1TouchToLogical(d, d->slotX[first], d->slotY[first], &lx, &ly);
    } else {
        cn1TouchToLogical(d, d->stX, d->stY, &lx, &ly);
    }
    if (!d->down) {
        d->down = 1;
        cn1LinuxPushEvent(CN1_EVENT_POINTER_PRESSED, lx, ly, flags);
    } else if (lx != d->lastX || ly != d->lastY) {
        cn1LinuxPushEvent(CN1_EVENT_POINTER_DRAGGED, lx, ly, flags);
    }
    d->lastX = lx;
    d->lastY = ly;
}

static void cn1HandleInput(CN1InputDev* d, const struct input_event* ev, int* relX, int* relY) {
    switch (ev->type) {
        case EV_ABS:
            if (d->mt) {
                switch (ev->code) {
                    case ABS_MT_SLOT: d->slot = ev->value >= 0 && ev->value < 10 ? ev->value : 9; break;
                    case ABS_MT_TRACKING_ID: d->slotId[d->slot] = ev->value; break;
                    case ABS_MT_POSITION_X: d->slotX[d->slot] = ev->value; break;
                    case ABS_MT_POSITION_Y: d->slotY[d->slot] = ev->value; break;
                    default: break;
                }
            } else if (ev->code == ABS_X) {
                d->stX = ev->value;
            } else if (ev->code == ABS_Y) {
                d->stY = ev->value;
            }
            break;
        case EV_REL:
            if (d->pointer && ev->code == REL_X) {
                *relX += ev->value;
            } else if (d->pointer && ev->code == REL_Y) {
                *relY += ev->value;
            } else if (d->pointer && ev->code == REL_WHEEL) {
                cn1LinuxPushEvent(CN1_EVENT_MOUSE_WHEEL, cn1CursorX, cn1CursorY, ev->value * 120);
            } else if (d->pointer && ev->code == REL_HWHEEL) {
                cn1LinuxPushEvent(CN1_EVENT_MOUSE_HWHEEL, cn1CursorX, cn1CursorY, ev->value * 120);
            } else if (d->rotary && ev->value != 0) {
                /* A rotary encoder steps focus: clockwise is down. */
                int k = ev->value > 0 ? 0xff54 : 0xff52;
                int n = ev->value > 0 ? ev->value : -ev->value;
                while (n-- > 0) {
                    cn1LinuxPushEvent(CN1_EVENT_KEY_PRESSED, 0, 0, k);
                    cn1LinuxPushEvent(CN1_EVENT_KEY_RELEASED, 0, 0, k);
                }
            }
            break;
        case EV_KEY:
            if (d->touch && ev->code == BTN_TOUCH) {
                if (!d->mt) d->stDown = ev->value != 0;
            } else if (d->pointer && cn1PointerMask(ev->code) != 0) {
                int mask = cn1PointerMask(ev->code);
                if (ev->value) {
                    cn1PointerButtons |= mask;
                    cn1LinuxPushEvent(CN1_EVENT_POINTER_PRESSED, cn1CursorX, cn1CursorY, mask);
                } else {
                    cn1PointerButtons &= ~mask;
                    cn1LinuxPushEvent(CN1_EVENT_POINTER_RELEASED, cn1CursorX, cn1CursorY, mask);
                }
            } else if (ev->code < BTN_MISC || ev->code >= KEY_OK) {
                cn1HandleKey(ev->code, ev->value);
            }
            break;
        case EV_SYN:
            if (ev->code == SYN_REPORT) {
                if (d->touch) {
                    cn1TouchFrame(d);
                }
                if (d->pointer && (*relX != 0 || *relY != 0)) {
                    int dx = *relX, dy = *relY, x, y;
                    /* Relative motion is in the panel's orientation. */
                    switch (cn1Rotation) {
                        case 90: x = dy; y = -dx; break;
                        case 180: x = -dx; y = -dy; break;
                        case 270: x = -dy; y = dx; break;
                        default: x = dx; y = dy; break;
                    }
                    x += cn1CursorX;
                    y += cn1CursorY;
                    cn1CursorX = x < 0 ? 0 : (x >= cn1LogicalW ? cn1LogicalW - 1 : x);
                    cn1CursorY = y < 0 ? 0 : (y >= cn1LogicalH ? cn1LogicalH - 1 : y);
                    cn1LinuxPushEvent(cn1PointerButtons ? CN1_EVENT_POINTER_DRAGGED : CN1_EVENT_POINTER_HOVER,
                            cn1CursorX, cn1CursorY, cn1PointerButtons);
                    *relX = 0;
                    *relY = 0;
                    cn1MoveCursor();
                }
            }
            break;
        default:
            break;
    }
}

static volatile sig_atomic_t cn1TermRequested = 0;

static void cn1OnTerm(int sig) {
    (void) sig;
    if (cn1TermRequested) {
        /* Second request: the application did not exit. */
        _exit(128 + SIGTERM);
    }
    cn1TermRequested = 1;
}

static void* cn1InputThread(void* arg) {
    struct pollfd fds[CN1_MAX_INPUT + 1];
    struct input_event evs[64];
    int relX[CN1_MAX_INPUT], relY[CN1_MAX_INPUT];
    (void) arg;
    memset(relX, 0, sizeof(relX));
    memset(relY, 0, sizeof(relY));
    while (cn1Running) {
        int n = 0, i, queued = 0;
        for (i = 0; i < cn1InputCount; i++) {
            fds[n].fd = cn1Inputs[i].fd;
            fds[n].events = POLLIN;
            fds[n].revents = 0;
            n++;
        }
        fds[n].fd = cn1InotifyFd;
        fds[n].events = POLLIN;
        fds[n].revents = 0;
        if (poll(fds, (nfds_t) (cn1InotifyFd >= 0 ? n + 1 : n), 200) < 0 && errno != EINTR) {
            usleep(100000);
        }
        if (cn1TermRequested == 1) {
            cn1TermRequested = 2;
            cn1LinuxPushEvent(CN1_EVENT_CLOSE, 0, 0, 0);
            queued = 1;
        }
        for (i = n - 1; i >= 0; i--) {
            ssize_t r;
            if (!(fds[i].revents & (POLLIN | POLLERR | POLLHUP))) {
                continue;
            }
            r = read(cn1Inputs[i].fd, evs, sizeof(evs));
            if (r < 0 && (errno == EAGAIN || errno == EINTR)) {
                continue;
            }
            if (r <= 0) {
                /* Unplugged. */
                cn1CloseInput(i);
                relX[i] = relX[cn1InputCount];
                relY[i] = relY[cn1InputCount];
                continue;
            }
            {
                int k, count = (int) (r / (ssize_t) sizeof(struct input_event));
                for (k = 0; k < count; k++) {
                    cn1HandleInput(&cn1Inputs[i], &evs[k], &relX[i], &relY[i]);
                }
            }
            queued = 1;
        }
        if (cn1InotifyFd >= 0 && (fds[n].revents & POLLIN)) {
            char buf[4096];
            ssize_t r = read(cn1InotifyFd, buf, sizeof(buf));
            (void) r;
            /* udev may not have applied permissions yet; give it a moment. */
            usleep(100000);
            cn1ScanInputs();
        }
        if (queued) {
            cn1Wake();
        }
    }
    return 0;
}

static void cn1StartInput(void) {
    pthread_t t;
    pthread_attr_t attr;
    cn1ScanInputs();
    cn1InotifyFd = inotify_init1(IN_NONBLOCK | IN_CLOEXEC);
    if (cn1InotifyFd >= 0) {
        inotify_add_watch(cn1InotifyFd, "/dev/input", IN_CREATE | IN_ATTRIB);
    }
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    pthread_create(&t, &attr, cn1InputThread, 0);
    pthread_attr_destroy(&attr);
}

/* ------------------------------------------------------------- natives */

static void cn1AtExit(void) {
    cn1Running = 0;
    cn1RestoreScreen();
}

/* CN1_SNAPSHOT=file.png writes frame CN1_SNAPSHOT_FRAME (default 60) as a PNG,
 * for CI and for checking a board's output remotely. Taken at a flush, so the
 * frame is always complete -- the back buffer at an arbitrary moment can be
 * half way through a paint. */
static void cn1MaybeSnapshot(void) {
    static int frame = 0;
    const char* path = getenv("CN1_SNAPSHOT");
    if (path == 0 || *path == 0) {
        return;
    }
    if (++frame == cn1EnvInt("CN1_SNAPSHOT_FRAME", 60)) {
        cairo_surface_flush(cn1WindowG.surface);
        cairo_surface_write_to_png(cn1WindowG.surface, path);
    }
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_isEmbeddedDisplay___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return JAVA_TRUE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_initDisplay___java_lang_String_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT title, JAVA_INT width, JAVA_INT height) {
    const char* want;
    struct sigaction sa;
    char sizeSpec[32];
    (void) title;
    if (cn1Initialized) {
        return;
    }
    cn1Initialized = 1;
    cn1LinuxInstallFaultHandlers();
    if (pipe(cn1WakePipe) == 0) {
        fcntl(cn1WakePipe[0], F_SETFL, O_NONBLOCK);
        fcntl(cn1WakePipe[1], F_SETFL, O_NONBLOCK);
    }
    cn1Rotation = cn1EnvInt("CN1_ROTATION", CN1_DEFAULT_ROTATION);
    if (cn1Rotation != 90 && cn1Rotation != 180 && cn1Rotation != 270) {
        cn1Rotation = 0;
    }
    cn1Dpi = cn1EnvInt("CN1_DPI", CN1_DEFAULT_DPI);
    snprintf(sizeSpec, sizeof(sizeSpec), "%s", cn1Env("CN1_SIZE", ""));
    if (sizeSpec[0] && strchr(sizeSpec, 'x')) {
        cn1PhysW = atoi(sizeSpec);
        cn1PhysH = atoi(strchr(sizeSpec, 'x') + 1);
    } else if (width > 0 && height > 0) {
        cn1PhysW = width;
        cn1PhysH = height;
    }
    cn1DisplayUp = 1;
    want = cn1HeadlessForced ? "headless" : cn1Env("CN1_DISPLAY", CN1_DEFAULT_DISPLAY);
    cn1Output = OUT_HEADLESS;
#ifdef CN1_EMBEDDED_HAS_DRM
    if ((strcmp(want, "auto") == 0 || strcmp(want, "kms") == 0) && cn1InitKms() == 0) {
        cn1Output = OUT_KMS;
    }
#endif
    if (cn1Output == OUT_HEADLESS && (strcmp(want, "auto") == 0 || strcmp(want, "fbdev") == 0) && cn1InitFbdev() == 0) {
        cn1Output = OUT_FBDEV;
    }
    if (cn1Output == OUT_HEADLESS && strcmp(want, "memory") == 0 && cn1InitMemory() == 0) {
        cn1Output = OUT_MEMORY;
#ifdef CN1_EMBEDDED_GPU
        /* Rendered by the GPU and read back: what CI checks the GPU renderer with. */
        if (cn1GpuInitOffscreen() != 0) {
            fprintf(stderr, "CN1: no usable GPU, using the software renderer\n");
        }
#endif
    }
    if (cn1Output == OUT_HEADLESS && strcmp(want, "headless") != 0 && strcmp(want, "auto") != 0
            && strcmp(want, "memory") != 0) {
        fprintf(stderr, "CN1: display '%s' unavailable, running headless\n", want);
    }
    if (cn1Rotation == 90 || cn1Rotation == 270) {
        cn1LogicalW = cn1PhysH;
        cn1LogicalH = cn1PhysW;
    } else {
        cn1LogicalW = cn1PhysW;
        cn1LogicalH = cn1PhysH;
    }
    if (cn1Dpi <= 0) {
        cn1Dpi = 96;
    }
    cn1ResizeBackBuffer(cn1LogicalW, cn1LogicalH);
#ifdef CN1_EMBEDDED_GPU
    if (cn1GpuActive() && cn1GpuAttachWindow(cn1WindowG.surface) != 0) {
        int scansOut = cn1GpuScansOut();
        fprintf(stderr, "CN1: the GPU cannot render the window, using the software renderer\n");
        cn1GpuShutdown();
        cn1WindowG.surface->gpu = 0;
#ifdef CN1_EMBEDDED_HAS_DRM
        if (scansOut && cn1KmsDumbBuffers() != 0) {
            cn1Output = OUT_HEADLESS;
        }
#else
        (void) scansOut;
#endif
    }
#endif
    atexit(cn1AtExit);
    {
        static const char* names[] = { "headless", "memory", "fbdev", "kms" };
        const char* renderer = "software";
#ifdef CN1_EMBEDDED_GPU
        if (cn1WindowG.surface != 0 && cn1WindowG.surface->gpu) {
            renderer = "gpu";
        }
#endif
        fprintf(stderr, "CN1: embedded display %s %dx%d rotation %d dpi %d renderer %s\n",
                names[cn1Output], cn1LogicalW, cn1LogicalH, cn1Rotation, cn1Dpi, renderer);
    }
    if (cn1Output == OUT_KMS || cn1Output == OUT_FBDEV) {
        cn1ConsoleGraphics();
        memset(&sa, 0, sizeof(sa));
        sa.sa_handler = cn1OnTerm;
        sigemptyset(&sa.sa_mask);
        sigaction(SIGTERM, &sa, 0);
        sigaction(SIGINT, &sa, 0);
    }
    if (cn1Output != OUT_HEADLESS && cn1Output != OUT_MEMORY) {
        cn1StartInput();
        cn1CursorX = cn1LogicalW / 2;
        cn1CursorY = cn1LogicalH / 2;
        cn1CursorEnabled = cn1EnvInt("CN1_CURSOR", cn1HasPointer && !cn1HasTouch ? 1 : 0);
#ifdef CN1_EMBEDDED_GPU
        if (cn1GpuScansOut()) {
            cn1MoveCursor();
        }
#endif
    }
    {
        CN1EmbRect all = { 0, 0, cn1LogicalW, cn1LogicalH };
        cn1Present(all);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mainWindowSetTitle___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT title) {
    (void) title;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_currentModifiers___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1CurrentModifiers;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_menuSetCommands___java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT spec) {
    (void) spec;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_getDisplayWidth___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1LogicalW;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_getDisplayHeight___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1LogicalH;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_screenDpi___R_int(CODENAME_ONE_THREAD_STATE) {
    return cn1Dpi > 0 ? cn1Dpi : 96;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_isHighContrastEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_isReduceMotionEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_isScreenReaderEnabled___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return JAVA_FALSE;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_isTouchDevice___R_boolean(CODENAME_ONE_THREAD_STATE) {
    return cn1HasTouch ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_getWindowGraphics___R_long(CODENAME_ONE_THREAD_STATE) {
    return (JAVA_LONG) (intptr_t) &cn1WindowG;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_flushGraphics___long_int_int_int_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG graphics, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height) {
    CN1EmbRect r;
    (void) graphics;
    r.x = x;
    r.y = y;
    r.w = width;
    r.h = height;
    /* Parked for the copy and the vblank wait so a collection does not stall on
     * this thread; the back buffer is not touched by Java until we return. */
    CN1_YIELD_THREAD;
    cn1Present(r);
    cn1MaybeSnapshot();
    CN1_RESUME_THREAD;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_pollEvent___int_1ARRAY_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT out) {
    int scratch[5];
    if (out == JAVA_NULL) {
        return JAVA_FALSE;
    }
    if (cn1LinuxPopEvent(scratch)) {
        JAVA_INT* arr = (JAVA_INT*) CN1_ARRAY_DATA(out);
        int len = (int) (*(JAVA_ARRAY) out).length;
        if (len >= 4) {
            arr[0] = scratch[0];
            arr[1] = scratch[1];
            arr[2] = scratch[2];
            arr[3] = scratch[3];
            if (len >= 5) {
                arr[4] = scratch[4];
            }
            return JAVA_TRUE;
        }
    }
    return JAVA_FALSE;
}

/* No assistive technology runs on an appliance screen; the semantic tree the
 * desktop mirrors into AT-SPI has nowhere to go. */
JAVA_VOID com_codename1_impl_linux_LinuxNative_accessibilityBegin__(CODENAME_ONE_THREAD_STATE) {
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_accessibilityNode___long_long_java_lang_String_java_lang_String_java_lang_String_java_lang_String_int_int_int_int_int(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG id, JAVA_LONG parentId, JAVA_OBJECT role, JAVA_OBJECT label,
        JAVA_OBJECT description, JAVA_OBJECT value, JAVA_INT x, JAVA_INT y, JAVA_INT width, JAVA_INT height, JAVA_INT flags) {
    (void) id; (void) parentId; (void) role; (void) label; (void) description; (void) value;
    (void) x; (void) y; (void) width; (void) height; (void) flags;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_accessibilityAction___long_java_lang_String_int_java_lang_String(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG nodeId, JAVA_OBJECT actionId, JAVA_INT actionHash, JAVA_OBJECT label) {
    (void) nodeId; (void) actionId; (void) actionHash; (void) label;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_accessibilityEnd___int(CODENAME_ONE_THREAD_STATE, JAVA_INT changeType) {
    (void) changeType;
}

/* The main thread's loop body: wait (parked) until the input thread queued
 * something or 100ms passed, then return so Java drains the queue. */
JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_pumpMessages___R_boolean(CODENAME_ONE_THREAD_STATE) {
    struct pollfd p;
    char buf[64];
    if (cn1WakePipe[0] < 0) {
        CN1_YIELD_THREAD;
        usleep(10000);
        CN1_RESUME_THREAD;
        return JAVA_TRUE;
    }
    p.fd = cn1WakePipe[0];
    p.events = POLLIN;
    p.revents = 0;
    CN1_YIELD_THREAD;
    if (poll(&p, 1, 100) > 0) {
        while (read(cn1WakePipe[0], buf, sizeof(buf)) > 0) {
        }
    }
    CN1_RESUME_THREAD;
    return JAVA_TRUE;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_enableHeadlessScreenshot___java_lang_String_int_int(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT path, JAVA_INT width, JAVA_INT height) {
    (void) path;
    cn1HeadlessForced = 1;
    /* Sizes only a display that is not up yet; the buffers are sized from them. */
    if (cn1DisplayUp) return;
    if (width > 0) cn1PhysW = width;
    if (height > 0) cn1PhysH = height;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_captureWindowToPngBytes___R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE) {
    unsigned char* data = 0;
    int len = 0;
    JAVA_OBJECT arr;
    if (!cn1WindowG.surface || !cn1LinuxSurfaceToPng(cn1WindowG.surface, &data, &len)) {
        return JAVA_NULL;
    }
    arr = cn1LinuxNewByteArray(threadStateData, data, len);
    free(data);
    return arr;
}

/* No window chrome on an appliance: no title bar to inset for, no drag regions. */
JAVA_VOID com_codename1_impl_linux_LinuxNative_mainWindowTitleBarInsets___int_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT result) {
    if (result != JAVA_NULL) {
        memset(CN1_ARRAY_DATA(result), 0, sizeof(JAVA_INT) * (size_t) (*(JAVA_ARRAY) result).length);
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_mainWindowTitleBarRegions___int_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT regions) {
    (void) regions;
}

/* An appliance has no desktop colour scheme; the application's theme decides. */
JAVA_INT com_codename1_impl_linux_LinuxNative_systemColorScheme___R_int(CODENAME_ONE_THREAD_STATE) {
    return -1;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_nativeThemeSettings___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    return newStringFromCString(threadStateData, "");
}

#endif /* CN1_LINUX_EMBEDDED */

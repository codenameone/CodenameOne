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
 * The bus and pin access behind com.codename1.impl.KernelHardware, shared by
 * the Linux port (cn1_linux_hardware.c) and the headless backend
 * (vm/backend/native/cn1_backend_hardware.c). Each defines the natives under
 * its own class's name and forwards here: ParparVM names a native after its
 * class, and the native-signature gate reads literal names. The translator
 * emits this header into every Linux, Windows and backend project.
 *
 * Bus and pin access for com.codename1.hardware on Linux: GPIO through the
 * GPIO character device (uapi v2, kernel 5.10+), I2C through i2c-dev, SPI
 * through spidev, serial ports through termios, PWM and backlight through
 * sysfs, CAN through SocketCAN and the watchdog through /dev/watchdog.
 *
 * Only kernel uapi headers are used -- no libgpiod, no libi2c -- so the unit
 * builds on glibc and musl alike and adds no runtime dependency. GTK-free: it
 * is compiled into the desktop and the embedded Linux targets.
 *
 * Every native returns a handle or a result >= 0, and -errno on failure; the
 * Java side (KernelHardware) turns a negative value into a HardwareException
 * carrying that errno. A timeout is reported as -ETIMEDOUT. Handles index a
 * small table here so a stale or forged handle from Java can never reach a
 * random file descriptor.
 
 */

#ifndef CN1_HARDWARE_IMPL_H
#define CN1_HARDWARE_IMPL_H

#include "cn1_globals.h"
#include <errno.h>
#include <stdlib.h>
#include <string.h>

#if defined(__linux__)
#include <errno.h>
#include <fcntl.h>
#include <poll.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <termios.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <net/if.h>
#include <linux/gpio.h>
#include <linux/i2c.h>
#include <linux/i2c-dev.h>
#include <linux/spi/spidev.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <linux/watchdog.h>

/* A malloc'd copy of a Java string; stringToUTF8's buffer is per thread and
 * reused by the next call. */
static char* cn1HwJStrDup(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT value) {
    const char* utf8 = value == JAVA_NULL ? 0 : stringToUTF8(threadStateData, value);
    char* copy;
    size_t length;
    if (utf8 == 0) {
        return 0;
    }
    length = strlen(utf8);
    copy = (char*) malloc(length + 1);
    if (copy != 0) {
        memcpy(copy, utf8, length + 1);
    }
    return copy;
}

#define CN1_HW_KEEP_ALIVE(obj) __asm__ __volatile__("" : : "r"(obj))

enum { HW_FREE = 0, HW_GPIO, HW_I2C, HW_SPI, HW_SERIAL, HW_PWM, HW_CAN, HW_WATCHDOG };

#define CN1_HW_SLOTS 256

typedef struct {
    int kind;
    int fd;
    int chip;      /* PWM: chip number */
    int channel;   /* PWM: channel; I2C: address; SPI: bits per word */
    int speed;     /* SPI: clock rate */
} CN1HwSlot;

static CN1HwSlot cn1HwSlots[CN1_HW_SLOTS];
static pthread_mutex_t cn1HwLock = PTHREAD_MUTEX_INITIALIZER;

static int cn1HwAlloc(int kind, int fd) {
    int i;
    pthread_mutex_lock(&cn1HwLock);
    for (i = 1; i < CN1_HW_SLOTS; i++) {
        if (cn1HwSlots[i].kind == HW_FREE) {
            memset(&cn1HwSlots[i], 0, sizeof(CN1HwSlot));
            cn1HwSlots[i].kind = kind;
            cn1HwSlots[i].fd = fd;
            pthread_mutex_unlock(&cn1HwLock);
            return i;
        }
    }
    pthread_mutex_unlock(&cn1HwLock);
    return -EMFILE;
}

/* Copies the slot out under the lock; returns 0 if the handle is not a live
 * slot of the expected kind. */
static int cn1HwGet(int h, int kind, CN1HwSlot* out) {
    int ok = 0;
    if (h <= 0 || h >= CN1_HW_SLOTS) {
        return 0;
    }
    pthread_mutex_lock(&cn1HwLock);
    if (cn1HwSlots[h].kind == kind) {
        *out = cn1HwSlots[h];
        ok = 1;
    }
    pthread_mutex_unlock(&cn1HwLock);
    return ok;
}

static int cn1HwOpenPath(const char* path, int flags) {
    int fd;
    do {
        fd = open(path, flags | O_CLOEXEC);
    } while (fd < 0 && errno == EINTR);
    return fd < 0 ? -errno : fd;
}

static int cn1HwWriteFile(const char* path, const char* value) {
    int fd = cn1HwOpenPath(path, O_WRONLY);
    ssize_t n;
    size_t len = strlen(value);
    if (fd < 0) {
        return fd;
    }
    do {
        n = write(fd, value, len);
    } while (n < 0 && errno == EINTR);
    if (n < 0) {
        int e = errno;
        close(fd);
        return -e;
    }
    close(fd);
    return 0;
}

static long long cn1HwReadNumber(const char* path) {
    char buf[64];
    int fd = cn1HwOpenPath(path, O_RDONLY);
    ssize_t n;
    if (fd < 0) {
        return fd;
    }
    n = read(fd, buf, sizeof(buf) - 1);
    close(fd);
    if (n <= 0) {
        return -EIO;
    }
    buf[n] = 0;
    return strtoll(buf, 0, 10);
}

/* Waits for fd to become readable. Returns 1 ready, -ETIMEDOUT, or -errno. */
static int cn1HwWaitReadable(int fd, int timeoutMs) {
    struct pollfd p;
    int r;
    p.fd = fd;
    p.events = POLLIN;
    p.revents = 0;
    do {
        r = poll(&p, 1, timeoutMs < 0 ? -1 : timeoutMs);
    } while (r < 0 && errno == EINTR);
    if (r < 0) {
        return -errno;
    }
    if (r == 0) {
        return -ETIMEDOUT;
    }
    if (p.revents & (POLLERR | POLLNVAL)) {
        return -EIO;
    }
    return 1;
}

/* ---------------------------------------------------------------- GPIO */

static JAVA_INT cn1HwImpl_hwGpioOpen(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT chip, JAVA_INT line, JAVA_BOOLEAN output,
        JAVA_BOOLEAN high, JAVA_INT bias, JAVA_INT edges) {
    char path[128];
    char* name = cn1HwJStrDup(threadStateData, chip);
    struct gpio_v2_line_request req;
    int chipFd, r, h;
    if (name == 0 || strchr(name, '/') != 0 || line < 0) {
        free(name);
        return -EINVAL;
    }
    snprintf(path, sizeof(path), "/dev/%s", name);
    free(name);
    chipFd = cn1HwOpenPath(path, O_RDWR);
    if (chipFd < 0) {
        return chipFd;
    }
    memset(&req, 0, sizeof(req));
    req.offsets[0] = (unsigned) line;
    req.num_lines = 1;
    strncpy(req.consumer, "codenameone", sizeof(req.consumer) - 1);
    if (output) {
        req.config.flags = GPIO_V2_LINE_FLAG_OUTPUT;
        req.config.num_attrs = 1;
        req.config.attrs[0].attr.id = GPIO_V2_LINE_ATTR_ID_OUTPUT_VALUES;
        req.config.attrs[0].attr.values = high ? 1 : 0;
        req.config.attrs[0].mask = 1;
    } else {
        req.config.flags = GPIO_V2_LINE_FLAG_INPUT;
        if (edges & 1) req.config.flags |= GPIO_V2_LINE_FLAG_EDGE_RISING;
        if (edges & 2) req.config.flags |= GPIO_V2_LINE_FLAG_EDGE_FALLING;
    }
    switch (bias) {
        case 1: req.config.flags |= GPIO_V2_LINE_FLAG_BIAS_DISABLED; break;
        case 2: req.config.flags |= GPIO_V2_LINE_FLAG_BIAS_PULL_UP; break;
        case 3: req.config.flags |= GPIO_V2_LINE_FLAG_BIAS_PULL_DOWN; break;
        default: break;
    }
    r = ioctl(chipFd, GPIO_V2_GET_LINE_IOCTL, &req);
    if (r < 0) {
        int e = errno;
        close(chipFd);
        return -e;
    }
    /* The line fd stays valid without the chip fd. */
    close(chipFd);
    h = cn1HwAlloc(HW_GPIO, req.fd);
    if (h < 0) {
        close(req.fd);
    }
    return h;
}

static JAVA_INT cn1HwImpl_hwGpioRead(CODENAME_ONE_THREAD_STATE, JAVA_INT h) {
    CN1HwSlot s;
    struct gpio_v2_line_values v;
    if (!cn1HwGet(h, HW_GPIO, &s)) {
        return -EBADF;
    }
    memset(&v, 0, sizeof(v));
    v.mask = 1;
    if (ioctl(s.fd, GPIO_V2_LINE_GET_VALUES_IOCTL, &v) < 0) {
        return -errno;
    }
    return (int) (v.bits & 1);
}

static JAVA_INT cn1HwImpl_hwGpioWrite(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_BOOLEAN high) {
    CN1HwSlot s;
    struct gpio_v2_line_values v;
    if (!cn1HwGet(h, HW_GPIO, &s)) {
        return -EBADF;
    }
    memset(&v, 0, sizeof(v));
    v.mask = 1;
    v.bits = high ? 1 : 0;
    if (ioctl(s.fd, GPIO_V2_LINE_SET_VALUES_IOCTL, &v) < 0) {
        return -errno;
    }
    return 0;
}

/* Returns 1 rising, 0 falling, -ETIMEDOUT, or -errno. */
static JAVA_INT cn1HwImpl_hwGpioWaitEdge(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_INT timeoutMs, JAVA_OBJECT tsOut) {
    CN1HwSlot s;
    struct gpio_v2_line_event ev;
    ssize_t n;
    int r;
    if (!cn1HwGet(h, HW_GPIO, &s)) {
        return -EBADF;
    }
    CN1_YIELD_THREAD;
    r = cn1HwWaitReadable(s.fd, timeoutMs);
    if (r == 1) {
        do {
            n = read(s.fd, &ev, sizeof(ev));
        } while (n < 0 && errno == EINTR);
        r = n == (ssize_t) sizeof(ev) ? (ev.id == GPIO_V2_LINE_EVENT_RISING_EDGE ? 1 : 0) : (n < 0 ? -errno : -EIO);
    }
    CN1_RESUME_THREAD;
    if (r >= 0 && tsOut != JAVA_NULL && CN1_ARRAY_LENGTH(tsOut) > 0) {
        ((JAVA_LONG*) CN1_ARRAY_DATA(tsOut))[0] = (JAVA_LONG) ev.timestamp_ns;
    }
    return r;
}

/* ----------------------------------------------------------------- I2C */

static JAVA_INT cn1HwImpl_hwI2cOpen(CODENAME_ONE_THREAD_STATE, JAVA_INT bus, JAVA_INT address) {
    char path[64];
    int fd, h;
    snprintf(path, sizeof(path), "/dev/i2c-%d", bus);
    fd = cn1HwOpenPath(path, O_RDWR);
    if (fd < 0) {
        return fd;
    }
    h = cn1HwAlloc(HW_I2C, fd);
    if (h < 0) {
        close(fd);
        return h;
    }
    pthread_mutex_lock(&cn1HwLock);
    cn1HwSlots[h].channel = address;
    pthread_mutex_unlock(&cn1HwLock);
    return h;
}

static JAVA_INT cn1HwImpl_hwI2cTransfer(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT w, JAVA_INT wl, JAVA_OBJECT rd, JAVA_INT rl) {
    CN1HwSlot s;
    struct i2c_msg msgs[2];
    struct i2c_rdwr_ioctl_data data;
    int count = 0, r;
    if (!cn1HwGet(h, HW_I2C, &s)) {
        return -EBADF;
    }
    /* i2c_msg.len is 16 bits: a longer transfer would run short and report success. */
    if (wl < 0 || rl < 0 || wl > 65535 || rl > 65535
            || (wl > 0 && (w == JAVA_NULL || wl > CN1_ARRAY_LENGTH(w)))
            || (rl > 0 && (rd == JAVA_NULL || rl > CN1_ARRAY_LENGTH(rd)))) {
        return -EINVAL;
    }
    if (wl > 0) {
        msgs[count].addr = (unsigned short) s.channel;
        msgs[count].flags = 0;
        msgs[count].len = (unsigned short) wl;
        msgs[count].buf = (unsigned char*) CN1_ARRAY_DATA(w);
        count++;
    }
    if (rl > 0) {
        msgs[count].addr = (unsigned short) s.channel;
        msgs[count].flags = I2C_M_RD;
        msgs[count].len = (unsigned short) rl;
        msgs[count].buf = (unsigned char*) CN1_ARRAY_DATA(rd);
        count++;
    }
    if (count == 0) {
        return 0;
    }
    data.msgs = msgs;
    data.nmsgs = (unsigned) count;
    r = ioctl(s.fd, I2C_RDWR, &data);
    CN1_HW_KEEP_ALIVE(w);
    CN1_HW_KEEP_ALIVE(rd);
    return r < 0 ? -errno : 0;
}

/* ----------------------------------------------------------------- SPI */

static JAVA_INT cn1HwImpl_hwSpiOpen(CODENAME_ONE_THREAD_STATE, JAVA_INT bus, JAVA_INT cs, JAVA_INT mode, JAVA_INT speed, JAVA_INT bits) {
    char path[64];
    unsigned char m = (unsigned char) mode, b = (unsigned char) (bits > 0 ? bits : 8);
    unsigned int hz = (unsigned int) speed;
    int fd, h;
    snprintf(path, sizeof(path), "/dev/spidev%d.%d", bus, cs);
    fd = cn1HwOpenPath(path, O_RDWR);
    if (fd < 0) {
        return fd;
    }
    if (ioctl(fd, SPI_IOC_WR_MODE, &m) < 0 || ioctl(fd, SPI_IOC_WR_BITS_PER_WORD, &b) < 0
            || ioctl(fd, SPI_IOC_WR_MAX_SPEED_HZ, &hz) < 0) {
        int e = errno;
        close(fd);
        return -e;
    }
    h = cn1HwAlloc(HW_SPI, fd);
    if (h < 0) {
        close(fd);
        return h;
    }
    pthread_mutex_lock(&cn1HwLock);
    cn1HwSlots[h].channel = b;
    cn1HwSlots[h].speed = (int) hz;
    pthread_mutex_unlock(&cn1HwLock);
    return h;
}

static JAVA_INT cn1HwImpl_hwSpiTransfer(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT tx, JAVA_OBJECT rx, JAVA_INT len) {
    CN1HwSlot s;
    struct spi_ioc_transfer t;
    int r;
    if (!cn1HwGet(h, HW_SPI, &s)) {
        return -EBADF;
    }
    if (len < 0 || tx == JAVA_NULL || rx == JAVA_NULL || len > CN1_ARRAY_LENGTH(tx) || len > CN1_ARRAY_LENGTH(rx)) {
        return -EINVAL;
    }
    memset(&t, 0, sizeof(t));
    t.tx_buf = (unsigned long) (uintptr_t) CN1_ARRAY_DATA(tx);
    t.rx_buf = (unsigned long) (uintptr_t) CN1_ARRAY_DATA(rx);
    t.len = (unsigned) len;
    t.speed_hz = (unsigned) s.speed;
    t.bits_per_word = (unsigned char) s.channel;
    r = ioctl(s.fd, SPI_IOC_MESSAGE(1), &t);
    CN1_HW_KEEP_ALIVE(tx);
    CN1_HW_KEEP_ALIVE(rx);
    return r < 0 ? -errno : 0;
}

/* -------------------------------------------------------------- serial */

static speed_t cn1HwBaud(int baud) {
    switch (baud) {
        case 1200: return B1200;
        case 2400: return B2400;
        case 4800: return B4800;
        case 9600: return B9600;
        case 19200: return B19200;
        case 38400: return B38400;
        case 57600: return B57600;
        case 115200: return B115200;
        case 230400: return B230400;
#ifdef B460800
        case 460800: return B460800;
#endif
#ifdef B921600
        case 921600: return B921600;
#endif
#ifdef B1000000
        case 1000000: return B1000000;
#endif
        default: return 0;
    }
}

static JAVA_INT cn1HwImpl_hwSerialOpen(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT dev, JAVA_INT baud, JAVA_INT dataBits, JAVA_INT parity, JAVA_INT stopBits) {
    char* path = cn1HwJStrDup(threadStateData, dev);
    struct termios t;
    speed_t sp = cn1HwBaud(baud);
    int fd, h;
    /* A serial port is a device node: refuse anything else the application
     * names (a FIFO, /dev/mem, a path climbing out of /dev). */
    if (path == 0 || strncmp(path, "/dev/", 5) != 0 || strstr(path, "..") != 0) {
        free(path);
        return -EINVAL;
    }
    if (sp == 0 || dataBits < 5 || dataBits > 8 || stopBits < 1 || stopBits > 2) {
        free(path);
        return -EINVAL;
    }
    fd = cn1HwOpenPath(path, O_RDWR | O_NOCTTY | O_NONBLOCK);
    free(path);
    if (fd < 0) {
        return fd;
    }
    if (tcgetattr(fd, &t) < 0) {
        int e = errno;
        close(fd);
        return -e;
    }
    cfmakeraw(&t);
    cfsetispeed(&t, sp);
    cfsetospeed(&t, sp);
    t.c_cflag &= ~(CSIZE | PARENB | PARODD | CSTOPB);
    t.c_cflag |= CLOCAL | CREAD;
    t.c_cflag |= dataBits == 5 ? CS5 : dataBits == 6 ? CS6 : dataBits == 7 ? CS7 : CS8;
    if (parity == 1) t.c_cflag |= PARENB | PARODD;
    if (parity == 2) t.c_cflag |= PARENB;
    if (stopBits == 2) t.c_cflag |= CSTOPB;
    t.c_cc[VMIN] = 0;
    t.c_cc[VTIME] = 0;
    if (tcsetattr(fd, TCSANOW, &t) < 0) {
        int e = errno;
        close(fd);
        return -e;
    }
    tcflush(fd, TCIOFLUSH);
    h = cn1HwAlloc(HW_SERIAL, fd);
    if (h < 0) {
        close(fd);
    }
    return h;
}

static JAVA_INT cn1HwImpl_hwSerialRead(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT buf, JAVA_INT off, JAVA_INT len, JAVA_INT timeoutMs) {
    CN1HwSlot s;
    ssize_t n = 0;
    int r;
    if (!cn1HwGet(h, HW_SERIAL, &s)) {
        return -EBADF;
    }
    if (buf == JAVA_NULL || off < 0 || len < 0 || off > CN1_ARRAY_LENGTH(buf) - len) {
        return -EINVAL;
    }
    if (len == 0) {
        return 0;
    }
    CN1_YIELD_THREAD;
    r = cn1HwWaitReadable(s.fd, timeoutMs);
    if (r == 1) {
        do {
            n = read(s.fd, (char*) CN1_ARRAY_DATA(buf) + off, (size_t) len);
        } while (n < 0 && errno == EINTR);
        r = n < 0 ? (errno == EAGAIN ? 0 : -errno) : (int) n;
    } else if (r == -ETIMEDOUT) {
        r = 0;
    }
    CN1_RESUME_THREAD;
    CN1_HW_KEEP_ALIVE(buf);
    return r;
}

static JAVA_INT cn1HwImpl_hwSerialWrite(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT buf, JAVA_INT off, JAVA_INT len) {
    CN1HwSlot s;
    int written = 0, err = 0;
    if (!cn1HwGet(h, HW_SERIAL, &s)) {
        return -EBADF;
    }
    if (buf == JAVA_NULL || off < 0 || len < 0 || off > CN1_ARRAY_LENGTH(buf) - len) {
        return -EINVAL;
    }
    CN1_YIELD_THREAD;
    while (written < len) {
        ssize_t n = write(s.fd, (char*) CN1_ARRAY_DATA(buf) + off + written, (size_t) (len - written));
        if (n < 0 && errno == EINTR) {
            continue;
        }
        if (n < 0 && errno == EAGAIN) {
            struct pollfd p;
            p.fd = s.fd;
            p.events = POLLOUT;
            p.revents = 0;
            poll(&p, 1, 1000);
            continue;
        }
        if (n <= 0) {
            err = n < 0 ? errno : EIO;
            break;
        }
        written += (int) n;
    }
    CN1_RESUME_THREAD;
    CN1_HW_KEEP_ALIVE(buf);
    return err ? -err : 0;
}

/* ----------------------------------------------------------------- PWM */

static JAVA_INT cn1HwImpl_hwPwmOpen(CODENAME_ONE_THREAD_STATE, JAVA_INT chip, JAVA_INT channel) {
    char path[128], num[16];
    int r, h;
    snprintf(path, sizeof(path), "/sys/class/pwm/pwmchip%d/pwm%d", chip, channel);
    if (access(path, F_OK) != 0) {
        snprintf(path, sizeof(path), "/sys/class/pwm/pwmchip%d/export", chip);
        snprintf(num, sizeof(num), "%d", channel);
        r = cn1HwWriteFile(path, num);
        if (r < 0 && r != -EBUSY) {
            return r;
        }
        /* udev applies the group permissions asynchronously after export. */
        snprintf(path, sizeof(path), "/sys/class/pwm/pwmchip%d/pwm%d/period", chip, channel);
        for (r = 0; r < 50 && access(path, W_OK) != 0; r++) {
            usleep(10000);
        }
    }
    h = cn1HwAlloc(HW_PWM, -1);
    if (h < 0) {
        return h;
    }
    pthread_mutex_lock(&cn1HwLock);
    cn1HwSlots[h].chip = chip;
    cn1HwSlots[h].channel = channel;
    pthread_mutex_unlock(&cn1HwLock);
    return h;
}

static int cn1HwPwmAttr(CN1HwSlot* s, const char* attr, long long value) {
    char path[128], v[32];
    snprintf(path, sizeof(path), "/sys/class/pwm/pwmchip%d/pwm%d/%s", s->chip, s->channel, attr);
    snprintf(v, sizeof(v), "%lld", value);
    return cn1HwWriteFile(path, v);
}

static JAVA_INT cn1HwImpl_hwPwmConfigure(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_LONG period, JAVA_LONG duty, JAVA_BOOLEAN enabled) {
    CN1HwSlot s;
    int r;
    if (!cn1HwGet(h, HW_PWM, &s)) {
        return -EBADF;
    }
    /* The kernel rejects duty > period at every step, so lower the duty
     * first when shrinking the period and raise the period first when growing. */
    cn1HwPwmAttr(&s, "duty_cycle", 0);
    r = cn1HwPwmAttr(&s, "period", period);
    if (r == 0) r = cn1HwPwmAttr(&s, "duty_cycle", duty);
    if (r == 0) r = cn1HwPwmAttr(&s, "enable", enabled ? 1 : 0);
    return r;
}

/* ----------------------------------------------------------------- CAN */

static JAVA_INT cn1HwImpl_hwCanOpen(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT iface) {
    char* name = cn1HwJStrDup(threadStateData, iface);
    struct sockaddr_can addr;
    unsigned idx;
    int fd, h;
    if (name == 0) {
        return -EINVAL;
    }
    idx = if_nametoindex(name);
    free(name);
    if (idx == 0) {
        return -ENODEV;
    }
    fd = socket(PF_CAN, SOCK_RAW | SOCK_CLOEXEC, CAN_RAW);
    if (fd < 0) {
        return -errno;
    }
    memset(&addr, 0, sizeof(addr));
    addr.can_family = AF_CAN;
    addr.can_ifindex = (int) idx;
    if (bind(fd, (struct sockaddr*) &addr, sizeof(addr)) < 0) {
        int e = errno;
        close(fd);
        return -e;
    }
    h = cn1HwAlloc(HW_CAN, fd);
    if (h < 0) {
        close(fd);
    }
    return h;
}

static JAVA_INT cn1HwImpl_hwCanSend(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_INT id, JAVA_BOOLEAN ext, JAVA_OBJECT data, JAVA_INT len) {
    CN1HwSlot s;
    struct can_frame f;
    ssize_t n;
    if (!cn1HwGet(h, HW_CAN, &s)) {
        return -EBADF;
    }
    if (len < 0 || len > 8 || (len > 0 && (data == JAVA_NULL || len > CN1_ARRAY_LENGTH(data)))) {
        return -EINVAL;
    }
    memset(&f, 0, sizeof(f));
    f.can_id = ext ? (((unsigned) id & CAN_EFF_MASK) | CAN_EFF_FLAG) : ((unsigned) id & CAN_SFF_MASK);
    f.can_dlc = (unsigned char) len;
    if (len > 0) {
        memcpy(f.data, CN1_ARRAY_DATA(data), (size_t) len);
    }
    do {
        n = write(s.fd, &f, sizeof(f));
    } while (n < 0 && errno == EINTR);
    return n == (ssize_t) sizeof(f) ? 0 : (n < 0 ? -errno : -EIO);
}

static JAVA_INT cn1HwImpl_hwCanReceive(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT idOut, JAVA_OBJECT data, JAVA_INT timeoutMs) {
    CN1HwSlot s;
    struct can_frame f;
    ssize_t n = 0;
    int r;
    if (!cn1HwGet(h, HW_CAN, &s)) {
        return -EBADF;
    }
    if (idOut == JAVA_NULL || CN1_ARRAY_LENGTH(idOut) < 2 || data == JAVA_NULL || CN1_ARRAY_LENGTH(data) < 8) {
        return -EINVAL;
    }
    CN1_YIELD_THREAD;
    r = cn1HwWaitReadable(s.fd, timeoutMs);
    if (r == 1) {
        do {
            n = read(s.fd, &f, sizeof(f));
        } while (n < 0 && errno == EINTR);
        r = n == (ssize_t) sizeof(f) ? 0 : (n < 0 ? -errno : -EIO);
    }
    CN1_RESUME_THREAD;
    if (r < 0) {
        return r;
    }
    if (f.can_id & CAN_EFF_FLAG) {
        ((JAVA_INT*) CN1_ARRAY_DATA(idOut))[0] = (JAVA_INT) (f.can_id & CAN_EFF_MASK);
        ((JAVA_INT*) CN1_ARRAY_DATA(idOut))[1] = 1;
    } else {
        ((JAVA_INT*) CN1_ARRAY_DATA(idOut))[0] = (JAVA_INT) (f.can_id & CAN_SFF_MASK);
        ((JAVA_INT*) CN1_ARRAY_DATA(idOut))[1] = 0;
    }
    memcpy(CN1_ARRAY_DATA(data), f.data, f.can_dlc > 8 ? 8 : f.can_dlc);
    return f.can_dlc > 8 ? 8 : f.can_dlc;
}

/* ------------------------------------------------------------ watchdog */

static JAVA_INT cn1HwImpl_hwWatchdogOpen(CODENAME_ONE_THREAD_STATE, JAVA_INT timeoutSeconds) {
    int fd = cn1HwOpenPath("/dev/watchdog", O_WRONLY);
    int t = timeoutSeconds, h;
    if (fd < 0) {
        return fd;
    }
    if (t > 0 && ioctl(fd, WDIOC_SETTIMEOUT, &t) < 0) {
        int e = errno;
        /* Leave it running: closing without the magic byte keeps the countdown. */
        close(fd);
        return -e;
    }
    h = cn1HwAlloc(HW_WATCHDOG, fd);
    if (h < 0) {
        close(fd);
    }
    return h;
}

static JAVA_INT cn1HwImpl_hwWatchdogKick(CODENAME_ONE_THREAD_STATE, JAVA_INT h) {
    CN1HwSlot s;
    int dummy = 0;
    if (!cn1HwGet(h, HW_WATCHDOG, &s)) {
        return -EBADF;
    }
    return ioctl(s.fd, WDIOC_KEEPALIVE, &dummy) < 0 ? -errno : 0;
}

/* --------------------------------------------------------------- close */

static JAVA_VOID cn1HwImpl_hwClose(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_BOOLEAN disarm) {
    CN1HwSlot s;
    if (h <= 0 || h >= CN1_HW_SLOTS) {
        return;
    }
    pthread_mutex_lock(&cn1HwLock);
    s = cn1HwSlots[h];
    cn1HwSlots[h].kind = HW_FREE;
    pthread_mutex_unlock(&cn1HwLock);
    if (s.kind == HW_FREE) {
        return;
    }
    if (s.kind == HW_WATCHDOG && disarm) {
        /* The magic close: drivers without nowayout stop the timer. */
        ssize_t n;
        do {
            n = write(s.fd, "V", 1);
        } while (n < 0 && errno == EINTR);
    }
    if (s.fd >= 0) {
        close(s.fd);
    }
}

/* ----------------------------------------------------------- discovery */

/* Lists /dev entries starting with prefix, newline separated. */
static void cn1HwListDev(const char* prefix, char* out, size_t cap) {
    DIR* d = opendir("/dev");
    struct dirent* e;
    size_t used = 0;
    size_t plen = strlen(prefix);
    out[0] = 0;
    if (!d) {
        return;
    }
    while ((e = readdir(d)) != 0) {
        size_t len = strlen(e->d_name);
        if (strncmp(e->d_name, prefix, plen) != 0 || used + len + 7 >= cap) {
            continue;
        }
        {
            int n = snprintf(out + used, cap - used, "%s\n", e->d_name);
            used = n < 0 ? used : (size_t) n >= cap - used ? cap - 1 : used + (size_t) n;
        }
    }
    closedir(d);
}

static void cn1HwListCan(char* out, size_t cap) {
    DIR* d = opendir("/sys/class/net");
    struct dirent* e;
    size_t used = 0;
    out[0] = 0;
    if (!d) {
        return;
    }
    while ((e = readdir(d)) != 0) {
        char path[300];
        if (e->d_name[0] == '.') {
            continue;
        }
        snprintf(path, sizeof(path), "/sys/class/net/%s/type", e->d_name);
        /* ARPHRD_CAN */
        if (cn1HwReadNumber(path) == 280 && used + strlen(e->d_name) + 2 < cap) {
            {
            int n = snprintf(out + used, cap - used, "%s\n", e->d_name);
            used = n < 0 ? used : (size_t) n >= cap - used ? cap - 1 : used + (size_t) n;
        }
        }
    }
    closedir(d);
}

/* kind: 0 gpiochips, 1 i2c, 2 spidev, 3 serial, 4 can. Newline separated
 * device names, or the empty string. */
static JAVA_OBJECT cn1HwImpl_hwList(CODENAME_ONE_THREAD_STATE, JAVA_INT kind) {
    char buf[8192];
    buf[0] = 0;
    switch (kind) {
        case 0: cn1HwListDev("gpiochip", buf, sizeof(buf)); break;
        case 1: cn1HwListDev("i2c-", buf, sizeof(buf)); break;
        case 2: cn1HwListDev("spidev", buf, sizeof(buf)); break;
        case 3: {
            const char* prefixes[] = { "ttyAMA", "ttyS", "ttyUSB", "ttyACM", "serial", "ttymxc", "ttyO" };
            size_t i, used = 0;
            for (i = 0; i < sizeof(prefixes) / sizeof(prefixes[0]) && used < sizeof(buf) - 1; i++) {
                cn1HwListDev(prefixes[i], buf + used, sizeof(buf) - used);
                used = strlen(buf);
            }
            break;
        }
        case 4: cn1HwListCan(buf, sizeof(buf)); break;
        default: break;
    }
    return newStringFromCString(threadStateData, buf);
}

/* ----------------------------------------------------------- backlight */

static int cn1HwBacklightDir(char* out, size_t cap) {
    DIR* d = opendir("/sys/class/backlight");
    struct dirent* e;
    int found = 0;
    if (!d) {
        return 0;
    }
    while ((e = readdir(d)) != 0) {
        if (e->d_name[0] != '.') {
            snprintf(out, cap, "/sys/class/backlight/%s", e->d_name);
            found = 1;
            break;
        }
    }
    closedir(d);
    return found;
}

/* op 0: current, 1: max, 2: set value. Returns the value, 0 for a set, or -errno. */
static JAVA_INT cn1HwImpl_hwBacklight(CODENAME_ONE_THREAD_STATE, JAVA_INT op, JAVA_INT value) {
    char dir[280], path[320], v[16];
    long long max;
    if (!cn1HwBacklightDir(dir, sizeof(dir))) {
        return -ENODEV;
    }
    snprintf(path, sizeof(path), "%s/max_brightness", dir);
    max = cn1HwReadNumber(path);
    if (op == 1) {
        return (int) max;
    }
    if (op == 0) {
        snprintf(path, sizeof(path), "%s/brightness", dir);
        return (int) cn1HwReadNumber(path);
    }
    if (max < 0) {
        return (int) max;
    }
    if (value < 0) value = 0;
    if (value > max) value = (int) max;
    snprintf(path, sizeof(path), "%s/brightness", dir);
    snprintf(v, sizeof(v), "%d", value);
    return cn1HwWriteFile(path, v);
}

#else /* !__linux__: a backend built for macOS or Windows has no such buses. */

static JAVA_INT cn1HwImpl_hwGpioOpen(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT chip, JAVA_INT line, JAVA_BOOLEAN output, JAVA_BOOLEAN high, JAVA_INT bias, JAVA_INT edges) {
    (void) threadStateData;
    (void) chip;
    (void) line;
    (void) output;
    (void) high;
    (void) bias;
    (void) edges;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwGpioRead(CODENAME_ONE_THREAD_STATE, JAVA_INT h) {
    (void) threadStateData;
    (void) h;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwGpioWrite(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_BOOLEAN high) {
    (void) threadStateData;
    (void) h;
    (void) high;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwGpioWaitEdge(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_INT timeoutMs, JAVA_OBJECT tsOut) {
    (void) threadStateData;
    (void) h;
    (void) timeoutMs;
    (void) tsOut;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwI2cOpen(CODENAME_ONE_THREAD_STATE, JAVA_INT bus, JAVA_INT address) {
    (void) threadStateData;
    (void) bus;
    (void) address;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwI2cTransfer(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT w, JAVA_INT wl, JAVA_OBJECT rd, JAVA_INT rl) {
    (void) threadStateData;
    (void) h;
    (void) w;
    (void) wl;
    (void) rd;
    (void) rl;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwSpiOpen(CODENAME_ONE_THREAD_STATE, JAVA_INT bus, JAVA_INT cs, JAVA_INT mode, JAVA_INT speed, JAVA_INT bits) {
    (void) threadStateData;
    (void) bus;
    (void) cs;
    (void) mode;
    (void) speed;
    (void) bits;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwSpiTransfer(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT tx, JAVA_OBJECT rx, JAVA_INT len) {
    (void) threadStateData;
    (void) h;
    (void) tx;
    (void) rx;
    (void) len;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwSerialOpen(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT dev, JAVA_INT baud, JAVA_INT dataBits, JAVA_INT parity, JAVA_INT stopBits) {
    (void) threadStateData;
    (void) dev;
    (void) baud;
    (void) dataBits;
    (void) parity;
    (void) stopBits;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwSerialRead(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT buf, JAVA_INT off, JAVA_INT len, JAVA_INT timeoutMs) {
    (void) threadStateData;
    (void) h;
    (void) buf;
    (void) off;
    (void) len;
    (void) timeoutMs;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwSerialWrite(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT buf, JAVA_INT off, JAVA_INT len) {
    (void) threadStateData;
    (void) h;
    (void) buf;
    (void) off;
    (void) len;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwPwmOpen(CODENAME_ONE_THREAD_STATE, JAVA_INT chip, JAVA_INT channel) {
    (void) threadStateData;
    (void) chip;
    (void) channel;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwPwmConfigure(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_LONG period, JAVA_LONG duty, JAVA_BOOLEAN enabled) {
    (void) threadStateData;
    (void) h;
    (void) period;
    (void) duty;
    (void) enabled;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwCanOpen(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT iface) {
    (void) threadStateData;
    (void) iface;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwCanSend(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_INT id, JAVA_BOOLEAN ext, JAVA_OBJECT data, JAVA_INT len) {
    (void) threadStateData;
    (void) h;
    (void) id;
    (void) ext;
    (void) data;
    (void) len;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwCanReceive(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_OBJECT idOut, JAVA_OBJECT data, JAVA_INT timeoutMs) {
    (void) threadStateData;
    (void) h;
    (void) idOut;
    (void) data;
    (void) timeoutMs;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwWatchdogOpen(CODENAME_ONE_THREAD_STATE, JAVA_INT timeoutSeconds) {
    (void) threadStateData;
    (void) timeoutSeconds;
    return -ENOSYS;
}

static JAVA_INT cn1HwImpl_hwWatchdogKick(CODENAME_ONE_THREAD_STATE, JAVA_INT h) {
    (void) threadStateData;
    (void) h;
    return -ENOSYS;
}

static JAVA_VOID cn1HwImpl_hwClose(CODENAME_ONE_THREAD_STATE, JAVA_INT h, JAVA_BOOLEAN disarm) {
    (void) threadStateData;
    (void) h;
    (void) disarm;
}

static JAVA_OBJECT cn1HwImpl_hwList(CODENAME_ONE_THREAD_STATE, JAVA_INT kind) {
    (void) kind;
    return newStringFromCString(threadStateData, "");
}

static JAVA_INT cn1HwImpl_hwBacklight(CODENAME_ONE_THREAD_STATE, JAVA_INT op, JAVA_INT value) {
    (void) threadStateData;
    (void) op;
    (void) value;
    return -ENOSYS;
}

#endif /* __linux__ */

#endif /* CN1_HARDWARE_IMPL_H */

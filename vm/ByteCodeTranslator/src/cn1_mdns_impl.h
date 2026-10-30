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
 * The multicast socket behind com.codename1.impl.MdnsEngine, shared by the
 * Linux port (cn1_linux_mdns.c) and the headless backend
 * (vm/backend/native/cn1_backend_mdns.c). Each defines the natives under its
 * own class's name and forwards here, as cn1_hardware_impl.h does.
 *
 * The whole protocol is Java; this is a UDP socket on port 5353 joined to
 * 224.0.0.251 on every IPv4 interface, plus the host's name and addresses. No
 * avahi and no library beyond libc. SO_REUSEPORT lets it share the port with a
 * system responder that may already be running, as every mDNS stack does.
 *
 * Every function answers >= 0 or -errno.
 */

#ifndef CN1_MDNS_IMPL_H
#define CN1_MDNS_IMPL_H

#include "cn1_globals.h"
#include <errno.h>
#include <string.h>

#if defined(__linux__) || defined(__APPLE__)
#include <arpa/inet.h>
#include <ifaddrs.h>
#include <net/if.h>
#include <netinet/in.h>
#include <poll.h>
#include <stdio.h>
#include <sys/socket.h>
#include <unistd.h>

#define CN1_MDNS_GROUP "224.0.0.251"
#define CN1_MDNS_PORT 5353
#define CN1_MDNS_MAX 9000
#define CN1_MDNS_MAX_IFS 16

/* An interface mDNS can use: up, multicast, IPv4 and not loopback. */
static int cn1MdnsUsable(const struct ifaddrs* a) {
    return a->ifa_addr != 0 && a->ifa_addr->sa_family == AF_INET
            && (a->ifa_flags & IFF_UP) && (a->ifa_flags & IFF_MULTICAST)
            && !(a->ifa_flags & IFF_LOOPBACK);
}

/* The IPv4 address of each usable interface, at most max of them. */
static int cn1MdnsInterfaces(struct in_addr* out, int max) {
    struct ifaddrs* list = 0;
    struct ifaddrs* a;
    int n = 0, i;
    if (getifaddrs(&list) != 0) {
        return 0;
    }
    for (a = list; a != 0 && n < max; a = a->ifa_next) {
        struct in_addr addr;
        int seen = 0;
        if (!cn1MdnsUsable(a)) {
            continue;
        }
        addr = ((struct sockaddr_in*) a->ifa_addr)->sin_addr;
        for (i = 0; i < n; i++) {
            if (out[i].s_addr == addr.s_addr) {
                seen = 1;
            }
        }
        if (!seen) {
            out[n++] = addr;
        }
    }
    freeifaddrs(list);
    return n;
}

static JAVA_INT cn1MdnsImpl_open(CODENAME_ONE_THREAD_STATE) {
    struct sockaddr_in bindAddr;
    struct in_addr ifs[CN1_MDNS_MAX_IFS];
    int fd, one = 1, n, i, joined = 0;
    unsigned char ttl = 255, loop = 1;
    (void) threadStateData;
    fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        return -errno;
    }
    setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));
#ifdef SO_REUSEPORT
    setsockopt(fd, SOL_SOCKET, SO_REUSEPORT, &one, sizeof(one));
#endif
    memset(&bindAddr, 0, sizeof(bindAddr));
    bindAddr.sin_family = AF_INET;
    bindAddr.sin_port = htons(CN1_MDNS_PORT);
    bindAddr.sin_addr.s_addr = htonl(INADDR_ANY);
    if (bind(fd, (struct sockaddr*) &bindAddr, sizeof(bindAddr)) != 0) {
        int e = errno;
        close(fd);
        return -e;
    }
    n = cn1MdnsInterfaces(ifs, CN1_MDNS_MAX_IFS);
    for (i = 0; i < n; i++) {
        struct ip_mreq m;
        m.imr_multiaddr.s_addr = inet_addr(CN1_MDNS_GROUP);
        m.imr_interface = ifs[i];
        if (setsockopt(fd, IPPROTO_IP, IP_ADD_MEMBERSHIP, &m, sizeof(m)) == 0) {
            joined++;
        }
    }
    if (joined == 0) {
        /* No usable interface yet (a board still waiting for DHCP): join on
         * the default one, which works once a route appears. */
        struct ip_mreq m;
        m.imr_multiaddr.s_addr = inet_addr(CN1_MDNS_GROUP);
        m.imr_interface.s_addr = htonl(INADDR_ANY);
        setsockopt(fd, IPPROTO_IP, IP_ADD_MEMBERSHIP, &m, sizeof(m));
    }
    setsockopt(fd, IPPROTO_IP, IP_MULTICAST_TTL, &ttl, sizeof(ttl));
    /* Two applications on one host find each other through the loop. */
    setsockopt(fd, IPPROTO_IP, IP_MULTICAST_LOOP, &loop, sizeof(loop));
    return fd;
}

/* Sends once per usable interface, so a board on two networks is seen on both. */
static JAVA_INT cn1MdnsImpl_send(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT packet, JAVA_INT length) {
    struct sockaddr_in to;
    struct in_addr ifs[CN1_MDNS_MAX_IFS];
    unsigned char copy[CN1_MDNS_MAX];
    int n, i, sent = 0, err = 0;
    if (fd < 0 || packet == JAVA_NULL || length <= 0 || length > CN1_MDNS_MAX
            || length > CN1_ARRAY_LENGTH(packet)) {
        return -EINVAL;
    }
    memcpy(copy, CN1_ARRAY_DATA(packet), (size_t) length);
    memset(&to, 0, sizeof(to));
    to.sin_family = AF_INET;
    to.sin_port = htons(CN1_MDNS_PORT);
    to.sin_addr.s_addr = inet_addr(CN1_MDNS_GROUP);
    CN1_YIELD_THREAD;
    n = cn1MdnsInterfaces(ifs, CN1_MDNS_MAX_IFS);
    for (i = 0; i < n; i++) {
        setsockopt(fd, IPPROTO_IP, IP_MULTICAST_IF, &ifs[i], sizeof(ifs[i]));
        if (sendto(fd, copy, (size_t) length, 0, (struct sockaddr*) &to, sizeof(to)) >= 0) {
            sent++;
        } else {
            err = errno;
        }
    }
    if (n == 0) {
        if (sendto(fd, copy, (size_t) length, 0, (struct sockaddr*) &to, sizeof(to)) >= 0) {
            sent++;
        } else {
            err = errno;
        }
    }
    CN1_RESUME_THREAD;
    /* A network that is down is not an error the engine can act on. */
    (void) err;
    return sent > 0 ? length : 0;
}

static JAVA_INT cn1MdnsImpl_receive(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT buffer, JAVA_INT timeoutMs) {
    unsigned char data[CN1_MDNS_MAX];
    struct pollfd p;
    ssize_t n = 0;
    int r;
    if (fd < 0 || buffer == JAVA_NULL) {
        return -EINVAL;
    }
    CN1_YIELD_THREAD;
    p.fd = fd;
    p.events = POLLIN;
    p.revents = 0;
    do {
        r = poll(&p, 1, timeoutMs < 0 ? 0 : timeoutMs);
    } while (r < 0 && errno == EINTR);
    if (r > 0) {
        do {
            n = recv(fd, data, sizeof(data), 0);
        } while (n < 0 && errno == EINTR);
        if (n < 0) {
            n = -errno;
        }
    } else if (r < 0) {
        n = -errno;
    }
    CN1_RESUME_THREAD;
    if (n > 0) {
        if (n > CN1_ARRAY_LENGTH(buffer)) {
            n = CN1_ARRAY_LENGTH(buffer);
        }
        memcpy(CN1_ARRAY_DATA(buffer), data, (size_t) n);
    }
    return (JAVA_INT) n;
}

/* "hostname\naddress\naddress\n". */
static JAVA_OBJECT cn1MdnsImpl_hostInfo(CODENAME_ONE_THREAD_STATE) {
    char out[64 + CN1_MDNS_MAX_IFS * 17];
    char name[64];
    struct in_addr ifs[CN1_MDNS_MAX_IFS];
    size_t used;
    int n, i;
    if (gethostname(name, sizeof(name)) != 0) {
        name[0] = 0;
    }
    name[sizeof(name) - 1] = 0;
    snprintf(out, sizeof(out), "%s\n", name);
    used = strlen(out);
    n = cn1MdnsInterfaces(ifs, CN1_MDNS_MAX_IFS);
    for (i = 0; i < n && used + 17 < sizeof(out); i++) {
        unsigned char* b = (unsigned char*) &ifs[i].s_addr;
        snprintf(out + used, sizeof(out) - used, "%u.%u.%u.%u\n", b[0], b[1], b[2], b[3]);
        used = strlen(out);
    }
    return newStringFromCString(threadStateData, out);
}

static JAVA_VOID cn1MdnsImpl_close(CODENAME_ONE_THREAD_STATE, JAVA_INT fd) {
    (void) threadStateData;
    if (fd >= 0) {
        close(fd);
    }
}

#else /* Windows: the engine reports that it could not open a socket. */

static JAVA_INT cn1MdnsImpl_open(CODENAME_ONE_THREAD_STATE) {
    (void) threadStateData;
    return -ENOSYS;
}

static JAVA_INT cn1MdnsImpl_send(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT packet, JAVA_INT length) {
    (void) threadStateData; (void) fd; (void) packet; (void) length;
    return -ENOSYS;
}

static JAVA_INT cn1MdnsImpl_receive(CODENAME_ONE_THREAD_STATE, JAVA_INT fd, JAVA_OBJECT buffer, JAVA_INT timeoutMs) {
    (void) threadStateData; (void) fd; (void) buffer; (void) timeoutMs;
    return -ENOSYS;
}

static JAVA_OBJECT cn1MdnsImpl_hostInfo(CODENAME_ONE_THREAD_STATE) {
    return newStringFromCString(threadStateData, "");
}

static JAVA_VOID cn1MdnsImpl_close(CODENAME_ONE_THREAD_STATE, JAVA_INT fd) {
    (void) threadStateData; (void) fd;
}

#endif

#endif /* CN1_MDNS_IMPL_H */

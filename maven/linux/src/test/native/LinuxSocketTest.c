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
/* Local POSIX regression test. Includes the production bridge with only the
 * Java ABI/GC hooks stubbed; run the application separately to test ParparVM GC. */
#define _GNU_SOURCE
#include <stdint.h>
#include <assert.h>
#include <time.h>
#include <dirent.h>
#include <signal.h>
#include <stdatomic.h>
#define CN1_LINUX_H
#define CODENAME_ONE_THREAD_STATE void* threadStateData
typedef void* JAVA_OBJECT;
typedef int64_t JAVA_LONG;
typedef int JAVA_INT;
typedef int JAVA_BOOLEAN;
#define JAVA_VOID void
#define JAVA_NULL NULL
#define JAVA_TRUE 1
#define JAVA_FALSE 0
#define CN1_YIELD_THREAD do { if (threadStateData) { atomic_store((atomic_int*) threadStateData, 1); } } while (0)
#define CN1_RESUME_THREAD ((void) 0)
typedef struct { int length; unsigned char data[]; } TestArray;
#define CN1_ARRAY_DATA(a) (((TestArray*) (a))->data)
#define CN1_ARRAY_LENGTH(a) (((TestArray*) (a))->length)
#include "../../../../../Ports/LinuxPort/nativeSources/cn1_linux_socket.c"
JAVA_OBJECT newStringFromCString(void* state, const char* s) { return strdup(s); }
const char* stringToUTF8(void* state, JAVA_OBJECT s) { return (const char*) s; }
#define ACCEPT com_codename1_impl_linux_LinuxNative_socketListenLoopback___int_R_long
#define STOP com_codename1_impl_linux_LinuxNative_socketStopListening___int
#define CONNECT com_codename1_impl_linux_LinuxNative_socketConnect___java_lang_String_int_int_R_long
#define READ com_codename1_impl_linux_LinuxNative_socketRead___long_byte_1ARRAY_int_int_R_int
#define WRITE com_codename1_impl_linux_LinuxNative_socketWrite___long_byte_1ARRAY_int_int_R_int
#define CLOSE com_codename1_impl_linux_LinuxNative_socketClose___long
#define CONNECTED com_codename1_impl_linux_LinuxNative_socketConnected___long_R_boolean
#define ERROR com_codename1_impl_linux_LinuxNative_socketErrorCode___long_R_int

static volatile sig_atomic_t receivedSignals;
static void interruption(int signalNumber) { (void) signalNumber; receivedSignals++; }

typedef struct { int port; JAVA_LONG token; TestArray* array; int result; atomic_int parked; } Job;
static void* accepting(void* p) { Job* j = p; j->token = ACCEPT(&j->parked, j->port); return NULL; }
static void* reading(void* p) { Job* j = p; j->result = READ(&j->parked, j->token, j->array, 0, j->array->length); return NULL; }
static void* writing(void* p) { Job* j = p; j->result = WRITE(&j->parked, j->token, j->array, 0, j->array->length); return NULL; }
static void join(pthread_t t) {
    struct timespec end;
    clock_gettime(CLOCK_REALTIME, &end);
    end.tv_sec += 4;
    assert(pthread_timedjoin_np(t, NULL, &end) == 0);
}
static TestArray* array(int size) {
    TestArray* a = calloc(1, sizeof(TestArray) + size);
    assert(a);
    a->length = size;
    return a;
}
static struct sockaddr_in address(int port, uint32_t ip) {
    struct sockaddr_in a;
    memset(&a, 0, sizeof(a));
    a.sin_family = AF_INET;
    a.sin_port = htons(port);
    a.sin_addr.s_addr = htonl(ip);
    return a;
}
static int freePort(void) {
    struct sockaddr_in a = address(0, INADDR_LOOPBACK);
    socklen_t size = sizeof(a);
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    assert(bind(fd, (struct sockaddr*) &a, sizeof(a)) == 0);
    assert(getsockname(fd, (struct sockaddr*) &a, &size) == 0);
    close(fd);
    return ntohs(a.sin_port);
}
static void waitParked(Job* job) {
    int i;
    for (i = 0; i < 2000; i++) {
        if (atomic_load(&job->parked)) { return; }
        usleep(1000);
    }
    assert(!"native operation did not reach its blocking/GC boundary");
}
static int rawConnect(int port, uint32_t ip) {
    struct sockaddr_in a = address(port, ip);
    struct timeval timeout = {3, 0};
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout));
    if (connect(fd, (struct sockaddr*) &a, sizeof(a)) != 0) { close(fd); return -1; }
    return fd;
}
static int pair(Job* j) {
    pthread_t t;
    int fd;
    atomic_store(&j->parked, 0);
    assert(pthread_create(&t, NULL, accepting, j) == 0);
    waitParked(j);
    /* A wildcard listener would also accept this second loopback address. */
    assert(rawConnect(j->port, INADDR_LOOPBACK + 1) < 0);
    fd = rawConnect(j->port, INADDR_LOOPBACK);
    assert(fd >= 0);
    join(t);
    assert(j->token > 0 && CONNECTED(NULL, j->token));
    return fd;
}
static int descriptorCount(void) {
    DIR* d = opendir("/proc/self/fd");
    int count = 0;
    assert(d);
    while (readdir(d)) { count++; }
    closedir(d);
    return count;
}
int main(void) {
    int initial = descriptorCount(), i;
    Job j = {0};
    TestArray* a = array(32);
    pthread_t thread;
    signal(SIGPIPE, SIG_DFL);
    assert(ACCEPT(NULL, -1) == 0 && ACCEPT(NULL, 65536) == 0);
    assert(!CONNECTED(NULL, 0) && ERROR(NULL, 0) == -1);
    CLOSE(NULL, 0);
    /* Bind failure reports a null token without allocating a listener. */
    {
        int occupied = socket(AF_INET, SOCK_STREAM, 0);
        struct sockaddr_in addr;
        j.port = freePort(); addr = address(j.port, INADDR_ANY);
        assert(bind(occupied, (struct sockaddr*) &addr, sizeof(addr)) == 0);
        assert(listen(occupied, 4) == 0);
        assert(ACCEPT(NULL, j.port) == 0);
        close(occupied);
    }
    /* Stop cancels a blocked accept and permits immediate restart. */
    j.port = freePort();
    for (i = 0; i < 40; i++) {
        j.token = -1;
        atomic_store(&j.parked, 0);
        assert(pthread_create(&thread, NULL, accepting, &j) == 0);
        waitParked(&j);
        STOP(NULL, j.port);
        join(thread);
        assert(j.token == 0);
        assert(rawConnect(j.port, INADDR_LOOPBACK) < 0);
    }
    /* Closing a peer wakes blocked reads. A stale token cannot affect its replacement. */
    for (i = 0; i < 60; i++) {
        int fd = pair(&j);
        JAVA_LONG old = j.token;
        j.array = a;
        atomic_store(&j.parked, 0);
        assert(pthread_create(&thread, NULL, reading, &j) == 0);
        waitParked(&j);
        CLOSE(NULL, old); CLOSE(NULL, old);
        join(thread);
        assert(j.result == -1 && !CONNECTED(NULL, old));
        close(fd);
        fd = pair(&j);
        assert(j.token != old);
        CLOSE(NULL, old);
        assert(READ(NULL, old, a, 0, 1) == -1);
        memcpy(a->data, "abcdef", 6);
        assert(WRITE(NULL, j.token, a, 2, 4) == 4);
        assert(recv(fd, a->data, 4, MSG_WAITALL) == 4);
        assert(memcmp(a->data, "cdef", 4) == 0);
        assert(WRITE(NULL, j.token, a, 32, 1) == -1);
        assert(READ(NULL, j.token, a, -1, 1) == -1);
        assert(WRITE(NULL, j.token, a, 0, 0) == 0);
        assert(READ(NULL, j.token, a, 0, 0) == 0);
        STOP(NULL, j.port); /* accepted connection survives listener shutdown */
        assert(send(fd, "ping", 4, MSG_NOSIGNAL) == 4);
        assert(READ(NULL, j.token, a, 1, 4) == 4);
        assert(memcmp(a->data + 1, "ping", 4) == 0);
        close(fd);
        assert(READ(NULL, j.token, a, 0, 1) == -1);
        CLOSE(NULL, j.token);
    }
    /* An interrupted blocking read resumes without dropping the connection. */
    {
        struct sigaction action;
        int fd = pair(&j);
        memset(&action, 0, sizeof(action));
        action.sa_handler = interruption;
        sigemptyset(&action.sa_mask);
        assert(sigaction(SIGUSR1, &action, NULL) == 0);
        j.array = a;
        atomic_store(&j.parked, 0);
        assert(pthread_create(&thread, NULL, reading, &j) == 0);
        waitParked(&j);
        for (i = 0; i < 5; i++) {
            usleep(10000);
            assert(pthread_kill(thread, SIGUSR1) == 0);
        }
        assert(send(fd, "again", 5, MSG_NOSIGNAL) == 5);
        join(thread);
        assert(receivedSignals > 0 && j.result == 5 && memcmp(a->data, "again", 5) == 0);
        CLOSE(NULL, j.token); close(fd); STOP(NULL, j.port);
    }
    /* Large blocked sends are cancelled; SIGPIPE remains suppressed after reset. */
    {
        int fd = pair(&j);
        TestArray* large = array(16 * 1024 * 1024);
        j.array = large;
        atomic_store(&j.parked, 0);
        assert(pthread_create(&thread, NULL, writing, &j) == 0);
        waitParked(&j);
        usleep(30000);
        CLOSE(NULL, j.token);
        join(thread);
        assert(j.result < large->length);
        close(fd); free(large); STOP(NULL, j.port);
        fd = pair(&j);
        {
            struct linger reset = {1, 0};
            setsockopt(fd, SOL_SOCKET, SO_LINGER, &reset, sizeof(reset));
        }
        close(fd); usleep(10000);
        assert(WRITE(NULL, j.token, a, 0, 32) < 32);
        assert(WRITE(NULL, j.token, a, 0, 32) < 32);
        CLOSE(NULL, j.token); STOP(NULL, j.port);
    }
    /* Existing outbound clients still use the same native bridge. */
    {
        int listener = socket(AF_INET, SOCK_STREAM, 0), fd;
        struct sockaddr_in addr;
        j.port = freePort(); addr = address(j.port, INADDR_LOOPBACK);
        assert(bind(listener, (struct sockaddr*) &addr, sizeof(addr)) == 0);
        assert(listen(listener, 4) == 0);
        j.token = CONNECT(NULL, "127.0.0.1", j.port, 0);
        assert(j.token > 0);
        fd = accept(listener, NULL, NULL); assert(fd >= 0);
        assert(send(fd, "client", 6, MSG_NOSIGNAL) == 6);
        assert(READ(NULL, j.token, a, 0, 6) == 6);
        assert(memcmp(a->data, "client", 6) == 0);
        CLOSE(NULL, j.token); close(fd); close(listener);
    }
    free(a);
    assert(descriptorCount() == initial);
    puts("PASS: loopback binding, conflict, 40 accept cancellations/restarts, 60 read-close/stale-token cycles, interrupted read, blocked write, SIGPIPE, bounds, outbound client, descriptor cleanup");
    return 0;
}

/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
 * Outbound HTTP and HTTPS for server-side binaries, on libcurl.
 *
 * Why libcurl rather than the raw-socket client in cn1_backend_net.c: that one is
 * plaintext, which is correct for the loopback control protocol it was written for
 * and useless for calling anything real. TLS needs a certificate store, hostname
 * verification, redirects and chunked decoding, and none of those are things to
 * hand-roll into a server that talks to the public internet. This is the same
 * choice the native Linux port already made.
 *
 * Peer and host verification are left at libcurl's defaults (both ON) and there is
 * deliberately no knob to turn them off: an "insecure" flag is the kind of thing
 * that ships enabled.
 */
#include "cn1_globals.h"
#include "cn1_virtual_thread.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef _WIN32
#include <unistd.h> /* CN1_RESUME_THREAD expands to usleep */
#include <poll.h>
#endif
#include <curl/curl.h>
/* For the ALPN answer of a connection libcurl made through OpenSSL; see
   cn1WebRequireH2Prereq. Both packaging targets link OpenSSL, and this file is
   compiled only when TLS is (build.sh drops it with CN1_BACKEND_HTTPS=0). */
#include <openssl/ssl.h>

#ifndef _WIN32
/* Defined in cn1_backend_server.c, beside the virtual-thread scheduler. */
int cn1BackendVtWait(int count, const int* fds, const int* events, long long timeoutMillis);
/* Defined there too: on a virtual thread that holds no monitor. */
int cn1BackendCanPark(void);

/* The most sockets one transfer holds at once -- two while libcurl races an IPv4
 * and an IPv6 connect, plus its resolver's wake-up pair. The same bound as a
 * virtual thread's wait record. */
#define CN1_WEB_MAX_SOCKETS 8

/* What libcurl has asked to be told about, kept by its socket and timer
 * callbacks. Lives on the calling virtual thread's own stack for one transfer. */
typedef struct {
    curl_socket_t fd[CN1_WEB_MAX_SOCKETS];
    int what[CN1_WEB_MAX_SOCKETS];
    int count;
    long timeoutMillis;
} CN1WebWatch;

static int cn1WebSocketCallback(CURL* easy, curl_socket_t s, int what, void* userp,
                                void* socketp) {
    CN1WebWatch* w = (CN1WebWatch*)userp;
    int i;
    (void)easy; (void)socketp;
    for(i = 0 ; i < w->count ; i++) {
        if(w->fd[i] == s) {
            break;
        }
    }
    if(what == CURL_POLL_REMOVE) {
        if(i < w->count) {
            w->count--;
            w->fd[i] = w->fd[w->count];
            w->what[i] = w->what[w->count];
        }
        return 0;
    }
    if(i == w->count) {
        if(w->count >= CN1_WEB_MAX_SOCKETS) {
            return -1;          /* libcurl fails the transfer rather than losing one */
        }
        w->fd[i] = s;
        w->count++;
    }
    w->what[i] = what;
    return 0;
}

static int cn1WebTimerCallback(CURLM* multi, long timeoutMillis, void* userp) {
    (void)multi;
    ((CN1WebWatch*)userp)->timeoutMillis = timeoutMillis;
    return 0;
}

/*
 * curl_easy_perform for a VIRTUAL thread: the same transfer, driven through the
 * multi interface so that every wait parks this virtual thread on its host's
 * poller instead of blocking the host inside libcurl's own poll(). A call to a
 * slow API from a handler then costs one virtual thread, not one of the hosts --
 * of which there is one per core, so a handful of slow calls used to stop the
 * server answering anything.
 *
 * libcurl reports which sockets it wants watched, and for what, through the
 * socket callback, and when it next needs a turn regardless through the timer
 * callback; the wait asks the host for exactly that, and afterwards a zero-time
 * poll says which socket woke it, so libcurl is told precisely what happened.
 * Everything the transfer does -- the connect and CONNECTTIMEOUT, TLS, redirects,
 * LOW_SPEED_TIME -- is libcurl's own logic unchanged. Falls back to the blocking
 * call when the multi handle cannot be had.
 */
static void cn1WebWatchMulti(CURLM* multi, CN1WebWatch* watch) {
    memset(watch, 0, sizeof(*watch));
    watch->timeoutMillis = -1;
    curl_multi_setopt(multi, CURLMOPT_SOCKETFUNCTION, cn1WebSocketCallback);
    curl_multi_setopt(multi, CURLMOPT_SOCKETDATA, watch);
    curl_multi_setopt(multi, CURLMOPT_TIMERFUNCTION, cn1WebTimerCallback);
    curl_multi_setopt(multi, CURLMOPT_TIMERDATA, watch);
}

/* Drives one transfer on `multi`, whose socket and timer callbacks report into
 * `watch` (cn1WebWatchMulti). With `park` the waits park the virtual thread;
 * without it they block this thread in poll(), which is what a WebClient's
 * pooled multi does off a virtual thread -- the pool's connections live in the
 * multi, so a request that cannot park still has to go through it to reuse
 * one, and a multi driven by the socket API must never be driven any other way.
 *
 * With a NULL `multi` a multi of the transfer's own is made and discarded, which
 * is the static Web path: no pool, the behaviour it has always had. */
static CURLcode cn1WebDrive(CURLM* pooled, CN1WebWatch* pooledWatch, CURL* curl, int park) {
    CURLM* multi = pooled;
    CN1WebWatch ownWatch;
    CN1WebWatch* w = pooledWatch;
    CURLcode rc = CURLE_FAILED_INIT;
    CURLMsg* msg;
    int running = 0;
    int left;
    if(multi == NULL) {
        multi = curl_multi_init();
        if(multi == NULL) {
            return curl_easy_perform(curl);
        }
        w = &ownWatch;
        cn1WebWatchMulti(multi, w);
    }
    if(curl_multi_add_handle(multi, curl) != CURLM_OK) {
        if(pooled == NULL) {
            curl_multi_cleanup(multi);
            return curl_easy_perform(curl);
        }
        /* A pooled multi that refuses a handle is not one to fall back from by
           creating a second transfer on it; fail this request. */
        return CURLE_FAILED_INIT;
    }
    curl_multi_socket_action(multi, CURL_SOCKET_TIMEOUT, 0, &running);
    while(running) {
        int fds[CN1_WEB_MAX_SOCKETS];
        int events[CN1_WEB_MAX_SOCKETS];
        struct pollfd probe[CN1_WEB_MAX_SOCKETS];
        int n = w->count;
        int i;
        int ready = 0;
        long long timeout = (long long)w->timeoutMillis;
        if(timeout == 0) {
            w->timeoutMillis = -1;
            curl_multi_socket_action(multi, CURL_SOCKET_TIMEOUT, 0, &running);
            continue;
        }
        if(n == 0 && timeout < 0) {
            /* Nothing to watch and no timer, which libcurl should never leave
               us with while a transfer runs; look again shortly rather than
               waiting for ever on nothing. */
            timeout = 10;
        }
        for(i = 0 ; i < n ; i++) {
            fds[i] = (int)w->fd[i];
            events[i] = ((w->what[i] & CURL_POLL_IN) ? 1 : 0)
                      | ((w->what[i] & CURL_POLL_OUT) ? 2 : 0);
            probe[i].fd = fds[i];
            probe[i].events = (short)(((events[i] & 1) ? POLLIN : 0)
                                      | ((events[i] & 2) ? POLLOUT : 0));
            probe[i].revents = 0;
        }
        /* Ask first, as every other wait here does: a socket that is already
           ready costs no park. */
        if(n > 0) {
            ready = poll(probe, (nfds_t)n, 0);
        }
        if(ready <= 0 && !park) {
            /* Off a virtual thread: block this thread, as curl_easy_perform
               would. Nothing to watch means only a timer, so sleep it out in
               poll() with no descriptors. */
            ready = poll(probe, (nfds_t)n, timeout > 1000 ? 1000 : (int)timeout);
        } else if(ready <= 0) {
            cn1BackendVtWait(n, fds, events, timeout);
            if(n > 0) {
                for(i = 0 ; i < n ; i++) {
                    probe[i].revents = 0;
                }
                ready = poll(probe, (nfds_t)n, 0);
            }
        }
        if(ready > 0) {
            for(i = 0 ; i < n ; i++) {
                int mask = 0;
                if(probe[i].revents & (POLLIN | POLLHUP)) {
                    mask |= CURL_CSELECT_IN;
                }
                if(probe[i].revents & POLLOUT) {
                    mask |= CURL_CSELECT_OUT;
                }
                if(probe[i].revents & (POLLERR | POLLNVAL)) {
                    mask |= CURL_CSELECT_ERR;
                }
                if(mask != 0) {
                    curl_multi_socket_action(multi, (curl_socket_t)fds[i], mask, &running);
                }
            }
        } else {
            curl_multi_socket_action(multi, CURL_SOCKET_TIMEOUT, 0, &running);
        }
    }
    while((msg = curl_multi_info_read(multi, &left)) != NULL) {
        if(msg->msg == CURLMSG_DONE) {
            rc = msg->data.result;
        }
    }
    curl_multi_remove_handle(multi, curl);
    if(pooled == NULL) {
        curl_multi_cleanup(multi);
    }
    return rc;
}
#endif

typedef struct {
    char* data;
    size_t length;
    /* The response headers, verbatim, one per line. Kept as one buffer rather
     * than parsed here: the Java side already has to split them, and a header
     * parser in C is a second place for the same rules to drift. */
    char* headers;
    size_t headerLength;
    long status;
    /* CURLINFO_HTTP_VERSION: 1 HTTP/1.0, 2 HTTP/1.1, 3 HTTP/2, 30 HTTP/3, 0 unknown. */
    long httpVersion;
    char error[CURL_ERROR_SIZE];
} CN1WebResponse;

/*
 * A BACKSTOP under libcurl's own aggregate header cap, and not the thing that
 * enforces this today. Worth stating plainly, because review asked for it on the
 * grounds that libcurl bounds a single header LINE and nothing bounds how many
 * arrive -- and that is not true of the libcurl shipped here. Measured, with this
 * check compiled out, a 1.8MB header block was refused by libcurl itself:
 *
 *     Too large response headers: 307397 > 307200
 *
 * So the accumulator was never actually unbounded on this build. This sits ABOVE
 * that 300KB cap deliberately: it changes nothing where the cap exists, and
 * catches a libcurl built without it rather than second-guessing the one that
 * has it. One megabyte is far past anything real either way -- an origin
 * server's whole block is tens of kilobytes, and the Java SE arm inherits the
 * JDK's 384KB, which is what refuses the same response there.
 *
 * Fixed rather than configurable, unlike the body bound: a caller can want a
 * large DOWNLOAD, and nobody wants a large header block.
 */
#define CN1_WEB_MAX_HEADER_BYTES ((size_t)(1024 * 1024))

/* Whether CN1_WEB_VERBOSE names a true value. "1" or "true" in any case; every
   other spelling, including "0", "false" and empty, leaves the trace off. Compared
   by hand rather than through tolower, which is locale sensitive. */
static int cn1WebVerboseEnabled(void) {
    const char* v = getenv("CN1_WEB_VERBOSE");
    if(v == NULL) {
        return 0;
    }
    if(strcmp(v, "1") == 0) {
        return 1;
    }
    return (v[0] == 't' || v[0] == 'T') && (v[1] == 'r' || v[1] == 'R')
        && (v[2] == 'u' || v[2] == 'U') && (v[3] == 'e' || v[3] == 'E') && v[4] == 0;
}

static size_t cn1WebHeader(void* contents, size_t size, size_t count, void* userp) {
    CN1WebResponse* r = (CN1WebResponse*)userp;
    size_t total = size * count;
    if(total > CN1_WEB_MAX_HEADER_BYTES - r->headerLength) {
        return 0; /* aborts the transfer; libcurl reports CURLE_WRITE_ERROR */
    }
    char* grown = (char*)realloc(r->headers, r->headerLength + total + 1);
    if(grown == NULL) {
        return 0; /* tells libcurl to abort the transfer */
    }
    r->headers = grown;
    memcpy(r->headers + r->headerLength, contents, total);
    r->headerLength += total;
    r->headers[r->headerLength] = 0;
    return total;
}

/* What a Java byte[] can hold. A response is handed back as one, so a transfer
   that outgrows this cannot be delivered however much memory the host has --
   and the length is carried in a size_t here and narrowed to an int there, so
   letting it past this point produces a NEGATIVE array length and then a memcpy
   of the full size_t into whatever that allocated. Refused while it is still a
   failed download, which Web turns into an IOException, rather than after it has
   become memory corruption. */
#define CN1_WEB_MAX_BODY_BYTES ((size_t)0x7fffffff)

/*
 * What an outbound response is ALLOWED to be, which is a different question from
 * what one can be represented as.
 *
 * The ceiling above is an integer-safety bound and nothing else: it stops a
 * length narrowing to a negative array size. It is no protection from the
 * machine, because the body is accumulated whole before Java sees any of it, so
 * an endless or merely huge upstream response reallocs until the container is
 * OOM-killed -- long before 2GB, and with the low-speed timeout no help at all
 * when the bytes are arriving quickly.
 *
 * CN1_WEB_MAX_RESPONSE_MB is the practical bound, defaulting to the same 64MB
 * CN1_HTTP_MAX_UPLOAD_MB uses for the inbound direction. The Java SE arm reads
 * the SAME variable with the SAME default -- it had no bound of any kind -- so
 * the two arms refuse the same response. Exceeding it aborts the transfer, which
 * libcurl reports as CURLE_WRITE_ERROR and Web turns into an IOException: a
 * failed download rather than a dead process.
 */
#define CN1_WEB_DEFAULT_MAX_RESPONSE_MB 64
static size_t cn1WebResponseLimit(void) {
    const char* text = getenv("CN1_WEB_MAX_RESPONSE_MB");
    long mb = CN1_WEB_DEFAULT_MAX_RESPONSE_MB;
    if(text != NULL && text[0] != 0) {
        /* PLAIN DIGITS, by hand, because this has to accept exactly what the Java
           SE arm accepts. strtol takes a sign and leading whitespace, so "+1" set
           a 1MB bound here while Web.maxResponseBytes ignored it and kept 64MB --
           and a response between those two sizes then succeeded under cn1:backend
           and aborted once packaged, which is the most expensive kind of
           divergence to track down. Same grammar, same length cap, same range.
           A misconfigured bound is ignored rather than clamped: it must not
           silently become a different one than the caller believes it set. */
        size_t length = strlen(text);
        long parsed = 0;
        int digits = length >= 1 && length <= 4;
        size_t iter;
        for(iter = 0 ; digits && iter < length ; iter++) {
            if(text[iter] < '0' || text[iter] > '9') {
                digits = 0;
            } else {
                parsed = parsed * 10 + (text[iter] - '0');
            }
        }
        if(digits && parsed >= 1 && parsed <= 2047) {
            mb = parsed;
        }
    }
    return (size_t)mb * 1024u * 1024u;
}

static size_t cn1WebWrite(void* contents, size_t size, size_t count, void* userp) {
    CN1WebResponse* r = (CN1WebResponse*)userp;
    size_t total = size * count;
    size_t allowed = cn1WebResponseLimit();
    if(total > CN1_WEB_MAX_BODY_BYTES - r->length) {
        return 0; /* aborts the transfer; libcurl reports CURLE_WRITE_ERROR */
    }
    if(r->length + total > allowed) {
        return 0; /* over the practical bound; same abort, same IOException */
    }
    char* grown = (char*)realloc(r->data, r->length + total + 1);
    if(grown == NULL) {
        return 0; /* tells libcurl to abort the transfer */
    }
    r->data = grown;
    memcpy(r->data + r->length, contents, total);
    r->length += total;
    r->data[r->length] = 0;
    return total;
}

/**
 * curl_global_init, called once from Java before any thread can be inside
 * libcurl.
 *
 * curl_easy_init below does this implicitly on first use, and that path is NOT
 * thread safe in a libcurl older than 7.84 or one whose curl_version_info does
 * not report CURL_VERSION_THREADSAFE -- two workers whose first outbound
 * request overlaps would both enter it. Nothing here can choose the system
 * libcurl's version, so the initialisation is made explicit instead and
 * Web.initialiseCurlOnce serialises it.
 *
 * Returns the CURLcode, so a failure is visible to the caller rather than
 * remembered as a successful initialisation.
 */
/*
 * A WebClient's connection pool.
 *
 * libcurl keeps finished connections in the cache of the handle that ran them --
 * a multi handle, here -- and hands one to the next transfer on that handle
 * whose host, port, protocol AND TLS configuration match, client certificate
 * included. So the pool is a stack of idle multis: a request takes one (or
 * makes one), runs on it alone, and gives it back with its connections in it.
 *
 * Why not a CURLSH sharing CURL_LOCK_DATA_CONNECT, which is the obvious spelling
 * of "a connection pool": libcurl documents that sharing connections between
 * concurrent threads is not supported, and HTTP/2 streams on a shared
 * connection from different threads are not supported either. A multi here is
 * only ever inside one request at a time, so neither can happen.
 *
 * And why connections cannot cross credentials: a pool belongs to ONE WebClient,
 * whose configuration is fixed by its first request (WebClient.checkUnused), so
 * every connection in it was authenticated with the same certificate. libcurl's
 * own matching compares the TLS configuration on top of that. The static Web
 * methods pass no pool at all, and get a multi of their own per request, as they
 * always have.
 *
 * Windows has no pooled path (no poll(), and the parked path is POSIX only): a
 * request there makes its own easy handle and closes it, as before. The
 * packaging targets are musl and glibc Linux.
 */
#ifndef _WIN32
#include <pthread.h>

/* Idle multis kept per client; more concurrent requests than this still run,
   their multis are simply closed instead of kept. */
#define CN1_WEB_POOL_IDLE 8
/* Idle connections kept per multi. Kept well under CN1_WEB_MAX_SOCKETS, so a
   transfer's own sockets always fit beside anything libcurl still watches. */
#define CN1_WEB_POOL_MAXCONNECTS 2L

typedef struct {
    CURLM* multi;
    CN1WebWatch watch;
} CN1WebPooled;

typedef struct {
    pthread_mutex_t lock;
    CN1WebPooled* idle[CN1_WEB_POOL_IDLE];
    int count;
} CN1WebPool;

static CN1WebPooled* cn1WebPooledNew(void) {
    CN1WebPooled* p = (CN1WebPooled*)calloc(1, sizeof(CN1WebPooled));
    if(p == NULL) {
        return NULL;
    }
    p->multi = curl_multi_init();
    if(p->multi == NULL) {
        free(p);
        return NULL;
    }
    curl_multi_setopt(p->multi, CURLMOPT_MAXCONNECTS, CN1_WEB_POOL_MAXCONNECTS);
    cn1WebWatchMulti(p->multi, &p->watch);
    return p;
}

static void cn1WebPooledFree(CN1WebPooled* p) {
    if(p != NULL) {
        curl_multi_cleanup(p->multi);
        free(p);
    }
}

static CN1WebPooled* cn1WebPoolTake(CN1WebPool* pool) {
    CN1WebPooled* p = NULL;
    pthread_mutex_lock(&pool->lock);
    if(pool->count > 0) {
        p = pool->idle[--pool->count];
    }
    pthread_mutex_unlock(&pool->lock);
    return p != NULL ? p : cn1WebPooledNew();
}

static void cn1WebPoolGive(CN1WebPool* pool, CN1WebPooled* p) {
    int kept = 0;
    pthread_mutex_lock(&pool->lock);
    if(pool->count < CN1_WEB_POOL_IDLE) {
        pool->idle[pool->count++] = p;
        kept = 1;
    }
    pthread_mutex_unlock(&pool->lock);
    if(!kept) {
        cn1WebPooledFree(p);
    }
}
#endif

JAVA_LONG com_codename1_backend_Web_poolCreateImpl___R_long(CODENAME_ONE_THREAD_STATE) {
#ifndef _WIN32
    CN1WebPool* pool = (CN1WebPool*)calloc(1, sizeof(CN1WebPool));
    if(pool == NULL) {
        return 0;
    }
    if(pthread_mutex_init(&pool->lock, NULL) != 0) {
        free(pool);
        return 0;
    }
    return (JAVA_LONG)(intptr_t)pool;
#else
    /* No pool on Windows; a non-zero handle so a WebClient still works there. */
    return 1;
#endif
}

/* Called by Web.Pool once the client is closed and no request is inside it. */
JAVA_VOID com_codename1_backend_Web_poolFreeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
#ifndef _WIN32
    CN1WebPool* pool = (CN1WebPool*)(intptr_t)handle;
    int i;
    if(pool == NULL) {
        return;
    }
    for(i = 0 ; i < pool->count ; i++) {
        cn1WebPooledFree(pool->idle[i]);
    }
    pthread_mutex_destroy(&pool->lock);
    free(pool);
#else
    (void)handle;
#endif
}

/* Copies a Java byte[] for libcurl, or NULL for null/empty. */
static char* cn1WebCopyBytes(JAVA_OBJECT arr, size_t* lengthOut) {
    size_t length;
    char* out;
    *lengthOut = 0;
    if(arr == JAVA_NULL) {
        return NULL;
    }
    length = (size_t)((JAVA_ARRAY)arr)->length;
    if(length == 0) {
        return NULL;
    }
    out = (char*)malloc(length);
    if(out != NULL) {
        memcpy(out, (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(arr), length);
        *lengthOut = length;
    }
    return out;
}

/* Frees a copy that held key material, overwriting it first so a private key or
   a password does not outlive the request in freed heap. Through a volatile
   pointer, because a memset of memory about to be freed is a dead store the
   optimiser is entitled to remove. */
static void cn1WebFreeSecret(char* secret, size_t length) {
    if(secret != NULL) {
        volatile char* p = (volatile char*)secret;
        size_t i;
        for(i = 0 ; i < length ; i++) {
            p[i] = 0;
        }
        free(secret);
    }
}

JAVA_INT com_codename1_backend_Web_globalInitImpl___R_int(CODENAME_ONE_THREAD_STATE) {
    return (JAVA_INT)curl_global_init(CURL_GLOBAL_DEFAULT);
}

/*
 * headerLines is one string with '\n' between headers, because passing a
 * String[] would mean walking a Java array from C for no benefit.
 *
 * callerHeaders says whether any of those lines came from the CALLER. The rest
 * are trace context the runtime adds itself (see Web.perform), and the redirect
 * rule below is about what the caller handed over, so it asks this rather than
 * whether the list is empty.
 */
/* State for cn1WebRequireH2Prereq. */
typedef struct {
    CURL* curl;
    /* Set when the callback refused the connection, with the protocol the
       server chose instead ("" for none). */
    int refused;
    char chosen[32];
} CN1WebH2Check;

#if LIBCURL_VERSION_NUM >= 0x075000
/* Runs after the connection is up -- TLS handshake and ALPN done -- and BEFORE
 * libcurl writes a byte of the request. The HTTP/2 requirement is decided here.
 *
 * CURL_HTTP_VERSION_2_PRIOR_KNOWLEDGE alone does not do it over TLS. Measured
 * against an HTTP/1.1-only server: ALPN offered h2, the server chose nothing,
 * libcurl took the missing answer to mean HTTP/1.1, and the request was SENT --
 * the fixture's request counter went up by one -- before the version check
 * after the transfer could refuse the response. For a POST or a DELETE that is
 * the state change happening at a server the caller ruled out. So the
 * connection's own ALPN answer is read here and anything but h2 aborts.
 *
 * Only an OpenSSL libcurl exposes that answer (CURLINFO_TLS_SSL_PTR hands out
 * the SSL*). With another TLS backend -- a macOS host build on SecureTransport --
 * this cannot tell and lets the transfer go on; the check after the transfer
 * still refuses an HTTP/1.1 response, but the request has gone out by then.
 * Both packaging targets are OpenSSL. Plain http:// is not checked here either:
 * prior knowledge sends the HTTP/2 preface first, and an HTTP/1.1 server reads
 * that as the unknown method PRI, not as the caller's request. */
static int cn1WebRequireH2Prereq(void* clientp, char* primaryIp, char* localIp,
                                 int primaryPort, int localPort) {
    CN1WebH2Check* check = (CN1WebH2Check*)clientp;
    struct curl_tlssessioninfo* info = NULL;
    (void)primaryIp; (void)localIp; (void)primaryPort; (void)localPort;
    if(curl_easy_getinfo(check->curl, CURLINFO_TLS_SSL_PTR, &info) != CURLE_OK
            || info == NULL || info->backend != CURLSSLBACKEND_OPENSSL
            || info->internals == NULL) {
        return CURL_PREREQFUNC_OK;
    }
    {
        const unsigned char* alpn = NULL;
        unsigned int length = 0;
        SSL_get0_alpn_selected((SSL*)info->internals, &alpn, &length);
        if(length == 2 && alpn[0] == 'h' && alpn[1] == '2') {
            return CURL_PREREQFUNC_OK;
        }
        check->refused = 1;
        if(length >= sizeof(check->chosen)) {
            length = sizeof(check->chosen) - 1;
        }
        if(length > 0) {
            memcpy(check->chosen, alpn, length);
        }
        check->chosen[length] = 0;
        return CURL_PREREQFUNC_ABORT;
    }
}
#endif

/* Web.performImpl's flags; see Web.FLAG_*. */
#define CN1_WEB_FLAG_REQUIRE_HTTP2 1
#define CN1_WEB_FLAG_CLIENT 2
#define CN1_WEB_FLAG_NO_REUSE 4
/* ClientCertificate's types. */
#define CN1_WEB_CERT_PEM 0
#define CN1_WEB_CERT_PKCS12 1

JAVA_LONG com_codename1_backend_Web_performImpl___long_java_lang_String_java_lang_String_byte_1ARRAY_byte_1ARRAY_boolean_int_int_int_int_byte_1ARRAY_byte_1ARRAY_int_java_lang_String_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_LONG poolHandle, JAVA_OBJECT method, JAVA_OBJECT url,
        JAVA_OBJECT headerLines, JAVA_OBJECT body, JAVA_BOOLEAN callerHeaders, JAVA_INT flags,
        JAVA_INT connectTimeoutMillis, JAVA_INT readTimeoutMillis, JAVA_INT requestTimeoutMillis,
        JAVA_OBJECT certificate, JAVA_OBJECT key, JAVA_INT certificateType, JAVA_OBJECT password) {
    CURL* curl;
    CURLcode rc;
    struct curl_slist* headers = NULL;
    CN1WebResponse* r;
    char* methodCopy = NULL;
    char* urlCopy = NULL;
    char* bodyCopy = NULL;
    JAVA_INT bodyLength = 0;
    int safeMethod;
    char* certCopy = NULL;
    size_t certLength = 0;
    char* keyCopy = NULL;
    size_t keyLength = 0;
    char* passwordCopy = NULL;
    size_t passwordLength = 0;
    CN1WebH2Check h2check;

    if(url == JAVA_NULL) {
        return 0;
    }
    /* stringToUTF8 returns this thread's scratch buffer, which the NEXT conversion
       overwrites -- so every string is copied before the next one is converted. */
    {
        const char* tmp = stringToUTF8(threadStateData, url);
        if(tmp == NULL) {
            return 0;
        }
        urlCopy = strdup(tmp);
        if(urlCopy == NULL) {
            return 0;
        }
    }
    if(method != JAVA_NULL) {
        const char* tmp = stringToUTF8(threadStateData, method);
        if(tmp != NULL) {
            methodCopy = strdup(tmp);
            if(methodCopy == NULL) {
                /* A NULL methodCopy means "no method was asked for", and libcurl
                   then sends GET -- so a failed copy of DELETE does not fail the
                   request, it CHANGES it. S3.deleteObject treats 200 as success,
                   and a GET of an object that exists answers 200, so the object
                   would be reported deleted and still be there.

                   This is the fourth allocation in this function to need the same
                   sentence, and the last: url above, the header block and its list
                   below, and this. The body, the response struct and the easy
                   handle were already checked. */
                free(urlCopy);
                return 0;
            }
        }
    }
    if(headerLines != JAVA_NULL) {
        /* BYTES, one per field-value character. HeaderLines.validate accepts
           0x20..0xff except 0x7f and says it tests "the byte that will be
           emitted" -- which was not true of this path: stringToUTF8 encoded an
           obs-text character into two bytes, so the header libcurl sent was not
           the one the caller wrote or the validation checked. */
        int headerLength = (int)((JAVA_ARRAY)headerLines)->length;
        if(headerLength > 0) {
            char* copy = (char*)malloc((size_t)headerLength + 1);
            if(copy != NULL) {
                memcpy(copy, (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(headerLines),
                       (size_t)headerLength);
                /* Terminated because the loop below walks a C string; the
                   validation refuses every character under 0x20, NUL included. */
                copy[headerLength] = 0;
            }
            if(copy == NULL) {
                /* THE SAME RULE AS THE APPEND BELOW, one allocation earlier. A null
                   copy left "line" null, the loop never ran, and the request went
                   out with no headers at all while reporting success -- which is
                   the failure the append was just taught to refuse, reached by the
                   strdup in front of it. Nothing is on the list yet, so there is
                   nothing to free but the two copies above. */
                free(urlCopy);
                free(methodCopy);
                return 0;
            }
            char* line = copy;
            while(line != NULL && *line != 0) {
                char* nl = strchr(line, '\n');
                if(nl != NULL) {
                    *nl = 0;
                }
                if(*line != 0) {
                    /* THE OLD LIST IS NOT LOST. curl_slist_append answers NULL on
                       failure and leaves what it was given allocated, so assigning
                       its result straight back dropped the only pointer to every
                       header appended so far -- leaked, and the request then went
                       out with none of them. An Authorization or If-None-Match
                       that quietly does not travel is a request that means
                       something else: the first is unauthenticated and the second
                       asks for the whole object. Same rule the body below states
                       outright -- a request whose parts could not be made is a
                       failed request, not a smaller one. */
                    struct curl_slist* appended = curl_slist_append(headers, line);
                    if(appended == NULL) {
                        curl_slist_free_all(headers);
                        free(copy);
                        free(urlCopy);
                        free(methodCopy);
                        return 0;
                    }
                    headers = appended;
                }
                line = nl == NULL ? NULL : nl + 1;
            }
            free(copy);
        }
    }
    if(body != JAVA_NULL) {
        JAVA_ARRAY arr = (JAVA_ARRAY)body;
        bodyLength = arr->length;
        bodyCopy = (char*)malloc(bodyLength == 0 ? 1 : (size_t)bodyLength);
        if(bodyCopy == NULL && bodyLength > 0) {
            /* The request must NOT go out without it. The POSTFIELDS block below
               is skipped when bodyCopy is null, so a failed allocation sent the
               same request with an EMPTY body and reported success: an S3
               putObject would replace the object with nothing, and the caller
               would be told it worked. A request whose body could not be made is
               a failed request. */
            free(urlCopy);
            free(methodCopy);
            curl_slist_free_all(headers);
            return 0;
        }
        if(bodyCopy != NULL && bodyLength > 0) {
            memcpy(bodyCopy, (JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(arr), (size_t)bodyLength);
        }
    }

    /* The credential, copied like everything else before the next Java string
       is converted. A credential that was given and could not be copied fails
       the request: sent without it, the request would go out unauthenticated
       and come back as the server's refusal, which reads as a wrong certificate
       rather than as the allocation failure it was. */
    if(certificateType >= 0) {
        certCopy = cn1WebCopyBytes(certificate, &certLength);
        if(certificateType == CN1_WEB_CERT_PEM) {
            keyCopy = cn1WebCopyBytes(key, &keyLength);
        }
        if(password != JAVA_NULL) {
            const char* tmp = stringToUTF8(threadStateData, password);
            if(tmp != NULL) {
                passwordLength = strlen(tmp);
                passwordCopy = (char*)malloc(passwordLength + 1);
                if(passwordCopy != NULL) {
                    memcpy(passwordCopy, tmp, passwordLength + 1);
                }
            }
        }
        if(certCopy == NULL || (certificateType == CN1_WEB_CERT_PEM && keyCopy == NULL)
                || (password != JAVA_NULL && passwordCopy == NULL)) {
            cn1WebFreeSecret(certCopy, certLength);
            cn1WebFreeSecret(keyCopy, keyLength);
            cn1WebFreeSecret(passwordCopy, passwordLength);
            free(urlCopy); free(methodCopy); free(bodyCopy);
            curl_slist_free_all(headers);
            return 0;
        }
    }

    r = (CN1WebResponse*)calloc(1, sizeof(CN1WebResponse));
    if(r == NULL) {
        cn1WebFreeSecret(certCopy, certLength);
        cn1WebFreeSecret(keyCopy, keyLength);
        cn1WebFreeSecret(passwordCopy, passwordLength);
        free(urlCopy); free(methodCopy); free(bodyCopy);
        curl_slist_free_all(headers);
        return 0;
    }

    curl = curl_easy_init();
    if(curl == NULL) {
        cn1WebFreeSecret(certCopy, certLength);
        cn1WebFreeSecret(keyCopy, keyLength);
        cn1WebFreeSecret(passwordCopy, passwordLength);
        free(r); free(urlCopy); free(methodCopy); free(bodyCopy);
        curl_slist_free_all(headers);
        return 0;
    }
    memset(&h2check, 0, sizeof(h2check));
    h2check.curl = curl;
    curl_easy_setopt(curl, CURLOPT_URL, urlCopy);
    /* HTTP AND HTTPS, AND NOTHING ELSE, on the first request and on every
       redirect. libcurl otherwise speaks whatever its build enabled, and the
       builds shipped here enable file:// -- measured, "file:///etc/hosts" came
       back as 684 bytes of the file. Urls.requireHttp refuses that on the Java
       side of both arms; this is the second lock on the same door, and the one
       that also covers a REDIRECT into file:// or ftp://, which no check on the
       caller's URL can see.

       CURLOPT_PROTOCOLS_STR is the current spelling and CURLOPT_PROTOCOLS the
       one before it, deprecated in 7.85.0. Guarded on the VERSION rather than
       #ifdef, because these are enum members and not macros -- the same trap
       CURLOPT_PATH_AS_IS fell into below, where an #ifdef was silently always
       false and the option was never set on any libcurl. */
#if LIBCURL_VERSION_NUM >= 0x075500
    curl_easy_setopt(curl, CURLOPT_PROTOCOLS_STR, "http,https");
    curl_easy_setopt(curl, CURLOPT_REDIR_PROTOCOLS_STR, "http,https");
#else
    curl_easy_setopt(curl, CURLOPT_PROTOCOLS, (long)(CURLPROTO_HTTP | CURLPROTO_HTTPS));
    curl_easy_setopt(curl, CURLOPT_REDIR_PROTOCOLS,
                     (long)(CURLPROTO_HTTP | CURLPROTO_HTTPS));
#endif
    /* The path goes out exactly as the caller wrote it.
       libcurl otherwise resolves "." and ".." before sending, while the SigV4
       signature was computed over the UNNORMALISED path -- so an S3 key with a dot
       segment in it is signed for one path and requested at another, and comes back
       SignatureDoesNotMatch. The JavaSE path does not normalise, so such a key works
       under cn1:backend and fails only once packaged. */
/* Guarded on the VERSION, not on #ifdef. CURLOPT_PATH_AS_IS is an enum member
   -- curl.h declares it as CURLOPT(CURLOPT_PATH_AS_IS, CURLOPTTYPE_LONG, 234),
   not as a macro -- so the preprocessor has never heard of it and the #ifdef
   this replaces was always false. The option was therefore never set, on any
   libcurl, and the guard read as if it were.
   What that costs: libcurl normalises dot segments, so a request for an S3 key
   holding "a/../b" goes out as "/b" while Aws signed "/a/../b", and the service
   answers SignatureDoesNotMatch. The Java SE arm sends the path as written, so
   the key works under cn1:backend and fails once packaged -- which is exactly
   the divergence the comment above claims to prevent.
   7.42.0 is where the option appeared. */
#if LIBCURL_VERSION_NUM >= 0x072A00
    curl_easy_setopt(curl, CURLOPT_PATH_AS_IS, 1L);
#endif
    /* The CA bundle a deployment names. Web's documentation tells a scratch
       image to point CURL_CA_BUNDLE or SSL_CERT_FILE at its bundle, "both read
       by libcurl itself" -- and neither is: CURL_CA_BUNDLE is read by the curl
       COMMAND, and SSL_CERT_FILE is OpenSSL's default path, which libcurl never
       consults once it was built with a bundle path of its own, as both shipped
       builds are. Measured on the musl and the glibc binary: with either
       variable naming the CA, verification still failed with "self-signed
       certificate in certificate chain". So they are read here, in that order.
       This only chooses WHICH anchors verify the server; verification itself
       stays on. */
    {
        const char* bundle = getenv("CURL_CA_BUNDLE");
        if(bundle == NULL || bundle[0] == 0) {
            bundle = getenv("SSL_CERT_FILE");
        }
        if(bundle != NULL && bundle[0] != 0) {
            curl_easy_setopt(curl, CURLOPT_CAINFO, bundle);
        }
    }
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, cn1WebWrite);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, r);
    curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, cn1WebHeader);
    curl_easy_setopt(curl, CURLOPT_HEADERDATA, r);
    curl_easy_setopt(curl, CURLOPT_ERRORBUFFER, r->error);
    /* Following a redirect RESENDS the caller's headers to wherever it points.
       libcurl drops Authorization when the host changes, but it knows nothing
       about X-Api-Key, X-Amz-Security-Token or any other bearer a caller
       invented, and those go to the new host in full. A 3xx from a service that
       has been taken over, or simply one that redirects off-domain, is then
       enough to hand an attacker the credential -- the caller never sees where
       its header went.
       So redirects are followed freely only when the caller supplied NO headers,
       where there is nothing to leak. With headers, following is restricted to
       the same host where libcurl can express that, and otherwise not done at
       all: the 3xx and its Location are returned as the response, which the
       caller can act on deliberately.

       AND A BODY IS A SECRET TOO, which "nothing to leak" was reading as though
       only header fields could carry one. A 307 or a 308 preserves the method
       AND the payload -- that is the whole difference between them and a 301 or
       302, which libcurl turns into a GET -- so a POST redirected off-domain
       arrives at the new host complete: a form with a password in it, a token
       exchange, a signed document. libcurl drops Authorization when the host
       changes and has never done anything of the kind for the body, because
       there is no general way to know what is in one. A bodied request is also
       the state-changing kind, so following blindly can mean performing it twice,
       the second time somewhere the caller never named. It therefore takes the
       same restricted path a headered one does. */
    /* AND ONLY FOR A SAFE METHOD. An empty DELETE or PUT carries neither a
       header nor a body, so the two tests above say there is nothing to leak --
       and there is not, but leaking is only half of it. A 307 or a 308 preserves
       the METHOD, so following one repeats the state change at a Location this
       caller never chose: a taken-over or merely misconfigured endpoint answers
       one redirect and the delete happens on its host instead. GET and HEAD are
       the methods RFC 9110 4.2.1 calls safe, and they are the only ones followed
       freely here. A method spelled any other way -- lower case, unknown, or
       absent-but-not-null -- is treated as unsafe, which is the direction to be
       wrong in; a NULL method is libcurl's own default GET. */
    safeMethod = methodCopy == NULL || strcmp(methodCopy, "GET") == 0
            || strcmp(methodCopy, "HEAD") == 0;
    /* A CLIENT CERTIFICATE IS A CREDENTIAL TOO. The blob stays on this handle
       across a redirect, so following one freely presents the certificate to
       whatever host the Location names, and a TLS server there that asks for
       one authenticates this caller. A certificate therefore restricts the
       redirect policy exactly as a caller's header does. The Java SE twin keeps
       its socket factory across a redirect for the same reason and decides the
       same way. */
    if(!callerHeaders && bodyLength == 0 && safeMethod && certificate == JAVA_NULL) {
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
    } else {
        /* This #ifdef may never fire, for the same reason the PATH_AS_IS one
           above did not: if libcurl declares CURLFOLLOW_SAMEHOST through its
           CURLOPT-style enum rather than as a macro, the preprocessor cannot see
           it. Left as it is deliberately, because the two fail in OPPOSITE
           directions. There, a guard that never fires left the option off and
           the request wrong; here it selects the #else, which does not follow
           the redirect at all -- more restrictive than intended, and still the
           safe answer, since the whole point is not to carry the caller's
           headers to another host. A version guard is not written for it because
           the release that introduced the constant cannot be checked from here;
           an unverified version number would be a worse guess than this. */
#ifdef CURLFOLLOW_SAMEHOST
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, (long)CURLFOLLOW_SAMEHOST);
#else
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 0L);
#endif
    }
    curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 5L);
    /* A CONNECT deadline and a STALL deadline, not a deadline on the whole
       transfer. CURLOPT_TIMEOUT caps the entire operation, so a large upload or
       download that is progressing perfectly well is aborted at 30 seconds for
       no reason other than its size -- and the Java SE arm does not do that: it
       sets a READ timeout, which fires only when a single read stalls. The two
       have to agree, or an S3 object big enough to take half a minute transfers
       under cn1:backend and fails once packaged.
       LOW_SPEED_LIMIT/LOW_SPEED_TIME is libcurl's spelling of the same idea:
       give up when the transfer makes essentially no progress for 30s. */
    if((flags & CN1_WEB_FLAG_CLIENT) == 0) {
        curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 30L);
        curl_easy_setopt(curl, CURLOPT_LOW_SPEED_LIMIT, 1L);
        curl_easy_setopt(curl, CURLOPT_LOW_SPEED_TIME, 30L);
    } else {
        /* A WebClient's own. 0 is "no limit" in WebClient and in the Java SE
           twin, but CONNECTTIMEOUT's 0 means libcurl's built-in 300 seconds,
           so no limit is spelled as the largest value it takes. */
        curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT_MS,
                connectTimeoutMillis > 0 ? (long)connectTimeoutMillis : 0x7fffffffL);
        if(readTimeoutMillis > 0) {
            /* Whole seconds, rounded UP: a 1500ms stall bound must not become a
               1s one. Documented on WebClient.setReadTimeoutMillis. */
            curl_easy_setopt(curl, CURLOPT_LOW_SPEED_LIMIT, 1L);
            curl_easy_setopt(curl, CURLOPT_LOW_SPEED_TIME,
                    /* Widened first: a JAVA_INT near its maximum would wrap when
                       999 is added and hand libcurl a negative time, which it
                       rejects -- leaving no stall bound at all. */
                    ((long)readTimeoutMillis + 999L) / 1000L);
        }
        if(requestTimeoutMillis > 0) {
            curl_easy_setopt(curl, CURLOPT_TIMEOUT_MS, (long)requestTimeoutMillis);
        }
        if(flags & CN1_WEB_FLAG_NO_REUSE) {
            curl_easy_setopt(curl, CURLOPT_FRESH_CONNECT, 1L);
            curl_easy_setopt(curl, CURLOPT_FORBID_REUSE, 1L);
        }
        if(flags & CN1_WEB_FLAG_REQUIRE_HTTP2) {
            /* PRIOR KNOWLEDGE, not 2TLS. 2TLS asks for HTTP/2 and settles for
               HTTP/1.1, and by the time the answer says which was used the
               request -- a POST, a DELETE -- has already been sent to the peer
               the caller ruled out. With prior knowledge the only ALPN offered is
               h2 and the connection speaks HTTP/2 from its first byte, so an
               HTTP/1.1-only peer fails the handshake or the preface before any
               request reaches it. The version check after the transfer is the
               second lock on the same door. */
            curl_easy_setopt(curl, CURLOPT_HTTP_VERSION,
                    (long)CURL_HTTP_VERSION_2_PRIOR_KNOWLEDGE);
#if LIBCURL_VERSION_NUM >= 0x075000
            curl_easy_setopt(curl, CURLOPT_PREREQFUNCTION, cn1WebRequireH2Prereq);
            curl_easy_setopt(curl, CURLOPT_PREREQDATA, &h2check);
#endif
        } else if(curl_version_info(CURLVERSION_NOW)->features & CURL_VERSION_HTTP2) {
            /* Offered, and HTTP/1.1 when the server declines -- the default of
               every libcurl since 7.62, set so an older one behaves the same. */
            curl_easy_setopt(curl, CURLOPT_HTTP_VERSION, (long)CURL_HTTP_VERSION_2TLS);
        }
    }
    if(certCopy != NULL) {
        /* As blobs, so the credential never touches the filesystem, and NOCOPY
           so the only copy is ours, which is overwritten before it is freed.
           libcurl takes what it keeps for a connection's TLS configuration from
           these during the transfer. 7.71.0 is where the blob options appeared;
           a libcurl older than that fails the request rather than silently
           sending it without the certificate. */
#if LIBCURL_VERSION_NUM >= 0x074700
        struct curl_blob certBlob;
        certBlob.data = certCopy;
        certBlob.len = certLength;
        certBlob.flags = CURL_BLOB_NOCOPY;
        curl_easy_setopt(curl, CURLOPT_SSLCERT_BLOB, &certBlob);
        curl_easy_setopt(curl, CURLOPT_SSLCERTTYPE,
                certificateType == CN1_WEB_CERT_PKCS12 ? "P12" : "PEM");
        if(keyCopy != NULL) {
            struct curl_blob keyBlob;
            keyBlob.data = keyCopy;
            keyBlob.len = keyLength;
            keyBlob.flags = CURL_BLOB_NOCOPY;
            curl_easy_setopt(curl, CURLOPT_SSLKEY_BLOB, &keyBlob);
            curl_easy_setopt(curl, CURLOPT_SSLKEYTYPE, "PEM");
        }
        if(passwordCopy != NULL) {
            curl_easy_setopt(curl, CURLOPT_KEYPASSWD, passwordCopy);
        }
#else
        strncpy(r->error, "this libcurl predates 7.71.0 and cannot present a client "
                "certificate", CURL_ERROR_SIZE - 1);
        r->status = -1;
        curl_easy_cleanup(curl);
        curl_slist_free_all(headers);
        cn1WebFreeSecret(certCopy, certLength);
        cn1WebFreeSecret(keyCopy, keyLength);
        cn1WebFreeSecret(passwordCopy, passwordLength);
        free(urlCopy); free(methodCopy); free(bodyCopy);
        return (JAVA_LONG)(intptr_t)r;
#endif
    }
    if((flags & CN1_WEB_FLAG_REQUIRE_HTTP2)
            && !(curl_version_info(CURLVERSION_NOW)->features & CURL_VERSION_HTTP2)) {
        strncpy(r->error, "HTTP/2 is required, and this binary's libcurl was built "
                "without HTTP/2 support", CURL_ERROR_SIZE - 1);
        r->status = -1;
        curl_easy_cleanup(curl);
        curl_slist_free_all(headers);
        cn1WebFreeSecret(certCopy, certLength);
        cn1WebFreeSecret(keyCopy, keyLength);
        cn1WebFreeSecret(passwordCopy, passwordLength);
        free(urlCopy); free(methodCopy); free(bodyCopy);
        return (JAVA_LONG)(intptr_t)r;
    }
    curl_easy_setopt(curl, CURLOPT_NOSIGNAL, 1L);
    curl_easy_setopt(curl, CURLOPT_USERAGENT, "codenameone-backend");
    /* CN1_WEB_VERBOSE=1 makes libcurl narrate the exchange on stderr. Off by
     * default and read per request rather than cached, so it can be turned on for
     * a running process through its environment without a rebuild. Request headers
     * carry credentials, so this is a debugging switch, not a logging one.
     *
     * AN EXPLICIT TRUE VALUE, which is what the line above always claimed and the
     * test below did not do: presence alone turned it on, so a deployment that
     * spells a disabled flag "CN1_WEB_VERBOSE=0" -- or "false", or empty -- got
     * the trace anyway, and with it every bearer token, API key and AWS session
     * credential the process sent, in its application log. The operator had
     * written the word for off. */
    if(cn1WebVerboseEnabled()) {
        curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
    }
    if(headers != NULL) {
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    }
    if(methodCopy != NULL) {
        curl_easy_setopt(curl, CURLOPT_CUSTOMREQUEST, methodCopy);
        /* CUSTOMREQUEST only changes the METHOD WORD. For HEAD that is not
         * enough: libcurl still expects a response body, so it reads the
         * Content-Length the server reports for the entity it is NOT sending and
         * waits for bytes that never arrive -- a hang until CURLOPT_TIMEOUT, with
         * "0 out of N bytes received". NOBODY is what tells it the response ends
         * at the headers. Found by S3.headObject, which is the first HEAD this
         * client ever sent. */
        if(strcmp(methodCopy, "HEAD") == 0) {
            curl_easy_setopt(curl, CURLOPT_NOBODY, 1L);
        }
    }
    /* AND ONLY WHEN THERE IS SOMETHING TO SEND, or a verb that asked for it.
       An empty byte array is not null, and the copy above allocates one byte for
       a zero-length body so that a failed malloc stays distinguishable -- so
       bodyCopy was non-null for request(null, url, null, new byte[0]) and setting
       POSTFIELDS put libcurl into POST mode. The caller supplied no method, which
       means GET on the Java SE twin and on every reading of this API: the same
       call reached a different route, or performed a state change, only once
       packaged. An explicit verb keeps the old behaviour, empty body and all,
       because CUSTOMREQUEST has already said what it is and a Content-Length of
       zero is part of what it means. */
    if(bodyCopy != NULL && (bodyLength > 0 || methodCopy != NULL)) {
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, bodyCopy);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, (long)bodyLength);
    }

    /* The transfer blocks; yield so the concurrent collector is not stalled by it.
       On a virtual thread it PARKS instead of blocking, so its host goes on
       running every other virtual thread; see cn1WebPerformParked. */
    CN1_YIELD_THREAD;
#ifndef _WIN32
    {
        CN1WebPool* pool = (CN1WebPool*)(intptr_t)poolHandle;
        CN1WebPooled* pooled = pool == NULL ? NULL : cn1WebPoolTake(pool);
        if(pool != NULL && pooled == NULL) {
            rc = CURLE_OUT_OF_MEMORY;
        } else if(pooled != NULL) {
            rc = cn1WebDrive(pooled->multi, &pooled->watch, curl, cn1BackendCanPark());
            /* The easy handle leaves the multi inside cn1WebDrive; its
               connection stays in the multi's cache for the next request. */
            cn1WebPoolGive(pool, pooled);
        } else if(cn1BackendCanPark()) {
            rc = cn1WebDrive(NULL, NULL, curl, 1);
        } else {
            rc = curl_easy_perform(curl);
        }
    }
#else
    (void)poolHandle;
    rc = curl_easy_perform(curl);
#endif
    CN1_RESUME_THREAD;

    if(rc == CURLE_OK) {
        curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &r->status);
#if LIBCURL_VERSION_NUM >= 0x073200
        curl_easy_getinfo(curl, CURLINFO_HTTP_VERSION, &r->httpVersion);
#endif
        if((flags & CN1_WEB_FLAG_REQUIRE_HTTP2) && r->httpVersion != CURL_HTTP_VERSION_2_0) {
            r->status = -1;
            snprintf(r->error, CURL_ERROR_SIZE, "HTTP/2 is required, and the server "
                    "answered over %s", r->httpVersion == CURL_HTTP_VERSION_1_1 ? "HTTP/1.1"
                    : r->httpVersion == CURL_HTTP_VERSION_1_0 ? "HTTP/1.0" : "another protocol");
        }
    } else {
        r->status = -1;
        if(r->error[0] == 0) {
            const char* msg = curl_easy_strerror(rc);
            strncpy(r->error, msg == NULL ? "transfer failed" : msg, CURL_ERROR_SIZE - 1);
        }
    }
    if(rc != CURLE_OK && h2check.refused) {
        snprintf(r->error, CURL_ERROR_SIZE, "HTTP/2 is required, and the server "
                "negotiated %s%s; the request was not sent",
                h2check.chosen[0] == 0 ? "no protocol (no ALPN answer)" : h2check.chosen,
                h2check.chosen[0] == 0 ? ", which is HTTP/1.1" : " instead");
    } else if(rc != CURLE_OK && (flags & CN1_WEB_FLAG_REQUIRE_HTTP2)
            && (rc == CURLE_HTTP2 || rc == CURLE_HTTP2_STREAM
                || rc == CURLE_WEIRD_SERVER_REPLY || rc == CURLE_UNSUPPORTED_PROTOCOL)) {
        /* An HTTP/1.1-only peer over plain http answers the HTTP/2 preface with
           something that is not HTTP/2, and libcurl reports what it saw rather
           than why. Only the protocol failures get this: a TLS or certificate
           error under the same flag is reported as itself. */
        char detail[CURL_ERROR_SIZE];
        memcpy(detail, r->error, CURL_ERROR_SIZE);
        detail[CURL_ERROR_SIZE - 1] = 0;
        snprintf(r->error, CURL_ERROR_SIZE, "HTTP/2 is required and could not be "
                "used with this server: %s", detail);
    }
    curl_easy_cleanup(curl);
    curl_slist_free_all(headers);
    cn1WebFreeSecret(certCopy, certLength);
    cn1WebFreeSecret(keyCopy, keyLength);
    cn1WebFreeSecret(passwordCopy, passwordLength);
    free(urlCopy); free(methodCopy); free(bodyCopy);
    return (JAVA_LONG)(intptr_t)r;
}

JAVA_INT com_codename1_backend_Web_protocolImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1WebResponse* r = (CN1WebResponse*)(intptr_t)handle;
    return r == NULL ? 0 : (JAVA_INT)r->httpVersion;
}

JAVA_INT com_codename1_backend_Web_statusImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1WebResponse* r = (CN1WebResponse*)(intptr_t)handle;
    return r == NULL ? -1 : (JAVA_INT)r->status;
}

JAVA_OBJECT com_codename1_backend_Web_errorImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1WebResponse* r = (CN1WebResponse*)(intptr_t)handle;
    if(r == NULL || r->error[0] == 0) {
        return JAVA_NULL;
    }
    return newStringFromUtf8Len(threadStateData, r->error, (int)strlen(r->error));
}

JAVA_OBJECT com_codename1_backend_Web_bodyImpl___long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1WebResponse* r = (CN1WebResponse*)(intptr_t)handle;
    JAVA_OBJECT arr;
    if(r == NULL) {
        return JAVA_NULL;
    }
    if(r->length > CN1_WEB_MAX_BODY_BYTES) {
        /* Unreachable while cn1WebWrite holds the line above, and checked anyway:
           the cast below is what turns a length this size into a negative one,
           and the memcpy after it does not consult the array's length. */
        return JAVA_NULL;
    }
    arr = allocArray(threadStateData, (int)r->length, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if(r->length > 0 && r->data != NULL) {
        memcpy((JAVA_ARRAY_BYTE*)CN1_ARRAY_DATA(arr), r->data, r->length);
    }
    return arr;
}

JAVA_OBJECT com_codename1_backend_Web_headersImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1WebResponse* r = (CN1WebResponse*)(intptr_t)handle;
    if(r == NULL || r->headers == NULL) {
        return JAVA_NULL;
    }
    /* The upstream's header block, octet for octet. Field values are not
       required to be UTF-8, and Web.parseHeaders splits and compares this the
       way the HTTP/1 server does -- byte to char. */
    return newStringFromAsciiLen(threadStateData, r->headers, (int)r->headerLength);
}

JAVA_VOID com_codename1_backend_Web_freeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1WebResponse* r = (CN1WebResponse*)(intptr_t)handle;
    if(r != NULL) {
        free(r->data);
        free(r->headers);
        free(r);
    }
}

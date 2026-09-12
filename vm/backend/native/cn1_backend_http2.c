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
 * HTTP/2 on nghttp2.
 *
 * The framing layer is not written here and should not be. HPACK alone is a
 * static table, a dynamic table with eviction, and Huffman coding, and getting any
 * of it subtly wrong produces a connection that works until it does not. nghttp2
 * is the library curl already links, and it owns framing, HPACK, flow control,
 * stream state, priority, CONTINUATION reassembly and GOAWAY.
 *
 * What is written here is the shape of the boundary. nghttp2 is callback-driven,
 * but this deliberately does NOT call back into Java: a C callback reaching into
 * the VM has to survive dead-code elimination and must not run while the collector
 * is moving, and neither is worth arranging for a protocol adapter. Instead the
 * callbacks accumulate COMPLETED requests into a queue on the session, and Java
 * pulls from it. Data flows one way across the boundary at a time, and every
 * native here is a plain function that returns.
 */
#include "cn1_globals.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <nghttp2/nghttp2.h>

#define CN1_H2_MAX_HEADERS 64
/* HttpServer.MAX_HEADER_BYTES. The count above bounds how MANY fields arrive,
   never how large they are, so without this a peer stays under 64 fields and
   still spends this process's memory a megabyte at a time -- across
   CONTINUATION frames, and again on each stream its SETTINGS allows at once.
   HTTP/1 has always refused that; this is the same ceiling for HTTP/2. */
#define CN1_H2_MAX_HEADER_BYTES (64 * 1024)
/* And a ceiling across the whole session, for the same reason the body limit has
   one: the per-stream figure is what ONE request may hold, and a client may hold
   the advertised stream concurrency open at once without ever sending END_STREAM,
   so the per-stream ceiling alone permits that multiple. Periodic control frames
   keep such a connection alive indefinitely. */
#define CN1_H2_MAX_SESSION_HEADER_BYTES (4 * CN1_H2_MAX_HEADER_BYTES)
/* The per-stream limit bounds ONE upload; it says nothing about how many run at
   once. With the advertised concurrency a single connection could hold a hundred
   nearly-complete 8 MiB bodies -- some 800 MiB of native buffers that live until
   each stream completes or resets, and nothing stopped a second connection doing
   the same. This is the ceiling for everything one session is holding. */
#define CN1_H2_MAX_SESSION_BODY_BYTES (4 * CN1_H2_MAX_BODY_BYTES)
/* Mirrors HttpServer.MAX_BODY_BYTES: the HTTP/1 paths refuse a larger body and
   HTTP/2 must agree, or the limit is only as good as the protocol chosen. */
#define CN1_H2_MAX_BODY_BYTES (8 * 1024 * 1024)

typedef struct CN1H2Header {
    char* name;
    char* value;
} CN1H2Header;

typedef struct CN1H2Request {
    int32_t streamId;
    char* method;
    char* path;
    char* scheme;
    char* authority;
    CN1H2Header headers[CN1_H2_MAX_HEADERS];
    int headerCount;
    size_t headerBytes;
    unsigned char* body;
    size_t bodyLength;
    size_t bodyCapacity;
    int complete;
    struct CN1H2Request* next;
} CN1H2Request;

/*
 * One response body, owned by the stream that is sending it.
 *
 * This used to be a single buffer on the session, and nghttp2 reads a body AFTER
 * submit returns, while it pumps output. serveHttp2 submits every finished
 * response before it drains, so with two streams completing in one receive cycle
 * the second submit freed the first one's buffer and reset the shared offset:
 * whichever body was submitted last got sent for both streams, or an empty one
 * did. The provider's own source pointer is what nghttp2 offers for exactly this,
 * and the session keeps the list so a stream reset before EOF still frees.
 */
typedef struct CN1H2Body {
    int32_t streamId;
    /* Exactly one of these carries the body. `data` is a buffer this owns; `fd` is
       an open descriptor this owns and reads each frame out of, which is how a file
       is served without its size ever existing in the heap. */
    unsigned char* data;
    int fd;
    int64_t fileOffset;
    size_t length;
    size_t offset;
    struct CN1H2Body* next;
} CN1H2Body;

typedef struct {
    nghttp2_session* session;
    /* Streams still being received, and requests ready for Java to take. */
    CN1H2Request* open;
    CN1H2Request* readyHead;
    CN1H2Request* readyTail;
    CN1H2Request* current;   /* the one Java is currently reading */
    /* Bytes nghttp2 wants written to the socket. Java drains this. */
    unsigned char* out;
    size_t outLength;
    size_t outCapacity;
    /* Response bodies still being written, one per stream. See CN1H2Body. */
    struct CN1H2Body* bodies;
} CN1H2Session;

/*
 * Frees one body, closing the descriptor when that is what it holds.
 *
 * Separate from cn1H2ReleaseBody, which unlinks it first, so that teardown can walk
 * the list straight through instead of searching it once per body -- and so that
 * there is no way to free a body without closing its descriptor. Doing it inline in
 * two places is how the descriptor leaked from the teardown path: a client that
 * dropped the connection mid-download left one open per request.
 */
/* File-backed response bodies alive across ALL sessions. Descriptors are a
   process resource, not a per-connection one: bounding them per session still
   multiplies by the connection count, and running out stops the process
   accepting sockets or opening files at all -- a failure with nothing to do
   with whichever client caused it. Every such body is created in respondFile
   and destroyed in cn1H2FreeBody, so those two are the whole accounting. */
static _Atomic long cn1H2OpenFileBodies = 0;

/* Heap held by submitted response bodies across ALL sessions, for the same
   reason the descriptors are counted that way: a per-session limit is a limit
   per CONNECTION, and the connection ceiling is in the thousands. Each session
   pausing itself after one oversized body still lets the process hold that body
   times every connection, which is gigabytes of native memory pinned by small
   GET requests whose senders never open their windows. Maintained at the three
   points that already exist -- submitted, drained, freed -- so it cannot drift
   from what the bodies actually hold. */
static _Atomic long cn1H2PendingBodyBytes = 0;

/* The ceiling cn1H2PendingBodyBytes is reserved against, or 0 for none.
   Kept here rather than passed per call so that the RESERVATION can sit next to
   the allocation it bounds: a limit tested in Java and enforced in C is two
   steps with a gap, and two sessions processed at once both read the counter
   below the limit and then both allocate. Set once from Java at startup. */
static _Atomic long cn1H2MaxBodyBytes = 0;

/* The ceiling cn1H2OpenFileBodies is reserved against, or 0 for none. Same
   reasoning as the byte ceiling above: tested in Java and taken in C is two
   steps with a gap, so every worker finishing a file response at once passed
   the check before any of them incremented, and the process-wide cap was really
   the cap plus one per concurrent worker -- each holding a DESCRIPTOR. */
static _Atomic long cn1H2MaxFileBodies = 0;

/* Reserves one descriptor slot, atomically. Returns 0 when the ceiling is
   reached, in which case nothing is taken. */
static int cn1H2ReserveFileBody(void) {
    long limit = atomic_load_explicit(&cn1H2MaxFileBodies, memory_order_relaxed);
    long current = atomic_load_explicit(&cn1H2OpenFileBodies, memory_order_relaxed);
    for(;;) {
        if(limit > 0 && current + 1 > limit) {
            return 0;
        }
        if(atomic_compare_exchange_weak_explicit(&cn1H2OpenFileBodies, &current,
                                                 current + 1,
                                                 memory_order_relaxed,
                                                 memory_order_relaxed)) {
            return 1;
        }
    }
}

/* Reserves `bytes` against the ceiling, atomically. Returns 0 when the
   reservation would cross it, in which case nothing is added. */
static int cn1H2ReserveBodyBytes(long bytes) {
    long limit = atomic_load_explicit(&cn1H2MaxBodyBytes, memory_order_relaxed);
    long current = atomic_load_explicit(&cn1H2PendingBodyBytes, memory_order_relaxed);
    for(;;) {
        if(limit > 0 && current + bytes > limit) {
            return 0;
        }
        if(atomic_compare_exchange_weak_explicit(&cn1H2PendingBodyBytes, &current,
                                                 current + bytes,
                                                 memory_order_relaxed,
                                                 memory_order_relaxed)) {
            return 1;
        }
        /* current now holds what another thread left; try again against that. */
    }
}

/* And the INBOUND side, for the identical reason. The per-session ceilings below
   bound one connection; the connection ceiling is in the thousands, so a few
   clients holding streams just under their session limit still add up to the
   whole machine. Counted where a request's bytes are added -- header fields and
   body chunks -- and released in cn1H2FreeRequest, which is the one place a
   request's memory goes away. */
static _Atomic long cn1H2InboundBytes = 0;
/* The ceiling on that total. Four sessions' worth: enough that no honest client
   meets it, small enough that a dishonest fleet cannot walk past it. */
#define CN1_H2_MAX_PROCESS_INBOUND_BYTES (4 * (CN1_H2_MAX_SESSION_BODY_BYTES \
        + CN1_H2_MAX_SESSION_HEADER_BYTES))

/* Reserves `bytes` of that total, atomically, exactly as cn1H2ReserveBodyBytes
   does for the pending side. Returns 0 when the reservation would cross the
   ceiling, in which case nothing is added.

   A load, a comparison and an add is not a reservation: every worker processing
   a session can read the same below-the-ceiling total and then add its own
   share. That was written off as an overshoot of one DATA chunk per session,
   which was wrong twice over. The charge is a CAPACITY DELTA, not a chunk -- the
   buffer grows to twice what is needed, so the last doubling under an 8MB
   per-body ceiling reserves about 4MB for a 16KB frame -- and it is bounded by
   the worker count, so the default 16 can walk tens of megabytes past a ceiling
   other sessions have already filled. A client choosing its upload sizes decides
   when they all cross together.

   The caller charges BEFORE allocating and gives the reservation back if the
   allocation fails, which is what keeps the counter from drifting up: the
   previous order -- test, allocate, charge -- was chosen for that same reason
   and is what made the test and the charge two steps. */
static int cn1H2ReserveInboundBytes(long bytes) {
    long current = atomic_load_explicit(&cn1H2InboundBytes, memory_order_relaxed);
    for(;;) {
        if(current + bytes > CN1_H2_MAX_PROCESS_INBOUND_BYTES) {
            return 0;
        }
        if(atomic_compare_exchange_weak_explicit(&cn1H2InboundBytes, &current,
                                                 current + bytes,
                                                 memory_order_relaxed,
                                                 memory_order_relaxed)) {
            return 1;
        }
        /* current now holds what another worker left; try again against that. */
    }
}

/* Gives back a reservation the allocation it was made for did not use. */
static void cn1H2ReleaseInboundBytes(long bytes) {
    atomic_fetch_sub_explicit(&cn1H2InboundBytes, bytes, memory_order_relaxed);
}

static void cn1H2FreeBody(CN1H2Body* body) {
    /* The WHOLE length, matching what was reserved: the buffer is one allocation
       and free() below returns all of it at once, however much of it had been
       sent. Releasing only the unsent remainder here paired with a per-byte
       decrement on the send path, which together tracked "bytes not yet written"
       rather than "bytes still held" -- and the ceiling exists for the second. A
       file body holds no buffer and was never charged. */
    if(body->data != NULL) {
        atomic_fetch_sub_explicit(&cn1H2PendingBodyBytes,
                (long)body->length, memory_order_relaxed);
    }
    if(body->fd >= 0) {
        atomic_fetch_sub_explicit(&cn1H2OpenFileBodies, 1, memory_order_relaxed);
        /* The descriptor became the session's when the response was submitted, so
           this is the one place that closes it: at EOF, at an early stream reset,
           and at teardown, all of which arrive here. */
        close(body->fd);
    }
    free(body->data);
    free(body);
}

static void cn1H2ReleaseBody(CN1H2Session* s, CN1H2Body* body) {
    CN1H2Body** link = &s->bodies;
    while(*link != NULL) {
        if(*link == body) {
            *link = body->next;
            break;
        }
        link = &(*link)->next;
    }
    cn1H2FreeBody(body);
}

static void cn1H2ReleaseBodyForStream(CN1H2Session* s, int32_t streamId) {
    CN1H2Body* body = s->bodies;
    while(body != NULL) {
        CN1H2Body* next = body->next;
        if(body->streamId == streamId) {
            cn1H2ReleaseBody(s, body);
        }
        body = next;
    }
}

static CN1H2Request* cn1H2FindOpen(CN1H2Session* s, int32_t streamId) {
    CN1H2Request* r = s->open;
    while(r != NULL) {
        if(r->streamId == streamId) {
            return r;
        }
        r = r->next;
    }
    return NULL;
}

static void cn1H2FreeRequest(CN1H2Request* r) {
    int i;
    if(r == NULL) {
        return;
    }
    /* bodyCapacity, matching what the growth path charged: free() below returns
       the whole allocation, not the part of it that was filled. */
    atomic_fetch_sub_explicit(&cn1H2InboundBytes,
            (long)(r->bodyCapacity + r->headerBytes + sizeof(CN1H2Request)),
            memory_order_relaxed);
    free(r->method);
    free(r->path);
    free(r->scheme);
    free(r->authority);
    for(i = 0 ; i < r->headerCount ; i++) {
        free(r->headers[i].name);
        free(r->headers[i].value);
    }
    free(r->body);
    free(r);
}

static void cn1H2Unlink(CN1H2Request** list, CN1H2Request* target) {
    CN1H2Request** link = list;
    while(*link != NULL) {
        if(*link == target) {
            *link = target->next;
            target->next = NULL;
            return;
        }
        link = &(*link)->next;
    }
}

static void cn1H2Enqueue(CN1H2Session* s, CN1H2Request* r) {
    r->next = NULL;
    if(s->readyTail == NULL) {
        s->readyHead = r;
        s->readyTail = r;
    } else {
        s->readyTail->next = r;
        s->readyTail = r;
    }
}

/* nghttp2 hands us bytes to put on the wire; they are buffered for Java to drain. */
/* How much serialised output one session may hold before it has to be drained.
   nghttp2 will happily fill this in one nghttp2_session_send() -- it emits as
   much as the peer's flow-control window allows -- so a client that raises its
   windows and asks for a large file could grow this buffer toward the whole
   window before Java got a chance to write any of it out. Returning WOULDBLOCK
   is the backpressure nghttp2 understands: it stops, keeps what it has not
   handed over, and offers it again after the drain. */
#define CN1_H2_MAX_OUT_BYTES (1024 * 1024)

/* What a session is worth keeping between responses. The same figure as
   HttpServer.MAX_IDLE_BUFFER_BYTES, which bounds the HTTP/1.1 side's per
   connection buffers for the same reason and with the same arithmetic behind it:
   the connection ceiling is in the thousands, so anything a session holds while
   it is doing nothing is multiplied by that. */
#define CN1_H2_IDLE_OUT_BYTES (16 * 1024)

static ssize_t cn1H2Send(nghttp2_session* session, const uint8_t* data, size_t length,
                         int flags, void* userData) {
    CN1H2Session* s = (CN1H2Session*)userData;
    (void)session;
    (void)flags;
    if(s->outLength >= CN1_H2_MAX_OUT_BYTES) {
        return NGHTTP2_ERR_WOULDBLOCK;
    }
    if(s->outLength + length > s->outCapacity) {
        size_t grown = (s->outLength + length) * 2 + 4096;
        unsigned char* buf = (unsigned char*)realloc(s->out, grown);
        if(buf == NULL) {
            return NGHTTP2_ERR_CALLBACK_FAILURE;
        }
        s->out = buf;
        s->outCapacity = grown;
    }
    memcpy(s->out + s->outLength, data, length);
    s->outLength += length;
    return (ssize_t)length;
}

static int cn1H2OnBeginHeaders(nghttp2_session* session, const nghttp2_frame* frame,
                               void* userData) {
    CN1H2Session* s = (CN1H2Session*)userData;
    CN1H2Request* r;
    (void)session;
    if(frame->hd.type != NGHTTP2_HEADERS || frame->headers.cat != NGHTTP2_HCAT_REQUEST) {
        return 0;
    }
    /* The STRUCTURE counts too, not only what arrives in it. It embeds
       CN1_H2_MAX_HEADERS slots, so one is about a kilobyte before a single
       header byte is read -- and a client that opens the advertised stream
       concurrency and sends minimal headers keeps the payload counters near
       zero while holding one of these per stream, per connection. Counted as
       what it is: a fixed cost per open request, charged here and released in
       cn1H2FreeRequest with everything else the request holds. */
    if(!cn1H2ReserveInboundBytes((long)sizeof(CN1H2Request))) {
        /* Refusing the stream rather than the connection: nghttp2 resets this
           one and the peer's other streams carry on, which is the proportionate
           answer to a process that is momentarily full. */
        return NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE;
    }
    r = (CN1H2Request*)calloc(1, sizeof(CN1H2Request));
    if(r == NULL) {
        cn1H2ReleaseInboundBytes((long)sizeof(CN1H2Request));
        return NGHTTP2_ERR_CALLBACK_FAILURE;
    }
    r->streamId = frame->hd.stream_id;
    r->next = s->open;
    s->open = r;
    return 0;
}

static char* cn1H2Dup(const uint8_t* value, size_t length) {
    char* out = (char*)malloc(length + 1);
    if(out == NULL) {
        return NULL;
    }
    memcpy(out, value, length);
    out[length] = 0;
    return out;
}

static int cn1H2OnHeader(nghttp2_session* session, const nghttp2_frame* frame,
                         const uint8_t* name, size_t nameLen,
                         const uint8_t* value, size_t valueLen,
                         uint8_t flags, void* userData) {
    CN1H2Session* s = (CN1H2Session*)userData;
    CN1H2Request* r;
    (void)session;
    (void)flags;
    if(frame->hd.type != NGHTTP2_HEADERS) {
        return 0;
    }
    r = cn1H2FindOpen(s, frame->hd.stream_id);
    if(r == NULL) {
        return 0;
    }
    /* Charged before anything is duplicated, and charged for the pseudo-headers
       too: a single enormous :path would otherwise walk straight past a ceiling
       that only looked at ordinary fields. */
    r->headerBytes += (size_t)nameLen + (size_t)valueLen;
    /* Charged in the SAME breath as r->headerBytes, and before every rejection
       below, because cn1H2FreeRequest gives back r->headerBytes whichever way
       this stream ends. Charging after the checks -- which is what this did --
       left the rejected field counted by headerBytes and never added to the
       global, so the free subtracted bytes the global had never gained and the
       total drifted DOWNWARD. Repeat a rejected header block and the process
       cap stops being a cap at all, which is the opposite of what it is for.
       The two figures have to move together or neither means anything. */
    atomic_fetch_add_explicit(&cn1H2InboundBytes, (long)(nameLen + valueLen),
                              memory_order_relaxed);
    if(r->headerBytes > CN1_H2_MAX_HEADER_BYTES) {
        return NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE;
    }
    {
        /* The same walk the body limit does, and bounded the same way: the
           streams are capped by the concurrency setting, and a field is capped
           by the per-stream ceiling above, so this cannot become the expensive
           part of parsing a header block. r is already on s->open, so its own
           bytes are counted by the walk rather than added to it. */
        size_t total = 0;
        CN1H2Request* other = s->open;
        while(other != NULL) {
            total += other->headerBytes;
            other = other->next;
        }
        other = s->readyHead;
        while(other != NULL) {
            total += other->headerBytes;
            other = other->next;
        }
        if(total > CN1_H2_MAX_SESSION_HEADER_BYTES) {
            return NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE;
        }
    }
    /* Already charged above, so this only asks whether the process is over. */
    if(atomic_load_explicit(&cn1H2InboundBytes, memory_order_relaxed)
            > CN1_H2_MAX_PROCESS_INBOUND_BYTES) {
        return NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE;
    }
    /* The pseudo-headers carry what a request line carries in HTTP/1.1.
       A FAILED COPY FAILS THE STREAM, the same answer the ordinary header below
       gives to the same failure: reporting success left the request queued with a
       required pseudo-header missing, and it was answered much later with an
       unrelated 400, 501 or 500 that says nothing about what happened. */
    if(nameLen == 7 && memcmp(name, ":method", 7) == 0) {
        r->method = cn1H2Dup(value, valueLen);
        return r->method == NULL ? NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE : 0;
    }
    if(nameLen == 5 && memcmp(name, ":path", 5) == 0) {
        r->path = cn1H2Dup(value, valueLen);
        return r->path == NULL ? NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE : 0;
    }
    if(nameLen == 7 && memcmp(name, ":scheme", 7) == 0) {
        r->scheme = cn1H2Dup(value, valueLen);
        return r->scheme == NULL ? NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE : 0;
    }
    if(nameLen == 10 && memcmp(name, ":authority", 10) == 0) {
        r->authority = cn1H2Dup(value, valueLen);
        return r->authority == NULL ? NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE : 0;
    }
    if(nameLen > 0 && name[0] == ':') {
        return 0; /* an unknown pseudo-header; nghttp2 has already validated it */
    }
    if(r->headerCount >= CN1_H2_MAX_HEADERS) {
        /* Reset the stream rather than keep the first CN1_H2_MAX_HEADERS and report
           success, which handed the handler a request with a later cookie,
           content-type or tracing header simply missing -- and nothing anywhere said
           so. TEMPORAL_CALLBACK_FAILURE fails this one stream and leaves the
           connection up; the client sees the request fail, which is the honest
           answer and the closest thing HTTP/2 has to HTTP/1's 431. */
        return NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE;
    }
    {
        /* BOTH COPIES OR NEITHER. Either malloc can fail under native memory
           pressure, and incrementing headerCount over a half-copied entry
           published a NULL that headerNameImpl/headerValueImpl then handed to
           strlen -- taking the whole backend down instead of this one stream.
           Failing the stream is the answer the row above already gives for too
           many headers, and it leaves the connection up. */
        char* dupName = cn1H2Dup(name, nameLen);
        char* dupValue = dupName == NULL ? NULL : cn1H2Dup(value, valueLen);
        if(dupName == NULL || dupValue == NULL) {
            free(dupName);
            free(dupValue);
            return NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE;
        }
        r->headers[r->headerCount].name = dupName;
        r->headers[r->headerCount].value = dupValue;
        r->headerCount++;
    }
    return 0;
}

static int cn1H2OnData(nghttp2_session* session, uint8_t flags, int32_t streamId,
                       const uint8_t* data, size_t length, void* userData) {
    CN1H2Session* s = (CN1H2Session*)userData;
    CN1H2Request* r = cn1H2FindOpen(s, streamId);
    (void)session;
    (void)flags;
    if(r == NULL) {
        return 0;
    }
    /* The same ceiling both HTTP/1 framing paths enforce (HttpServer.MAX_BODY_BYTES).
       Without it a peer can stream DATA on one stream -- or on each of the streams
       its SETTINGS allows at once -- until this process is out of native memory,
       which the HTTP/1 side simply does not permit. Resetting the stream rather
       than failing the callback keeps the connection and its other streams alive:
       one oversized upload is that request's problem, not the session's. */
    if(r->bodyLength + length > CN1_H2_MAX_BODY_BYTES) {
        return NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE;
    }
    {
        /* Walked rather than counted in a running total: the total would have to
           be decremented everywhere a request is freed, and one missed path
           leaks budget until the session refuses everything. Both lists hold
           bodies -- open ones are still arriving, ready ones are waiting for
           Java to read them -- and neither is longer than the concurrency
           setting. r is on the open list, so its own bodyLength is already in
           the sum and only the new bytes are added. */
        size_t total = length;
        CN1H2Request* other = s->open;
        while(other != NULL) {
            total += other->bodyLength;
            other = other->next;
        }
        other = s->readyHead;
        while(other != NULL) {
            total += other->bodyLength;
            other = other->next;
        }
        if(total > CN1_H2_MAX_SESSION_BODY_BYTES) {
            return NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE;
        }
    }
    /* Reserved before the buffer grows and given back if the growth fails, so
       the counter neither drifts up nor admits two workers on the strength of
       one reading. cn1H2FreeRequest gives back the capacity when the request
       ends, which is the other half of the same arrangement. */
    if(r->bodyLength + length > r->bodyCapacity) {
        /* CAPACITY, NOT LENGTH, on both the test and the charge. This grows to
           twice what is needed, so charging the payload left roughly half of every
           buffer unaccounted -- a client that stopped just after a growth boundary
           held 8MB while the guard had been told 4MB, and across the permitted
           sessions and streams that is a process-wide ceiling admitting twice what
           it says. The capacity is what free() gives back, so it is what the
           counter has to follow.

           RESERVED, not tested and then charged. The delta below is a capacity
           step, so the last doubling under the per-body ceiling reserves about
           4MB for one 16KB frame: every worker reading the same total before any
           of them adds to it is how a fleet of sessions crosses the process
           ceiling together, by far more than the one chunk each this used to
           claim. Given back below if the growth fails, which is the whole reason
           the charge used to come after it. */
        size_t grown = (r->bodyLength + length) * 2 + 1024;
        size_t added;
        unsigned char* buf;
        if(grown > CN1_H2_MAX_BODY_BYTES) {
            grown = CN1_H2_MAX_BODY_BYTES;
        }
        added = grown - r->bodyCapacity;
        if(!cn1H2ReserveInboundBytes((long)added)) {
            return NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE;
        }
        buf = (unsigned char*)realloc(r->body, grown);
        if(buf == NULL) {
            cn1H2ReleaseInboundBytes((long)added);
            return NGHTTP2_ERR_CALLBACK_FAILURE;
        }
        r->body = buf;
        r->bodyCapacity = grown;
    }
    memcpy(r->body + r->bodyLength, data, length);
    r->bodyLength += length;
    return 0;
}

/*
 * Whether the `length` bytes at `at` are `lower`, ignoring ASCII case.
 *
 * A field VALUE is not required to be lower case -- only a field name is -- so
 * an Expect token has to be compared this way. Folded by hand rather than with
 * strncasecmp, which folds by the C locale: the token is ASCII by specification
 * and must not depend on where the process is running, which is the same reason
 * nothing in this project reaches for toLowerCase on a protocol token.
 */
static int cn1H2EqualsLower(const char* at, size_t length, const char* lower) {
    size_t iter;
    for(iter = 0 ; iter < length ; iter++) {
        char c = at[iter];
        if(c >= 'A' && c <= 'Z') {
            c = (char)(c - 'A' + 'a');
        }
        if(c != lower[iter]) {
            return 0;
        }
    }
    return lower[length] == 0 ? 1 : 0;
}

/*
 * Whether every token of an Expect field is 100-continue, which is the only
 * expectation this server can make true.
 *
 * The twin of HttpServer.onlyExpects100Continue, token for token, including its
 * two decisions: EVERY token is tested rather than just whether the one we know
 * is among them -- "100-continue, custom-extension" carries something nobody can
 * satisfy -- and an empty field expects nothing, so it is not an expectation
 * this server has met.
 */
static int cn1H2OnlyExpects100Continue(const char* value) {
    const char* at = value;
    int any = 0;
    for(;;) {
        const char* comma = strchr(at, ',');
        const char* end = comma == NULL ? at + strlen(at) : comma;
        const char* start = at;
        size_t length;
        while(start < end && (*start == ' ' || *start == '\t')) {
            start++;
        }
        while(end > start && (end[-1] == ' ' || end[-1] == '\t')) {
            end--;
        }
        length = (size_t)(end - start);
        if(length > 0) {
            any = 1;
            if(length != 12 || cn1H2EqualsLower(start, 12, "100-continue") == 0) {
                return 0;
            }
        }
        if(comma == NULL) {
            return any;
        }
        at = comma + 1;
    }
}

/*
 * Answers the Expect field as soon as the request's headers are in, rather than
 * after a body that is waiting to be invited.
 *
 * The point of 100-continue is that the client does not send the body until it is
 * told to. Enqueuing a request only on END_STREAM meant serveHttp2 never saw one
 * that was waiting, so it could not answer, and the client sat until its own
 * timeout before sending anyway -- every upload from such a client paying that
 * wait. And an expectation this server cannot satisfy was simply ignored here,
 * while the HTTP/1.1 path refuses it with 417: the same request answered two
 * ways depending on the protocol that carried it.
 *
 * Answered in C because this is where the headers are, and because the interim
 * response has to go out before the stream is anything Java can see. The rule
 * itself is the same rule, and the tests check both protocols against it.
 *
 * Returns 1 when the stream has been answered and freed, 0 when it continues.
 */
static int cn1H2ResolveExpect(CN1H2Session* s, CN1H2Request* r, int bodyToCome) {
    int found = 0;
    int satisfiable = 1;
    int iter;
    /* EVERY occurrence, not the first. A list field may be split across field
       lines -- "expect: 100-continue" then "expect: custom-extension" is the same
       field value as the comma form, and RFC 9110 5.3 says a recipient combines
       them -- so stopping at the first one answered 100 to a client that had also
       asked for something nobody can satisfy. The HTTP/1.1 path never had this
       hole: getHeader joins repeated fields with ", " before the same rule reads
       them, which is why only this loop needed the correction. */
    for(iter = 0 ; iter < r->headerCount ; iter++) {
        /* Lower case by the protocol: RFC 9113 requires it of a field name and
           nghttp2 has already refused anything else, which is how the
           pseudo-headers above are matched too. */
        if(r->headers[iter].name == NULL
                || strcmp(r->headers[iter].name, "expect") != 0) {
            continue;
        }
        found = 1;
        if(cn1H2OnlyExpects100Continue(r->headers[iter].value) == 0) {
            satisfiable = 0;
        }
    }
    if(found == 0) {
        return 0;
    }
    if(satisfiable == 0) {
        /* 417, now. Per-stream rather than closing the connection, which is the
           h2 answer to one bad request -- the status is what has to agree with
           the HTTP/1.1 path, not the framing. */
        nghttp2_nv nva[1];
        nva[0].name = (uint8_t*)":status";
        nva[0].namelen = 7;
        nva[0].value = (uint8_t*)"417";
        nva[0].valuelen = 3;
        nva[0].flags = NGHTTP2_NV_FLAG_NONE;
        nghttp2_submit_response(s->session, r->streamId, nva, 1, NULL);
        cn1H2Unlink(&s->open, r);
        cn1H2FreeRequest(r);
        return 1;
    }
    if(bodyToCome) {
        /* The interim response the client is waiting for. A failure to submit it
           is not fatal to the stream: the client sends the body once its own wait
           expires, which is exactly the behaviour this removes. */
        nghttp2_nv nva[1];
        nva[0].name = (uint8_t*)":status";
        nva[0].namelen = 7;
        nva[0].value = (uint8_t*)"100";
        nva[0].valuelen = 3;
        nva[0].flags = NGHTTP2_NV_FLAG_NONE;
        nghttp2_submit_headers(s->session, NGHTTP2_FLAG_NONE, r->streamId,
                               NULL, nva, 1, NULL);
    }
    return 0;
}

static int cn1H2OnFrameRecv(nghttp2_session* session, const nghttp2_frame* frame,
                            void* userData) {
    CN1H2Session* s = (CN1H2Session*)userData;
    CN1H2Request* r;
    int endStream = (frame->hd.flags & NGHTTP2_FLAG_END_STREAM) != 0;
    (void)session;
    if(frame->hd.type != NGHTTP2_HEADERS && frame->hd.type != NGHTTP2_DATA) {
        return 0;
    }
    r = cn1H2FindOpen(s, frame->hd.stream_id);
    if(r == NULL) {
        return 0;
    }
    /* BEFORE the END_STREAM test below, because a request that is waiting to be
       told to send its body has not set it. */
    if(frame->hd.type == NGHTTP2_HEADERS
            && frame->headers.cat == NGHTTP2_HCAT_REQUEST
            && cn1H2ResolveExpect(s, r, endStream == 0)) {
        return 0;
    }
    if(endStream == 0) {
        return 0;
    }
    /* The request is complete only now: END_STREAM is what says the client has
       finished, whether it arrived on HEADERS or on the last DATA frame. */
    cn1H2Unlink(&s->open, r);
    r->complete = 1;
    cn1H2Enqueue(s, r);
    return 0;
}

static int cn1H2OnStreamClose(nghttp2_session* session, int32_t streamId,
                              uint32_t errorCode, void* userData) {
    CN1H2Session* s = (CN1H2Session*)userData;
    CN1H2Request* r = cn1H2FindOpen(s, streamId);
    (void)session;
    (void)errorCode;
    if(r != NULL) {
        /* Reset before it completed: drop it rather than leak the stream state. */
        cn1H2Unlink(&s->open, r);
        cn1H2FreeRequest(r);
    } else {
        /* Not open, so it may be COMPLETE and waiting for Java to take it. Looking
           only at the open list left a cancelled request queued: serveHttp2() then
           ran its handler and tried to respond on a stream nghttp2 had already
           closed, and that failure reached the outer catch and dropped the whole
           connection -- resetting every other stream multiplexed on it. Not
           s->current, which Java is reading right now; nextRequest frees that one
           when it moves on. */
        CN1H2Request* ready = s->readyHead;
        while(ready != NULL && ready->streamId != streamId) {
            ready = ready->next;
        }
        if(ready != NULL) {
            cn1H2Unlink(&s->readyHead, ready);
            /* cn1H2Unlink does not know about the tail, and this may have BEEN the
               tail. The list is bounded by the concurrency setting, so finding the
               new one is cheaper than a second link to keep in step. */
            s->readyTail = s->readyHead;
            while(s->readyTail != NULL && s->readyTail->next != NULL) {
                s->readyTail = s->readyTail->next;
            }
            cn1H2FreeRequest(ready);
        }
    }
    /* A response whose body nghttp2 never read to EOF -- the peer reset the stream,
       or the body limit above reset it -- would otherwise sit on the list until the
       session ends. */
    cn1H2ReleaseBodyForStream(s, streamId);
    return 0;
}

JAVA_LONG com_codename1_backend_Http2_createImpl___R_long(CODENAME_ONE_THREAD_STATE) {
    nghttp2_session_callbacks* callbacks;
    CN1H2Session* s;
    nghttp2_settings_entry settings[2];

    s = (CN1H2Session*)calloc(1, sizeof(CN1H2Session));
    if(s == NULL) {
        return 0;
    }
    if(nghttp2_session_callbacks_new(&callbacks) != 0) {
        free(s);
        return 0;
    }
    nghttp2_session_callbacks_set_send_callback(callbacks, cn1H2Send);
    nghttp2_session_callbacks_set_on_begin_headers_callback(callbacks, cn1H2OnBeginHeaders);
    nghttp2_session_callbacks_set_on_header_callback(callbacks, cn1H2OnHeader);
    nghttp2_session_callbacks_set_on_data_chunk_recv_callback(callbacks, cn1H2OnData);
    nghttp2_session_callbacks_set_on_frame_recv_callback(callbacks, cn1H2OnFrameRecv);
    nghttp2_session_callbacks_set_on_stream_close_callback(callbacks, cn1H2OnStreamClose);

    if(nghttp2_session_server_new(&s->session, callbacks, s) != 0) {
        nghttp2_session_callbacks_del(callbacks);
        free(s);
        return 0;
    }
    nghttp2_session_callbacks_del(callbacks);

    /* The connection preface. A server MUST send SETTINGS first; a client that
       does not see it will not proceed. */
    settings[0].settings_id = NGHTTP2_SETTINGS_MAX_CONCURRENT_STREAMS;
    settings[0].value = 100;
    settings[1].settings_id = NGHTTP2_SETTINGS_INITIAL_WINDOW_SIZE;
    settings[1].value = 1024 * 1024;
    if(nghttp2_submit_settings(s->session, NGHTTP2_FLAG_NONE, settings, 2) != 0) {
        nghttp2_session_del(s->session);
        free(s);
        return 0;
    }
    return (JAVA_LONG)(intptr_t)s;
}

/* Feeds received bytes in. Returns how many were consumed, or -1. */
JAVA_INT com_codename1_backend_Http2_receiveImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    JAVA_ARRAY_BYTE* data;
    ssize_t n;
    if(s == NULL || buffer == JAVA_NULL) {
        return -1;
    }
    data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    n = nghttp2_session_mem_recv(s->session, (const uint8_t*)&data[offset], (size_t)length);
    return n < 0 ? -1 : (JAVA_INT)n;
}

/* Runs nghttp2's output side, filling the outbound buffer. */
JAVA_INT com_codename1_backend_Http2_pumpImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL) {
        return -1;
    }
    return nghttp2_session_send(s->session) == 0 ? 0 : -1;
}

JAVA_INT com_codename1_backend_Http2_pendingOutputImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    return s == NULL ? 0 : (JAVA_INT)s->outLength;
}

/*
 * Heap held by response bodies that have been SUBMITTED and not yet fully
 * written. This is not outLength: that buffer is what nghttp2 has already
 * serialised, while a submitted body is pulled from its provider only as the
 * peer's flow-control window allows. A client that stops sending WINDOW_UPDATE
 * therefore leaves every body it asked for retained here, which is the figure a
 * caller has to cap -- flushing frees nothing when the window is shut.
 *
 * A file-backed body owns a descriptor rather than a buffer, so it adds no heap
 * and is not counted; descriptors are bounded by the stream concurrency limit.
 */
JAVA_LONG com_codename1_backend_Http2_pendingBodyBytesImpl___long_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    CN1H2Body* body;
    int64_t total = 0;
    if(s == NULL) {
        return 0;
    }
    for(body = s->bodies ; body != NULL ; body = body->next) {
        if(body->data != NULL && body->length > body->offset) {
            total += (int64_t)(body->length - body->offset);
        }
    }
    return (JAVA_LONG)total;
}

/*
 * File-backed response bodies outstanding across the process. Reported
 * separately from the byte figure because it is a different resource with a
 * different limit: such a body holds a DESCRIPTOR and no heap, so it is
 * invisible to the byte accounting, and a peer that never opens its window
 * keeps one per stream for as long as it likes.
 */
/*
 * Response-body heap outstanding across the PROCESS. The per-session figure says
 * what one connection is holding; this says what the machine is holding, which
 * is the number that decides whether there is memory left.
 */
JAVA_LONG com_codename1_backend_Http2_pendingBodyBytesAllImpl___R_long(CODENAME_ONE_THREAD_STATE) {
    return (JAVA_LONG)atomic_load_explicit(&cn1H2PendingBodyBytes, memory_order_relaxed);
}

JAVA_INT com_codename1_backend_Http2_pendingBodyFilesImpl___R_int(CODENAME_ONE_THREAD_STATE) {
    return (JAVA_INT)atomic_load_explicit(&cn1H2OpenFileBodies, memory_order_relaxed);
}

/* The ceiling for outstanding response bodies across the process. */
JAVA_VOID com_codename1_backend_Http2_setMaxBodyBytesImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG limit) {
    atomic_store_explicit(&cn1H2MaxBodyBytes, (long)limit, memory_order_relaxed);
}

/* The ceiling for outstanding file-backed response bodies across the process. */
JAVA_VOID com_codename1_backend_Http2_setMaxFileBodiesImpl___int(CODENAME_ONE_THREAD_STATE, JAVA_INT limit) {
    atomic_store_explicit(&cn1H2MaxFileBodies, (long)limit, memory_order_relaxed);
}

/* Takes everything nghttp2 wants written, and empties the buffer. */
JAVA_OBJECT com_codename1_backend_Http2_drainImpl___long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    JAVA_OBJECT arr;
    if(s == NULL) {
        return JAVA_NULL;
    }
    arr = allocArray(threadStateData, (int)s->outLength, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if(s->outLength > 0) {
        memcpy((JAVA_ARRAY_BYTE*)((JAVA_ARRAY)arr)->data, s->out, s->outLength);
        s->outLength = 0;
    }
    /* Give the CAPACITY back too, not just the length. A session that once sent
       something large otherwise keeps that buffer for as long as it stays open,
       and a keep-alive pool of them holds every peak it ever reached.

       ONE CONSTANT for the test and for the size it shrinks to, because them
       being two different numbers is what went wrong: the test asked for more
       than CN1_H2_MAX_OUT_BYTES while the shrink went to 8KB, so a peer whose
       window kept the buffer just under a megabyte -- which the doubling growth
       lands on easily -- was never shrunk at all, and that capacity is in no
       accounting the process-wide ceilings cover. Multiplied by the connection
       limit, a client that fills and drains one response per connection and then
       leaves it open holds gigabytes the response-body guard never sees.

       So: anything above the idle size goes back to the idle size. The cost is a
       realloc per drain for a session whose framing exceeds 16KB, against a
       memcpy of the whole payload and a write syscall on the same path. */
    if(s->outCapacity > CN1_H2_IDLE_OUT_BYTES) {
        unsigned char* shrunk = (unsigned char*)realloc(s->out,
                                                        CN1_H2_IDLE_OUT_BYTES);
        if(shrunk != NULL) {
            s->out = shrunk;
            s->outCapacity = CN1_H2_IDLE_OUT_BYTES;
        }
    }
    return arr;
}

/*
 * Makes the next completed request current, so the accessors below describe it.
 * Returns its stream id, or -1 when there is none waiting.
 */
JAVA_INT com_codename1_backend_Http2_nextRequestImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL) {
        return -1;
    }
    if(s->current != NULL) {
        cn1H2FreeRequest(s->current);
        s->current = NULL;
    }
    if(s->readyHead == NULL) {
        return -1;
    }
    s->current = s->readyHead;
    s->readyHead = s->readyHead->next;
    if(s->readyHead == NULL) {
        s->readyTail = NULL;
    }
    s->current->next = NULL;
    return (JAVA_INT)s->current->streamId;
}

JAVA_OBJECT com_codename1_backend_Http2_methodImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL || s->current == NULL || s->current->method == NULL) {
        return JAVA_NULL;
    }
    /* Byte for byte, as asciiString() in HttpServer does for HTTP/1. Not a
       UTF-8 decode: an HTTP target is a sequence of OCTETS, and decoding
       them here made Request.byteAt -- which narrows each char back to a
       byte for percentDecode -- see different bytes than arrived. */
    return newStringFromAsciiLen(threadStateData, s->current->method,
                                 (int)strlen(s->current->method));
}

JAVA_OBJECT com_codename1_backend_Http2_pathImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL || s->current == NULL || s->current->path == NULL) {
        return JAVA_NULL;
    }
    /* Byte for byte, as asciiString() in HttpServer does for HTTP/1. Not a
       UTF-8 decode: an HTTP target is a sequence of OCTETS, and decoding
       them here made Request.byteAt -- which narrows each char back to a
       byte for percentDecode -- see different bytes than arrived. */
    return newStringFromAsciiLen(threadStateData, s->current->path,
                                 (int)strlen(s->current->path));
}

JAVA_OBJECT com_codename1_backend_Http2_authorityImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL || s->current == NULL || s->current->authority == NULL) {
        return JAVA_NULL;
    }
    /* Byte for byte, as asciiString() in HttpServer does for HTTP/1. Not a
       UTF-8 decode: an HTTP target is a sequence of OCTETS, and decoding
       them here made Request.byteAt -- which narrows each char back to a
       byte for percentDecode -- see different bytes than arrived. */
    return newStringFromAsciiLen(threadStateData, s->current->authority,
                                 (int)strlen(s->current->authority));
}

JAVA_INT com_codename1_backend_Http2_headerCountImpl___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    return (s == NULL || s->current == NULL) ? 0 : (JAVA_INT)s->current->headerCount;
}

JAVA_OBJECT com_codename1_backend_Http2_headerNameImpl___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_INT index) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL || s->current == NULL || index < 0 || index >= s->current->headerCount) {
        return JAVA_NULL;
    }
    /* A field value is OCTETS, not text: RFC 9110 allows obs-text, and the
       HTTP/1 path widens each byte to a char. Decoding UTF-8 here turned a
       lone 0xE9 into U+FFFD and collapsed a multi-byte run into one
       character, so a handler or a @RequestHeader binding saw a different
       value depending on which protocol carried it. */
    return newStringFromAsciiLen(threadStateData, s->current->headers[index].name,
            (int)strlen(s->current->headers[index].name));
}

JAVA_OBJECT com_codename1_backend_Http2_headerValueImpl___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_INT index) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL || s->current == NULL || index < 0 || index >= s->current->headerCount) {
        return JAVA_NULL;
    }
    /* A field value is OCTETS, not text: RFC 9110 allows obs-text, and the
       HTTP/1 path widens each byte to a char. Decoding UTF-8 here turned a
       lone 0xE9 into U+FFFD and collapsed a multi-byte run into one
       character, so a handler or a @RequestHeader binding saw a different
       value depending on which protocol carried it. */
    return newStringFromAsciiLen(threadStateData, s->current->headers[index].value,
            (int)strlen(s->current->headers[index].value));
}

JAVA_OBJECT com_codename1_backend_Http2_bodyImpl___long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    JAVA_OBJECT arr;
    size_t length;
    if(s == NULL || s->current == NULL) {
        return JAVA_NULL;
    }
    length = s->current->bodyLength;
    arr = allocArray(threadStateData, (int)length, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if(length > 0) {
        memcpy((JAVA_ARRAY_BYTE*)((JAVA_ARRAY)arr)->data, s->current->body, length);
    }
    return arr;
}

/* nghttp2 reads the response body through this, after submit returns. */
static ssize_t cn1H2ReadBody(nghttp2_session* session, int32_t streamId, uint8_t* buf,
                             size_t length, uint32_t* dataFlags, nghttp2_data_source* source,
                             void* userData) {
    CN1H2Session* s = (CN1H2Session*)userData;
    CN1H2Body* body = (CN1H2Body*)source->ptr;
    size_t remaining;
    (void)session;
    (void)streamId;
    if(body == NULL) {
        *dataFlags |= NGHTTP2_DATA_FLAG_EOF;
        return 0;
    }
    remaining = body->length - body->offset;
    if(remaining > length) {
        remaining = length;
    }
    if(remaining > 0) {
        if(body->fd >= 0) {
            /* Straight into nghttp2's frame buffer. pread rather than read so the
               descriptor needs no seek position of its own -- two streams may be
               serving the same file. */
            ssize_t got;
            /* Retried here rather than deferred. NGHTTP2_ERR_DEFERRED suspends the
               provider until nghttp2_session_resume_data() is called, and nothing
               calls it -- a single EINTR would have left the download open and
               silent forever. A regular file never returns EAGAIN, so an error that
               is not EINTR is a real one. */
            do {
                got = pread(body->fd, buf, remaining,
                            (off_t)(body->fileOffset + (int64_t)body->offset));
            } while(got < 0 && errno == EINTR);
            if(got < 0) {
                return NGHTTP2_ERR_TEMPORAL_CALLBACK_FAILURE;
            }
            if(got == 0) {
                /* The file is shorter than Content-Length said -- it was truncated
                   under us. Ending the stream here sends fewer bytes than promised,
                   which the client detects; stalling forever would not. */
                *dataFlags |= NGHTTP2_DATA_FLAG_EOF;
                cn1H2ReleaseBody(s, body);
                return 0;
            }
            remaining = (size_t)got;
        } else {
            memcpy(buf, body->data + body->offset, remaining);
        }
        body->offset += remaining;
        /* NOT DECREMENTED HERE. What the ceiling bounds is memory held, and
           body->data stays allocated at its full length until the stream ends --
           sending a byte frees nothing. Counting sent bytes instead let a client
           grant credit for all but the last byte of one body after another and
           leave each multi-megabyte allocation charged at one byte, so the budget
           admitted the next one. The whole allocation is released in
           cn1H2FreeBody, which is the moment the memory actually goes away. */
    }
    if(body->offset >= body->length) {
        *dataFlags |= NGHTTP2_DATA_FLAG_EOF;
        cn1H2ReleaseBody(s, body);
    }
    return (ssize_t)remaining;
}

/*
 * Builds the response header block shared by both response forms.
 *
 * The nva entries point INTO *statusOut and *headerOut, which the caller frees
 * after submitting -- nghttp2 copies what it needs during the submit call. Returns
 * the header count, or -1 when the status could not be read.
 */
static long cn1H2BuildHeaders(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT status,
                              JAVA_OBJECT headerLines, nghttp2_nv* nva,
                              char** statusOut, char** headerOut) {
    char* statusCopy;
    char* headerCopy = NULL;
    size_t count = 0;

    *statusOut = NULL;
    *headerOut = NULL;
    {
        const char* tmp = stringToUTF8(threadStateData, status);
        if(tmp == NULL) {
            return -1;
        }
        statusCopy = strdup(tmp);
        if(statusCopy == NULL) {
            return -1;
        }
    }
    if(headerLines != JAVA_NULL) {
        /* BYTES, one per field-value character, because that is what a field
           value is -- see Http2.headerBytes. stringToUTF8 was encoding them, so
           an obs-text character the server's own validation accepts went out as
           one byte over HTTP/1.1 and two here. */
        int headerLength = (int)((JAVA_ARRAY)headerLines)->length;
        if(headerLength > 0) {
            headerCopy = (char*)malloc((size_t)headerLength + 1);
            if(headerCopy != NULL) {
                memcpy(headerCopy, (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)headerLines)->data,
                       (size_t)headerLength);
                /* Terminated because the parsing below walks a C string. Nothing
                   in the block can be a NUL: the validation refuses every
                   character under 0x20. */
                headerCopy[headerLength] = 0;
            }
            if(headerCopy == NULL) {
                /* A FAILED COPY IS NOT "NO HEADERS". Leaving headerCopy null here
                   is indistinguishable below from a response that carried none, so
                   the status and body went out and reported success while every
                   header the handler set was dropped -- the same silent loss the
                   comment below describes for a block that did not fit, which is
                   why that one counts first. A Set-Cookie that does not travel
                   ends the session; a CORS or cache header that does not travel
                   changes what the client is allowed to do with the answer. */
                free(statusCopy);
                return -1;
            }
        }
    }
    *statusOut = statusCopy;
    *headerOut = headerCopy;

    nva[count].name = (uint8_t*)":status";
    nva[count].namelen = 7;
    nva[count].value = (uint8_t*)statusCopy;
    nva[count].valuelen = strlen(statusCopy);
    nva[count].flags = NGHTTP2_NV_FLAG_NONE;
    count++;

    if(headerCopy != NULL) {
        char* line = headerCopy;
        /* Counted first, because running out of room used to end the loop quietly:
           the response went out with the headers that fit and reported success, so
           a late Set-Cookie, a CORS header or a security header simply was not
           there over HTTP/2 while HTTP/1 sent all of them. Refusing is the honest
           answer -- the handler asked for something this path cannot deliver. */
        {
            int wanted = count;
            char* scan = headerCopy;
            while(scan != NULL && *scan != 0) {
                char* nl = strchr(scan, '\n');
                char* colon = strchr(scan, ':');
                /* The colon has to be on THIS line: strchr runs to the end of the
                   whole block, so a colon further down would have counted a line
                   that has none, and the count would refuse responses that fit. */
                if(colon != NULL && (nl == NULL || colon < nl)) {
                    wanted++;
                }
                scan = nl == NULL ? NULL : nl + 1;
            }
            if(wanted > CN1_H2_MAX_HEADERS) {
                free(statusCopy);
                free(headerCopy);
                return -1;
            }
        }
        while(line != NULL && *line != 0 && count < CN1_H2_MAX_HEADERS) {
            char* nl = strchr(line, '\n');
            char* colon;
            if(nl != NULL) {
                *nl = 0;
            }
            colon = strchr(line, ':');
            if(colon != NULL) {
                char* value = colon + 1;
                *colon = 0;
                while(*value == ' ') {
                    value++;
                }
                /* HTTP/2 header names must be lower case; a capital is a protocol
                   error the peer will reset the stream over. */
                {
                    char* c = line;
                    while(*c != 0) {
                        if(*c >= 'A' && *c <= 'Z') {
                            *c = (char)(*c - 'A' + 'a');
                        }
                        c++;
                    }
                }
                /* Connection-specific headers are forbidden in HTTP/2. */
                if(strcmp(line, "connection") != 0 && strcmp(line, "keep-alive") != 0
                        && strcmp(line, "transfer-encoding") != 0 && strcmp(line, "upgrade") != 0) {
                    nva[count].name = (uint8_t*)line;
                    nva[count].namelen = strlen(line);
                    nva[count].value = (uint8_t*)value;
                    nva[count].valuelen = strlen(value);
                    nva[count].flags = NGHTTP2_NV_FLAG_NONE;
                    count++;
                }
            }
            line = nl == NULL ? NULL : nl + 1;
        }
    }
    return (long)count;
}

/*
 * Submits a response. headerLines is "name: value" separated by '\n'; the status
 * is passed separately because :status is a pseudo-header nghttp2 requires first.
 */
JAVA_INT com_codename1_backend_Http2_respondImpl___long_int_java_lang_String_byte_1ARRAY_byte_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_INT streamId, JAVA_OBJECT status, JAVA_OBJECT headerLines, JAVA_OBJECT body) {
    CN1H2Body* pending;
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    nghttp2_nv nva[CN1_H2_MAX_HEADERS + 1];
    char* headerCopy = NULL;
    char* statusCopy = NULL;
    size_t count = 0;
    nghttp2_data_provider provider;
    int rc;

    if(s == NULL) {
        return -1;
    }
    {
        long built = cn1H2BuildHeaders(threadStateData, status, headerLines, nva,
                                       &statusCopy, &headerCopy);
        if(built < 0) {
            return -1;
        }
        count = (size_t)built;
    }

    /* A resubmission for the same stream would otherwise leave the old one to be
       freed only at stream close. */
    cn1H2ReleaseBodyForStream(s, streamId);
    pending = NULL;
    if(body != JAVA_NULL && ((JAVA_ARRAY)body)->length > 0) {
        JAVA_ARRAY arr = (JAVA_ARRAY)body;
        /* RESERVED first. Charging after the copy spends exactly what the
           ceiling exists to withhold, and does it once per session that happens
           to be running -- so the real peak was the limit plus a body for every
           concurrent responder, whatever the configured number said. */
        if(!cn1H2ReserveBodyBytes((long)arr->length)) {
            free(statusCopy);
            free(headerCopy);
            return -2;
        }
        pending = (CN1H2Body*)malloc(sizeof(CN1H2Body));
        if(pending != NULL) {
            pending->data = (unsigned char*)malloc((size_t)arr->length);
            if(pending->data == NULL) {
                free(pending);
                pending = NULL;
            } else {
                memcpy(pending->data, (JAVA_ARRAY_BYTE*)arr->data, (size_t)arr->length);
                pending->streamId = streamId;
                pending->fd = -1;
                pending->fileOffset = 0;
                pending->length = (size_t)arr->length;
                pending->offset = 0;
                pending->next = s->bodies;
                s->bodies = pending;
            }
        }
        if(pending == NULL) {
            /* The reservation outlived its body; give it back or the ceiling
               ratchets down one failed allocation at a time. */
            atomic_fetch_sub_explicit(&cn1H2PendingBodyBytes, (long)arr->length,
                                      memory_order_relaxed);
            /* The body could not be copied. Submitting anyway sends the headers with
               an EMPTY body and reports success, so the caller ships a 200 whose
               content silently went missing under memory pressure. Failing here lets
               it be seen. */
            free(statusCopy);
            free(headerCopy);
            return -1;
        }
    }
    provider.source.ptr = pending;
    provider.read_callback = cn1H2ReadBody;

    rc = nghttp2_submit_response(s->session, streamId, nva, count,
                                 pending != NULL ? &provider : NULL);
    free(statusCopy);
    free(headerCopy);
    return rc == 0 ? 0 : -1;
}

/*
 * Submits a response whose body is a range of an open file.
 *
 * The descriptor becomes the session's here, whatever happens: on the failure paths
 * below and, once submitted, when the body is released at EOF, at an early stream
 * reset or at teardown. A caller that closed it itself would pull the file out from
 * under the provider mid-response.
 */
JAVA_INT com_codename1_backend_Http2_respondFileImpl___long_int_java_lang_String_byte_1ARRAY_int_long_long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_INT streamId, JAVA_OBJECT status, JAVA_OBJECT headerLines, JAVA_INT fd, JAVA_LONG offset, JAVA_LONG length) {
    CN1H2Body* pending;
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    nghttp2_nv nva[CN1_H2_MAX_HEADERS + 1];
    char* headerCopy = NULL;
    char* statusCopy = NULL;
    size_t count = 0;
    nghttp2_data_provider provider;
    int rc;

    if(s == NULL || fd < 0) {
        if(fd >= 0) {
            close(fd);
        }
        return -1;
    }
    {
        long built = cn1H2BuildHeaders(threadStateData, status, headerLines, nva,
                                       &statusCopy, &headerCopy);
        if(built < 0) {
            close(fd);
            return -1;
        }
        count = (size_t)built;
    }

    cn1H2ReleaseBodyForStream(s, streamId);
    pending = (CN1H2Body*)malloc(sizeof(CN1H2Body));
    if(pending == NULL) {
        close(fd);
        free(statusCopy);
        free(headerCopy);
        return -1;
    }
    /* RESERVED before the descriptor is taken, not counted after. */
    if(!cn1H2ReserveFileBody()) {
        free(pending);
        free(statusCopy);
        free(headerCopy);
        return -2;
    }
    pending->streamId = streamId;
    pending->data = NULL;
    pending->fd = fd;
    pending->fileOffset = (int64_t)offset;
    pending->length = (size_t)length;
    pending->offset = 0;
    pending->next = s->bodies;
    s->bodies = pending;

    provider.source.ptr = pending;
    provider.read_callback = cn1H2ReadBody;

    rc = nghttp2_submit_response(s->session, streamId, nva, count, &provider);
    if(rc != 0) {
        /* The provider will never run, so nothing else will free this. */
        cn1H2ReleaseBody(s, pending);
    }
    free(statusCopy);
    free(headerCopy);
    return rc == 0 ? 0 : -1;
}

JAVA_BOOLEAN com_codename1_backend_Http2_wantsMoreImpl___long_R_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL) {
        return JAVA_FALSE;
    }
    return (nghttp2_session_want_read(s->session) || nghttp2_session_want_write(s->session))
            ? JAVA_TRUE : JAVA_FALSE;
}

JAVA_VOID com_codename1_backend_Http2_destroyImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    CN1H2Request* r;
    if(s == NULL) {
        return;
    }
    nghttp2_session_del(s->session);
    r = s->open;
    while(r != NULL) {
        CN1H2Request* next = r->next;
        cn1H2FreeRequest(r);
        r = next;
    }
    r = s->readyHead;
    while(r != NULL) {
        CN1H2Request* next = r->next;
        cn1H2FreeRequest(r);
        r = next;
    }
    cn1H2FreeRequest(s->current);
    free(s->out);
    while(s->bodies != NULL) {
        CN1H2Body* next = s->bodies->next;
        cn1H2FreeBody(s->bodies);
        s->bodies = next;
    }
    free(s);
}

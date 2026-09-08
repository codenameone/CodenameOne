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
#include <nghttp2/nghttp2.h>

#define CN1_H2_MAX_HEADERS 64
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
    unsigned char* data;
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

static void cn1H2ReleaseBody(CN1H2Session* s, CN1H2Body* body) {
    CN1H2Body** link = &s->bodies;
    while(*link != NULL) {
        if(*link == body) {
            *link = body->next;
            break;
        }
        link = &(*link)->next;
    }
    free(body->data);
    free(body);
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
static ssize_t cn1H2Send(nghttp2_session* session, const uint8_t* data, size_t length,
                         int flags, void* userData) {
    CN1H2Session* s = (CN1H2Session*)userData;
    (void)session;
    (void)flags;
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
    r = (CN1H2Request*)calloc(1, sizeof(CN1H2Request));
    if(r == NULL) {
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
    /* The pseudo-headers carry what a request line carries in HTTP/1.1. */
    if(nameLen == 7 && memcmp(name, ":method", 7) == 0) {
        r->method = cn1H2Dup(value, valueLen);
        return 0;
    }
    if(nameLen == 5 && memcmp(name, ":path", 5) == 0) {
        r->path = cn1H2Dup(value, valueLen);
        return 0;
    }
    if(nameLen == 7 && memcmp(name, ":scheme", 7) == 0) {
        r->scheme = cn1H2Dup(value, valueLen);
        return 0;
    }
    if(nameLen == 10 && memcmp(name, ":authority", 10) == 0) {
        r->authority = cn1H2Dup(value, valueLen);
        return 0;
    }
    if(nameLen > 0 && name[0] == ':') {
        return 0; /* an unknown pseudo-header; nghttp2 has already validated it */
    }
    if(r->headerCount < CN1_H2_MAX_HEADERS) {
        r->headers[r->headerCount].name = cn1H2Dup(name, nameLen);
        r->headers[r->headerCount].value = cn1H2Dup(value, valueLen);
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
    if(r->bodyLength + length > r->bodyCapacity) {
        size_t grown = (r->bodyLength + length) * 2 + 1024;
        if(grown > CN1_H2_MAX_BODY_BYTES) {
            grown = CN1_H2_MAX_BODY_BYTES;
        }
        unsigned char* buf = (unsigned char*)realloc(r->body, grown);
        if(buf == NULL) {
            return NGHTTP2_ERR_CALLBACK_FAILURE;
        }
        r->body = buf;
        r->bodyCapacity = grown;
    }
    memcpy(r->body + r->bodyLength, data, length);
    r->bodyLength += length;
    return 0;
}

static int cn1H2OnFrameRecv(nghttp2_session* session, const nghttp2_frame* frame,
                            void* userData) {
    CN1H2Session* s = (CN1H2Session*)userData;
    CN1H2Request* r;
    (void)session;
    if((frame->hd.flags & NGHTTP2_FLAG_END_STREAM) == 0) {
        return 0;
    }
    if(frame->hd.type != NGHTTP2_HEADERS && frame->hd.type != NGHTTP2_DATA) {
        return 0;
    }
    r = cn1H2FindOpen(s, frame->hd.stream_id);
    if(r == NULL) {
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
    return newStringFromCString(threadStateData, s->current->method);
}

JAVA_OBJECT com_codename1_backend_Http2_pathImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL || s->current == NULL || s->current->path == NULL) {
        return JAVA_NULL;
    }
    return newStringFromCString(threadStateData, s->current->path);
}

JAVA_OBJECT com_codename1_backend_Http2_authorityImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL || s->current == NULL || s->current->authority == NULL) {
        return JAVA_NULL;
    }
    return newStringFromCString(threadStateData, s->current->authority);
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
    return newStringFromCString(threadStateData, s->current->headers[index].name);
}

JAVA_OBJECT com_codename1_backend_Http2_headerValueImpl___long_int_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_INT index) {
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    if(s == NULL || s->current == NULL || index < 0 || index >= s->current->headerCount) {
        return JAVA_NULL;
    }
    return newStringFromCString(threadStateData, s->current->headers[index].value);
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
        memcpy(buf, body->data + body->offset, remaining);
        body->offset += remaining;
    }
    if(body->offset >= body->length) {
        *dataFlags |= NGHTTP2_DATA_FLAG_EOF;
        cn1H2ReleaseBody(s, body);
    }
    return (ssize_t)remaining;
}

/*
 * Submits a response. headerLines is "name: value" separated by '\n'; the status
 * is passed separately because :status is a pseudo-header nghttp2 requires first.
 */
JAVA_INT com_codename1_backend_Http2_respondImpl___long_int_java_lang_String_java_lang_String_byte_1ARRAY_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_INT streamId, JAVA_OBJECT status, JAVA_OBJECT headerLines, JAVA_OBJECT body) {
    CN1H2Body* pending;
    CN1H2Session* s = (CN1H2Session*)(intptr_t)handle;
    nghttp2_nv nva[CN1_H2_MAX_HEADERS + 1];
    char* headerCopy = NULL;
    char* statusCopy = NULL;
    size_t count = 0;
    nghttp2_data_provider provider;
    int rc;

    if(s == NULL || status == JAVA_NULL) {
        return -1;
    }
    {
        const char* tmp = stringToUTF8(threadStateData, status);
        if(tmp == NULL) {
            return -1;
        }
        statusCopy = strdup(tmp);
    }
    if(headerLines != JAVA_NULL) {
        const char* tmp = stringToUTF8(threadStateData, headerLines);
        if(tmp != NULL && tmp[0] != 0) {
            headerCopy = strdup(tmp);
        }
    }

    nva[count].name = (uint8_t*)":status";
    nva[count].namelen = 7;
    nva[count].value = (uint8_t*)statusCopy;
    nva[count].valuelen = strlen(statusCopy);
    nva[count].flags = NGHTTP2_NV_FLAG_NONE;
    count++;

    if(headerCopy != NULL) {
        char* line = headerCopy;
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

    /* A resubmission for the same stream would otherwise leave the old one to be
       freed only at stream close. */
    cn1H2ReleaseBodyForStream(s, streamId);
    pending = NULL;
    if(body != JAVA_NULL && ((JAVA_ARRAY)body)->length > 0) {
        JAVA_ARRAY arr = (JAVA_ARRAY)body;
        pending = (CN1H2Body*)malloc(sizeof(CN1H2Body));
        if(pending != NULL) {
            pending->data = (unsigned char*)malloc((size_t)arr->length);
            if(pending->data == NULL) {
                free(pending);
                pending = NULL;
            } else {
                memcpy(pending->data, (JAVA_ARRAY_BYTE*)arr->data, (size_t)arr->length);
                pending->streamId = streamId;
                pending->length = (size_t)arr->length;
                pending->offset = 0;
                pending->next = s->bodies;
                s->bodies = pending;
            }
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
        free(s->bodies->data);
        free(s->bodies);
        s->bodies = next;
    }
    free(s);
}

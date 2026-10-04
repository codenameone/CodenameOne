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
 * HTTP(S) networking for the native Codename One Linux port, backed by libcurl.
 * Codename One's HttpConnection is a request/response model: headers + an
 * optional POST body are set, then the response code/headers/body are read. The
 * libcurl easy API performs the whole transfer in one call, so the bridge
 * buffers the request body (httpWriteBody) and lazily runs the transfer the
 * first time the response is queried (httpResponseCode / httpReadBody), serving
 * the captured status, headers and body afterwards.
 */

#define _GNU_SOURCE
#include "cn1_linux.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <unistd.h> /* usleep -- explicit so strict/clang toolchains (zig, iOS) compile this */
#include <time.h>
#include <curl/curl.h>

extern JAVA_OBJECT newStringFromCString(CODENAME_ONE_THREAD_STATE, const char* str);
extern const char* stringToUTF8(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT str);
extern JAVA_OBJECT allocArray(CODENAME_ONE_THREAD_STATE, int length, struct clazz* type, int primitiveSize, int dim);
extern struct clazz class_array1__java_lang_String;

typedef struct {
    char* key;
    char* value;
} CN1Header;

typedef struct {
    CURL* easy;
    struct curl_slist* reqHeaders;
    char* url;
    int post;

    unsigned char* reqBody;
    int reqLen;
    int reqCap;
    int reqReadPos;

    unsigned char* respBody;
    int respLen;
    int respCap;
    int respReadPos;

    CN1Header* respHeaders;
    int respHeaderCount;
    int respHeaderCap;

    long status;
    char* statusMessage;
    int performed;
    int contentLength;

    /* ConnectionRequest.setHttpMethod, as asked for -- GET and POST included,
     * which override the post flag; NULL when the request named none. */
    char* method;
    /* ConnectionRequest.setTimeout and setReadTimeout, in milliseconds; 0 is
     * none. Both were accepted and ignored, so a request to a server that never
     * answered waited forever. */
    long connectTimeoutMs;
    long readTimeoutMs;
    /* Set when the transfer itself failed -- refused, timed out, reset -- as
     * opposed to answering with an error status. */
    int failed;
    /* The read-timeout bookkeeping the progress callback keeps: when data last
     * moved, how much had moved then, and whether the callback ended the
     * transfer for being idle too long. */
    long long lastActivityMs;
    curl_off_t lastDown;
    curl_off_t lastUp;
    int readTimedOut;
    /* Whether the callback has seen the connection up, so its first call after
     * connecting restarts the idle clock. */
    int sawConnect;
} CN1Http;

static void cn1HttpEnsureResp(CN1Http* c, int extra) {
    if (c->respLen + extra > c->respCap) {
        int cap = c->respCap > 0 ? c->respCap * 2 : 8192;
        while (cap < c->respLen + extra) {
            cap *= 2;
        }
        c->respBody = (unsigned char*) realloc(c->respBody, cap);
        c->respCap = cap;
    }
}

static long long cn1NowMs(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (long long) ts.tv_sec * 1000LL + ts.tv_nsec / 1000000L;
}

/* ConnectionRequest.setReadTimeout as an idle limit, in milliseconds: the time
 * between pieces of data, not a deadline for the whole transfer. libcurl's own
 * low-speed options measure AVERAGE speed in whole seconds, so a response that
 * trickled a byte every 900ms against a 500ms limit never timed out. The gap is
 * checked when data arrives as well as while none does, because libcurl calls
 * this only about once a second when the transfer is idle. Until the connection
 * is up the clock restarts: connecting is the connect timeout's business. */
static int cn1HttpProgressCb(void* userdata, curl_off_t dltotal, curl_off_t dlnow,
        curl_off_t ultotal, curl_off_t ulnow) {
    CN1Http* c = (CN1Http*) userdata;
    long long now = cn1NowMs();
    curl_off_t connected = 0;
    (void) dltotal;
    (void) ultotal;
    if (curl_easy_getinfo(c->easy, CURLINFO_CONNECT_TIME_T, &connected) != CURLE_OK || connected == 0) {
        c->lastActivityMs = now;
        return 0;
    }
    /* Progress first: data that arrived since the last call is activity now,
     * however long the call interval was. The first call after connecting
     * starts the clock too, so a slow connect is not counted as idle reading. */
    if (dlnow != c->lastDown || ulnow != c->lastUp || !c->sawConnect) {
        c->sawConnect = 1;
        c->lastDown = dlnow;
        c->lastUp = ulnow;
        c->lastActivityMs = now;
        return 0;
    }
    if (now - c->lastActivityMs > c->readTimeoutMs) {
        c->readTimedOut = 1;
        return 1;
    }
    return 0;
}

static size_t cn1HttpWriteCb(char* ptr, size_t size, size_t nmemb, void* userdata) {
    CN1Http* c = (CN1Http*) userdata;
    size_t total = size * nmemb;
    cn1HttpEnsureResp(c, (int) total);
    memcpy(c->respBody + c->respLen, ptr, total);
    c->respLen += (int) total;
    return total;
}

static size_t cn1HttpReadCb(char* buffer, size_t size, size_t nitems, void* userdata) {
    CN1Http* c = (CN1Http*) userdata;
    size_t avail = (size_t) (c->reqLen - c->reqReadPos);
    size_t want = size * nitems;
    if (want > avail) {
        want = avail;
    }
    if (want > 0) {
        memcpy(buffer, c->reqBody + c->reqReadPos, want);
        c->reqReadPos += (int) want;
    }
    return want;
}

static size_t cn1HttpHeaderCb(char* buffer, size_t size, size_t nitems, void* userdata) {
    CN1Http* c = (CN1Http*) userdata;
    size_t total = size * nitems;
    char* line = (char*) buffer;
    char* colon = memchr(line, ':', total);
    if (colon != 0) {
        int klen = (int) (colon - line);
        char* vstart = colon + 1;
        int vlen;
        while (vstart < line + total && (*vstart == ' ' || *vstart == '\t')) {
            vstart++;
        }
        vlen = (int) (line + total - vstart);
        while (vlen > 0 && (vstart[vlen - 1] == '\r' || vstart[vlen - 1] == '\n')) {
            vlen--;
        }
        if (c->respHeaderCount >= c->respHeaderCap) {
            c->respHeaderCap = c->respHeaderCap > 0 ? c->respHeaderCap * 2 : 16;
            c->respHeaders = (CN1Header*) realloc(c->respHeaders, sizeof(CN1Header) * c->respHeaderCap);
        }
        c->respHeaders[c->respHeaderCount].key = strndup(line, klen);
        c->respHeaders[c->respHeaderCount].value = strndup(vstart, vlen);
        c->respHeaderCount++;
    }
    return total;
}

static void cn1HttpPerform(CN1Http* c) {
    CURLcode rc;
    long code = 0;
    if (c->performed) {
        return;
    }
    c->performed = 1;
    curl_easy_setopt(c->easy, CURLOPT_URL, c->url);
    curl_easy_setopt(c->easy, CURLOPT_FOLLOWLOCATION, 0L);
    curl_easy_setopt(c->easy, CURLOPT_WRITEFUNCTION, cn1HttpWriteCb);
    curl_easy_setopt(c->easy, CURLOPT_WRITEDATA, c);
    curl_easy_setopt(c->easy, CURLOPT_HEADERFUNCTION, cn1HttpHeaderCb);
    curl_easy_setopt(c->easy, CURLOPT_HEADERDATA, c);
    if (c->reqHeaders) {
        curl_easy_setopt(c->easy, CURLOPT_HTTPHEADER, c->reqHeaders);
    }
    {
        /* A buffered body is sent whatever the post flag says: RequestBuilder
         * writes one for a DELETE without setting it, and CUSTOMREQUEST below then
         * sent the right verb with an empty body (the Windows port already sends
         * whatever was buffered). An explicit GET, HEAD or POST wins over both:
         * ConnectionRequest lets a request set setPost(true) for how its arguments
         * are sent and then ask for GET, or the reverse, and the method it asked
         * for is the one it means. */
        int sendBody = c->post || c->reqLen > 0;
        if (c->method != 0 && (strcmp(c->method, "GET") == 0 || strcmp(c->method, "HEAD") == 0)) {
            sendBody = 0;
        } else if (c->method != 0 && strcmp(c->method, "POST") == 0) {
            sendBody = 1;
        }
        if (sendBody) {
            curl_easy_setopt(c->easy, CURLOPT_POST, 1L);
            curl_easy_setopt(c->easy, CURLOPT_READFUNCTION, cn1HttpReadCb);
            curl_easy_setopt(c->easy, CURLOPT_READDATA, c);
            curl_easy_setopt(c->easy, CURLOPT_POSTFIELDSIZE, (long) c->reqLen);
        }
    }
    if (c->method != 0 && strcmp(c->method, "GET") != 0 && strcmp(c->method, "POST") != 0) {
        /* After the body setup: CUSTOMREQUEST replaces only the request line's
         * verb, so a PUT or PATCH still sends the body POST would have. */
        if (strcmp(c->method, "HEAD") == 0) {
            curl_easy_setopt(c->easy, CURLOPT_NOBODY, 1L);
        } else {
            curl_easy_setopt(c->easy, CURLOPT_CUSTOMREQUEST, c->method);
        }
    }
    if (c->connectTimeoutMs > 0) {
        curl_easy_setopt(c->easy, CURLOPT_CONNECTTIMEOUT_MS, c->connectTimeoutMs);
    }
    if (c->readTimeoutMs > 0) {
        /* An idle limit, not a deadline: a large download must not be cut off for
         * taking long. See cn1HttpProgressCb. */
        c->lastActivityMs = cn1NowMs();
        c->lastDown = 0;
        c->lastUp = 0;
        c->readTimedOut = 0;
        c->sawConnect = 0;
        curl_easy_setopt(c->easy, CURLOPT_XFERINFOFUNCTION, cn1HttpProgressCb);
        curl_easy_setopt(c->easy, CURLOPT_XFERINFODATA, c);
        curl_easy_setopt(c->easy, CURLOPT_NOPROGRESS, 0L);
    }
    /* curl_easy_perform runs the whole blocking HTTP transfer; yield to the GC
     * across it so a thread parked in the network stack never stalls a GC mark. */
    CN1_YIELD_THREAD;
    rc = curl_easy_perform(c->easy);
    CN1_RESUME_THREAD;
    curl_easy_getinfo(c->easy, CURLINFO_RESPONSE_CODE, &code);
    c->status = code;
    c->failed = rc != CURLE_OK;
    c->statusMessage = strdup(rc == CURLE_OK ? "OK"
            : c->readTimedOut ? "the read timed out" : curl_easy_strerror(rc));
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_httpOpen___java_lang_String_boolean_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT url, JAVA_BOOLEAN read, JAVA_BOOLEAN write) {
    CN1Http* c;
    const char* u = url == JAVA_NULL ? 0 : stringToUTF8(threadStateData, url);
    (void) read;
    (void) write;
    if (!u) {
        return 0;
    }
    c = (CN1Http*) calloc(1, sizeof(CN1Http));
    c->easy = curl_easy_init();
    if (c->easy == 0) {
        free(c);
        return 0;
    }
    c->url = strdup(u);
    c->contentLength = -1;
    return (JAVA_LONG) (intptr_t) c;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_httpSetMethod___long_boolean(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection, JAVA_BOOLEAN post) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    if (c) {
        c->post = post ? 1 : 0;
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_httpSetCustomMethod___long_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection, JAVA_OBJECT method) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    const char* m;
    if (!c || method == JAVA_NULL) {
        return;
    }
    m = stringToUTF8(threadStateData, method);
    free(c->method);
    /* Kept even for GET and POST: an explicit one overrides the post flag. */
    c->method = m == 0 ? 0 : strdup(m);
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_httpSetConnectTimeout___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection, JAVA_INT millis) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    if (c) {
        c->connectTimeoutMs = millis > 0 ? (long) millis : 0;
    }
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_httpSetReadTimeout___long_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection, JAVA_INT millis) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    if (c) {
        c->readTimeoutMs = millis > 0 ? (long) millis : 0;
    }
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_httpFailure___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    if (!c) {
        return JAVA_NULL;
    }
    cn1HttpPerform(c);
    return c->failed ? newStringFromCString(threadStateData, c->statusMessage ? c->statusMessage : "failed") : JAVA_NULL;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_httpSetHeader___long_java_lang_String_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection, JAVA_OBJECT key, JAVA_OBJECT value) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    char* k;
    char* v;
    char line[8192];
    if (!c || key == JAVA_NULL) {
        return;
    }
    /* Copy the name: converting the value reuses stringToUTF8's per-thread
     * buffer, which otherwise sent every header as "value: value". */
    k = cn1LinuxJStrDup(threadStateData, key);
    v = value == JAVA_NULL ? 0 : cn1LinuxJStrDup(threadStateData, value);
    if (k != 0) {
        snprintf(line, sizeof(line), "%s: %s", k, v == 0 ? "" : v);
        c->reqHeaders = curl_slist_append(c->reqHeaders, line);
    }
    free(k);
    free(v);
}

JAVA_INT com_codename1_impl_linux_LinuxNative_httpResponseCode___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    if (!c) {
        return 0;
    }
    cn1HttpPerform(c);
    return (JAVA_INT) c->status;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_httpResponseMessage___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    if (!c) {
        return JAVA_NULL;
    }
    cn1HttpPerform(c);
    return newStringFromCString(threadStateData, c->statusMessage ? c->statusMessage : "");
}

JAVA_INT com_codename1_impl_linux_LinuxNative_httpContentLength___long_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    int i;
    if (!c) {
        return -1;
    }
    cn1HttpPerform(c);
    for (i = 0; i < c->respHeaderCount; i++) {
        if (strcasecmp(c->respHeaders[i].key, "Content-Length") == 0) {
            return atoi(c->respHeaders[i].value);
        }
    }
    return c->respLen;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_httpHeaderField___long_java_lang_String_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection, JAVA_OBJECT name) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    const char* n;
    int i;
    if (!c || name == JAVA_NULL) {
        return JAVA_NULL;
    }
    cn1HttpPerform(c);
    n = stringToUTF8(threadStateData, name);
    for (i = c->respHeaderCount - 1; i >= 0; i--) {
        if (strcasecmp(c->respHeaders[i].key, n) == 0) {
            return newStringFromCString(threadStateData, c->respHeaders[i].value);
        }
    }
    return JAVA_NULL;
}

JAVA_OBJECT com_codename1_impl_linux_LinuxNative_httpHeaderFieldNames___long_R_java_lang_String_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    JAVA_OBJECT arr;
    JAVA_OBJECT* elements;
    int i;
    if (!c) {
        return allocArray(threadStateData, 0, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    }
    cn1HttpPerform(c);
    arr = allocArray(threadStateData, c->respHeaderCount, &class_array1__java_lang_String, sizeof(JAVA_OBJECT), 1);
    if (arr != JAVA_NULL) {
        elements = (JAVA_OBJECT*) CN1_ARRAY_DATA(arr);
        for (i = 0; i < c->respHeaderCount; i++) {
            JAVA_OBJECT cn1__s = newStringFromCString(threadStateData, c->respHeaders[i].key);
            CN1_WRITE_BARRIER(arr, cn1__s);  /* each allocation is a safepoint: arr may be old */
            elements[i] = cn1__s;
        }
    }
    return arr;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_httpReadBody___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    char* data;
    int avail;
    int n;
    if (!c || buffer == JAVA_NULL || length <= 0) {
        return -1;
    }
    cn1HttpPerform(c);
    avail = c->respLen - c->respReadPos;
    if (avail <= 0) {
        return -1;
    }
    n = length < avail ? length : avail;
    data = (char*) CN1_ARRAY_DATA(buffer);
    memcpy(data + offset, c->respBody + c->respReadPos, (size_t) n);
    c->respReadPos += n;
    return n;
}

JAVA_INT com_codename1_impl_linux_LinuxNative_httpWriteBody___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    char* data;
    if (!c || buffer == JAVA_NULL || length <= 0) {
        return -1;
    }
    if (c->reqLen + length > c->reqCap) {
        int cap = c->reqCap > 0 ? c->reqCap * 2 : 8192;
        while (cap < c->reqLen + length) {
            cap *= 2;
        }
        c->reqBody = (unsigned char*) realloc(c->reqBody, cap);
        c->reqCap = cap;
    }
    data = (char*) CN1_ARRAY_DATA(buffer);
    memcpy(c->reqBody + c->reqLen, data + offset, (size_t) length);
    c->reqLen += length;
    return length;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_httpClose___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    int i;
    if (!c) {
        return;
    }
    if (c->easy) {
        curl_easy_cleanup(c->easy);
    }
    if (c->reqHeaders) {
        curl_slist_free_all(c->reqHeaders);
    }
    for (i = 0; i < c->respHeaderCount; i++) {
        free(c->respHeaders[i].key);
        free(c->respHeaders[i].value);
    }
    free(c->respHeaders);
    free(c->reqBody);
    free(c->respBody);
    free(c->url);
    free(c->statusMessage);
    free(c->method);
    free(c);
}

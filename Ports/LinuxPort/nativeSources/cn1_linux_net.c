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
    if (c->post) {
        curl_easy_setopt(c->easy, CURLOPT_POST, 1L);
        curl_easy_setopt(c->easy, CURLOPT_READFUNCTION, cn1HttpReadCb);
        curl_easy_setopt(c->easy, CURLOPT_READDATA, c);
        curl_easy_setopt(c->easy, CURLOPT_POSTFIELDSIZE, (long) c->reqLen);
    }
    /* curl_easy_perform runs the whole blocking HTTP transfer; yield to the GC
     * across it so a thread parked in the network stack never stalls a GC mark. */
    CN1_YIELD_THREAD;
    rc = curl_easy_perform(c->easy);
    CN1_RESUME_THREAD;
    curl_easy_getinfo(c->easy, CURLINFO_RESPONSE_CODE, &code);
    c->status = code;
    c->statusMessage = strdup(rc == CURLE_OK ? "OK" : curl_easy_strerror(rc));
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

JAVA_VOID com_codename1_impl_linux_LinuxNative_httpSetHeader___long_java_lang_String_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG connection, JAVA_OBJECT key, JAVA_OBJECT value) {
    CN1Http* c = (CN1Http*) (intptr_t) connection;
    const char* k;
    const char* v;
    char line[8192];
    if (!c || key == JAVA_NULL) {
        return;
    }
    k = stringToUTF8(threadStateData, key);
    v = value == JAVA_NULL ? "" : stringToUTF8(threadStateData, value);
    snprintf(line, sizeof(line), "%s: %s", k, v);
    c->reqHeaders = curl_slist_append(c->reqHeaders, line);
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
        elements = (JAVA_OBJECT*) (*(JAVA_ARRAY) arr).data;
        for (i = 0; i < c->respHeaderCount; i++) {
            elements[i] = newStringFromCString(threadStateData, c->respHeaders[i].key);
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
    data = (char*) (*(JAVA_ARRAY) buffer).data;
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
    data = (char*) (*(JAVA_ARRAY) buffer).data;
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
    free(c);
}

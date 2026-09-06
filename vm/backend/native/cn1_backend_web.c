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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef _WIN32
#include <unistd.h> /* CN1_RESUME_THREAD expands to usleep */
#endif
#include <curl/curl.h>

typedef struct {
    char* data;
    size_t length;
    /* The response headers, verbatim, one per line. Kept as one buffer rather
     * than parsed here: the Java side already has to split them, and a header
     * parser in C is a second place for the same rules to drift. */
    char* headers;
    size_t headerLength;
    long status;
    char error[CURL_ERROR_SIZE];
} CN1WebResponse;

static size_t cn1WebHeader(void* contents, size_t size, size_t count, void* userp) {
    CN1WebResponse* r = (CN1WebResponse*)userp;
    size_t total = size * count;
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

static size_t cn1WebWrite(void* contents, size_t size, size_t count, void* userp) {
    CN1WebResponse* r = (CN1WebResponse*)userp;
    size_t total = size * count;
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

/*
 * headerLines is one string with '\n' between headers, because passing a
 * String[] would mean walking a Java array from C for no benefit.
 */
JAVA_LONG com_codename1_backend_Web_performImpl___java_lang_String_java_lang_String_java_lang_String_byte_1ARRAY_R_long(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT method, JAVA_OBJECT url, JAVA_OBJECT headerLines, JAVA_OBJECT body) {
    CURL* curl;
    CURLcode rc;
    struct curl_slist* headers = NULL;
    CN1WebResponse* r;
    char* methodCopy = NULL;
    char* urlCopy = NULL;
    char* bodyCopy = NULL;
    JAVA_INT bodyLength = 0;

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
    }
    if(method != JAVA_NULL) {
        const char* tmp = stringToUTF8(threadStateData, method);
        methodCopy = tmp == NULL ? NULL : strdup(tmp);
    }
    if(headerLines != JAVA_NULL) {
        const char* tmp = stringToUTF8(threadStateData, headerLines);
        if(tmp != NULL && tmp[0] != 0) {
            char* copy = strdup(tmp);
            char* line = copy;
            while(line != NULL && *line != 0) {
                char* nl = strchr(line, '\n');
                if(nl != NULL) {
                    *nl = 0;
                }
                if(*line != 0) {
                    headers = curl_slist_append(headers, line);
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
        if(bodyCopy != NULL && bodyLength > 0) {
            memcpy(bodyCopy, (JAVA_ARRAY_BYTE*)arr->data, (size_t)bodyLength);
        }
    }

    r = (CN1WebResponse*)calloc(1, sizeof(CN1WebResponse));
    if(r == NULL) {
        free(urlCopy); free(methodCopy); free(bodyCopy);
        curl_slist_free_all(headers);
        return 0;
    }

    curl = curl_easy_init();
    if(curl == NULL) {
        free(r); free(urlCopy); free(methodCopy); free(bodyCopy);
        curl_slist_free_all(headers);
        return 0;
    }
    curl_easy_setopt(curl, CURLOPT_URL, urlCopy);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, cn1WebWrite);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, r);
    curl_easy_setopt(curl, CURLOPT_HEADERFUNCTION, cn1WebHeader);
    curl_easy_setopt(curl, CURLOPT_HEADERDATA, r);
    curl_easy_setopt(curl, CURLOPT_ERRORBUFFER, r->error);
    curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
    curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 5L);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 30L);
    curl_easy_setopt(curl, CURLOPT_NOSIGNAL, 1L);
    curl_easy_setopt(curl, CURLOPT_USERAGENT, "codenameone-backend");
    /* CN1_WEB_VERBOSE=1 makes libcurl narrate the exchange on stderr. Off by
     * default and read per request rather than cached, so it can be turned on for
     * a running process through its environment without a rebuild. Request headers
     * carry credentials, so this is a debugging switch, not a logging one. */
    if(getenv("CN1_WEB_VERBOSE") != NULL) {
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
    if(bodyCopy != NULL) {
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, bodyCopy);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, (long)bodyLength);
    }

    /* The transfer blocks; yield so the concurrent collector is not stalled by it. */
    CN1_YIELD_THREAD;
    rc = curl_easy_perform(curl);
    CN1_RESUME_THREAD;

    if(rc == CURLE_OK) {
        curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &r->status);
    } else {
        r->status = -1;
        if(r->error[0] == 0) {
            const char* msg = curl_easy_strerror(rc);
            strncpy(r->error, msg == NULL ? "transfer failed" : msg, CURL_ERROR_SIZE - 1);
        }
    }
    curl_easy_cleanup(curl);
    curl_slist_free_all(headers);
    free(urlCopy); free(methodCopy); free(bodyCopy);
    return (JAVA_LONG)(intptr_t)r;
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
    return newStringFromCString(threadStateData, r->error);
}

JAVA_OBJECT com_codename1_backend_Web_bodyImpl___long_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1WebResponse* r = (CN1WebResponse*)(intptr_t)handle;
    JAVA_OBJECT arr;
    if(r == NULL) {
        return JAVA_NULL;
    }
    arr = allocArray(threadStateData, (int)r->length, &class_array1__JAVA_BYTE, sizeof(JAVA_ARRAY_BYTE), 1);
    if(r->length > 0 && r->data != NULL) {
        memcpy((JAVA_ARRAY_BYTE*)((JAVA_ARRAY)arr)->data, r->data, r->length);
    }
    return arr;
}

JAVA_OBJECT com_codename1_backend_Web_headersImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1WebResponse* r = (CN1WebResponse*)(intptr_t)handle;
    if(r == NULL || r->headers == NULL) {
        return JAVA_NULL;
    }
    return newStringFromCString(threadStateData, r->headers);
}

JAVA_VOID com_codename1_backend_Web_freeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    CN1WebResponse* r = (CN1WebResponse*)(intptr_t)handle;
    if(r != NULL) {
        free(r->data);
        free(r->headers);
        free(r);
    }
}

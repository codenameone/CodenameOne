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

/* What a Java byte[] can hold. A response is handed back as one, so a transfer
   that outgrows this cannot be delivered however much memory the host has --
   and the length is carried in a size_t here and narrowed to an int there, so
   letting it past this point produces a NEGATIVE array length and then a memcpy
   of the full size_t into whatever that allocated. Refused while it is still a
   failed download, which Web turns into an IOException, rather than after it has
   become memory corruption. */
#define CN1_WEB_MAX_BODY_BYTES ((size_t)0x7fffffff)

static size_t cn1WebWrite(void* contents, size_t size, size_t count, void* userp) {
    CN1WebResponse* r = (CN1WebResponse*)userp;
    size_t total = size * count;
    if(total > CN1_WEB_MAX_BODY_BYTES - r->length) {
        return 0; /* aborts the transfer; libcurl reports CURLE_WRITE_ERROR */
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
       caller can act on deliberately. */
    if(headers == NULL) {
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
    curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 30L);
    curl_easy_setopt(curl, CURLOPT_LOW_SPEED_LIMIT, 1L);
    curl_easy_setopt(curl, CURLOPT_LOW_SPEED_TIME, 30L);
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
    if(r->length > CN1_WEB_MAX_BODY_BYTES) {
        /* Unreachable while cn1WebWrite holds the line above, and checked anyway:
           the cast below is what turns a length this size into a negative one,
           and the memcpy after it does not consult the array's length. */
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

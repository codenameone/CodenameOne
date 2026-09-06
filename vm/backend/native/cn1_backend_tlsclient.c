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
 * Outbound (client-side) TLS on OpenSSL, as an UPGRADE of a connected socket.
 *
 * It is an upgrade rather than a secure connect because that is the shape the
 * database protocols need: PostgreSQL sends an SSLRequest packet and MySQL an
 * SSLRequest capability flag, both in plaintext, and only then does the handshake
 * begin on the same descriptor. A connect-time flag could not express that.
 *
 * Two things here are the whole security value, and both are easy to leave out:
 *
 * - SSL_CTX_set_default_verify_paths plus SSL_VERIFY_PEER, so an untrusted chain
 *   fails the handshake. Without the mode, OpenSSL completes the handshake and
 *   reports the failure only if you go looking, which nobody does.
 * - SSL_set1_host, so the certificate has to be FOR the host we asked for. A
 *   verified chain for someone else's name is not authentication, and OpenSSL
 *   does not check the name unless it is told to.
 *
 * SNI is sent separately (SSL_set_tlsext_host_name): it tells the server which
 * certificate to present and proves nothing on its own.
 *
 * Built as a stub when the backend is compiled without TLS (CN1_BACKEND_NO_TLS),
 * rather than left out of the build: Tcp always declares these natives, and a
 * native whose symbol is absent is dropped from the Java side by the dead-code
 * pass, which would make Tcp.startTls silently do nothing.
 */
#include "cn1_globals.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#ifndef CN1_BACKEND_NO_TLS

#ifndef _WIN32
#include <unistd.h> /* CN1_RESUME_THREAD expands to usleep */
#endif
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <openssl/x509v3.h>

static int cn1ClientTlsInitialised = 0;
/* One context per trust root, because a context holds the trust store. The
 * system store is the common case and gets slot 0; a caller that supplies its own
 * CA bundle -- which is how a managed database or a development container is
 * reached -- gets a slot keyed by the file's path. The table is small and never
 * shrinks: the number of distinct trust roots a process uses is the number of
 * databases and services it talks to. */
#define CN1_TLS_CONTEXT_SLOTS 8
static SSL_CTX* cn1ClientTlsContexts[CN1_TLS_CONTEXT_SLOTS];
static char cn1ClientTlsRoots[CN1_TLS_CONTEXT_SLOTS][1024];
static int cn1ClientTlsContextCount = 0;
/* The last handshake failure, for the message Java throws. Per process rather
 * than per thread: a failed connect is reported immediately by the thread that
 * saw it, and a race here would at worst attach the wrong reason to a failure
 * that happened anyway. */
static char cn1ClientTlsError[512];

static void cn1ClientTlsRecordError(const char* stage) {
    unsigned long code = ERR_get_error();
    char buffer[256];
    buffer[0] = 0;
    if(code != 0) {
        ERR_error_string_n(code, buffer, sizeof(buffer));
    }
    snprintf(cn1ClientTlsError, sizeof(cn1ClientTlsError), "%s%s%s", stage,
             buffer[0] ? ": " : "", buffer);
}

static SSL_CTX* cn1ClientTlsEnsureContext(const char* caFile) {
    const char* key = caFile == 0 ? "" : caFile;
    SSL_CTX* ctx;
    int iter;
    for(iter = 0 ; iter < cn1ClientTlsContextCount ; iter++) {
        if(strcmp(cn1ClientTlsRoots[iter], key) == 0) {
            return cn1ClientTlsContexts[iter];
        }
    }
    if(cn1ClientTlsContextCount >= CN1_TLS_CONTEXT_SLOTS) {
        snprintf(cn1ClientTlsError, sizeof(cn1ClientTlsError),
                 "too many distinct TLS trust roots (limit %d)", CN1_TLS_CONTEXT_SLOTS);
        return 0;
    }
    if(strlen(key) >= sizeof(cn1ClientTlsRoots[0])) {
        snprintf(cn1ClientTlsError, sizeof(cn1ClientTlsError), "the CA path is too long");
        return 0;
    }
    if(!cn1ClientTlsInitialised) {
        SSL_library_init();
        SSL_load_error_strings();
        cn1ClientTlsInitialised = 1;
    }
    ctx = SSL_CTX_new(TLS_client_method());
    if(ctx == 0) {
        cn1ClientTlsRecordError("could not create a TLS context");
        return 0;
    }
    /* TLS 1.2 is the floor; everything below it is broken in ways not worth
     * carrying, and OpenSSL's defaults above it beat a hand-written cipher list
     * that goes stale. */
    SSL_CTX_set_min_proto_version(ctx, TLS1_2_VERSION);
    if(key[0] == 0) {
        if(SSL_CTX_set_default_verify_paths(ctx) != 1) {
            /* No system trust store. Refuse rather than fall back to trusting
             * everything: an unverified connection that looks encrypted is worse
             * than a plaintext one that looks plaintext. */
            cn1ClientTlsRecordError("no system CA store is available");
            SSL_CTX_free(ctx);
            return 0;
        }
    } else if(SSL_CTX_load_verify_locations(ctx, key, 0) != 1) {
        /* The caller named a CA bundle and it did not load. Falling back to the
         * system store would verify against roots the caller deliberately did not
         * choose, which is not what was asked for. */
        cn1ClientTlsRecordError("could not load the CA bundle");
        SSL_CTX_free(ctx);
        return 0;
    }
    SSL_CTX_set_verify(ctx, SSL_VERIFY_PEER, 0);
    strcpy(cn1ClientTlsRoots[cn1ClientTlsContextCount], key);
    cn1ClientTlsContexts[cn1ClientTlsContextCount] = ctx;
    cn1ClientTlsContextCount++;
    return ctx;
}

JAVA_LONG com_codename1_backend_Tcp_startTlsImpl___long_java_lang_String_java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT host, JAVA_OBJECT caFile) {
    SSL_CTX* ctx;
    SSL* ssl;
    char* h;
    const char* ca;
    int fd = handle <= 0 ? -1 : (int)(handle - 1);
    int rc;
    if(fd < 0) {
        snprintf(cn1ClientTlsError, sizeof(cn1ClientTlsError), "the socket is closed");
        return 0;
    }
    /* stringToUTF8 hands back THIS THREAD'S single scratch buffer, and the next
     * conversion frees and reallocates it. The host has to be copied before the
     * CA path is converted; using it afterwards is a use-after-free that presents
     * as a wild pointer somewhere else entirely. */
    {
        const char* tmp = host == JAVA_NULL ? 0 : stringToUTF8(threadStateData, host);
        if(tmp == 0) {
            snprintf(cn1ClientTlsError, sizeof(cn1ClientTlsError),
                     "no host name to verify against");
            return 0;
        }
        h = strdup(tmp);
        if(h == 0) {
            snprintf(cn1ClientTlsError, sizeof(cn1ClientTlsError), "out of memory");
            return 0;
        }
    }
    ca = caFile == JAVA_NULL ? 0 : stringToUTF8(threadStateData, caFile);
    ctx = cn1ClientTlsEnsureContext(ca);
    if(ctx == 0) {
        free(h);
        return 0;
    }
    ssl = SSL_new(ctx);
    if(ssl == 0) {
        cn1ClientTlsRecordError("could not create a TLS session");
        free(h);
        return 0;
    }
    SSL_set_fd(ssl, fd);
    SSL_set_tlsext_host_name(ssl, h);
    /* The name check. Without it a valid certificate for any other host would
     * pass, which is most of what TLS is for here. */
    if(SSL_set1_host(ssl, h) != 1) {
        cn1ClientTlsRecordError("could not set the expected host name");
        SSL_free(ssl);
        free(h);
        return 0;
    }
    CN1_YIELD_THREAD;
    rc = SSL_connect(ssl);
    CN1_RESUME_THREAD;
    free(h);
    if(rc != 1) {
        long verify = SSL_get_verify_result(ssl);
        if(verify != X509_V_OK) {
            snprintf(cn1ClientTlsError, sizeof(cn1ClientTlsError),
                     "certificate rejected: %s", X509_verify_cert_error_string(verify));
        } else {
            cn1ClientTlsRecordError("handshake failed");
        }
        SSL_free(ssl);
        return 0;
    }
    return (JAVA_LONG)(intptr_t)ssl;
}

JAVA_OBJECT com_codename1_backend_Tcp_tlsErrorImpl___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    return newStringFromCString(threadStateData,
            cn1ClientTlsError[0] ? cn1ClientTlsError : "unknown TLS failure");
}

JAVA_INT com_codename1_backend_Tcp_tlsReadImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG session, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    SSL* ssl = (SSL*)(intptr_t)session;
    JAVA_ARRAY_BYTE* data;
    int n;
    if(ssl == 0 || buffer == JAVA_NULL) {
        return -2;
    }
    data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    CN1_YIELD_THREAD;
    n = SSL_read(ssl, (char*)&data[offset], (int)length);
    CN1_RESUME_THREAD;
    if(n > 0) {
        return (JAVA_INT)n;
    }
    /* A clean close_notify is end of stream, not an error; anything else is. */
    if(SSL_get_error(ssl, n) == SSL_ERROR_ZERO_RETURN) {
        return -1;
    }
    return -2;
}

JAVA_INT com_codename1_backend_Tcp_tlsWriteImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG session, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    SSL* ssl = (SSL*)(intptr_t)session;
    JAVA_ARRAY_BYTE* data;
    int written = 0;
    if(ssl == 0 || buffer == JAVA_NULL) {
        return -2;
    }
    data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    /* SSL_write can return a short count, and the caller checks for the full
     * length, so the loop is here rather than in Java. */
    while(written < (int)length) {
        int n;
        CN1_YIELD_THREAD;
        n = SSL_write(ssl, (char*)&data[offset + written], (int)length - written);
        CN1_RESUME_THREAD;
        if(n <= 0) {
            return -2;
        }
        written += n;
    }
    return (JAVA_INT)written;
}

void com_codename1_backend_Tcp_tlsCloseImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG session) {
    SSL* ssl = (SSL*)(intptr_t)session;
    if(ssl == 0) {
        return;
    }
    /* One shutdown attempt: the descriptor is closed right after this, so waiting
     * for the peer's close_notify would only delay it. */
    SSL_shutdown(ssl);
    SSL_free(ssl);
}

#else /* CN1_BACKEND_NO_TLS */

JAVA_LONG com_codename1_backend_Tcp_startTlsImpl___long_java_lang_String_java_lang_String_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT host, JAVA_OBJECT caFile) {
    return 0;
}

JAVA_OBJECT com_codename1_backend_Tcp_tlsErrorImpl___R_java_lang_String(CODENAME_ONE_THREAD_STATE) {
    return newStringFromCString(threadStateData,
            "this binary was built without TLS (CN1_BACKEND_HTTPS=0)");
}

JAVA_INT com_codename1_backend_Tcp_tlsReadImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG session, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    return -2;
}

JAVA_INT com_codename1_backend_Tcp_tlsWriteImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG session, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    return -2;
}

void com_codename1_backend_Tcp_tlsCloseImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG session) {
}

#endif

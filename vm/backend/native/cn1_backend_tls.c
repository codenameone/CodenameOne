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
 * Server-side TLS on OpenSSL.
 *
 * One SSL_CTX for the process (it holds the certificate and the session cache) and
 * one SSL per connection. The handshake runs on the worker that picks the
 * connection up, where the descriptor is already blocking -- doing it on the
 * reactor thread would block every other connection behind one slow client.
 *
 * TLS 1.2 is the floor. Everything below it is broken in ways that are not worth
 * carrying, and OpenSSL's defaults above that are better than a hand-written
 * cipher list that goes stale.
 */
#include "cn1_globals.h"
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#ifndef _WIN32
#include <unistd.h> /* CN1_RESUME_THREAD expands to usleep */
#endif
#include <openssl/ssl.h>
#include <openssl/err.h>

static int cn1TlsInitialised = 0;

/*
 * ALPN. HTTP/2 over TLS is only ever reached this way -- there is no upgrade
 * handshake for h2 over TLS, so a server that does not advertise "h2" here will
 * never speak it however complete the rest of its implementation is.
 *
 * The wire format is a list of length-prefixed names. h2 is offered first so a
 * client that supports both gets it; http/1.1 stays in the list because most
 * clients still ask for it and a server that only offers h2 refuses them.
 */
static const unsigned char CN1_ALPN_BOTH[] = { 2, 'h', '2', 8, 'h', 't', 't', 'p', '/', '1', '.', '1' };
static const unsigned char CN1_ALPN_HTTP11[] = { 8, 'h', 't', 't', 'p', '/', '1', '.', '1' };
/* The h2 policy travels in the callback's own arg rather than in a variable
   beside it: a process that serves two TLS ports had the second createContext
   overwrite the first one's setting, and every context shares this callback, so
   a server built for http/1.1 could start negotiating h2 (or stop offering it)
   because of an unrelated server elsewhere in the same process. */

static int cn1AlpnSelect(SSL* ssl, const unsigned char** out, unsigned char* outlen,
                         const unsigned char* in, unsigned int inlen, void* arg) {
    int offerH2 = (int)(intptr_t)arg;
    const unsigned char* offered = offerH2 ? CN1_ALPN_BOTH : CN1_ALPN_HTTP11;
    unsigned int offeredLen = offerH2 ? (unsigned int)sizeof(CN1_ALPN_BOTH)
                                      : (unsigned int)sizeof(CN1_ALPN_HTTP11);
    (void)ssl;
    if(SSL_select_next_proto((unsigned char**)out, outlen, offered, offeredLen, in, inlen)
            != OPENSSL_NPN_NEGOTIATED) {
        /* No overlap. NOACK rather than ALERT_FATAL: a client that offered only
           protocols we do not speak still gets a working http/1.1 connection,
           which is what it would have had with no ALPN at all. */
        return SSL_TLSEXT_ERR_NOACK;
    }
    return SSL_TLSEXT_ERR_OK;
}

JAVA_LONG com_codename1_backend_Tls_createContextImpl___java_lang_String_java_lang_String_boolean_R_long(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT certPath, JAVA_OBJECT keyPath, JAVA_BOOLEAN offerHttp2) {
    SSL_CTX* ctx;
    char* cert;
    const char* key;
    if(certPath == JAVA_NULL || keyPath == JAVA_NULL) {
        return 0;
    }
    if(!cn1TlsInitialised) {
        SSL_library_init();
        SSL_load_error_strings();
        cn1TlsInitialised = 1;
    }
    /* stringToUTF8 hands back this thread's scratch buffer, so the first path is
       copied before the second conversion overwrites it. */
    {
        const char* tmp = stringToUTF8(threadStateData, certPath);
        if(tmp == NULL) {
            return 0;
        }
        cert = strdup(tmp);
    }
    key = stringToUTF8(threadStateData, keyPath);
    if(key == NULL) {
        free(cert);
        return 0;
    }
    ctx = SSL_CTX_new(TLS_server_method());
    if(ctx == NULL) {
        free(cert);
        return 0;
    }
    SSL_CTX_set_min_proto_version(ctx, TLS1_2_VERSION);
    SSL_CTX_set_alpn_select_cb(ctx, cn1AlpnSelect,
            (void*)(intptr_t)(offerHttp2 ? 1 : 0));
    /* The handshake and the record layer both want to retry on a partial write
       with a moved buffer; without this OpenSSL refuses and the connection dies
       on a large response. */
    SSL_CTX_set_mode(ctx, SSL_MODE_ACCEPT_MOVING_WRITE_BUFFER | SSL_MODE_AUTO_RETRY);
    if(SSL_CTX_use_certificate_chain_file(ctx, cert) != 1 ||
       SSL_CTX_use_PrivateKey_file(ctx, key, SSL_FILETYPE_PEM) != 1 ||
       SSL_CTX_check_private_key(ctx) != 1) {
        SSL_CTX_free(ctx);
        free(cert);
        return 0;
    }
    free(cert);
    return (JAVA_LONG)(intptr_t)ctx;
}

JAVA_VOID com_codename1_backend_Tls_freeContextImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    SSL_CTX* ctx = (SSL_CTX*)(intptr_t)handle;
    if(ctx != NULL) {
        SSL_CTX_free(ctx);
    }
}

/* Runs the handshake. Returns the session handle, or 0. */
JAVA_LONG com_codename1_backend_Tls_acceptImpl___long_int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_LONG ctxHandle, JAVA_INT fd) {
    SSL_CTX* ctx = (SSL_CTX*)(intptr_t)ctxHandle;
    SSL* ssl;
    int rc;
    if(ctx == NULL || fd < 0) {
        return 0;
    }
    ssl = SSL_new(ctx);
    if(ssl == NULL) {
        return 0;
    }
    if(SSL_set_fd(ssl, fd) != 1) {
        SSL_free(ssl);
        return 0;
    }
    CN1_YIELD_THREAD;
    rc = SSL_accept(ssl);
    CN1_RESUME_THREAD;
    if(rc != 1) {
        /* A failed handshake is ordinary traffic -- a scanner, a client with no
           common cipher, a plaintext request to an https port. Drain the error
           queue so it cannot be misattributed to the next connection on this
           thread. */
        ERR_clear_error();
        SSL_free(ssl);
        return 0;
    }
    return (JAVA_LONG)(intptr_t)ssl;
}

/* -1 at end of stream, -2 on error, otherwise the byte count. */
JAVA_INT com_codename1_backend_Tls_readImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    SSL* ssl = (SSL*)(intptr_t)handle;
    JAVA_ARRAY_BYTE* data;
    int n;
    if(ssl == NULL || buffer == JAVA_NULL) {
        return -2;
    }
    data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    CN1_YIELD_THREAD;
    n = SSL_read(ssl, &data[offset], length);
    CN1_RESUME_THREAD;
    if(n > 0) {
        return (JAVA_INT)n;
    }
    {
        int err = SSL_get_error(ssl, n);
        ERR_clear_error();
        if(err == SSL_ERROR_ZERO_RETURN) {
            return -1; /* the peer closed the session cleanly */
        }
        return -2;
    }
}

JAVA_INT com_codename1_backend_Tls_writeImpl___long_byte_1ARRAY_int_int_R_int(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle, JAVA_OBJECT buffer, JAVA_INT offset, JAVA_INT length) {
    SSL* ssl = (SSL*)(intptr_t)handle;
    JAVA_ARRAY_BYTE* data;
    JAVA_INT written = 0;
    if(ssl == NULL || buffer == JAVA_NULL) {
        return -1;
    }
    data = (JAVA_ARRAY_BYTE*)((JAVA_ARRAY)buffer)->data;
    CN1_YIELD_THREAD;
    while(written < length) {
        int n = SSL_write(ssl, &data[offset + written], length - written);
        if(n <= 0) {
            ERR_clear_error();
            CN1_RESUME_THREAD;
            return -1;
        }
        written += (JAVA_INT)n;
    }
    CN1_RESUME_THREAD;
    return written;
}

/* The protocol ALPN settled on: "h2", "http/1.1", or null when there was none. */
JAVA_OBJECT com_codename1_backend_Tls_negotiatedProtocolImpl___long_R_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    SSL* ssl = (SSL*)(intptr_t)handle;
    const unsigned char* proto = NULL;
    unsigned int len = 0;
    char name[32];
    if(ssl == NULL) {
        return JAVA_NULL;
    }
    SSL_get0_alpn_selected(ssl, &proto, &len);
    if(proto == NULL || len == 0 || len >= sizeof(name)) {
        return JAVA_NULL;
    }
    memcpy(name, proto, len);
    name[len] = 0;
    return newStringFromUtf8Len(threadStateData, name, (int)strlen(name));
}

JAVA_VOID com_codename1_backend_Tls_closeImpl___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG handle) {
    SSL* ssl = (SSL*)(intptr_t)handle;
    if(ssl == NULL) {
        return;
    }
    /* One shutdown, not the two-step wait for the peer's close_notify: a client
       that has already gone would otherwise hold the worker until the deadline. */
    SSL_shutdown(ssl);
    ERR_clear_error();
    SSL_free(ssl);
}

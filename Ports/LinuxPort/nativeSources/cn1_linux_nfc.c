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
 * Linux port NFC reader bridge over PC/SC. libpcsclite is opened with dlopen on
 * first use, so the port takes no link dependency on it; a system without the
 * library (or without a running pcscd) reports NFC as unsupported.
 *
 * The PC/SC types are declared locally with pcsclite's Linux ABI (LONG and
 * DWORD are C longs there) so the build needs no PC/SC headers either.
 *
 * One context serves the whole process. Blocking calls (waiting for a card,
 * transmitting an APDU) are bracketed with CN1_YIELD_THREAD/CN1_RESUME_THREAD so
 * a thread parked on a reader does not stall the garbage collector. A pending
 * wait is cancelled with SCardCancel plus an atomic flag the wait loop checks.
 */

#include "cn1_linux.h"
#include <dlfcn.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

typedef long cn1_pcsc_long;
typedef unsigned long cn1_pcsc_dword;

typedef struct {
    const char* szReader;
    void* pvUserData;
    cn1_pcsc_dword dwCurrentState;
    cn1_pcsc_dword dwEventState;
    cn1_pcsc_dword cbAtr;
    unsigned char rgbAtr[33];
} cn1_pcsc_reader_state;

typedef struct {
    cn1_pcsc_dword dwProtocol;
    cn1_pcsc_dword cbPciLength;
} cn1_pcsc_io_request;

#define CN1_SCARD_S_SUCCESS 0L
#define CN1_SCARD_SCOPE_SYSTEM 2UL
#define CN1_SCARD_SHARE_SHARED 2UL
#define CN1_SCARD_PROTOCOL_T0 1UL
#define CN1_SCARD_PROTOCOL_T1 2UL
#define CN1_SCARD_LEAVE_CARD 0UL
#define CN1_SCARD_STATE_UNAWARE 0UL
#define CN1_SCARD_STATE_PRESENT 0x20UL
#define CN1_SCARD_STATE_MUTE 0x200UL
#define CN1_NFC_MAX_READERS 8
#define CN1_NFC_SLICE_MS 250UL

typedef cn1_pcsc_long (*cn1_pcsc_establish_fn)(cn1_pcsc_dword, const void*, const void*, cn1_pcsc_long*);
typedef cn1_pcsc_long (*cn1_pcsc_list_fn)(cn1_pcsc_long, const char*, char*, cn1_pcsc_dword*);
typedef cn1_pcsc_long (*cn1_pcsc_status_fn)(cn1_pcsc_long, cn1_pcsc_dword, cn1_pcsc_reader_state*, cn1_pcsc_dword);
typedef cn1_pcsc_long (*cn1_pcsc_connect_fn)(cn1_pcsc_long, const char*, cn1_pcsc_dword, cn1_pcsc_dword, cn1_pcsc_long*, cn1_pcsc_dword*);
typedef cn1_pcsc_long (*cn1_pcsc_disconnect_fn)(cn1_pcsc_long, cn1_pcsc_dword);
typedef cn1_pcsc_long (*cn1_pcsc_transmit_fn)(cn1_pcsc_long, const cn1_pcsc_io_request*, const unsigned char*, cn1_pcsc_dword, cn1_pcsc_io_request*, unsigned char*, cn1_pcsc_dword*);
typedef cn1_pcsc_long (*cn1_pcsc_cancel_fn)(cn1_pcsc_long);

static cn1_pcsc_establish_fn cn1PcscEstablish;
static cn1_pcsc_list_fn cn1PcscList;
static cn1_pcsc_status_fn cn1PcscStatus;
static cn1_pcsc_connect_fn cn1PcscConnect;
static cn1_pcsc_disconnect_fn cn1PcscDisconnect;
static cn1_pcsc_transmit_fn cn1PcscTransmit;
static cn1_pcsc_cancel_fn cn1PcscCancel;

/* 0 = not tried, 1 = ready, -1 = unavailable. Written once under the CAS. */
static int cn1NfcState;
static cn1_pcsc_long cn1NfcContext;
static int cn1NfcCancelFlag;

/* Protocol of each connected handle is needed for SCardTransmit's PCI; the
 * Java side passes it back with the handle, packed into the upper bits. */

static int cn1NfcInit(void) {
    int st = __atomic_load_n(&cn1NfcState, __ATOMIC_ACQUIRE);
    void* lib;
    int expected = 0;
    if (st != 0) {
        return st == 1;
    }
    if (!__atomic_compare_exchange_n(&cn1NfcState, &expected, 2, 0, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) {
        while ((st = __atomic_load_n(&cn1NfcState, __ATOMIC_ACQUIRE)) == 2) {
            usleep(1000);
        }
        return st == 1;
    }
    lib = dlopen("libpcsclite.so.1", RTLD_NOW | RTLD_LOCAL);
    if (lib == 0) {
        lib = dlopen("libpcsclite.so", RTLD_NOW | RTLD_LOCAL);
    }
    if (lib != 0) {
        cn1PcscEstablish = (cn1_pcsc_establish_fn) dlsym(lib, "SCardEstablishContext");
        cn1PcscList = (cn1_pcsc_list_fn) dlsym(lib, "SCardListReaders");
        cn1PcscStatus = (cn1_pcsc_status_fn) dlsym(lib, "SCardGetStatusChange");
        cn1PcscConnect = (cn1_pcsc_connect_fn) dlsym(lib, "SCardConnect");
        cn1PcscDisconnect = (cn1_pcsc_disconnect_fn) dlsym(lib, "SCardDisconnect");
        cn1PcscTransmit = (cn1_pcsc_transmit_fn) dlsym(lib, "SCardTransmit");
        cn1PcscCancel = (cn1_pcsc_cancel_fn) dlsym(lib, "SCardCancel");
    }
    if (lib != 0 && cn1PcscEstablish && cn1PcscList && cn1PcscStatus && cn1PcscConnect
            && cn1PcscDisconnect && cn1PcscTransmit && cn1PcscCancel
            && cn1PcscEstablish(CN1_SCARD_SCOPE_SYSTEM, 0, 0, &cn1NfcContext) == CN1_SCARD_S_SUCCESS) {
        __atomic_store_n(&cn1NfcState, 1, __ATOMIC_RELEASE);
        return 1;
    }
    __atomic_store_n(&cn1NfcState, -1, __ATOMIC_RELEASE);
    return 0;
}

JAVA_BOOLEAN com_codename1_impl_linux_LinuxNative_nfcAvailable___R_boolean(CODENAME_ONE_THREAD_STATE) {
    JAVA_BOOLEAN ok;
    CN1_YIELD_THREAD;
    ok = cn1NfcInit() ? JAVA_TRUE : JAVA_FALSE;
    CN1_RESUME_THREAD;
    return ok;
}

/*
 * Blocks until a card is present on any reader, the timeout elapses (timeoutMs
 * <= 0 waits forever) or nfcCancel is called. Returns a positive value packing
 * the card handle (low 48 bits) and the negotiated protocol (bits 48..55), 0 on
 * timeout, -1 on a PC/SC failure and -2 when cancelled.
 */
static JAVA_LONG cn1NfcWait(JAVA_INT timeoutMs) {
    char readers[2048];
    cn1_pcsc_reader_state states[CN1_NFC_MAX_READERS];
    long waited = 0;
    __atomic_store_n(&cn1NfcCancelFlag, 0, __ATOMIC_RELEASE);
    while (1) {
        cn1_pcsc_dword len = sizeof(readers);
        cn1_pcsc_long rv;
        int count = 0;
        int i;
        const char* p;
        if (__atomic_load_n(&cn1NfcCancelFlag, __ATOMIC_ACQUIRE)) {
            return -2;
        }
        if (timeoutMs > 0 && waited >= timeoutMs) {
            return 0;
        }
        rv = cn1PcscList(cn1NfcContext, 0, readers, &len);
        if (rv == CN1_SCARD_S_SUCCESS) {
            readers[sizeof(readers) - 1] = 0;
            for (p = readers; *p != 0 && count < CN1_NFC_MAX_READERS; p += strlen(p) + 1) {
                memset(&states[count], 0, sizeof(states[count]));
                states[count].szReader = p;
                states[count].dwCurrentState = CN1_SCARD_STATE_UNAWARE;
                count++;
            }
        }
        if (count == 0) {
            /* No reader plugged in (or none yet): poll again after a slice. */
            usleep(CN1_NFC_SLICE_MS * 1000);
            waited += CN1_NFC_SLICE_MS;
            continue;
        }
        rv = cn1PcscStatus(cn1NfcContext, CN1_NFC_SLICE_MS, states, (cn1_pcsc_dword) count);
        waited += CN1_NFC_SLICE_MS;
        if (rv != CN1_SCARD_S_SUCCESS) {
            /* Timeout, cancel and transient reader removal all loop; the flag and
             * the elapsed time decide when to stop. */
            continue;
        }
        for (i = 0; i < count; i++) {
            if ((states[i].dwEventState & CN1_SCARD_STATE_PRESENT) != 0
                    && (states[i].dwEventState & CN1_SCARD_STATE_MUTE) == 0) {
                cn1_pcsc_long handle = 0;
                cn1_pcsc_dword proto = 0;
                rv = cn1PcscConnect(cn1NfcContext, states[i].szReader, CN1_SCARD_SHARE_SHARED,
                        CN1_SCARD_PROTOCOL_T0 | CN1_SCARD_PROTOCOL_T1, &handle, &proto);
                if (rv == CN1_SCARD_S_SUCCESS) {
                    return (JAVA_LONG) (((unsigned long long) handle) & 0xFFFFFFFFFFFFULL)
                            | (((JAVA_LONG) (proto & 0xFF)) << 48);
                }
            }
        }
    }
}

JAVA_LONG com_codename1_impl_linux_LinuxNative_nfcWaitCard___int_R_long(CODENAME_ONE_THREAD_STATE, JAVA_INT timeoutMs) {
    JAVA_LONG r;
    if (!cn1NfcInit()) {
        return -1;
    }
    CN1_YIELD_THREAD;
    r = cn1NfcWait(timeoutMs);
    CN1_RESUME_THREAD;
    return r;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_nfcCancel__(CODENAME_ONE_THREAD_STATE) {
    __atomic_store_n(&cn1NfcCancelFlag, 1, __ATOMIC_RELEASE);
    if (__atomic_load_n(&cn1NfcState, __ATOMIC_ACQUIRE) == 1) {
        cn1PcscCancel(cn1NfcContext);
    }
}

/* Sends one APDU on a handle from nfcWaitCard; returns the full response
 * (data plus SW1 SW2) or null when the card is gone or the call failed. */
JAVA_OBJECT com_codename1_impl_linux_LinuxNative_nfcTransmit___long_byte_1ARRAY_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_LONG card, JAVA_OBJECT apdu) {
    unsigned char* in;
    unsigned char* out;
    cn1_pcsc_dword outLen = 65538;
    JAVA_OBJECT result;
    cn1_pcsc_io_request pci;
    cn1_pcsc_long rv;
    int len;
    if (apdu == JAVA_NULL || __atomic_load_n(&cn1NfcState, __ATOMIC_ACQUIRE) != 1) {
        return JAVA_NULL;
    }
    len = (int) (*(JAVA_ARRAY) apdu).length;
    in = (unsigned char*) malloc(len > 0 ? (size_t) len : 1);
    /* Heap, not stack: an extended-length response is up to 64 KB. */
    out = (unsigned char*) malloc((size_t) outLen);
    if (in == 0 || out == 0) {
        free(in);
        free(out);
        return JAVA_NULL;
    }
    memcpy(in, CN1_ARRAY_DATA(apdu), (size_t) len);
    pci.dwProtocol = (cn1_pcsc_dword) ((card >> 48) & 0xFF);
    pci.cbPciLength = sizeof(pci);
    CN1_YIELD_THREAD;
    rv = cn1PcscTransmit((cn1_pcsc_long) (card & 0xFFFFFFFFFFFFLL), &pci, in, (cn1_pcsc_dword) len, 0, out, &outLen);
    CN1_RESUME_THREAD;
    free(in);
    result = rv == CN1_SCARD_S_SUCCESS ? cn1LinuxNewByteArray(threadStateData, out, (int) outLen) : JAVA_NULL;
    free(out);
    return result;
}

JAVA_VOID com_codename1_impl_linux_LinuxNative_nfcDisconnect___long(CODENAME_ONE_THREAD_STATE, JAVA_LONG card) {
    if (__atomic_load_n(&cn1NfcState, __ATOMIC_ACQUIRE) == 1) {
        cn1PcscDisconnect((cn1_pcsc_long) (card & 0xFFFFFFFFFFFFLL), CN1_SCARD_LEAVE_CARD);
    }
}

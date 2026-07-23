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
 * C ABI of the in-process libcn1ble shared library (a btleplug bridge built as
 * a Rust cdylib). The native port BLE bridge (cn1_linux_ble.c / cn1_windows_ble.c)
 * forwards each ParparVM-translated NativeBleBridge method to one of these.
 *
 * Contract: commands are fire-and-forget and return immediately; each carries a
 * positive monotonic id and is answered by exactly one terminal event echoed on
 * the cn1ble_poll_event stream. cn1ble_poll_event is the only blocking call.
 */
#ifndef CN1BLE_H
#define CN1BLE_H

#ifdef __cplusplus
extern "C" {
#endif

/* 1 if a BLE adapter is available and the engine started, else 0. */
int   cn1ble_start(void);

/* 1 while the engine is running and able to accept commands. */
int   cn1ble_is_alive(void);

/* Blocks up to timeout_ms for the next event. Returns a malloc'd JSON C string
 * the caller must release with cn1ble_free; NULL on timeout; "" once closed. */
char* cn1ble_poll_event(long timeout_ms);

/* Frees a string returned by cn1ble_poll_event. */
void  cn1ble_free(char* p);

void  cn1ble_scan_start(long id, const char* service_csv);
void  cn1ble_scan_stop(long id);
void  cn1ble_connect(long id, const char* address);
void  cn1ble_disconnect(long id, const char* address);
void  cn1ble_discover(long id, const char* address);
void  cn1ble_read(long id, const char* address, const char* service, const char* characteristic);
void  cn1ble_write(long id, const char* address, const char* service, const char* characteristic,
        const unsigned char* value, int value_len, int no_response);
void  cn1ble_subscribe(long id, const char* address, const char* service, const char* characteristic,
        int enable);
void  cn1ble_read_descriptor(long id, const char* address, const char* service, const char* characteristic,
        const char* descriptor);
void  cn1ble_write_descriptor(long id, const char* address, const char* service, const char* characteristic,
        const char* descriptor, const unsigned char* value, int value_len);
void  cn1ble_read_rssi(long id, const char* address);
void  cn1ble_close(void);

#ifdef __cplusplus
}
#endif

#endif /* CN1BLE_H */

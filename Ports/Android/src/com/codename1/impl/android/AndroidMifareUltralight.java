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
package com.codename1.impl.android;

import com.codename1.util.AsyncResource;

class AndroidMifareUltralight extends com.codename1.nfc.MifareUltralight {

    private final android.nfc.tech.MifareUltralight native_;

    AndroidMifareUltralight(android.nfc.tech.MifareUltralight n) {
        this.native_ = n;
    }

    @Override
    public int getPageCount() {
        switch (native_.getType()) {
            case android.nfc.tech.MifareUltralight.TYPE_ULTRALIGHT:
                return 16;
            case android.nfc.tech.MifareUltralight.TYPE_ULTRALIGHT_C:
                return 48;
            default:
                return 0;
        }
    }

    @Override
    public AsyncResource<byte[]> readPages(final int firstPage) {
        return AndroidNfc.asyncIo(new java.util.concurrent.Callable<byte[]>() {
            public byte[] call() throws Exception {
                if (!native_.isConnected()) {
                    native_.connect();
                }
                return native_.readPages(firstPage);
            }
        });
    }

    @Override
    public AsyncResource<Boolean> writePage(final int page, final byte[] data) {
        return AndroidNfc.asyncIo(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() throws Exception {
                if (!native_.isConnected()) {
                    native_.connect();
                }
                native_.writePage(page, data);
                return Boolean.TRUE;
            }
        });
    }
}

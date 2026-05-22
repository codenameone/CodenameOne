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

class AndroidIsoDep extends com.codename1.nfc.IsoDep {

    private final android.nfc.tech.IsoDep native_;

    AndroidIsoDep(android.nfc.tech.IsoDep n) {
        this.native_ = n;
    }

    @Override
    public byte[] getHistoricalBytes() {
        byte[] b = native_.getHistoricalBytes();
        return b != null ? b : new byte[0];
    }

    @Override
    public int getMaxTransceiveLength() {
        return native_.getMaxTransceiveLength();
    }

    @Override
    public boolean isExtendedLengthSupported() {
        return native_.isExtendedLengthApduSupported();
    }

    @Override
    public AsyncResource<byte[]> transceive(final byte[] payload) {
        return AndroidNfc.asyncIo(new java.util.concurrent.Callable<byte[]>() {
            public byte[] call() throws Exception {
                if (!native_.isConnected()) {
                    native_.connect();
                }
                return native_.transceive(payload);
            }
        });
    }
}

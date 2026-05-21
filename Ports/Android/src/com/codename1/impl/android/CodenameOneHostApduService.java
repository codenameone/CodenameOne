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

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import com.codename1.nfc.ApduResponse;
import com.codename1.nfc.HostCardEmulationService;

/**
 * Android bridge that forwards HCE APDUs to the application-supplied
 * com.codename1.nfc.HostCardEmulationService. Codename One's Maven plugin
 * and build daemon register this class in the application's
 * AndroidManifest.xml and generate the matching apduservice.xml from the
 * AIDs reported by HostCardEmulationService.getAids() at build time.
 *
 * At runtime the application calls
 * Nfc.getInstance().registerHostCardEmulationService(myService) which
 * installs the live instance here so processCommandApdu() can dispatch.
 */
public class CodenameOneHostApduService extends HostApduService {

    private static volatile HostCardEmulationService delegate;

    static void bind(HostCardEmulationService svc) {
        delegate = svc;
    }

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        HostCardEmulationService d = delegate;
        if (d == null) {
            return ApduResponse.SW_FILE_NOT_FOUND;
        }
        try {
            byte[] resp = d.processCommand(apdu);
            return resp != null ? resp : ApduResponse.SW_UNKNOWN_ERROR;
        } catch (Throwable t) {
            return ApduResponse.SW_UNKNOWN_ERROR;
        }
    }

    @Override
    public void onDeactivated(int reason) {
        HostCardEmulationService d = delegate;
        if (d != null) {
            try {
                d.onDeactivated(reason);
            } catch (Throwable ignore) {
            }
        }
    }
}

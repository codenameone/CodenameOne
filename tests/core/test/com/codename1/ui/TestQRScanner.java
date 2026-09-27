/*
 * Copyright (c) 2017, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.ext.codescan.ScanResult;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.spinner.Picker;
import org.littlemonkey.qrscanner.QRScanner;

public class TestQRScanner extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        testQRScanner();
        return true;
    }
    
    private void testQRScanner() {
        // Currently this test can't be fully run because we don't have a way to 
        // interact with the native QRScanner.  For now this test will serve
        // to at least prove compile-time success for building projects with the QR
        // scanner
        if (false) {
            QRScanner.scanQRCode(new ScanResult() {
                public void scanCompleted(String contents, String formatName, byte[] rawBytes) {
                    Dialog.show("Completed", contents, "OK", null);
                }

                public void scanCanceled() {
                    Dialog.show("Cancelled", "Scan Cancelled", "OK", null);
                }

                public void scanError(int errorCode, String message) {
                    Dialog.show("Error", message, "OK", null);
                }
            });
        }

    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
    
    
    
}

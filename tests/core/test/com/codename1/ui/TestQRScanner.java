/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.ext.codescan.ScanResult;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.spinner.Picker;
import org.littlemonkey.qrscanner.QRScanner;

/**
 *
 * @author shannah
 */
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

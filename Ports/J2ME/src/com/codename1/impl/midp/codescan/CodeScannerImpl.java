/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.midp.codescan;

import com.codename1.codescan.CodeScanner;
import com.codename1.codescan.ScanResult;
import com.codename1.media.Media;

/**
 *
 * @author Chen
 */
public class CodeScannerImpl extends CodeScanner{

    private BarCodeScanner bs;
    
    public CodeScannerImpl(Media recorder) {
        bs = new BarCodeScanner(recorder);
    }
    
    public void scanQRCode(ScanResult callback) {
        bs.startScaningQRcode(callback);
    }

    public void scanBarCode(ScanResult callback) {
        bs.startScaningBarCode(callback);
    }
    
}

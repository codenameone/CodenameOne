/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codename1.impl.midp.codescan;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
//import com.nvsoft.csk.util.Configuration;
import com.codename1.ui.Image;
import com.google.zxing.common.GlobalHistogramBinarizer;
import java.util.Hashtable;

import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;

/**
 * Thread which does the work of capturing a frame and decoding it.
 *
 * @author Sean Owen
 */
final class SnapshotThread implements Runnable {
//    private static final Logger logger = LoggerFactory.getLogger(SnapshotThread.class);

    private final BarCodeScanner barCodeScanner;
    private final Object waitLock;
    private volatile boolean done;
    private final MultimediaManager multimediaManager;
    private String bestEncoding;

    SnapshotThread(BarCodeScanner barCodeScanner) {
        this.barCodeScanner = barCodeScanner;
        waitLock = new Object();
        done = false;
        multimediaManager = BarCodeScanner.buildMultimediaManager();
    } 

    void continueRun() {
        synchronized (waitLock) {
            waitLock.notifyAll();
        }
    }

    private void waitForSignal() {
        synchronized (waitLock) {
            try {
                waitLock.wait();
            } catch (InterruptedException ie) {
                // continue
            }
        }
    }

    void stop() {
        done = true;
        continueRun();
    }

    public void run() {
        do {
            waitForSignal();
            if (done) {
            	break;
            }
            BinaryBitmap bitmap = null;
            try {
                Player player = barCodeScanner.getPlayer();
                if (player == null) {
                	break;
                }
                multimediaManager.setFocus(player);
                byte[] snapshot = takeSnapshot();
                if (snapshot == null) {
                	break;
                }
                Image capturedImage = Image.createImage(snapshot, 0, snapshot.length);
                LuminanceSource source = new CN1ImageLuminanceSource(capturedImage);
                bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result result = null;
                if(barCodeScanner.type == BarCodeScanner.QRCODE){
                    Reader reader = new QRCodeReader();
                    result = reader.decode(bitmap);
                }else{
                    MultiFormatReader reader = new MultiFormatReader();
                    result = reader.decodeBarcode(bitmap);                
                }
                barCodeScanner.handleDecodedText(result);                
                
               
            } catch (ReaderException re) {
                    barCodeScanner.showError("Not found!");
            } catch (Exception e) {
                barCodeScanner.showError(e.getMessage());
            }
        } while (!done);
    }

    private byte[] takeSnapshot() throws MediaException {

        String bestEncoding = guessBestEncoding();

        VideoControl videoControl = barCodeScanner.getVideoControl();
        if (videoControl == null) {
            throw new MediaException("Can't obtain video control");
        }
        byte[] snapshot = null;
        try {
            snapshot = videoControl.getSnapshot("".equals(bestEncoding) ? null : bestEncoding);
        } catch (MediaException me) {
        }
        if (snapshot == null) {
            // Fall back on JPEG; seems that some cameras default to PNG even
            // when PNG isn't supported!
            snapshot = videoControl.getSnapshot("encoding=jpeg");
            if (snapshot == null) {
                throw new MediaException("Can't obtain a snapshot");
            }
        }
        return snapshot;
    }

    private synchronized String guessBestEncoding() throws MediaException {
        if (bestEncoding == null) {
            // Check this property, present on some Nokias?
            String supportsVideoCapture = System.getProperty("supports.video.capture");
            if ("false".equals(supportsVideoCapture)) {
                throw new MediaException("supports.video.capture is false");
            }

            bestEncoding = "";
            String videoSnapshotEncodings = System.getProperty("video.snapshot.encodings");
            if (videoSnapshotEncodings != null) {
                // We know explicitly what the camera supports; see if PNG is among them since
                // Image.createImage() should always support it
                int pngEncodingStart = videoSnapshotEncodings.indexOf("encoding=png");
                if (pngEncodingStart >= 0) {
                    int space = videoSnapshotEncodings.indexOf(' ', pngEncodingStart);
                    bestEncoding = space >= 0 ?
                            videoSnapshotEncodings.substring(pngEncodingStart, space) :
                            videoSnapshotEncodings.substring(pngEncodingStart);
                }
            }
        }
        return bestEncoding;
    }

}

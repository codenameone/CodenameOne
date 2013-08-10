/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.blackberry.codescan;

import com.codename1.codescan.CodeScanner;
import com.codename1.codescan.ScanResult;
import com.codename1.media.Media;
import com.codename1.ui.Form;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.container.MainScreen;

/**
 *
 * @author Chen
 */
public class CodeScannerImpl extends CodeScanner{

    private MultimediaManager multimediaManager;
    private ScanResult callback;
    private boolean isQRScanning;
    private VideoControl videoControl;
    private Player player;
    private Field viewFinder;
    private ViewFinderScreen viewFinderScreen;
    private BarcodeScanTask task;
    private Timer timer;
    private Result result;
    
    public CodeScannerImpl(MultimediaManager multimediaManager) {
        // Setup barcode decoding hints, arg passed is not used at present
        this.multimediaManager = multimediaManager;
    }
    
    public void scanQRCode(ScanResult callback) {
        this.callback = callback;
        isQRScanning = true;
        startScan();
    }

    public void scanBarCode(ScanResult callback) {
        this.callback = callback;
        isQRScanning = false;
        startScan();
    }
    

    public void cancelScan() {
        stopScan();
        if (callback != null) {
            callback.scanCanceled();
            //callback.onEvent(doneEvent);
            callback = null;
        }
    }

    public void handleDecodedText(Result theResult) {
        // And then stop the scan and go back to the LWUIT form,
        // if there was no error. TODO: fix this properly later.
        callback.scanCompleted(theResult.getText(), theResult.getBarcodeFormat().getName(), theResult.getRawBytes());
        stopScan();
        callback = null;
    }

    /**
     * Get the player instance
     *
     * @return player instance
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the video control
     *
     * @return video control instance
     */
    public VideoControl getVideoControl() {
        return videoControl;
    }

    public void startScan() {
        try {
            System.gc();

            player = Manager.createPlayer("capture://video");
            player.realize();
            multimediaManager.setZoom(player);
            multimediaManager.setExposure(player);
            multimediaManager.setFlash(player);
            player.start();
            videoControl = (VideoControl) player.getControl("VideoControl");

            viewFinder = (Field) videoControl.initDisplayMode(
                    VideoControl.USE_GUI_PRIMITIVE,
                    "net.rim.device.api.ui.Field");

            if (videoControl != null) {
                viewFinderScreen = new ViewFinderScreen();
                UiApplication.getUiApplication().invokeLater(new Runnable() {

                    public void run() {
                        UiApplication.getUiApplication().pushScreen(
                                viewFinderScreen);
                        viewFinder.setFocus();

                    }
                });
                videoControl.setVisible(true);
                videoControl.setDisplayFullScreen(true);
                task = new BarcodeScanTask();
                // create timer every 3 seconds, get a screenshot
                timer = new Timer();
                timer.schedule(task, 0, 3000); // once every 3 seconds 
            } else {
                throw new MediaException("Video Control is not initialized");
            }
        } catch (Exception e) {
            callback.scanError(-1, e.getMessage());
        }
    }

    public void stopScan() {
        if (timer != null) {
            timer.cancel(); // stop the timer
        }
        // Destroy the videoControl and player

        if (videoControl != null) {
            // TODO: This might not be needed, but have it just in case
            videoControl.setVisible(false);
            videoControl = null;
        }
        if (player != null) {
            player.close();
            player = null;
        }
        if (viewFinderScreen != null) {
            synchronized (Application.getEventLock()) {
                // viewFinderScreen.close();
                UiApplication.getUiApplication().popScreen(viewFinderScreen);
            }
            viewFinderScreen = null;
        }
        System.gc();
    }

    final class BarcodeScanTask extends TimerTask {

        public void run() {
            try {

                Bitmap bmpScreenshot = new Bitmap(Display.getWidth(),
                        Display.getHeight());
                multimediaManager.setFocus(player);
                Display.screenshot(bmpScreenshot);
                // creating luminance source
                LuminanceSource source = new BitmapLuminanceSource(
                        bmpScreenshot);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                        source));

                if(isQRScanning){
                    QRCodeReader reader = new QRCodeReader();
                    result = reader.decodeQR(bitmap);
                }else{
                    MultiFormatReader reader = new MultiFormatReader();
                    result = reader.decodeBarcode(bitmap);                
                }
                handleDecodedText(result);
                timer.cancel(); // stop the timer
            } catch (Throwable e) {
                callback.scanError(-1, e.getMessage());
            }
        }
    }

    public final class ViewFinderScreen extends MainScreen {

        public ViewFinderScreen() throws Exception {
            super(MainScreen.DEFAULT_CLOSE);
            add(viewFinder);
        }

        protected boolean navigationClick(int arg0, int arg1) {
            cancelScan();
            this.close();
            return true;
        }

        public boolean onClose() {
            try {
                stopScan();
                return super.onClose();                
            } catch (Exception e) {
                return false;
            }
        }
    }

}

package com.codename1.impl.midp.codescan;

import com.codename1.codescan.ScanResult;
import com.codename1.media.Media;

import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;

import com.google.zxing.Result;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;

public class BarCodeScanner {
    
    private Media media;    
    private Player player;    
    private SnapshotThread snapshotThread;
    private Form cameraForm;
    private Form backForm;
    private ScanResult callback;
    int type;
    public static final int BARCODE = 0;
    public static final int QRCODE = 1;
    
    
    public BarCodeScanner(Media media) {
        this.media = media;
        backForm = Display.getInstance().getCurrent();
    }

    private void startScan() {
        player = (Player) media.getVideoComponent().getClientProperty("nativePlayer");
        
        MultimediaManager multimediaManager = buildMultimediaManager();
        if (player != null) {
            multimediaManager.setZoom(player);
            multimediaManager.setExposure(player);
            multimediaManager.setFlash(player);
        }
        media.play();
        snapshotThread = new SnapshotThread(this);
        new Thread(snapshotThread).start();
        if (Display.getInstance().isEdt()) {
            cameraForm.show();
        } else {
            // Now show the dialog in EDT
            Display.getInstance().callSerially(new Runnable() {

                public void run() {
                    cameraForm.show();
                }
            });
        }
    }


    static MultimediaManager buildMultimediaManager() {
        if (isAMMSPresent()) {
            return new AdvancedMultimediaManager();
        } else {
            return new DefaultMultimediaManager();
        }
    }

    class HandleDecodedTextCall implements Runnable {
        Result theResult;
        public void run() {
            callback.scanCompleted(theResult.getText(), theResult.getBarcodeFormat().getName(), theResult.getRawBytes());
            stop();
            media.cleanup();
            backForm.showBack();
            backForm = null;
            cameraForm = null;
            callback = null;
        }        
    }

    public void handleDecodedText(final Result theResult) {
        HandleDecodedTextCall h = new HandleDecodedTextCall();
        h.theResult = theResult;
        Display.getInstance().callSerially(h);
    }

    private void stop() {
        media.pause();        
    }
    
    private void startScaning(ScanResult callback) {
        this.callback = callback;
            try {
                // Add the listener for scan and cancel
                Container cmdContainer = new Container(new FlowLayout(Component.CENTER));
                Button scanButton = new Button(new Command("Scan") {

                    public void actionPerformed(ActionEvent evt) {
                        cameraForm.repaint();
                        if (snapshotThread != null) {
                            snapshotThread.continueRun();
                        }
                    }
                });
                Button cancelButton = new Button(new Command("Cancel") {

                    public void actionPerformed(ActionEvent evt) {
                        if (snapshotThread != null) {
                            snapshotThread.stop();
                            cancelScan();
                        }
                    }
                });
                cmdContainer.addComponent(scanButton);
                cmdContainer.addComponent(cancelButton);
                cameraForm = new Form();
                cameraForm.setScrollable(false);
                cameraForm.setLayout(new BorderLayout());
                cameraForm.addComponent(BorderLayout.CENTER, media.getVideoComponent());
                cameraForm.addComponent(BorderLayout.SOUTH, cmdContainer);
            } catch (Exception e) {
//	            throw new AppException("Image/video capture not supported on this phone", e).setCode(97);
                e.printStackTrace();
            }
        startScan();
    }

    class CancelScanCall implements Runnable {
        public void run() {
            stop();
            media.cleanup();
            backForm.showBack();
            callback.scanCanceled();
            backForm = null;
            cameraForm = null;
            callback = null;
        }        
    }

    private void cancelScan() {
        Display.getInstance().callSerially(new CancelScanCall());
    }
    
    private static boolean isAMMSPresent() {
        try {
            Class.forName("javax.microedition.amms.GlobalManager");
            return true;
        } catch (ClassNotFoundException _ex) {
            return false;
        }

    }

    Player getPlayer() {
        return player;
    }

    VideoControl getVideoControl() {
        return (VideoControl) media.getVideoComponent().getClientProperty("VideoControl");
    }

    void showError(String err) {
        callback.scanError(-1, err);
    }

    void startScaningQRcode(ScanResult callback) {
        type = QRCODE;
        startScaning(callback);
    }

    void startScaningBarCode(ScanResult callback) {
        type = BARCODE;
        startScaning(callback);
    }
    
    
    
}

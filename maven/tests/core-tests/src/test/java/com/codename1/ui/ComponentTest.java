package com.codename1.ui;


import com.codename1.ext.codescan.ScanResult;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.testing.TestRunnerComponent;
import com.codename1.ui.Toolbar;
import com.codename1.ui.geom.GeneralPathTest;
import java.io.IOException;
import com.codename1.ui.layouts.BoxLayout;
import org.littlemonkey.qrscanner.QRScanner;

/**
 * This file was generated by <a href="https://www.codenameone.com/">Codename One</a> for the purpose 
 * of building native mobile applications using Java.
 */
public class ComponentTest {

    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature
        Log.bindCrashProtection(true);
    }
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        TestRunnerComponent testRunner = new TestRunnerComponent();
        testRunner.add(new GeneralPathTest());
        testRunner.add(new TestComponent());
        testRunner.showForm();
       
        
        if (System.currentTimeMillis() < 100) {
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

    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }
    
    public void destroy() {
    }

}

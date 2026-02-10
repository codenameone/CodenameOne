package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.NetworkManager;

public class VPNDetectionAPITest extends BaseTest {
    @Override
    public boolean runTest() {
        try {
            NetworkManager manager = NetworkManager.getInstance();
            manager.isVPNDetectionSupported();
            manager.isVPNActive();
            done();
        } catch (Throwable t) {
            fail("VPN detection API invocation failed: " + t);
        }
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}

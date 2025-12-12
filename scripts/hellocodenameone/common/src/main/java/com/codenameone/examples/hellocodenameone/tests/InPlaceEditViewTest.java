package com.codenameone.examples.hellocodenameone.tests;

import com.codenameone.examples.hellocodenameone.InPlaceEditViewNative;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;

public class InPlaceEditViewTest extends BaseTest {
    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        InPlaceEditViewNative nativeInterface = NativeLookup.create(InPlaceEditViewNative.class);
        if (nativeInterface != null && nativeInterface.isSupported()) {
            nativeInterface.runReproductionTest((success, error) -> {
                if (!success) {
                    fail("Reproduction test failed: " + error);
                } else {
                    done();
                }
            });
        } else {
            done();
        }
        return true;
    }
}

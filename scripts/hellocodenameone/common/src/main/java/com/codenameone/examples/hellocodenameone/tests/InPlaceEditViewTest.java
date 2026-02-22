package com.codenameone.examples.hellocodenameone.tests;

import com.codenameone.examples.hellocodenameone.InPlaceEditViewNative;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;

public class InPlaceEditViewTest extends BaseTest {
    private static InPlaceEditViewTest instance;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        instance = this;
        InPlaceEditViewNative nativeInterface = NativeLookup.create(InPlaceEditViewNative.class);
        if (nativeInterface != null && nativeInterface.isSupported()) {
            nativeInterface.runReproductionTest();
        } else {
            done();
        }
        return true;
    }

    public static void onSuccess() {
        instance.done();
    }

    public static void onError(String error) {
        instance.fail("Reproduction test failed: " + error);
    }
}

package com.codenameone.examples.hellocodenameone.tests;

import com.codenameone.examples.hellocodenameone.InPlaceEditViewNative;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;

public class InPlaceEditViewTest extends BaseTest {
    @Override
    public boolean runTest() throws Exception {
        InPlaceEditViewNative nativeInterface = NativeLookup.create(InPlaceEditViewNative.class);
        if (nativeInterface != null && nativeInterface.isSupported()) {
            nativeInterface.runReproductionTest();
            // Allow time for the race condition to trigger
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
                Display.getInstance().callSerially(() -> done());
            }).start();
        } else {
            done();
        }
        return true;
    }
}

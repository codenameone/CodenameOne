package com.codenameone.examples.hellocodenameone.tests;

import com.codenameone.examples.hellocodenameone.InPlaceEditViewNative;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.ui.util.UITimer;

public class InPlaceEditViewTest extends BaseTest {
    @Override
    public boolean runTest() throws Exception {
        InPlaceEditViewNative nativeInterface = NativeLookup.create(InPlaceEditViewNative.class);
        if (nativeInterface != null && nativeInterface.isSupported()) {
            nativeInterface.runReproductionTest();
            // Allow time for the race condition to trigger
            UITimer.timer(5000, false, Display.getInstance().getCurrent(), () -> {
                done();
            });
        } else {
            done();
        }
        return true;
    }
}

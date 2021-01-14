/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.demos.signin;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Display;

/**
 * This essentially tests that Firebase doesn't crash with an error.  This test project
 * includes a GoogleService-Info.plist file inside the native/ios directory that is required
 * by firebase in order to work.  If, for some reason, the build server doesn't include this
 * file in the bundle, then firebase will fail with an error like:
 * <Notice>: *** Terminating app due to uncaught exception 'com.firebase.core', reason: '`[FIRApp configure];` (`FirebaseApp.configure()` in Swift) could not find a valid GoogleService-Info.plist in your project. Please download one from https://console.firebase.google.com/.'
 * @author shannah
 */
public class TestFirebase extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        if ("ios".equals(Display.getInstance().getPlatformName()) && !Display.getInstance().isSimulator()) {
            SignIn.doFirebase();
        }
        return true;
    }
    
}

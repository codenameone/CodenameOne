package com.codenameone.developerguide.advancedtopics;

import com.codename1.io.Log;
import com.codename1.system.NativeLookup;
import com.mycompany.myapp.MyNative;

/**
 * Snippets showcasing native interface usage.
 */
public class NativeInterfaceSnippets {

    public void callNativeInterface() {
        // tag::nativeLookup[]
        MyNative my = NativeLookup.create(MyNative.class);
        if (my != null && my.isSupported()) {
            Log.p(my.helloWorld("Hi"));
        }
        // end::nativeLookup[]
    }
}

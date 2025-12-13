package com.codenameone.developerguide.advancedtopics;

import com.codename1.impl.android.AndroidNativeUtil;

// tag::nativeCallsImpl[]
class NativeCallsImpl {
     public void nativeMethod() {
        AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
            public void run() {
               // ...
            }
        });
     }
    // ...
}
// end::nativeCallsImpl[]

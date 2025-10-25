package com.codenameone.developerguide.advancedtopics;

/**
 * Illustrates running code on Android's UI thread.
 */
public class AndroidThreadSnippets {

    public void runAsync() {
        // tag::androidRunOnUiThread[]
        com.codename1.impl.android.AndroidNativeUtil.getActivity().runOnUiThread(new Runnable() {
            public void run() {
               // your native code here...
            }
        });
        // end::androidRunOnUiThread[]
    }

    public void runBlocking() {
        // tag::androidRunOnUiThreadAndBlock[]
        com.codename1.impl.android.AndroidImplementation.runOnUiThreadAndBlock(new Runnable() {
            public void run() {
               // your native code here...
            }
        });
        // end::androidRunOnUiThreadAndBlock[]
    }
}

package com.codenameone.support.mobihelp;

import android.content.Context;
import com.codename1.impl.android.AndroidNativeUtil;

// tag::mobihelpNativeImplContext[]
class MobihelpNativeImpl {
    // tag::mobihelpNativeContext[]
    private static Context context() {
        return AndroidNativeUtil.getActivity().getApplicationContext();
    }
    // end::mobihelpNativeContext[]

    // tag::mobihelpNativeClearUserData[]
    public void clearUserData() {
        com.freshdesk.mobihelp.Mobihelp.clearUserData(context());
    }
    // end::mobihelpNativeClearUserData[]

    private static AndroidActivityWrapper activity() {
        return new AndroidActivityWrapper();
    }

    // tag::mobihelpNativeShowSolutions[]
    public void showSolutions() {
        activity().runOnUiThread(new Runnable() {
            public void run() {
                com.freshdesk.mobihelp.Mobihelp.showSolutions(context());
            }
        });

    }
    // end::mobihelpNativeShowSolutions[]

    // tag::mobihelpNativeGetUnreadCountAsync[]
    public void getUnreadCountAsync(final int callbackId) {
        activity().runOnUiThread(new Runnable() {
            public void run() {
                com.freshdesk.mobihelp.Mobihelp.getUnreadCountAsync(context(), new com.freshdesk.mobihelp.UnreadUpdatesCallback() {
                    public void onResult(com.freshdesk.mobihelp.MobihelpCallbackStatus status, Integer count) {
                        MobihelpNativeCallback.fireUnreadUpdatesCallback(callbackId, status.ordinal(), count);
                    }
                });
            }
        });

    }
    // end::mobihelpNativeGetUnreadCountAsync[]
}
// end::mobihelpNativeImplContext[]

// Helper wrapper to simulate Android Activity runOnUiThread for compilation
class AndroidActivityWrapper {
    void runOnUiThread(Runnable runnable) {
        runnable.run();
    }
}

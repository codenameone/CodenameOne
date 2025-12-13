package com.codenameone.support.mobihelp;

import com.codename1.ui.Display;
import java.util.HashMap;
import java.util.Map;

// tag::mobihelpNativeCallback[]
public class MobihelpNativeCallback {
    private static int nextId = 0;
    private static Map<Integer, UnreadUpdatesCallback> callbacks = new HashMap<>();

    static int registerUnreadUpdatesCallback(UnreadUpdatesCallback callback) {
        callbacks.put(nextId, callback);
        return nextId++;
    }

    public static void fireUnreadUpdatesCallback(int callbackId, final int status, final int count) {
        final UnreadUpdatesCallback cb = callbacks.get(callbackId);
        if (cb != null) {
            callbacks.remove(callbackId);
            Display.getInstance().callSerially(new Runnable() {

                public void run() {
                    MobihelpCallbackStatus status2 = MobihelpCallbackStatus.values()[status];
                    cb.onResult(status2, count);
                }

            });
        }
    }
}
// end::mobihelpNativeCallback[]

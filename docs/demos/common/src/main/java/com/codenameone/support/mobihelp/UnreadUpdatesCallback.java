package com.codenameone.support.mobihelp;

// tag::unreadUpdatesCallback[]
public interface UnreadUpdatesCallback {
    //This method is called once the unread updates count is available.
    void onResult(MobihelpCallbackStatus status, Integer count);
}
// end::unreadUpdatesCallback[]

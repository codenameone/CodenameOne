package com.freshdesk.mobihelp;

public interface UnreadUpdatesCallback {
    void onResult(MobihelpCallbackStatus status, Integer count);
}

package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeInterface;

public interface LocalNotificationNative extends NativeInterface {
    int countPendingNotificationsWithId(String notificationId);
}

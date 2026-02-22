package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeInterface;

public interface LocalNotificationNative extends NativeInterface {
    void clearScheduledLocalNotifications(String notificationId);
    int getScheduledLocalNotificationCount(String notificationId);
}

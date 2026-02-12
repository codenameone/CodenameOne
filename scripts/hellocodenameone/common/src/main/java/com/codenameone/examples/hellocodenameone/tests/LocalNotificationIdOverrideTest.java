package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.notifications.LocalNotification;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.hellocodenameone.LocalNotificationNative;

public class LocalNotificationIdOverrideTest extends BaseTest {
    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        LocalNotificationNative nativeInterface = NativeLookup.create(LocalNotificationNative.class);
        if (nativeInterface == null || !nativeInterface.isSupported()) {
            done();
            return true;
        }

        String notificationId = "cn1ss-local-notification-id-override";
        Display.getInstance().cancelLocalNotification(notificationId);

        LocalNotification first = new LocalNotification();
        first.setId(notificationId);
        first.setAlertTitle("First");
        first.setAlertBody("First body");
        Display.getInstance().scheduleLocalNotification(first, System.currentTimeMillis() + 60000, LocalNotification.REPEAT_NONE);

        LocalNotification second = new LocalNotification();
        second.setId(notificationId);
        second.setAlertTitle("Second");
        second.setAlertBody("Second body");
        Display.getInstance().scheduleLocalNotification(second, System.currentTimeMillis() + 120000, LocalNotification.REPEAT_NONE);

        UITimer.timer(1000, false, Display.getInstance().getCurrent(), () -> {
            int pendingWithSameId = nativeInterface.countPendingNotificationsWithId(notificationId);
            Display.getInstance().cancelLocalNotification(notificationId);
            if (pendingWithSameId != 1) {
                fail("Expected a single pending notification for id '" + notificationId + "' but found " + pendingWithSameId);
            } else {
                done();
            }
        });

        return true;
    }
}

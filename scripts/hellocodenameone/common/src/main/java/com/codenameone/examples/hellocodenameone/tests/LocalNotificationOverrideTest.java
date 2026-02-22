package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.notifications.LocalNotification;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codenameone.examples.hellocodenameone.LocalNotificationNative;

public class LocalNotificationOverrideTest extends BaseTest {
    private static final long FIRE_OFFSET_MS = 5 * 60 * 1000L;

    @Override
    public boolean runTest() {
        LocalNotificationNative nativeInterface = NativeLookup.create(LocalNotificationNative.class);
        if (nativeInterface == null || !nativeInterface.isSupported()) {
            done();
            return true;
        }

        try {
            String notificationId = "cn1-local-notification-override-" + System.currentTimeMillis();
            nativeInterface.clearScheduledLocalNotifications(notificationId);

            schedule(notificationId, "first");
            schedule(notificationId, "second");

            int count = nativeInterface.getScheduledLocalNotificationCount(notificationId);
            nativeInterface.clearScheduledLocalNotifications(notificationId);

            if (count != 1) {
                fail("Expected one scheduled notification for duplicate id, but found " + count);
                return true;
            }
            done();
        } catch (Throwable t) {
            fail("Local notification override test failed: " + t);
        }
        return true;
    }

    private void schedule(String notificationId, String body) {
        LocalNotification notification = new LocalNotification();
        notification.setId(notificationId);
        notification.setAlertTitle("Local Notification Override Test");
        notification.setAlertBody(body);
        Display.getInstance().scheduleLocalNotification(
                notification,
                System.currentTimeMillis() + FIRE_OFFSET_MS,
                LocalNotification.REPEAT_NONE
        );
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}

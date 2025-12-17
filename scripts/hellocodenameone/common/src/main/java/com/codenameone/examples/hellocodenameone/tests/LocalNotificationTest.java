package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.notifications.LocalNotification;
import com.codename1.ui.Display;
import com.codename1.ui.util.UITimer;

public class LocalNotificationTest extends BaseTest {
    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        LocalNotification n1 = new LocalNotification();
        n1.setId("test-notification-1");
        n1.setAlertTitle("Test Notification 1");
        n1.setAlertBody("This is a test notification");
        Display.getInstance().scheduleLocalNotification(n1, System.currentTimeMillis() + 1000, LocalNotification.REPEAT_NONE);

        LocalNotification n2 = new LocalNotification();
        n2.setId("test-notification-2");
        n2.setAlertTitle("Test Notification 2");
        n2.setAlertBody("This is a repeating notification");
        Display.getInstance().scheduleLocalNotification(n2, System.currentTimeMillis() + 2000, LocalNotification.REPEAT_MINUTE);

        LocalNotification n3 = new LocalNotification();
        n3.setId("test-notification-3");
        n3.setAlertTitle("Test Notification 3");
        n3.setAlertBody("This is a notification with badge");
        n3.setBadgeNumber(5);
        Display.getInstance().scheduleLocalNotification(n3, System.currentTimeMillis() + 3000, LocalNotification.REPEAT_NONE);

        UITimer.timer(4000, false, () -> {
            Display.getInstance().cancelLocalNotification("test-notification-1");
            Display.getInstance().cancelLocalNotification("test-notification-2");
            Display.getInstance().cancelLocalNotification("test-notification-3");
            done();
        });

        return true;
    }
}

package com.codename1.notifications;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation.ScheduledNotification;
import com.codename1.ui.Display;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalNotificationTest extends UITestBase {

    @FormTest
    void testScheduleNotificationRecordsRequest() throws Exception {
        implementation.clearScheduledNotifications();

        LocalNotification notification = new LocalNotification();
        notification.setId("reminder");
        notification.setBadgeNumber(3);
        notification.setAlertBody("Stretch now");
        notification.setAlertTitle("Break");
        notification.setAlertSound("/notification_sound_bell.mp3");
        notification.setAlertImage("/icon.png");
        notification.setForeground(true);

        long triggerTime = 123456L;
        Display.getInstance().scheduleLocalNotification(notification, triggerTime, LocalNotification.REPEAT_DAY);

        List<ScheduledNotification> scheduled = implementation.getScheduledNotifications();
        assertEquals(1, scheduled.size());
        ScheduledNotification stored = scheduled.get(0);
        assertSame(notification, stored.getNotification());
        assertEquals(triggerTime, stored.getFirstTime());
        assertEquals(LocalNotification.REPEAT_DAY, stored.getRepeat());

        assertEquals("reminder", notification.getId());
        assertEquals(3, notification.getBadgeNumber());
        assertEquals("Stretch now", notification.getAlertBody());
        assertEquals("Break", notification.getAlertTitle());
        assertEquals("/notification_sound_bell.mp3", notification.getAlertSound());
        assertEquals("/icon.png", notification.getAlertImage());
        assertTrue(notification.isForeground());
    }

    @FormTest
    void testCancelNotificationRemovesMatchingEntry() throws Exception {
        implementation.clearScheduledNotifications();

        LocalNotification morning = new LocalNotification();
        morning.setId("morning");
        LocalNotification evening = new LocalNotification();
        evening.setId("evening");

        Display.getInstance().scheduleLocalNotification(morning, 10L, LocalNotification.REPEAT_NONE);
        Display.getInstance().scheduleLocalNotification(evening, 20L, LocalNotification.REPEAT_WEEK);
        assertEquals(2, implementation.getScheduledNotifications().size());

        Display.getInstance().cancelLocalNotification("morning");

        List<ScheduledNotification> remaining = implementation.getScheduledNotifications();
        assertEquals(1, remaining.size());
        assertEquals("evening", remaining.get(0).getNotification().getId());

        Display.getInstance().cancelLocalNotification(null);
        assertEquals(1, implementation.getScheduledNotifications().size());
    }
}

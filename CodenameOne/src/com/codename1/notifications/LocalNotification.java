/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.notifications;

/// Local notifications are user notifications that are scheduled by the app itself. They
/// are very similar to push notifications, except that they originate locally, rather than
/// remotely.
///
/// They enable an app that isnt running in the foreground to let its users know it
/// has information for them. The information could be a message, an impending calendar
/// event, or new data on a remote server. They can display an alert message or
/// they can badge the app icon. They can also play a sound when the alert or badge
/// number is shown.
///
/// When users are notified that the app has a message, event, or other data for them,
/// they can launch the app and see the details. They can also choose to ignore the notification,
/// in which case the app is not activated.
///
/// This class encapsulates a single notification (though the notification can
/// be repeating).
///
/// Usage
/// ```java
/// // File: GeofenceListenerImpl.java
/// public class GeofenceListenerImpl implements GeofenceListener {
/// @Override
///     public void onExit(String id) {
///     }
/// @Override
///     public void onEntered(String id) {
///         if(!Display.getInstance().isMinimized()) {
///             Display.getInstance().callSerially(() -> {
///                 Dialog.show("Welcome", "Thanks for arriving", "OK", null);
///             });
///         } else {
///             LocalNotification ln = new LocalNotification();
///             ln.setId("LnMessage");
///             ln.setAlertTitle("Welcome");
///             ln.setAlertBody("Thanks for arriving!");
///             Display.getInstance().scheduleLocalNotification(ln, System.currentTimeMillis() + 10, LocalNotification.REPEAT_NONE);
///         }
///     }
/// }
/// ```
///
/// ```java
/// // File: GeofenceSample.java
/// Geofence gf = new Geofence("test", loc, 100, 100000);
/// LocationManager.getLocationManager().addGeoFencing(GeofenceListenerImpl.class, gf);
/// ```
///
/// **Android Note:** The default image that is used on the android top status bar
/// and on the notification itself is the App's icon.  However Android 5 and above will only display
/// this image as a silhouette using alpha pixels.  This will result in many icons appearing to be
/// a blank white square.  In such cases you can provide an alternate image to be displayed instead.
/// Place a 24x24 image named "ic_stat_notify.png" in your project's native/android
/// directory, and this image will be used instead.
/// @author shannah
///
/// #### See also
///
/// - com.codename1.ui.Display#scheduleLocalNotification(LocalNotification n, long firstTime, int repeat)
///
/// - com.codename1.ui.Display#cancelLocalNotification(java.lang.String id)
public class LocalNotification {

    /// Constant used in `#setRepeatType(int)` to indicate that this
    /// notification should not be repeated.
    public static final int REPEAT_NONE = 0;

    /// Constant used in `#setRepeatType(int)` to indicate that this
    /// notification should be repeated every 1 minute.
    public static final int REPEAT_MINUTE = 1;

    /// Constant used in `#setRepeatType(int)` to indicate that this
    /// notification should be repeated every hour.
    public static final int REPEAT_HOUR = 3;

    /// Constant used in `#setRepeatType(int)` to indicate that this
    /// notification should be repeated every day.
    public static final int REPEAT_DAY = 4;

    /// Constant used in `#setRepeatType(int)` to indicate that this
    /// notification should be repeated every week.
    public static final int REPEAT_WEEK = 5;

    // We don't support month or year right now because it is too complicated
    // to keep track of leap years, and days in month on platforms that only 
    // support repeat by milliseconds etc..

    private String id = "";
    private int badgeNumber = -1;
    private String alertBody = "";
    private String alertTitle = "";
    private String alertSound = "";
    private String alertImage = "";
    private boolean foreground;

    /// Gets the badge number to set for this notification.
    ///
    /// #### Returns
    ///
    /// the badgeNumber
    public int getBadgeNumber() {
        return badgeNumber;
    }

    /// Gets the badge number to set for this notification.
    ///
    /// #### Parameters
    ///
    /// - `badgeNumber`: the badgeNumber to set
    public void setBadgeNumber(int badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    /// Gets the alert body to be displayed for this notification.
    ///
    /// #### Returns
    ///
    /// the alertBody
    public String getAlertBody() {
        return alertBody;
    }

    /// Sets the alert body to be displayed for this notification.
    ///
    /// #### Parameters
    ///
    /// - `alertBody`: the alertBody to set
    public void setAlertBody(String alertBody) {
        this.alertBody = alertBody;
    }

    /// Gets the alert title to be displayed for this notification.
    ///
    /// #### Returns
    ///
    /// the alertTitle
    public String getAlertTitle() {
        return alertTitle;
    }

    /// Sets the alert title to be displayed for this notification.
    ///
    /// #### Parameters
    ///
    /// - `alertTitle`: the alertTitle to set
    public void setAlertTitle(String alertTitle) {
        this.alertTitle = alertTitle;
    }

    /// Gets the alert sound to be sounded when the notification arrives.  This
    /// should refer to a sound file that is bundled in the default package of your
    /// app.
    ///
    /// #### Returns
    ///
    /// the alertSound
    public String getAlertSound() {
        return alertSound;
    }

    /// Sets the alert sound to be sounded when the notification arrives.  This
    /// should refer to a sound file that is bundled in the default package of your
    /// app.
    /// The name of the file must start with the "notification_sound" prefix.
    ///
    /// `````java LocalNotification n = new LocalNotification(); n.setAlertSound("/notification_sound_bells.mp3"); `````
    ///
    /// #### Parameters
    ///
    /// - `alertSound`: the alertSound to set
    public void setAlertSound(String alertSound) {
        this.alertSound = alertSound;
    }

    /// Gets the ID of the notification.  The ID is the only information that is
    /// passed to `LocalNotificationCallback#localNotificationReceived(java.lang.String)`
    /// so you can use it as a lookup key to retrieve the rest of the information as required
    /// from storage or some other mechanism.
    ///
    /// The ID can also be used to cancel the notification later using `com.codename1.ui.Display#cancelLocalNotification(java.lang.String)`
    ///
    /// #### Returns
    ///
    /// the id
    public String getId() {
        return id;
    }

    /// Sets the ID of the notification.  The ID is the only information that is
    /// passed to `LocalNotificationCallback#localNotificationReceived(java.lang.String)`
    /// so you can use it as a lookup key to retrieve the rest of the information as required
    /// from storage or some other mechanism.
    ///
    /// The ID can also be used to cancel the notification later using `com.codename1.ui.Display#cancelLocalNotification(java.lang.String)`
    ///
    /// #### Parameters
    ///
    /// - `id`: the id to set
    public void setId(String id) {
        this.id = id;
    }

    /// Gets the notification image
    ///
    /// #### Returns
    ///
    /// image path
    public String getAlertImage() {
        return alertImage;
    }

    /// Sets an image to be displayed on the platform notifications bar, if the underlying platform
    /// supports image displaying otherwise the image will be ignored.
    ///
    /// #### Parameters
    ///
    /// - `image`: a path to the image, the image needs to be placed in the app root.
    public void setAlertImage(String image) {
        this.alertImage = image;
    }

    /// Checks whether this notification will be displayed in the device's notification center even when the app is in the foreground.
    ///
    /// #### Returns
    ///
    /// True if the notification will display in the device's notification center even when the app is in the foreground.
    ///
    /// #### Since
    ///
    /// 7.0
    public boolean isForeground() {
        return foreground;
    }

    /// Set whether this notification should be displayed in the device's notification center even when the app is
    /// in the foreground.
    ///
    /// #### Parameters
    ///
    /// - `foreground`: True to display this notification in the notification center even when the app is in the foreground.
    ///
    /// #### Since
    ///
    /// 7.0
    public void setForeground(boolean foreground) {
        this.foreground = foreground;
    }

}

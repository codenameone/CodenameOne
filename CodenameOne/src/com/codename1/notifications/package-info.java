/// Local Notification API
///
/// Local notifications are similar to push notifications, except that they are initiated locally by the app, rather than
/// remotely.
/// They are useful for communicating information to the user while the app is running in the background, since they
/// manifest
/// themselves as pop-up notifications on supported devices.
///
/// Sending Notifications
///
/// The process for sending a notification is:
///
///
/// - Create a `com.codename1.notifications.LocalNotification` object with the information you want to send in
/// the notification.
///
///
/// - Pass the object to `com.codename1.ui.Display#scheduleLocalNotification(LocalNotification,long,int)`.
///
/// Notifications can either be set up as one-time only or as repeating.
///
/// Example Sending Notification
///
/// ```java
///         `LocalNotification n = new LocalNotification();
///         n.setId("demo-notification");
///         n.setAlertBody("It's time to take a break and look at me");
///         n.setAlertTitle("Break Time!");
///         n.setAlertSound("/notification_sound_bells.mp3"); //file name must begin with notification_sound
///
///         Display.getInstance().scheduleLocalNotification(
///                 n,
///                 System.currentTimeMillis() + 10 * 1000, // fire date/time
///                 LocalNotification.REPEAT_MINUTE  // Whether to repeat and what frequency
///         );`
///
/// ```
///
/// The resulting notification will look like
///
/// The above screenshot was taken on the iOS simulator.
///
/// Receiving Notifications
///
/// The API for receiving/handling local notifications is also similar to push. Your application's main lifecycle class
/// needs
/// to implement the `com.codename1.notifications.LocalNotificationCallback` interface which includes a single
/// method: `com.codename1.notifications.LocalNotificationCallback#notificationReceived(String)`
///
/// The notificationId parameter will match the id value of the notification as set using
/// `com.codename1.notifications.LocalNotification#setId(String)`.
///
/// Example Receiving Notification
///
/// ```java
///         `public class BackgroundLocationDemo implements LocalNotificationCallback {
///             //...
///
///             public void init(Object context) {
///                 //...`
///
///             public void start() {
///                 //...
///
///             }
///
///             public void stop() {
///                 //...
///             }
///
///             public void destroy() {
///                 //...
///             }
///
///             public void localNotificationReceived(String notificationId) {
///                 System.out.println("Received local notification "+notificationId);
///             }
///         }
///        }
///
/// ```
///
/// **NOTE:** `com.codename1.notifications.LocalNotificationCallback#localNotificationReceived(String)`
/// is only called when the user responds to the notification by tapping on the alert. If the user doesn't opt to
/// click on the notification, then this event handler will never be fired.
///
/// Cancelling Notifications
///
/// Repeating notifications will continue until they are canceled by the app. You can cancel a single notification by
/// calling `com.codename1.ui.Display#cancelLocalNotification(String)`
///
/// Where notificationId is the string id that was set for the notification using `com.codename1.notifications.LocalNotification#setId(String)`.
///
/// Sample App
///
/// You can see a full sample that uses the new local notifications API
/// [here](https://github.com/codenameone/codenameone-demos/blob/master/LocalNotificationTest/src/com/codename1/tests/localnotifications/LocalNotificationTest.java).
package com.codename1.notifications;

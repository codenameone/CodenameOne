// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::working-with-ios-java-001[]
LocalNotification n = new LocalNotification();
n.setId("demo-notification");
n.setAlertBody("it's time to take a break and look at me");
n.setAlertTitle("Break Time!");
n.setAlertSound("beep-01a.mp3");

Display.getInstance().scheduleLocalNotification(
 n,
 System.currentTimeMillis() + 10 * 1000, // fire date/time
 LocalNotification.REPEAT_MINUTE // Whether to repeat and what frequency
);
// end::working-with-ios-java-001[]

// tag::working-with-ios-java-002[]
public void localNotificationReceived(String notificationId) {
}
// end::working-with-ios-java-002[]

// tag::working-with-ios-java-003[]
public class BackgroundLocationDemo implements LocalNotificationCallback {
 //...

 public void init(Object context) {
 //...
 }

 public void start() {
 //...

 }

 public void stop() {
 //...
 }

 public void destroy() {
 //...
 }

 public void localNotificationReceived(String notificationId) {
 System.out.println("Received local notification "+notificationId);
 }
}
// end::working-with-ios-java-003[]

// tag::working-with-ios-java-004[]
Display.getInstance().cancelLocalNotification(notificationId);
// end::working-with-ios-java-004[]

// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::notifications-and-background-execution-java-001[]
import com.codename1.notifications.NotificationPermissionRequest;
import com.codename1.notifications.NotificationPermissionResult.AuthorizationLevel;

NotificationPermissionRequest req = new NotificationPermissionRequest()
        .provisional(true)      // iOS: deliver quietly without an explicit prompt
        .timeSensitive(true);   // iOS: allow the time-sensitive interruption level

Display.getInstance().requestNotificationPermission(req, result -> {
    if (result.isGranted()) {
        // schedule notifications
    }
    AuthorizationLevel level = result.getAuthorizationLevel();
});
// end::notifications-and-background-execution-java-001[]

// tag::notifications-and-background-execution-java-002[]
LocalNotification n = new LocalNotification()
        .setChannelId("messages")                 // Android channel (see below)
        .setGroup("chat-42")                       // bundle related notifications
        .setProgress(100, 40)                      // determinate progress bar (Android)
        .setTimeSensitive(true);                   // break through Focus / elevated importance
n.setId("msg-42");
n.setAlertTitle("New message");
n.setAlertBody("Tap to reply");
n.addAction("open", "Open");
n.addInputAction("reply", "Reply", "Type a message", "Send");  // inline quick reply
Display.getInstance().scheduleLocalNotification(n, System.currentTimeMillis() + 1000,
        LocalNotification.REPEAT_NONE);
// end::notifications-and-background-execution-java-002[]

// tag::notifications-and-background-execution-java-003[]
new NotificationChannelBuilder("messages", "Messages")
        .description("Incoming chat messages")
        .importance(NotificationChannelBuilder.IMPORTANCE_HIGH)
        .sound("/notification_sound_ping.mp3")
        .enableVibration(true)
        .lightColor(0xff0000)
        .register();
// end::notifications-and-background-execution-java-003[]

// tag::notifications-and-background-execution-java-004[]
WorkRequest req = WorkRequest.builder("sync", SyncWorker.class)
        .setRequiresNetwork(true)
        .setRequiresCharging(true)
        .setPeriodic(6 * 60 * 60 * 1000L)
        .putInputData("account", "primary")
        .build();
BackgroundWork.schedule(req);
// end::notifications-and-background-execution-java-004[]

// tag::notifications-and-background-execution-java-005[]
ForegroundService svc = ForegroundService.start("downloads", "Downloading", "Please wait",
        null, service -> {
            for (int i = 0; i <= 100; i++) {
                service.updateNotification("Downloading", i + "%");
                // ... do a chunk of work ...
            }
        });
// auto-stops when the task returns, or call svc.stop()
// end::notifications-and-background-execution-java-005[]

// tag::notifications-and-background-execution-java-006[]
public class MyApp extends Lifecycle {
    public void onReceivedSharedContent(SharedContent content) {
        for (SharedContent.Item item : content.getItems()) {
            if (item.getType() == SharedContent.TYPE_IMAGE) {
                importImage(item.getFilePath()); // a FileSystemStorage path
            }
        }
    }
}
// end::notifications-and-background-execution-java-006[]

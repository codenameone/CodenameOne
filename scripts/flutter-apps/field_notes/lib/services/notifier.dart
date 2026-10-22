import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class Notifier {
  Notifier._();

  static final Notifier instance = Notifier._();

  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  int _nextId = 0;

  Future<void> init() async {
    const settings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
      iOS: DarwinInitializationSettings(),
    );
    await _plugin.initialize(settings, onDidReceiveNotificationResponse: (response) {
      lastOpened = response.payload;
    });
    await _plugin
        .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
  }

  String? lastOpened;

  Future<void> noteSaved(String title) {
    const details = NotificationDetails(
      android: AndroidNotificationDetails('notes', 'Notes',
          channelDescription: 'Saved notes', importance: Importance.high, priority: Priority.high),
      iOS: DarwinNotificationDetails(),
    );
    return _plugin.show(_nextId++, 'Note saved', title, details, payload: title);
  }
}

// package:flutter_local_notifications, over Codename One's LocalNotification.
// Every settings and details class maps to one options holder; what the platform
// cannot express (channels, styles, LEDs) is accepted and dropped. A tap reaches
// onDidReceiveNotificationResponse through the generated entry point.

@JavaName('com.codename1.flutter.plugins.notifications.Importance')
enum Importance { unspecified, none, min, low, defaultImportance, high, max }

@JavaName('com.codename1.flutter.plugins.notifications.Priority')
enum Priority { min, low, defaultPriority, high, max }

@JavaName('com.codename1.flutter.plugins.notifications.RepeatInterval')
enum RepeatInterval { everyMinute, hourly, daily, weekly }

@JavaName('com.codename1.flutter.plugins.notifications.AndroidScheduleMode')
enum AndroidScheduleMode { alarmClock, exact, exactAllowWhileIdle, inexact, inexactAllowWhileIdle }

@JavaName('com.codename1.flutter.plugins.notifications.DateTimeComponents')
enum DateTimeComponents { time, dayOfWeekAndTime, dayOfMonthAndTime, dateAndTime }

@JavaName('com.codename1.flutter.plugins.notifications.UILocalNotificationDateInterpretation')
enum UILocalNotificationDateInterpretation { absoluteTime, wallClockTime }

@JavaName('com.codename1.flutter.plugins.notifications.NotificationResponseType')
enum NotificationResponseType { selectedNotification, selectedNotificationAction }

@JavaName('com.codename1.flutter.plugins.notifications.NotificationAppLaunchDetails')
class NotificationAppLaunchDetails {
  external NotificationAppLaunchDetails(bool didNotificationLaunchApp, NotificationResponse? notificationResponse);
  external bool get didNotificationLaunchApp;
  external NotificationResponse? get notificationResponse;
}

@JavaName('com.codename1.flutter.plugins.notifications.NotificationResponse')
class NotificationResponse {
  external int? get id;
  external String? get payload;
  external String? get actionId;
  external String? get input;
  external NotificationResponseType get notificationResponseType;
}

@JavaName('com.codename1.flutter.plugins.notifications.NotificationOptions')
class InitializationSettings {
  external InitializationSettings({Object? android, Object? iOS, Object? macOS, Object? linux, Object? windows, Object? importance, Object? priority, Object? channelDescription, Object? icon, Object? playSound, Object? enableVibration, Object? sound, Object? ticker, Object? styleInformation, Object? color, Object? largeIcon, Object? showWhen, Object? autoCancel, Object? ongoing, Object? category, Object? groupKey, Object? channelShowBadge, Object? visibility, Object? fullScreenIntent, Object? presentAlert, Object? presentBadge, Object? presentSound, Object? presentBanner, Object? presentList, Object? badgeNumber, Object? subtitle, Object? threadIdentifier, Object? attachments, Object? requestAlertPermission, Object? requestBadgePermission, Object? requestSoundPermission, Object? defaultPresentAlert, Object? defaultPresentBadge, Object? defaultPresentSound, Object? defaultPresentBanner, Object? defaultPresentList, Object? notificationCategories, Object? enableLights, Object? ledColor, Object? timeoutAfter, Object? onlyAlertOnce, Object? actions, Object? number, Object? tag});
}

@JavaName('com.codename1.flutter.plugins.notifications.NotificationOptions')
class AndroidInitializationSettings {
  external AndroidInitializationSettings(String defaultIcon, {Object? android, Object? iOS, Object? macOS, Object? linux, Object? windows, Object? importance, Object? priority, Object? channelDescription, Object? icon, Object? playSound, Object? enableVibration, Object? sound, Object? ticker, Object? styleInformation, Object? color, Object? largeIcon, Object? showWhen, Object? autoCancel, Object? ongoing, Object? category, Object? groupKey, Object? channelShowBadge, Object? visibility, Object? fullScreenIntent, Object? presentAlert, Object? presentBadge, Object? presentSound, Object? presentBanner, Object? presentList, Object? badgeNumber, Object? subtitle, Object? threadIdentifier, Object? attachments, Object? requestAlertPermission, Object? requestBadgePermission, Object? requestSoundPermission, Object? defaultPresentAlert, Object? defaultPresentBadge, Object? defaultPresentSound, Object? defaultPresentBanner, Object? defaultPresentList, Object? notificationCategories, Object? enableLights, Object? ledColor, Object? timeoutAfter, Object? onlyAlertOnce, Object? actions, Object? number, Object? tag});
}

@JavaName('com.codename1.flutter.plugins.notifications.NotificationOptions')
class DarwinInitializationSettings {
  external DarwinInitializationSettings({Object? android, Object? iOS, Object? macOS, Object? linux, Object? windows, Object? importance, Object? priority, Object? channelDescription, Object? icon, Object? playSound, Object? enableVibration, Object? sound, Object? ticker, Object? styleInformation, Object? color, Object? largeIcon, Object? showWhen, Object? autoCancel, Object? ongoing, Object? category, Object? groupKey, Object? channelShowBadge, Object? visibility, Object? fullScreenIntent, Object? presentAlert, Object? presentBadge, Object? presentSound, Object? presentBanner, Object? presentList, Object? badgeNumber, Object? subtitle, Object? threadIdentifier, Object? attachments, Object? requestAlertPermission, Object? requestBadgePermission, Object? requestSoundPermission, Object? defaultPresentAlert, Object? defaultPresentBadge, Object? defaultPresentSound, Object? defaultPresentBanner, Object? defaultPresentList, Object? notificationCategories, Object? enableLights, Object? ledColor, Object? timeoutAfter, Object? onlyAlertOnce, Object? actions, Object? number, Object? tag});
}

@JavaName('com.codename1.flutter.plugins.notifications.NotificationOptions')
class LinuxInitializationSettings {
  external LinuxInitializationSettings(String defaultActionName, {Object? android, Object? iOS, Object? macOS, Object? linux, Object? windows, Object? importance, Object? priority, Object? channelDescription, Object? icon, Object? playSound, Object? enableVibration, Object? sound, Object? ticker, Object? styleInformation, Object? color, Object? largeIcon, Object? showWhen, Object? autoCancel, Object? ongoing, Object? category, Object? groupKey, Object? channelShowBadge, Object? visibility, Object? fullScreenIntent, Object? presentAlert, Object? presentBadge, Object? presentSound, Object? presentBanner, Object? presentList, Object? badgeNumber, Object? subtitle, Object? threadIdentifier, Object? attachments, Object? requestAlertPermission, Object? requestBadgePermission, Object? requestSoundPermission, Object? defaultPresentAlert, Object? defaultPresentBadge, Object? defaultPresentSound, Object? defaultPresentBanner, Object? defaultPresentList, Object? notificationCategories, Object? enableLights, Object? ledColor, Object? timeoutAfter, Object? onlyAlertOnce, Object? actions, Object? number, Object? tag});
}

@JavaName('com.codename1.flutter.plugins.notifications.NotificationOptions')
class NotificationDetails {
  external NotificationDetails({Object? android, Object? iOS, Object? macOS, Object? linux, Object? windows, Object? importance, Object? priority, Object? channelDescription, Object? icon, Object? playSound, Object? enableVibration, Object? sound, Object? ticker, Object? styleInformation, Object? color, Object? largeIcon, Object? showWhen, Object? autoCancel, Object? ongoing, Object? category, Object? groupKey, Object? channelShowBadge, Object? visibility, Object? fullScreenIntent, Object? presentAlert, Object? presentBadge, Object? presentSound, Object? presentBanner, Object? presentList, Object? badgeNumber, Object? subtitle, Object? threadIdentifier, Object? attachments, Object? requestAlertPermission, Object? requestBadgePermission, Object? requestSoundPermission, Object? defaultPresentAlert, Object? defaultPresentBadge, Object? defaultPresentSound, Object? defaultPresentBanner, Object? defaultPresentList, Object? notificationCategories, Object? enableLights, Object? ledColor, Object? timeoutAfter, Object? onlyAlertOnce, Object? actions, Object? number, Object? tag});
}

@JavaName('com.codename1.flutter.plugins.notifications.NotificationOptions')
class AndroidNotificationDetails {
  external AndroidNotificationDetails(String channelId, String channelName, {Object? android, Object? iOS, Object? macOS, Object? linux, Object? windows, Object? importance, Object? priority, Object? channelDescription, Object? icon, Object? playSound, Object? enableVibration, Object? sound, Object? ticker, Object? styleInformation, Object? color, Object? largeIcon, Object? showWhen, Object? autoCancel, Object? ongoing, Object? category, Object? groupKey, Object? channelShowBadge, Object? visibility, Object? fullScreenIntent, Object? presentAlert, Object? presentBadge, Object? presentSound, Object? presentBanner, Object? presentList, Object? badgeNumber, Object? subtitle, Object? threadIdentifier, Object? attachments, Object? requestAlertPermission, Object? requestBadgePermission, Object? requestSoundPermission, Object? defaultPresentAlert, Object? defaultPresentBadge, Object? defaultPresentSound, Object? defaultPresentBanner, Object? defaultPresentList, Object? notificationCategories, Object? enableLights, Object? ledColor, Object? timeoutAfter, Object? onlyAlertOnce, Object? actions, Object? number, Object? tag});
}

@JavaName('com.codename1.flutter.plugins.notifications.NotificationOptions')
class DarwinNotificationDetails {
  external DarwinNotificationDetails({Object? android, Object? iOS, Object? macOS, Object? linux, Object? windows, Object? importance, Object? priority, Object? channelDescription, Object? icon, Object? playSound, Object? enableVibration, Object? sound, Object? ticker, Object? styleInformation, Object? color, Object? largeIcon, Object? showWhen, Object? autoCancel, Object? ongoing, Object? category, Object? groupKey, Object? channelShowBadge, Object? visibility, Object? fullScreenIntent, Object? presentAlert, Object? presentBadge, Object? presentSound, Object? presentBanner, Object? presentList, Object? badgeNumber, Object? subtitle, Object? threadIdentifier, Object? attachments, Object? requestAlertPermission, Object? requestBadgePermission, Object? requestSoundPermission, Object? defaultPresentAlert, Object? defaultPresentBadge, Object? defaultPresentSound, Object? defaultPresentBanner, Object? defaultPresentList, Object? notificationCategories, Object? enableLights, Object? ledColor, Object? timeoutAfter, Object? onlyAlertOnce, Object? actions, Object? number, Object? tag});
}

@JavaName('com.codename1.flutter.plugins.notifications.NotificationOptions')
class LinuxNotificationDetails {
  external LinuxNotificationDetails({Object? android, Object? iOS, Object? macOS, Object? linux, Object? windows, Object? importance, Object? priority, Object? channelDescription, Object? icon, Object? playSound, Object? enableVibration, Object? sound, Object? ticker, Object? styleInformation, Object? color, Object? largeIcon, Object? showWhen, Object? autoCancel, Object? ongoing, Object? category, Object? groupKey, Object? channelShowBadge, Object? visibility, Object? fullScreenIntent, Object? presentAlert, Object? presentBadge, Object? presentSound, Object? presentBanner, Object? presentList, Object? badgeNumber, Object? subtitle, Object? threadIdentifier, Object? attachments, Object? requestAlertPermission, Object? requestBadgePermission, Object? requestSoundPermission, Object? defaultPresentAlert, Object? defaultPresentBadge, Object? defaultPresentSound, Object? defaultPresentBanner, Object? defaultPresentList, Object? notificationCategories, Object? enableLights, Object? ledColor, Object? timeoutAfter, Object? onlyAlertOnce, Object? actions, Object? number, Object? tag});
}

@JavaName('com.codename1.flutter.plugins.notifications.NotificationOptions')
class AndroidNotificationChannel {
  external AndroidNotificationChannel(String id, String name, {Object? android, Object? iOS, Object? macOS, Object? linux, Object? windows, Object? importance, Object? priority, Object? channelDescription, Object? icon, Object? playSound, Object? enableVibration, Object? sound, Object? ticker, Object? styleInformation, Object? color, Object? largeIcon, Object? showWhen, Object? autoCancel, Object? ongoing, Object? category, Object? groupKey, Object? channelShowBadge, Object? visibility, Object? fullScreenIntent, Object? presentAlert, Object? presentBadge, Object? presentSound, Object? presentBanner, Object? presentList, Object? badgeNumber, Object? subtitle, Object? threadIdentifier, Object? attachments, Object? requestAlertPermission, Object? requestBadgePermission, Object? requestSoundPermission, Object? defaultPresentAlert, Object? defaultPresentBadge, Object? defaultPresentSound, Object? defaultPresentBanner, Object? defaultPresentList, Object? notificationCategories, Object? enableLights, Object? ledColor, Object? timeoutAfter, Object? onlyAlertOnce, Object? actions, Object? number, Object? tag});
}

@JavaName('com.codename1.flutter.plugins.notifications.FlutterLocalNotificationsPlugin')
class FlutterLocalNotificationsPlugin {
  external FlutterLocalNotificationsPlugin();
  external Future<bool?> initialize(InitializationSettings initializationSettings,
      {void Function(NotificationResponse details)? onDidReceiveNotificationResponse,
      Object? onDidReceiveBackgroundNotificationResponse});
  external Future<void> show(int id, String? title, String? body, NotificationDetails? notificationDetails,
      {String? payload});
  external Future<void> periodicallyShow(int id, String? title, String? body, RepeatInterval repeatInterval,
      NotificationDetails notificationDetails, {AndroidScheduleMode androidScheduleMode, String? payload});
  external Future<void> zonedSchedule(int id, String? title, String? body, Object scheduledDate,
      NotificationDetails notificationDetails, {AndroidScheduleMode androidScheduleMode, String? payload,
      DateTimeComponents? matchDateTimeComponents,
      UILocalNotificationDateInterpretation? uiLocalNotificationDateInterpretation});
  external Future<void> cancel(int id, {String? tag});
  external Future<void> cancelAll();
  external Future<NotificationAppLaunchDetails?> getNotificationAppLaunchDetails();
  external Future<List<Object>> pendingNotificationRequests();
  external T? resolvePlatformSpecificImplementation<T>([Type? witness]);
}

// What resolvePlatformSpecificImplementation would return; it returns null here,
// so the usual `?.requestNotificationsPermission()` is skipped.
@JavaName('com.codename1.flutter.plugins.notifications.FlutterLocalNotificationsPlugin')
class AndroidFlutterLocalNotificationsPlugin {
  external Future<bool?> requestNotificationsPermission();
  external Future<bool?> requestExactAlarmsPermission();
  external Future<void> createNotificationChannel(AndroidNotificationChannel channel);
}

@JavaName('com.codename1.flutter.plugins.notifications.FlutterLocalNotificationsPlugin')
class IOSFlutterLocalNotificationsPlugin {
  external Future<bool?> requestPermissions({bool alert, bool badge, bool sound, bool critical,
      bool provisional});
}

@JavaName('com.codename1.flutter.plugins.notifications.FlutterLocalNotificationsPlugin')
class MacOSFlutterLocalNotificationsPlugin {
  external Future<bool?> requestPermissions({bool alert, bool badge, bool sound, bool critical,
      bool provisional});
}

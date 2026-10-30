// package:app_settings, over Display.execute: app-settings: on iOS, a settings
// intent on Android, nothing elsewhere.

@JavaName('com.codename1.flutter.plugins.appsettings.AppSettingsType')
enum AppSettingsType { accessibility, alarm, apn, appLocale, batteryOptimization, bluetooth, cellular,
  dataRoaming, date, developer, device, display, generalSettings, hotspot, internalStorage, location,
  lockAndPassword, manageUnknownApps, nfc, notification, security, settings, sound, subscriptions, vpn,
  wifi, wireless }

@JavaName('com.codename1.flutter.plugins.appsettings.AppSettingsPanelType')
enum AppSettingsPanelType { internetConnectivity, nfc, volume, wifi }

@JavaName('com.codename1.flutter.plugins.appsettings.AppSettings')
class AppSettings {
  external static Future<void> openAppSettings({AppSettingsType type, bool asAnotherTask});
  external static Future<void> openAppSettingsPanel(AppSettingsPanelType type);
}

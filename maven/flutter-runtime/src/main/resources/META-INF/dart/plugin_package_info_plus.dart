// package:package_info_plus, from the app's build properties (AppName,
// package_name, AppVersion).

@JavaName('com.codename1.flutter.plugins.packageinfo.PackageInfo')
class PackageInfo {
  external static Future<PackageInfo> fromPlatform();
  external static void setMockInitialValues({required String appName, required String packageName,
      required String version, required String buildNumber, required String buildSignature,
      String? installerStore, DateTime? updateTime});
  external String get appName;
  external String get packageName;
  external String get version;
  external String get buildNumber;
  external String get buildSignature;
  external String? get installerStore;
}

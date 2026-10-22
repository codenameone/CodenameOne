// package:device_info_plus, from Display's platform and device properties. Fields
// the platform does not expose to Codename One read as "unknown".

@JavaName('com.codename1.flutter.plugins.deviceinfo.BaseDeviceInfo')
class BaseDeviceInfo {
  external Map<String, dynamic> get data;
}

@JavaName('com.codename1.flutter.plugins.deviceinfo.AndroidBuildVersion')
class AndroidBuildVersion {
  external String get release;
  external int get sdkInt;
}

@JavaName('com.codename1.flutter.plugins.deviceinfo.AndroidDeviceInfo')
class AndroidDeviceInfo extends BaseDeviceInfo {
  external String get model;
  external String get brand;
  external String get manufacturer;
  external String get device;
  external String get id;
  external bool get isPhysicalDevice;
  external AndroidBuildVersion get version;
}

@JavaName('com.codename1.flutter.plugins.deviceinfo.IosDeviceInfo')
class IosDeviceInfo extends BaseDeviceInfo {
  external String get name;
  external String get model;
  external String get systemName;
  external String get systemVersion;
  external String? get identifierForVendor;
  external bool get isPhysicalDevice;
}

@JavaName('com.codename1.flutter.plugins.deviceinfo.DeviceInfoPlugin')
class DeviceInfoPlugin {
  external DeviceInfoPlugin();
  external Future<AndroidDeviceInfo> get androidInfo;
  external Future<IosDeviceInfo> get iosInfo;
  external Future<BaseDeviceInfo> get deviceInfo;
}

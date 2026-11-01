// package:android_play_install_referrer. The Play Install Referrer service is not
// bound, so installReferrer fails as it does where the service is unavailable.

@JavaName('com.codename1.flutter.plugins.installreferrer.ReferrerDetails')
class ReferrerDetails {
  external String? get installReferrer;
  external int get referrerClickTimestampSeconds;
  external int get installBeginTimestampSeconds;
  external int get referrerClickTimestampServerSeconds;
  external int get installBeginTimestampServerSeconds;
  external String? get installVersion;
  external bool get googlePlayInstantParam;
}

@JavaName('com.codename1.flutter.plugins.installreferrer.AndroidPlayInstallReferrer')
class AndroidPlayInstallReferrer {
  external static Future<ReferrerDetails> get installReferrer;
}

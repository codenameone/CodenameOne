// cn1:package firebase_app_check
// firebase_app_check over the firebase pack. Codename One has no Play Integrity,
// DeviceCheck or App Attest attestation, so the providers are accepted and only the
// debug provider yields a token: the Display property `firebase.appCheck.debugToken`
// (a debug token registered in the Firebase console). Without it getToken answers null.

@JavaName('com.codename1.flutter.plugins.firebase.AndroidProvider')
enum AndroidProvider { debug, playIntegrity, safetyNet }

@JavaName('com.codename1.flutter.plugins.firebase.AppleProvider')
enum AppleProvider { debug, deviceCheck, appAttest, appAttestWithDeviceCheckFallback }

@JavaName('com.codename1.flutter.plugins.firebase.ReCaptchaV3Provider')
class ReCaptchaV3Provider {
  external ReCaptchaV3Provider(String siteKey);
}

@JavaName('com.codename1.flutter.plugins.firebase.ReCaptchaV3Provider')
class ReCaptchaEnterpriseProvider {
  external ReCaptchaEnterpriseProvider(String siteKey);
}

@JavaName('com.codename1.flutter.plugins.firebase.FirebaseAppCheck')
class FirebaseAppCheck {
  external static FirebaseAppCheck get instance;
  external static FirebaseAppCheck instanceFor({required Object app});
  external Future<void> activate({Object? webProvider, Object? providerWeb, AndroidProvider? androidProvider,
      AppleProvider? appleProvider, Object? providerAndroid, Object? providerApple});
  external Future<String?> getToken([bool? forceRefresh]);
  external Future<String> getLimitedUseToken();
  external Future<void> setTokenAutoRefreshEnabled(bool isTokenAutoRefreshEnabled);
  external Stream<String?> get onTokenChange;
  external bool get isTokenAutoRefreshEnabled;
}

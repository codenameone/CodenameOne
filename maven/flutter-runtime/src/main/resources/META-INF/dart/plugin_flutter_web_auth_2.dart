// package:flutter_web_auth_2: the sign-in page in a BrowserComponent on its own
// form; the callback URL completes the future, cancel fails it with
// PlatformException(code: 'CANCELED').

@JavaName('com.codename1.flutter.plugins.webauth.FlutterWebAuth2Options')
class FlutterWebAuth2Options {
  external FlutterWebAuth2Options({bool? preferEphemeral, String? debugOrigin, int? intentFlags,
      String? windowName, int? timeout, String? landingPageHtml, bool? silentAuth, bool? useWebview,
      String? httpsHost, String? httpsPath});
  external String? get httpsHost;
  external String? get httpsPath;
  external int? get timeout;
}

@JavaName('com.codename1.flutter.plugins.webauth.FlutterWebAuth2')
class FlutterWebAuth2 {
  external static Future<String> authenticate({required String url, required String callbackUrlScheme,
      FlutterWebAuth2Options options});
}

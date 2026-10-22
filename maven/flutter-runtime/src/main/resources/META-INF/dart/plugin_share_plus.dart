// package:share_plus, over Display.share. The platform sheet reports no outcome,
// so every share completes with ShareResultStatus.unavailable, as share_plus does
// on platforms that cannot tell.

@JavaName('com.codename1.flutter.plugins.share.ShareResultStatus')
enum ShareResultStatus { success, dismissed, unavailable }

@JavaName('com.codename1.flutter.plugins.share.ShareResult')
class ShareResult {
  external String get raw;
  external ShareResultStatus get status;
}

@JavaName('com.codename1.flutter.plugins.share.ShareParams')
class ShareParams {
  external ShareParams({String? text, String? subject, String? title, Uri? uri, List<XFile>? files,
      Rect? sharePositionOrigin});
}

@JavaName('com.codename1.flutter.plugins.share.SharePlus')
class SharePlus {
  external static SharePlus get instance;
  external Future<ShareResult> share(ShareParams params);
}

// The pre-10 static API, still what most apps call.
@JavaName('com.codename1.flutter.plugins.share.SharePlus')
class Share {
  external static Future<ShareResult> share(String text, {String? subject, Rect? sharePositionOrigin});
  external static Future<ShareResult> shareUri(Uri uri, {Rect? sharePositionOrigin});
  external static Future<ShareResult> shareXFiles(List<XFile> files, {String? subject, String? text,
      Rect? sharePositionOrigin});
}

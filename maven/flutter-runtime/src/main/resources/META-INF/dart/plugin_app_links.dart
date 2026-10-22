// package:app_links, over the AppArg property the platform sets when a link
// opens the app. The generated entry point forwards each start to it.

@JavaName('com.codename1.flutter.plugins.applinks.AppLinks')
class AppLinks {
  external AppLinks();
  external Future<Uri?> getInitialLink();
  external Future<String?> getInitialLinkString();
  external Future<Uri?> getLatestLink();
  external Future<String?> getLatestLinkString();
  external Stream<String> get stringLinkStream;
  external Stream<Uri> get uriLinkStream;
}

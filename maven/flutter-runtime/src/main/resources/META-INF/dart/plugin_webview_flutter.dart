// package:webview_flutter (the v4 controller API), hosting Codename One's
// BrowserComponent. Navigation decisions go through BrowserNavigationCallback,
// JavaScript through execute/executeAndReturnString and channels through
// addJSCallback. Without a display the controller records its state and consults
// the navigation delegate, so app logic around it still runs.

@JavaName('com.codename1.flutter.plugins.webview.JavaScriptMode')
enum JavaScriptMode { disabled, unrestricted }

@JavaName('com.codename1.flutter.plugins.webview.NavigationDecision')
enum NavigationDecision { prevent, navigate }

@JavaName('com.codename1.flutter.plugins.webview.LoadRequestMethod')
enum LoadRequestMethod { get, post }

@JavaName('com.codename1.flutter.plugins.webview.NavigationRequest')
class NavigationRequest {
  external String get url;
  external bool get isMainFrame;
}

@JavaName('com.codename1.flutter.plugins.webview.WebResourceError')
class WebResourceError {
  external int get errorCode;
  external String get description;
  external bool? get isForMainFrame;
  external String? get url;
}

@JavaName('com.codename1.flutter.plugins.webview.JavaScriptMessage')
class JavaScriptMessage {
  external JavaScriptMessage({required String message});
  external String get message;
}

@JavaName('com.codename1.flutter.plugins.webview.NavigationDelegate')
class NavigationDelegate {
  external NavigationDelegate({dynamic Function(NavigationRequest request)? onNavigationRequest,
      void Function(String url)? onPageStarted, void Function(String url)? onPageFinished,
      void Function(int progress)? onProgress, void Function(WebResourceError error)? onWebResourceError,
      void Function(dynamic change)? onUrlChange, void Function(dynamic request)? onHttpAuthRequest});
}

@JavaName('com.codename1.flutter.plugins.webview.WebViewController')
class WebViewController {
  external WebViewController();
  external Future<void> loadRequest(Uri uri, {LoadRequestMethod method, Map<String, String> headers, Uint8List? body});
  external Future<void> loadHtmlString(String html, {String? baseUrl});
  external Future<void> loadFlutterAsset(String key);
  external Future<void> loadFile(String absoluteFilePath);
  external Future<void> setJavaScriptMode(JavaScriptMode javaScriptMode);
  external Future<void> setNavigationDelegate(NavigationDelegate delegate);
  external Future<void> runJavaScript(String javaScript);
  external Future<Object> runJavaScriptReturningResult(String javaScript);
  external Future<void> addJavaScriptChannel(String name, {required void Function(JavaScriptMessage message) onMessageReceived});
  external Future<void> removeJavaScriptChannel(String javaScriptChannelName);
  external Future<void> reload();
  external Future<void> goBack();
  external Future<void> goForward();
  external Future<bool> canGoBack();
  external Future<bool> canGoForward();
  external Future<String?> currentUrl();
  external Future<String?> getTitle();
  external Future<void> setBackgroundColor(Color color);
  external Future<void> setUserAgent(String? userAgent);
  external Future<void> clearCache();
  external Future<void> clearLocalStorage();
  external Future<void> enableZoom(bool enabled);
}

@JavaName('com.codename1.flutter.plugins.webview.WebViewWidget')
class WebViewWidget extends Widget {
  external WebViewWidget({Key? key, required WebViewController controller, dynamic gestureRecognizers});
}

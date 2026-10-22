// package:connectivity_plus, over NetworkManager's network type and its change
// listener. Headless (tests) it answers [ConnectivityResult.none].

@JavaName('com.codename1.flutter.plugins.connectivity.ConnectivityResult')
enum ConnectivityResult { bluetooth, wifi, ethernet, mobile, none, vpn, other }

@JavaName('com.codename1.flutter.plugins.connectivity.Connectivity')
class Connectivity {
  external Connectivity();
  external Future<List<ConnectivityResult>> checkConnectivity();
  external Stream<List<ConnectivityResult>> get onConnectivityChanged;
}

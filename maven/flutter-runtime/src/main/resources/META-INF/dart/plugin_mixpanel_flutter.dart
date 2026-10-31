// package:mixpanel_flutter, over Mixpanel's HTTP ingestion API (/track and
// /engage, JSON, batched until flush). cn1PendingCount is Codename One's, not the
// plugin's: the queued events and profile updates.

@JavaName('com.codename1.flutter.plugins.mixpanel.People')
class People {
  external void set(String prop, dynamic to);
  external void setOnce(String prop, dynamic to);
  external void increment(String prop, double by);
  external void append(String name, dynamic value);
  external void union(String name, List<dynamic> value);
  external void remove(String name, dynamic value);
  external void unset(String name);
  external void trackCharge(double amount, {Map<String, dynamic>? properties});
  external void clearCharges();
  external void deleteUser();
}

@JavaName('com.codename1.flutter.plugins.mixpanel.Mixpanel')
class Mixpanel {
  external static Future<Mixpanel> init(String token, {bool optOutTrackingDefault,
      required bool trackAutomaticEvents, Map<String, dynamic>? superProperties, Map<String, dynamic>? config});
  external int get cn1PendingCount;
  external void setServerURL(String serverURL);
  external void setLoggingEnabled(bool loggingEnabled);
  external void setUseIpAddressForGeolocation(bool useIpAddressForGeolocation);
  external void setFlushBatchSize(int flushBatchSize);
  external Future<bool> hasOptedOutTracking();
  external void optInTracking();
  external void optOutTracking();
  external Future<void> identify(String distinctId);
  external void alias(String alias, String distinctId);
  external Future<String> getDistinctId();
  external Future<void> track(String eventName, {Map<String, dynamic>? properties});
  external void timeEvent(String eventName);
  external Future<double> eventElapsedTime(String eventName);
  external void registerSuperProperties(Map<String, dynamic> properties);
  external void registerSuperPropertiesOnce(Map<String, dynamic> properties);
  external void unregisterSuperProperty(String propertyName);
  external Future<Map<String, dynamic>> getSuperProperties();
  external void clearSuperProperties();
  external People getPeople();
  external void reset();
  external Future<void> flush();
}

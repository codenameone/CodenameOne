// package:shared_preferences, over com.codename1.flutter.plugins.sharedpreferences:
// typed values kept in Codename One Storage (in memory when headless).

@JavaName('com.codename1.flutter.plugins.sharedpreferences.SharedPreferences')
class SharedPreferences {
  external static Future<SharedPreferences> getInstance();
  external static void setMockInitialValues(Map<String, Object> values);
  external Set<String> getKeys();
  external Object? get(String key);
  external bool containsKey(String key);
  external String? getString(String key);
  external int? getInt(String key);
  external double? getDouble(String key);
  external bool? getBool(String key);
  external List<String>? getStringList(String key);
  external Future<bool> setString(String key, String value);
  external Future<bool> setInt(String key, int value);
  external Future<bool> setDouble(String key, double value);
  external Future<bool> setBool(String key, bool value);
  external Future<bool> setStringList(String key, List<String> value);
  external Future<bool> remove(String key);
  external Future<bool> clear();
  external Future<void> reload();
  external Future<bool> commit();
}

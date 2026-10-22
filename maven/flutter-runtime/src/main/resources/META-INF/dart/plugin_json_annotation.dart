// package:json_annotation: the annotations drive json_serializable, whose output
// (`*.g.dart`) an app commits, so only what that generated code calls at run time
// is backed here.

@JavaName('com.codename1.flutter.plugins.json.JsonAnnotation.$enumDecode')
external K $enumDecode<K, V>(Map<K, V> enumValues, Object? source, {K? unknownValue});

@JavaName('com.codename1.flutter.plugins.json.JsonAnnotation.$enumDecodeNullable')
external K? $enumDecodeNullable<K, V>(Map<K, V> enumValues, Object? source, {K? unknownValue});

class JsonSerializable {
  external const JsonSerializable({bool? explicitToJson, bool? includeIfNull, Object? fieldRename,
      bool? createFactory, bool? createToJson, bool? checked, bool? anyMap, bool? disallowUnrecognizedKeys});
}

class JsonKey {
  external const JsonKey({String? name, Object? defaultValue, bool? includeFromJson, bool? includeToJson,
      bool? includeIfNull, bool? required, Object? fromJson, Object? toJson, Object? unknownEnumValue});
}

class JsonValue {
  external const JsonValue(Object? value);
}

class JsonEnum {
  external const JsonEnum({bool? alwaysCreate, Object? fieldRename, String? valueField});
}

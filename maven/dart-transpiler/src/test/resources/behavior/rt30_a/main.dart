import 'dart:convert';
void main() {
  dynamic d = 3;
  Object? o = null;
  print(d is dynamic); print(o is Object?); print(o is Object);
  print((d as dynamic) + 1); print(o as Object?);
  Map<String, dynamic> m = jsonDecode('{"a":1,"b":[1,2.5,"x"],"c":{"d":null}}');
  print(m['a'] is int); print(m['b'] is List<dynamic>); print(m['b'] is List<Object?>);
  print(m['c'] is Map<String, dynamic>);
  final List<dynamic> l = m['b'];
  print(l.map((e) => e.runtimeType).toList());
  print(m.runtimeType == Map<String, dynamic>().runtimeType);
  print(<String, dynamic>{}.runtimeType); print(<dynamic>[].runtimeType); print(<Object?>[].runtimeType);
  print(dynamic); print(Object); print(Type); print(Pattern);
  print(<Type>[int, String, dynamic].runtimeType);
  print([int, String]); print({int: 1, dynamic: 2});
  Pattern p = 'a'; print(p is String); p = RegExp('b'); print(p is RegExp);
  print([p, 'x'].runtimeType); print(['x', RegExp('y')].runtimeType);
  print(jsonEncode(m));
}

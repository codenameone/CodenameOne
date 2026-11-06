import 'dart:async';
import 'dart:collection';

Future<int> twice(int v) async => v * 2;

// No declared return type: Dart infers Future<String> from the async body.
label(int v) async => 'L$v';

Future<List<int>> classicFor(int n) async {
  return [for (var i = 0; i < n; i++) await twice(i)];
}

Future<List<Object>> untypedCallee() async {
  return [for (var i = 0; i < 2; i++) await label(i)];
}

Future<void> nullableResume() async {
  final m = <String, int>{'a': 1};
  final removed = await Future.sync(() => m.remove('a'));
  final missing = await Future.sync(() => m.remove('zz'));
  print('removed $removed missing $missing ${removed == null} ${missing == null}');
  final tree = SplayTreeMap<int, String>();
  final first = await Future.value(tree.firstKey());
  print('first ${first ?? -1}');
  final c = Completer<bool?>();
  c.complete(null);
  final b = await c.future;
  print('b ${b == null} ${b ?? false}');
  final listed = [await c.future, true];
  print(listed.runtimeType);
}

void main() async {
  print(await classicFor(4));
  final s = await untypedCallee();
  print('$s ${s.runtimeType}');
  final mixed = [[1], ['a']];
  print(mixed.runtimeType);
  print(mixed[0].runtimeType);
  final Object first = mixed[0];
  print(first == mixed[0]);
  print(mixed.map((e) => e.toString()).join('|'));
  final nested = [[1, 2], [3.5]];
  print(nested.runtimeType);
  await nullableResume();
}

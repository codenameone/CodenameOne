Future<int> later(int v) => Future<int>.delayed(Duration.zero, () => v);

Future<List<int>?> maybe(bool give) async => give ? [7, 8] : null;

Future<Map<String, int>?> maybeMap(bool give) async => give ? {'m': 1} : null;

Future<void> maps(List<int> xs, bool flag) async {
  final m = {for (final x in xs) if (x.isOdd) 'o$x': await later(x * 2)};
  print('${m is Map<String, int>} $m');
  final n = {'a': 1, if (flag) 'b': await later(2) else 'c': 3, ...?(await maybeMap(flag))};
  print('${n is Map<String, int>} $n ${n.length}');
  final e = {for (final x in xs) if (await later(x) > 100) 'big$x': x};
  print('${e.length} $e');
}

Future<void> nullSpreads(bool flag) async {
  final a = [0, ...?(await maybe(flag)), if (flag) await later(9)];
  print('${a.runtimeType} $a');
  final b = [for (var i = 0; i < 2; i++) ...?(await maybe(i == 1))];
  print('$b ${b.length}');
}

String describe(Object o) {
  switch (o) {
    case int v when v > 10:
    case int v when v < -10:
      return 'big int $v';
    case (int a, int b):
    case (int b, int a) when a == b:
      return 'pair ${a - b}';
    case String s when s.length > 2:
    case String s when s.startsWith('z'):
      return 'text ${s.length}';
    default:
      return 'other';
  }
}

Future<String> describeLater(Object o) async {
  switch (o) {
    case int v when v > 10:
    case int v when v < -10:
      return 'big int ${await later(v)}';
    case [int first, ...]:
    case (int first, _):
      final w = await later(first);
      return 'first $w';
    default:
      return 'other';
  }
}

Future<void> main() async {
  await maps([1, 2, 3, 200], true);
  await maps([4], false);
  await nullSpreads(true);
  await nullSpreads(false);
  for (final o in [42, -50, 3, (5, 2), (4, 4), 'abc', 'zz', 'q', 1.5]) {
    print(describe(o));
  }
  for (final o in [42, -50, 3, [6, 1], (9, 'z'), 'q']) {
    print(await describeLater(o));
  }
}

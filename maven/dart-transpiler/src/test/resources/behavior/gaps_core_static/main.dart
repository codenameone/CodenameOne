class Config {
  static const String env = String.fromEnvironment('ENV', defaultValue: 'stage');
  static const bool flag = bool.fromEnvironment('FLAG', defaultValue: true);
  static const int level = int.fromEnvironment('LEVEL', defaultValue: 3);
}

void main() {
  print(Config.env);
  print(Config.flag);
  print(Config.level);
  final a = List.of([1, 2, 3]);
  final b = List.from(['x', 'y']);
  final c = List.generate(3, (i) => i * i);
  final d = List.filled(2, 'z');
  final e = List.empty();
  final f = List.unmodifiable([4, 5]);
  print('$a $b $c $d $e $f ${e.length}');
  final m1 = Map.fromEntries([MapEntry('k', 1), MapEntry('j', 2)]);
  final m2 = Map.from({'a': 1});
  final m3 = Map.of({'b': 2});
  final m4 = Map.fromIterable([1, 2], key: (x) => 'n$x', value: (x) => x * 10);
  final m5 = Map.unmodifiable({'u': 0});
  print('$m1 $m2 $m3 $m4 $m5');
  final s1 = Set.of([1, 1, 2]);
  final s2 = Set.from(['p', 'p']);
  print('$s1 $s2');
  print(String.fromCharCode(65) + String.fromCharCodes([66, 67]));
  print(Iterable.empty().isEmpty);
  print(Iterable.generate(3, (i) => i + 1).toList());
  print('${DateTime.monday} ${DateTime.sunday} ${DateTime.january} ${DateTime.december}');
  print(DateTime(2024, 1, 1).weekday == DateTime.monday);
  final enc = Uri.encodeComponent('a b&c');
  print(enc);
  print(Uri.decodeComponent(enc));
  print(Uri.encodeQueryComponent('a b&c'));
  print(Uri.decodeQueryComponent('a+b%26c'));
  print(RangeError.checkValidRange(0, 2, 5));
  print(RangeError.checkNotNegative(4));
  print(RangeError.checkValueInInterval(3, 1, 5));
  RangeError.checkValidIndex(1, [1, 2]);
  try {
    RangeError.checkNotNegative(-1, 'n');
  } on RangeError catch (err) {
    print('caught range');
  }
  try {
    throw RangeError.range(9, 0, 5);
  } on RangeError catch (_) {
    print('caught range2');
  }
  try {
    throw RangeError.value(9);
  } on RangeError catch (_) {
    print('caught value');
  }
  try {
    throw RangeError.index(9, [1]);
  } on RangeError catch (_) {
    print('caught index');
  }
  print(ArgumentError.checkNotNull('ok'));
  try {
    throw ArgumentError.value(3, 'x');
  } on ArgumentError catch (_) {
    print('caught arg');
  }
  try {
    throw ArgumentError.notNull('y');
  } on ArgumentError catch (_) {
    print('caught notNull');
  }
  try {
    throw AssertionError('bad');
  } on AssertionError catch (err) {
    print('assert ${err.message}');
  }
  final o = Object();
  print(identityHashCode(o) == identityHashCode(o));
}

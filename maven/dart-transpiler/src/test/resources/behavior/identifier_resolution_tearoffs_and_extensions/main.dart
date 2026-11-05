// Sweep of identifier-resolution gaps: static tear-offs with as parameter
// defaults, generic function-typed fields called directly,
// bare getters shared by several extensions, and generic extension closures.

typedef Callback = void Function();

class Config {
  final Callback onEvent;
  final Future<void> Function() before;
  Config({this.onEvent = _noOp, this.before = _noOpFuture});
  static void _noOp() {}
  static Future<void> _noOpFuture() => Future.value();
}

class Sorted<K> {
  final Comparator<K> _comparator;
  final List<K> _items = [];
  Sorted(this._comparator);
  void add(K k) {
    var i = 0;
    while (i < _items.length && _comparator(_items[i], k) < 0) {
      i++;
    }
    _items.insert(i, k);
  }
  List<K> get items => _items;
}

extension NumStats on Iterable<num> {
  num? get lowOrNull {
    num? r;
    for (final v in this) {
      if (r == null || v < r) r = v;
    }
    return r;
  }
  num get low => lowOrNull ?? (throw StateError('No element'));
}

extension IntStats on Iterable<int> {
  int? get lowOrNull {
    int? r;
    for (final v in this) {
      if (r == null || v < r) r = v;
    }
    return r;
  }
  int get low => lowOrNull ?? (throw StateError('No element'));
}

extension Picks<E> on List<E> {
  List<E> keep(bool Function(E) test) => [for (final e in this) if (test(e)) e];
}

Future<void> main() async {
  final c = Config();
  c.onEvent();
  await c.before();
  print('config ok');

  final s = Sorted<int>((a, b) => a.compareTo(b));
  for (final v in [5, 1, 4, 2]) {
    s.add(v);
  }
  print(s.items);

  print(<int>[3, 1, 2].low);
  print(<num>[2.5, 1.5].low);
  print(<int>[].lowOrNull);

  print(<int>[1, 2, 3, 4, 5].keep((x) => x.isOdd));

  final dyn = <dynamic>[1, 2, 3];
  final ints = dyn.cast<int>();
  print(ints.first + 10);
}

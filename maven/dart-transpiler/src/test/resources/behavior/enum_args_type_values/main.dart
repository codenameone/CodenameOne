// Enhanced enums that pass arguments to their constructor, `is` tests applied to a
// Type value (a type parameter or a type literal), and an inherited setter assigned
// from a subclass that also implements a getter-only interface.
enum Phase {
  start('begins'),
  middle('carries on', 2),
  finish('ends', 3);

  const Phase(this.label, [this.weight = 1]);

  final String label;
  final int weight;

  @override
  String toString() => '$label/$weight';
}

enum Named {
  alpha(code: 'A'),
  beta(code: 'B', rank: 5);

  const Named({required this.code, this.rank = 0});

  final String code;
  final int rank;
}

class Box<V> {
  String kind() {
    if (V is String) return 'type is a String value';
    if (V is Type) return 'type literal is a Type';
    return 'other';
  }

  bool objectTest() => V is Object;
}

abstract class HasPosition {
  int get position;
}

class Cursor {
  int _p = 0;
  int get position => _p;
  set position(int v) {
    if (v < 0) throw ArgumentError('negative');
    _p = v;
  }
}

class Tracker extends Cursor implements HasPosition {
  set jumpTo(int target) {
    position = target;
  }

  void advance() {
    position = position + 1;
    position += 2;
  }
}

int looseCompare(Object? a, Object? b) => (a as Comparable).compareTo(b);

E smallest<E>(List<E> items, [int Function(E, E)? compare]) {
  compare ??= looseCompare;
  var best = items.first;
  for (final x in items) {
    if (compare(x, best) < 0) best = x;
  }
  return best;
}

void main() {
  print('smallest ${smallest(<int>[4, 2, 9])} ${smallest(<String>['b', 'a'], (x, y) => y.compareTo(x))}');
  for (final p in Phase.values) {
    print('${p.name} ${p.index} $p ${p.label} ${p.weight}');
  }
  for (final n in Named.values) {
    print('${n.name} ${n.code} ${n.rank}');
  }
  print(Box<String>().kind());
  print(Box<int>().objectTest());
  final t = Tracker();
  t.jumpTo = 4;
  t.advance();
  print('position ${t.position}');
  HasPosition h = t;
  print('through interface ${h.position}');
}

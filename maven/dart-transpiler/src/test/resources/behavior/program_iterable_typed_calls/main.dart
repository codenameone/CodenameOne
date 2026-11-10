import 'dart:collection';

// A program Iterable that declares fold itself, and one that inherits every
// lookup from IterableBase; both are called with explicit type arguments and
// untyped closure parameters, and through the by-test lookups.
class Bag<E> extends IterableBase<E> {
  final List<E> _items;
  Bag(this._items);
  @override
  Iterator<E> get iterator => _items.iterator;
  @override
  T fold<T>(T initial, T Function(T, E) combine) {
    var acc = initial;
    for (final e in _items) {
      acc = combine(acc, e);
    }
    return acc;
  }

  R pair<R, S>(R first, S second, R Function(R, S) join) => join(first, second);
}

class Plain<E> extends IterableBase<E> {
  final List<E> _items;
  Plain(this._items);
  @override
  Iterator<E> get iterator => _items.iterator;
}

void main() {
  final b = Bag<int>([1, 2, 3, 4]);
  print(b.fold<int>(10, (a, e) => a + e));
  print(b.fold<double>(0.5, (a, e) => a * 2 + e));
  print(b.fold<String>('', (a, e) => a + e.toString()));
  print(b.fold<List<int>>(<int>[], (a, e) => a..add(e * e)));
  print(b.pair<String, int>('n', 7, (a, s) => '$a=${s + 1}'));
  try {
    b.single;
  } on StateError catch (e) {
    print('bag single: ${e.message}');
  }
  final p = Plain<int>([5, 6, 7]);
  print(Plain<int>([42]).single);
  print(p.firstWhere((x) => x > 5));
  print(p.lastWhere((x) => x < 7));
  print(p.singleWhere((x) => x == 6));
  print(p.firstWhere((x) => x > 9, orElse: () => -1));
  print(p.lastWhere((x) => x > 9, orElse: () => -2));
  print(p.singleWhere((x) => x > 9, orElse: () => -3));
  try {
    p.singleWhere((x) => x > 5);
  } on StateError catch (e) {
    print('plain singleWhere: ${e.message}');
  }
  try {
    Plain<int>([]).single;
  } on StateError catch (e) {
    print('plain single: ${e.message}');
  }
  print(p.fold<int>(0, (a, e) => a - e));
  print(p.fold<List<int>>(<int>[], (a, e) => a..insert(0, e)));
  print(p.fold<String>('>', (a, e) => '$a$e'));
}

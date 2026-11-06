List<R> applyAll<E, R>(List<E> items, R Function(E) f) => [for (final i in items) f(i)];

E maxBy<E extends Comparable<E>>(List<E> items, int Function(E, E) cmp) {
  var best = items.first;
  for (final i in items) {
    if (cmp(i, best) > 0) best = i;
  }
  return best;
}

T firstWhere2<T>(Iterable<T> it, bool Function(T) test, T orElse) {
  for (final v in it) {
    if (test(v)) return v;
  }
  return orElse;
}

class Box<T extends Comparable<T>> {
  final List<T> items = [];
  void add(T t) => items.add(t);
  T best() => maxBy<T>(items, (a, b) => a.compareTo(b));
  List<T> sorted() {
    final copy = [...items];
    copy.sort((a, b) => b.compareTo(a));
    return copy;
  }
}

void main() {
  print(applyAll([1, 2, 3], (int x) => x * 2));
  print(applyAll(['a', 'bb'], (String s) => s.length));
  print(maxBy([3, 9, 4], (a, b) => a.compareTo(b)));
  print(maxBy(['pear', 'apple', 'zoo'], (a, b) => a.length.compareTo(b.length)));
  final b = Box<String>()..add('m')..add('z')..add('c');
  print(b.best());
  print(b.sorted());
}

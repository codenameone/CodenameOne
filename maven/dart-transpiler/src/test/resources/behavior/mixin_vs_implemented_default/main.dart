import 'dart:collection';

// A mixin member that an implemented type also has a body for, and a
// mixin member that a dart:collection mixin before it inherits.
class Wrapper<E> {
  final List<E> inner;
  Wrapper(this.inner);
  String clear() => 'wrapper clear';
  Iterable<T> expand<T>(Iterable<T> Function(E) f) => inner.expand(f);
}

mixin ReadOnly<E> {
  String clear() => 'read-only clear';
}

class ReadOnlyWrapper<E> extends Wrapper<E> with ReadOnly<E> {
  ReadOnlyWrapper(List<E> l) : super(l);
}

class Empty<E> with ReadOnly<E> implements ReadOnlyWrapper<E> {
  List<E> get inner => <E>[];
  Iterable<T> expand<T>(Iterable<T> Function(E) f) => const [];
}

mixin Viewing<E> {
  Set<E> toSet() => {};
}

class Numbers with ListMixin<int>, Viewing<int> {
  final List<int> _d = [3, 1, 3];
  int get length => _d.length;
  set length(int v) => _d.length = v;
  int operator [](int i) => _d[i];
  void operator []=(int i, int v) => _d[i] = v;
}

void main() {
  print(ReadOnlyWrapper([1, 2]).clear());
  print(Empty<int>().clear());
  ReadOnlyWrapper<int> w = ReadOnlyWrapper<int>([1, 2]);
  print(w.expand((int x) => <int>[x, x * 10].map((int y) => y + 1)).toList());
  print(Numbers().toSet());
  print(Numbers().toList());
}

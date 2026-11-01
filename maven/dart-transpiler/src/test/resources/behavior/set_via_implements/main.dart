import 'dart:collection';

abstract class _WrapBase<E> implements Iterable<E> {
  Iterable<E> get _inner;
  @override
  bool any(bool Function(E) test) => _inner.any(test);
  @override
  Iterable<T> cast<T>() => _inner.cast<T>();
  @override
  bool contains(Object? element) => _inner.contains(element);
  @override
  E elementAt(int index) => _inner.elementAt(index);
  @override
  bool every(bool Function(E) test) => _inner.every(test);
  @override
  Iterable<T> expand<T>(Iterable<T> Function(E) f) => _inner.expand(f);
  @override
  E get first => _inner.first;
  @override
  E firstWhere(bool Function(E) test, {E Function()? orElse}) =>
      _inner.firstWhere(test, orElse: orElse);
  @override
  T fold<T>(T initialValue, T Function(T, E) combine) =>
      _inner.fold(initialValue, combine);
  @override
  Iterable<E> followedBy(Iterable<E> other) => _inner.followedBy(other);
  @override
  void forEach(void Function(E) f) => _inner.forEach(f);
  @override
  bool get isEmpty => _inner.isEmpty;
  @override
  bool get isNotEmpty => _inner.isNotEmpty;
  @override
  Iterator<E> get iterator => _inner.iterator;
  @override
  String join([String separator = '']) => _inner.join(separator);
  @override
  E get last => _inner.last;
  @override
  E lastWhere(bool Function(E) test, {E Function()? orElse}) =>
      _inner.lastWhere(test, orElse: orElse);
  @override
  int get length => _inner.length;
  @override
  Iterable<T> map<T>(T Function(E) f) => _inner.map(f);
  @override
  E reduce(E Function(E, E) combine) => _inner.reduce(combine);
  @override
  E get single => _inner.single;
  @override
  E singleWhere(bool Function(E) test, {E Function()? orElse}) =>
      _inner.singleWhere(test, orElse: orElse);
  @override
  Iterable<E> skip(int n) => _inner.skip(n);
  @override
  Iterable<E> skipWhile(bool Function(E) test) => _inner.skipWhile(test);
  @override
  Iterable<E> take(int n) => _inner.take(n);
  @override
  Iterable<E> takeWhile(bool Function(E) test) => _inner.takeWhile(test);
  @override
  List<E> toList({bool growable = true}) => _inner.toList(growable: growable);
  @override
  Set<E> toSet() => _inner.toSet();
  @override
  Iterable<E> where(bool Function(E) test) => _inner.where(test);
  @override
  Iterable<T> whereType<T>() => _inner.whereType<T>();
  @override
  String toString() => _inner.toString();
}

class WrapIterable<E> extends _WrapBase<E> {
  @override
  final Iterable<E> _inner;
  WrapIterable(this._inner);
}

class WrapSet<E> extends _WrapBase<E> implements Set<E> {
  @override
  final Set<E> _inner;
  WrapSet(this._inner);
  @override
  bool add(E value) => _inner.add(value);
  @override
  void addAll(Iterable<E> elements) => _inner.addAll(elements);
  @override
  void clear() => _inner.clear();
  @override
  Set<T> cast<T>() => _inner.cast<T>();
  @override
  bool containsAll(Iterable<Object?> other) => _inner.containsAll(other);
  @override
  Set<E> difference(Set<Object?> other) => _inner.difference(other);
  @override
  Set<E> intersection(Set<Object?> other) => _inner.intersection(other);
  @override
  E? lookup(Object? element) => _inner.lookup(element);
  @override
  bool remove(Object? value) => _inner.remove(value);
  @override
  void removeAll(Iterable<Object?> elements) => _inner.removeAll(elements);
  @override
  void removeWhere(bool Function(E) test) => _inner.removeWhere(test);
  @override
  void retainAll(Iterable<Object?> elements) => _inner.retainAll(elements);
  @override
  void retainWhere(bool Function(E) test) => _inner.retainWhere(test);
  @override
  Set<E> union(Set<E> other) => _inner.union(other);
  @override
  Set<E> toSet() => WrapSet<E>(_inner.toSet());
}

abstract mixin class ReadOnlySetMixin<E> implements Set<E> {
  static Never _fail() => throw UnsupportedError('read-only set');
  @override
  bool add(E value) => _fail();
  @override
  void addAll(Iterable<E> elements) => _fail();
  @override
  void clear() => _fail();
  @override
  bool remove(Object? value) => _fail();
  @override
  void removeAll(Iterable<Object?> elements) => _fail();
  @override
  void removeWhere(bool Function(E) test) => _fail();
  @override
  void retainAll(Iterable<Object?> elements) => _fail();
  @override
  void retainWhere(bool Function(E) test) => _fail();
}

class ReadOnlySetView<E> extends WrapSet<E> with ReadOnlySetMixin<E> {
  ReadOnlySetView(super.base);
}

class KeysOf<E> extends _WrapBase<E> with ReadOnlySetMixin<E> {
  final Map<E, dynamic> _map;
  KeysOf(this._map);
  @override
  Iterable<E> get _inner => _map.keys;
  @override
  Set<T> cast<T>() {
    if (this is KeysOf<T>) return this as KeysOf<T>;
    return _inner.cast<T>().toSet();
  }
  @override
  bool containsAll(Iterable<Object?> other) => other.every(contains);
  @override
  Set<E> difference(Set<Object?> other) =>
      where((e) => !other.contains(e)).toSet();
  @override
  Set<E> intersection(Set<Object?> other) => where(other.contains).toSet();
  @override
  E lookup(Object? element) =>
      throw UnsupportedError('no lookup');
  @override
  Set<E> union(Set<E> other) => toSet()..addAll(other);
  @override
  String toString() => SetBase.setToString(this);
}

void show(String label, Set<int> s) {
  print('$label ${s.length} ${s.contains(2)} ${s.toList()}');
}

void main() {
  final w = WrapSet<int>({1, 2, 3});
  show('wrap', w);
  final copy = w.toSet();
  copy.add(9);
  show('copy', copy);
  print(w.where((e) => e.isOdd).toList());
  final ro = ReadOnlySetView<int>({2, 4});
  show('ro', ro);
  try {
    ro.add(5);
  } on UnsupportedError catch (e) {
    print('ro add: ${e.message}');
  }
  final keys = KeysOf<int>({1: 'a', 2: 'b'});
  show('keys', keys);
  print(keys);
  print(keys.cast<int>().length);
  print(keys.union({7}).toList());
  final Set<int> asSet = keys;
  print(asSet.difference({1}).toList());
}

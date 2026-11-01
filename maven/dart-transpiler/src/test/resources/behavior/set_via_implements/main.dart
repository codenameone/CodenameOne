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
  noneMain();
}

// A set wrapper, a read-only mixin, a view applying it, and an always-empty set
// that extends IterableBase, applies the mixin and implements the view.
class Fwd<E> extends Iterable<E> implements Set<E> {
  final Set<E> _inner;
  Fwd(this._inner);
  @override
  Iterator<E> get iterator => _inner.iterator;
  @override
  int get length => _inner.length;
  @override
  bool contains(Object? o) => _inner.contains(o);
  @override
  bool add(E v) => _inner.add(v);
  @override
  void addAll(Iterable<E> v) => _inner.addAll(v);
  @override
  Set<T> cast<T>() => Fwd<T>(_inner.cast<T>());
  @override
  void clear() => _inner.clear();
  @override
  bool containsAll(Iterable<Object?> o) => _inner.containsAll(o);
  @override
  Set<E> difference(Set<Object?> o) => _inner.difference(o);
  @override
  Set<E> intersection(Set<Object?> o) => _inner.intersection(o);
  @override
  E? lookup(Object? o) => _inner.lookup(o);
  @override
  bool remove(Object? v) => _inner.remove(v);
  @override
  void removeAll(Iterable<Object?> v) => _inner.removeAll(v);
  @override
  void removeWhere(bool Function(E) t) => _inner.removeWhere(t);
  @override
  void retainAll(Iterable<Object?> v) => _inner.retainAll(v);
  @override
  void retainWhere(bool Function(E) t) => _inner.retainWhere(t);
  @override
  Set<E> union(Set<E> o) => _inner.union(o);
  @override
  Set<E> toSet() => _inner.toSet();
  // Iterable members the wrapper forwards explicitly. NoneSet below implements Fwd
  // but extends IterableBase, so it takes IterableBase's versions of these.
  @override
  Iterable<E> where(bool Function(E) t) => _inner.where(t);
  @override
  bool any(bool Function(E) t) => _inner.any(t);
  @override
  bool every(bool Function(E) t) => _inner.every(t);
  @override
  Iterable<T> map<T>(T Function(E) f) => _inner.map(f);
  @override
  Iterable<E> skip(int n) => _inner.skip(n);
  @override
  Iterable<E> take(int n) => _inner.take(n);
  @override
  String join([String separator = '']) => _inner.join(separator);
  @override
  T fold<T>(T initial, T Function(T, E) combine) => _inner.fold(initial, combine);
  @override
  void forEach(void Function(E) f) => _inner.forEach(f);
  @override
  List<E> toList({bool growable = true}) => _inner.toList(growable: growable);
}

mixin NoWrites<E> implements Set<E> {
  static Never _no() => throw UnsupportedError('read-only');
  @override
  bool add(E v) => _no();
  @override
  void addAll(Iterable<E> v) => _no();
  @override
  bool remove(Object? v) => _no();
  @override
  void removeAll(Iterable<Object?> v) => _no();
  @override
  void retainAll(Iterable<Object?> v) => _no();
  @override
  void removeWhere(bool Function(E) t) => _no();
  @override
  void retainWhere(bool Function(E) t) => _no();
  @override
  void clear() => _no();
}

class FwdView<E> extends Fwd<E> with NoWrites<E> {
  FwdView(Set<E> s) : super(s);
}

class NoneSet<E> extends IterableBase<E> with NoWrites<E> implements FwdView<E> {
  const NoneSet();
  @override
  Set<E> get _inner => <E>{};
  @override
  Iterator<E> get iterator => <E>[].iterator;
  @override
  int get length => 0;
  @override
  NoneSet<T> cast<T>() => NoneSet<T>();
  @override
  bool contains(Object? o) => false;
  @override
  bool containsAll(Iterable<Object?> o) => o.isEmpty;
  @override
  E? lookup(Object? o) => null;
  @override
  Set<E> toSet() => <E>{};
  @override
  Set<E> union(Set<E> o) => Set.of(o);
  @override
  Set<E> intersection(Set<Object?> o) => <E>{};
  @override
  Set<E> difference(Set<Object?> o) => <E>{};
}

void noneMain() {
  const NoneSet<int> n = NoneSet<int>();
  print('none ${n.length} ${n.contains(1)} ${n.isEmpty}');
  final Set<num> c = n.cast<num>();
  print('none cast ${c.length} ${c.isEmpty}');
  try {
    n.removeAll([1]);
  } on UnsupportedError catch (e) {
    print('none removeAll: ${e.message}');
  }
  try {
    n.add(3);
  } on UnsupportedError catch (e) {
    print('none add: ${e.message}');
  }
  print(n.union({4, 5}).toList());
  print('none iter ${n.where((x) => x > 0).length} ${n.any((x) => true)} '
      '${n.every((x) => false)} ${n.map((x) => x * 2).toList()} '
      '${n.skip(1).isEmpty} ${n.take(2).length} [${n.join(',')}] '
      '${n.fold<int>(7, (a, b) => a + b)} ${n.toList()}');
  try {
    n.single;
  } on StateError catch (e) {
    print('none single: ${e.message}');
  }
  var seen = 0;
  n.forEach((x) => seen++);
  final FwdView<int> w = FwdView<int>({3, 4, 5});
  print('view iter $seen ${w.where((x) => x > 3).toList()} ${w.any((x) => x == 4)} '
      '${w.every((x) => x > 2)} ${w.map((x) => x + 1).toList()} '
      '${w.skip(1).toList()} ${w.take(1).toList()} ${w.join('-')} '
      '${w.fold<int>(0, (a, b) => a + b)} ${w.toList()} ${FwdView<int>({9}).single}');
  final FwdView<int> v = FwdView<int>({1, 2});
  print('view ${v.length} ${v.cast<num>().length}');
  try {
    v.clear();
  } on UnsupportedError catch (e) {
    print('view clear: ${e.message}');
  }
}

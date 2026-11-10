import 'dart:collection';

class FoldedMap<C, K, V> implements Map<K, V> {
  final C Function(K) _fold;
  final _base = <C, MapEntry<K, V>>{};
  FoldedMap(this._fold);

  @override
  V? operator [](Object? key) => _base[_fold(key as K)]?.value;
  @override
  void operator []=(K key, V value) {
    _base[_fold(key)] = MapEntry(key, value);
  }

  @override
  void addAll(Map<K, V> other) => other.forEach((k, v) => this[k] = v);
  @override
  void addEntries(Iterable<MapEntry<K, V>> entries) => _base.addEntries(
      entries.map((e) => MapEntry(_fold(e.key), MapEntry(e.key, e.value))));
  @override
  Map<K2, V2> cast<K2, V2>() => _base.map((_, e) => MapEntry(e.key as K2, e.value as V2));
  @override
  void clear() => _base.clear();
  @override
  bool containsKey(Object? key) => _base.containsKey(_fold(key as K));
  @override
  bool containsValue(Object? value) => _base.values.any((e) => e.value == value);
  @override
  Iterable<MapEntry<K, V>> get entries => _base.entries.map((e) => MapEntry(e.value.key, e.value.value));
  @override
  void forEach(void Function(K, V) f) => _base.forEach((_, e) => f(e.key, e.value));
  @override
  bool get isEmpty => _base.isEmpty;
  @override
  bool get isNotEmpty => _base.isNotEmpty;
  @override
  Iterable<K> get keys => _base.values.map((e) => e.key);
  @override
  int get length => _base.length;
  @override
  Map<K2, V2> map<K2, V2>(MapEntry<K2, V2> Function(K, V) t) =>
      _base.map((_, e) => t(e.key, e.value));
  @override
  V putIfAbsent(K key, V Function() ifAbsent) =>
      _base.putIfAbsent(_fold(key), () => MapEntry(key, ifAbsent())).value;
  @override
  V? remove(Object? key) => _base.remove(_fold(key as K))?.value;
  @override
  void removeWhere(bool Function(K, V) test) => _base.removeWhere((_, e) => test(e.key, e.value));
  @override
  V update(K key, V Function(V) update, {V Function()? ifAbsent}) => _base
      .update(_fold(key), (e) => MapEntry(key, update(e.value)),
          ifAbsent: ifAbsent == null ? null : () => MapEntry(key, ifAbsent()))
      .value;
  @override
  void updateAll(V Function(K, V) update) =>
      _base.updateAll((_, e) => MapEntry(e.key, update(e.key, e.value)));
  @override
  Iterable<V> get values => _base.values.map((e) => e.value);
  @override
  String toString() => MapBase.mapToString(this);
}

class NoCaseMap<V> extends FoldedMap<String, String, V> {
  NoCaseMap() : super((k) => k.toLowerCase());
}

void main() {
  final m = NoCaseMap<int>();
  m['One'] = 1;
  m['ONE'] = 11;
  m.addEntries([MapEntry('Two', 2), MapEntry('three', 3)]);
  print(m.values.toList());
  print(m.keys.join(','));
  print(m.update('two', (v) => v * 10));
  print(m.update('Four', (v) => v, ifAbsent: () => 4));
  print(m['FOUR']);
  print(m.values.fold<int>(0, (a, b) => a + b));
  final FoldedMap<String, String, int> f = m;
  print(f.values.length);
  print(m);
  final Map<String, Object> loose = {};
  loose.addEntries([MapEntry('n', 1), MapEntry('s', 'x')]);
  print(loose);
}

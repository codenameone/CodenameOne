class Keyed<C, K, V> {
  final C Function(K) _canon;
  final _base = <C, MapEntry<K, V>>{};
  Keyed(this._canon);

  void operator []=(K key, V value) {
    _base[_canon(key)] = MapEntry(key, value);
  }

  V? operator [](K key) => _base[_canon(key)]?.value;

  V putIfAbsent(K key, V Function() ifAbsent) =>
      _base.putIfAbsent(_canon(key), () => MapEntry(key, ifAbsent())).value;

  void forEach(void Function(K key, V value) f) {
    _base.forEach((key, pair) => f(pair.key, pair.value));
  }

  void removeWhere(bool Function(K key, V value) test) =>
      _base.removeWhere((_, pair) => test(pair.key, pair.value));

  Iterable<MapEntry<K, V>> get entries =>
      _base.entries.map((e) => MapEntry(e.value.key, e.value.value));

  Iterable<K> get keys => _base.values.map((pair) => pair.key);
  int get length => _base.length;
}

void main() {
  final m = Keyed<String, String, int>((k) => k.toLowerCase());
  m['Apple'] = 1;
  m['APPLE'] = 2;
  m['Pear'] = 3;
  print(m['apple']);
  print(m.putIfAbsent('PLUM', () => 9));
  print(m.putIfAbsent('pear', () => 0));
  m.forEach((k, v) => print('$k=$v'));
  m.removeWhere((k, v) => v > 5);
  print(m.keys.toList());
  print(m.entries.map((e) => '${e.key}:${e.value}').join(','));
  print(m.length);
}

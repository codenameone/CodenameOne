import 'dart:async';
import 'dart:collection';

const _tokenKey = #_tokenKey;

String describe() {
  final v = Zone.current[_tokenKey];
  return v == null ? 'none' : 'token=$v';
}

class PairView<K, V> extends UnmodifiableMapBase<K, V> {
  final Map<K, V> _a;
  final Map<K, V> _b;
  PairView(this._a, this._b);

  @override
  V? operator [](Object? key) => _a.containsKey(key) ? _a[key] : _b[key];

  @override
  Iterable<K> get keys => <K>{..._a.keys, ..._b.keys};
}

void main() {
  print(describe());
  final r = runZoned(() {
    print(describe());
    final inner = runZoned(() => describe(), zoneValues: {#other: 1});
    print('inner $inner');
    return 7;
  }, zoneValues: {_tokenKey: 'abc'});
  print('result $r');
  print(describe());

  final view = PairView<String, int>({'a': 1, 'b': 2}, {'b': 20, 'c': 3});
  print(view['a']);
  print(view['b']);
  print(view['c']);
  print(view['zz']);
  print(view.length);
  print(view.containsKey('c'));
  print(view.keys.toList());
  try {
    view['d'] = 4;
  } on UnsupportedError catch (e) {
    print('set: ${e.message}');
  }
  try {
    view.remove('a');
  } on UnsupportedError catch (e) {
    print('remove: ${e.message}');
  }
  Map<String, int> asMap = view;
  print(asMap.length);
}

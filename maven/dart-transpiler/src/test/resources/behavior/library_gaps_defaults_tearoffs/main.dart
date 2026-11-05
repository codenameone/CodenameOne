import 'dart:collection';
import 'dart:convert';

class _Registry {
  static final Map<String, Set<String>> pending = {};
}

class Registry {
  void mark(String key, String v) {
    _Registry.pending[key] = <String>{};
    _Registry.pending[key]!.add(v);
  }

  int count(String key) => _Registry.pending[key]?.length ?? 0;
  void clear(String key) => _Registry.pending.remove(key);
}

class Box<T> {
  final T value;
  Box(this.value);
  bool get big => (value as int) > 5;
  String describe() => 'box($value)';
}

class Ring<E> extends Object with ListMixin<E> {
  final List<E> _items = [];
  @override
  int get length => _items.length;
  @override
  set length(int n) => _items.length = n;
  @override
  E operator [](int i) => _items[i];
  @override
  void operator []=(int i, E v) => _items[i] = v;
  @override
  void add(E v) => _items.add(v);
}

String scan<T>(Ring<Box<T>> ring) {
  final first = ring.elementAt(0);
  return '${first.big} ${ring[1].describe()}';
}


void main() {
  final decoded = ['{"a":1}', '[2,3]'].map(jsonDecode).toList();
  print(decoded);
  final enc = [1, 'x'].map(jsonEncode).join('|');
  print(enc);
  final reg = Registry();
  reg.mark('k', 'v');
  print(reg.count('k'));
  reg.clear('k');
  print(reg.count('k'));
  final ring = Ring<Box<int>>()..add(Box(9))..add(Box(2));
  print(scan(ring));
}

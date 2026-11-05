import 'dart:collection';
import 'dart:convert';
import 'dart:typed_data';

class Bits extends Object with ListMixin<bool> {
  final List<bool> _store = <bool>[];
  @override
  int get length => _store.length;
  @override
  set length(int n) {
    while (_store.length < n) {
      _store.add(false);
    }
    while (_store.length > n) {
      _store.removeLast();
    }
  }

  @override
  bool operator [](int i) => _store[i];
  @override
  void operator []=(int i, bool v) {
    _store[i] = v;
  }
}

void main() {
  final b = Bits();
  b.length = 4;
  b[1] = true;
  b[3] = true;
  print(b.toList());
  print(b.sublist(1, 3));
  print(b.length);
  print(b.contains(true));
  print(b.indexOf(true));
  print(b.where((x) => x).length);
  print(b.join(','));

  List<int> ints = <int>[5, 6, 7, 8];
  List<int> part = ints.sublist(1, 3);
  print(part);
  List<int> tail = ints.sublist(2);
  print(tail);

  double d = 0.0 / 0.0;
  print(d.isNaN);
  double inf = 1.0 / 0.0;
  print(inf.isInfinite);
  print(inf.isFinite);
  print(2.5.isNegative);
  double negZero = -0.0;
  print(negZero.isNegative);
  print(d.isNegative);
  int three = 3;
  print(three.isFinite);
  print(three.isNaN);
  print((-three).isNegative);

  final enc = Uri.encodeComponent('a b&c/d');
  print(enc);
  print(Uri.decodeComponent(enc));

  final f = Float64List(3);
  f[0] = 5;
  f[1] = 2.5;
  print(f[0] + f[1]);

  final bytes = utf8.encoder.convert('hi');
  print(bytes);
  print(utf8.decoder.convert(bytes));
  Codec<String, List<int>> c = utf8;
  print(c.encoder.convert('ok'));
  print(c.decoder.convert(<int>[111, 107]));
}

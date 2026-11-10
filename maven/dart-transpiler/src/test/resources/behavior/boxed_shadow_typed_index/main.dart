import 'dart:typed_data';

typedef Builder = String Function(String? child);

String wrap(Builder b, String? v) => b(v);

String outer(bool flag) {
  String? child;
  void set() {
    child = 'boxed';
  }

  if (flag) {
    set();
  }
  // The closure parameter shadows the captured (boxed) local.
  final r = wrap((String? child) => 'param:' + (child ?? 'none'), 'given');
  return '$r ${child ?? 'unset'}';
}

String plain() {
  // Same name as the boxed local above, but never captured here.
  String child;
  child = 'plain';
  return child;
}

void main() {
  print(outer(true));
  print(outer(false));
  print(plain());

  final b = Uint8List(4);
  b[0] = 250;
  b[0] += 10;
  b[1] ^= 0x0f;
  b[2] |= 3;
  b[3] = b[0] + 1;
  print(b);

  final w = Uint64List(2);
  w[0] = 5;
  w[0] += 7;
  w[1] = w[0] * 2;
  print('${w[0]} ${w[1]}');

  final f = Float64List(2);
  f[0] = 1.5;
  f[0] *= 3;
  f[1] = f[0] / 2;
  print('${f[0]} ${f[1]} ${f[0].isNaN}');
}

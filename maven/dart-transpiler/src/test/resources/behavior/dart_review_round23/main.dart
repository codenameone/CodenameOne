// Duration's operators (+ - * ~/ unary minus, the relational ones, ==), typed
// and through dynamic. int.parse reads an unsigned 0x literal up to
// 0xffffffffffffffff as two's-complement bits, but a '-' keeps the signed
// range. A list's reversed view throws when the list changes length mid-loop.
// Uri reads a port the way int.parse does, so a sign or 0x is accepted.

void durations() {
  final a = Duration(seconds: 90);
  final b = Duration(milliseconds: 1500);
  print(a + b);
  print(a - b);
  print(b - a);
  print(a * 2);
  print(b * 1.5);
  print(b * 0.3333);
  num n = 3;
  print(a * n);
  print(a ~/ 4);
  print(-a);
  print(-(a - b));
  print(a < b);
  print(a <= a);
  print(a > b);
  print(b >= a);
  print(a == Duration(minutes: 1, seconds: 30));
  print(a != b);
  var total = Duration.zero;
  for (final d in [a, b, Duration(microseconds: 7)]) {
    total += d;
  }
  print(total);
  total -= a;
  print(total);
  total *= 2;
  print(total);
  total ~/= 3;
  print(total);
  dynamic x = a;
  dynamic y = b;
  print(x + y);
  print(x - y);
  print(x * 2);
  print(x ~/ 3);
  print(-x);
  print(x < y);
  print(x >= y);
  try {
    print(a ~/ 0);
  } on UnsupportedError {
    print('~/ 0 is an UnsupportedError');
  }
}

void parse(String s) {
  print('$s -> ${int.tryParse(s)}');
}

void ints() {
  for (final s in [
    '0x7fffffffffffffff',
    '0x8000000000000000',
    '0xffffffffffffffff',
    '0XFFFFFFFFFFFFFFFF',
    '+0xffffffffffffffff',
    '0x00000000000000000fffffffffffffffe',
    ' 0xffffffffffffffff ',
    '0x10000000000000000',
    '-0x8000000000000000',
    '-0x8000000000000001',
    '-0xffffffffffffffff',
    '-0x1',
    '0x',
    '9223372036854775808',
    '-9223372036854775808',
    '\u0663',
    '1\u0663',
    '0x\u0663',
  ]) {
    parse(s);
  }
  print(int.parse('0xffffffffffffffff'));
  print(int.tryParse('8000000000000000', radix: 16));
  print(int.tryParse('-8000000000000000', radix: 16));
  try {
    int.parse('-0xffffffffffffffff');
  } on FormatException {
    print('-0xffffffffffffffff is a FormatException');
  }
}

void reversedViews() {
  final grow = [1, 2, 3];
  try {
    for (final v in grow.reversed) {
      print('saw $v');
      grow.add(v * 10);
    }
  } on ConcurrentModificationError {
    print('grow: ConcurrentModificationError');
  }
  final shrink = [1, 2, 3, 4];
  try {
    for (final v in shrink.reversed) {
      print('saw $v');
      shrink.removeLast();
    }
  } on ConcurrentModificationError {
    print('shrink: ConcurrentModificationError');
  }
  final same = [1, 2, 3];
  for (final v in same.reversed) {
    same[0] = v;
  }
  print(same);
  final view = same.reversed;
  same.add(9);
  print(view.toList());
}

void uris() {
  for (final s in [
    'http://a.com:+80/',
    'http://a.com:-1/',
    'http://[::1]:+0x50/',
    'http://[::1]:-1/x',
    'foo://a.com:+5/',
    'http://a.com:0x10/',
    'http://a.com:-0x10/',
    'http://a.com:0080/x',
    'http://a.com:+/',
    'http://a.com:-/',
    'http://a.com:+-1/',
    'http://a.com:٣/',
  ]) {
    final u = Uri.tryParse(s);
    print(u == null ? '$s -> null' : '$s -> ${u.port} $u');
  }
  try {
    Uri.parse('http://a.com:+/');
  } on FormatException {
    print('parse: FormatException');
  }
  // An oversized decimal port in a URI Dart need not normalise parses; reading it throws.
  for (final s in [
    'http://a.com:99999999999999999999/',
    'foo://a.com:9223372036854775808',
    'http://A.com:99999999999999999999/',
    'http://u@a.com:99999999999999999999/',
    'http://a.com:99999999999999999999/x/../y',
  ]) {
    final u = Uri.tryParse(s);
    if (u == null) {
      print('$s -> null');
      continue;
    }
    print('$s -> $u ${u.host} ${u == Uri.parse(s)}');
    try {
      print(u.port);
    } on FormatException catch (e) {
      print('port: $e');
    }
  }
}

void main() {
  durations();
  ints();
  reversedViews();
  uris();
}

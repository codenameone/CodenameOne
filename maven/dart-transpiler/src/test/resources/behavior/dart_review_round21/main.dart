// Labeled break/continue reach the statement they name, not the innermost
// loop. A local `late` variable runs its initializer on first read, and a
// read before any assignment throws. An unnamed constructor that calls a
// NAMED superclass constructor runs that one. A value-returning override of a
// void method still evaluates what it returns. Uri rejects a bracketed host
// that is not an IP address. A closure in a try block or switch case captures
// a reassigned local, and `continue` in an indexed loop keeps the index right.

final log = <String>[];

void labels() {
  final out = <String>[];
  outer:
  for (var i = 0; i < 3; i++) {
    for (var j = 0; j < 3; j++) {
      if (j == 1) continue outer;
      if (i == 2) break outer;
      out.add('$i$j');
    }
    out.add('never');
  }
  print(out);

  var k = 0;
  loop:
  while (true) {
    k++;
    for (final x in [1, 2, 3]) {
      if (x == 2 && k < 3) continue loop;
      if (k == 3) break loop;
    }
  }
  print('k=$k');

  block:
  {
    print('in block');
    if (k == 3) break block;
    print('not reached');
  }

  var n = 0;
  scan:
  for (var i = 0; i < 10; i++) {
    switch (i) {
      case 4:
        break scan;
      default:
        n += i;
    }
  }
  print('n=$n');

  var pairs = 0;
  rows:
  for (final r in [1, 2, 3]) {
    var c = 0;
    while (c < 3) {
      c++;
      if (c == r) continue rows;
      pairs++;
    }
  }
  print('pairs=$pairs');
}

int calls = 0;
int expensive(int v) {
  calls++;
  log.add('expensive $v');
  return v;
}

void lateLocals(bool assign) {
  late int x = expensive(42);
  print('before read calls=$calls');
  print(x);
  print(x + 1);
  print('after reads calls=$calls');

  late int w = expensive(7);
  w = 5;
  print('w=$w calls=$calls');

  late int y;
  if (assign) y = 3;
  try {
    print(y);
  } on Error catch (e) {
    print('caught: $e');
  }

  late final String s;
  if (assign) {
    s = 'one';
  }
  try {
    s = 'two';
    print('assigned s=$s');
  } on Error catch (e) {
    print('caught: $e');
  }

  var base = 1;
  late final captured = base * 10;
  base = 4;
  int Function() read = () => captured;
  print('captured=${read()}');

  late int counter;
  counter = 1;
  counter += 2;
  counter++;
  print('counter=$counter');
}

class Base {
  final String how;
  Base() : how = 'default' {
    log.add('Base()');
  }
  Base.named(int v) : how = 'named $v' {
    log.add('Base.named($v)');
  }
}

class Child extends Base {
  final int extra;
  Child(this.extra) : super.named(extra * 2);
}

class Counter {
  int n = 0;
  int x = 0;
  void bump() {}
  void setX() {}
  void touch() {}
  void peek() {}
  void noisyRead() {}
  int get noisy {
    log.add('noisy getter');
    return 1;
  }
}

class Loud extends Counter {
  @override
  int bump() {
    return n++;
  }

  @override
  int setX() {
    return x = 5;
  }

  @override
  int touch() => n += 10;

  @override
  int peek() {
    return x;
  }

  @override
  int noisyRead() => noisy;
}

void extras() {
  // A closure inside a try block or a switch case captures a local that is
  // reassigned there.
  var a = 0;
  try {
    int Function() f = () => a;
    a = 2;
    print('try capture ${f()}');
  } finally {
    print('finally');
  }
  var b = 0;
  switch (a) {
    case 2:
      int Function() g = () => b;
      b = 7;
      print('switch capture ${g()}');
  }
  // `continue` in an indexed loop still advances the index.
  final xs = ['a', 'b', 'c', 'd'];
  for (final (i, e) in xs.indexed) {
    if (e == 'b') continue;
    print('$i:$e');
  }
}

void main() {
  labels();
  extras();
  lateLocals(true);
  lateLocals(false);
  print(log);
  log.clear();

  final c = Child(3);
  print('${c.how} extra=${c.extra}');
  print(log);

  final l = Loud();
  l.bump();
  l.bump();
  l.setX();
  l.touch();
  l.peek();
  l.noisyRead();
  print('n=${l.n} x=${l.x}');
  print(log);

  for (final s in [
    'http://[not-an-ip]/',
    'http://[::1]/',
    'http://[::1]:8080/p',
    'http://[2001:db8::7]/',
    'http://[::ffff:192.168.1.1]/',
    'http://[1:2:3:4:5:6:7:8]/',
    'http://[1:2:3:4:5:6:7:8:9]/',
    'http://[1::2::3]/',
    'http://[::1%25eth0]/',
    'http://[fe80::1%25en0]:80/',
    'http://[]/',
    'http://[12345::]/',
    'http://[::256.1.1.1]/',
    'http://[g::1]/',
    'http://[FE80::A]/',
    'http://[v1.fe]/',
    'http://[1:2:3:4:5:6:7::]/',
    'http://[::1.2.3.4]/',
    'http://[1:2:3:4:5:6:1.2.3.4]/',
    'http://[1:2:3:4:5:6:7:1.2.3.4]/',
    'http://[::1%eth0]/',
    'http://[::1%25]/',
    'http://[1:2:3:4:5:6:7:8::]/',
    'http://[::01.2.3.4]/',
  ]) {
    final u = Uri.tryParse(s);
    print('$s -> ${u == null ? 'null' : '${u.host}|${u.port}'}');
  }
  try {
    Uri.parse('http://[bad]/');
    print('parsed');
  } on FormatException {
    print('FormatException');
  }
}

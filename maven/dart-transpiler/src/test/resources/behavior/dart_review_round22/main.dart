// An async closure returns a Future and reports what its body throws through
// it, typed or not. A case with no body of its own does not borrow the guard
// of the case it falls into. Uri drops the default port of http and https.
// An extension member resolves against the extensions the calling library can
// see, not whichever registered first. An object pattern reads each getter
// once even when it both tests and binds the value.

import 'review22_a.dart';
import 'review22_b.dart';
import 'review22_c.dart' hide Tripler;
import 'review22_e.dart';
import 'review22_g.dart' as g;

Future<void> asyncClosures() async {
  final f = () async {
    throw StateError('boom');
  };
  final r = f();
  print(r is Future);
  try {
    await r;
  } catch (e) {
    print('caught $e');
  }
  final dbl = (int x) async => x * 2;
  print(await dbl(21));
  final bang = (x) async => '$x!';
  print(await bang('hi'));
  final pair = (a, b) async {
    return '$a-$b';
  };
  print(await pair(1, 2));
  Future<int> Function() k = () async => 5;
  print(await k());
  final order = <String>[];
  final m = () async {
    order.add('in');
  };
  final fut = m();
  order.add('after');
  await fut;
  print(order);
  final thrower = (int x) async {
    if (x > 0) {
      throw ArgumentError('neg');
    }
    return x;
  };
  final t = thrower(1);
  print('no sync throw');
  try {
    await t;
  } catch (e) {
    print('async $e');
  }
  Future<void> Function() onRefresh = () async {
    order.add('refresh');
  };
  await onRefresh();
  print(order);
}

String fallThrough(int v) {
  switch (v) {
    case 1:
    case 2 when false:
      return 'body';
    case 3 when false:
    case 4:
      return 'body4';
    case 5:
    case int n when n > 100:
      return 'big';
    case 6 when true:
    case 7:
      return 'body7';
    case int m when m > 40 && m < 45:
    case 8:
      return 'mid';
    default:
      return 'default';
  }
}

class Box {
  final int _v;
  int reads = 0;
  Box(this._v);
  int get value {
    reads++;
    return _v;
  }
}

void patterns() {
  final b = Box(3);
  switch (b) {
    case Box(value: int x):
      print('x=$x reads=${b.reads}');
  }
  final c = Box(7);
  if (c case Box(value: var y) when y > 0) {
    print('y=$y reads=${c.reads}');
  }
  final d = Box(9);
  final s = switch (d) {
    Box(value: int z) when z > 5 => 'z$z',
    _ => 'other',
  };
  print('$s reads=${d.reads}');
  // Cases of one switch share a getter's value: Dart reads it the first time a
  // case needs it and never again in that switch.
  final e = Box(4);
  switch (e) {
    case Box(value: 5):
      print('five');
    case Box(value: int v) when v > 10:
      print('big');
    case Box(value: 4):
      print('four reads=${e.reads}');
  }
  final f = Box(6);
  final r = switch (f) {
    Box(value: 1) => 'one',
    Box(value: 2) => 'two',
    Box(value: final n) => 'n$n',
  };
  print('$r reads=${f.reads}');
  for (final g in [Box(1), Box(2)]) {
    switch (g) {
      case Box(value: 1):
      case Box(value: 2) when g.reads == 1:
        print('shared ${g.reads}');
      default:
        print('none');
    }
  }
}

void main() async {
  await asyncClosures();
  for (final v in [1, 2, 3, 4, 5, 150, 50, 6, 7, 8, 42]) {
    print('$v ${fallThrough(v)}');
  }
  for (final s in [
    'http://a.com:80/x',
    'https://a.com:443',
    'https://a.com:80',
    'ws://a.com:80/x',
    'HTTP://A.com:80',
    'https://u@a.com:443/p?q#f',
    'http://a.com:080',
    'foo://a.com:0',
  ]) {
    final u = Uri.parse(s);
    print('$u ${u.port} ${u == Uri.parse(u.toString())}');
  }
  print(Uri.parse('http://a.com:80/x') == Uri.parse('http://a.com/x'));
  print({Uri.parse('http://a.com:80/x'), Uri.parse('http://a.com/x')}.length);
  print(Uri.parse('http://a.com:/p'));
  print(Uri.parse('http://[::1]:80/'));
  print(viaA('s'));
  print(viaB('s'));
  print(tagA('t'));
  print(tagB('t'));
  print(5.twice);
  print('x'.shout);
  print('y'.whisper);
  print(g.viaG());
  patterns();
}

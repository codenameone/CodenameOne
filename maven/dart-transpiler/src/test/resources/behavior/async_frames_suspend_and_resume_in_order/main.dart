import 'dart:async';

void later(Future<void> Function() body) {
  body();
}

Future<int> f() async {
  return 3;
}

Future<int> slowAdd(int a, int b) async {
  await null;
  return a + b;
}

Future<int> sequential() async {
  var x = await slowAdd(1, 2);
  var y = await slowAdd(x, 10);
  int z;
  z = await slowAdd(y, 100);
  return z;
}

Future<List<String>> loops() async {
  final out = <String>[];
  for (var i = 0; i < 3; i++) {
    final v = await slowAdd(i, 0);
    out.add('for$v');
    await null;
  }
  var n = 0;
  while (n < 3) {
    n = await slowAdd(n, 1);
    if (n == 2) {
      continue;
    }
    out.add('while$n');
  }
  do {
    await null;
    n--;
  } while (n > 0);
  out.add('do$n');
  outer:
  for (final s in ['a', 'b', 'c']) {
    for (var j = 0; j < 3; j++) {
      await null;
      if (j == 1) continue outer;
      if (s == 'c') break outer;
      out.add('$s$j');
    }
  }
  final k = await slowAdd(1, 1);
  switch (k) {
    case 1:
      out.add('one');
      break;
    case 2:
      await null;
      out.add('two');
      break;
    default:
      out.add('other');
  }
  return out;
}

Future<void> closureSeesMutation() async {
  var count = 0;
  final read = () => count;
  await null;
  count = 5;
  print('closure reads ${read()}');
  count = await slowAdd(count, 1);
  print('closure reads ${read()}');
}

Future<void> awaitShapes() async {
  Future<int>? none;
  final a = await none;
  print('nullable future $a');
  FutureOr<int> fo = 4;
  final b = await fo;
  FutureOr<int> fo2 = Future.value(5);
  final c = await fo2;
  dynamic d = Future.value('dyn');
  final e = await d;
  dynamic plain = 9;
  final g = await plain;
  print('futureor $b $c dynamic $e $g');
}

final ticks = <String>[];

Future<void> tick(String n) async {
  ticks.add('$n-a');
  await null;
  ticks.add('$n-b');
}

Future<void> main() async {
  later(() async {
    print('v1');
    await f();
    print('v3');
  });
  print('v2');
  var order = <String>[];
  final t = sequential().then((v) => order.add('sequential $v'));
  order.add('sync after call');
  await t;
  print(order.join(', '));
  final looped = await loops();
  print(looped.join(' '));
  await closureSeesMutation();
  await awaitShapes();
  final done = Completer<void>();
  runZonedGuarded(() {
    final void Function() handler = () async {
      await null;
      throw StateError('void closure failed');
    };
    handler();
  }, (e, st) {
    print('zone caught $e');
    done.complete();
  });
  await done.future;
  tick('x');
  tick('y');
  ticks.add('main');
  await null;
  await null;
  print(ticks.join(' '));
  print('end');
}

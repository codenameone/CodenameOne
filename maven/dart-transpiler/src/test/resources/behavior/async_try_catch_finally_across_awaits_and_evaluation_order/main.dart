// try/catch/finally holding awaits in async bodies, and evaluation order around awaits.

class Oops implements Exception {
  final String why;
  Oops(this.why);
  String toString() => 'Oops($why)';
}

final log = <String>[];

Future<int> val(int v) async {
  log.add('val $v');
  return v;
}

Future<int> boom(String why) async {
  log.add('boom $why');
  throw Oops(why);
}

void flush(String title) {
  print('$title: ${log.join(', ')}');
  log.clear();
}

Future<String> tryInBody() async {
  try {
    final a = await val(1);
    log.add('got $a');
    await boom('t1');
    log.add('unreached');
  } on Oops catch (e, s) {
    log.add('caught $e ${s != null}');
    final b = await val(2);
    log.add('in catch $b');
  } finally {
    log.add('finally');
    await val(3);
    log.add('after finally await');
  }
  return 'done';
}

Future<int> returnThroughFinally() async {
  try {
    await val(10);
    return 11;
  } finally {
    log.add('fin runs before return');
    await val(12);
  }
}

Future<int> loopsThroughFinally() async {
  var total = 0;
  for (var i = 0; i < 5; i++) {
    try {
      if (i == 1) continue;
      if (i == 3) break;
      total += await val(i);
    } finally {
      log.add('fin $i');
    }
  }
  outer:
  for (var j = 0; j < 3; j++) {
    try {
      try {
        await val(100 + j);
        if (j == 1) break outer;
      } finally {
        log.add('inner fin $j');
      }
    } finally {
      log.add('outer fin $j');
    }
  }
  return total;
}

Future<void> finallyThrowReplaces() async {
  try {
    try {
      await boom('first');
    } finally {
      await val(20);
      throw Oops('second');
    }
  } catch (e) {
    log.add('outer saw $e');
  }
}

Future<void> rethrowAndUntyped() async {
  try {
    try {
      await boom('r');
    } on StateError {
      log.add('wrong clause');
    } on Oops catch (e) {
      log.add('typed $e');
      await val(30);
      rethrow;
    }
  } catch (e, s) {
    log.add('untyped $e');
  }
  try {
    await val(31);
    throw 'a string';
  } on int {
    log.add('int clause');
  } catch (e) {
    log.add('bare catch $e');
  }
}

Future<int> nestedReturns() async {
  try {
    try {
      await val(40);
      return 41;
    } finally {
      log.add('inner');
      await val(42);
    }
  } finally {
    log.add('outer');
  }
}

Future<void> awaitFreeTryInFrame() async {
  await val(50);
  try {
    log.add('plain try');
    throw Oops('plain');
  } on Oops catch (e) {
    log.add('plain catch $e');
  } finally {
    log.add('plain finally');
  }
  await val(51);
}

Future<int> uncaughtThroughFinally() async {
  try {
    await boom('escape');
    return 0;
  } finally {
    log.add('cleanup');
  }
}

class Holder {
  int n = 0;
  Holder? next;
  String show(int a, {int b = 0, int c = 0}) => 'n=$n a=$a b=$b c=$c';
}

Holder holder = Holder();

class Widgetish {
  Holder field = Holder();
  static Holder shared = Holder();

  Future<void> receivers() async {
    // A field receiver is read before the awaits in the arguments.
    log.add(field.show(await swapField(this)));
    log.add(Widgetish.shared.show(await swapShared()));
    log.add(holder.show(await swapTop()));
  }
}

Future<int> swapField(Widgetish w) async {
  w.field = Holder()..n = 7;
  return 1;
}

Future<int> swapShared() async {
  Widgetish.shared = Holder()..n = 8;
  return 2;
}

Future<int> swapTop() async {
  holder = Holder()..n = 9;
  return 3;
}

int tick(String s, int v) {
  log.add(s);
  return v;
}

Future<void> argumentOrder() async {
  final h = Holder();
  log.add(h.show(tick('p1', 1), c: tick('c', 3), b: await val(2)));
  log.add(h.show(c: tick('c2', 3), tick('p2', 1), b: await val(4)));
}

Future<void> nullShorting() async {
  Holder? none;
  final r = none?.next!.show(await val(60));
  log.add('short $r');
  final some = Holder()..next = (Holder()..n = 5);
  final r2 = some?.next!.show(await val(61));
  log.add('long $r2');
}

Future<void> forHeaderAwaits() async {
  final fns = <int Function()>[];
  for (var i = await val(0); i < await val(3); i += await val(1)) {
    fns.add(() => i);
  }
  log.add('captured ${fns.map((f) => f()).toList()}');
}

Future<void> main() async {
  print(await tryInBody());
  flush('tryInBody');
  print(await returnThroughFinally());
  flush('returnThroughFinally');
  print(await loopsThroughFinally());
  flush('loopsThroughFinally');
  await finallyThrowReplaces();
  flush('finallyThrowReplaces');
  await rethrowAndUntyped();
  flush('rethrowAndUntyped');
  print(await nestedReturns());
  flush('nestedReturns');
  await awaitFreeTryInFrame();
  flush('awaitFreeTryInFrame');
  try {
    await uncaughtThroughFinally();
  } catch (e) {
    log.add('main caught $e');
  }
  flush('uncaughtThroughFinally');
  await Widgetish().receivers();
  flush('receivers');
  await argumentOrder();
  flush('argumentOrder');
  await nullShorting();
  flush('nullShorting');
  await forHeaderAwaits();
  flush('forHeaderAwaits');
}

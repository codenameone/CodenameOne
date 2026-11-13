import 'dart:async';

final List<String> log = <String>[];

Stream<int> counted(int n, String tag) async* {
  try {
    for (var i = 0; i < n; i++) {
      log.add('$tag yield $i');
      yield i;
    }
  } finally {
    log.add('$tag finally');
  }
}

Stream<int> nested() async* {
  yield 100;
  yield* counted(2, 'inner');
  try {
    yield* Stream<int>.fromIterable(<int>[7, 8]);
  } finally {
    log.add('nested finally');
  }
  yield 200;
}

Stream<int> failing() async* {
  yield 1;
  await Future<void>.delayed(Duration.zero);
  throw StateError('boom');
}

Future<int> sumUntil(Stream<int> s, int limit) async {
  var sum = 0;
  try {
    await for (final v in s) {
      if (v >= limit) {
        log.add('break at $v');
        break;
      }
      if (v.isOdd) {
        continue;
      }
      sum += v;
    }
  } finally {
    log.add('sum finally $sum');
  }
  return sum;
}

Future<String> returnInside(Stream<int> s) async {
  await for (var v in s) {
    if (v == 1) {
      return 'returned $v';
    }
  }
  return 'none';
}

Future<void> catches() async {
  try {
    await for (var v in failing()) {
      log.add('failing got $v');
    }
  } catch (e) {
    log.add('caught ${e.runtimeType}');
  }
  var c = StreamController<int>(onCancel: () => log.add('controller cancelled'));
  c.add(5);
  c.add(6);
  try {
    await for (var v in c.stream) {
      log.add('ctl $v');
      if (v == 6) {
        throw ArgumentError('stop');
      }
    }
  } on ArgumentError catch (e) {
    log.add('arg ${e.message}');
  }
}

Stream<String> ticker(String tag) async* {
  for (var i = 0; i < 3; i++) {
    log.add('$tag produce $i');
    yield '$tag$i';
  }
}

Future<void> interleave() async {
  scheduleMicrotask(() => log.add('micro A'));
  final f1 = () async {
    await for (var v in ticker('x')) {
      log.add('got $v');
    }
  }();
  scheduleMicrotask(() => log.add('micro B'));
  Future<void>(() => log.add('timer'));
  await f1;
  log.add('interleave done');
}

Future<void> pauseAndCancel() async {
  final done = Completer<void>();
  late StreamSubscription<int> sub;
  sub = counted(5, 'p').listen((v) {
    log.add('listen $v');
    if (v == 1) {
      sub.pause();
      log.add('paused');
      Future<void>(() {
        log.add('resuming');
        sub.resume();
      });
    }
    if (v == 3) {
      sub.cancel().then((_) {
        log.add('cancel complete');
        done.complete();
      });
    }
  });
  await done.future;
}

Future<void> main() async {
  log.add('sum ${await sumUntil(counted(6, 'a'), 4)}');
  log.add(await returnInside(counted(4, 'b')));
  await catches();
  final all = <int>[];
  await for (var v in nested()) {
    all.add(v);
  }
  log.add('nested $all');
  await interleave();
  await pauseAndCancel();
  final empty = <int>[];
  await for (var v in Stream<int>.empty()) {
    empty.add(v);
  }
  log.add('empty $empty');
  for (final l in log) {
    print(l);
  }
}

import 'dart:async';

final log = <String>[];

void later(void Function() f) => f();

Future<int> load() async => 7;

Future<void> topLevel(String tag) async {
  log.add('$tag-1');
  await load();
  log.add('$tag-3');
}

Future<int> throwsEarly(bool fail) async {
  if (fail) {
    throw StateError('early');
  }
  await Future.value(0);
  return 1;
}

Future<String> inner() async {
  log.add('inner-a');
  await null;
  log.add('inner-b');
  return 'in';
}

Future<String> outer() async {
  log.add('outer-a');
  final v = await inner();
  log.add('outer-b:$v');
  return 'out';
}

Future<int> bump(int n) async {
  n += 1;
  await Future.value(n);
  n += 1;
  return n;
}

Future<void> main() async {
  // An un-awaited closure runs to its first await, then the caller goes on.
  later(() async {
    log.add('c1');
    await load();
    log.add('c3');
  });
  log.add('c2');

  // An un-awaited top-level call.
  topLevel('t');
  log.add('t-2');

  // await of a completed future still yields.
  final done = Future.value(5);
  await Future.value(null);
  scheduleMicrotask(() => log.add('micro'));
  final v = await done;
  log.add('completed:$v');

  // A throw before the first await is the Future's error, never the caller's.
  try {
    final f = throwsEarly(true);
    log.add('no sync throw');
    f.catchError((Object e) {
      log.add('catchError:$e');
      return 0;
    });
  } catch (e) {
    log.add('sync throw:$e');
  }
  try {
    await throwsEarly(true);
  } catch (e) {
    log.add('await caught:$e');
  }
  final completer = Completer<void>();
  runZonedGuarded(() {
    throwsEarly(true);
    later(() async {
      throw ArgumentError('void closure');
    });
    log.add('guarded body done');
  }, (e, st) {
    log.add('zone:$e');
  });
  Timer(const Duration(milliseconds: 20), completer.complete);
  await completer.future;

  // Nested async.
  final o = outer();
  log.add('after outer call');
  log.add('outer result:${await o}');

  // Ordering against scheduleMicrotask and Future(() {}).
  Future(() => log.add('event'));
  scheduleMicrotask(() => log.add('micro2'));
  final p = topLevel('m');
  log.add('m-2');
  await p;
  await Future(() {});
  log.add('bump:${await bump(1)}');

  for (final line in log) {
    print(line);
  }
}

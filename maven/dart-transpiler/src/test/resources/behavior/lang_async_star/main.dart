import 'dart:async';

Stream<int> countTo(int n) async* {
  for (var i = 1; i <= n; i++) {
    await Future.delayed(Duration.zero);
    yield i;
  }
}

Stream<int> doubled(Stream<int> source) async* {
  await for (final v in source) {
    yield v * 2;
  }
}

Stream<String> labels() async* {
  yield 'start';
  yield* doubled(countTo(2)).map((v) => 'v$v');
  yield 'end';
}

Stream<int> failing() async* {
  yield 1;
  throw StateError('boom');
}

Future<void> main() async {
  var sum = 0;
  await for (final v in countTo(4)) {
    sum += v;
  }
  print('sum $sum');

  await for (final s in labels()) {
    print(s);
  }

  await for (final v in countTo(100)) {
    if (v == 3) {
      print('break at $v');
      break;
    }
  }

  try {
    await for (final v in failing()) {
      print('got $v');
    }
  } on StateError catch (e) {
    print('error ${e.message}');
  }
  print('done');
}

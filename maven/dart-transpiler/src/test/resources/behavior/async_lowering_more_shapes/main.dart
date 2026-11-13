Future<int> later(int v) => Future<int>.delayed(Duration.zero, () => v);

Future<bool> isBig(int v) async => (await later(v)) > 10;

Future<String> doWhileAwait(int n) async {
  final out = <int>[];
  var i = 0;
  do {
    out.add(await later(i * 3));
    i++;
  } while (i < n);
  return out.join(',');
}

Iterable<int> doWhileYield(int n) sync* {
  var i = n;
  do {
    yield i;
    i -= 2;
  } while (i > 0);
}

Stream<int> doWhileStar(int n) async* {
  var i = 0;
  do {
    yield await later(i);
    i++;
  } while (i < n);
}

Future<String> labeledBlock(int v) async {
  var s = 'start';
  outer:
  {
    s += '-${await later(v)}';
    if (v > 2) break outer;
    s += '-small';
  }
  check:
  if (v.isEven) {
    s += '-even${await later(v * 2)}';
    if (v > 3) break check;
    s += '-tiny';
  }
  return s;
}

Future<String> tryShapes(int v) async {
  final log = <String>[];
  try {
    try {
      log.add('a${await later(v)}');
      if (v == 1) throw StateError('one');
      if (v == 2) throw FormatException('two');
    } on StateError catch (e) {
      log.add('state ${await later(e.message.length)}');
      rethrow;
    } on FormatException {
      log.add('fmt');
    } finally {
      log.add('fin${await later(0)}');
    }
  } catch (e, st) {
    log.add('outer ${e.runtimeType} ${st.toString().isNotEmpty}');
  }
  return log.join(' ');
}

Future<String> guarded(Object o) async {
  final out = <String>[];
  for (var round = 0; round < 2; round++) {
    switch (o) {
      case int n when await isBig(n):
        out.add('big $n');
      case int n when n < 0:
      case String _ when round == 1:
        out.add('neg-or-str');
      case int n:
        final m = await later(n + round);
        if (m.isOdd) {
          out.add('odd $m');
          break;
        }
        out.add('int $m');
      case (int a, int b):
        final s = await later(a + b);
        out.add('rec $s');
        if (round == 0 && s > 100) continue fallback;
      case String s when (await later(s.length)) > 3:
        out.add('long $s');
      fallback:
      default:
        out.add('other${await later(round)}');
    }
  }
  return out.join('; ');
}

Future<void> mapCase(List<int> xs) async {
  final map = {for (final x in xs) if (await isBig(x)) 'k$x': await later(x)};
  print('${map is Map<String, int>} $map');
}

Future<void> collections(List<int> xs, bool flag) async {
  final evens = [for (final x in xs) if (x.isEven) await later(x * 10)];
  print('${evens.runtimeType} $evens ${evens.fold<int>(0, (a, b) => a + b)}');
  final mixed = [0, if (flag) await later(1) else -1, for (var i = 0; i < 2; i++) await later(i + 5)];
  print('$mixed ${mixed.length}');
  final set = {for (final x in xs) await later(x % 3)};
  print('${set is Set<int>} $set');
  await mapCase(xs);
  final words = <String>['a', if (flag) 'b${await later(2)}'];
  print('${words.runtimeType} $words');
}

Future<String> awaitedHeads(int n) async {
  final out = <String>[];
  var i = 0;
  while (await later(i) < n) {
    i++;
  }
  out.add('w$i');
  var j = 0;
  do {
    j += 2;
  } while ((await later(j)) < n);
  out.add('d$j');
  for (final x in await Future.value([n, n + 1])) {
    out.add('f$x');
  }
  switch (await later(n % 3)) {
    case 0:
      out.add('zero');
    case final r:
      out.add('r$r');
  }
  final (a, b) = await Future.value((n, n * 2));
  out.add('p${a + b}');
  tag:
  out.add('t${await later(1)}');
  return out.join(' ');
}

void main() async {
  print(await awaitedHeads(4));
  print(await awaitedHeads(3));
  print(await doWhileAwait(3));
  print(await doWhileAwait(0));
  print(doWhileYield(5).toList());
  print(await doWhileStar(3).toList());
  for (final v in [1, 4, 6]) {
    print(await labeledBlock(v));
  }
  for (final v in [0, 1, 2]) {
    print(await tryShapes(v));
  }
  for (final o in <Object>[42, -3, 5, 4, (1, 2), (60, 70), 'hello', 'hi', 2.5]) {
    print(await guarded(o));
  }
  await collections([1, 2, 3, 12, 20], true);
  await collections([5], false);
}

import 'dart:async';

final List<String> log = <String>[];

Future<int> tick(String who, int v) async {
  log.add('$who:start');
  await Future<void>.delayed(Duration.zero);
  log.add('$who:end');
  return v;
}

Future<int> arrowAwait(String who) async => await tick(who, 2) * 10;

Future<void> arrowVoid(String who) async => log.add('$who:${await tick(who, 1)}');

class Reader {
  int closed = 0;
  Future<void> cancel() async => await _close();
  Future<void> cancelTwice() async {
    await cancel();
    return await _close();
  }
  Future<void> _close() async {
    await Future<void>.delayed(Duration.zero);
    closed++;
  }
}

extension Doubler on Future<int> {
  Future<int> doubled() async => (await this) * 2;
}

Future<String> withLocalFunction(String who) async {
  int calls = 0;
  String label(int n) {
    calls++;
    return '$who#$n';
  }

  int fact(int n) => n <= 1 ? 1 : n * fact(n - 1);

  Future<int> step(int n) async {
    await Future<void>.delayed(Duration.zero);
    return n + 1;
  }

  final a = label(await step(0));
  final b = label(await step(1));
  return '$a $b calls=$calls fact=${fact(5)}';
}

Future<void> main() async {
  final both = await Future.wait(<Future<int>>[arrowAwait('x'), arrowAwait('y')]);
  print(both);
  await Future.wait(<Future<void>>[arrowVoid('p'), arrowVoid('q')]);
  print(log.join(','));
  final r = Reader();
  final c = r.cancel();
  print('closed before: ${r.closed}');
  await c;
  print('closed after: ${r.closed}');
  await r.cancelTwice();
  print('closed twice: ${r.closed}');
  print(await Future<int>.value(21).doubled());
  final names = await Future.wait(<Future<String>>[withLocalFunction('m'), withLocalFunction('n')]);
  print(names);
  print(await Stream<int>.fromIterable(<int>[7]).single);
  try {
    await Stream<int>.fromIterable(<int>[1, 2]).single;
  } on StateError catch (e) {
    print('two: ${e.message}');
  }
  try {
    await Stream<int>.empty().single;
  } on StateError catch (e) {
    print('none: ${e.message}');
  }
}

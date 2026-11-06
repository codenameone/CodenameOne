import 'dart:async';
Stream<int> slow(List<String> log) async* {
  try {
    for (var i = 0; i < 4; i++) { await Future.delayed(Duration(milliseconds: 2)); log.add('y$i'); yield i; }
  } finally { log.add('fin'); }
}
Future<int> deep(int n) async => n == 0 ? 0 : 1 + await deep(n - 1);
void main() async {
  var log = <String>[];
  late StreamSubscription<int> sub;
  sub = slow(log).listen((x) async { log.add('L$x'); if (x == 1) { sub.pause(Future.delayed(Duration(milliseconds: 10), () => log.add('resume'))); } if (x == 2) { await sub.cancel(); log.add('cx'); } });
  await Future.delayed(Duration(milliseconds: 60));
  print(log.join(' '));
  print(await deep(500));
  var c = Completer<int>();
  Future.delayed(Duration(milliseconds: 1), () => c.complete(4));
  var w = await Future.wait([c.future, Future.value(2), () async { await null; return 3; }()]);
  print(w);
  try { await Future.wait([Future.error('x'), Future.value(1)]); } catch (e) { print('fw $e'); }
  var r = await Future.value(1).then<int>((v) => throw 'th').catchError((e) => 9);
  print(r);
  try { await Future.delayed(Duration(milliseconds: 50), () => 1).timeout(Duration(milliseconds: 5)); } on TimeoutException { print('timeout'); }
  var acc = <String>[];
  var fs = [for (var i = 0; i < 3; i++) () async { acc.add('s$i'); await null; acc.add('e$i'); }()];
  await Future.wait(fs);
  print(acc.join(' '));
  print(await Stream.fromIterable([1,2,3]).asyncMap((x) async { await null; return x * 2; }).fold<int>(0, (a, b) => a + b));
}

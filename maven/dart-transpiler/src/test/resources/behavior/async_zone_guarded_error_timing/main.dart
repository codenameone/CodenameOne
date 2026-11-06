import 'dart:async';
Stream<int> gen(int n) async* { for (var i = 0; i < n; i++) { await null; yield i; } }
Future<int> tf() async { try { await Future.value(1); throw StateError('x'); } finally { print('fin'); } }
Future<void> main() async {
  var s = 0;
  await for (var v in gen(3000)) { s += v; }
  print(s);
  try { await tf(); } catch (e) { print('caught $e'); }
  runZonedGuarded(() async { await null; throw 'zoned'; }, (e, st) { print('zone got $e'); });
  var z = Zone.current.fork(zoneValues: {#k: 7});
  await z.run(() async { await null; print('zv ${Zone.current[#k]}'); });
  Future<void> Function() f = () async => print('arrow');
  await f();
  print('end');
}

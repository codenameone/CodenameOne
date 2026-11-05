import 'dart:async';
Stream<int> gen(List<String> log) async* {
  try {
    for (var i = 0; i < 5; i++) { log.add('y$i'); yield i; await null; }
  } finally { log.add('genfin'); }
}
Stream<int> inner() async* { yield 1; yield 2; }
Stream<int> outer() async* { yield 0; yield* inner(); yield* Stream.fromIterable([3,4]); yield 5; }
Stream<int> errs() async* { yield 1; throw 'bad'; }
void main() async {
  var log = <String>[];
  await for (var x in gen(log)) { log.add('g$x'); if (x == 2) break; }
  log.add('afterbreak');
  print(log.join(' ')); log.clear();
  print((await outer().toList()).join(','));
  try { await for (var x in errs()) { print('e$x'); } } catch (e) { print('caught $e'); }
  var sub = gen(log).listen((x) { log.add('L$x'); });
  await Future.delayed(Duration.zero);
  sub.pause(); log.add('paused');
  await Future.delayed(Duration(milliseconds: 5));
  sub.resume(); log.add('resumed');
  await Future.delayed(Duration(milliseconds: 5));
  await sub.cancel(); log.add('cancelled');
  print(log.join(' ')); log.clear();
  var r = [];
  scheduleMicrotask(() => r.add('m1'));
  Timer.run(() => r.add('t1'));
  Future(() => r.add('f1')).then((_) { r.add('f1then'); scheduleMicrotask(() => r.add('m3')); });
  Future.value(1).then((_) => r.add('fv'));
  Future.microtask(() => r.add('m2'));
  r.add('sync');
  await Future.delayed(Duration(milliseconds: 10));
  print(r.join(' '));
  var c = StreamController<int>();
  var it = StreamIterator(c.stream);
  c.add(1); c.add(2); c.close();
  while (await it.moveNext()) { print('it ${it.current}'); }
  var c2 = StreamController<int>(onCancel: () => print('onCancel'));
  c2.add(7); c2.add(8);
  await for (var x in c2.stream) { print('c2 $x'); break; }
  print('done');
}

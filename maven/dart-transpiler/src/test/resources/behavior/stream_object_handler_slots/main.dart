import 'dart:async';

class Collector {
  final List<String> seen = [];
  void onErr(Object e, StackTrace s) => seen.add('err:$e');
  void onDat(int v) => seen.add('dat:$v');
  void done() => seen.add('done');
}

Future<void> main() async {
  final c = StreamController<int>();
  final sub = c.stream.listen((v) => print('first $v'));
  sub.onData((v) {
    print('data $v');
  });
  sub.onError((e, s) {
    print('error2 $e');
  });
  sub.onDone(() => print('done'));
  c.add(1);
  c.addError('boom');
  await c.close();
  await Future.delayed(Duration.zero);

  final col = Collector();
  final c2 = StreamController<int>();
  final s2 = c2.stream.listen(null);
  s2.onData(col.onDat);
  s2.onError(col.onErr);
  s2.onDone(col.done);
  c2.add(7);
  c2.addError('bad');
  await c2.close();
  await Future.delayed(Duration.zero);
  print(col.seen);

  final completer = Completer<int>();
  final c3 = StreamController<int>();
  c3.stream.listen((v) => print('v $v'), onError: completer.completeError);
  c3.addError('late');
  try {
    await completer.future;
  } catch (e) {
    print('caught $e');
  }
  final c4 = StreamController<int>();
  final s4 = c4.stream.listen(null);
  s4.onError((e) => print('error1 $e'));
  c4.addError('one');
  await c4.close();
  await Future.delayed(Duration.zero);
}

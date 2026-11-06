import 'dart:async';
import 'dart:io';
import 'dart:math';

class Collector implements StreamConsumer<int> {
  final List<int> got = [];
  @override
  Future<void> addStream(Stream<int> stream) async {
    await for (final v in stream) {
      got.add(v);
    }
  }

  @override
  Future<void> close() async {
    got.add(-1);
  }
}

Future<int> slow() => Future.delayed(const Duration(milliseconds: 200), () => 1);

Future<void> main() async {
  unawaited(Future.value(1));
  try {
    await slow().timeout(const Duration(milliseconds: 10));
  } on TimeoutException {
    print('timed out');
  }
  print('timeout onTimeout ${await slow().timeout(const Duration(milliseconds: 10), onTimeout: () => 9)}');
  Future.value(2).ignore();
  print('asStream ${await Future.value(3).asStream().toList()}');

  final Completer<String> c = Completer.sync();
  c.complete('done');
  print('completer.sync ${await c.future}');

  final order = <String>[];
  scheduleMicrotask(() => order.add('micro'));
  order.add('sync');
  await Future.delayed(Duration.zero);
  print('order $order');

  var ticks = 0;
  final done = Completer<void>();
  Timer.periodic(const Duration(milliseconds: 5), (t) {
    ticks++;
    if (t.tick >= 3) {
      t.cancel();
      done.complete();
    }
  });
  await done.future;
  print('periodic $ticks');

  print('zoned ${runZoned(() => 'z')}');
  print('zone parent ${Zone.current.parent == null}');

  final events = <String>[];
  final ctrl = StreamController<int>(
    onListen: () => events.add('listen'),
    onPause: () => events.add('pause'),
    onResume: () => events.add('resume'),
    onCancel: () => events.add('cancel'),
  );
  final sub = ctrl.stream.listen((v) => events.add('v$v'));
  ctrl.add(1);
  await Future.delayed(Duration.zero);
  sub.pause();
  print('isPaused ${ctrl.isPaused}');
  sub.resume();
  ctrl.sink.add(2);
  await ctrl.addStream(Stream.fromIterable([3, 4]));
  await Future.delayed(Duration.zero);
  await sub.cancel();
  print('events $events');

  final c2 = StreamController<String>();
  final collected = c2.stream.toList();
  final EventSink<String> es = c2.sink;
  es.add('10');
  final StreamSink<String> ss = c2;
  ss.add('11');
  await ss.close();
  await ss.done;
  print('sink ${await collected}');

  final col = Collector();
  await Stream.fromIterable([1, 2, 3]).pipe(col);
  print('pipe ${col.got}');

  final doubled = Stream.fromIterable([1, 2]).transform(
      StreamTransformer.fromHandlers(handleData: (v, sink) => sink.add((v as int) * 2)));
  print('fromHandlers ${await doubled.toList()}');


  final StreamIterator<String> it = StreamIterator(Stream.fromIterable(['a', 'b', 'c']));
  final seen = <String>[];
  while (await it.moveNext()) {
    seen.add(it.current);
    if (seen.length == 2) break;
  }
  await it.cancel();
  print('iterator $seen');


  Object e1 = HandshakeException('hs');
  print('tls ${e1 is TlsException} ${e1 is IOException} ${TlsException('t') is HandshakeException}');
  try {
    throw TlsException('nope');
  } on TlsException catch (e) {
    print('caught ${e.message}');
  }
}

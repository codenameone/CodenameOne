import 'dart:async';
import 'dart:convert';

extension Countdown on int {
  Stream<int> countdown() async* {
    for (var i = this; i > 0; i--) {
      await null;
      yield i;
    }
    yield* Stream.fromIterable([0]);
  }
}

mixin Ticker {
  String get label;

  Stream<String> ticks(int n) async* {
    var i = 0;
    while (i < n) {
      yield '$label#$i';
      await Future.delayed(Duration.zero);
      i++;
    }
  }
}

class Clock with Ticker {
  @override
  String get label => 'clk';
}

Stream<List<int>> bytes(List<List<int>> chunks) async* {
  for (final c in chunks) {
    yield c;
  }
}

Future<void> main() async {
  final log = <String>[];

  // utf8 decode split mid-sequence, then lines split across chunks.
  final text = utf8.encode('alpha\r\nbeéta\ngam');
  final lines = await bytes([text.sublist(0, 9), text.sublist(9, 13), text.sublist(13)])
      .transform(utf8.decoder)
      .transform(const LineSplitter())
      .toList();
  print('lines: $lines');

  // json decode over a chunked text stream.
  final decoded = await Stream.fromIterable(['{"a": [1, ', '2], "b": ', 'true}'])
      .transform(json.decoder)
      .first;
  print('json: $decoded');

  // Transform chain with interleaving against microtasks.
  final c = StreamController<int>();
  final sub = c.stream
      .skip(1)
      .where((x) => x.isOdd)
      .map((x) => x * 10)
      .take(3)
      .listen((v) => log.add('take:$v'), onDone: () => log.add('take:done'));
  for (var i = 0; i < 10; i++) {
    c.add(i);
  }
  scheduleMicrotask(() => log.add('micro'));
  await Future.delayed(Duration.zero);
  print(log.join(' '));
  print('controller has listener after take: ${c.hasListener}');
  await sub.cancel();
  log.clear();

  // Pause and resume through a derived stream reach the source.
  final src = StreamController<int>(
      onPause: () => log.add('src:pause'), onResume: () => log.add('src:resume'),
      onCancel: () => log.add('src:cancel'));
  final s2 = src.stream.distinct().skipWhile((x) => x < 2).listen((v) => log.add('d:$v'));
  src.add(1);
  src.add(2);
  src.add(2);
  await Future.delayed(Duration.zero);
  s2.pause();
  src.add(3);
  await Future.delayed(Duration.zero);
  log.add('paused:${src.isPaused}');
  s2.resume();
  await Future.delayed(Duration.zero);
  await s2.cancel();
  print(log.join(' '));
  log.clear();

  // asyncMap holds the source while a future is pending.
  final am = await Stream.fromIterable([1, 2, 3])
      .asyncMap((x) async {
        await Future.delayed(Duration(milliseconds: 1));
        return x * x;
      })
      .expand((x) => [x, -x])
      .takeWhile((x) => x != -9)
      .toList();
  print('asyncMap: $am');

  // Errors pass through forwarding transforms without ending the stream.
  final ec = StreamController<int>();
  final got = <String>[];
  final done = Completer<void>();
  ec.stream.skip(0).distinct().listen((v) => got.add('v$v'),
      onError: (e) => got.add('e:$e'), onDone: done.complete);
  ec.add(1);
  ec.addError('boom');
  ec.add(2);
  ec.close();
  await done.future;
  print('errors: $got');

  // take(0) ends at once.
  print('take0: ${await Stream.fromIterable([1, 2]).take(0).toList()}');

  // async* in an extension and a mixin.
  print('ext: ${await 3.countdown().toList()}');
  print('mixin: ${await Clock().ticks(3).toList()}');
  final early = await Clock().ticks(100).take(2).toList();
  print('mixin take: $early');
}

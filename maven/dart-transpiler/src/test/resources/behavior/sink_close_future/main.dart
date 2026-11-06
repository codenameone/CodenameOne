import 'dart:async';

// A program StreamSink whose close() answers a future the caller awaits.
class Recorder implements StreamSink<int> {
  final List<int> got = [];
  final Completer<void> _done = Completer<void>();
  @override
  void add(int event) => got.add(event);
  @override
  void addError(Object error, [StackTrace? stackTrace]) => got.add(-1);
  @override
  Future addStream(Stream<int> stream) => stream.forEach(add);
  @override
  Future close() {
    got.add(0);
    _done.complete();
    return _done.future;
  }

  @override
  Future get done => _done.future;
}

// A program Sink whose close() also answers a future, as Dart allows.
class Group implements Sink<String> {
  final List<String> log = [];
  @override
  void add(String data) => log.add(data);
  @override
  Future<String> close() async {
    log.add('closing');
    return 'closed ${log.length}';
  }
}

// An override that reaches the superclass's future through super.close().
class LoudGroup extends Group {
  @override
  Future<String> close() async {
    log.add('loud');
    return super.close();
  }
}

void feed(Sink<String> s) {
  s.add('via sink');
  s.close();
}

Future<void> main() async {
  final r = Recorder();
  r.add(1);
  await r.addStream(Stream.fromIterable([2, 3]));
  await r.close();
  print(r.got);
  await r.done;
  print('done');

  final g = Group();
  g.add('a');
  final msg = await g.close();
  print(msg);
  await g.close().then((v) => print('then $v'));
  feed(g);
  print(g.log);

  final Group lg = LoudGroup();
  print(await lg.close());
  feed(lg);
  print(lg.log);
  await dynamicClose();
}

// A dynamic receiver still reaches the future, not the void bridge.
Future<void> dynamicClose() async {
  dynamic d = Group();
  d.add('dyn');
  print(await d.close());
  final Object o = LoudGroup();
  print(await (o as dynamic).close());
}

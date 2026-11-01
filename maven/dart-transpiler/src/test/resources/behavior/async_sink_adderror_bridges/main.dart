import 'dart:async';

// A sink that implements EventSink and records what reaches it.
class LogSink implements EventSink<String> {
  final List<String> log = [];
  @override
  void add(String event) => log.add('add $event');
  @override
  void addError(Object error, [StackTrace? stackTrace]) =>
      log.add('error $error trace=${stackTrace != null}');
  @override
  void close() => log.add('close');
}

// A StreamSink whose addError forwards to a delegate.
class ForwardSink implements StreamSink<String> {
  final StreamController<String> inner = StreamController<String>();
  final List<String> seen = [];
  @override
  void add(String event) => inner.add(event);
  @override
  void addError(Object error, [StackTrace? stackTrace]) {
    seen.add('forward $error');
    inner.addError(error, stackTrace);
  }
  @override
  Future addStream(Stream<String> stream) => inner.addStream(stream);
  @override
  Future close() => inner.close();
  @override
  Future get done => inner.done;
}

class IntSink implements EventSink<int> {
  int total = 0;
  @override
  void add(int event) => total += event;
  @override
  void addError(Object error, [StackTrace? stackTrace]) {}
  @override
  void close() => total *= 10;
}

void feed(EventSink<String> s) {
  s.add('one');
  s.addError('boom');
  s.addError('bang', StackTrace.current);
}

Future<void> main() async {
  final log = LogSink();
  feed(log);
  log.close();
  log.log.forEach(print);

  final f = ForwardSink();
  final got = <String>[];
  f.inner.stream.listen(got.add, onError: (e) => got.add('listened $e'));
  StreamSink<String> asSink = f;
  asSink.add('a');
  asSink.addError('oops', StackTrace.current);
  asSink.addError('plain');
  await f.close();
  f.seen.forEach(print);
  got.forEach(print);

  // A tear-off as Future.then's onError.
  final c = Completer<Object?>();
  final failing = Future<int>.error('late failure');
  failing.then((v) => c.complete(v), onError: c.completeError);
  try {
    await c.future;
  } catch (e) {
    print('completer got $e');
  }
  final c2 = Completer<Object?>();
  final Future<int> seven = Future.value(7);
  seven.then(c2.complete, onError: c2.completeError);
  print('value ${await c2.future}');
  final doubled = await seven.then((v) => v * 2);
  print('doubled ${doubled + 1}');

  // An int sink: add(int) must implement EventSink<int>.add.
  final ints = IntSink();
  EventSink<int> asInts = ints;
  asInts.add(3);
  asInts.add(4);
  asInts.close();
  print('ints ${ints.total}');

  // Future.wait over a list built from dynamic values.
  final List<dynamic> pending = [Future.value('x'), Future.delayed(Duration.zero, () => 'y')];
  final List<Future> fs = [for (final p in pending) p as Future];
  print('waited ${await Future.wait(fs)}');
}

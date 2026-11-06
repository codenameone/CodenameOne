import 'dart:async';
import 'dart:io' as io;

/// A package-side class sharing a runtime type's simple name.
class FileSystemException implements Exception {
  final String message;
  FileSystemException(this.message);
}

/// A consumer whose close() narrows the future, as an IOSink's request does.
class Collector implements StreamConsumer<List<int>> {
  final List<int> bytes = [];
  @override
  Future addStream(Stream<List<int>> stream) => stream.forEach(bytes.addAll);
  @override
  Future<String> close() async => 'collected ${bytes.length}';
}

class Greeter {
  static String greet(String who, {String punct = '!'}) => 'hi $who$punct';
}

const greet = Greeter.greet;

String describe(Object e) => switch (e) {
      io.FileSystemException(message: final m) => 'runtime: $m',
      FileSystemException(message: final m) => 'program: $m',
      _ => 'other',
    };

/// Forwards a transformed stream into a typed sink, as a wrapper sink does.
class Forwarder<S, T> {
  final StreamController<S> _in = StreamController<S>();
  final StreamSink<T> _out;
  Forwarder(StreamTransformer<S, T> transformer, this._out) {
    _in.stream.transform(transformer).listen(_out.add, onDone: _out.close);
  }
  void add(S v) => _in.add(v);
  Future close() => _in.close();
}

Future<void> main() async {
  final s = Stream<List<int>>.fromIterable([
    [1, 2],
    [3]
  ]);
  final result = await s.pipe(Collector());
  print(result);
  final c = StreamController<List<int>>();
  final got = <int>[];
  c.stream.listen(got.addAll, onDone: () => print('done $got'));
  c.add([4, 5]);
  await c.close();
  print(describe(io.FileSystemException('disk')));
  print(describe(FileSystemException('mine')));
  final out = StreamController<String>();
  final seen = <String>[];
  final finished = out.stream.listen(seen.add).asFuture();
  final f = Forwarder<int, String>(
      StreamTransformer<int, String>.fromHandlers(
          handleData: (v, sink) => sink.add('<$v>')),
      out);
  f.add(7);
  f.add(8);
  await f.close();
  await finished;
  print(seen);
  print(greet('bob'));
  print(greet('amy', punct: '?'));
}

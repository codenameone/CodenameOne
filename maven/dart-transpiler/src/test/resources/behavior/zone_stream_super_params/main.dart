import 'dart:async';

// A positional super parameter forwarded to a runtime (StreamView) superclass.
class Wrapped<T> extends StreamView<T> {
  Wrapped(super.stream);
}

class Bytes extends StreamView<List<int>> {
  const Bytes(super.stream);
  Future<int> total() => fold<int>(0, (a, b) => a + b.length);
}

abstract class Converting<S, T> {
  factory Converting.fromFunction(T Function(S) f) = _FnConverting<S, T>;
  Sink<S> bind(Sink<T> target);
}

class _FnConverting<S, T> implements Converting<S, T> {
  final T Function(S) f;
  _FnConverting(this.f);
  @override
  Sink<S> bind(Sink<T> target) => _MapSink<S, T>(f, target);
}

class _MapSink<S, T> implements Sink<S> {
  final T Function(S) f;
  final Sink<T> target;
  _MapSink(this.f, this.target);
  @override
  void add(S data) => target.add(f(data));
  @override
  void close() => target.close();
}

class Collect implements Sink<String> {
  final List<String> seen = [];
  @override
  void add(String data) => seen.add(data);
  @override
  void close() => seen.add('closed');
}

String Function(int) twice(String Function(int, String) f) => (n) => f(n, '$n$n');

Future<void> main() async {
  // The closure's parameter has the name of the local being declared.
  var label = twice((n, label) => 'label:$label/$n');
  print(label(3));

  final w = Wrapped<int>(Stream.fromIterable([1, 2, 3]));
  print(await w.toList());
  final b = Bytes(Stream.fromIterable([
    [1, 2],
    [3]
  ]));
  print(await b.total());

  final out = Collect();
  var sink = Converting<int, String>.fromFunction((n) => 'n=$n').bind(out);
  sink.add(4);
  sink.add(5);
  sink.close();
  print(out.seen);
}

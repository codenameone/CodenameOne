import 'dart:async';

// A sink whose two type parameters are named the opposite way round from
// the library's own StreamSink<S>/StreamController<T> declarations.
class SwappedForwardingSink<S, T> implements StreamSink<S> {
  final StreamController<S> _controller = StreamController<S>();
  final StreamSink<T> _inner;
  final StreamTransformer<S, T> _transformer;

  SwappedForwardingSink(this._transformer, this._inner) {
    _controller.stream.transform(_transformer).listen(_inner.add,
        onError: _inner.addError, onDone: () => _inner.close());
  }

  @override
  void add(S event) => _controller.add(event);

  @override
  void addError(Object error, [StackTrace? stackTrace]) =>
      _controller.addError(error, stackTrace);

  @override
  Future addStream(Stream<S> stream) => _controller.addStream(stream);

  Future forwardTransformed(Stream<S> stream) =>
      _inner.addStream(stream.transform(_transformer));

  @override
  Future close() {
    _controller.close();
    return _inner.done;
  }

  @override
  Future get done => _inner.done;
}

class PairCombiner<S, T> {
  final StreamTransformer<T, S> _back;
  PairCombiner(this._back);
  Stream<S> reversed(Stream<T> input) =>
      identityStream<S>(input.transform(_back));
}

Stream<T> identityStream<T>(Stream<T> s) => s;

class Keyed<K, V> {
  final Map<K, V> entries = {};
  void removeAll(Iterable<K> keys) {
    for (final k in keys) {
      entries.remove(k);
    }
  }
}

Future<void> main() async {
  final collected = <String>[];
  final out = StreamController<String>();
  out.stream.listen(collected.add);
  final sink = SwappedForwardingSink<int, String>(
      StreamTransformer<int, String>.fromHandlers(
          handleData: (v, s) => s.add('n$v')),
      out);
  sink.add(1);
  await sink.addStream(Stream.fromIterable([2, 3]));
  await sink.close();
  print('swapped sink: $collected');

  final lengths = PairCombiner<int, String>(
      StreamTransformer<String, int>.fromHandlers(
          handleData: (v, s) => s.add(v.length)));
  print('reversed: ${await lengths.reversed(Stream.fromIterable(['ab', 'cde'])).toList()}');

  final keyed = Keyed<Object, int>();
  keyed.entries['a'] = 1;
  keyed.entries['b'] = 2;
  keyed.entries[3] = 3;
  final dynamic box = keyed;
  final List<String> names = ['a', 'b'];
  (box as Keyed<Object, int>).removeAll(names);
  print('keyed left: ${keyed.entries}');

}

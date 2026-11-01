import 'dart:async';
import 'dart:convert';

// A converter whose chunked sink parameter is spelled `Sink`.
class Doubler extends Converter<List<int>, List<int>> {
  const Doubler();
  @override
  List<int> convert(List<int> input) => [for (final b in input) b * 2];
  @override
  DoublerSink startChunkedConversion(Sink<List<int>> sink) => DoublerSink(sink);
}

class DoublerSink implements Sink<List<int>> {
  final Sink<List<int>> _out;
  DoublerSink(this._out);
  @override
  void add(List<int> chunk) => _out.add([for (final b in chunk) b * 2]);
  @override
  void close() => _out.close();
}

class Collect implements Sink<List<int>> {
  final List<int> got = [];
  @override
  void add(List<int> data) => got.addAll(data);
  @override
  void close() => print('collected $got');
}

// A StreamView of byte chunks handed to a converter as a stream.
class Bytes extends StreamView<List<int>> {
  Bytes(Stream<List<int>> s) : super(s);
  Stream<String> asText() => utf8.decoder.bind(this);
}

class Pair<T> {
  final T a;
  final T b;
  Pair(this.a, this.b);
}

class Dedup<T> extends Iterable<T> {
  final Iterable<T> _src;
  Dedup(this._src);
  @override
  Iterator<T> get iterator => _src.toSet().iterator;
}

class Joined<T> extends Iterable<T> {
  final Iterable<Iterable<T>> _parts;
  Joined(this._parts);
  @override
  Iterator<T> get iterator => _parts.expand((p) => p).iterator;
}

class Maps<K, V> {
  final List<Map<K, V>> _maps;
  Maps(this._maps);
  Iterable<K> get keys => Dedup(Joined(_maps.map((m) => m.keys)));
}

abstract class Chan<T> {
  Stream<T> get stream;
}

class ControllerChan<T> implements Chan<T> {
  final StreamController<T> c = StreamController<T>();
  @override
  Stream<T> get stream => c.stream;
}

class Mux<T> {
  final StreamController<T> _in = StreamController<T>();
  final ControllerChan<dynamic> raw = ControllerChan<dynamic>();
  Chan<dynamic>? _chan;
  final List<String> seen = [];
  void start() {
    _chan = raw;
    _in.stream.cast<List<dynamic>>().listen((message) {
      if (message.length != 2) return;
      seen.add('${message[0]}:${message[1]}');
    });
    _chan!.stream.cast<List>().listen((message) {
      final id = (message[0] as num).toInt();
      seen.add('raw $id/${message.length}');
    });
  }
}

Future<void> main() async {
  final c = Collect();
  final s = const Doubler().startChunkedConversion(c);
  s.add([1, 2]);
  s.close();
  print(const Doubler().convert([3, 4]));

  final text = await Bytes(Stream.fromIterable([utf8.encode('he'), utf8.encode('llo')])).asText().join();
  print(text);

  print(Maps<String, int>([{'a': 1, 'b': 2}, {'b': 3, 'c': 4}]).keys.toList());

  final m = Mux<Object>();
  m.start();
  m._in.add([1, 'x']);
  m._in.add([2]);
  m._in.add(['y', 3]);
  m.raw.c.add([7, 'z']);
  m.raw.c.add([8.9]);
  await Future<void>.delayed(Duration.zero);
  print(m.seen..sort());
}

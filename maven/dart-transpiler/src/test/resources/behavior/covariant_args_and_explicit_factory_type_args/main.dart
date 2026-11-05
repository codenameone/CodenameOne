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

abstract class KeyStore<E> {
  Future<void> dropAll(Iterable<dynamic> keys);
}

class MemoryKeyStore<E> implements KeyStore<E> {
  final Map<dynamic, E> values;
  MemoryKeyStore(this.values);
  @override
  Future<void> dropAll(Iterable<dynamic> keys) async {
    for (final k in keys) {
      values.remove(k);
    }
  }
}

class PrefixedKeys {
  final KeyStore _store;
  PrefixedKeys(this._store);
  String _prefixed(String k) => 'p.' + k;
  Future<KeyStore> _open() async => _store;
  Future<void> dropAll(List<String> keys) async {
    final prefixed = keys.map(_prefixed);
    final store = await _open();
    await store.dropAll(prefixed);
  }
}

class Channel<T> {
  final Stream<T> stream;
  final List<T> sent;
  Channel._(this.stream, this.sent);
  factory Channel.guarded(Stream<T> stream, List<T> sent) =>
      Channel<T>._(stream.where((e) => true), sent);
  static Channel<T> make<T>(Stream<T> stream) => Channel<T>._(stream, <T>[]);
}

class ChannelMapper<S, T> {
  final StreamTransformer<T, S> _down;
  ChannelMapper(this._down);
  Channel<S> bind(Channel<T> channel) =>
      Channel<S>.guarded(channel.stream.transform(_down), <S>[]);
  Channel<S> bindStatic(Channel<T> channel) =>
      Channel.make<S>(channel.stream.transform(_down));
}

int Function() bumpedReader(int x) => (x += 10) > 0 ? () => x * 2 : () => -x;
String Function(String) suffixer(String s) => (s = s.toUpperCase()).isEmpty ? (t) => t : (t) => s + t;

Never failWith(String what) {
  throw StateError('failed: ' + what);
}

class Checker {
  Never expectedThing(String name) {
    failWith('expected ' + name);
  }
}

class Reply {
  final int code;
  Reply(this.code);
}

class ReplyWithUrl extends Reply {
  final String url;
  ReplyWithUrl(super.code, this.url);
}

Future<String> describeReply(Reply r) async {
  try {
    if (r case ReplyWithUrl(:final url)) {
      return 'url ' + url + ' ' + r.code.toString();
    } else {
      return 'plain ' + r.code.toString();
    }
  } catch (e) {
    rethrow;
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

  final memory = MemoryKeyStore<int>({'p.x': 1, 'p.y': 2, 'z': 3});
  await PrefixedKeys(memory).dropAll(['x', 'y']);
  print('prefixed left: ${memory.values}');

  final mapper = ChannelMapper<int, String>(
      StreamTransformer<String, int>.fromHandlers(
          handleData: (v, s) => s.add(v.length * 100)));
  final mapped = mapper.bind(Channel<String>.guarded(Stream.fromIterable(['a', 'bb']), <String>[]));
  print('factory mapped: ${await mapped.stream.toList()} sent ${mapped.sent}');
  final mappedStatic = mapper.bindStatic(Channel.make<String>(Stream.fromIterable(['ccc'])));
  print('static mapped: ${await mappedStatic.stream.toList()}');

  print('arrow reassigned capture: ${bumpedReader(1)()} ${bumpedReader(-20)()}');
  print('arrow string capture: ${suffixer('ab')('-x')}');
  try {
    Checker().expectedThing('digit');
  } on StateError catch (e) {
    print('never body: ${e.message}');
  }
  print('if-case async: ${await describeReply(ReplyWithUrl(200, '/a'))} / ${await describeReply(Reply(404))}');
}

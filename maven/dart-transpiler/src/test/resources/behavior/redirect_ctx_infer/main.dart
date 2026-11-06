import 'dart:async';
import 'dart:convert';

class Union<E> {
  final Set<Set<E>> sets;
  final bool disjoint;
  Union(Set<Set<E>> sets, {bool disjoint = false})
      : sets = sets,
        disjoint = disjoint;
  Union.from(Iterable<Set<E>> sets, {bool disjoint = false})
      : this(sets.toSet(), disjoint: disjoint);
  int get count => sets.fold(0, (a, s) => a + s.length);
}

class Pair<S, T> {
  final Converter<T, S> decode;
  final Converter<S, T> encode;
  const Pair(this.decode, this.encode);
  Pair.fromCodec(Codec<S, T> codec) : this(codec.decoder, codec.encoder);
}

class Chan<T> {
  final Stream<T> stream;
  final StreamSink<T> sink;
  Chan(this.stream, this.sink);
}

Chan<T> wrap<T>(Stream<Object?> input, StreamSink<T> out) =>
    Chan(input.cast(), StreamController(sync: true)..stream.cast<T>().pipe(out));

Future<void> main() async {
  var u = Union<int>.from([{1, 2}, {3}], disjoint: true);
  print('${u.count} ${u.disjoint}');
  var p = Pair<String, List<int>>.fromCodec(utf8);
  print("${p.encode.convert("ab")} ${p.decode.convert([104, 105])}");
  var got = StreamController<String>();
  var c = wrap<String>(Stream<Object?>.fromIterable(['x', 'y']), got.sink);
  var seen = <String>[];
  var done = got.stream.listen(seen.add).asFuture();
  c.sink.add('one');
  c.sink.add('two');
  await c.sink.close();
  await done;
  print(seen.join(','));
  print(await c.stream.join('-'));
}

import 'dart:async';

class Chan<T> {
  final Stream<T> stream;
  final StreamSink<T> sink;
  Chan(this.stream, this.sink);

  Chan<S> cast<S>() => Chan<S>(stream.cast<S>(),
      StreamController<S>(sync: true)..stream.cast<T>().pipe(sink));
}

class Lazy<T> {
  final Object Function() _make;
  Stream<T>? stream;
  Lazy(this._make);

  Stream<T> get value {
    var result = _make();
    if (result is Future<Stream<T>>) {
      throw StateError('future');
    } else {
      stream = result as Stream<T>;
    }
    return stream!;
  }
}

class Lazy2<T> {
  final FutureOr<Stream<T>> Function() _make;
  Stream<T>? stream;
  Lazy2(this._make);

  Stream<T> get value {
    var result = _make();
    if (result is Future<Stream<T>>) {
      throw StateError('future');
    } else {
      stream = result;
    }
    return stream!;
  }
}

Future<void> main() async {
  final out = StreamController<Object>();
  final c = Chan<Object>(Stream<Object>.fromIterable([1, 2, 3]), out.sink);
  final Chan<int> ints = c.cast<int>();
  print(await ints.stream.toList());
  final seen = <Object>[];
  out.stream.listen(seen.add);
  ints.sink.add(4);
  ints.sink.add(5);
  await Future<void>.delayed(Duration(milliseconds: 10));
  print(seen);
  print(await Lazy<int>(() => Stream<int>.value(9)).value.toList());
  print(await Lazy2<int>(() => Stream<int>.value(10)).value.toList());
}

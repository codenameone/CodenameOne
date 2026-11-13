import 'dart:async';
import 'dart:typed_data';

class Wrapped<T> {
  final T value;
  Wrapped(this.value);
  String toString() => 'Wrapped($value)';
}

// The class type variable of Completer (T) shares its name with this method's T.
Future<List<Wrapped<T>>> collect<T>(Iterable<FutureOr<T>> items) {
  var results = <Wrapped<T>?>[];
  var pending = 0;
  late Completer<List<Wrapped<T>>> completer;
  for (var item in items) {
    if (item is Future<T>) {
      var i = results.length;
      results.add(null);
      pending++;
      item.then((v) {
        results[i] = Wrapped<T>(v);
        if (--pending == 0) {
          completer.complete(List.from(results));
        }
      });
    } else {
      results.add(Wrapped<T>(item));
    }
  }
  if (pending == 0) {
    return Future.value(List.from(results));
  }
  completer = Completer<List<Wrapped<T>>>();
  return completer.future;
}

T finish<T>(Stream<List<int>> source, T Function(StreamSubscription<List<int>>, Future<Uint8List>) result) {
  var out = BytesBuilder(copy: false);
  var completer = Completer<Uint8List>.sync();
  var sub = source.listen(out.add, onError: completer.completeError, onDone: () {
    completer.complete(out.takeBytes());
  }, cancelOnError: true);
  return result(sub, completer.future);
}

class Chunks<T> {
  final List<T> items;
  Chunks(this.items);
  Stream<List<T>> chunks(int n) async* {
    for (var i = 0; i < items.length; i += n) {
      yield items.sublist(i, i + n > items.length ? items.length : i + n);
    }
  }
}

Future<Uint8List> gather(Chunks<int> c) => finish(c.chunks(2), (_, f) => f);

Future<void> main() async {
  var a = await collect<int>([1, 2, 3]);
  print(a);
  var b = await collect<String>([Future.value('x'), 'y', Future.value('z')]);
  print(b);
  var bytes = await gather(Chunks<int>([1, 2, 3, 4, 5]));
  print(bytes);
  var len = await finish(Stream.fromIterable([[7, 8], [9]]), (_, f) => f.then((b) => b.length));
  print(len);
}

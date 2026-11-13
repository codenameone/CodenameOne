import 'dart:async';

class Box<E> {
  final List<E> items;
  Box(this.items);
  List<T> flat<T>(Iterable<T> Function(E) f) => [for (var e in items) ...f(e)];
}

Iterable<T> expandAll<E, T>(List<E> xs, Iterable<T> Function(E) f) =>
    xs.expand(f);

extension Pairs<T> on Stream<T> {
  Stream<List<T>> pairs() {
    var slice = <T>[];
    return transform(StreamTransformer.fromHandlers(handleData: (data, sink) {
      slice.add(data);
      if (slice.length == 2) {
        sink.add(List<T>.of(slice));
        slice = [];
      }
    }, handleDone: (sink) {
      if (slice.isNotEmpty) sink.add(slice);
      sink.close();
    }));
  }
}

void main() async {
  var box = Box<int>([1, 2]);
  print(box.flat((e) => [e, e * 10]));
  print(expandAll([1, 2], (int e) => ['a$e', 'b$e']).toList());
  Iterable<String> Function(int) twice = (n) => ['x$n', 'y$n'];
  print(twice(3).join(','));
  print(await Stream.fromIterable([1, 2, 3]).pairs().toList());
}

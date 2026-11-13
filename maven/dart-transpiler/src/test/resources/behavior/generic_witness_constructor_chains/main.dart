// @dart=2.19
import 'dart:async';

abstract class Pile<E> {
  int get count;
  String describe() => 'Pile<$E> of $count';
}

// Only a factory: no generative constructor, yet it extends a generic class.
abstract class NamedPile<E> extends Pile<E> {
  factory NamedPile(List<E> items) => ListPile<E>(items);
  String get label;
}

// A plain class mixed in first becomes the superclass.
abstract class Tagging<E> {
  String tag() => 'tag<$E>';
}

class ListPile<E> with Tagging<E> implements NamedPile<E> {
  final List<E> items;
  ListPile(this.items);
  int get count => items.length;
  String get label => 'list';
  String describe() => 'ListPile<$E> of $count';
}

class Gate {
  Future<T> pass<T>(Future<T> Function() task) {
    final done = Completer();
    return Future.value().then((_) => task()).whenComplete(done.complete);
  }
}

abstract class Pipe<T> {
  factory Pipe(Stream<T> input, StreamSink<T> output) = SimplePipe<T>;
  Stream<T> get input;
  StreamSink<T> get output;
  Pipe<S> retype<S>();
}

class SimplePipe<T> implements Pipe<T> {
  final Stream<T> input;
  final StreamSink<T> output;
  SimplePipe(this.input, this.output);
  Pipe<S> retype<S>() => Pipe(
      input.cast(), StreamController(sync: true)..stream.cast<T>().listen((_) {}));
}

Future<dynamic> readThrough(Gate g) {
  return g.pass(() async => 'read');
}

void main() async {
  final p = NamedPile<int>([1, 2, 3]);
  print(p.describe());
  print(p.label);
  print((p as ListPile<int>).tag());
  print(p.runtimeType);
  print(p is Tagging<int>);
  print(await readThrough(Gate()));
  final pipe = Pipe<Object>(Stream.fromIterable(['a', 'b']), StreamController());
  final typed = pipe.retype<String>();
  print(await typed.input.join(','));
  print(typed is Pipe<String>);
}

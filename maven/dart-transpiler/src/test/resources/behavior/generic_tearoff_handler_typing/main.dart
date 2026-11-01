// Handler and tear-off typing through a class type parameter, a raw generic type
// as a list element, whereType with a parameterized type, and a generic factory
// whose type arguments come from the expected return type.
import 'dart:async';

class Box<T> {
  final Future<T> source;
  Box(this.source);

  Future<T> relay() {
    final completer = Completer<T>.sync();
    source.then(completer.complete, onError: completer.completeError);
    return completer.future;
  }

  Future<T?> firstOf(Stream<T> s) {
    final completer = Completer<T?>.sync();
    late StreamSubscription<T> sub;
    sub = s.listen(null, onError: completer.completeError, onDone: completer.complete,
        cancelOnError: true);
    sub.onData((event) {
      sub.cancel().whenComplete(() {
        completer.complete(event);
      });
    });
    return completer.future;
  }
}

extension FirstOf<T> on Stream<T> {
  Future<T?> get firstOrNothing {
    var completer = Completer<T?>.sync();
    final subscription = listen(null,
        onError: completer.completeError, onDone: completer.complete, cancelOnError: true);
    subscription.onData((event) {
      subscription.cancel().whenComplete(() {
        completer.complete(event);
      });
    });
    return completer.future;
  }
}

class Relay<T> {
  final T value;
  Relay(this.value);
  String fire() => 'relay $value';
}

String? _fireOne(Relay<Object?> r) => r.value == null ? null : r.fire();

class Hub {
  List<Relay>? relays;
  List<String> fireAll() => [...?relays?.map(_fireOne).nonNulls];
}

abstract class Pipe<T> {
  factory Pipe(List<T> items, StreamController<T> out) => _Pipe<T>(items, out);
  List<T> get items;
  StreamController<T> get out;
}

class _Pipe<T> implements Pipe<T> {
  @override
  final List<T> items;
  @override
  final StreamController<T> out;
  _Pipe(this.items, this.out);
}

mixin PipeMixin<T> implements Pipe<T> {
  Pipe<S> cast<S>() => Pipe(items.cast(), StreamController(sync: true)..stream.cast<T>().listen(out.add));
}

class NumPipe with PipeMixin<num> {
  @override
  final List<num> items;
  @override
  final StreamController<num> out = StreamController<num>(sync: true);
  NumPipe(this.items);
}

Future<void> main() async {
  print(await Box<String>(Future.value('hi')).relay());
  print(await Box<int>(Future.value(7)).relay());
  print(await Box<int>(Stream<int>.empty().first.catchError((_) => -1)).relay());
  print(await Box<String>(Future.value('x')).firstOf(Stream.fromIterable(['a', 'b'])));
  print(await Box<int>(Future.value(0)).firstOf(const Stream<int>.empty()));

  print(await Stream.fromIterable([4, 5]).firstOrNothing);
  print(await const Stream<String>.empty().firstOrNothing);

  final h = Hub();
  print(h.fireAll());
  h.relays = [Relay<int>(1), Relay<String>('b'), Relay<Object?>(null)];
  print(h.fireAll());

  final List<Object> mixed = [Relay<int>(1), 'not a relay', 3, Relay<int>(2), <int>[5], <String>['s']];
  final ints = mixed.whereType<Relay<int>>().toList();
  print(ints.map((r) => r.value + 1).toList());
  print(mixed.whereType<Relay>().length);
  print(mixed.whereType<List<int>>().toList());
  print(mixed.whereType<List<String>>().toList());

  final p = NumPipe([1, 2.5, 3]);
  final got = <num>[];
  p.out.stream.listen(got.add);
  final c = p.cast<int>();
  print(c.items.length);
  c.out.add(4);
  await Future<void>.delayed(Duration.zero);
  print(got);
}

import 'dart:async';

typedef Thunk<R> = R Function();

abstract class Outcome<T> {}

class Success<T> implements Outcome<T> {
  final T value;
  Success(this.value);
  @override
  String toString() => 'Success($value)';
}

class Failure implements Outcome<Never> {
  final Object error;
  final StackTrace? trace;
  Failure(this.error, [this.trace]);
  @override
  String toString() => 'Failure($error)';
}

Future<Outcome<T>> settle<T>(Future<T> f) =>
    f.then(Success.new, onError: Failure.new);

class Holder<T> {
  final T? item;
  Holder(this.item);
}

int? _size(Holder<Object?> h) => h.item == null ? null : h.item.toString().length;

List<int> sizes(List<Holder<Object>>? hs) => [...?hs?.map(_size).nonNulls];

void _finish(EventSink sink) {
  sink.close();
}

class Doubler<T> {
  final void Function(T, EventSink<T>) onData;
  Doubler(this.onData);
  Stream<T> run(Stream<T> s) =>
      s.transform(StreamTransformer.fromHandlers(handleData: onData, handleDone: _finish));
}

class Tagging<T> implements EventSink<T> {
  final EventSink<String> out;
  Tagging(this.out);
  @override
  void add(T event) => out.add('<$event>');
  @override
  void addError(Object e, [StackTrace? st]) => out.addError(e, st);
  @override
  void close() => out.close();
}

class Untag implements EventSink<String> {
  final EventSink out;
  Untag(this.out);
  @override
  void add(String event) => out.add(event.substring(1, event.length - 1));
  @override
  void addError(Object e, [StackTrace? st]) => out.addError(e, st);
  @override
  void close() => out.close();
}

class Tagger<T> {
  Stream<String> bind(Stream<T> source) =>
      Stream<String>.eventTransformed(source, Tagging<T>.new);
}

class Untagger<T> {
  Stream<T> bind(Stream<String> source) => Stream<T>.eventTransformed(source, _make);
  static EventSink<String> _make(EventSink sink) => Untag(sink);
}

class Pipe<T> {
  final Stream<T> stream;
  Pipe(this.stream);
  Pipe<S> retype<S>() => Pipe(stream.cast());
}

class Serial {
  Future _last = Future.value();
  Future<T> run<T>(FutureOr<T> Function() task) {
    var previous = _last;
    var completer = Completer();
    _last = completer.future;
    return previous.then((_) {
      return Future.value(null).then((_) => task());
    }).whenComplete(completer.complete);
  }
}

T collect<T>(List<int> data, T Function(int, List<int>) result) {
  var copy = List<int>.of(data);
  return result(copy.length, copy);
}

class Repeater {
  final Thunk _callback;
  Timer? timer;
  Repeater(this._callback) : timer = Timer(Duration.zero, _callback);
  void again() {
    timer = Timer(Duration.zero, _callback);
  }
}

class Counter {
  var count = 0;
  Counter() {
    var step = 1;
    void bump() => count += step;
    bump();
    step = 10;
    bump();
  }
}

void main() async {
  print(await settle(Future.value(3)));
  print(await settle(Future<int>.error('bad')));
  print(sizes([Holder<Object>('abc'), Holder<Object>(12)]));
  print(sizes(null));
  print(await Doubler<int>((d, s) => s.add(d * 2)).run(Stream.fromIterable([1, 2])).toList());
  var tagged = Tagger<int>().bind(Stream.fromIterable([7, 8]));
  print(await Untagger<String>().bind(tagged).toList());
  print(await Pipe<Object>(Stream.fromIterable(['x', 'y'])).retype<String>().stream.toList());
  var serial = Serial();
  print(await serial.run(() => 'one'));
  print(await serial.run(() async => 2));
  print(collect([4, 5, 6], (n, xs) => '$n:${xs.last}'));
  var ticks = 0;
  var rep = Repeater(() => ticks++);
  await Future.delayed(const Duration(milliseconds: 10));
  rep.again();
  await Future.delayed(const Duration(milliseconds: 10));
  print('ticks $ticks');
  print(Counter().count);
  var label = 'a';
  String read() => label;
  label = 'b';
  print(read());
}

import 'dart:async';

class Holder<T> {
  final Completer<T> _c = Completer<T>();
  Future<T> get future => _c.future;

  // A FutureOr<T> parameter takes a plain T or a Future<T>.
  void complete(FutureOr<T> value) {
    if (value is! Future) {
      _c.complete(value);
      return;
    }
    (value as Future<T>).then((v) => _c.complete(v));
  }
}

FutureOr<int> maybeAsync(bool async) => async ? Future.value(2) : 1;

Future<void> main() async {
  final a = Holder<String>();
  a.complete('plain');
  print(await a.future);
  final b = Holder<String>();
  b.complete(Future.value('later'));
  print(await b.future);
  print(await maybeAsync(false));
  print(await maybeAsync(true));
  FutureOr<int> x = 5;
  print(x is Future);
  print(await x);
}

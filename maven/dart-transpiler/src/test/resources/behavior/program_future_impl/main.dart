import 'dart:async';

class Relay<T> implements Future<T> {
  final Future<T> _inner;
  Relay(this._inner);

  static Future<T> wrap<T>(Future future) =>
      future is Future<T> ? future : future.then((v) => v as T);

  @override
  Stream<T> asStream() => _inner.asStream();

  @override
  Future<T> catchError(Function onError, {bool Function(Object error)? test}) =>
      _inner.catchError(onError, test: test);

  @override
  Future<S> then<S>(FutureOr<S> Function(T) onValue, {Function? onError}) =>
      _inner.then(onValue, onError: onError);

  @override
  Future<T> whenComplete(FutureOr Function() action) =>
      _inner.whenComplete(action);

  @override
  Future<T> timeout(Duration timeLimit, {FutureOr<T> Function()? onTimeout}) =>
      _inner.timeout(timeLimit, onTimeout: onTimeout);
}

Future<void> main() async {
  final r = Relay<int>(Future.value(41));
  print(await r.then((v) => v + 1));
  print(await r);
  final s = await Relay<String>(Future.value('x')).asStream().toList();
  print(s);
  final e = Relay<int>(Future.error('boom'));
  print(await e.catchError((err) => -1));
  var done = false;
  await Relay<int>(Future.value(1)).whenComplete(() => done = true);
  print(done);
  print(await Relay.wrap<int>(Future.value(7)));
}

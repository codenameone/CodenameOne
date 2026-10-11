// Codename One Flutter runtime API stubs -- dart:async Stream support.
//
// Stream<T>/StreamSubscription<T>/StreamController<T>, the minimal subset
// needed for platform channels (EventChannel.receiveBroadcastStream returns
// a Stream) and for application code that creates its own. See
// dart.async.Stream's class comment (maven/dart-runtime) for exactly what is
// and is not implemented.
//
// Same conventions as flutter_material.dart's header:
//  - positional constructor parameters  -> Java constructor arguments
//  - named constructor parameters       -> void setter methods of the same name
//  - instance getters                   -> no-arg method calls (name())
//  - named parameters of methods        -> canonical positional order as declared
//  - most callback-shaped parameters are typed Object here (dynamic in Dart
//    terms); the runtime class behind them dispatches with instanceof.
//    listen()'s onData is the one exception -- real Dart types it strictly
//    void, and outside a few names the emitter special-cases (Future.then
//    et al.), a Dart closure literal transpiles to a bare, untyped Java
//    lambda, so the REAL Java parameter (dart.async.Stream, not this stub)
//    must be a genuine Funcs functional interface for it to compile at all.

@JavaName('dart.async.StreamSubscription')
abstract class StreamSubscription<T> {
  external Future<void> cancel();
  external void onData(Object? handleData);
  external void onError(Object? handleError);
  external void onDone(Object? handleDone);
  external void pause();
  external void resume();
  external bool get isPaused;
}

@JavaName('dart.async.Stream')
abstract class Stream<T> {
  external StreamSubscription<T> listen(Object? onData,
      {Object? onError, Object? onDone, bool? cancelOnError});
  external Stream<R> map<R>(Object transform);
  external Stream<T> where(Object test);
}

@JavaName('dart.async.StreamController')
class StreamController<T> {
  external StreamController(
      {Object? onListen, Object? onPause, Object? onResume, Object? onCancel, bool? sync});
  // Dart's `StreamController<T>.broadcast(...)` named constructor, declared
  // as a static factory method (see flutter_material.dart's EdgeInsets.all
  // for the same convention). The return type is left raw rather than
  // `StreamController<T>` to avoid depending on a class-instantiated static
  // member access (`StreamController<int>.broadcast()`); write
  // `StreamController<int>.broadcast()` as `StreamController.broadcast()`
  // with the target's declared type carrying the type argument instead.
  external static StreamController broadcast(
      {Object? onListen, Object? onCancel, bool? sync});
  external Stream<T> get stream;
  external bool get isClosed;
  external bool get hasListener;
  external void add(T event);
  external void addError(Object error);
  external Future<void> close();
}

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
//  - every callback-shaped parameter here stays Object/dynamic, matching
//    the convention of every other stub's optional callbacks -- the Java
//    SIGNATURE that actually receives a transpiled closure lives in
//    dart.async.Stream/StreamSubscription/StreamController (maven/dart-runtime),
//    never in this file. Outside a few names the emitter special-cases
//    (Future.then et al.), a Dart closure literal transpiles to a bare,
//    untyped Java lambda with no wrapping, so whatever REAL Java parameter
//    it lands on must be a genuine Funcs functional interface or the
//    generated call fails to compile -- a plain Object parameter is not
//    one. listen()'s onData, map()'s transform, where()'s test and
//    listen()'s onDone are all concretely typed on the Java side for this
//    reason. onError stays Object even there -- real Dart's untyped
//    onError accepts a one- or a two-argument handler, and the stub
//    convention pads an omitted trailing named parameter with a bare null,
//    which would be ambiguous between a one- and a two-argument overload --
//    so the emitter itself pins a transpiled onError: closure to the Funcs
//    type its own arity needs before the call, the same way it pins
//    Future.then's onError:. See dart.async.Stream's class comment for the
//    full detail.

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

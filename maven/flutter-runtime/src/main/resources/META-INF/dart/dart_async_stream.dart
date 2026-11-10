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
//  - the callbacks a closure's parameter is read from (listen's onData, map,
//    where, forEach, ...) are typed, so `stream.listen((xs) => xs.contains(1))`
//    knows xs; the rest stay Object/dynamic -- the Java
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

@JavaName('dart.async.TimeoutException')
class TimeoutException implements Exception {
  external TimeoutException(String? message, [Duration? duration]);
  external String? get message;
  external Duration? get duration;
}

@JavaName('dart.async.StreamSubscription')
abstract class StreamSubscription<T> {
  external Future<void> cancel();
  external void onData(Function? handleData);
  external void onError(Function? handleError);
  external void onDone(Function? handleDone);
  external void pause([Future<void>? resumeSignal]);
  external void resume();
  external bool get isPaused;
  external Future<E> asFuture<E>([E? futureValue]);
}

@JavaName('dart.async.Stream')
abstract class Stream<T> {
  external static Stream<T> eventTransformed<T>(Stream source, dynamic Function(dynamic) mapSink);
  external Future pipe(StreamConsumer<T> streamConsumer);
  external StreamSubscription<T> listen(void Function(T event)? onData,
      {Function? onError, void Function()? onDone, bool? cancelOnError});
  external Stream<R> map<R>(R Function(T event) transform);
  external Stream<T> where(bool Function(T event) test);
  // Named constructors, as static factories spelled with the class's own T: the emitter
  // binds it from the call's arguments (bindStaticClassTypeParams), so
  // `Stream.fromIterable([1, 2])` is a Stream<int>, as Dart's constructor infers. A raw
  // return made every element dynamic. Never `fromIterable<T>`: a method T is not bound.
  external static Stream<T> fromIterable(Iterable<T> elements);
  external static Stream<T> value(T value);
  external static Stream<T> empty();
  external static Stream<T> error(Object error);
  external static Stream<T> fromFuture(Future<T> future);
  external static Stream<T> fromFutures(Iterable<Future<T>> futures);
  external static Stream<T> periodic(Duration period, [T Function(int)? computation]);
  external Stream<R> expand<R>(Iterable<R> Function(T) convert);
  external Stream<R> asyncMap<R>(dynamic Function(T) convert);
  external Stream<T> take(int count);
  external Stream<T> skip(int count);
  external Stream<T> takeWhile(bool Function(T element) test);
  external Stream<T> skipWhile(bool Function(T element) test);
  external Stream<T> distinct([bool Function(T, T)? equals]);
  external Stream<T> handleError(Function onError, {Object? test});
  external Stream<R> transform<R>(StreamTransformer<T, R> streamTransformer);
  external Stream<R> cast<R>();
  external Stream<T> asBroadcastStream({Object? onListen, Object? onCancel});
  external bool get isBroadcast;
  external Future<List<T>> toList();
  external Future<Set<T>> toSet();
  external Future<void> forEach(void Function(T element) action);
  external Future drain([Object? futureValue]);
  external Future<int> get length;
  external Future<bool> get isEmpty;
  external Future<T> get first;
  external Future<T> get last;
  external Future<T> get single;
  external Future<T> firstWhere(bool Function(T element) test, {Object? orElse});
  external Future<bool> any(bool Function(T element) test);
  external Future<bool> every(bool Function(T) test);
  external Future<bool> contains(Object? needle);
  external Future<String> join([String separator = ""]);
  external Future<S> fold<S>(S initialValue, S Function(S, T) combine);
  external Future<T> reduce(dynamic Function(T, T) combine);
  external Future<T> elementAt(int index);
}

@JavaName('dart.async.StreamTransformer')
abstract class StreamTransformer<S, T> {
  external factory StreamTransformer(
      StreamSubscription<T> Function(Stream<S> stream, bool cancelOnError) onListen);
  external Stream<T> bind(Stream<S> stream);
  // Typed with the class's S and T, which the call's expected type binds
  // (`stream.transform(StreamTransformer.fromHandlers(handleData: (data, sink) ..))`).
  external static StreamTransformer<S, T> fromHandlers(
      {void Function(S, EventSink<T>)? handleData, Function? handleError,
      void Function(EventSink<T>)? handleDone});
}


// Dart's StreamTransformerBase: a transformer that only supplies bind.
@JavaName('dart.async.StreamTransformerBase')
abstract class StreamTransformerBase<S, T> implements StreamTransformer<S, T> {
  external StreamTransformerBase();
  Stream<T> bind(Stream<S> stream);
}

typedef ZoneCallback<R> = R Function();
typedef ZoneUnaryCallback<R, T> = R Function(T);
typedef ZoneBinaryCallback<R, T1, T2> = R Function(T1, T2);

@JavaName('dart.async.StreamConsumer')
abstract class StreamConsumer<S> {
  external Future addStream(Stream<S> stream);
  external Future close();
}

@JavaName('dart.async.StreamSink')
abstract class StreamSink<S> implements StreamConsumer<S> {
  external void add(S event);
  external void addError(Object error, [StackTrace? stackTrace]);
  external Future addStream(Stream<S> stream);
  external Future close();
  external Future get done;
}

@JavaName('dart.async.StreamSink')
abstract class EventSink<T> {
  external void add(T event);
  external void addError(Object error, [StackTrace? stackTrace]);
  external void close();
}

@JavaName('dart.async.Sink')
abstract class Sink<T> {
  external void add(T data);
  external void close();
}

@JavaName('dart.async.StreamIterator')
class StreamIterator<T> {
  external StreamIterator(Stream<T> stream);
  external T get current;
  external Future<bool> moveNext();
  external Future cancel();
}

@JavaName('dart.async.Zone')
class Zone {
  external static Zone get current;
  external static Zone get root;
  external Zone? get parent;
  external dynamic operator [](Object? key);
  external Zone fork({ZoneSpecification? specification, Map<Object?, Object?>? zoneValues});
  external R Function() registerCallback<R>(R Function() callback);
  external R Function() bindCallback<R>(R Function() callback);
  external R Function(T) bindUnaryCallback<R, T>(R Function(T) callback);
  external R Function(T1, T2) bindBinaryCallback<R, T1, T2>(R Function(T1, T2) callback);
  external R run<R>(R Function() body);
  external R runUnary<R, T>(R Function(T) body, T argument);
  external void runGuarded(void Function() body);
  external void handleUncaughtError(Object error, StackTrace stackTrace);
  external void print(String line);
  external bool inSameErrorZone(Zone otherZone);
}

// What a ZoneSpecification handler calls to defer to the zones above it.
@JavaName('dart.async.ZoneDelegate')
class ZoneDelegate {
  external R run<R>(Zone zone, R Function() f);
  external R Function() registerCallback<R>(Zone zone, R Function() f);
  external void print(Zone zone, String line);
  external void handleUncaughtError(Zone zone, Object error, StackTrace stackTrace);
}

// A forked zone's handlers. handleUncaughtError, print, run and registerCallback
// are modelled; the timer and microtask interceptors are not.
@JavaName('dart.async.ZoneSpecification')
class ZoneSpecification {
  external ZoneSpecification(
      {Object? handleUncaughtError, Object? print, Object? run, Object? registerCallback});
  external static ZoneSpecification from(ZoneSpecification other,
      {Object? handleUncaughtError, Object? print, Object? run, Object? registerCallback});
}

// Dart's StreamView: forwards every event of the stream it wraps.
@JavaName('dart.async.StreamView')
class StreamView<T> extends Stream<T> {
  external StreamView(Stream<T> stream);
}

@JavaName('dart.async.Async.unawaited')
external void unawaited(Future<void>? future);

@JavaName('dart.async.Async.scheduleMicrotask')
external void scheduleMicrotask(void Function() callback);

@JavaName('dart.async.Async.runZoned')
external R runZoned<R>(R Function() body, {Map<Object?, Object?>? zoneValues, Object? zoneSpecification});

@JavaName('dart.async.Async.runZonedGuarded')
external R? runZonedGuarded<R>(R Function() body, void Function(Object error, StackTrace stack) onError,
    {Map<Object?, Object?>? zoneValues, Object? zoneSpecification});

@JavaName('dart.async.StreamController')
class StreamController<T> implements StreamSink<T> {
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
      {void Function()? onListen, void Function()? onCancel, bool? sync});
  external Stream<T> get stream;
  external bool get isClosed;
  external bool get hasListener;
  external void add(T event);
  external void addError(Object error, [StackTrace? stackTrace]);
  external Future<void> close();
  external StreamSink<T> get sink;
  external bool get isPaused;
  external Future get done;
  external Future addStream(Stream<T> source, {bool? cancelOnError});
  external void Function()? get onListen;
  external set onListen(void Function()? handler);
  external void Function()? get onCancel;
  external set onCancel(Object? handler);
  external void Function()? get onPause;
  external set onPause(void Function()? handler);
  external void Function()? get onResume;
  external set onResume(void Function()? handler);
}

@JavaName('dart.async.AsyncError')
class AsyncError {
  external AsyncError(Object error, [Object? stackTrace]);
  external Object get error;
  external Object get stackTrace;
  external static Object defaultStackTrace(Object error);
}

import 'dart:async';

// A two-way channel: a stream in, a sink out.
abstract class Channel<T> {
  Stream<T> get stream;
  StreamSink<T> get sink;
  Channel<S> retype<S>();
}

// The mixin every channel extends; implements the whole interface.
abstract mixin class ChannelMixin<T> implements Channel<T> {
  @override
  Channel<S> retype<S>() => PlainChannel<S>(stream.cast<S>(), sink as StreamSink<S>);
  void pipeTo(Channel<T> other) {
    stream.listen(other.sink.add);
  }
}

class PlainChannel<T> extends ChannelMixin<T> {
  @override
  final Stream<T> stream;
  @override
  final StreamSink<T> sink;
  PlainChannel(this.stream, this.sink);
}

// A sink with a narrower close, as a socket sink has.
class SocketSink implements StreamSink<dynamic> {
  final StreamController<dynamic> _out;
  SocketSink(this._out);
  @override
  void add(dynamic event) => _out.add('sock:$event');
  @override
  void addError(Object error, [StackTrace? stackTrace]) => _out.addError(error);
  @override
  Future addStream(Stream<dynamic> stream) => _out.addStream(stream);
  @override
  Future close([int? code]) {
    print('socket close $code');
    return _out.close();
  }
  @override
  Future get done => _out.done;
}

// Extends the mixin RAW (dynamic) and narrows sink's type covariantly.
abstract class SocketChannel extends ChannelMixin {
  @override
  SocketSink get sink;
}

class LoopSocket extends SocketChannel {
  final StreamController<dynamic> _wire = StreamController<dynamic>();
  late final SocketSink _sink = SocketSink(_wire);
  @override
  Stream get stream => _wire.stream;
  @override
  SocketSink get sink => _sink;
}

// A generic channel over many virtual channels, reading list messages.
class Multi<T> extends ChannelMixin<T> {
  final Channel<dynamic> _inner;
  final StreamController<T> _local = StreamController<T>();
  Multi(this._inner) {
    _inner.stream.cast<List>().listen((message) {
      final id = message[0] as int;
      _local.add(message[1] as T);
      print('multi got id $id');
    });
  }
  @override
  Stream<T> get stream => _local.stream;
  @override
  StreamSink<T> get sink => _local.sink;
}

// A handler that only knows the socket channel type.
class Handler {
  final SocketChannel channel;
  Handler(this.channel);
  void start() {
    channel.stream.listen((frame) => print('handler frame $frame'));
  }
  void send(String s) => channel.sink.add(s);
  Future stop() => channel.sink.close(1000);
}

// An interface that only adds to StreamSink: add/close are found through it.
abstract interface class FrameSink implements StreamSink<dynamic> {
  void addFrame(String frame);
}

class TaggedSink implements FrameSink {
  final StreamController<dynamic> _out;
  TaggedSink(this._out);
  @override
  void addFrame(String frame) => add('frame:$frame');
  @override
  void add(dynamic event) => _out.add(event);
  @override
  void addError(Object error, [StackTrace? stackTrace]) => _out.addError(error);
  @override
  Future addStream(Stream<dynamic> stream) => _out.addStream(stream);
  @override
  Future close() => _out.close();
  @override
  Future get done => _out.done;
}

Future<void> useFrameSink(FrameSink sink) async {
  sink.add('raw');
  sink.addFrame('f1');
  await sink.close();
}

Future<void> main() async {
  final sock = LoopSocket();
  final h = Handler(sock);
  h.start();
  h.send('hello');
  await Future<void>.delayed(Duration.zero);
  await h.stop();

  final ctl = StreamController<dynamic>();
  final m = Multi<String>(PlainChannel<dynamic>(ctl.stream, ctl.sink));
  m.stream.listen((s) => print('local $s'));
  ctl.add([1, 'one']);
  ctl.add([2, 'two']);
  await Future<void>.delayed(Duration.zero);

  final out = StreamController<int>();
  final p = PlainChannel<int>(Stream.fromIterable([4, 5]), out.sink);
  out.stream.listen((v) => print('piped $v'));
  final dyn = p.retype<int>();
  print('retyped ${await dyn.stream.toList()}');
  print(dyn is ChannelMixin<int>);
  print(sock.retype<dynamic>() is Channel);

  final framed = StreamController<dynamic>();
  framed.stream.listen((e) => print('framed $e'), onDone: () => print('framed done'));
  await useFrameSink(TaggedSink(framed));

  // Two plain controllers fed back to back: one event per microtask each.
  final a = StreamController<String>();
  final b = StreamController<String>();
  a.stream.listen(print);
  b.stream.listen(print);
  a.add('a1');
  a.add('a2');
  b.add('b1');
  b.add('b2');
  print('sync part done');
  await Future<void>.delayed(Duration.zero);
}

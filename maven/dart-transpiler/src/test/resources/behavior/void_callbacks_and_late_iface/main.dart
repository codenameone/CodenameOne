// A late field declared by an interface and implemented by a plain field, a
// null-aware call of a void method used as a statement, and a map of callbacks keyed
// by a null-asserted lookup.

abstract class Codec {
  late bool escape;
  String encode(String s);
}

class PlainCodec implements Codec {
  @override
  bool escape = false;
  @override
  String encode(String s) => escape ? s.replaceAll(':', '\\c') : s;
}

class Sink {
  final List<String> out = [];
  void add(String s) {
    out.add(s);
  }
}

class Channel {
  final Sink sink = Sink();
}

class Dispatcher {
  final Function(String) onFrame;
  final void Function() onDone;
  Dispatcher({required Function(String) onFrame, required this.onDone}) : onFrame = onFrame;
  void feed(String s) {
    onFrame(s);
    onDone();
  }
}

class Handler {
  Channel? channel;
  final List<String> log = [];
  final Map<String, Function(Map<String, String>)> watchers = {};
  late Codec codec;

  void _onFrame(String f) {
    log.add('frame $f');
  }

  void _onDone() {
    log.add('done');
  }

  void run() {
    codec = PlainCodec();
    codec.escape = true;
    log.add(codec.encode('a:b'));
    Dispatcher(onFrame: (f) { _onFrame(f); }, onDone: _onDone).feed('x');
    channel?.sink.add('skipped');
    channel = Channel();
    channel?.sink.add('kept');
    log.add(channel!.sink.out.join(','));
  }

  void Function() watch(Map<String, String> headers, Function(Map<String, String>) callback) {
    watchers[headers['id']!] = callback;
    return () {
      watchers.remove(headers['id']);
    };
  }
}

void main() {
  final h = Handler();
  h.run();
  final stop = h.watch({'id': 'w1'}, (m) => h.log.add('got ${m['k']}'));
  h.watchers['w1']!({'k': 'v'});
  stop();
  h.log.add('watchers ${h.watchers.length}');
  print(h.log.join('\n'));
}

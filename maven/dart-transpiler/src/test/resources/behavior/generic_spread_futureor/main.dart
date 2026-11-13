import 'dart:async';

// A spread list of map entries keeps the entry type, so its key is a Stream<T>.
class Registry<T> {
  final _subs = <Stream<T>, String?>{};
  void add(Stream<T> s) => _subs[s] = null;
  String describe(Stream<T> s) => 'stream';
  int fill() {
    var n = 0;
    for (var entry in [..._subs.entries]) {
      if (entry.value != null) continue;
      var stream = entry.key;
      _subs[stream] = describe(stream);
      n++;
    }
    return n;
  }
}

// A completer of T completed with Future.sync over a FutureOr<T> computation.
class Once<T> {
  final _completer = Completer<T>();
  bool get hasRun => _completer.isCompleted;
  Future<T> run(FutureOr<T> Function() computation) {
    if (!hasRun) _completer.complete(Future.sync(computation));
    return _completer.future;
  }
}

Future<void> main() async {
  final r = Registry<int>();
  r.add(Stream.value(1));
  r.add(Stream.value(2));
  print(r.fill());
  print(r.fill());
  final o = Once<String>();
  print(await o.run(() => 'first'));
  print(await o.run(() => 'second'));
  final a = Once<int>();
  print(await a.run(() async => 7));
}

import 'dart:async';

class Splitter {
  final List<String> log = [];
  void _onCancel(StreamController<int> c) {
    log.add('cancel');
  }

  StreamController<int> make() {
    final controller = StreamController<int>();
    controller.onCancel = () => _onCancel(controller);
    return controller;
  }
}

class Group {
  int pending = 0;
  bool closed = false;
  final List<String> log = [];

  void add(Future<int> task) {
    pending++;
    task.then((value) {
      if (closed) return null;
      if (value < 0) return null;
      pending--;
      log.add('value $value');
    }).catchError((Object error, StackTrace stackTrace) {
      if (closed) return null;
      log.add('error $error');
    });
  }
}

void main() async {
  final s = Splitter();
  final c = s.make();
  StreamSubscription<int>? inner;
  final out = StreamController<int>();
  out
    ..onPause = () {
      inner?.pause();
    }
    ..onResume = () {
      inner?.resume();
    };
  final sub = c.stream.listen((_) {});
  await sub.cancel();
  print(s.log);
  print(out.onPause != null);

  final g = Group();
  g.add(Future.value(7));
  g.add(Future.error('bad'));
  await Future.delayed(Duration.zero);
  await Future.delayed(Duration.zero);
  print(g.log);
  print(g.pending);

  final bottoms = <int, List<int>>{};
  final bottomIndex = 2;
  final got = bottoms.putIfAbsent(5, () => List<int>.filled(3, bottomIndex));
  got[0] += 1;
  print(bottoms);
}

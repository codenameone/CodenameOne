import 'dart:async';

class Hub {
  final List<String> log = [];
  late final StreamController<int> ctl;

  Hub() {
    ctl = StreamController<int>.broadcast(onListen: this._onListen, onCancel: _onCancel);
  }

  void _onListen() => log.add('listen');
  void _onCancel() => log.add('cancel');
}

void _done() => print('cleanup');

Future<void> main() async {
  final hub = Hub();
  final got = <int>[];
  final sub = hub.ctl.stream.listen(got.add);
  hub.ctl.add(1);
  hub.ctl.add(2);
  await Future<void>.delayed(Duration.zero);
  await sub.cancel();
  print(hub.log.join(','));
  print(got);
  final v = await Future<int>.value(7).whenComplete(_done);
  print(v);
  await Future<int>.value(8).whenComplete(() => print('lambda cleanup'));
  final sync = StreamController<String>.broadcast(sync: true);
  sync.stream.listen((s) => print('sync $s'));
  sync.add('a');
  print('after add');
}

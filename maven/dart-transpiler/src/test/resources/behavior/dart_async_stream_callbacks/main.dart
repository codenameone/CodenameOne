// dart:async Stream.map/where and listen's onError (both the one- and the
// two-argument form) and onDone, each passed a transpiled closure literal --
// every one of them used to transpile to Java that failed to compile, since
// only listen's onData was a concrete Funcs functional interface on the Java
// side (see dart.async.Stream's class comment, maven/dart-runtime). sync:
// true makes a broadcast StreamController deliver synchronously, matching
// this runtime's only supported delivery mode, so the printed order here is
// the same order a real Dart VM produces for the identical script.
import 'dart:async';

void main() {
  final controller = StreamController<int>.broadcast(sync: true);
  final results = <String>[];
  controller.stream.map((x) => x + 1).listen((v) => results.add('mapped:$v'));
  controller.stream.where((x) => x % 2 == 0).listen((v) => results.add('even:$v'));
  for (final v in [1, 2, 3, 4]) {
    controller.add(v);
  }
  print('map-where: $results');

  final c1 = StreamController<String>.broadcast(sync: true);
  final log1 = <String>[];
  c1.stream.listen(
    (v) => log1.add('data:$v'),
    onError: (e) => log1.add('err1:$e'),
    onDone: () => log1.add('done1'),
  );
  c1.add('a');
  c1.addError('boom1');
  c1.close();
  print('onerror-1arg: $log1');

  final c2 = StreamController<String>.broadcast(sync: true);
  final log2 = <String>[];
  c2.stream.listen(
    (v) => log2.add('data:$v'),
    onError: (e, st) => log2.add('err2:$e'),
    onDone: () => log2.add('done2'),
  );
  c2.add('b');
  c2.addError('boom2');
  c2.close();
  print('onerror-2arg: $log2');
}

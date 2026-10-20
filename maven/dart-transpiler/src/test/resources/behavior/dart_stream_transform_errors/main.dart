import 'dart:async';

Future<void> main() async {
  // handleError's own throw is emitted downstream
  final c = StreamController<int>();
  final seen = <String>[];
  final done = Completer<void>();
  void Function(Object) replace = (e) => throw StateError('replaced $e');
  c.stream.handleError(replace).listen(
      (v) => seen.add('data $v'),
      onError: (e) => seen.add('error ${e is StateError ? e.message : e}'),
      onDone: () => done.complete());
  c.add(1);
  c.addError('boom');
  c.add(2);
  await c.close();
  await done.future;
  print(seen.join(', '));

  // a throwing distinct equality turns one event into an error and goes on
  final out = <String>[];
  final d = Completer<void>();
  Stream.fromIterable([1, 1, 2, 3, 3]).distinct((a, b) {
    if (b == 2) throw 'cmp';
    return a == b;
  }).listen((v) => out.add('$v'), onError: (e) => out.add('err $e'), onDone: () => d.complete());
  await d.future;
  print(out.join(', '));

  // cancelling a derived stream cancels its quiet source
  var cancelled = false;
  void Function() markCancelled = () {
    cancelled = true;
  };
  final src = StreamController<int>(onCancel: markCancelled);
  final sub = src.stream.distinct().listen((_) {});
  await Future<void>.delayed(Duration.zero);
  await sub.cancel();
  await Future<void>.delayed(Duration.zero);
  print('source cancelled: $cancelled');
}

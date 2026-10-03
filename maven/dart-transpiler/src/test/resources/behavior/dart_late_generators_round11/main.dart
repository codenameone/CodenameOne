// late fields are checked; a sync* body runs when iterated, once per iteration;
// Object.hashAll hashes elements; List<int>.from checks element types; forEach
// refuses a length change; queryParameters is read-only; contains honours its
// start; Future.wait honours eagerError and cleanUp.
import 'dart:async';

class Holder {
  late int count;
  late final String name;
  static late String label;

  String describe() => '$name:$count';
}

class Point2 {
  final int x;
  final int y;
  Point2(this.x, this.y);

  @override
  bool operator ==(Object other) => other is Point2 && other.x == x && other.y == y;

  @override
  int get hashCode => Object.hashAll([x, y]);
}

Iterable<int> counted(List<String> log) sync* {
  log.add('start');
  yield 1;
  log.add('mid');
  yield 2;
}

Iterable<int> stopsEarly(bool stop) sync* {
  yield 1;
  if (stop) {
    return;
  }
  yield 2;
}

Future<void> main() async {
  final h = Holder();
  try {
    print(h.count);
  } on Error {
    print('count not initialized');
  }
  h.count = 3;
  h.count += 1;
  h.count++;
  h.name = 'a';
  try {
    h.name = 'b';
  } on Error {
    print('name already initialized');
  }
  print(h.describe());
  try {
    print(Holder.label);
  } on Error {
    print('label not initialized');
  }
  Holder.label = 'L';
  print(Holder.label);

  final log = <String>[];
  final gen = counted(log);
  print(log);
  print(gen.toList());
  print(log);
  print(gen.toList());
  print(log);
  print(stopsEarly(true).toList());
  print(stopsEarly(false).toList());

  print(Point2(1, 2).hashCode == Point2(1, 2).hashCode);
  print({Point2(1, 2)}.contains(Point2(1, 2)));

  try {
    print(List<int>.from(<num>[1.9]));
  } on Error {
    print('from refused a double');
  }

  final grow = [1, 2];
  try {
    grow.forEach((e) {
      if (grow.length < 4) {
        grow.add(3);
      }
    });
    print('no error');
  } on ConcurrentModificationError {
    print('forEach refused the change');
  }

  try {
    Uri.parse('https://x/?a=1').queryParameters['b'] = '2';
    print('mutable');
  } on UnsupportedError {
    print('query parameters are read-only');
  }

  print('abc'.contains('a', 1));
  print('abc'.contains('c', 1));
  print('abc'.contains(RegExp('a'), 1));

  final never = Completer<int>().future;
  final List<Future<int>> inputs = [Future<int>.error(StateError('boom')), never];
  final eager = await Future.wait(inputs, eagerError: true)
      .then((v) => 'ok', onError: (e) => 'eager ${e is StateError}');
  print(eager);
  final cleaned = <int>[];
  final late4 = Completer<int>();
  final waited = Future.wait<int>([Future.value(1), Future<int>.error(ArgumentError('x')), late4.future],
      cleanUp: (v) {
    cleaned.add(v);
  });
  late4.complete(4);
  try {
    await waited;
  } catch (e) {
    print('wait failed with an ArgumentError: ${e is ArgumentError}');
  }
  print(cleaned);
}

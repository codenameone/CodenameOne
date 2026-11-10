Future<int> twice(int v) async => v * 2;

Stream<(String, int)> pairs() async* {
  yield ('x', 1);
  yield ('y', 2);
  yield ('z', 3);
}

Future<void> overList() async {
  var points = [(1, 2), (3, 4)];
  for (var (a, b) in points) {
    final d = await twice(a + b);
    print('point $a,$b doubled sum $d');
  }
  final named = [(name: 'ann', age: 30), (name: 'bo', age: 4)];
  final greeters = <void Function()>[];
  for (final (:name, :age) in named) {
    await twice(age);
    greeters.add(() => print('hello $name ($age)'));
  }
  for (final g in greeters) {
    g();
  }
  for (var [first, ...others] in [[1, 2, 3], [4]]) {
    if (first == 4) break;
    print('first $first others ${await Future.value(others)}');
  }
}

Future<void> overStream() async {
  var total = 0;
  await for (final (label, n) in pairs()) {
    total += await twice(n);
    print('$label -> $total');
    if (label == 'y') break;
  }
  print('stream total $total');
}

void main() async {
  await overList();
  await overStream();
}

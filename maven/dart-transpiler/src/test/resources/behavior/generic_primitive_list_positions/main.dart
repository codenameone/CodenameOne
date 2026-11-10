class Box<T> {
  final T v;
  Box(this.v);
  List<T> one() => [v];
  Future<List<T>> later() async => [v, v];
  Map<String, List<T>> byKey() => {'k': [v], 'j': [v, v]};
  Stream<List<T>> st() async* {
    yield [v];
  }
  Iterable<List<T>> rows() => [[v], [v, v, v]];
  List<List<T>> grid() => [[v, v], [v]];
  Map<String, Map<int, List<T>>> deep() => {'a': {1: [v]}};
  Future<Map<String, List<T>>> laterMap() async => {'z': [v, v]};
}

Future<void> main() async {
  var b = Box<int>(4);
  List<int> a = b.one();
  print(a.first + 1);
  List<int> c = await b.later();
  print(c.fold<int>(0, (p, e) => p + e));
  await for (List<int> e in b.st()) {
    print(e[0] - 1);
  }
  Map<String, List<int>> m = b.byKey();
  print(m['k']![0] * 3);
  print(m['j']!.length);
  for (List<int> r in b.rows()) {
    print(r.reduce((x, y) => x + y));
  }
  List<List<int>> g = b.grid();
  print(g[0][1] + g[1][0]);
  print(b.deep()['a']![1]![0] + 2);
  Map<String, List<int>> lm = await b.laterMap();
  print(lm['z']!.fold<int>(0, (p, e) => p + e));

  var d = Box<double>(1.5);
  Map<String, List<double>> dm = d.byKey();
  print(dm['j']![1] * 2);
  List<List<double>> dg = d.grid();
  print(dg[0][0] + 0.25);
  List<double> dl = await d.later();
  print(dl.last);
}

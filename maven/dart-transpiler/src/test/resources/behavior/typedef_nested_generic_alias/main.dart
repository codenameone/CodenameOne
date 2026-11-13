typedef Ints = Iterable<int>;
typedef Cb = void Function(int);
typedef M = Map<String, List<int>>;
typedef N = int?;
typedef Row<T> = List<T>;
typedef Grid<T> = List<Row<T>>;
typedef Same<T> = T;
typedef Id = Key;
typedef Key = String;
typedef Pt = (int, int);
typedef Pts = List<Pt>;
extension type Meters(double v) { Meters operator +(Meters o) => Meters(v + o.v); }
typedef Len = Meters;

int sum(Ints xs) => xs.fold(0, (p, e) => p + e);
void each(Ints xs, Cb cb) { for (final x in xs) cb(x); }
Same<int> dbl(Same<int> x) => x * 2;
N maybe(bool b) => b ? 7 : null;

void main() {
  Ints xs = [1, 2, 3];
  print(sum(xs));
  print(sum({4, 5}));
  each(xs.where((x) => x > 1), (v) => print('v$v'));
  M m = {'a': [1, 2]};
  m['b'] = [3];
  m['a']!.add(9);
  print(m['a']!.length + m['b']!.first);
  print(m.values.expand((l) => l).reduce((p, q) => p + q));
  N n = maybe(true);
  print((n ?? 0) + 1);
  print(maybe(false) ?? -1);
  Grid<int> g = [[1, 2], [3]];
  g.add(<int>[4, 5, 6]);
  print(g.map((r) => r.length).toList());
  Row<String> r = ['x'];
  r.add('y');
  print(r.join());
  print(dbl(21));
  Id id = 'hello';
  print(id.toUpperCase());
  Pts ps = [(1, 2), (3, 4)];
  print(ps.map((p) => p.$1 + p.$2).toList());
  Len a = Meters(1.5);
  Len b2 = a + Meters(2.0);
  print(b2.v);
  Object o = xs;
  print(o is Ints);
  print(o is M);
  final cbs = <Cb>[(i) => print('cb$i')];
  cbs.first(3);
  Map<Key, Ints> mk = {'z': [8]};
  print(mk['z']!.first);
}

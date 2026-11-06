typedef Op = int Function(int);
typedef Pair = ({Op f, (int, Op) g});
void main() {
  final r = (a: 1, inner: (f: (int x) => x * 2, n: (g: (String s) => s.length, k: 'k')));
  print(r.inner.f(5)); print(r.inner.n.g('abc')); print(r.inner.n.k);
  print(r.runtimeType);
  ({int Function() get, void Function(int) set}) cell() { var v = 0; return (get: () => v, set: (x) { v = x; }); }
  final c = cell(); c.set(7); print(c.get());
  final nested = ((() => 1, () => 'two'), [() => 3.0]);
  print(nested.$1.$1() + nested.$2[0]()); print(nested.$1.$2());
  print(nested.$1.$1.runtimeType);
  final (a, b) = nested.$1; print('${a()}${b()}');
  final r2 = (fn: <T>(T x) => [x]);
  print(r2.fn(1)); print(r2.fn('s').runtimeType);
  Object o = r; print(o is ({int a, ({Op f, ({int Function(String) g, String k}) n}) inner}));
  print(r == r); 
}

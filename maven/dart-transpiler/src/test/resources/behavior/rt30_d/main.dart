T id<T>(T x) => x;
List<T> rep<T>(T x, int n) => n == 0 ? <T>[] : [x, ...rep<T>(x, n - 1)];
int depth<T>(Object? x) => x is List ? 1 + depth<T>(x.isEmpty ? null : x.first) : 0;
Map<K, List<V>> grp<K, V>(List<V> xs, K Function(V) k) {
  if (xs.isEmpty) return <K, List<V>>{};
  final r = grp<K, V>(xs.sublist(1), k);
  r.putIfAbsent(k(xs.first), () => <V>[]).insert(0, xs.first);
  return r;
}
R fold<T, R>(List<T> xs, R z, R Function(R, T) f) => xs.isEmpty ? z : fold<T, R>(xs.sublist(1), f(z, xs.first), f);
void main() {
  print(rep(1, 3)); print(rep(1, 3).runtimeType); print(rep<num>(1, 2).runtimeType);
  print(rep('a', 2).runtimeType);
  print(depth<int>([[[1]]]));
  final g = grp([1, 2, 3, 4, 5], (int x) => x.isEven);
  print(g); print(g.runtimeType);
  print(fold<int, String>([1, 2, 3], '', (String a, int b) => '$a$b'));
  print(fold<int, double>([1, 2], 0.5, (a, b) => a + b));
  print(id(id)(3));
  print(id<List<int>>([1]).runtimeType);
}

T pick<T>(List<Object> xs) => xs.firstWhere((x) => x is T) as T;
T make<T>(T Function() f) => f();
List<T> rep<T>(T v, int n) => List<T>.filled(n, v);
class Box<T> { T v; Box(this.v); Box<R> map<R>(R Function(T) f) => Box<R>(f(v)); R fold<R>(R init) => init; }
void main() {
  String s = pick([1, 'two', 3]);
  print(s.length);
  int n = make(() => 41);
  print(n + 1);
  List<String> r = rep('a', 3);
  print(r.join());
  final b = Box(3).map((x) => 'v$x');
  print(b.v.length);
  String f = Box(1).fold('z');
  print(f.toUpperCase());
  int i = Box('q').map((x) => x.length).v;
  print(i + 1);
  final List<double> ds = rep(1.5, 2);
  print(ds.reduce((a, b) => a + b));
}

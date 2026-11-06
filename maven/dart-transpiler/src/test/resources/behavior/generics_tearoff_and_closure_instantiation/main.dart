T id<T>(T x) => x;
List<T> wrap<T>(T x) => <T>[x];
class Box<T> {
  T v; Box(this.v);
  R map<R>(R Function(T) f) => f(v);
  List<T> asList() => [v];
  T Function(T) idf() => (T x) => x;
}
typedef IntF = int Function(int);
void main() {
  int Function(int) f = id;
  print(f(3));
  print(f.runtimeType);
  List<int> Function(int) w = wrap;
  print(w(2).runtimeType);
  var g = wrap<String>;
  print(g('a').runtimeType);
  print(g.runtimeType);
  var b = Box<int>(5);
  print(b.map((x) => '$x').runtimeType);
  print(b.idf().runtimeType);
  print(b.idf() is int Function(int));
  print(b.idf() is String Function(String));
  Object o = wrap;
  print(o is List<int> Function(int));
  print(o.runtimeType);
  var gen = <T>(T x) => <T>[x];
  print(gen<double>(1.5).runtimeType);
  print(gen(1).runtimeType);
  List<num> Function(num) gn = gen;
  print(gn(2).runtimeType);
  print(gen is Function);
  dynamic d = gen;
  print(d<String>('z').runtimeType);
  print(d(true).runtimeType);
}

import 'dart:async';
T id<T>(T x) => x;
Future<T> later<T>(T x) async { await null; return x; }
List<T> rep<T extends num>(T x, [int n = 2]) => List.filled(n, x);
(T, U) pair<T, U>(T a, U b) => (a, b);
String s16(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p) => '${a+b+c+d+e+f+g+h+i+j+k+l+m+n+o+p}';
class Box<T> { final T v; Box(this.v); R map<R>(R Function(T) f) => f(v); String toString() => 'Box($v)'; }
Future<void> main() async {
  final T Function<T>(T) g = id; print(g<int>(3)); print(g('s'));
  int Function(int) gi = id; print(gi(4)); print(gi == id); print(identical(id<int>, id<int>));
  final h = later; print(await h<String>('a')); print(await later(5));
  final r = rep; print(r(1.5)); print(rep<int>(2, 3));
  final pf = pair; final rec = pf<int, String>(1, 'b'); print(rec); print(rec.$2);
  final List<Function> fs = [id, later, rep, pair]; print(fs.length);
  print(Function.apply(s16, List.generate(16, (i) => i)));
  dynamic d16 = s16; print(d16(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1));
  final b = Box(3); final mm = b.map; print(mm<String>((x) => 'v$x'));
  final c = Completer<int Function(int)>(); scheduleMicrotask(() => c.complete(id<int>)); final got = await c.future; print(got(9));
  var counter = 0; T inc<T>(T x) { counter++; return x; }
  final ti = inc; ti<int>(1); ti('x'); print(counter);
  final stream = Stream.fromIterable([1, 2, 3]).map(id); print(await stream.toList());
  final (fa, fb) = (id<int>, id<String>); print(fa(1)); print(fb('z')); print(fa == fb);
  print(<Object>{id<int>, id<int>, id<String>}.length);
  final Object o = id; print(o is T Function<T>(T)); print(o is int Function(int));
}

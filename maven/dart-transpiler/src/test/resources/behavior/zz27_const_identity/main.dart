class P { final double d; const P(this.d); }
class Box<T> { final T v; const Box(this.v); }
class N { final P p; final List<int> l; const N(this.p, this.l); }
enum Color { red, green }
class E { final Color c; const E(this.c); }
class Q { final int x; const Q(this.x); }
void f([Q q = const Q(3)]) { print(identical(q, const Q(3))); }
class O { int x = 0; }
enum S { a, b; static const all = [a, b]; static final first = all[0]; static final n = all.length; }
Object? g(Object o) => o;
Future<int?> al() async => null;
Future<num> fo(bool b) async { return b ? 1 : 2.5; }
void main() async {
  print(identical(const P(0.0), const P(-0.0)));
  print(identical(const P(double.nan), const P(double.nan)));
  print(identical(const P(1.0), const P(1.0)));
  print(identical(const Box<int>(1), const Box<num>(1)));
  print(identical(const Box<int>(1), const Box<int>(1)));
  print(identical(const Box(1), const Box<int>(1)));
  print(identical(const N(P(1.0), [1,2]), const N(P(1.0), [1,2])));
  print(identical(const N(P(1.0), [1,2]), const N(P(1.0), [2,1])));
  print(identical(const {1: 'a', 2: 'b'}, const {2: 'b', 1: 'a'}));
  print(identical(const {1, 2}, const {2, 1}));
  print(identical(const {1, 2}, const {1, 2}));
  print(identical(const [1], const <num>[1]));
  print(identical(const E(Color.red), const E(Color.red)));
  print(identical(const E(Color.red), const E(Color.green)));
  print(identical(Q(1), Q(1)));
  print(identical(new Q(1), const Q(1)));
  f(); f(const Q(3)); f(Q(3));
  var a = 1, b = 2; (a, b) = (b, a); print('$a $b');
  var o = O(); var l = [0, 0];
  var i = 0;
  int p1, p2; var cnt=0; int nx(){return ++cnt;} (p1, p2) = (nx(), nx()); print('$p1 $p2');
  var (x, [y, z]) = (1, [2, 3]); print('$x $y $z');
  print(S.all); print(S.first); print(S.n); print(S.values);
  Object v = 3;
  print([for (var e in [1, 'a', 2.5]) if (e is int) e + 1]);
  print([if (v is int) v * 2 else 0]);
  if (v is! int) { print('no'); } else { print(v.isEven); }
  print(await al());
  var fl = () async { return null; };
  print(await fl());
  var fl2 = (bool c) async => c ? 1 : null;
  print(await fl2(false)); print(await fl2(true));
  print(await fo(false));
  print((const Box<int>(1)).runtimeType);
}

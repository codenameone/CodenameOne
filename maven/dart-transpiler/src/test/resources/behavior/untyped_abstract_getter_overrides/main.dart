abstract class A { get v; get w => 2; }
class B extends A { int get v => 1; }
class C extends A { get v => 'c'; double get w => 2.5; }
class D { late int n; int get m => n; }
class E implements D { late int n = 4; int get m => n * 2; }
abstract class Cmp<T extends Comparable<T>> { T best(List<T> l) { var b = l.first; for (var x in l) if (x.compareTo(b) > 0) b = x; return b; } }
class IC extends Cmp<num> {}
class SC extends Cmp<String> {}
class W<T> { T? t; W(this.t); bool same(W<T> o) => t == o.t; }
void main() {
  List<A> as = [B(), C()];
  for (var a in as) print('${a.v} ${a.w}');
  print(as[0].v == as[0].v); print(identical(as[0].w, as[0].w)); print(as[1].w == 2.5);
  D d = E(); print(d.m + d.n); D d2 = D(); d2.n = 3; print(d2.m == d2.n); print(identical(d.n, d.n));
  try { D().n; print('no'); } catch (e) { print('late err'); }
  print(IC().best([1, 5.5, 3])); print(SC().best(['a', 'c', 'b']));
  print(W<int?>(null).same(W<int?>(null))); print(W<int>(2).same(W(2)));
  num q = 3; q = q + 0.5; print(q); var r = 1; Object o = r; o = 'x'; print(o);
}

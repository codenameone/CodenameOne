abstract class A { late final x; }
class B extends A { late final int x = 10; }
class P { late int n; }
class Q extends P { late int n = 2; }
class M1 { var k; }
mixin Mx { int get k => 5; set k(v) {} }
class UsesMx extends M1 with Mx {}
void main() {
  A a = B(); print(a.x); print(identical(a.x, a.x));
  P p = Q(); print(p.n + 1); p.n = 9; print(p.n);
  M1 m = UsesMx(); print(m.k); print(UsesMx().k);
}

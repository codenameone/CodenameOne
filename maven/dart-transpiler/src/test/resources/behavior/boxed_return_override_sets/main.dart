abstract class A { num measure(int x); }
abstract class B { int measure(num x); }
class C implements A, B {
  int measure(num x) => x.toInt() * 2;
}
class D implements A, B {
  measure(x) => x.toInt() + 1;
}
abstract class P { Object get v; }
abstract class Q { bool get v; }
class R implements P, Q { bool get v => true; }
abstract class S { double f(); }
abstract class T { num f(); }
class U extends S implements T { double f() => 1.5; }
mixin M { num g() => 3; }
abstract class N { int g(); }
class W with M implements N { int g() => 4; }
abstract class X { Object h(); }
class Y implements X, S { double h() => 2.0; double f() => 0.5; }
void main() {
  A a = C(); B b = C();
  print(a.measure(3)); print(b.measure(2.5));
  A a2 = D(); B b2 = D();
  print(a2.measure(3)); print(b2.measure(4.7));
  P p = R(); Q q = R(); print(p.v); print(q.v);
  S s = U(); T t = U(); print(s.f()); print(t.f());
  M m = W(); N n = W(); print(m.g()); print(n.g());
  X x = Y(); print(x.h()); S s2 = Y(); print(s2.f());
  dynamic d = C(); print(d.measure(1));
}

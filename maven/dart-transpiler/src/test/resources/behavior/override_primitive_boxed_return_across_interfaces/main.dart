// A member whose Java return is primitive for one supertype and boxed for another, where
// the two supertypes' Java parameter lists differ, so one method plus a bridge serves both.
abstract class A { num measure(int x); }
abstract class B { int measure(num x); }
class C implements A, B {
  int measure(num x) => x.toInt() * 2;
}
class D implements A, B {
  measure(x) => x.toInt() + 1;
}
abstract class E { Object scale(int x); }
abstract class F { double scale(num x); }
class G implements E, F {
  double scale(num x) => x * 1.5;
}
abstract class H { Object test(int x); }
abstract class J { bool test(num x); }
class K implements H, J {
  test(x) => x > 2;
}
void main() {
  A a = C(); B b = C(); C c = C();
  print(a.measure(3)); print(b.measure(2.5)); print(c.measure(7) + 1);
  A a2 = D(); B b2 = D(); D d2 = D();
  print(a2.measure(3)); print(b2.measure(4.7)); print(d2.measure(1) * 10);
  E e = G(); F f = G(); print(e.scale(2)); print(f.scale(3)); print(G().scale(4) / 2);
  H h = K(); J j = K(); print(h.test(1)); print(j.test(3.5)); print(!K().test(9));
  dynamic dy = C(); print(dy.measure(1));
  dynamic dk = K(); print(dk.test(5));
}

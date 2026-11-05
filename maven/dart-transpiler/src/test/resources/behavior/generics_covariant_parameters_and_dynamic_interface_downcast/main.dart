class A { num f(num x) => x; }
class B extends A { int f(covariant int x) => x * 2; }
abstract class I<T> { void put(T x); }
class C implements I<int> { final l = <int>[]; void put(int x) => l.add(x); }
class D<T> { void take(T x) => print('take $x'); }
class E extends D<int> { void take(int x) => print('E $x'); }
void main() {
  A a = B();
  print(a.f(3));
  try { a.f(1.5); } catch (e) { print('TE ${e is TypeError}'); }
  I<num> i = C() as dynamic;
  try { i.put(2.5); } catch (e) { print('TE2 ${e is TypeError}'); }
  D<num> d = E();
  d.take(1);
  try { d.take(2.5); } catch (e) { print('TE3 ${e is TypeError}'); }
  var tear = d.take;
  print(tear.runtimeType);
  try { tear(3.5); } catch (e) { print('TE4 ${e is TypeError}'); }
  D<Object> dob = D<String>();
  try { dob.take(1); } catch (e) { print('TE5 ${e is TypeError}'); }
  var t2 = dob.take; print(t2 is void Function(Object));
}

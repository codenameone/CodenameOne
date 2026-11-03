class Box<T> { T v; Box(this.v); }
abstract class I {}
abstract class J implements I {}
class A implements J {}
class B extends A {}
class C implements I {}
abstract class K {}
class D1 implements I, K {}
class D2 implements I, K {}
mixin M {}
class Base {}
class S1 extends Base with M {}
class S2 extends Base with M {}
class P {}
class Q1 extends P {}
class Q2 extends P {}
void main() {
  var c = DateTime.now().year > 2000;
  var x1 = c ? Box<int>(1) : Box<num>(2);
  print(x1.runtimeType);
  var x2 = c ? <int>[1] : <int>{2};
  print(x2 is Iterable<int>);
  print(x2 is List<int>);
  var x3 = c ? B() : C();
  print(x3 is I);
  var x4 = [D1(), D2()];
  print(x4.runtimeType);
  var x5 = [S1(), S2()];
  print(x5.runtimeType);
  var x6 = [Q1(), Q2()];
  print(x6.runtimeType);
  var x7 = {B(), A()};
  print(x7.runtimeType);
  var x8 = [B(), C()];
  print(x8.runtimeType);
  var y = c ? 1 : 2.0;
  print(y);
  print(y.runtimeType);
  print(y + 1);
  print(y / 2);
  print(y ~/ 1);
  var z = !c ? 1 : 2.5;
  print(z * 2);
  var w = c ? 3 : 4.0;
  w = w + 0.5;
  print(w);
  var l = [1, 2.5];
  print(l.runtimeType);
  print(l[0]);
  var e = c ? null : 1;
  print(e);
  var lst = [c ? 1 : 2.0, 3];
  print(lst);
  print([Box<int>(1), Box<num>(1)].runtimeType);
}

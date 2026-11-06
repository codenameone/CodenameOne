class A { void m(num x) { print('A.m $x'); } }
class B extends A { @override void m(covariant int x) { print('B.m ${x + 1}'); } }
abstract class Sink2 { void put(covariant Object x); }
class IntSink implements Sink2 { @override void put(int x) { print('IntSink $x'); } }
class Pet { String get name => 'pet'; }
class Cat extends Pet { @override String get name => 'cat'; }
class Dog extends Pet {}
class Holder { covariant Pet h = Pet(); }
class CatHolder extends Holder { @override Cat h = Cat(); }
class Feeder { void feed(covariant Pet p) { print('Feeder ${p.name}'); } }
class CatFeeder extends Feeder { @override void feed(Cat p) { print('CatFeeder ${p.name}'); } }
class Scale { void weigh(covariant num w) { print('Scale $w'); } }
class IntScale extends Scale { @override void weigh(int w) { print('IntScale ${w * 2}'); } }
void main() {
  Feeder f = CatFeeder();
  f.feed(Cat());
  try { f.feed(Dog()); print('no throw'); } catch (e) { print('inherited covariant TypeError ${e is TypeError}'); }
  Scale sc = IntScale();
  sc.weigh(4);
  try { sc.weigh(4.5); print('no throw'); } catch (e) { print('inherited covariant num TypeError ${e is TypeError}'); }
  A a = B();
  a.m(1);
  try { a.m(1.5); print('no throw'); } catch (e) { print('override TypeError ${e is TypeError}'); }
  B bb = B();
  bb.m(10);
  A plain = A();
  plain.m(2.5);
  Sink2 s = IntSink();
  s.put(7);
  try { s.put('x'); print('no throw'); } catch (e) { print('interface TypeError ${e is TypeError}'); }
  Holder hd = CatHolder();
  hd.h = Cat();
  print('holder ${hd.h.name}');
  try { hd.h = Dog(); print('no throw'); } catch (e) { print('covariant field TypeError ${e is TypeError}'); }
}

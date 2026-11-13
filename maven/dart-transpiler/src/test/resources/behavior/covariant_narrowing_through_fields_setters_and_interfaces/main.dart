class Pet {}
class Cat extends Pet {}
class Dog extends Pet {}
class Holder { covariant Pet h = Pet(); }
class CatHolder extends Holder { Cat h = Cat(); }
class NumBox { covariant num x = 0; }
class IntBox extends NumBox { int x = 1; }
mixin M on Holder {}
class MH extends Holder with M { Cat h = Cat(); }
abstract class S { set s(covariant Pet p); }
class CS implements S { Cat? last; set s(Cat c) { last = c; } }
class Base { void m(covariant Pet p) { print('Base'); } }
class Mid extends Base {}
class Leaf extends Mid { void m(Cat c) { print('Leaf'); } }
abstract class I { void take(covariant Pet p); }
class Impl implements I { void take(Cat c) { print('Impl'); } }
class G<T> { void put(T t) { print('G put'); } }
class GS extends G<Pet> { void put(covariant Cat c) { print('GS put'); } }
void main() {
  Holder h = CatHolder();
  try { h.h = Dog(); print('no throw'); } catch (e) { print('TE ${e is TypeError}'); }
  NumBox nb = IntBox();
  try { nb.x = 1.5; print('no throw ${nb.x}'); } catch (e) { print('TE ${e is TypeError}'); }
  nb.x = 7; print((nb as IntBox).x); print(nb.x);
  Holder mh = MH();
  try { mh.h = Dog(); print('no throw'); } catch (e) { print('TE ${e is TypeError}'); }
  S s = CS();
  try { s.s = Dog(); print('no throw'); } catch (e) { print('TE ${e is TypeError}'); }
  Base b = Leaf();
  try { b.m(Dog()); } catch (e) { print('TE ${e is TypeError}'); }
  I i = Impl();
  try { i.take(Dog()); } catch (e) { print('TE ${e is TypeError}'); }
  G<Pet> g = GS();
  try { g.put(Dog()); } catch (e) { print('TE ${e is TypeError}'); }
  g.put(Cat());
  dynamic d = CatHolder();
  try { d.h = Dog(); print('no throw'); } catch (e) { print('TE ${e is TypeError}'); }
  Holder ok = CatHolder(); ok.h = Cat(); print(ok.h is Cat);
}

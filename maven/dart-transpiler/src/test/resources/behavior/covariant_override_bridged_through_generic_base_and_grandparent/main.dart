class Pet {}
class Cat extends Pet {}
class Dog extends Pet {}
class G<T> { void put(T t) { print('G put'); } }
class GS extends G<Pet> { void put(covariant Cat c) { print('GS put'); } }
class H<T> { void put(T t) { print('H put'); } }
class HS extends H<Cat> { void put(Cat c) { print('HS put'); } }
class Base { void m(covariant Pet p) { print('Base'); } }
class Mid extends Base {}
class Leaf extends Mid { void m(Cat c) { print('Leaf'); } }
void main() {
  G<Pet> g = GS(); g.put(Cat());
  GS gs = GS(); gs.put(Cat());
  H<Cat> h = HS(); h.put(Cat());
  Base b = Leaf(); b.m(Cat());
  try { b.m(Dog()); } catch (e) { print('TE ${e is TypeError}'); }
}

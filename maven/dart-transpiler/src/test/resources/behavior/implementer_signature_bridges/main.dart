// Implementers whose member signatures differ from the implemented class's:
// covariant parameters, int/double against num, generic specialization.
class Animal {
  String get name => 'animal';
}

class Dog extends Animal {
  @override
  String get name => 'dog';
  String bark() => 'woof';
}

class Keeper {
  String feed(Animal a) => 'keeper feeds ${a.name}';
  num weight() => 1.5;
  num count(int x) => x;
  double ratio() => 0.5;
  int size() => 1;
}

class DogKeeper implements Keeper {
  @override
  String feed(covariant Dog a) => 'dogkeeper feeds ${a.name} ${a.bark()}';
  @override
  int weight() => 42;
  @override
  int count(num x) => x.toInt() * 2;
  @override
  double ratio() => 0.25;
  @override
  int size() => 7;
}

class Box<T> {
  T? _v;
  Box([this._v]);
  T get value => _v as T;
  void put(T v) {
    _v = v;
  }
  T pick(T a, T b) => a;
  bool same(Box<T> other) => other._v == _v;
  @override
  bool operator ==(Object other) => other is Box<T> && other._v == _v;
  @override
  int get hashCode => _v.hashCode;
}

class IntBox implements Box<int> {
  @override
  int? _v = 0;
  @override
  int get value => _v!;
  @override
  void put(int v) {
    _v = v * 10;
  }
  @override
  int pick(int a, int b) => b;
  @override
  bool same(Box<int> other) => other.value == value;
}

class Base {
  String hello() => 'base';
}

mixin Loud on Base {
  String shout() => '${hello()}!';
}

class Quiet implements Base {
  @override
  String hello() => 'quiet';
}

class Both extends Base with Loud {
  @override
  String hello() => 'both';
}

String useKeeper(Keeper k) =>
    '${k.feed(Dog())} ${k.weight()} ${k.count(3)} ${k.ratio()} ${k.size()}';

void main() {
  print(useKeeper(Keeper()));
  print(useKeeper(DogKeeper()));
  Box<int> b = IntBox();
  b.put(4);
  print(b.value);
  print(b.pick(1, 2));
  print(b.same(Box<int>(40)));
  Box<int> plain = Box<int>(3);
  print(plain.same(Box<int>(3)));
  print(plain == Box<int>(3));
  Base q = Quiet();
  print(q.hello());
  print(Both().shout());
  final dk = DogKeeper();
  int w = dk.weight() + 1;
  print(w);
  final ib = IntBox();
  int p = ib.pick(5, 6) * 2;
  print(p);
  print(dk.count(2.5));
  ib.put(3);
  print(ib.value + 1);
}

// A Dart override may narrow a parameter (`covariant`, or implicitly). Java cannot,
// so the narrowed method was emitted as an OVERLOAD: a call through the base type
// ran the base's body, never the override. The emitter now adds a bridge with the
// base's signature that casts and delegates.
class Animal {
  String get name => 'animal';
}

class Dog extends Animal {
  @override
  String get name => 'dog';
}

class Keeper {
  String greet(Animal a) => 'keeper greets ' + a.name;
  void feed(Animal a) {
    print('keeper feeds ' + a.name);
  }
}

class DogKeeper extends Keeper {
  @override
  String greet(covariant Dog d) => 'dog keeper greets ' + d.name;
  @override
  void feed(Dog d) {
    print('dog keeper feeds ' + d.name);
  }
}

abstract class Judge<T> {
  bool same(Judge<T> other);
}

class NameJudge extends Judge<String> {
  final String n;
  NameJudge(this.n);
  @override
  bool same(covariant NameJudge other) => other.n == n;
}

void main() {
  Keeper k = DogKeeper();
  print(k.greet(Dog()));
  k.feed(Dog());
  Judge<String> j = NameJudge('a');
  print(j.same(NameJudge('a')));
  print(j.same(NameJudge('b')));
}

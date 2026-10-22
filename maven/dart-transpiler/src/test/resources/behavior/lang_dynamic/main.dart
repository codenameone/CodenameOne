class Pet {
  String name;
  int age;
  Pet(this.name, this.age);
  String get label => '$name/$age';
  String greet(String who, {String punct = '!'}) => 'hi $who from $name$punct';
}

class Dog extends Pet {
  Dog(String n) : super(n, 3);
  String bark() => 'woof';
}

void main() {
  dynamic m = {'a': [1, 2, 3], 'b': 'text'};
  print(m['a'].length);
  print(m['a'][1]);
  print(m['b'].toUpperCase());
  print(m.containsKey('a'));
  print(m['a'].map((x) => x * 2).toList());
  dynamic d = Dog('rex');
  print(d.name);
  print(d.label);
  print(d.bark());
  print(d.greet('ann', punct: '?'));
  d.age = 7;
  print(d.age);
  try {
    d.fly();
  } on NoSuchMethodError {
    print('no fly');
  }
}

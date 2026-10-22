class Counter {
  int total = 0;
  int add([int count = 1]) {
    total += count;
    return total;
  }

  String greet({String name = 'world', String? suffix}) => 'hello $name${suffix ?? ''}';
}

void main() {
  dynamic c = Counter();
  print(c.add());
  print(c.add(5));
  print(c.greet());
  print(c.greet(name: 'dart'));
  print(c.greet(suffix: '!'));
}

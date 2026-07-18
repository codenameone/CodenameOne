int total = 0;

class Vec {
  final double x;
  final double y;
  Vec(this.x, this.y);
  Vec.unit()
      : x = 1.0,
        y = 1.0;
  factory Vec.origin() {
    return Vec(0.0, 0.0);
  }
  Vec operator +(Vec other) {
    return Vec(x + other.x, y + other.y);
  }
  bool operator ==(Vec other) {
    return x == other.x && y == other.y;
  }
  String describe() {
    return '($x, $y)';
  }
}

class Counter {
  int value = 0;
  void bump() {
    value++;
    total++;
  }

  void add(int n) {
    value += n;
  }
}

class Base {
  String greet() {
    return 'base';
  }
}

class Derived extends Base {
  @override
  String greet() {
    return super.greet() + '+derived';
  }
}

void main() {
  Vec a = Vec(1.0, 2.0);
  Vec b = Vec.unit();
  Vec c = a + b;
  print(c.describe());
  print(Vec.origin().describe());
  print(a + b == Vec(2.0, 3.0));
  Counter k = Counter();
  k
    ..bump()
    ..bump()
    ..add(3);
  print(k.value);
  print(total);
  List<int> base = [1, 2];
  bool extra = true;
  List<int> combined = [
    0,
    ...base,
    if (extra) 99 else 98,
    for (int i = 0; i < 2; i++) i * 10,
  ];
  print(combined);
  List<String> names = [for (final n in ['a', 'b']) n.toUpperCase()];
  print(names);
  int p = 1, q = 2;
  print(p + q);
  print(Derived().greet());
}

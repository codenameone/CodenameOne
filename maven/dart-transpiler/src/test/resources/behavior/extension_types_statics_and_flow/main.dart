// Extension types: statics, redirecting factories, compound assignment through
// shadowing accessors, and the extension-type marker surviving inference.

extension type Id(int value) {
  static int created = 0;
  static const int base = 1000;
  static final String label = 'id';
  static int lazy = _initLazy();
  static int _initLazy() {
    print('lazy init');
    return 7;
  }

  factory Id.of(int v) = Id;
  factory Id.offset(int v) = Id.shifted;
  Id.shifted(int v) : value = v + base;

  static Id make(int v) {
    created++;
    return Id(v);
  }

  String describe() => '$label#$value';
  bool get isEven => value % 3 == 0;
}

extension IntStats on int {
  static int calls = 0;
  static const String tag = 'stats';
  int twice() {
    calls += 1;
    return this * 2;
  }
}

class Cell {
  int v = 0;
}

extension type Doubling(Cell cell) {
  int get v => cell.v;
  set v(int x) => cell.v = x * 2;
}

class Shape {
  final String name;
  Shape(this.name);
  factory Shape.square() = Square;
  factory Shape.named(String n) = Shape;
}

class Square extends Shape {
  Square() : super('square');
}

T pick<T>(T a, T b) => a;

Future<Id> later(int v) async => Id(v);

void main() async {
  print(Id.created);
  Id.make(1);
  Id.make(2);
  print(Id.created);
  Id.created = 10;
  Id.created += 5;
  Id.created++;
  print(Id.created);
  print(Id.base);
  print(Id.label);
  print('before lazy');
  print(Id.lazy);
  Id.lazy *= 2;
  print(Id.lazy);
  print(Id.make(3).describe());

  print(3.twice());
  print(4.twice());
  print(IntStats.calls);
  IntStats.calls -= 1;
  print(IntStats.calls);
  print(IntStats.tag);

  print(Id.of(4).describe());
  print(Id.offset(5).describe());
  print(Shape.square().name);
  print(Shape.named('tri').name);

  var d = Doubling(Cell());
  d.v = 3;
  d.v += 1;
  print(d.v);
  d.v++;
  print(d.v);
  ++d.v;
  print(d.cell.v);
  d.v--;
  print(d.v);

  var ids = [Id(3), Id(4)];
  print(pick(Id(6), Id(7)).describe());
  print(ids.first.describe());
  print(ids.first.isEven);
  for (final x in ids) {
    print(x.describe());
  }
  Id? maybe;
  print((maybe ?? Id(9)).describe());
  var flag = ids.length > 1;
  print((flag ? ids[0] : ids[1]).describe());
  print((await later(12)).describe());
  print(ids.map((e) => e.describe()).toList());
}

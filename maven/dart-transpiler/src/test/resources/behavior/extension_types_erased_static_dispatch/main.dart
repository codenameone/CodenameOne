// Dart 3 extension types: erased to the representation at run time, members
// dispatched statically.

extension type Id(int value) {
  Id.zero() : this(0);
  Id.doubled(int v) : value = v * 2;
  factory Id.parse(String s) => Id(int.parse(s));

  static Id make(int v) => Id(v * 3);

  bool get isValid => value > 0;
  Id next() => Id(value + 1);
  Id operator +(Id other) => Id(value + other.value);
  bool operator <(Id other) => value < other.value;
  String describe([String prefix = 'id']) => '$prefix#$value';
}

// implements the representation type: int's members are available.
extension type Meters(double v) implements double {
  Meters operator *(int k) => Meters(v * k);
  String get label => '${v}m';
}

// implements another extension type.
extension type Admin(Id id) implements Id {
  String get role => 'admin:${id.value}';
}

// generic extension type.
extension type Box<T>(List<T> items) {
  T get first => items[0];
  int get count => items.length;
  Box<T> add(T t) {
    items.add(t);
    return this;
  }
}

extension type _Secret(String raw) {
  String get masked => '*' * raw.length;
}

class Counter {
  int n = 0;
}

extension type Handle(Counter c) {
  int get n => c.n;
  set scaled(int v) => c.n = v * 10;
  void bump() => c.n++;
}

void main() {
  var a = Id(3);
  var b = Id.zero();
  var c = Id.doubled(4);
  var d = Id.parse('41');
  print(a.value);
  print(b.value);
  print(c.value);
  print(d.value);
  print(a.isValid);
  print(b.isValid);
  print(a.next().value);
  print((a + c).value);
  print(a < c);
  print(a.describe());
  print(a.describe('x'));
  print(Id.make(9).value);

  // Erasure: the run-time type is the representation's.
  print(a.runtimeType);
  print(a is int);
  Object? o = a;
  print(o is Id);
  print(o is int);
  print((o as Id).value);
  print(7 as Id);
  print(a);
  print('$a');

  List<Id> ids = [Id(1), Id(2), Id(3)];
  print(ids);
  print(ids.runtimeType);
  print(ids.map((i) => i.next().value).toList());
  Map<Id, String> names = {Id(5): 'five'};
  print(names[Id(5)]);

  var m = Meters(2.5);
  print(m.label);
  print((m * 2).label);
  print(m.floor());
  print(m + 1.0);
  print(m.runtimeType);

  var ad = Admin(Id(7));
  print(ad.role);
  print(ad.describe());
  print(ad.isValid);
  print(ad.runtimeType);

  var box = Box<String>(['a']);
  box.add('b').add('c');
  print(box.first);
  print(box.count);
  print(box.items);
  print(box.runtimeType);

  var s = _Secret('hello');
  print(s.masked);
  print(s.raw);

  var h = Handle(Counter());
  h.scaled = 4;
  print(h.n);
  h.bump();
  print(h.n);
  print(h.runtimeType);

  // Dynamic calls see only the representation.
  dynamic dyn = a;
  print(dyn.isEven);
  try {
    print(dyn.describe());
  } on NoSuchMethodError {
    print('NoSuchMethodError: describe');
  }
  dynamic dh = h;
  print(dh.n);
}

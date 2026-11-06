// User types named like java.lang classes must not shadow what the generated code uses.
class Record {
  final String id;
  Record(this.id);
  @override
  String toString() => 'Record($id)';
}

class Process {
  int steps = 0;
  void run() => steps++;
}

class Thread {
  final List<String> posts = [];
}

class Character {
  final String name;
  final int level;
  const Character(this.name, this.level);
}

class Override {
  const Override();
}

class Long {
  final int v;
  Long(this.v);
  Long operator +(Long o) => Long(v + o.v);
}

class Number {
  final double x;
  Number(this.x);
  bool get isBig => x > 10;
}

class Runtime {
  static int boots = 0;
  static void boot() => boots++;
}

class Math {
  static int twice(int x) => x * 2;
}

class Integer {
  final int raw;
  Integer(this.raw);
}

class Boolean {
  final bool b;
  Boolean(this.b);
}

class StringBuilder {
  final StringBuffer _b = StringBuffer();
  void add(Object o) => _b.write(o);
  String build() => _b.toString();
}

enum Exception { none, minor, major }

enum Short { a, b }

enum Double { one, two }

enum Class { first, second }

enum System { on, off }

class Error {
  final String m;
  Error(this.m);
}

class Iterable {
  final int n;
  Iterable(this.n);
}

enum Void { only }

T pick<T>(List<T> l) => l.first;

void main() {
  final r = Record('r1');
  print(r);
  print(r.runtimeType);
  final p = Process()..run()..run();
  print(p.steps);
  final t = Thread()..posts.add('hi');
  print(t.posts);
  const c = Character('elf', 3);
  print('${c.name} ${c.level} ${c.runtimeType}');
  print(const Override() == const Override());
  print((Long(2) + Long(40)).v);
  print(Number(12.5).isBig);
  Runtime.boot();
  print(Runtime.boots);
  print(Math.twice(21));
  print(Integer(5).raw + 1);
  print(Boolean(true).b);
  final sb = StringBuilder()..add(1)..add('x');
  print(sb.build());
  print(Exception.major.index);
  print(Exception.values);
  print(Short.b);
  print(Double.two.name);
  print(Class.second);
  print(System.off);
  final ints = <int>[3, 4];
  print(pick(ints) + 1);
  final strs = ['a', 'b'];
  print(pick(strs).toUpperCase());
  Object o = 7;
  print(o is int);
  print(1.5.toStringAsFixed(1));
  print(int.parse('12') + double.parse('0.5'));
  print([1, 2, 3].map((e) => e * 2).toList());
  print({'k': true});
  try {
    throw StateError('bad');
  } catch (e) {
    print(e.runtimeType);
  }
  print(Error('e1').m);
  print(Iterable(3).n);
  print(Void.only);
  print(identical(r, r));
  print('$r'.length);
  print(r.hashCode == r.hashCode);
}

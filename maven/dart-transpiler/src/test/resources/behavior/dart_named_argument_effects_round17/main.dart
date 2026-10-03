// Named arguments are evaluated in the order they are written, whatever the
// argument's shape: a method on a value, a static method, a constructor, a
// getter, and a plain read that a later call changes.
final List<String> trace = <String>[];
int level = 1;

int raise() {
  level += 10;
  return level;
}

class Counter {
  int n = 0;
  static int total = 0;
  int next() {
    n++;
    trace.add('next$n');
    return n;
  }

  int get peek {
    trace.add('peek$n');
    return n;
  }

  int get tick {
    n++;
    trace.add('tick$n');
    return n;
  }

  static int bump() {
    total++;
    trace.add('bump$total');
    return total;
  }
}

class Tag {
  final String name;
  Tag(this.name) {
    trace.add('Tag($name)');
  }
}

class Pair {
  final Object a;
  final Object b;
  Pair({required this.a, required this.b});
  String describe() => 'a=$a b=$b';
}

class Box {
  final int value;
  Box(this.value);
}

void main() {
  final c = Counter();
  print(Pair(b: c.next(), a: c.next()).describe());
  print(trace.join(','));
  trace.clear();

  print(Pair(b: Counter.bump(), a: Counter.bump()).describe());
  print(trace.join(','));
  trace.clear();

  print(Pair(b: Tag('first').name, a: Tag('second').name).describe());
  print(trace.join(','));
  trace.clear();

  print(Pair(b: c.peek, a: c.next()).describe());
  print(trace.join(','));
  trace.clear();

  final box = Box(1);
  print(Pair(b: box.value, a: c.next() + box.value).describe());
  trace.clear();

  // An index read is operator [], ordered against the call that changes the list.
  final xs = <int>[7, 8, 9];
  print(Pair(b: xs[0], a: xs.removeAt(0)).describe());
  print(Pair(b: xs.length, a: xs.removeAt(0)).describe());

  // A variable read nested in an expression is read where it is written.
  print(Pair(b: level + 1, a: raise()).describe());
  print(Pair(b: 'L$level', a: raise()).describe());

  // Two getters with effects and no call among the arguments.
  final d = Counter();
  print(Pair(b: d.tick, a: d.tick).describe());
  print(trace.join(','));
  trace.clear();
}

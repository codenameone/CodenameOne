import 'dart:core' hide Object, int, List, String, Map, Future, Iterable;
import 'dart:core' as core;

class int {
  final core.int v;
  int(this.v);
  int plus(int o) => int(v + o.v);
  core.String toString() => 'int<$v>';
}

class List<T> {
  final core.List<T> items = [];
  void add(T t) => items.add(t);
  core.int get length => items.length;
  core.String toString() => 'MyList$items';
}

class String {
  final core.String raw;
  String(this.raw);
  String shout() => String(raw.toUpperCase() + '!');
  core.String toString() => 'Str($raw)';
}

class Map {
  final core.String tag;
  Map(this.tag);
  core.String lookup(core.String k) => '$tag:$k';
}

class Future {
  core.bool done = false;
  void finish() => done = true;
}

class Iterable {
  core.int count = 0;
  Iterable step() { count++; return this; }
}

class Object {
  core.String describe() => 'my object';
  core.String toString() => 'Obj';
}

class Box<T> {
  final T item;
  Box(this.item);
  T get() => item;
}

String greet(String s) => s.shout();

void main() {
  core.print(core.int.parse('3') + 4);
  core.print(core.DateTime.now().year > 2000);
  core.print(core.List.filled(3, 'x'));
  core.print(core.String.fromCharCodes([72, 105]));
  core.print(core.double.parse('1.5') * 2);
  core.print(core.Map<core.String, core.int>.from({'a': 1}));
  core.print(core.Iterable.generate(3, (k) => k * k).toList());
  core.Future<core.int>.value(9).then((v) => core.print('fut $v'));

  var s = String('ab');
  core.print(s.raw);
  core.print(s.shout());
  core.print(greet(String('hi')).raw);
  String t = String('typed');
  core.print(t);

  var i = int(2).plus(int(5));
  core.print(i.v);
  core.print(i);

  var l = List<core.int>();
  l.add(1);
  l.add(2);
  core.print(l.length);
  core.print(l);

  var m = Map('cfg');
  core.print(m.lookup('k'));

  var f = Future();
  f.finish();
  core.print(f.done);

  var it = Iterable().step().step();
  core.print(it.count);

  var o = Object();
  core.print(o.describe());
  core.print(o);

  var b = Box<String>(String('boxed'));
  core.print(b.get().raw);
}

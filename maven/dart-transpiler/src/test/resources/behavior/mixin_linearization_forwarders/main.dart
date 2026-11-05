class Base {
  String greet(String who, {String punct = '.'}) => 'base $who$punct';
  String tag([int n = 1]) => 'base-tag$n';
  T pick<T>(T a, T b) => a;
  int raw = 0;
  set value(int v) => raw = v;
  int get value => raw;
  String step() => 'B';
}

mixin Loud on Base {
  @override
  String greet(String who, {String punct = '!'}) => 'LOUD $who$punct';
  @override
  String tag([int n = 2]) => 'loud-tag$n';
  @override
  T pick<T>(T a, T b) => b;
  @override
  set value(int v) => raw = v * 10;
  @override
  String step() => 'L>' + super.step();
}

mixin Quiet on Base {
  @override
  String step() => 'Q>' + super.step();
}

class Child extends Base with Loud, Quiet {}

mixin Indexer<E, L extends List<E>> {
  L get items;
  E at(int i) => items[i];
  void put(int i, E e) {
    items[i] = e;
  }
}

class Box with Indexer<String, List<String>> {
  @override
  final List<String> items = ['a', 'b', 'c'];
}

mixin Slots on Object {
  List<int> get slots;
  int first() => slots[0];
}

class SlotHolder with Slots {
  @override
  List<int> slots = [7, 8];
}

abstract class Buf<E> {
  E get defaultValue;
  List<E> data = [];
  void fill(Iterable<E> src) {
    if (src is List<E>) {
      data = List<E>.filled(src.length, defaultValue);
      data.setRange(0, src.length, src);
    } else {
      data = src.toList();
    }
  }
}

class IntBuf extends Buf<int> {
  @override
  int get defaultValue => 0;
}

void main() {
  final c = Child();
  print(c.greet('ann'));
  print(c.greet('bob', punct: '?'));
  print(c.tag());
  print(c.tag(5));
  print(c.pick<int>(1, 2));
  c.value = 3;
  print(c.value);
  print(c.step());
  final b = Box();
  print(b.at(1));
  b.put(1, 'z');
  print(b.items.join());
  print(SlotHolder().first());
  final ib = IntBuf();
  ib.fill([4, 5, 6]);
  print(ib.data);
  ib.fill({9, 8});
  print(ib.data);
  Iterable<int> it = [1, 2];
  if (it is List<int>) {
    final dst = List<int>.filled(3, 0);
    dst.setRange(0, 2, it);
    print(dst);
  }
  Object o = [3, 4];
  if (o is List) {
    print(o.length);
  }
}

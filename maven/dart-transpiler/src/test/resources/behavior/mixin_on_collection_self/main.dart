import 'dart:collection';
import 'dart:typed_data';

// A program interface that is itself a List, and an abstract class used as a mixin
// that only implements it: bare first/last/this[i]/for-in reach the applier's list.
abstract class Bag<E> implements List<E> {
  String describe();
}

abstract mixin class BagHelpers<E> implements Bag<E> {
  String describe() {
    var parts = <String>[];
    for (var x in this) {
      parts.add('$x');
    }
    return 'first=$first last=$last at1=${this[1]} all=${parts.join(",")}';
  }
}

class NameBag<E> with BagHelpers<E>, ListMixin<E> implements Bag<E> {
  final List<E> _items = <E>[];
  int get length => _items.length;
  set length(int n) => _items.length = n;
  E operator [](int i) => _items[i];
  void operator []=(int i, E v) => _items[i] = v;
  void add(E v) => _items.add(v);
}

// A mixin on a program IterableBase subclass.
class Countdown extends IterableBase<int> {
  final int from;
  Countdown(this.from);
  Iterator<int> get iterator => List<int>.generate(from, (i) => from - i).iterator;
}

mixin Summed on Countdown {
  int total() {
    var t = 0;
    for (var v in this) {
      t += v;
    }
    return t + first * 100 + last * 1000;
  }
}

class SummedCountdown extends Countdown with Summed {
  SummedCountdown(int n) : super(n);
}

// A mixin on Iterable directly, applied to a ListMixin class.
mixin Joined<E> on Iterable<E> {
  String joined() => '${first}..${last}';
}

class Trio extends ListBase<String> with Joined<String> {
  final List<String> _s = ['a', 'b', 'c'];
  int get length => _s.length;
  set length(int n) => _s.length = n;
  String operator [](int i) => _s[i];
  void operator []=(int i, String v) => _s[i] = v;
}

// super.x = v from a mixin on a class with a setter.
class Box {
  int _v = 0;
  int get value => _v;
  set value(int v) => _v = v;
}

mixin Doubling on Box {
  @override
  set value(int v) {
    super.value = v * 2;
  }
}

class DoublingBox extends Box with Doubling {}

// A generic field typed by a type parameter bounded by TypedDataList.
class Ring<E, L extends TypedDataList<E>> {
  L table;
  Ring(this.table);
  void put(int i, E v) {
    table[i] = v;
  }
  E at(int i) => table[i];
}

void main() {
  var b = NameBag<String>();
  b.add('x');
  b.add('y');
  b.add('z');
  print(b.describe());
  print(SummedCountdown(3).total());
  print(Trio().joined());
  var d = DoublingBox();
  d.value = 21;
  print(d.value);
  var r = Ring<int, Uint8List>(Uint8List(4));
  r.put(2, 7);
  print(r.at(2));
  print(r.table);
}

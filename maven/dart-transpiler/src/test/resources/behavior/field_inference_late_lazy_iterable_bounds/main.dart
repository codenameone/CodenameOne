abstract class Bag {
  factory Bag(int n, {bool big = false}) {
    RangeError.checkNotNegative(n, 'n');
    Bag b;
    if (big) {
      b = _BigBag(n);
    } else {
      b = _SmallBag(n);
    }
    return b;
  }
  int get size;
}

class _BigBag implements Bag {
  final int size;
  _BigBag(int n) : size = n * 10;
}

class _SmallBag implements Bag {
  final int size;
  _SmallBag(this.size);
}

class Joiner {
  final bool wide;
  Joiner(this.wide);
  late final _sep = wide ? ' | ' : ',';
  final _tag = 1 > 2 ? 'big' : 'small';
  String join(String a, String b) {
    var p = a;
    if (p.endsWith(_sep)) p = p.substring(0, p.length - 1);
    return '$p$_sep$b:${_tag.length}';
  }
}

abstract class Counter<E, T extends Iterable<E>> {
  int count(T? items) {
    if (items == null) return -1;
    var n = 0;
    for (var e in items) {
      if (e != null) n++;
    }
    for (E e in items) {
      n += e.hashCode == 0 ? 0 : 0;
    }
    return n;
  }
}

class ListCounter<E> extends Counter<E, List<E>> {}

List<int> ints([int? n]) {
  n ??= 3;
  var l = List<int>.filled(n, 0, growable: true);
  l.add(n);
  return l;
}

List<double> doubles([int? n]) {
  n ??= 2;
  var l = List<double>.filled(n, 0.5, growable: true);
  return l;
}

void main() {
  print(ints());
  print(ints(1));
  print(doubles());
  print(Bag(2).size);
  print(Bag(2, big: true).size);
  print(Joiner(true).join('a', 'b'));
  print(Joiner(false).join('x,', 'y'));
  print(ListCounter<int>().count([1, 2, 3]));
  print(ListCounter<String>().count(null));
}

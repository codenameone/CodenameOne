abstract class Bag<E> implements Iterable<E> {
  Iterable<E> get inner;
  Iterable<T> recast<T>() => inner.cast<T>();
  Iterable<E> snapshot() => inner.toList();
  @override
  Iterator<E> get iterator => inner.iterator;
}

class ListBag<E> extends Bag<E> {
  final List<E> items;
  ListBag(this.items);
  @override
  Iterable<E> get inner => items;
  @override
  List<T> recast<T>() => items.cast<T>();
  @override
  List<E> snapshot() => List<E>.of(items);

  dynamic noSuchMethod(Invocation i) => super.noSuchMethod(i);
}

class SetBag<E> extends Bag<E> {
  final Set<E> items;
  SetBag(this.items);
  @override
  Iterable<E> get inner => items;
  @override
  Set<E> snapshot() => Set<E>.of(items);
  dynamic noSuchMethod(Invocation i) => super.noSuchMethod(i);
}

class Echo {
  @override
  dynamic noSuchMethod(Invocation i) {
    return super.noSuchMethod(i);
  }
}

class Box<E> {
  final List<E> items;
  Box(this.items);
  List<E> copy() => List<E>.of(items);
  List<E> get view => items;
}

void main() {
  final box = Box<int>([4, 5]);
  final List<int> c = box.copy();
  c.add(6);
  print(c);
  print(box.view.length);
  final List<double> dc = Box<double>([1.5]).copy();
  print(dc.first + 1);

  final lb = ListBag<int>([3, 1, 2]);
  final List<int> l = lb.snapshot();
  l.add(9);
  print(l);
  print(l.reduce((a, b) => a + b));
  print(lb.recast<Object>());
  final Bag<int> asBag = lb;
  final it = asBag.snapshot();
  print(it.length);
  print(it is List<int>);
  print(it is List<double>);
  if (it is List<int>) {
    it.add(7);
    print(it.last + 1);
    print(it.fold<int>(0, (a, b) => a + b));
  }
  final Object boxed = Box<double>([0.5, 2.0]).copy();
  print(boxed is List<double>);
  print(boxed is List<int>);
  if (boxed is List<double>) {
    print(boxed.first * 4);
  }
  final sl = ListBag<String>(['c', 'a', 'b']);
  final List<String> ls = sl.snapshot();
  ls.add('z');
  print(ls);
  final Bag<String> sAsBag = sl;
  print(sAsBag.snapshot() is List<String>);
  print(sAsBag.snapshot() is List<int>);
  final sb = SetBag<String>({'a', 'b', 'a'});
  final Set<String> s = sb.snapshot();
  s.add('c');
  print(s);
  final Bag<String> sbag = sb;
  print(sbag.snapshot() is Set<String>);
  print(sbag.snapshot().toList());
  dynamic e = Echo();
  try {
    e.whisper();
  } on NoSuchMethodError {
    print('nsm');
  }
}

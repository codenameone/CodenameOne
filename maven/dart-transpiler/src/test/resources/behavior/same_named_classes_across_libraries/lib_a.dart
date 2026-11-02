class Box {
  final int value;
  Box(this.value);
  String show() => 'A.Box($value)';
}

class Shelf<E> {
  final List<E> items = <E>[];
  void put(E item) => items.add(item);
  E first() => items.first;
  String describe() => 'A.Shelf of ${items.length}';
}

mixin Tagger {
  String tag(String s) => '[a:$s]';
}

class Sizer {
  int size(String s) => s.length;
}

String useA() {
  final s = Shelf<int>();
  s.put(4);
  s.put(5);
  return '${Box(1).show()} ${s.describe()} ${s.first() + 1}';
}

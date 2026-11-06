abstract class Shape {
  int get sides;
  String get label;
  String describe() => '$label has $sides sides';
}

class Square extends Shape {
  @override
  final int sides = 4;
  @override
  final String label;
  Square(this.label);
}

abstract class _Base<E> {
  Iterable<E> get _items;
  int get count => _items.length;
}

class Bag<E> extends _Base<E> {
  @override
  final List<E> _items;
  Bag(this._items);
}

class Problem {
  final String source;
  Problem(this.source);
}

class NamedProblem extends Problem {
  NamedProblem(String s) : super(s);
  @override
  String get source => 'named:' + super.source;
}

abstract class Style {
  String get separator;
}

class Slash implements Style {
  @override
  final separator = '/';
}

abstract class Ring<E, L extends List<E>> {
  final L _table;
  Ring(this._table);
  int _copyInto(List<E> target) {
    target.setRange(0, _table.length, _table);
    return _table.length;
  }
  L _create(int n);
  L snapshot() {
    final L out = _create(_table.length);
    _copyInto(out);
    return out;
  }
}

class IntRing extends Ring<int, List<int>> {
  IntRing(List<int> t) : super(t);
  @override
  List<int> _create(int n) => List<int>.filled(n, 0);
}

void main() {
  print(IntRing([4, 5, 6]).snapshot());
  Shape s = Square('box');
  print(s.describe());
  print(s.sides);
  print(Bag<int>([1, 2, 3]).count);
  Problem p = NamedProblem('x');
  print(p.source);
  Style st = Slash();
  print(st.separator);
}

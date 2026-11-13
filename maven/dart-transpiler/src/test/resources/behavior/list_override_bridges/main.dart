import 'dart:collection';

class Tracked<E> extends ListBase<E> {
  final List<E> _inner = <E>[];
  final List<String> log = <String>[];

  @override
  int get length => _inner.length;

  @override
  set length(int n) => _inner.length = n;

  @override
  E operator [](int i) => _inner[i];

  @override
  void operator []=(int i, E v) => _inner[i] = v;

  @override
  void add(E e) => _inner.add(e);

  @override
  void addAll(Iterable<E> items) {
    log.add('addAll ${items.length}');
    for (var e in items) {
      _inner.add(e);
    }
  }

  @override
  List<E> sublist(int start, [int? end]) {
    log.add('sublist $start ${end ?? -1}');
    return _inner.sublist(start, end);
  }
}


void main() {
  var t = Tracked<int>();
  t.addAll([1, 2, 3]);
  List<int> asList = t;
  asList.addAll([4, 5]);
  print(asList.sublist(1));
  print(asList.sublist(1, 3));
  print(t.sublist(2));
  // Runtime code reaching the overrides: List.of/addAll from another list,
  // and cascade through a List-typed reference.
  var other = <int>[0];
  other.addAll(t);
  print(other);
  (asList..addAll([6])).length;
  print(t.length);
  print(t.log.join('|'));
}

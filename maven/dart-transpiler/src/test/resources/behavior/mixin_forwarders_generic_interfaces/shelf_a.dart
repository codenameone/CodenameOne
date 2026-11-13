import 'dart:collection';

class Shelf<E> {
  E pick(int index) => throw UnimplementedError();
  String describeEverythingOnTheShelfWithAVeryLongName(E sample, int count, String prefix, bool loud) => 'base';
  List<E> firstTwo() => throw UnimplementedError();
}

mixin Picking<E> on IterableBase<E> {
  E pick(int index) => elementAt(index);
  String describeEverythingOnTheShelfWithAVeryLongName(E sample, int count, String prefix, bool loud) =>
      '$prefix${loud ? '!' : '.'}$sample x$count of $length';
  List<E> firstTwo() => take(2).toList();
}

class IntShelf extends IterableBase<int> with Picking<int> implements Shelf<int> {
  final List<int> items;
  IntShelf(this.items);
  Iterator<int> get iterator => items.iterator;
}

String useA() {
  Shelf<int> s = IntShelf([5, 6, 7]);
  return '${s.pick(1) + 1} ${s.describeEverythingOnTheShelfWithAVeryLongName(9, 2, 'a', true)} ${s.firstTwo()}';
}

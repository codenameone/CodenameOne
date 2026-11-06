// @dart=2.19
import 'dart:collection';

abstract class DelegatingView<E> implements List<E> {
  List<E> get delegate;

  @override
  E get first => delegate.first;

  @override
  int get length => delegate.length;

  @override
  E operator [](int index) => delegate[index];

  @override
  List<E> operator +(List<E> other) => delegate + other;

  @override
  bool contains(Object? element) => delegate.contains(element);
}

mixin Tagged<E> {
  String tag = 'tagged';
}

class Items<E> with Tagged<E>, ListMixin<E>, DelegatingView<E> {
  final List<E> _items;

  Items(this._items);

  @override
  List<E> get delegate => _items;

  @override
  set length(int newLength) {
    _items.length = newLength;
  }

  @override
  void operator []=(int index, E value) {
    _items[index] = value;
  }
}

void main() {
  final it = Items<String>(['a', 'b', 'c']);
  print(it[1]);
  print(it.length);
  print(it.first);
  print(it.contains('c'));
  it[0] = 'z';
  print(it.join(','));
  print((it + ['d']).length);
  print(it.tag);
  print(it.toList());
}

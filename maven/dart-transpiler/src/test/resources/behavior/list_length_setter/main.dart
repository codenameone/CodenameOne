// `list.length = n` on fields, locals and through a forwarding setter.

class Bag {
  final List<String?> items = ['a', 'b', 'c'];
  List<int> counts = [1, 2, 3, 4];
}

class Window<E> {
  final List<E> _inner = [];
  void add(E v) => _inner.add(v);
  set size(int v) => _inner.length = v;
  int get size => _inner.length;
}

void main() {
  final bag = Bag();
  bag.items.length = 1;
  print(bag.items);
  bag.items.length = 3;
  print(bag.items);
  bag.counts.length = 2;
  print(bag.counts);
  final local = <double>[1.5, 2.5, 3.5];
  local.length = 1;
  print(local);
  final dyn = <Object?>[1, 'two'];
  print(dyn.length = 4);
  print(dyn);
  final w = Window<int?>()..add(1)..add(2);
  w.size = 1;
  print(w.size);
  w.size = 3;
  print(w._inner);
  final fixed = List<int?>.filled(2, 0);
  try {
    fixed.length = 1;
  } on UnsupportedError {
    print('fixed refused');
  }
  final ro = List<int>.unmodifiable([1]);
  try {
    ro.length = 0;
  } on UnsupportedError {
    print('unmodifiable refused');
  }
  try {
    bag.items.length = -1;
  } on RangeError {
    print('negative refused');
  }
  print(bag.items.length);
}

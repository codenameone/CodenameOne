import 'dart:collection';

class Window extends ListBase<int> {
  final List<int?> _items = [];
  @override
  int get length => _items.length;
  @override
  set length(int n) => _items.length = n;
  @override
  int operator [](int i) => _items[i]!;
  @override
  void operator []=(int i, int v) => _items[i] = v;
}

class Ring with ListMixin<String> {
  final List<String> _s = ['a', 'b', 'c', 'd'];
  @override
  int get length => _s.length;
  @override
  set length(int n) => _s.length = n;
  @override
  String operator [](int i) => _s[i];
  @override
  void operator []=(int i, String v) => _s[i] = v;
}

void main() {
  final w = Window()..addAll([1, 2, 3, 4, 5]);
  print(w.sublist(2));
  print(w.sublist(1, 3));
  final r = Ring();
  print(r.sublist(1));
  print(r.sublist(0, 2).join('-'));
}

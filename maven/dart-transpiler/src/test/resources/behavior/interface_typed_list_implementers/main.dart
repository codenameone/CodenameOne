// A class that `implements List<E>` without applying ListMixin itself: a reference
// typed as that class must still offer every List member, and iterate.
import 'dart:collection';

abstract class Shelf<E> implements List<E> {
  String get label;
}

class ArrayShelf<E> extends Object with ListMixin<E> implements Shelf<E> {
  final List<E> _items = <E>[];
  @override
  final String label;
  ArrayShelf(this.label);

  @override
  int get length => _items.length;
  @override
  set length(int n) => _items.length = n;
  @override
  E operator [](int i) => _items[i];
  @override
  void operator []=(int i, E v) => _items[i] = v;
  @override
  void add(E v) => _items.add(v);
}

int total(Shelf<int> s) {
  var sum = 0;
  for (final v in s) {
    sum += v;
  }
  return sum;
}

String describe(Shelf<String> s) {
  final parts = <String>[];
  for (var i = 0; i < s.length; i++) {
    parts.add(s[i]);
  }
  return '${s.label}:${parts.join(',')}';
}

void main() {
  final Shelf<int> nums = ArrayShelf<int>('nums');
  nums.add(3);
  nums.add(4);
  nums.addAll([5, 6]);
  print(nums.length);
  print(total(nums));
  print(nums.first);
  print(nums.last);
  print(nums.isEmpty);
  print(nums.where((v) => v.isEven).toList());
  print(nums.map((v) => v * 10).join('|'));
  print(nums.contains(5));
  print(nums.indexOf(6));
  nums[0] = 30;
  print(nums[0]);
  print(nums.sublist(1, 3));
  final Shelf<String> words = ArrayShelf<String>('words');
  words.add('a');
  words.add('b');
  print(describe(words));
  print(words.reversed.toList());
  print(words is List<String>);
  final List<int> asList = nums;
  print(asList.length);
}

import 'dart:collection';

abstract mixin class Frozen<E> implements List<E> {
  static UnsupportedError _refuse() => UnsupportedError('frozen');
  void add(E value) => throw _refuse();
  E removeLast() => throw _refuse();
}

abstract mixin class Greets {
  String get who;
  String greet() => 'hello $who';
}

class Plain extends Greets {
  @override
  String get who => 'plain';
}

class Base {
  String describe() => 'base';
}

class Mixed extends Base with Greets {
  @override
  String get who => 'mixed';
}

class Counter extends IterableMixin<int> {
  @override
  Iterator<int> get iterator => [5, 6, 7].iterator;
}

void main() {
  print(Frozen._refuse().message);
  print(Plain().greet());
  final m = Mixed();
  print('${m.describe()} ${m.greet()}');
  print(Counter());
}

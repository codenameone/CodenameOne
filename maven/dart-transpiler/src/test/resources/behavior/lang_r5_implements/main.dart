import 'dart:collection';

class Base {
  int count = 1;
  final String label;
  Base(this.label);
  String describe() => '$label:$count';
  int twice(int x) => x * 2;
  String get kind => 'base';
}

class Impl implements Base {
  @override
  int count = 10;
  @override
  final String label = 'impl';
  @override
  String describe() => 'Impl($count)';
  @override
  int twice(int x) => x * 3;
  @override
  String get kind => 'impl';
}

String use(Base b) => '${b.describe()} ${b.twice(4)} ${b.kind} ${b.label}';

void main() {
  print(use(Base('b')));
  Impl i = Impl();
  i.count++;
  print(use(i));
  Base asBase = i;
  print(asBase is Impl);
}

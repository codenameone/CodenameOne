abstract class Shape {
  String get name;
}

class Square implements Shape {
  final int side;
  Square(this.side);
  String get name => 'square$side';
}

class Holder<T> {
  final T value;
  Holder(this.value);
  String toString() => 'Holder($value)';
}

class Column {
  final List<Shape> children;
  Column({required this.children});
}

Column build(bool header, int rows) {
  return Column(children: [
    if (header) Square(0),
    ...List.generate(rows, (index) => Square(index + 1)),
  ]);
}

Future<void> main() async {
  List<Holder<String>> held = [Holder('x'), ...List.generate(2, (i) => Holder('y$i'))];
  print(held);
  var c = build(true, 3);
  print(c.children.map((s) => s.name).join(','));
  print(build(false, 2).children.length);
}

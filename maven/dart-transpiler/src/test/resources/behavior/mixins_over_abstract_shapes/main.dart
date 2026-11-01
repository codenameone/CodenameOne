abstract class Shape {
  String get name;
  double area();
}

mixin Describe on Shape {
  String describe() => '$name with area ${area().toStringAsFixed(1)}';
}

mixin Counter {
  int _hits = 0;
  void hit() => _hits++;
  int get hits => _hits;
}

mixin Tagged {
  String get tag => 'tag:${runtimeType}';
}

class Square extends Shape with Describe, Counter, Tagged {
  final double side;
  Square(this.side);
  @override
  String get name => 'square';
  @override
  double area() => side * side;
}

mixin Numbers {
  Iterable<int> upTo(int n) sync* {
    for (var i = 0; i < n; i++) {
      yield i;
    }
  }

  Stream<int> ticks(int n) async* {
    for (var i = 0; i < n; i++) {
      yield i * 10;
    }
  }
}

class Gen with Numbers {}

Future<void> main() async {
  var sq = Square(3);
  sq.hit();
  sq.hit();
  print(sq.describe());
  print(sq.hits);
  print(sq.tag);
  var g = Gen();
  print(g.upTo(3).toList());
  print(await g.ticks(3).toList());
}

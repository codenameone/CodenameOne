Future<int> twice(int v) async => v * 2;
Future<bool> isBig(int v) async {
  print('guard $v');
  return v > 1;
}

sealed class Shape {}
class Circle extends Shape {
  final int r;
  Circle(this.r);
}
class Square extends Shape {
  final int side;
  Square(this.side);
}

Future<String> classify(Object o) async {
  if (o case int n when await isBig(n)) {
    return 'big int ${await twice(n)}';
  } else {
    return 'not a big int';
  }
}

Future<void> ifCases() async {
  print(await classify(1));
  print(await classify(5));
  print(await classify('s'));
  if (await Future<Object>.value((3, 'c')) case (int a, String b)) {
    print('subject $a $b ${await twice(a)}');
  }
  final fns = <String Function()>[];
  for (final v in <Object>[1, 'two', 3]) {
    if (v case int n when await isBig(n)) {
      fns.add(() => 'captured $n');
    }
  }
  for (final f in fns) {
    print(f());
  }
}

Future<int> area(Shape s) async => switch (s) {
      Circle(r: var r) when await isBig(r) => await twice(r * r),
      Circle(:var r) => r,
      Square(:var side) => side * side,
    };

Future<void> switches() async {
  print(await area(Circle(1)));
  print(await area(Circle(3)));
  print(await area(Square(4)));
  final label = switch (await twice(2)) { 4 => 'four', _ => 'other' };
  print(label);
  var n = 0;
  final word = switch (n) { 0 => await Future.value('zero'), _ => 'many' };
  print(word);
}

Future<void> records() async {
  final r = (await twice(1), name: await Future.value('rec'), 7);
  print(r);
  print(r.name);
  final (a, b) = (await twice(10), await twice(20));
  print('$a $b');
}

void main() async {
  await ifCases();
  await switches();
  await records();
}

enum Grade { a(90), b(75), c(50);
  final int min; const Grade(this.min);
  static Grade of(int s) => values.firstWhere((g) => s >= g.min, orElse: () => c);
  static int get count => values.length;
  static const fallback = Grade.c;
  Grade get next => values[(index + 1) % values.length];
  bool get isTop => this == values.first;
  static String label(Grade g) => g.name.toUpperCase();
}
Iterable<int> fib() sync* { var a = 0, b = 1; while (true) { yield a; (a, b) = (b, a + b); } }
class Pt { final int x, y; Pt(this.x, this.y); }
void main() {
  print([95, 80, 10, 75].map(Grade.of).toList());
  print(Grade.of(60));
  print(Grade.count);
  print(Grade.fallback);
  print(Grade.a.next);
  print(Grade.c.next);
  print(Grade.a.isTop);
  print(Grade.values.map(Grade.label).join(','));
  final f = Grade.of; print(f(91));
  print(fib().take(8).toList());
  var a = 1, b = 2;
  (a, b) = (b, a);
  print('$a $b');
  var xs = [0, 0, 0];
  int p, q;
  [p, _, q] = [7, 8, 9];
  print('$p $q');
  int px, py;
  Pt(x: px, y: py) = Pt(3, 4);
  print(px + py);
  var s = 'x';
  ({'k': s}) = {'k': 'y'};
  print(s);
  String n; int m;
  (n, m: m) = ('nn', m: 5);
  print('$n $m');
  xs[0] = 1;
  print(xs);
}

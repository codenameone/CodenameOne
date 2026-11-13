typedef Pager<T> = Future<({List<T> items, bool hasMore})> Function(int page);

(int, String) pair() => (1, 'a');
({int a, bool b}) named() => (a: 1, b: true);
(int, {String label}) mixed() => (7, label: 'seven');

Future<({List<int> items, bool hasMore})> fetch(int page) async =>
    (items: [page, page + 1], hasMore: page < 2);

class Holder {
  final (String, int) entry;
  Holder(this.entry);
}

class Box {
  final int? v;
  Box(this.v);
}

class Abortable {
  final String? trigger;
  Abortable(this.trigger);
}

class Point {
  final int x;
  final int y;
  Point(this.x, this.y);
  Point.origin() : x = 0, y = 0;
  String toString() => 'P($x,$y)';
}

class Cell<T> {
  final T value;
  Cell(this.value);
  String toString() => 'Cell($value)';
}

class Svc {
  final int id;
  Svc() : id = 42;
}

class Maker {
  final Svc Function() factory = Svc.new;
}

class Fmt {
  static String up(String s) => s.toUpperCase();
  static String low(String s) => s.toLowerCase();
}

String twice(String s) => '$s$s';
String bang(String s) => '$s!';

const List<(String, int)> table = [('one', 1), ('two', 2)];

String describe(Object o) {
  switch (o) {
    case (int a, int b):
      return 'pair $a $b';
    case Point(x: 0, y: 0):
      return 'origin';
    case Point(:final x, :final y):
      return 'point $x $y';
    default:
      return 'other';
  }
}

String shape(Object? o) => switch (o) {
      (String s, int n) when n > 1 => 'many $s',
      (String s, _) => 'one $s',
      Box(v: final x?) => 'box $x',
      _ => 'nothing',
    };

DateTime fixed() => DateTime(2020, 1, 2);

void main() async {
  final p = pair();
  print(p);
  print('${p.$1} ${p.$2}');
  final n = named();
  print(n);
  print('${n.a} ${n.b}');
  final m = mixed();
  print(m);
  print('${m.$1} ${m.label}');
  print(p == (1, 'a'));
  print(p.hashCode == (1, 'a').hashCode);
  print(n == (b: true, a: 1));
  print(p == (2, 'a'));

  final r = await fetch(1);
  print('${r.items} ${r.hasMore}');
  Pager<int> pager = fetch;
  final r2 = await pager(2);
  print('${r2.items} ${r2.hasMore}');

  Object o = (3, 'x');
  final cast = o as (int, String);
  print(cast.$2);

  final h = Holder(('k', 9));
  print(h.entry.$1);
  List<(String, int)> local = [('a', 1)];
  local.add(('b', 2));
  print(local);
  print(table);

  final (a, b) = pair();
  print('$a $b');
  var (x: px, y: py) = (x: 5, y: 6);
  print('$px $py');
  final (name, count, icon) = ('home', 3, 'H');
  print('$name $count $icon');

  for (final (k, v) in table) {
    print('$k=$v');
  }

  Object bo = Box(4);
  if (bo case Box(v: final x?)) {
    print('box has $x');
  }
  Object bn = Box(null);
  if (bn case Box(v: final x?)) {
    print('unexpected $x');
  } else {
    print('box empty');
  }
  Object ab = Abortable('timeout');
  if (ab case Abortable(:final trigger?)) {
    print('trigger $trigger');
  }

  print(describe((1, 2)));
  print(describe(Point(0, 0)));
  print(describe(Point(1, 2)));
  print(describe('z'));
  print(shape(('a', 3)));
  print(shape(('a', 1)));
  print(shape(Box(8)));
  print(shape(null));

  final ctor = Point.new;
  print(ctor(3, 4));
  final origin = Point.origin;
  print(origin());
  print([1, 2].map(Cell<int>.new).toList());
  print(['q'].map(Cell.new).toList());
  print(Maker().factory().id);

  final up = Fmt.up;
  print(up('hi'));
  print(['a', 'b'].map(Fmt.up).toList());
  DateTime Function()? clock;
  final DateTime d1 = (clock ?? fixed)();
  print(d1.year);
  clock = null;
  final DateTime d2 = (clock ?? DateTime.now)();
  print(d2.year > 2000);
  final fns = [twice, bang];
  for (final f in fns) {
    print(f('x'));
  }
  final sfns = [Fmt.up, Fmt.low];
  print(sfns.map((f) => f('Mx')).toList());
}

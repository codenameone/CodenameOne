// Null-aware assignment through an index or a setter, operators on dynamic
// values, and named arguments evaluated in the order they are written.

String log(String s) {
  print('eval $s');
  return s;
}

class Pair {
  final String a;
  final String b;
  Pair({required this.a, required this.b});
  String describe({required String first, required String second}) => '$first-$second';
}

class Holder {
  String? name;
  int _count = 0;
  int? get count => _count == 0 ? null : _count;
  set count(int? v) {
    print('set count $v');
    _count = v ?? 0;
  }
}

void main() {
  // ??= on an index, as a statement and as a value
  var m = <String, List<int>>{};
  m['a'] ??= [];
  m['a']!.add(1);
  m['a'] ??= [99];
  print(m['a']);
  var got = m['b'] ??= [2];
  print(got);
  print(m.length);

  // ??= on a field and through a setter
  var h = Holder();
  h.name ??= 'first';
  h.name ??= 'second';
  print(h.name);
  h.count ??= 3;
  h.count ??= 4;
  print(h.count);

  // operators on dynamic values
  dynamic x = 1;
  print(x + 2);
  x += 1.5;
  print(x);
  print(-x);
  dynamic i = 7;
  print(i ~/ 2);
  print(i % 4);
  print(i > 2);
  i++;
  print(i);
  dynamic s = 'ab';
  print(s + 'c');
  print(s * 2);
  print(1 == 1.0);
  dynamic one = 1;
  print(one == 1.0);
  print(<num>[1].contains(1.0));

  // named arguments run in source order
  var p = Pair(b: log('b'), a: log('a'));
  print('${p.a}${p.b}');
  print(p.describe(second: log('2'), first: log('1')));

  // String patterns and fixed-point formatting
  print('a1b22c'.split(RegExp(r'[0-9]+')));
  print('abc'.replaceAll(RegExp(''), '-'));
  print(1.0.toStringAsFixed(20));
  print(1.005.toStringAsFixed(2));
}

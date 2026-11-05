import 'dart:async';
void main() async {
  Object o = (1, 'a');
  print(o is (int, String));
  print(o is (int, int));
  var r = o as (int, String);
  print(r.$2);
  try { o as (int, int); print('no throw'); } on TypeError { print('TypeError'); }
  Object n = (null, 'x');
  print(n is (int?, String));
  print(n is (int, String));
  Object m = (3, 'x');
  print(m is (int?, String));
  Object named = (a: 1, b: 2.5);
  print(named is ({int a, double b}));
  print(named is ({int a, int b}));
  Future<(int, String)> f = Future.value((2, 'b'));
  var fr = await f;
  print(fr.$1 + 1);
  var s = Stream.fromIterable([(1, 'x'), (2, 'y')]);
  await for (var (a, b) in s) { print('$a$b'); }
  Map<String, (int, String)> mp = {'k': (5, 'v')};
  print(mp['k']!.$1 * 2);
  print(mp['k'] == (5, 'v'));
  (num, Object) wide = (5, 'v');
  print(wide == mp['k']);
  print({(1, 2), (1, 2)}.length);
  print((1, 2).hashCode == (1, 2).hashCode);
  List<(int, String)> lst = [(1, 'a')];
  print(lst is List<(int, String)>);
  print(lst is List<(int, int)>);
  print((1, 'a'));
  print((a: 1, 2));
}

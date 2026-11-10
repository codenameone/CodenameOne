class Box<T> { T v; Box(this.v); }
T pick<T>(T a, T b) => b;
List<T> two<T>(T a, T b) => [a, b];
String tag<T>(T x) => '$T';
Type tOf<T>() => T;
void main() {
  var ln = <Null>[null];
  print([ln is List<int?>, ln is List<int>, ln.runtimeType]);
  List<int?> li = [1, null];
  try { var x = li as List<int>; print('nothrow ${x.length}'); } catch (e) { print('threw'); }
  li.add(null); print(li);
  dynamic dl = <int>[1];
  try { dl.add(null); print('added'); } catch (e) { print('TypeError ${e is TypeError}'); }
  Map<String, int?> m = {'a': null, 'b': 1};
  print([m.runtimeType, m is Map<String, int>, m is Map<String, int?>, m is Map<String, Object?>]);
  var s = {null, 1}; var mp = {null: 1, 2: null};
  print([s.runtimeType, mp.runtimeType, s.contains(null), mp[null]]);
  var nest = [[null], [1]];
  print([nest.runtimeType, nest[0].runtimeType]);
  print([pick(null, 1).runtimeType, two(null, 1).runtimeType, two(1, 'a').runtimeType, tag(null), tag<int?>(null), tag(1)]);
  List<int>? nl; List<int?> other = [null];
  var sp = [...?nl, ...other, 3];
  print([sp.runtimeType, sp]);
  bool c = false;
  var ie = [1, if (c) null];
  var ie2 = [if (c) 1];
  var fe = [for (var i in [1, 2]) i == 1 ? null : i];
  print([ie.runtimeType, ie2.runtimeType, fe.runtimeType, fe]);
  Map<String, Object?> mo = {'a': null, 'b': 1};
  Object? Function(Object?) f = (o) => o;
  print([mo.runtimeType, f(null), Box<Null>(null).runtimeType, Box<Object?>(1).runtimeType, Box(null).runtimeType]);
  print([List<int?> == List<int?>, tOf<int?>() == int, tOf<int?>() == tOf<int?>(), <int?>[].runtimeType == <int?>[].runtimeType, <int?>[].runtimeType == <int>[].runtimeType, null.runtimeType, null.runtimeType == Null]);
}

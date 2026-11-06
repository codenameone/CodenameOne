Future<T> f<T>(T x) async => x;
class P<T> { final T a; final List<T> b; P(this.a, this.b); }
void main() async {
  var c = true; List<int>? nl;
  var l = [if (c) await f(1) else 2, for (var i in [1, 2]) await f(i * 10), ...?nl, ...[await f(5)]];
  print('$l ${l.runtimeType}');
  var s = {await f('a'), if (!c) 'b', for (var x in ['c']) await f(x)};
  print('$s ${s.runtimeType}');
  var m = {'k': await f(1), 'j': await f(2.5)};
  print('$m ${m.runtimeType}');
  var m2 = <String, num>{if (c) 'z': await f(3), for (var i in [1]) '$i': await f(i)};
  print('$m2 ${m2.runtimeType}');
  var nest = [[await f(1)], {await f('x'): [await f(2)]}];
  print('$nest ${nest.runtimeType} ${nest[0].runtimeType} ${nest[1].runtimeType}');
  Map<String,int>? nm; var m3 = {...?nm, 'a': 1}; print('$m3 ${m3.runtimeType}');
  var (x, y) = (await f(1), await f('s'));
  print('${x.runtimeType} ${y.runtimeType}');
  var (List<int> li, n: String nn) = ([1], n: "q"); print('$li $nn ${li.runtimeType}');
  final (a, b) = (P<int>(1, [2]), <String>['z']); print('${a.b.runtimeType} ${b.runtimeType}');
  var {"k": kk, "j": jj} = m; print('$kk $jj');
  switch (<String, Object>{'a': [1, 2, 3]}) { case {'a': [var h, ...var rest]}: print('$h $rest ${rest.runtimeType}'); }
}

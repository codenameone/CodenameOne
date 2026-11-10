import 'dart:async';
void main() async {
  final fs = <void Function()>[];
  for (var i = 0; i < 3; i++) { fs.add(() => print(i)); i += 0; }
  for (final f in fs) f();
  final g = <int Function()>[];
  for (var i = 0; i < 4; i++) { g.add(() => i++); if (i == 1) i++; }
  print(g.map((f) => f()).toList());
  var big = 1 << 40; final h = <int Function()>[];
  for (var k = big; k < big + 2; k++) { h.add(() => k); }
  print(h.map((f) => f()).toList());
  try { for (var i = 0; i < 2; i++) { g.add(() => i); if (i == 1) throw 'x'; } } catch (e) { print(e); }
  print(g.last());
  final v = await Stream.fromIterable([1,2,3]).expand((x) => [x, x*10]).toList();
  print(v);
  final r = await Future<int>.error('boom').catchError((e) => 5);
  print(r);
  try { await Future<int>.error('e2').catchError((e) => 'str'); } catch (e) { print(e.runtimeType); }
  final r2 = await Future<int>.error(StateError('s')).catchError((e) => 9, test: (e) => e is StateError);
  print(r2);
  try { await Future<int>.error(ArgumentError('a')).catchError((e) => 9, test: (e) => e is StateError); } catch (e) { print('passed ${e.runtimeType}'); }
}

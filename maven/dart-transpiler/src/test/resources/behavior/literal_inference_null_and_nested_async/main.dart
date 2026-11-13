import 'dart:async';
Future<int> one() async => 1;
Future<double> half() async => 0.5;
Future<List<num>> lst(bool c) async {
  var x = await one();
  return [x, if (c) 2.5, for (var i = 0; i < 2; i++) i, ...[await half()]];
}
Future<List<int?>> ln() async { var x = await one(); return [x, null]; }
Future<Map<String, int?>> mp() async { var x = await one(); return {'a': x, 'b': null}; }
T? firstOr<T>(List<T> l, bool Function(T) p) { for (var e in l) { if (p(e)) return e; } return null; }
Future<void> main() async {
  var x = await one();
  var y = await half();
  var a = [x, y];
  var b = [x, 'a'];
  var c = [x, null];
  var d = {'k': x, 'j': y};
  var e = {'k': x, 'j': null};
  var g = [[x], ['s']];
  print([a, b, c, d, e, g].map((z) => z.runtimeType).join(','));
  var l = await lst(true); print(l.runtimeType); print(l);
  var l2 = await ln(); print(l2.runtimeType);
  var m = await mp(); print(m.runtimeType);
  print(firstOr<int>([1, 2, 3], (v) => v > 5));
  print(firstOr<int>([1, 2, 3], (v) => v > 1));
  final comp = Completer<int?>();
  scheduleMicrotask(() => comp.complete(null));
  var cv = await comp.future; print(cv);
  print([a is List<num>, a is List<int>, c is List<int?>, c is List<int>, g is List<List<Object>>]);
  for (var row in g) { print((row as dynamic).first); }
  dynamic dd = g; print(dd[0][0]);
  var aw = [await one(), await one()]; print(aw.runtimeType);
  var aw2 = [await one(), await half()]; print(aw2.runtimeType);
  var aw3 = {await one(): await half()}; print(aw3.runtimeType);
}

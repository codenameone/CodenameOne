import 'dart:async';
void main() async {
  var a = () { try { return 1; } catch (e) { return 2; } };
  print(a.runtimeType); print(a());
  var b = () { throw 'x'; };
  print(b.runtimeType);
  var c = () async { print('c'); };
  print(c.runtimeType); await c();
  var d = () async { return 3; };
  print(d.runtimeType); print(await d());
  var e = () async { await null; };
  print(e.runtimeType);
  var f = () { try { return 1; } finally { print('fin'); } };
  print(f.runtimeType); print(f());
  var g = (bool x) { if (x) return 1; return 2.5; };
  print(g.runtimeType); print(g(false));
  var h = () { try { return 'a'; } on FormatException catch (_) { rethrow; } };
  print(h.runtimeType);
  var i = (int x) { if (x > 0) return null; return x; };
  print(i.runtimeType);
  var j = () sync* { yield 1; };
  print(j.runtimeType); print(j().toList());
  var k = () async* { yield 'a'; };
  print(k.runtimeType); print(await k().toList());
  var l = () { return; };
  print(l.runtimeType);
  var m = () {};
  print(m.runtimeType);
  var n = () => throw StateError('s');
  print(n.runtimeType);
  try { b(); } catch (e) { print('caught $e'); }
  var o = () async => throw 'z';
  print(o.runtimeType);
}

class Box<T> { T Function(T) f; Box(this.f); }
abstract class Shape { String name(); }
class Sq implements Shape { String name() => 'sq'; }
T pick<T>(List<T> xs, int i) => xs[i];
R appl<R>(R Function() g) => g();
void main() {
  Box<num> b = Box<int>((x) => x + 1);
  try { var f = b.f; print('got'); print(f(1)); } catch (e) { print('R ${e is TypeError}'); }
  Box<Object> bo = Box<String>((s) => s);
  try { bo.f; print('no'); } catch (e) { print('R2 ${e is TypeError}'); }
  dynamic s = Sq();
  Shape sh = s; print(sh.name());
  dynamic n = 3;
  try { Shape x = n; print('no ${x}'); } catch (e) { print('DC ${e is TypeError}'); }
  try { var l = <Shape>[n]; print('no $l'); } catch (e) { print('DL ${e is TypeError}'); }
  try { print((n as Shape).name()); } catch (e) { print('AS ${e is TypeError}'); }
  dynamic gp = pick;
  print(gp(<int>[1,2], 1));
  print(appl(() => pick(['a','b'], 0)).runtimeType);
  print(appl<List<int>>(() => [1]).runtimeType);
  var nest = appl(() => appl(() => <double>[1.5]));
  print(nest.runtimeType);
  T idT<T>(T x) => x;
  int Function(int) fi = idT;
  print(fi.runtimeType);
  dynamic dfi = fi;
  try { dfi('s'); } catch (e) { print('DF ${e is TypeError}'); }
  try { dynamic one = 1; var x = one as String; print(x); } catch (e) { print('AS2 ${e is TypeError}'); }
  T conv<T>(Object? o) => o as T;
  try { conv<List<String>>(<int>[1]); print('no'); } catch (e) { print('AS3 ${e is TypeError}'); }
  print(conv<List<Object>>(<int>[1]));
  try { print(conv<int?>(null)); conv<int>(null as dynamic); } catch (e) { print('AS4 ${e is TypeError}'); }
}

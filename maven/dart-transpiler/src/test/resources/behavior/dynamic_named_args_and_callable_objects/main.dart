class P { void f({int a = 1, String? b}) => print('f $a $b'); int call(int x) => x * 2; }
void main() {
  more();
  dynamic p = P();
  p.f(a: 3, b: 'q');
  p.f(b: 'z');
  try { p.f(c: 1); } catch (e) { print('nsm ${e is NoSuchMethodError}'); }
  try { p.f(a: 'bad'); } catch (e) { print('te ${e is TypeError}'); }
  dynamic fn = ({required int n}) => n + 1;
  print(fn(n: 4));
  print(p(21));
  dynamic l = [3,1,2];
  l.sort((int a, int b) => b - a); print(l);
  try { l.add('s'); print(l); } catch (e) { print('te2 ${e is TypeError}'); }
  dynamic m = <String,int>{};
  try { m['k'] = 'v'; } catch (e) { print('te3 ${e is TypeError}'); }
  dynamic s = 'abc';
  try { s.padLeft('x'); } catch (e) { print('te4 ${e is TypeError}'); }
  try { s.substring(1, 'x'); } catch (e) { print('te5 ${e is TypeError}'); }
  print(s.replaceAll('b', 'B'));
  dynamic list = <int>[1,2];
  try { list.insert(0, 1.5); } catch (e) { print('te6 ${e is TypeError}'); }
  try { list.addAll(['a']); } catch (e) { print('te7 ${e is TypeError}'); }
  try { (list as dynamic).indexOf('a'); print('ok idx'); } catch (e) { print('te8'); }
  dynamic n = 5;
  try { n.clamp('a', 3); } catch (e) { print('te11 ${e is TypeError}'); }
  print(n.toStringAsFixed(2));
}

class Adder {
  int call(int a, {int by = 1}) => a + by;
}

void more() {
  dynamic add = Adder();
  print(add(1));
  print(add(1, by: 10));
  dynamic named = ({required String who, int times = 1}) => who * times;
  print(named(who: 'ab', times: 3));
  print(named(who: 'c'));
  try { named(); } catch (e) { print('missing ${e is NoSuchMethodError}'); }
}

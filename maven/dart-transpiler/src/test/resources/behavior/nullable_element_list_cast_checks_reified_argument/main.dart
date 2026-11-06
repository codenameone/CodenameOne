class Box<T> { T? v; void put(T x) { v = x; } }
class W { Box<Object> b; W(this.b); }
int c = 0;
W w() { c++; return W(Box<int>()); }
void t(String name, void Function() f) {
  try { f(); print('$name ok'); } catch (e) { print('$name ${e is TypeError ? 'TypeError' : e.runtimeType}'); }
}
void main() {
  t('c1', () { dynamic g = <T>(T x) => x; print(g<int>(1)); g<int>('s'); });
  t('c2', () { dynamic g = <T extends num>(T x) => x; g('s'); });
  t('c3', () { var g = <T extends num>(T x) => x; Function h = g; Function.apply(h, ['s']); });
  t('n1', () { Object? l = <int?>[null]; var r = l as List<int>; print(r); });
  t('n2', () { Object? l = <int?>[1]; var r = l as List<int>; print(r); });
  t('n3', () { Object? m = <String,int?>{}; m as Map<String,int>; });
  t('n4', () { Object? x = null; x as List<int>; });
  t('n5', () { List<int?> l = [null]; Object o = l; print(o is List<int>); });
  t('ord', () { w().b.put('s'); });
  print(c);
  t('ord2', () { c = 0; var x = w(); x.b.put(c++); print(c); });
  t('compound', () { Box<Object> q = Box<int>(); q.put(q.v ?? 'z'); });
}

class Box<T> { T? v; void put(T x) { v = x; } T get() => v as T; }
int n = 0;
Box<Object> mk(Box<Object> b) { n++; print('mk$n'); return b; }
Object arg(Object o) { print('arg'); return o; }
mixin M<T> { T? m; void setM(T x) { m = x; } }
class C<T> with M<T> {}
abstract class I<T> { void take(T x); }
class Impl<T> implements I<T> { void take(T x) { print('took $x'); } }
class Holder { void Function(int) f = (int x) => print('f$x'); }
T id<T>(T x) => x;
void t(String name, void Function() f) {
  try { f(); print('$name ok'); } catch (e) { print('$name ${e.runtimeType.toString().contains('Error') ? 'TypeError' : e.runtimeType}'); }
}
void main() {
  Box<Object> b = Box<int>();
  t('order', () => mk(b).put(arg('s')));
  t('order2', () { var l = <Box<Object>>[Box<int>()]; var i = 0; l[i++].put(1); print(i); });
  t('cascade', () { Box<Object> c = Box<String>(); c..put('a')..put(3); });
  t('nullrecv', () { Box<Object>? z; z?.put(arg('x')); print('z'); });
  t('mixin', () { M<Object> m = C<int>(); m.setM('x'); });
  t('mixinok', () { M<Object> m = C<int>(); m.setM(4); print(m.m); });
  t('iface', () { I<Object> i = Impl<int>(); i.take('x'); });
  t('ifaceok', () { I<num> i = Impl<num>(); i.take(2.5); });
  t('dyn', () { dynamic d = Box<int>(); d.put('x'); });
  t('dynok', () { dynamic d = Box<int>(); d.put(3); print(d.get()); });
  t('closure', () { dynamic g = <T extends num>(T x) => x; print(g(1)); g('s'); });
  t('fnfield', () { Object o = Holder().f; print(o is void Function(int)); print(o is void Function(String)); });
  t('ascoll', () { Object l = <int>[1]; var x = l as List<num>; print(x); l as List<String>; });
  t('asnull', () { Object? l = <int?>[null]; l as List<int>; });
  t('asnull2', () { Object? l = <int>[1]; print((l as List<int?>).length); });
  t('generic', () { Object f = id; print(f is T Function<T>(T)); print(f is int Function(int)); var g = id; print(g<String>('q')); });
  t('mapas', () { Object m = <String,int>{'a':1}; m as Map<String,num>; m as Map<String,String>; });
  t('postinc', () { var bs = [Box<int>()]; Box<Object> q = bs[0]; int k=0; q.put(k++); print(k); print(q.get()); });
}

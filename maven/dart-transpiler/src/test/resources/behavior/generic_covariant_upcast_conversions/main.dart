class A<T> { void add(T x) { print('A $x'); } void addAll(List<T> xs) { print('A ${xs.length}'); } }
class B extends A<int> { @override void add(int x) { print('B $x'); } }
abstract class Sink2<T> { void put(T x); }
class IntSink implements Sink2<int> { void put(int x) => print('put $x'); }
class Mix<T> with M<T> {}
mixin M<T> { void m(T x) => print('m $x'); }
void main() {
  A<num> a = B();
  a.add(1);
  try { a.add(1.5); } catch (e) { print('1 ${e is TypeError}'); }
  try { a.addAll(<num>[1.5]); } catch (e) { print('2 ${e is TypeError}'); }
  A<Object> ao = A<String>();
  try { ao.add(3); } catch (e) { print('3 ${e is TypeError}'); }
  Sink2<num> s = IntSink();
  try { s.put(2.5); } catch (e) { print('4 ${e is TypeError}'); }
  M<Object> m = Mix<int>();
  try { m.m('x'); } catch (e) { print('5 ${e is TypeError}'); }
  dynamic da = B();
  try { da.add(2.5); } catch (e) { print('6 ${e is TypeError}'); }
  var fs = <void Function(Object)>[];
  try { fs.add(a.add as dynamic); } catch (e) { print('7 ${e is TypeError}'); }
  List<num> ln = <int>[1];
  try { ln.add(1.5); } catch (e) { print('8 ${e is TypeError}'); }
  try { ln[0] = 2.5; } catch (e) { print('9 ${e is TypeError}'); }
  Map<Object, Object> mo = <String, int>{};
  try { mo['a'] = 'b'; } catch (e) { print('10 ${e is TypeError}'); }
}

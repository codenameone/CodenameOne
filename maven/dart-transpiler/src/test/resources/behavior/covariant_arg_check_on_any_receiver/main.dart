class D<T> { void take(T x) => print('D $x'); T get v => throw 'x'; }
class E extends D<int> { void take(int x) => print('E $x'); }
class W { D<num> d = E(); D<num> get g => E(); }
D<num> getD() => E();
class Base { Base(Object o) { print('base ${o is Function}'); } }
class Sub extends Base { Sub(D<num> d) : super(d.take); }
void main() {
  var w = W();
  try { w.d.take(2.5); } catch (e) { print('F ${e is TypeError}'); }
  try { w.g.take(2.5); } catch (e) { print('G ${e is TypeError}'); }
  try { getD().take(2.5); } catch (e) { print('C ${e is TypeError}'); }
  try { (E() as D<num>).take(2.5); } catch (e) { print('N ${e is TypeError}'); }
  D<num> d = E();
  var t = d.take;
  try { t(2.5); } catch (e) { print('T ${e is TypeError}'); }
  void Function(num) t2 = d.take;
  try { t2(3.5); } catch (e) { print('T2 ${e is TypeError}'); }
  try { [d].first.take(4.5); } catch (e) { print('L ${e is TypeError}'); }
  try { Sub(d); } catch (e) { print('S ${e is TypeError}'); }
  d.take(7);
}

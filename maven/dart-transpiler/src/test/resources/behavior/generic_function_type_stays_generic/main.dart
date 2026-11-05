typedef Cb<T> = void Function(T);
class Ev<T> { final Cb<T> cb; Ev(this.cb); void fire(T v) => cb(v); }
class P { num get v => 1; num m(num x) => x; }
class Q extends P { @override int get v => 2; @override int m(num x) => x.toInt() + 1; }
void main() {
  Ev<num> e = Ev<int>((i) => print('i $i'));
  e.fire(3);
  try { e.fire(3.5); } catch (x) { print('F ${x is TypeError}'); }
  try { Cb<num> c = e.cb; print('no'); c(1); } catch (x) { print('CB ${x is TypeError}'); }
  P p = Q(); print(p.v); print(p.m(2.7));
  dynamic dp = p; print(dp.m(1));
  var gens = <T>(T a, T b) => [a, b];
  print(gens(1, 2).runtimeType);
  print(gens<num>(1, 2.5).runtimeType);
  dynamic dg = gens;
  print(dg(1, 'a').runtimeType);
  print(dg<int>(1, 2).runtimeType);
  try { dg<int>(1, 'a'); } catch (x) { print('DG ${x is TypeError}'); }
  List<T> Function<T>(T, T) keep = gens;
  print(keep<String>('a','b').runtimeType);
}

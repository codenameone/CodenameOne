class V implements Comparable<V> { final int n; V(this.n); int compareTo(V o) => n - o.n; String toString() => 'V$n'; }
class W extends V { W(int n) : super(n); }
abstract class Ent<T extends Ent<T>> implements Comparable<T> {}
class E1 extends Ent<E1> { final int k; E1(this.k); int compareTo(E1 o) => k - o.k; String toString() => 'E$k'; }
T maxOf<T extends Comparable<T>>(T a, T b) => a.compareTo(b) >= 0 ? a : b;
void main() {
  dynamic f = maxOf;
  void t(String l, Object? Function() g) { try { print(g()); } catch (e) { print('$l ${e is TypeError}'); } }
  t('i', () => f(1, 2)); t('d', () => f(1.5, 2.5)); t('s', () => f('a', 'b')); t('v', () => f(V(1), V(2)));
  t('w', () => f(W(1), W(2))); t('dt', () => f(DateTime(2020), DateTime(2021)).year); t('du', () => f(Duration(seconds:1), Duration(seconds:2)));
  t('b', () => f(true, false)); t('bi', () => f(BigInt.one, BigInt.two)); t('mix', () => f(1, 2.5)); t('e1', () => f(E1(1), E1(2)));
  t('xi', () => f<int>(1, 2)); t('xd', () => f<double>(1.0, 2.0)); t('xn', () => f<num>(1, 2)); t('xw', () => f<W>(W(1), W(3)));
  t('xv', () => f<V>(V(1), V(3))); t('xdu', () => f<Duration>(Duration.zero, Duration(days:1))); t('xbi', () => f<BigInt>(BigInt.one, BigInt.two));
  t('xc', () => f<Comparable>(1, 2)); t('xo', () => f<Object>(1, 2)); t('xe', () => f<E1>(E1(1), E1(3)));
  t('xu', () => f<Uri>(Uri.parse('a'), Uri.parse('b')));
}

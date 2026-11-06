class V implements Comparable<V> { final int n; V(this.n); int compareTo(V o) => n - o.n; String toString() => 'V$n'; }
T maxOf<T extends Comparable<T>>(T a, T b) => a.compareTo(b) >= 0 ? a : b;
void main() {
  dynamic f = maxOf;
  for (var p in [[1, 2], [1.5, 2.5], ['a', 'b'], [V(1), V(2)], [DateTime(2020), DateTime(2021)], [Duration(seconds: 1), Duration(seconds: 2)], [true, false], [BigInt.one, BigInt.two], [1, 2.5]]) {
    try { print(f(p[0], p[1])); } catch (e) { print('err ${e is TypeError}'); }
  }
  try { print(f<int>(1, 2)); } catch (e) { print('ei ${e is TypeError}'); }
  try { print(f<double>(1.0, 2.0)); } catch (e) { print('ed ${e is TypeError}'); }
  try { print(f<String>('a', 'b')); } catch (e) { print('es ${e is TypeError}'); }
  try { print(f<V>(V(1), V(3))); } catch (e) { print('ev ${e is TypeError}'); }
  try { print(f<num>(1, 2)); } catch (e) { print('en ${e is TypeError}'); }
  try { print(f<Duration>(Duration.zero, Duration(days:1))); } catch (e) { print('edu ${e is TypeError}'); }
  try { print(f<DateTime>(DateTime(1), DateTime(2)).year); } catch (e) { print('edt ${e is TypeError}'); }
  try { print(f<BigInt>(BigInt.one, BigInt.two)); } catch (e) { print('eb ${e is TypeError}'); }
  try { print(f<Comparable>(1, 2)); } catch (e) { print('ec ${e is TypeError}'); }
}

class Long { int v = 1; }
class MainLong { String toString() => 'ml'; }
class Math { static int sq(int x) => x * x; }
class System { static String get out => 'sysout'; }
class C { int? _n; int _k = 0; }
void main() {
  var l = Long(); l.v++; print(l.v); print(l.runtimeType); print(MainLong()); print(Math.sq(3)); print(System.out);
  print(3.0.floor()); print(int.parse('12') + 1);
  var c = C(); c._n = 1;
  if (c._n != null) { c._n = c._n! + 1; print(c._n); }
  c._k++; c._k += 2; print(c._k--); print(c._k);
  Object? z = 5;
  for (var i = 0; i < 2; i++) { if (z is int) { print(z + i); z = 'x$i'; } else { print(z); } }
  print(switch (z) { String s when s.length > 1 => 'S$s', _ => 'none' });
}

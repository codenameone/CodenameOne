enum Color { red, green, blue }
class P { final int x; P(this.x); @override bool operator ==(Object o) => o is P && o.x == x; @override int get hashCode => x.hashCode; @override String toString() => 'P($x)'; }
class Q { final int x; Q(this.x); }
void main() {
  final m = <String, int>{};
  for (var i = 0; i < 2000; i++) { m['k${i % 50}'] = i; }
  print(m.length); print(m['k7']);
  final e = <Color, String>{Color.red: 'r', Color.blue: 'b'};
  print(e[Color.blue]); print(e.containsKey(Color.green)); print(e.keys.toList());
  final s = <P>{P(1), P(2), P(1)}; print(s.length); print(s.contains(P(2)));
  final q1 = Q(1); final qs = <Q>{q1, Q(1)}; print(qs.length); print(qs.contains(q1)); print(qs.contains(Q(1)));
  final idm = Map<Object, int>.identity(); idm[q1] = 1; idm[Q(1)] = 2; idm['a'] = 3; idm['a'] = 4; print(idm.length);
  final ih = Set<Object>.identity(); ih.add(1); ih.add(1); ih.add(P(1)); ih.add(P(1)); print(ih.length);
  final fm = <Function, int>{}; void f() {} 
  fm[main] = 1; fm[main] = 2; fm[print] = 3; print(fm.length);
  final ts = <Object>{}; for (var i = 0; i < 100000; i++) { ts.add(i % 10); ts.remove(i % 10); } print(ts.length);
  final big = <Object?, int>{null: 0, 1: 1, 1.0: 2, 'x': 3, #s: 4, true: 5, Color.red: 6}; print(big.length); print(big[1]);
  print(identityHashCode(q1) == identityHashCode(q1)); print(Map.of(e) == e);
  final lst = [for (var i = 0; i < 5; i++) () => i]; final fs = lst.toSet(); print(fs.length);
}

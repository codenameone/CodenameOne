sealed class S {}
class X extends S { final int n; X(this.n); }
class Y extends S { final String s; Y(this.s); }
String f(Object? o) => switch (o) { int i when i > 3 => 'big$i', int i => 'small${i + 1}', String s when s.isNotEmpty => s.toUpperCase(), _ => 'other' };
String g(S s) { if (s is! X) return 'notx'; return 'x${s.n + 1}'; }
String h(Object o) { if (o is! String) { return 'ns'; } return switch (o.length) { 0 => 'empty', _ => o.substring(1) }; }
String k(Object? o) { switch (o) { case int v when v.isEven: return 'even$v'; case String v: return v.toUpperCase(); } return 'none'; }
void main() {
  print(f(5)); print(f(2)); print(f('ab')); print(f(''));
  print(g(X(2))); print(g(Y('q')));
  print(h('hello')); print(h('')); print(h(3));
  print(k(4)); print(k(3)); print(k('z'));
  Object? p = 3;
  if (p is int) { var r = switch (p) { > 2 => p + 10, _ => p }; print(r); p = 'now'; print(p is String ? p.length : -1); }
  num q = 3; if (q is int) { q++; print(q); q += 1.5; print(q); }
  Object w = 1; if (w is int) { w = w + 1; print(w); }
}

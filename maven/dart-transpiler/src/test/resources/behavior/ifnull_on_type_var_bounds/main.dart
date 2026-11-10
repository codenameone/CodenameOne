class S<T extends String?> {
  T v; S(this.v);
  int len() => (v ?? "dflt").length;
  String up() => (v ?? 'x').toUpperCase();
}
class C<T extends Comparable<T>> {
  T? a; T b; C(this.a, this.b);
  int cmp() => (a ?? b).compareTo(b);
  T pick() => a ?? b;
}
class L<T extends List<int>?> {
  T v; L(this.v);
  int first() => (v ?? [9]).first;
}
class N<T extends num?> {
  T v; N(this.v);
  String s() => (v ?? 2).toString();
  double d() => (v ?? 1.5) * 2.0;
}
R first<R extends Object>(R? a, R b) => a ?? b;
void main() {
  print(S<String?>(null).len());
  print(S<String>('ab').up());
  print(C<String>(null, 'b').cmp());
  print(C<String>("e", "c").cmp());
  print(C<String>('a', 'z').pick().length);
  print(L<List<int>?>(null).first());
  print(N<int?>(null).s());
  print(N<double?>(null).d());
  print(first<String>(null, 'w').length);
}

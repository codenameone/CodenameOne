class C {
  String named(int p, {int a = 1, String? b}) => 'n $p $a $b';
  String opt([int x = 7, int? y]) => 'o $x $y';
  T gen<T>(T v, {T? w}) => v;
}
String topn({int? a, int b = 2}) => 'top $a $b';
void main() {
  final c = C();
  Function nf = c.named;
  print(nf(1)); print(nf(2, a: 3)); print(nf(3, b: null)); print(nf(4, b: 'x', a: 9));
  Function of = c.opt; print(of()); print(of(1)); print(of(1, null)); print(of(null == null ? 2 : 3, 4));
  Function gf = c.gen; print(gf<int>(5)); print(gf(6, w: 7)); print(gf('s'));
  Function tf = topn; print(tf()); print(tf(a: null, b: 5));
  dynamic d = c.named; print(d(5, a: 6));
  try { nf(1, z: 2); } catch (e) { print('nsm ${e.runtimeType.toString().contains("NoSuchMethod")}'); }
  try { nf(); } catch (e) { print('nsm2'); }
  final l = <Function>[c.named, topn];
  print(Function.apply(l[0], [8], {#a: 1}));
  print(l[1](b: 0));
}

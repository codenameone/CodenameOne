class P<T> {
  final T v;
  P(this.v);
  String get t => '$T';
}
T id<T>(T x) => x;
List<T> wrap<T>(T x) => <T>[x];
void main() {
  dynamic pd = P.new;
  print(pd(4).t);
  print((Function.apply(P.new, [4]) as P).t);
  dynamic w = wrap;
  print(w(3).runtimeType);
  print(Function.apply(wrap, ['s']).runtimeType);
  print(Function.apply(id, [1]));
  more();
}

class N<T extends num> {
  N();
  String get t => '$T';
}
class R<T extends Comparable<T>> {
  R();
  String get t => '$T';
}
class F<T> {
  final T v;
  F._(this.v);
  factory F(T v) => F._(v);
  const F.c(this.v);
  F.r(T v) : this._(v);
  String get t => '$T';
}
void more() {
  dynamic n = N.new;
  print(n().t);
  dynamic f = F.new, fc = F.c, fr = F.r;
  print('${f(1).t} ${fc(1).t} ${fr(1).t}');
  print((Function.apply(F.r, ['x']) as F).t);
  F<int> Function(int) typed = F.new;
  print(typed(2).t);
}

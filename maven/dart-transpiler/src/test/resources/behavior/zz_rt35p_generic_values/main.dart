class G<T> {
  final T v;
  G(this.v);
  String toString() => 'G<$T>($v)';
}

T identity<T>(T x) => x;

List<T> twice<T>(T x) => [x, x];

void main() {
  final c = G.new;
  print(c(1));
  print(c('s'));
  print(c(1).runtimeType);
  print(c is Function);
  dynamic d = c;
  print(d(2.5));
  print(Function.apply(c, [true]));
  final f = identity;
  print(f(1).runtimeType);
  print(f('x'));
  print(f == identity);
  print(f == f);
  final t = twice;
  print(t(3).runtimeType);
  print(t('a'));
  print(c == c);
}

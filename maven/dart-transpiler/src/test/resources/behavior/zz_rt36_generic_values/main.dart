class G<T> {
  final T v;
  G(this.v);
  String toString() => 'G<$T>($v)';
}

class N<T extends num> {
  final T v;
  N(this.v);
  String toString() => 'N<$T>($v)';
}

T identity<T>(T x) => x;

T firstOf<T>(List<T> xs) => xs.first;

T pick<T extends Comparable<T>>(T a, T b) => a.compareTo(b) >= 0 ? a : b;

void main() {
  final a = G.new;
  final b = G.new;
  print(a == b);
  print(a(1));
  print(b('s'));

  final n = N.new;
  print(n(2));
  print(n(2.5));

  final f = firstOf;
  final x = f(<String>['s', 't']);
  print(x.length);
  print(f(<int>[7]));

  final p = pick;
  print(p(3, 9));
  print(p('b', 'a').toUpperCase());

  T Function<T>(T) id = identity;
  print(id(5).isEven);
  print(id<String>('q').length);
}

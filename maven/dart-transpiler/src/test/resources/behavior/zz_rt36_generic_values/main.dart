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
  final Object ob = b;
  print(ob == a);
  print(identical(a, b));
  print(b.hashCode == a.hashCode);
  print(G.new == G.new);
  print(identical(G.new, G.new));
  print(G.new.hashCode == G.new.hashCode);
  print(<Object>{G.new, G.new}.length);
  print(<Object>[b].length);

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
  print(id == identity);
  print(identical(id, identity));
  final g1 = G<int>(1);
  print(g1.runtimeType);
  print(G<List<int>>([1]).runtimeType);
  print(G<G<String>>(G<String>('x')));
  print(G<Map<String, List<int>>>({}).runtimeType);
  print(G<G<int>>(G<int>(1)).runtimeType);
  print(G<List<Object>>([]).runtimeType);
  print(G<List<Object>>([]).runtimeType.toString());
  print(<G<int>>[].runtimeType);
  print(G<Set<Object>>({}).runtimeType);
}

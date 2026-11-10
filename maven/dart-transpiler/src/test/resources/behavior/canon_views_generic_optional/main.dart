T gen<T>(T a, {int n = 0}) => a;
T pos<T>(T a, [T? b]) => b ?? a;
List<T> rep<T>(T a, {int n = 1}) => List<T>.filled(n, a);
List<T> opt<T>(T a, [int n = 1]) => List<T>.filled(n, a);

class G<T> {
  final T v;
  final int k;
  G(this.v, {this.k = 1});
  @override
  String toString() => 'G($v,$k)';
}

class H {
  final int x;
  H([this.x = 7]);
}

int plain(int a) => a + 1;

typedef IntFn = int Function(int);

void main() {
  final g = gen;
  print(g<int>(3));
  print(g<String>('s', n: 2));
  print(g(4.5));
  print({g, gen}.length);
  final p = pos;
  print(p<int>(1));
  print(p<int>(1, 2));
  print(p('a', 'b'));

  final r = rep;
  print(r<num>(1, n: 2).runtimeType);
  print(r<String>('x'));
  final q = opt;
  print(q<num>(2).runtimeType);
  print(q<int>(2, 3));
  final c = G.new;
  print(c<int>(5));
  print(c<String>('w', k: 9));
  final h = H.new;
  print(h().x);
  print(h(3).x);
  print(c<String>('z') is G<String>);
  print(c<String>('z') is G<int>);

  final b = G.new;
  final s = {G.new, b};
  print(s.length);
  Object o = G.new;
  print({o, b, G.new}.length);
  print({c, b}.contains(G.new));
  final m = <Object, int>{G.new: 1};
  m[b] = 2;
  print(m.length);
  print(m[o]);

  IntFn typed = plain;
  Function loose = plain;
  Object obj = plain;
  final fs = <Function>{typed, loose, obj as Function, plain};
  print(fs.length);
  final fm = <Object, String>{typed: 't'};
  print(fm[loose]);
  print(fm.containsKey(obj));

  final inst = gen<int>;
  int Function(int, {int n}) inst2 = gen;
  print({inst, inst2}.length);
  print(inst == inst2);
  print(gen<int> == gen<String>);
  print(gen<int> == inst);
  Object un = gen;
  print(un == gen);
  print(identical(G.new, G.new));
  print(identical(g, gen));
  final ids = Set<Object>.identity()..add(G.new)..add(b)..add(o)..add(c);
  print(ids.length);
  final idf = Set<Object>.identity()..add(typed)..add(loose)..add(obj)..add(plain);
  print(idf.length);
  final idm = Map<Object, int>.identity();
  idm[G.new] = 1;
  idm[b] = 2;
  idm[typed] = 3;
  idm[obj] = 4;
  print(idm.length);
  print(idm[o]);
  print(ids.contains(G.new));
  print((Set<Object>.identity()..add(g)..add(gen)).length);
}

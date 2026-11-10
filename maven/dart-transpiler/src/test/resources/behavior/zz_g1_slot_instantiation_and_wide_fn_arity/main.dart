T gen<T>(T x) => x;

int f5(int a, int b, int c, int d, int e) => a + b + c + d + e;
int f8(int a, int b, int c, int d, int e, int f, int g, int h) =>
    a + b + c + d + e + f + g + h;
int f12(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j,
        int k, int l) =>
    a + b + c + d + e + f + g + h + i + j + k + l;
int fopt(int a, int b, int c, int d, int e, [int f = 10, int g = 20]) =>
    a + b + c + d + e + f + g;
int fnamed(int a, int b, int c, int d, int e, {int f = 100, int g = 200}) =>
    a + b + c + d + e + f + g;

Future<T> g5<T>(T a, T b, T c, T d, T e) async => e;
Future<T> g8<T>(T a, T b, T c, T d, T e, T f, T g, T h) async => h;
Future<T> g12<T>(T a, T b, T c, T d, T e, T f, T g, T h, T i, T j, T k, T l) async => l;
Future<T> gopt<T>(T a, T b, T c, T d, T e, [T? f]) async => f ?? e;
Future<T> gnamed<T>(T a, T b, T c, T d, T e, {T? f}) async => f ?? a;
Future<void> v6<T>(T a, T b, T c, T d, T e, T f) async { print('v6 $f'); }

class Wide<T> {
  final List<T> all;
  Wide(T a, T b, T c, T d, T e, T f) : all = [a, b, c, d, e, f];
  Wide.opt(T a, T b, T c, T d, T e, [T? f]) : all = [a, b, c, d, e, if (f != null) f];
  Wide.named(T a, T b, T c, T d, T e, {T? f}) : all = [a, b, c, d, e, if (f != null) f];
}

Future<void> wide() async {
  final fs = <Object>[g5, g8, g12, gopt, gnamed, v6];
  print(await (fs[0] as Function)(1, 2, 3, 4, 5));
  print(await (fs[1] as Function)(1, 2, 3, 4, 5, 6, 7, 8));
  print(await (fs[2] as Function)(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
  print(await (fs[3] as Function)(1, 2, 3, 4, 5));
  print(await (fs[3] as Function)(1, 2, 3, 4, 5, 6));
  print(await (fs[4] as Function)(1, 2, 3, 4, 5));
  print(await (fs[4] as Function)(1, 2, 3, 4, 5, f: 9));
  await (fs[5] as Function)(1, 2, 3, 4, 5, 6);
  final m = <String, Object>{'g12': g12};
  print(await (m['g12'] as Function)('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l'));
  print(fs[2] == g12);
  final cs = <Object>[Wide.new, Wide.opt, Wide.named];
  print(((cs[0] as Function)(1, 2, 3, 4, 5, 6) as Wide).all);
  print(((cs[1] as Function)(1, 2, 3, 4, 5) as Wide).all);
  print(((cs[1] as Function)(1, 2, 3, 4, 5, 6) as Wide).all);
  print(((cs[2] as Function)(1, 2, 3, 4, 5, f: 7) as Wide).all);
}

void main() async {
  Object Function(Object) f = gen;
  print(f == gen<Object>);
  print(f == gen);
  print(f(7));
  int Function(int) fi = gen;
  print(fi == gen<int>);
  print(fi == gen<Object>);
  Object o = gen;
  print(o == gen);
  dynamic dd = gen;
  print(dd == gen);
  print(dd<String>('s'));

  Function p5 = f5;
  Function p8 = f8;
  Function p12 = f12;
  Function po = fopt;
  Function pn = fnamed;
  print(p5(1, 2, 3, 4, 5));
  print(p8(1, 2, 3, 4, 5, 6, 7, 8));
  print(p12(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
  print(po(1, 2, 3, 4, 5));
  print(po(1, 2, 3, 4, 5, 6));
  print(pn(1, 2, 3, 4, 5));
  print(pn(1, 2, 3, 4, 5, g: 1));
  print(p5 == f5);
  print(p12 is int Function(int, int, int, int, int, int, int, int, int, int, int, int));
  print(Function.apply(p8, [1, 1, 1, 1, 1, 1, 1, 1]));
  final list = <Function>[f5, f12, fnamed];
  print(Function.apply(list[2], [1, 1, 1, 1, 1], {#f: 0}));
  Object ob = f8;
  print((ob as Function)(1, 2, 3, 4, 5, 6, 7, 8));
  await wide();
}

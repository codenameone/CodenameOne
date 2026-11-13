typedef IntOp = int Function(int);
typedef VoidCb = void Function();
typedef Named = String Function(int a, {String? sep});
typedef Opt = int Function(int a, [int b]);
int twice(int x) => x * 2;
String named(int a, {String? sep}) => '$a${sep ?? "-"}';
int opt(int a, [int b = 1]) => a + b;
void run(VoidCb cb) { print(cb.runtimeType); cb(); }
void main() {
  Object f = twice;
  print(f is IntOp);
  print(f is VoidCb);
  print(f is int Function(Object));
  print(f is Object Function(int));
  print(f is Function);
  Object n = named;
  print(n is Named);
  print(n is String Function(int));
  print(n is String Function(int, {String? sep, int? x}));
  Object o = opt;
  print(o is Opt);
  print(o is int Function(int));
  print(o is int Function(int, int));
  print(o is int Function(int, int, int));
  try { (f as VoidCb)(); } catch (e) { print('cast fail'); }
  IntOp g = f as IntOp; print(g(4));
  int Function(int)? nf; print(nf == null); nf = twice; print(nf!(5));
  Object? maybe = null; print(maybe is IntOp?); print(maybe is IntOp);
  var c1 = () {}; print(c1.runtimeType);
  var c2 = () { return null; }; print(c2.runtimeType);
  VoidCb c3 = () { return; }; print(c3.runtimeType);
  run(() { print('ran'); });
  var c4 = (int x) { if (x > 0) return null; }; print(c4.runtimeType);
  print(IntOp); print(Named); print(Opt);
  print(IntOp == a2t<int Function(int)>());
  print(twice.runtimeType == IntOp);
  print(named.runtimeType); print(opt.runtimeType);
}
Type a2t<T>() => T;

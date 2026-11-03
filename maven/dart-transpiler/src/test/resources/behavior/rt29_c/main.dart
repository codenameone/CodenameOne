typedef IntOp = int Function(int);
(int, IntOp) pair() => (3, (x) => x + 1);
({String n, void Function() cb}) rec() => (n: 'r', cb: () {});
void main() {
  var p = pair();
  print(p.$2(p.$1));
  print(p.$2.runtimeType);
  Object r = p;
  print(r is (int, IntOp));
  print(r is (int, int Function(int)));
  print(r is (int, String Function(int)));
  print(r is (Object, Function));
  var q = rec();
  print(q.n); q.cb();
  print(q.cb.runtimeType);
  print((1, 'a').runtimeType == (int, String));
  var s = <(int, String)>{(1, 'a'), (1, 'a'), (2, 'b')};
  print(s.length);
  void Function(int) sink = (int x) { print('got $x'); };
  sink(1); print(sink.runtimeType);
  Function f = ([int? x]) => x;
  print(f.runtimeType); print(f()); print(f(2));
  Function g = ({required int k}) => k;
  print(g.runtimeType); print(g(k: 5));
  Null Function() nn = () => null; print(nn.runtimeType);
  var vv = () => print('x'); print(vv.runtimeType);
}

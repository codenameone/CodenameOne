import 'dart:async';
typedef VoidCallback = void Function();
class Notifier {
  final List<VoidCallback> _ls = [];
  void addListener(VoidCallback l) => _ls.add(l);
  void removeListener(VoidCallback l) => _ls.remove(l);
  void notify() { for (final l in List<VoidCallback>.of(_ls)) { l(); } }
  int get count => _ls.length;
}
class Counter { int calls = 0; void tick() { calls++; } }
class Panel {
  final Notifier n;
  int ticks = 0;
  Panel(this.n);
  void _onTick() { ticks++; }
  void init() { n.addListener(_onTick); }
  void dispose() { n.removeListener(this.handler); n.removeListener(_onTick); }
  void handler() {}
}
void take(void Function() f, void Function() g) { print(f == g); }
int one() => 1;
int twice(int Function() f) => f() * 2;
void main() async {
  var n = Notifier();
  var c = Counter();
  n.addListener(c.tick);
  n.notify();
  n.removeListener(c.tick);
  n.notify();
  print('${c.calls} ${n.count}');
  var p = Panel(n);
  p.init(); n.notify(); p.dispose(); n.notify();
  print('${p.ticks} ${n.count}');
  take(c.tick, c.tick);
  take(c.tick, Counter().tick);
  var seen = <int>{};
  print([1, 2, 1, 3].where(seen.add).toList());
  var got = <int>[];
  var ctl = StreamController<int>();
  var sub = ctl.stream.listen(got.add);
  ctl.add(1); await Future.delayed(Duration.zero);
  await sub.cancel(); ctl.add(2); await Future.delayed(Duration.zero);
  print(got);
  // static tear-off into two function types stays identical
  int Function() a = one;
  Object b = one;
  print(identical(a, b)); print(a == b);
  dynamic d = one; print(identical(d, a));
  print(twice(one));
  // conditional closure local
  var flag = got.isNotEmpty;
  var f = flag ? (int x) => 'p$x' : (int x) => 'n$x';
  print(f is String Function(int)); print(f(3));
  var g = flag ? (int x) => x + 1 : (int x) => x - 1;
  print(g is int Function(int)); print(g(3) + 1);
  print(f is int Function(int)); print(g is String Function(int));
  print(f(4).length); print(g(4).isEven);
  var h = flag ? (String s) => s.length > 1 : (String s) => s.isEmpty;
  print(h is bool Function(String)); print(h is int Function(String)); print(h('ab') && true);
  var k = flag ? (int x) => x * 1.5 : (int x) => x / 2;
  print(k is double Function(int)); print(k(2).toStringAsFixed(1));
  var r = f(3); print(r.toUpperCase()); var q = g(3) + k(2); print(q);
  var m = flag ? (int x) => x + 1 : (int x) => x * 0.5; print(m is num Function(int)); print(m(2) + 1);
}

class C {
  int n = 0;
  void tick() { n++; }
  int add(int x) => x + n;
  static void st() {}
  void named({int a = 1, int b = 2}) { n += a * b; }
}
void top() {}
class Reg {
  final List<void Function()> ls = [];
  void add(void Function() f) => ls.add(f);
  void remove(void Function() f) => ls.remove(f);
}
void main() {
  var c = C(), d = C();
  void Function() lam = () => c.tick();
  void Function() t = c.tick;
  print('lam ${t == lam} ${lam == t}');
  print('recv ${c.tick == d.tick} ${c.tick == c.tick}');
  print('static ${C.st == C.st} ${top == top} ${identical(top, top)}');
  Object o = c.tick;
  print('obj ${o == c.tick} ${c.tick == o} ${o.hashCode == c.tick.hashCode}');
  var s = <Object>{c.tick, c.tick, d.tick, top, top, C.st};
  print('set ${s.length}');
  var m = <Function, int>{};
  m[c.add] = 1; m[c.add] = 2;
  print('map ${m.length} ${m[c.add]}');
  print('cast ${(c.tick as Function) == c.tick} ${(o as void Function()) == t}');
  var lst = <void Function()>[c.tick];
  print('lit ${lst.contains(c.tick)} ${lst.indexOf(c.tick)}');
  Function nf = c.named;
  print('named ${nf == c.named}');
  c.named(a: 3);
  (c.named)(b: 5);
  print('n ${c.n}');
  var r = Reg();
  for (var i = 0; i < 3; i++) { r.add(c.tick); }
  r.remove(c.tick);
  print('reg ${r.ls.length}');
  for (var f in r.ls) f();
  print('n2 ${c.n}');
  var e = c.tick; var g = c.tick;
  print('local ${e == g} ${e.hashCode == g.hashCode}');
  print('lamself ${lam == lam}');
  int Function(int) ai = c.add;
  print('call ${ai(2)} ${ai == c.add}');
  print('tostr ${c.tick.runtimeType == t.runtimeType}');
}

typedef VoidCallback = void Function();
class C { int n = 0; void tick() { n++; } }
mixin M { void mm() {} }
class WithM with M {}
class Base { void m() {} }
class Sub extends Base { void m() {} void reg(Reg r) { r.add(super.m); r.remove(super.m); } }
class Reg {
  final s = <VoidCallback>{};
  final m = <VoidCallback, int>{};
  final l = <VoidCallback>[];
  void add(VoidCallback f) { s.add(f); m[f] = (m[f] ?? 0) + 1; l.add(f); }
  void remove(VoidCallback f) { s.remove(f); m.remove(f); l.remove(f); }
  bool has(VoidCallback f) => s.contains(f);
  String get st => '${s.length} ${m.length} ${l.length}';
}
class Owner {
  final r = Reg();
  int hits = 0;
  void on() { hits++; }
  void attach() { r.add(on); }
  void detach() { r.remove(on); }
}
void main() {
  final c = C(); final r = Reg();
  r.add(c.tick); r.add(c.tick);
  print('dup ${r.st} has ${r.has(c.tick)}');
  r.remove(c.tick);
  print('rm ${r.st}');
  final w = WithM(); r.add(w.mm); r.remove(w.mm); print('mixin ${r.st}');
  Sub().reg(r); print('super ${r.st}');
  for (var i = 0; i < 300; i++) { final o = Owner(); o.attach(); o.detach(); if (o.r.st != '0 0 0') print('bad $i'); }
  print('loop ok');
}

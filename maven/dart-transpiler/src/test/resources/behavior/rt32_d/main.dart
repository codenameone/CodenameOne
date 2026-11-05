class C { int n = 0; void tick() { n++; } }
class Base { String m() => 'base'; }
class Sub extends Base {
  @override String m() => 'sub';
  String Function() sup() { final String Function() f = super.m; return f; }
}
mixin M { String mm() => 'mm'; }
class WithM with M {}
class Holder {
  late final void Function() cb;
  Holder() { cb = onTick; }
  void onTick() {}
}
void main() {
  final c = C();
  final s = <void Function()>{};
  s.add(c.tick); s.add(c.tick);
  print('set ${s.length}');
  final m = <void Function(), int>{};
  m[c.tick] = 1; m[c.tick] = 2;
  print('map ${m.length}');
  final void Function() a = c.tick;
  final void Function() b = c.tick;
  print('eq ${a == b} hash ${a.hashCode == b.hashCode} ident ${identical(a, b)}');
  final list = <void Function()>[];
  list.add(c.tick);
  list.remove(b);
  print('list ${list.length}');
  final sb = Sub();
  final x1 = sb.sup(); final x2 = sb.sup();
  print('super ${x1 == x2} ${x1()}');
  final w = WithM();
  final String Function() m1 = w.mm; final String Function() m2 = w.mm;
  print('mixin ${m1 == m2}');
  final h = Holder();
  final void Function() hc = h.onTick;
  print('holder ${h.cb == hc}');
  final lst = <void Function()>[];
  for (var i = 0; i < 100; i++) { lst.add(c.tick); }
  for (var i = 0; i < 100; i++) { final void Function() r = c.tick; lst.remove(r); }
  print('loop ${lst.length}');
}

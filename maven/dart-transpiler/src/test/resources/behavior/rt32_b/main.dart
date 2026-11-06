class C {
  int n = 0;
  void tick() { n++; }
  int get g => n;
  C operator +(C o) => this;
  T id<T>(T x) => x;
}
class Base { String m() => 'base'; }
class Sub extends Base {
  @override String m() => 'sub';
  String Function() sup() => super.m;
}
mixin M { String mm() => 'mm'; }
class WithM with M {}
extension E on C { int twice() => n * 2; }

void main() {
  final c = C();
  final s = <Function>{};
  s.add(c.tick); s.add(c.tick);
  print('set len ${s.length} contains ${s.contains(c.tick)}');
  final m = <Function, int>{};
  m[c.tick] = 1; m[c.tick] = 2;
  print('map len ${m.length} val ${m[c.tick]}');
  final cb = c.tick;
  final list = <void Function()>[cb];
  list.remove(c.tick);
  print('list after remove ${list.length}');
  var x = c.tick;
  print('x==c.tick ${x == c.tick} identical ${identical(x, c.tick)}');
  print('hash eq ${c.tick.hashCode == c.tick.hashCode}');
  final c2 = C();
  print('diff recv ${c.tick == c2.tick}');
  print('generic ${c.id == c.id} inst ${(c.id<int>) == (c.id<int>)}');
  int Function(int) gi = c.id;
  print('generic view ${gi == c.id<int>} ${gi(4)}');
  final sb = Sub();
  print('super ${sb.sup() == sb.sup()} ${sb.sup()()} vs m ${sb.sup() == sb.m}');
  final w = WithM();
  print('mixin ${w.mm == w.mm} ${{w.mm, w.mm}.length}');
  print('ext ${c.twice == c.twice}');
  Object o = c.tick; Function f = c.tick;
  print('views ${o == f} ${{o, f}.length}');
  print('str ${[c.tick].indexOf(c.tick)} ${[c.tick].contains(c.tick)}');
  Object v = 3;
  final fn = () { if (v is int) { return v + 1; } return 0; };
  print('cond ${fn()}');
  final g = v is int ? () { if (v is int) return v * 2; return -1; } : () => 0;
  print('cond2 ${g()}');
}
